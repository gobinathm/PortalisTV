package io.github.gobinathm.portalistv.core.datastore

import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.Deinterlace
import io.github.gobinathm.portalistv.core.player.HardwareDecoding
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import kotlinx.serialization.Serializable

/**
 * The stored form of [AppSettings]. Enums are kept by name and every field has a default, so a
 * file written by another app version still loads; unknown or invalid values fall back to defaults.
 */
@Serializable
internal data class StoredSettings(
    val networkCachingMs: Int = PlayerSettings.DEFAULT_NETWORK_CACHING_MS,
    val hardwareDecoding: String = HardwareDecoding.AUTO.name,
    val deinterlace: String = Deinterlace.AUTO.name,
    val defaultAspect: String = AspectMode.BEST_FIT.name,
    val audioTimeStretch: Boolean = true,
    val preferredAudioLanguage: String? = null,
    val preferredSubtitleLanguage: String? = null,
    val pictureInPicture: Boolean = true,
)

internal fun StoredSettings.toAppSettings() = AppSettings(
    player = PlayerSettings(
        networkCachingMs = networkCachingMs.coerceIn(PlayerSettings.NETWORK_CACHING_RANGE_MS),
        hardwareDecoding = enumOrDefault(hardwareDecoding, HardwareDecoding.AUTO),
        deinterlace = enumOrDefault(deinterlace, Deinterlace.AUTO),
        defaultAspect = enumOrDefault(defaultAspect, AspectMode.BEST_FIT),
        audioTimeStretch = audioTimeStretch,
        preferredAudioLanguage = preferredAudioLanguage?.takeIf(::isLanguageCode),
        preferredSubtitleLanguage = preferredSubtitleLanguage?.takeIf(::isLanguageCode),
    ),
    pictureInPicture = pictureInPicture,
)

internal fun AppSettings.toStored() = StoredSettings(
    networkCachingMs = player.networkCachingMs,
    hardwareDecoding = player.hardwareDecoding.name,
    deinterlace = player.deinterlace.name,
    defaultAspect = player.defaultAspect.name,
    audioTimeStretch = player.audioTimeStretch,
    preferredAudioLanguage = player.preferredAudioLanguage,
    preferredSubtitleLanguage = player.preferredSubtitleLanguage,
    pictureInPicture = pictureInPicture,
)

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/** ISO 639-1 or 639-2 codes only; the value is passed to libVLC as an option. */
private fun isLanguageCode(code: String) = LANGUAGE_CODE.matches(code)

private val LANGUAGE_CODE = Regex("[a-z]{2,3}")
