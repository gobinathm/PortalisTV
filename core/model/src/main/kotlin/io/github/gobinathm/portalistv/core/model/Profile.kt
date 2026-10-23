package io.github.gobinathm.portalistv.core.model

/** One portal the user connects to, with the device identity presented to it. */
data class Profile(
    /** Database row id; 0 for a profile that has not been saved yet. */
    val id: Long = 0,
    /** Stable identifier that survives export and import. */
    val uuid: String,
    val name: String,
    val sortOrder: Int = 0,
    /** ARGB color for the profile avatar, or null for the theme default. */
    val color: Int? = null,
    val createdAtMillis: Long,
    val portal: Portal,
    val proxy: Proxy = Proxy(),
    val device: DeviceIdentity,
    val deviceIds: DeviceIds = DeviceIds(),
    val firmware: Firmware = Firmware(),
    val hardware: Hardware = Hardware(),
    val compatibility: Compatibility = Compatibility(),
    val playback: Playback = Playback(),
)

data class Portal(
    val url: Secret,
    val language: String = "en",
    /** IANA time zone sent to the portal, or null to use the device's. */
    val timezone: String? = null,
    /** Plain HTTP is refused unless the user opts in for this profile. */
    val allowInsecureHttp: Boolean = false,
    /** Only for portals that require a login on top of the MAC address. */
    val login: Secret? = null,
    val password: Secret? = null,
)

data class Proxy(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int? = null,
    val username: Secret? = null,
    val password: Secret? = null,
)

data class DeviceIdentity(
    /** Set-top box model id from the device catalog. */
    val stbModel: String,
    /** A custom user agent, or null to build it from the model and firmware. */
    val userAgent: String? = null,
    val displayResolution: String = "1920x1080",
    val mac: Secret,
    val serial: Secret? = null,
)

data class DeviceIds(
    val sendDeviceId: Boolean = false,
    val deviceId: Secret? = null,
    /** Random per-profile seed that device ids are derived from; never a hardware identifier. */
    val deviceIdSeed: Secret? = null,
    val macBasedDeviceId: Boolean = false,
    val deviceId2: Secret? = null,
    val signature: Secret? = null,
)

/**
 * Firmware reported to the portal. With a [presetId] the version fields come from the device
 * catalog; with none, the fields below are sent as entered.
 */
data class Firmware(
    val presetId: String? = null,
    val playerEngineVersion: String? = null,
    val jsApiVersion: String? = null,
    val stbApiVersion: String? = null,
    val imageVersion: String? = null,
    val imageDescription: String? = null,
    val imageDate: String? = null,
)

data class Hardware(
    val vendor: String? = null,
    val version: String? = null,
)

data class Compatibility(
    val ministraMode: Boolean = false,
    /** Lets the portal see that this is not a real set-top box. */
    val allowEmulatorDetection: Boolean = false,
)

data class Playback(
    val engine: PlayerEngine = PlayerEngine.VLC,
    val udpxyEnabled: Boolean = false,
    val udpxyUrl: String? = null,
    /** Forces the multicast protocol behind UDPXY, or null to keep the portal's. */
    val udpxyProtocol: MulticastProtocol? = null,
    val externalPlayerKeys: ExternalPlayerKeys = ExternalPlayerKeys(),
)

// Stored by name: renaming a constant needs a database migration.
enum class PlayerEngine { VLC, EXTERNAL }

enum class MulticastProtocol { UDP, RTP }

/** Remote keys that an external player passes back to the app. */
data class ExternalPlayerKeys(
    val back: Boolean = false,
    val exit: Boolean = false,
    val ok: Boolean = false,
)
