package io.github.gobinathm.portalistv.core.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/**
 * Set-top box models, firmware images and user agents a profile can present to a portal.
 *
 * The data lives in `catalog/devices.json` so changes are reviewed as data, not code. Ids are
 * stored in profiles: never rename or remove one, only add.
 */
@Serializable
data class DeviceCatalog(
    val version: Int,
    val defaultModel: String,
    val apiVersions: ApiVersions,
    val userAgents: List<UserAgentPreset>,
    val displayResolutions: List<String>,
    val firmware: List<FirmwareImage>,
    val models: List<StbModel>,
) {
    @Transient
    private val modelsById = models.associateBy { it.id }

    @Transient
    private val firmwareById = firmware.associateBy { it.id }

    fun model(id: String): StbModel? = modelsById[id]

    fun firmware(id: String): FirmwareImage? = firmwareById[id]

    /** The firmware images available for [model], oldest first. */
    fun firmwareFor(model: StbModel): List<FirmwareImage> = model.firmware.map { firmwareById.getValue(it) }

    fun defaultFirmware(model: StbModel): FirmwareImage = firmwareById.getValue(model.defaultFirmwareId)

    /** @throws IllegalStateException if ids are duplicated or references are dangling. */
    fun validate() {
        check(models.map { it.id }.toSet().size == models.size) { "Duplicate model id" }
        check(firmware.map { it.id }.toSet().size == firmware.size) { "Duplicate firmware id" }
        check(userAgents.map { it.id }.toSet().size == userAgents.size) { "Duplicate user agent id" }
        check(userAgents.isNotEmpty()) { "No user agents" }
        check(defaultModel in modelsById) { "Unknown default model $defaultModel" }
        for (model in models) {
            check(model.firmware.isNotEmpty()) { "${model.id} has no firmware" }
            model.firmware.forEach { check(it in firmwareById) { "${model.id} references unknown firmware $it" } }
            check(model.defaultFirmwareId in model.firmware) { "${model.id} default firmware is not in its list" }
        }
    }

    companion object {
        private const val RESOURCE = "/catalog/devices.json"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): DeviceCatalog = json.decodeFromString(serializer(), text).also { it.validate() }

        /** Loads the catalog bundled with the app. */
        fun load(): DeviceCatalog {
            val stream = checkNotNull(DeviceCatalog::class.java.getResourceAsStream(RESOURCE)) { "$RESOURCE missing" }
            return parse(stream.bufferedReader().use { it.readText() })
        }
    }
}

/** API versions reported to the portal unless a custom firmware overrides them. */
@Serializable
data class ApiVersions(
    val js: String,
    val stb: String,
    val playerEngine: String,
)

@Serializable
data class UserAgentPreset(
    val id: String,
    val name: String,
    val value: String,
)

/** A firmware image. Its [id] is also the image description reported to the portal. */
@Serializable
data class FirmwareImage(
    val id: String,
    val imageVersion: String,
    val imageDate: String,
) {
    val imageDescription: String get() = id
}

/** A set-top box model. Its [id] is the model name reported to the portal. */
@Serializable
data class StbModel(
    val id: String,
    val firmware: List<String>,
    /** Defaults to the only firmware when a model has just one. */
    private val defaultFirmware: String? = null,
) {
    val defaultFirmwareId: String get() = defaultFirmware ?: firmware.last()
}
