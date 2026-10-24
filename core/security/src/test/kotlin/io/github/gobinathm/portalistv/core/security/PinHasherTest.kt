package io.github.gobinathm.portalistv.core.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinHasherTest {
    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun `matches only the same pin`() {
        val stored = hasher.hash("1234")
        assertThat(hasher.matches("1234", stored)).isTrue()
        assertThat(hasher.matches("1235", stored)).isFalse()
        assertThat(stored.hash).doesNotContain("1234")
    }

    @Test
    fun `salts every hash`() {
        assertThat(hasher.hash("1234").hash).isNotEqualTo(hasher.hash("1234").hash)
    }

    @Test
    fun `rejects a damaged hash instead of failing`() {
        assertThat(hasher.matches("1234", PinHash("not base64!", "x", 1_000))).isFalse()
    }
}
