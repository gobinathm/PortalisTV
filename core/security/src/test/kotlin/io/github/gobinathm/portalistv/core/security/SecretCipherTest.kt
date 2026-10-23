package io.github.gobinathm.portalistv.core.security

import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException

// The Android Keystore wrapping is exercised on device; this covers the cipher contract itself.
class SecretCipherTest {
    private val cipher = SecretCipher(newAead())

    @Test
    fun `round-trips a secret`() {
        val ciphertext = cipher.encryptString("00:1A:79:12:34:56", "profile:1:mac")
        assertThat(cipher.decryptString(ciphertext, "profile:1:mac")).isEqualTo("00:1A:79:12:34:56")
    }

    @Test
    fun `ciphertext does not contain the plaintext and is randomized`() {
        val first = cipher.encryptString("secret-value", "ad")
        val second = cipher.encryptString("secret-value", "ad")
        assertThat(first.decodeToString()).doesNotContain("secret-value")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `rejects ciphertext bound to other associated data`() {
        val ciphertext = cipher.encryptString("secret", "profile:1:mac")
        assertThrows(GeneralSecurityException::class.java) {
            cipher.decryptString(ciphertext, "profile:2:mac")
        }
    }

    @Test
    fun `rejects tampered ciphertext`() {
        val ciphertext = cipher.encryptString("secret", "ad")
        ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) { cipher.decryptString(ciphertext, "ad") }
    }

    @Test
    fun `rejects ciphertext from another key`() {
        val ciphertext = SecretCipher(newAead()).encryptString("secret", "ad")
        assertThrows(GeneralSecurityException::class.java) { cipher.decryptString(ciphertext, "ad") }
    }

    private fun newAead(): Aead {
        AeadConfig.register()
        val entry = KeysetHandle.generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM)
            .withRandomId()
            .makePrimary()
        return KeysetHandle.newBuilder().addEntry(entry).build()
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
