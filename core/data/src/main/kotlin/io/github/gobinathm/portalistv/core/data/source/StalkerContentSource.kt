package io.github.gobinathm.portalistv.core.data.source

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.Episode
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.LogoPack
import io.github.gobinathm.portalistv.core.model.PagedResult
import io.github.gobinathm.portalistv.core.model.Programme
import io.github.gobinathm.portalistv.core.model.Season
import io.github.gobinathm.portalistv.core.model.SourceAccount
import io.github.gobinathm.portalistv.core.model.SourceException
import io.github.gobinathm.portalistv.core.model.VodCategory
import io.github.gobinathm.portalistv.core.model.VodItem
import io.github.gobinathm.portalistv.core.model.VodPlayback
import io.github.gobinathm.portalistv.core.stalker.AccountBlockedException
import io.github.gobinathm.portalistv.core.stalker.AuthorizationException
import io.github.gobinathm.portalistv.core.stalker.Channel
import io.github.gobinathm.portalistv.core.stalker.InvalidPortalUrlException
import io.github.gobinathm.portalistv.core.stalker.Page
import io.github.gobinathm.portalistv.core.stalker.PortalNotFoundException
import io.github.gobinathm.portalistv.core.stalker.ProtocolException
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.core.stalker.StalkerException
import io.github.gobinathm.portalistv.core.stalker.VodSection
import io.github.gobinathm.portalistv.core.stalker.seriesContents
import io.github.gobinathm.portalistv.core.stalker.vodCategories
import io.github.gobinathm.portalistv.core.stalker.vodLink
import io.github.gobinathm.portalistv.core.stalker.vodPage
import io.github.gobinathm.portalistv.core.stalker.VodItem as PortalVodItem

/** [ContentSource] for Stalker/Ministra portals. Channels without a logo get one from [logos] if it has one. */
@Suppress("TooManyFunctions") // One override per ContentSource call.
internal class StalkerContentSource(
    private val client: StalkerClient,
    override val playbackUserAgent: String?,
    private val logos: BundledLogos,
) : ContentSource {
    override suspend fun connect(): SourceAccount = mapErrors {
        client.connect().let { SourceAccount(displayName = it.name, message = it.message) }
    }

    override suspend fun liveCategories(): List<LiveCategory> = mapErrors {
        client.genres().map { LiveCategory(id = it.id, title = it.title, adult = it.censored) }
    }

    override suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel> = mapErrors {
        val result = client.channels(categoryId, page)
        val bundled = logos.index()
        PagedResult(result.items.map { it.toLive(bundled) }, result.totalItems, result.pageCount)
    }

    override suspend fun nowNext(channel: LiveChannel): List<Programme> = programmes(channel, size = 2)

    // Portals without a per-day guide still answer the short guide for many hours ahead.
    override suspend fun guide(channel: LiveChannel): List<Programme> = programmes(channel, GUIDE_SIZE)

    private suspend fun programmes(channel: LiveChannel, size: Int): List<Programme> = mapErrors {
        client.shortEpg(channel.id, size).map {
            Programme(it.title, it.description, it.startSeconds * MILLIS, it.stopSeconds * MILLIS)
        }
    }

    override suspend fun streamUrl(channel: LiveChannel): String = mapErrors { client.createLink(channel.playbackRef) }

    override suspend fun vodCategories(): List<VodCategory> = mapErrors {
        client.vodCategories(VodSection.MOVIES).map { VodCategory(it.id, it.title, it.censored) }
    }

    override suspend fun vodItems(categoryId: String, page: Int): PagedResult<VodItem> = mapErrors {
        val result = client.vodPage(VodSection.MOVIES, categoryId, page)
        PagedResult(result.items.map { it.toVod() }, result.totalItems, result.pageCount)
    }

    override suspend fun seasons(series: VodItem): List<Season> = mapErrors {
        allPages { page -> client.seriesContents(VodSection.MOVIES, series.id, page = page) }
            .filter { it.isSeason }
            .map { Season(it.id, it.number, it.name) }
            .sortedBy { it.number ?: Int.MAX_VALUE }
    }

    override suspend fun episodes(series: VodItem, season: Season): List<Episode> = mapErrors {
        allPages { page -> client.seriesContents(VodSection.MOVIES, series.id, season.id, page = page) }
            .filter { it.isEpisode }
            .map { Episode(it.id, it.number, it.episodeTitle ?: it.name) }
            // Portals list the newest episode first.
            .sortedBy { it.number ?: Int.MAX_VALUE }
    }

    /**
     * Movies and episodes are played through their file, the way current portals expect; an
     * item's own command is the fallback for portals that list no files.
     */
    override suspend fun vodStreamUrl(item: VodPlayback): String = mapErrors {
        val files = client.seriesContents(
            VodSection.MOVIES,
            item.itemId,
            seasonId = item.seasonId ?: "0",
            episodeId = item.episodeId ?: "0",
        ).items.filter { it.isFile }
        val file = files.firstOrNull()
        val command = item.playbackRef
        when {
            file != null -> client.vodLink("/media/file_${file.id}.mpg")
            command != null -> client.vodLink(command)
            else -> throw SourceException(SourceException.Reason.BAD_RESPONSE)
        }
    }

    /** Every page of a list, up to a limit that keeps a broken portal from looping. */
    private suspend fun allPages(load: suspend (page: Int) -> Page<PortalVodItem>): List<PortalVodItem> {
        val first = load(1)
        val rest = (2..first.pageCount.coerceAtMost(MAX_PAGES)).flatMap { load(it).items }
        return first.items + rest
    }

    private fun PortalVodItem.toVod() = VodItem(
        id = id,
        title = name,
        description = description,
        posterUrl = posterUrl,
        year = year,
        rating = rating,
        genres = genres,
        director = director,
        actors = actors,
        durationMinutes = durationMinutes,
        isSeries = isSeries,
        adult = censored,
        playbackRef = cmd,
    )

    private fun Channel.toLive(bundledLogos: Map<String, String>) = LiveChannel(
        id = id,
        name = name,
        number = number,
        logoUrl = logoUrl ?: bundledLogos[LogoPack.key(name)],
        categoryId = genreId,
        adult = censored,
        playbackRef = cmd,
    )
}

private const val MILLIS = 1000L
private const val MAX_PAGES = 30

/** About 16 hours on a typical portal. */
private const val GUIDE_SIZE = 30

/** Translates portal client errors into [SourceException]; plain network errors pass through. */
internal inline fun <T> mapErrors(block: () -> T): T = try {
    block()
} catch (e: StalkerException) {
    throw e.toSourceException()
}

internal fun StalkerException.toSourceException(): SourceException = when (this) {
    is InvalidPortalUrlException -> SourceException(SourceException.Reason.INVALID_ADDRESS, cause = this)
    is PortalNotFoundException -> SourceException(SourceException.Reason.NOT_FOUND, cause = this)
    is AccountBlockedException -> SourceException(SourceException.Reason.DEVICE_REFUSED, portalMessage, this)
    is AuthorizationException -> SourceException(SourceException.Reason.AUTHORIZATION, cause = this)
    is ProtocolException -> SourceException(SourceException.Reason.BAD_RESPONSE, cause = this)
}
