package io.github.gobinathm.portalistv.core.stalker

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val PORTAL_VERSION = "5.6.1"
private const val API_SIGNATURE = "262"
private const val MILLIS_PER_SECOND = 1000L

/**
 * Query parameters of `get_profile`, which tells the portal what device is connecting.
 * Device ids are sent only when the profile enables them.
 */
internal fun profileParams(config: StalkerConfig, random: String?, nowMillis: Long): List<Pair<String, String>> {
    val identity = config.identity
    val version = "ImageDescription: ${identity.imageDescription}; ImageDate: ${identity.imageDate}; " +
        "PORTAL version: $PORTAL_VERSION; API Version: JS API version: ${identity.jsApiVersion}; " +
        "STB API version: ${identity.stbApiVersion}; Player Engine version: ${identity.playerEngineVersion}"
    val metrics = buildJsonObject {
        put("mac", config.mac)
        put("sn", config.serial)
        put("model", identity.stbModel)
        put("type", "STB")
        put("uid", config.deviceIds?.deviceId.orEmpty())
        put("random", random.orEmpty())
    }
    return buildList {
        add("type" to "stb")
        add("action" to "get_profile")
        add("hd" to "1")
        add("ver" to version)
        add("num_banks" to "2")
        add("sn" to config.serial)
        add("stb_type" to identity.stbModel)
        add("client_type" to "STB")
        add("image_version" to identity.imageVersion)
        add("video_out" to "hdmi")
        config.deviceIds?.let {
            add("device_id" to it.deviceId)
            add("device_id2" to it.deviceId2)
            add("signature" to it.signature)
        }
        add("auth_second_step" to "1")
        identity.hardwareVersion?.let { add("hw_version" to it) }
        add("not_valid_token" to "0")
        add("metrics" to metrics.toString())
        add("timestamp" to (nowMillis / MILLIS_PER_SECOND).toString())
        add("api_signature" to API_SIGNATURE)
    }
}
