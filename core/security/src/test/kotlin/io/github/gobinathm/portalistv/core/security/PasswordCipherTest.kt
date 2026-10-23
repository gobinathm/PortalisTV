package io.github.gobinathm.portalistv.core.security

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class PasswordCipherTest {
    // Few iterations keep the tests fast; the format stores the count, so decryption still works.
    private val cipher = PasswordCipher(iterations = PasswordCipher.MIN_ITERATIONS)
    private val password = "correct horse".toCharArray()
    private val plaintext = """{"profiles":[{"mac":"00:1A:79:12:34:56"}]}""".encodeToByteArray()

    @Test
    fun `round-trips with the right password`() {
        val data = cipher.encrypt(plaintext, password)
        assertThat(cipher.decrypt(data, password)).isEqualTo(plaintext)
    }

    @Test
    fun `output hides the plaintext and differs every time`() {
        val first = cipher.encrypt(plaintext, password)
        val second = cipher.encrypt(plaintext, password)
        assertThat(first.decodeToString()).doesNotContain("00:1A:79")
        assertThat(first).isNotEqualTo(second)
        assertThat(first.copyOfRange(0, 4).decodeToString()).isEqualTo("PTVB")
    }

    @Test
    fun `does not clear the caller's password`() {
        cipher.encrypt(plaintext, password)
        assertThat(String(password)).isEqualTo("correct horse")
    }

    @Test
    fun `rejects a wrong password`() {
        val data = cipher.encrypt(plaintext, password)
        assertThrows(WrongPasswordException::class.java) { cipher.decrypt(data, "wrong".toCharArray()) }
    }

    @Test
    fun `rejects a modified ciphertext`() {
        val data = cipher.encrypt(plaintext, password)
        data[data.lastIndex] = (data.last().toInt() xor 1).toByte()
        assertThrows(WrongPasswordException::class.java) { cipher.decrypt(data, password) }
    }

    @Test
    fun `rejects a modified header because it is authenticated`() {
        val data = cipher.encrypt(plaintext, password)
        // Change the iteration count (bytes 5-8) to another value inside the allowed range.
        ByteBuffer.wrap(data, 5, 4).putInt(PasswordCipher.MIN_ITERATIONS + 1)
        assertThrows(WrongPasswordException::class.java) { cipher.decrypt(data, password) }
    }

    @Test
    fun `rejects iteration counts outside the allowed range`() {
        val data = cipher.encrypt(plaintext, password)
        ByteBuffer.wrap(data, 5, 4).putInt(Int.MAX_VALUE)
        assertThrows(UnsupportedFormatException::class.java) { cipher.decrypt(data, password) }
    }

    @Test
    fun `rejects data that is not a backup`() {
        assertThrows(UnsupportedFormatException::class.java) {
            cipher.decrypt("not a backup at all, just text".encodeToByteArray(), password)
        }
        assertThrows(UnsupportedFormatException::class.java) { cipher.decrypt(ByteArray(0), password) }
    }

    @Test
    fun `rejects an unknown format version`() {
        val data = cipher.encrypt(plaintext, password)
        data[4] = 2
        assertThrows(UnsupportedFormatException::class.java) { cipher.decrypt(data, password) }
    }
}
