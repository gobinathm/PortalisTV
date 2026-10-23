package io.github.gobinathm.portalistv.core.catalog

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.model.DeviceIdentity
import io.github.gobinathm.portalistv.core.model.Firmware
import io.github.gobinathm.portalistv.core.model.Portal
import io.github.gobinathm.portalistv.core.model.Profile
import io.github.gobinathm.portalistv.core.model.Secret
import org.junit.Test

class ReportedIdentityTest {
    private val catalog = DeviceCatalog.load()

    private fun profile(model: String = "MAG250", firmware: Firmware = Firmware(), userAgent: String? = null) =
        Profile(
            uuid = "u",
            name = "p",
            createdAtMillis = 0,
            portal = Portal(url = Secret("https://portal.invalid/c/")),
            device = DeviceIdentity(stbModel = model, userAgent = userAgent, mac = Secret("00:1A:79:00:00:01")),
            firmware = firmware,
        )

    @Test
    fun `a preset reports its image and the catalog API versions`() {
        val identity = catalog.resolve(profile(firmware = Firmware(presetId = "0.2.18-r14-pub-250")))
        assertThat(identity.stbModel).isEqualTo("MAG250")
        assertThat(identity.imageDescription).isEqualTo("0.2.18-r14-pub-250")
        assertThat(identity.imageVersion).isEqualTo("218")
        assertThat(identity.jsApiVersion).isEqualTo("343")
        assertThat(identity.stbApiVersion).isEqualTo("146")
        assertThat(identity.playerEngineVersion).isEqualTo("0x588")
        assertThat(identity.userAgent).contains("MAG200 stbapp ver: 2 rev: 250")
    }

    @Test
    fun `a preset ignores leftover custom fields`() {
        val firmware = Firmware(presetId = "0.2.18-r14-pub-250", imageDescription = "custom", jsApiVersion = "1")
        val identity = catalog.resolve(profile(firmware = firmware))
        assertThat(identity.imageDescription).isEqualTo("0.2.18-r14-pub-250")
        assertThat(identity.jsApiVersion).isEqualTo("343")
    }

    @Test
    fun `custom firmware sends entered fields and falls back for empty ones`() {
        val firmware = Firmware(
            presetId = null,
            imageDescription = "my-image",
            jsApiVersion = "350",
            stbApiVersion = " ",
        )
        val identity = catalog.resolve(profile(model = "MAG424", firmware = firmware))
        assertThat(identity.imageDescription).isEqualTo("my-image")
        assertThat(identity.jsApiVersion).isEqualTo("350")
        assertThat(identity.stbApiVersion).isEqualTo("146")
        // Empty image fields come from the model's default firmware.
        assertThat(identity.imageDate).isEqualTo("Fri May 8 15:39:55 UTC 2020")
    }

    @Test
    fun `a preset from another model falls back to the model default`() {
        val identity = catalog.resolve(profile(model = "MAG424", firmware = Firmware(presetId = "0.2.18-r14-pub-250")))
        assertThat(identity.imageDescription).isEqualTo("2.20.02-pub-424")
    }

    @Test
    fun `an unknown model keeps its name and uses the default model's firmware`() {
        val identity = catalog.resolve(profile(model = "MAG999", firmware = Firmware(presetId = "gone")))
        assertThat(identity.stbModel).isEqualTo("MAG999")
        assertThat(identity.imageDescription).isEqualTo("0.2.18-r19-pub-250")
    }

    @Test
    fun `a custom user agent wins`() {
        assertThat(catalog.resolve(profile(userAgent = "Custom/1.0")).userAgent).isEqualTo("Custom/1.0")
    }
}
