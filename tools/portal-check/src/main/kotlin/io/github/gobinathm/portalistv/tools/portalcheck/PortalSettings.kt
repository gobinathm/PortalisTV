package io.github.gobinathm.portalistv.tools.portalcheck

import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.catalog.resolve
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.stalker.DeviceIds
import io.github.gobinathm.portalistv.core.stalker.StalkerConfig
import java.io.File
import java.util.Properties
import java.util.TimeZone
import io.github.gobinathm.portalistv.core.stalker.DeviceIdentity as ClientIdentity

/**
 * The client settings in [file] (see portal-check.properties.example): the portal and the device
 * identity to present, with the catalog's defaults for what isn't set.
 */
fun loadPortalSettings(file: File): StalkerConfig {
    val settings = Properties().apply { file.inputStream().use(::load) }
    fun value(key: String) = settings.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
    val url = requireNotNull(value("url")) { "url is required" }
    val mac = requireNotNull(value("mac")) { "mac is required" }.uppercase()
    val catalog = DeviceCatalog.load()
    val model = value("model") ?: catalog.defaultModel
    val profile = Profile(
        uuid = "portal-check",
        name = "portal-check",
        createdAtMillis = 0,
        portal = Portal(url = Secret(url)),
        device = DeviceIdentity(stbModel = model, mac = Secret(mac)),
    ).let { it.copy(firmware = it.firmware.copy(presetId = catalog.model(model)?.defaultFirmwareId)) }
    val reported = catalog.resolve(profile)
    val ids = value("deviceId")?.let { DeviceIds(it, value("deviceId2").orEmpty(), value("signature").orEmpty()) }
    return StalkerConfig(
        portalUrl = url,
        allowInsecureHttp = value("allowInsecureHttp").toBoolean(),
        mac = mac,
        serial = value("serial") ?: DEFAULT_SERIAL,
        language = value("language") ?: "en",
        timezone = value("timezone") ?: TimeZone.getDefault().id,
        identity = ClientIdentity(
            stbModel = reported.stbModel,
            userAgent = reported.userAgent,
            imageVersion = reported.imageVersion,
            imageDate = reported.imageDate,
            imageDescription = reported.imageDescription,
            jsApiVersion = reported.jsApiVersion,
            stbApiVersion = reported.stbApiVersion,
            playerEngineVersion = reported.playerEngineVersion,
            hardwareVersion = null,
        ),
        deviceIds = ids,
    )
}

private const val DEFAULT_SERIAL = "062014N012345"
