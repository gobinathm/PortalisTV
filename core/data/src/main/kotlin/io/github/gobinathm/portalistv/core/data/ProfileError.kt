package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import java.net.URI
import java.net.URISyntaxException

enum class ProfileError {
    NAME_REQUIRED,
    URL_REQUIRED,
    URL_INVALID,

    /** Plain HTTP without the per-profile opt-in. */
    URL_INSECURE,
    MAC_INVALID,
    PROXY_HOST_REQUIRED,
    PROXY_PORT_INVALID,
    UDPXY_URL_INVALID,
}

private val macPattern = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")
private const val MAX_PORT = 65_535

/** Normalizes user input: trims text fields and upper-cases the MAC address. */
fun Profile.normalized(): Profile = copy(
    name = name.trim(),
    portal = portal.copy(url = Secret(portal.url.value.trim())),
    proxy = proxy.copy(host = proxy.host.trim()),
    device = device.copy(mac = Secret(device.mac.value.trim().uppercase())),
    playback = playback.copy(udpxyUrl = playback.udpxyUrl?.trim()?.ifEmpty { null }),
)

/** Checks a normalized profile; an empty set means it can be saved. */
fun Profile.validate(): Set<ProfileError> = buildSet {
    if (name.isBlank()) add(ProfileError.NAME_REQUIRED)
    addAll(validatePortalUrl(portal.url.value, portal.allowInsecureHttp))
    if (!macPattern.matches(device.mac.value)) add(ProfileError.MAC_INVALID)
    if (proxy.enabled) {
        if (proxy.host.isBlank()) add(ProfileError.PROXY_HOST_REQUIRED)
        if (proxy.port == null || proxy.port !in 1..MAX_PORT) add(ProfileError.PROXY_PORT_INVALID)
    }
    if (playback.udpxyEnabled && parseHttpUrl(playback.udpxyUrl.orEmpty()) == null) {
        add(ProfileError.UDPXY_URL_INVALID)
    }
}

/** The portal is reached over plain HTTP, so traffic, including the MAC and token, can be read on the network. */
val Profile.usesPlainHttp: Boolean
    get() = parseHttpUrl(portal.url.value.trim())?.scheme.equals("http", ignoreCase = true)

private fun validatePortalUrl(url: String, allowInsecureHttp: Boolean): Set<ProfileError> {
    val uri = parseHttpUrl(url)
    return when {
        url.isBlank() -> setOf(ProfileError.URL_REQUIRED)
        uri == null -> setOf(ProfileError.URL_INVALID)
        uri.scheme.equals("http", ignoreCase = true) && !allowInsecureHttp -> setOf(ProfileError.URL_INSECURE)
        else -> emptySet()
    }
}

private fun parseHttpUrl(url: String): URI? = try {
    URI(url).takeIf { it.scheme?.lowercase() in setOf("http", "https") && !it.host.isNullOrEmpty() }
} catch (_: URISyntaxException) {
    null
}
