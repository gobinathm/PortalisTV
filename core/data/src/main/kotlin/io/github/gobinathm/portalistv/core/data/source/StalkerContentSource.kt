package io.github.gobinathm.portalistv.core.data.source

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PagedResult
import io.github.gobinathm.portalistv.core.model.Programme
import io.github.gobinathm.portalistv.core.model.SourceAccount
import io.github.gobinathm.portalistv.core.model.SourceException
import io.github.gobinathm.portalistv.core.stalker.AccountBlockedException
import io.github.gobinathm.portalistv.core.stalker.AuthorizationException
import io.github.gobinathm.portalistv.core.stalker.Channel
import io.github.gobinathm.portalistv.core.stalker.InvalidPortalUrlException
import io.github.gobinathm.portalistv.core.stalker.PortalNotFoundException
import io.github.gobinathm.portalistv.core.stalker.ProtocolException
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import io.github.gobinathm.portalistv.core.stalker.StalkerException

/** [ContentSource] for Stalker/Ministra portals. */
internal class StalkerContentSource(
    private val client: StalkerClient,
    override val playbackUserAgent: String?,
) : ContentSource {
    override suspend fun connect(): SourceAccount = mapErrors {
        client.connect().let { SourceAccount(displayName = it.name, message = it.message) }
    }

    override suspend fun liveCategories(): List<LiveCategory> = mapErrors {
        client.genres().map { LiveCategory(id = it.id, title = it.title, adult = it.censored) }
    }

    override suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel> = mapErrors {
        val result = client.channels(categoryId, page)
        PagedResult(result.items.map { it.toLive() }, result.totalItems, result.pageCount)
    }

    override suspend fun nowNext(channel: LiveChannel): List<Programme> = mapErrors {
        client.shortEpg(channel.id).map {
            Programme(it.title, it.description, it.startSeconds * MILLIS, it.stopSeconds * MILLIS)
        }
    }

    override suspend fun streamUrl(channel: LiveChannel): String = mapErrors { client.createLink(channel.playbackRef) }

    private fun Channel.toLive() = LiveChannel(
        id = id,
        name = name,
        number = number,
        logoUrl = logoUrl,
        categoryId = genreId,
        adult = censored,
        playbackRef = cmd,
    )
}

private const val MILLIS = 1000L

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
