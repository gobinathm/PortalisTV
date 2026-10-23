package io.github.gobinathm.portalistv.core.database

import com.google.common.truth.Truth.assertThat
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import io.github.gobinathm.portalistv.core.security.SecretCipher
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.google.crypto.tink.Aead as TinkAead

class DatabaseKeyTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val cipher = newCipher()

    private fun databaseKey(secretCipher: SecretCipher = cipher) = DatabaseKey(
        keyFile = folder.root.resolve("keys/database.key"),
        databaseFile = folder.root.resolve("portalis.db"),
        cipher = secretCipher,
    )

    @Test
    fun `creates a raw key spec of 32 random bytes`() {
        val spec = databaseKey().rawKeySpec().decodeToString()
        assertThat(spec).matches("x'[0-9a-f]{64}'")
    }

    @Test
    fun `returns the same key on later calls`() {
        val first = databaseKey().rawKeySpec()
        assertThat(databaseKey().rawKeySpec()).isEqualTo(first)
    }

    @Test
    fun `stores the key only in encrypted form`() {
        val spec = databaseKey().rawKeySpec().decodeToString()
        val hex = spec.removePrefix("x'").removeSuffix("'")
        val stored = folder.root.resolve("keys/database.key").readBytes()
        assertThat(stored.toHex()).doesNotContain(hex)
        assertThat(folder.root.resolve("keys/database.key.tmp").exists()).isFalse()
    }

    @Test
    fun `refuses to replace the key of an existing database`() {
        folder.root.resolve("portalis.db").writeText("existing")
        assertThrows(DatabaseKeyUnavailableException::class.java) { databaseKey().rawKeySpec() }
        assertThat(folder.root.resolve("keys/database.key").exists()).isFalse()
    }

    @Test
    fun `reports a key that the keystore can no longer decrypt`() {
        databaseKey().rawKeySpec()
        assertThrows(DatabaseKeyUnavailableException::class.java) { databaseKey(newCipher()).rawKeySpec() }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

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
