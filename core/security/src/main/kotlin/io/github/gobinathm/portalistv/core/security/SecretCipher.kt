package io.github.gobinathm.portalistv.core.security

import com.google.crypto.tink.Aead
import java.security.GeneralSecurityException

/**
 * Authenticated encryption for secrets at rest.
 *
 * [associatedData] binds a ciphertext to its context (for example `"profile:<id>:mac"`), so a
 * ciphertext copied to another field or record fails to decrypt instead of yielding a valid secret.
 */
class SecretCipher(private val aead: Aead) {
    fun encrypt(plaintext: ByteArray, associatedData: String): ByteArray =
        aead.encrypt(plaintext, associatedData.encodeToByteArray())

    /** @throws GeneralSecurityException if the ciphertext was tampered with or bound to other data. */
    fun decrypt(ciphertext: ByteArray, associatedData: String): ByteArray =
        aead.decrypt(ciphertext, associatedData.encodeToByteArray())

    fun encryptString(plaintext: String, associatedData: String): ByteArray =
        encrypt(plaintext.encodeToByteArray(), associatedData)

    fun decryptString(ciphertext: ByteArray, associatedData: String): String =
        decrypt(ciphertext, associatedData).decodeToString()
}
