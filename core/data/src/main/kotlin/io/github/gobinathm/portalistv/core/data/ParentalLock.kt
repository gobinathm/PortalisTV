package io.github.gobinathm.portalistv.core.data

import io.github.gobinathm.portalistv.core.data.di.IoDispatcher
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.security.PinHasher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The outcome of entering a PIN. */
sealed interface PinResult {
    data object Correct : PinResult

    data object Wrong : PinResult

    /** Too many wrong tries; try again after [seconds]. */
    data class Paused(val seconds: Int) : PinResult
}

/**
 * The parental PIN. A correct PIN unlocks adult content (and Settings, if locked) until the app
 * is closed. Wrong tries are limited: after [MAX_TRIES] in a row, entry pauses for a while.
 */
@Singleton
class ParentalLock @Inject constructor(
    private val settings: SettingsRepository,
    private val hasher: PinHasher,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val _unlocked = MutableStateFlow(false)

    /** Whether the PIN has been entered since the app started. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val mutex = Mutex()
    private var wrongTries = 0
    private var pausedUntil = 0L

    suspend fun hasPin(): Boolean = settings.settings.first().parental.pin != null

    /** True when there's nothing to ask: no PIN set, or already unlocked. */
    suspend fun isOpen(): Boolean = _unlocked.value || !hasPin()

    suspend fun check(pin: String): PinResult = mutex.withLock {
        val now = clock.nowMillis()
        if (now < pausedUntil) return PinResult.Paused(((pausedUntil - now) / MILLIS + 1).toInt())
        val stored = settings.settings.first().parental.pin ?: return PinResult.Correct.also { _unlocked.value = true }
        val correct = withContext(io) { hasher.matches(pin, stored) }
        if (correct) {
            wrongTries = 0
            _unlocked.value = true
            PinResult.Correct
        } else {
            wrongTries++
            if (wrongTries >= MAX_TRIES) {
                wrongTries = 0
                pausedUntil = now + PAUSE_MS
                PinResult.Paused((PAUSE_MS / MILLIS).toInt())
            } else {
                PinResult.Wrong
            }
        }
    }

    /** Sets or replaces the PIN; the caller has already checked the current one, if any. */
    suspend fun setPin(pin: String) {
        val hash = withContext(io) { hasher.hash(pin) }
        settings.update { it.copy(parental = it.parental.copy(pin = hash)) }
        _unlocked.value = true
    }

    /** Removes the PIN, and with it the Settings lock. */
    suspend fun removePin() {
        settings.update { it.copy(parental = it.parental.copy(pin = null, lockSettings = false)) }
    }

    companion object {
        const val PIN_LENGTH = 4
        const val MAX_TRIES = 5
        private const val PAUSE_MS = 30_000L
        private const val MILLIS = 1000L
    }
}
