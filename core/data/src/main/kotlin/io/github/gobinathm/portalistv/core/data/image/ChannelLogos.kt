package io.github.gobinathm.portalistv.core.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gobinathm.portalistv.core.data.source.PortalConfig
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.network.HttpClients
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Image loaders for channel logos. Logos come from the portal, so each profile's loader uses that
 * profile's proxy and honors its plain-HTTP opt-in, like every other portal request. The caches
 * are shared between profiles.
 */
@Singleton
class ChannelLogos @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val httpClients: HttpClients,
    private val portalConfig: PortalConfig,
) {
    private val memoryCache by lazy {
        MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build()
    }
    private val diskCache by lazy {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("logos").toOkioPath())
            .maxSizeBytes(DISK_CACHE_BYTES)
            .build()
    }
    private val loaders = mutableMapOf<Long, Pair<Profile, ImageLoader>>()

    @Synchronized
    fun loader(profile: Profile): ImageLoader {
        loaders[profile.id]?.let { (cached, loader) -> if (cached == profile) return loader }
        val client = imageClient(profile)
        val loader = ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .memoryCache { memoryCache }
            .diskCache { diskCache }
            .build()
        return loader.also { loaders[profile.id] = profile to it }
    }

    private fun imageClient(profile: Profile): OkHttpClient {
        val allowHttp = profile.portal.allowInsecureHttp
        return httpClients.forProfile(portalConfig.proxy(profile)).newBuilder()
            .addInterceptor { chain ->
                if (!allowHttp && !chain.request().url.isHttps) throw IOException("Plain HTTP is not allowed")
                chain.proceed(chain.request())
            }
            .build()
    }

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.1
        const val DISK_CACHE_BYTES = 32L * 1024 * 1024
    }
}
