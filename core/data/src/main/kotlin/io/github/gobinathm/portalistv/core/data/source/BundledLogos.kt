package io.github.gobinathm.portalistv.core.data.source

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gobinathm.portalistv.core.data.di.IoDispatcher
import io.github.gobinathm.portalistv.core.model.LogoPack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The logos packed into this build ([LogoPack]); empty unless whoever built the app made a pack.
 * They load from the app itself, so using them contacts nothing.
 */
@Singleton
class BundledLogos @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var index: Map<String, String>? = null

    /** Image addresses by [LogoPack.key] of the channel name. */
    suspend fun index(): Map<String, String> = index ?: mutex.withLock {
        index ?: withContext(io) { read() }.also { index = it }
    }

    private fun read(): Map<String, String> {
        val properties = try {
            context.assets.open(LogoPack.INDEX).use { stream -> Properties().apply { load(stream) } }
        } catch (_: FileNotFoundException) {
            return emptyMap()
        }
        return properties.stringPropertyNames().associateWith { key ->
            "$ASSET_URI${LogoPack.DIR}/${properties.getProperty(key)}"
        }
    }

    private companion object {
        const val ASSET_URI = "file:///android_asset/"
    }
}
