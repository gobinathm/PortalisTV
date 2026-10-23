package io.github.gobinathm.portalistv.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.gobinathm.portalistv.core.model.MulticastProtocol
import io.github.gobinathm.portalistv.core.model.PlayerEngine

// Secrets are stored as plain columns: the whole database file is encrypted by SQLCipher.
@Entity(tableName = "profiles", indices = [Index(value = ["uuid"], unique = true)])
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val uuid: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val color: Int?,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
    @Embedded(prefix = "portal_") val portal: PortalColumns,
    @Embedded(prefix = "proxy_") val proxy: ProxyColumns,
    @Embedded(prefix = "device_") val device: DeviceColumns,
    @Embedded(prefix = "device_ids_") val deviceIds: DeviceIdColumns,
    @Embedded(prefix = "firmware_") val firmware: FirmwareColumns,
    @Embedded(prefix = "hardware_") val hardware: HardwareColumns,
    @Embedded(prefix = "compat_") val compatibility: CompatibilityColumns,
    @Embedded(prefix = "playback_") val playback: PlaybackColumns,
)

data class PortalColumns(
    val url: String,
    val language: String,
    val timezone: String?,
    @ColumnInfo(name = "allow_insecure_http") val allowInsecureHttp: Boolean,
    val login: String?,
    val password: String?,
)

data class ProxyColumns(
    val enabled: Boolean,
    val host: String,
    val port: Int?,
    val username: String?,
    val password: String?,
)

data class DeviceColumns(
    @ColumnInfo(name = "stb_model") val stbModel: String,
    @ColumnInfo(name = "user_agent") val userAgent: String?,
    @ColumnInfo(name = "display_resolution") val displayResolution: String,
    val mac: String,
    val serial: String?,
)

data class DeviceIdColumns(
    @ColumnInfo(name = "send") val sendDeviceId: Boolean,
    @ColumnInfo(name = "id") val deviceId: String?,
    val seed: String?,
    @ColumnInfo(name = "mac_based") val macBasedDeviceId: Boolean,
    @ColumnInfo(name = "id2") val deviceId2: String?,
    val signature: String?,
)

data class FirmwareColumns(
    @ColumnInfo(name = "preset_id") val presetId: String?,
    @ColumnInfo(name = "player_engine_version") val playerEngineVersion: String?,
    @ColumnInfo(name = "js_api_version") val jsApiVersion: String?,
    @ColumnInfo(name = "stb_api_version") val stbApiVersion: String?,
    @ColumnInfo(name = "image_version") val imageVersion: String?,
    @ColumnInfo(name = "image_description") val imageDescription: String?,
    @ColumnInfo(name = "image_date") val imageDate: String?,
)

data class HardwareColumns(
    val vendor: String?,
    val version: String?,
)

data class CompatibilityColumns(
    @ColumnInfo(name = "ministra_mode") val ministraMode: Boolean,
    @ColumnInfo(name = "allow_emulator_detection") val allowEmulatorDetection: Boolean,
)

data class PlaybackColumns(
    val engine: PlayerEngine,
    @ColumnInfo(name = "udpxy_enabled") val udpxyEnabled: Boolean,
    @ColumnInfo(name = "udpxy_url") val udpxyUrl: String?,
    @ColumnInfo(name = "udpxy_protocol") val udpxyProtocol: MulticastProtocol?,
    @ColumnInfo(name = "external_key_back") val externalKeyBack: Boolean,
    @ColumnInfo(name = "external_key_exit") val externalKeyExit: Boolean,
    @ColumnInfo(name = "external_key_ok") val externalKeyOk: Boolean,
)
