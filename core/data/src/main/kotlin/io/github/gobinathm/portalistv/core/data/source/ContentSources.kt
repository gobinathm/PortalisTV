package io.github.gobinathm.portalistv.core.data.source

import io.github.gobinathm.portalistv.core.model.ContentSource
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.network.HttpClients
import io.github.gobinathm.portalistv.core.stalker.StalkerClient
import javax.inject.Inject
import javax.inject.Singleton

/** Creates the [ContentSource] for a profile, and keeps one per profile so sessions are reused. */
@Singleton
class ContentSources @Inject constructor(
    private val httpClients: HttpClients,
    private val portalConfig: PortalConfig,
) {
    private val sources = mutableMapOf<Long, Pair<Profile, ContentSource>>()

    /**
     * @throws io.github.gobinathm.portalistv.core.stalker.InvalidPortalUrlException if the
     * profile's portal URL cannot be used.
     */
    @Synchronized
    fun forProfile(profile: Profile): ContentSource {
        // An edited profile gets a fresh session with its new settings.
        sources[profile.id]?.let { (cached, source) -> if (cached == profile) return source }
        val http = httpClients.forProfile(portalConfig.proxy(profile))
        val client = StalkerClient(http, portalConfig.stalkerConfig(profile))
        return StalkerContentSource(client).also { sources[profile.id] = profile to it }
    }
}
