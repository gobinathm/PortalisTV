package io.github.gobinathm.portalistv.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A stored PIN: never the PIN itself, only a salted, slow hash of it. */
data class PinHash(val salt: String, val hash: String, val iterations: Int)

/**
 * Hashes parental PINs with PBKDF2-HMAC-SHA256 and a random salt. A 4-digit PIN has few
 * possibilities, so the hash is only one layer: it lives in the Keystore-encrypted settings, and
 * the app limits attempts.
 */
class PinHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun hash(pin: String): PinHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PinHash(encode(salt), encode(derive(pin, salt, iterations)), iterations)
    }

    /** Compares in constant time, so timing reveals nothing about how close a guess was. */
    fun matches(pin: String, stored: PinHash): Boolean {
        // A damaged stored value never matches.
        val salt = runCatching { decode(stored.salt) }.getOrNull()
        val expected = runCatching { decode(stored.hash) }.getOrNull()
        return salt != null && expected != null && MessageDigest.isEqual(derive(pin, salt, stored.iterations), expected)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String) = Base64.getDecoder().decode(text)

    companion object {
        /** Slow enough to make guessing costly, fast enough not to be noticed on a TV. */
        const val DEFAULT_ITERATIONS = 100_000
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
    }
}
