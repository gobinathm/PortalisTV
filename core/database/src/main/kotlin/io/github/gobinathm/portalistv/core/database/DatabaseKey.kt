package io.github.gobinathm.portalistv.core.database

import io.github.gobinathm.portalistv.core.security.SecretCipher
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom

/** The database exists but its key cannot be recovered, so its contents are unreadable. */
class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The SQLCipher key: 32 random bytes, stored only in encrypted form in [keyFile].
 *
 * The key is handed to SQLCipher in raw-key form (`x'…'`), which skips the passphrase KDF.
 * Stretching a random 256-bit key adds nothing but cold-start time on slow TV boxes.
 */
class DatabaseKey(
    private val keyFile: File,
    private val databaseFile: File,
    private val cipher: SecretCipher,
    private val random: SecureRandom = SecureRandom(),
) {
    /**
     * Returns the key in SQLCipher raw-key form, creating it on first use.
     *
     * @throws DatabaseKeyUnavailableException if a database exists without a usable key.
     */
    @Synchronized
    fun rawKeySpec(): ByteArray {
        val key = if (keyFile.exists()) load() else create()
        try {
            return rawKeySpec(key)
        } finally {
            key.fill(0)
        }
    }

    private fun load(): ByteArray = try {
        cipher.decrypt(keyFile.readBytes(), ASSOCIATED_DATA)
    } catch (e: GeneralSecurityException) {
        throw DatabaseKeyUnavailableException("Database key could not be decrypted", e)
    } catch (e: IOException) {
        throw DatabaseKeyUnavailableException("Database key could not be read", e)
    }

    private fun create(): ByteArray {
        if (databaseFile.exists()) {
            throw DatabaseKeyUnavailableException("Database exists but its key is missing")
        }
        val key = ByteArray(KEY_BYTES).also(random::nextBytes)
        keyFile.parentFile?.mkdirs()
        // Write then rename, so a crash never leaves a truncated key behind.
        val temp = File(keyFile.parentFile, "${keyFile.name}.tmp")
        temp.writeBytes(cipher.encrypt(key, ASSOCIATED_DATA))
        if (!temp.renameTo(keyFile)) {
            temp.delete()
            throw IOException("Could not store the database key")
        }
        return key
    }

    private companion object {
        const val KEY_BYTES = 32
        const val ASSOCIATED_DATA = "database:key"
        const val PREFIX = "x'"
        const val BYTE_MASK = 0xFF
        const val NIBBLE_BITS = 4
        const val NIBBLE_MASK = 0x0F
        val HEX = "0123456789abcdef".toCharArray()

        // Built byte by byte: a String copy of the key could not be wiped from memory.
        fun rawKeySpec(key: ByteArray): ByteArray {
            val spec = ByteArray(PREFIX.length + key.size * 2 + 1)
            PREFIX.forEachIndexed { i, c -> spec[i] = c.code.toByte() }
            key.forEachIndexed { i, byte ->
                val v = byte.toInt() and BYTE_MASK
                val at = PREFIX.length + i * 2
                spec[at] = HEX[v ushr NIBBLE_BITS].code.toByte()
                spec[at + 1] = HEX[v and NIBBLE_MASK].code.toByte()
            }
            spec[spec.lastIndex] = '\''.code.toByte()
            return spec
        }
    }
}
