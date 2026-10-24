package io.github.gobinathm.portalistv.feature.vod

import io.github.gobinathm.portalistv.core.model.VodItem
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Items seen in on-demand lists, so the detail page can show one without asking the portal again
 * (portals have no call for a single item). Kept in memory only.
 */
@Singleton
class VodItems @Inject constructor() {
    private val items = ConcurrentHashMap<String, VodItem>()

    fun remember(list: List<VodItem>) = list.forEach { items[it.id] = it }

    operator fun get(id: String): VodItem? = items[id]
}
