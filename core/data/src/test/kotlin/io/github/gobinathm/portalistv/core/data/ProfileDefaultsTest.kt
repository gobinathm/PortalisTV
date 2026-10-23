package io.github.gobinathm.portalistv.core.data

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import org.junit.Test

class ProfileDefaultsTest {
    private val catalog = DeviceCatalog.load()
    private val defaults = ProfileDefaults(catalog, clock = { 42L })

    @Test
    fun `new profiles use the catalog defaults`() {
        val profile = defaults.newProfile()
        assertThat(profile.id).isEqualTo(0)
        assertThat(profile.createdAtMillis).isEqualTo(42L)
        assertThat(profile.device.stbModel).isEqualTo("MAG250")
        assertThat(profile.firmware.presetId).isEqualTo("0.2.18-r19-pub-250")
        assertThat(profile.deviceIds.deviceIdSeed?.value).matches("[0-9a-f]{32}")
    }

    @Test
    fun `new profiles get distinct identifiers`() {
        val first = defaults.newProfile()
        val second = defaults.newProfile()
        assertThat(first.uuid).isNotEqualTo(second.uuid)
        assertThat(first.deviceIds.deviceIdSeed).isNotEqualTo(second.deviceIds.deviceIdSeed)
    }

    @Test
    fun `random mac addresses use the expected prefix and format`() {
        repeat(20) {
            assertThat(defaults.randomMac()).matches("00:1A:79(:[0-9A-F]{2}){3}")
        }
    }
}
