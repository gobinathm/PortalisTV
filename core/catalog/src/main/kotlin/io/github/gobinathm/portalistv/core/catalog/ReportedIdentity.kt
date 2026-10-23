package io.github.gobinathm.portalistv.core.catalog

import io.github.gobinathm.portalistv.core.model.Profile

/** The device identity strings a profile presents to its portal, after presets are applied. */
data class ReportedIdentity(
    val stbModel: String,
    val imageVersion: String,
    val imageDate: String,
    val imageDescription: String,
    val jsApiVersion: String,
    val stbApiVersion: String,
    val playerEngineVersion: String,
    val userAgent: String,
    val displayResolution: String,
)

/**
 * Resolves what [profile] reports to the portal.
 *
 * - A firmware preset supplies the image fields; API versions come from the catalog.
 * - A custom firmware (no preset) sends each field as entered, falling back to the model's
 *   default firmware and the catalog's API versions for fields left empty.
 * - An id the catalog no longer knows falls back to the defaults instead of failing, so an old
 *   profile keeps working; the stored model name is still reported as is.
 */
fun DeviceCatalog.resolve(profile: Profile): ReportedIdentity {
    val device = profile.device
    val custom = profile.firmware
    val model = model(device.stbModel) ?: requireNotNull(model(defaultModel))
    val preset = custom.presetId?.let(::firmware)?.takeIf { it.id in model.firmware }
    val base = preset ?: defaultFirmware(model)
    val isCustom = custom.presetId == null

    fun customOr(value: String?, fallback: String) = value?.takeIf { isCustom && it.isNotBlank() } ?: fallback

    return ReportedIdentity(
        stbModel = device.stbModel,
        imageVersion = customOr(custom.imageVersion, base.imageVersion),
        imageDate = customOr(custom.imageDate, base.imageDate),
        imageDescription = customOr(custom.imageDescription, base.imageDescription),
        jsApiVersion = customOr(custom.jsApiVersion, apiVersions.js),
        stbApiVersion = customOr(custom.stbApiVersion, apiVersions.stb),
        playerEngineVersion = customOr(custom.playerEngineVersion, apiVersions.playerEngine),
        userAgent = device.userAgent?.takeIf { it.isNotBlank() } ?: userAgents.first().value,
        displayResolution = device.displayResolution,
    )
}
