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
    val showAdultContent: Boolean = false,
    val startProfileUuid: String? = null,
    val theme: String = AppTheme.DARK.name,
    val accent: String = AccentColor.AZURE.name,
    /** Row names in order; unknown names are dropped and missing rows added on reading. */
    val homeRowOrder: List<String> = emptyList(),
    val hiddenHomeRows: Set<String> = emptySet(),
    /** Key code to action name; null until the user changes them, meaning the defaults. */
    val keyBindings: Map<String, String>? = null,
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
    showAdultContent = showAdultContent,
    startProfileUuid = startProfileUuid,
    theme = enumOrDefault(theme, AppTheme.DARK),
    accent = enumOrDefault(accent, AccentColor.AZURE),
    homeRows = normalizedHomeRows(
        homeRowOrder.mapNotNull { name -> HomeRow.entries.firstOrNull { it.name == name } }
            .map { HomeRowSetting(it, visible = it.name !in hiddenHomeRows) },
    ),
    keyBindings = keyBindings?.let(::readKeyBindings) ?: RemoteKeys.DEFAULT,
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
    showAdultContent = showAdultContent,
    startProfileUuid = startProfileUuid,
    theme = theme.name,
    accent = accent.name,
    homeRowOrder = homeRows.map { it.row.name },
    hiddenHomeRows = homeRows.filterNot { it.visible }.mapTo(HashSet()) { it.row.name },
    keyBindings = keyBindings.takeIf { it != RemoteKeys.DEFAULT }
        ?.map { (code, action) -> code.toString() to action.name }
        ?.toMap(),
)

/** Drops entries with an unknown action or a button that can't be mapped. */
private fun readKeyBindings(stored: Map<String, String>): Map<Int, PlayerAction> = stored.mapNotNull { (code, name) ->
    val keyCode = code.toIntOrNull()?.takeIf(RemoteKeys::isMappable)
    val action = PlayerAction.entries.firstOrNull { it.name == name }
    if (keyCode != null && action != null) keyCode to action else null
}.toMap()

private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

/** ISO 639-1 or 639-2 codes only; the value is passed to libVLC as an option. */
private fun isLanguageCode(code: String) = LANGUAGE_CODE.matches(code)

private val LANGUAGE_CODE = Regex("[a-z]{2,3}")
