package io.github.gobinathm.portalistv.core.data.source

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.model.DeviceIds
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Proxy
import io.github.gobinathm.portalistv.core.model.Secret
import io.github.gobinathm.portalistv.core.model.TestProfiles
import io.github.gobinathm.portalistv.core.network.ProxyConfig
import org.junit.Test

class PortalConfigTest {
    private val config = PortalConfig(DeviceCatalog.load(), deviceTimeZone = { "Europe/Berlin" })

    private val seeded = TestProfiles.minimal().copy(
        deviceIds = DeviceIds(sendDeviceId = true, deviceIdSeed = Secret("seed-1")),
    )

    private fun Profile.withIds(transform: (DeviceIds) -> DeviceIds) = copy(deviceIds = transform(deviceIds))

    @Test
    fun `resolves the identity from the catalog`() {
        val stalker = config.stalkerConfig(TestProfiles.minimal())
        assertThat(stalker.identity.stbModel).isEqualTo("MAG250")
        assertThat(stalker.identity.imageDescription).isEqualTo("0.2.18-r19-pub-250")
        assertThat(stalker.identity.jsApiVersion).isEqualTo("343")
        assertThat(stalker.identity.userAgent).contains("stbapp")
        assertThat(stalker.mac).isEqualTo("00:1A:79:00:00:01")
    }

    @Test
    fun `custom firmware fields are sent`() {
        val custom = TestProfiles.minimal().copy(firmware = Firmware(presetId = null, jsApiVersion = "350"))
        assertThat(config.stalkerConfig(custom).identity.jsApiVersion).isEqualTo("350")
    }

    @Test
    fun `uses the device time zone unless the profile sets one`() {
        assertThat(config.stalkerConfig(TestProfiles.minimal()).timezone).isEqualTo("Europe/Berlin")
        assertThat(config.stalkerConfig(TestProfiles.full()).timezone).isEqualTo("Europe/Berlin")
        val tokyo = TestProfiles.minimal().let { it.copy(portal = it.portal.copy(timezone = "Asia/Tokyo")) }
        assertThat(config.stalkerConfig(tokyo).timezone).isEqualTo("Asia/Tokyo")
    }

    @Test
    fun `sends no device ids unless enabled`() {
        assertThat(config.stalkerConfig(TestProfiles.minimal()).deviceIds).isNull()
    }

    @Test
    fun `derives stable, distinct device ids from the seed`() {
        val ids = config.stalkerConfig(seeded).deviceIds!!
        assertThat(ids.deviceId).matches("[0-9A-F]{64}")
        assertThat(ids.deviceId2).matches("[0-9A-F]{64}")
        assertThat(setOf(ids.deviceId, ids.deviceId2, ids.signature)).hasSize(3)
        assertThat(config.stalkerConfig(seeded).deviceIds).isEqualTo(ids)

        val otherSeed = seeded.withIds { it.copy(deviceIdSeed = Secret("seed-2")) }
        assertThat(config.stalkerConfig(otherSeed).deviceIds!!.deviceId).isNotEqualTo(ids.deviceId)
    }

    @Test
    fun `mac based ids follow the mac address, not the seed`() {
        val macBased = seeded.withIds { it.copy(macBasedDeviceId = true) }
        val reseeded = macBased.withIds { it.copy(deviceIdSeed = Secret("other")) }
        assertThat(config.stalkerConfig(reseeded).deviceIds).isEqualTo(config.stalkerConfig(macBased).deviceIds)
    }

    @Test
    fun `entered ids win over derived ones`() {
        val entered = seeded.withIds { it.copy(deviceId = Secret("MINE"), signature = Secret("SIG")) }
        val ids = config.stalkerConfig(entered).deviceIds!!
        assertThat(ids.deviceId).isEqualTo("MINE")
        assertThat(ids.signature).isEqualTo("SIG")
        assertThat(ids.deviceId2).matches("[0-9A-F]{64}")
    }

    @Test
    fun `derives a serial in set-top box format unless one is entered`() {
        assertThat(config.stalkerConfig(seeded).serial).matches("062014N0[0-9]{5}")
        assertThat(config.stalkerConfig(TestProfiles.full()).serial).isEqualTo("SN0000000002")
    }

    @Test
    fun `maps an enabled proxy only`() {
        assertThat(config.proxy(TestProfiles.minimal())).isNull()
        assertThat(config.proxy(TestProfiles.full())).isEqualTo(ProxyConfig("proxy.invalid", 3128, "pu", "pp"))
        val noPort = TestProfiles.minimal().copy(proxy = Proxy(enabled = true, host = "p.invalid", port = null))
        assertThat(config.proxy(noPort)).isNull()
    }
}
