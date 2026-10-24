package io.github.gobinathm.portalistv.core.datastore

import androidx.datastore.core.CorruptionException
import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.github.gobinathm.portalistv.core.player.AspectMode
import io.github.gobinathm.portalistv.core.player.Deinterlace
import io.github.gobinathm.portalistv.core.player.HardwareDecoding
import io.github.gobinathm.portalistv.core.player.PlayerSettings
import io.github.gobinathm.portalistv.core.security.PinHash
import io.github.gobinathm.portalistv.core.security.SecretCipher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import com.google.crypto.tink.Aead as TinkAead

class SettingsSerializerTest {
    private val cipher = newCipher()
    private val serializer = SettingsSerializer { cipher }

    @Test
    fun `round-trips every setting`() = runTest {
        val settings = AppSettings(
            player = PlayerSettings(
                networkCachingMs = 3000,
                hardwareDecoding = HardwareDecoding.OFF,
                deinterlace = Deinterlace.ON,
                defaultAspect = AspectMode.FILL,
                audioTimeStretch = false,
                preferredAudioLanguage = "ta",
                preferredSubtitleLanguage = "en",
            ),
            pictureInPicture = false,
            showAdultContent = true,
            startProfileUuid = "6f1c1f0e-0000-4000-8000-000000000001",
            theme = AppTheme.OLED_BLACK,
            accent = AccentColor.MINT,
            keyBindings = mapOf(183 to PlayerAction.LAST_CHANNEL),
            parental = ParentalSettings(PinHash("c2FsdA==", "aGFzaA==", 1_000), lockSettings = true),
            homeRows = listOf(
                HomeRowSetting(HomeRow.LIVE_TV),
                HomeRowSetting(HomeRow.FAVORITES, visible = false),
                HomeRowSetting(HomeRow.CONTINUE_WATCHING),
                HomeRowSetting(HomeRow.ON_DEMAND),
            ),
        )
        assertThat(serializer.readFrom(ByteArrayInputStream(write(settings)))).isEqualTo(settings)
    }

    @Test
    fun `stores nothing in plain text`() = runTest {
        val bytes = write(AppSettings(player = PlayerSettings(preferredAudioLanguage = "ta")))
        assertThat(bytes.decodeToString()).doesNotContain("preferredAudioLanguage")
    }

    @Test
    fun `an empty file gives the defaults`() = runTest {
        assertThat(serializer.readFrom(ByteArrayInputStream(ByteArray(0)))).isEqualTo(AppSettings())
    }

    @Test
    fun `a file from another key is reported as corrupt`() = runTest {
        val other = SettingsSerializer { newCipher() }
        val bytes = ByteArrayOutputStream().also { other.writeTo(AppSettings(), it) }.toByteArray()
        val error = runCatching { serializer.readFrom(ByteArrayInputStream(bytes)) }.exceptionOrNull()
        assertThat(error).isInstanceOf(CorruptionException::class.java)
    }

    @Test
    fun `invalid stored values fall back to defaults`() {
        val stored = StoredSettings(
            networkCachingMs = 999_999,
            hardwareDecoding = "TURBO",
            preferredAudioLanguage = "en; --extraintf=http",
        )
        val player = stored.toAppSettings().player
        assertThat(player.networkCachingMs).isEqualTo(PlayerSettings.NETWORK_CACHING_RANGE_MS.last)
        assertThat(player.hardwareDecoding).isEqualTo(HardwareDecoding.AUTO)
        assertThat(player.preferredAudioLanguage).isNull()
    }

    private suspend fun write(settings: AppSettings): ByteArray =
        ByteArrayOutputStream().also { serializer.writeTo(settings, it) }.toByteArray()

    private fun newCipher(): SecretCipher {
        AeadConfig.register()
        val entry = KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM)
            .withRandomId()
            .makePrimary()
        val aead = KeysetHandle.newBuilder().addEntry(entry).build()
            .getPrimitive(RegistryConfiguration.get(), TinkAead::class.java)
        return SecretCipher(aead)
    }
}
