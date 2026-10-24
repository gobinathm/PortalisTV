package io.github.gobinathm.portalistv.core.data

import com.google.common.truth.Truth.assertThat
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.security.PinHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ParentalLockTest {
    private val settings = object : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())

        override suspend fun update(transform: (AppSettings) -> AppSettings) = settings.update(transform)
    }
    private var now = 0L

    private fun lock(dispatcher: TestDispatcher) =
        ParentalLock(settings, PinHasher(iterations = 1_000), { now }, dispatcher)

    @Test
    fun `is open without a pin`() = runTest {
        val lock = lock(StandardTestDispatcher(testScheduler))
        assertThat(lock.isOpen()).isTrue()
    }

    @Test
    fun `unlocks with the right pin only`() = runTest {
        val lock = lock(StandardTestDispatcher(testScheduler))
        lock.setPin("2468")
        val fresh = lock(StandardTestDispatcher(testScheduler))
        assertThat(fresh.isOpen()).isFalse()
        assertThat(fresh.check("1111")).isEqualTo(PinResult.Wrong)
        assertThat(fresh.check("2468")).isEqualTo(PinResult.Correct)
        assertThat(fresh.isOpen()).isTrue()
    }

    @Test
    fun `pauses after too many wrong tries`() = runTest {
        val lock = lock(StandardTestDispatcher(testScheduler))
        lock.setPin("2468")
        val fresh = lock(StandardTestDispatcher(testScheduler))
        repeat(ParentalLock.MAX_TRIES - 1) { assertThat(fresh.check("0000")).isEqualTo(PinResult.Wrong) }
        assertThat(fresh.check("0000")).isInstanceOf(PinResult.Paused::class.java)
        // Even the right PIN waits out the pause.
        assertThat(fresh.check("2468")).isInstanceOf(PinResult.Paused::class.java)
        now += 31_000
        assertThat(fresh.check("2468")).isEqualTo(PinResult.Correct)
    }

    @Test
    fun `removing the pin also drops the settings lock`() = runTest {
        val lock = lock(StandardTestDispatcher(testScheduler))
        lock.setPin("2468")
        settings.update { it.copy(parental = it.parental.copy(lockSettings = true)) }
        lock.removePin()
        assertThat(settings.settings.value.parental.pin).isNull()
        assertThat(settings.settings.value.parental.lockSettings).isFalse()
    }
}
