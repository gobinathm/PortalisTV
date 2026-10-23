package io.github.gobinathm.portalistv.core.security

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** The password is wrong, or the file was modified. AES-GCM cannot tell the two apart. */
class WrongPasswordException(cause: Throwable? = null) :
    GeneralSecurityException("Wrong password or damaged file", cause)

/** The data is not a backup this app can read. */
class UnsupportedFormatException(message: String) : GeneralSecurityException(message)

/**
 * Password-based encryption for exported backups, which leave the device and so cannot use
 * Keystore keys.
 *
 * Format: `"PTVB" | version (1) | iterations (4) | salt (16) | nonce (12) | AES-256-GCM ciphertext`.
 * The key is derived with PBKDF2-HMAC-SHA256, and the whole header is authenticated as
 * associated data, so a changed iteration count or salt fails decryption like a wrong password.
 */
class PasswordCipher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .put(VERSION)
            .putInt(iterations)
            .put(salt)
            .put(nonce)
            .array()
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, iterations, nonce)
        cipher.updateAAD(header)
        return header + cipher.doFinal(plaintext)
    }

    /**
     * @throws WrongPasswordException if the password is wrong or the data was changed.
     * @throws UnsupportedFormatException if the data is not a backup or is from a newer format.
     */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val header = readHeader(data)
        val cipher = cipher(Cipher.DECRYPT_MODE, password, header.salt, header.iterations, header.nonce)
        cipher.updateAAD(data, 0, HEADER_BYTES)
        return try {
            cipher.doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException(e)
        }
    }

    private class Header(val iterations: Int, val salt: ByteArray, val nonce: ByteArray)

    private fun readHeader(data: ByteArray): Header {
        val buffer = ByteBuffer.wrap(data)
        val magic = ByteArray(MAGIC.size)
        val valid = data.size >= HEADER_BYTES + TAG_BYTES &&
            buffer.get(magic).let { magic.contentEquals(MAGIC) } &&
            buffer.get() == VERSION
        if (!valid) throw UnsupportedFormatException("Not a Portalis backup, or from a newer version")
        val iterations = buffer.getInt()
        // Bounds stop a crafted file from demanding years of key derivation.
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw UnsupportedFormatException("Unsupported key derivation settings")
        }
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val nonce = ByteArray(NONCE_BYTES).also { buffer.get(it) }
        return Header(iterations, salt, nonce)
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, iterations: Int, nonce: ByteArray): Cipher {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        val keyBytes = try {
            SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BYTES * Byte.SIZE_BITS, nonce))
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    companion object {
        /** OWASP's 2023 recommendation for PBKDF2-HMAC-SHA256. */
        const val DEFAULT_ITERATIONS = 600_000
        const val MIN_ITERATIONS = 1_000
        const val MAX_ITERATIONS = 10_000_000

        private val MAGIC = "PTVB".encodeToByteArray()
        private const val VERSION: Byte = 1
        private const val SALT_BYTES = 16
        private const val NONCE_BYTES = 12
        private const val TAG_BYTES = 16
        private const val KEY_BITS = 256
        private val HEADER_BYTES = MAGIC.size + 1 + Int.SIZE_BYTES + SALT_BYTES + NONCE_BYTES
        private const val KDF = "PBKDF2WithHmacSHA256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
