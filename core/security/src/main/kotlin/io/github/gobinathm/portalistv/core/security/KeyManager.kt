package io.github.gobinathm.portalistv.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.github.gobinathm.portalistv.core.common.log.Log

/** Separate keys per purpose, so one key never protects unrelated data. */
enum class KeyPurpose(internal val keysetName: String) {
    /** Wraps the database passphrase. */
    DATABASE("database"),

    /** Encrypts app settings. */
    SETTINGS("settings"),
}

/**
 * Provides [SecretCipher]s backed by Tink AES-256-GCM keysets.
 *
 * Each keyset is stored in private shared preferences, encrypted by a non-exportable
 * Android Keystore master key, so the keyset is useless when copied off the device.
 * Keysets are excluded from backups (see the app's data extraction rules), which means a
 * reinstall or restore on another device starts with fresh keys by design.
 */
class KeyManager(private val context: Context) {
    private val ciphers = mutableMapOf<KeyPurpose, SecretCipher>()

    init {
        AeadConfig.register()
    }

    @Synchronized
    fun cipher(purpose: KeyPurpose): SecretCipher =
        ciphers.getOrPut(purpose) { SecretCipher(loadAead(purpose)) }

    private fun loadAead(purpose: KeyPurpose): Aead {
        val handle = AndroidKeysetManager.Builder()
            .withSharedPref(context, purpose.keysetName, PREFS_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        Log.i(TAG) { "Loaded keyset for ${purpose.name}" }
        return handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private companion object {
        const val TAG = "KeyManager"
        const val PREFS_FILE = "portalis_keysets"
        const val MASTER_KEY_URI = "android-keystore://portalis_master_key"
    }
}
