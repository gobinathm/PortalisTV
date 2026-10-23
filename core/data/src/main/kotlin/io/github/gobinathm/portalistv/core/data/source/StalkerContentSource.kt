package io.github.gobinathm.portalistv.core.data.source

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.LiveCategory
import io.github.gobinathm.portalistv.core.model.LiveChannel
import io.github.gobinathm.portalistv.core.model.PagedResult
import io.github.gobinathm.portalistv.core.model.SourceAccount
import io.github.gobinathm.portalistv.core.stalker.Channel
import io.github.gobinathm.portalistv.core.stalker.StalkerClient

/** [ContentSource] for Stalker/Ministra portals. */
internal class StalkerContentSource(private val client: StalkerClient) : ContentSource {
    override suspend fun connect(): SourceAccount =
        client.connect().let { SourceAccount(displayName = it.name, message = it.message) }

    override suspend fun liveCategories(): List<LiveCategory> =
        client.genres().map { LiveCategory(id = it.id, title = it.title, adult = it.censored) }

    override suspend fun liveChannels(categoryId: String, page: Int): PagedResult<LiveChannel> {
        val result = client.channels(categoryId, page)
        return PagedResult(result.items.map { it.toLive() }, result.totalItems, result.pageCount)
    }

    override suspend fun streamUrl(channel: LiveChannel): String = client.createLink(channel.playbackRef)

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
