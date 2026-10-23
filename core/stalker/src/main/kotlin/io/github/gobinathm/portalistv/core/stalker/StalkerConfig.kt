package io.github.gobinathm.portalistv.core.stalker

/**
 * Everything the client presents to a portal, already resolved from the profile and the device
 * catalog. Values are sent as given; this module makes no policy decisions about them.
 */
data class StalkerConfig(
    /** The address the user entered, usually ending in `/c/`. */
    val portalUrl: String,
    val allowInsecureHttp: Boolean,
    val mac: String,
    val serial: String,
    val language: String,
    /** IANA time zone id. */
    val timezone: String,
    val identity: DeviceIdentity,
    val deviceIds: DeviceIds?,
    val login: String? = null,
    val password: String? = null,
)

data class DeviceIdentity(
    val stbModel: String,
    val userAgent: String,
    val imageVersion: String,
    val imageDate: String,
    val imageDescription: String,
    val jsApiVersion: String,
    val stbApiVersion: String,
    val playerEngineVersion: String,
    val hardwareVersion: String?,
)

/** Sent only when the profile enables "send device ID". */
data class DeviceIds(
    val deviceId: String,
    val deviceId2: String,
    val signature: String,
)
