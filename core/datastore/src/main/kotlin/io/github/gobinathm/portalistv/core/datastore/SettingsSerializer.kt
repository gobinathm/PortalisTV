package io.github.gobinathm.portalistv.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.gobinathm.portalistv.core.security.SecretCipher
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

/**
 * Stores [AppSettings] as JSON encrypted with the settings key. [cipherProvider] is first called on
 * DataStore's IO scope, since loading the key touches the Android Keystore.
 */
internal class SettingsSerializer(cipherProvider: () -> SecretCipher) : Serializer<AppSettings> {
    private val cipher by lazy(cipherProvider)

    override val defaultValue = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            val text = cipher.decryptString(bytes, ASSOCIATED_DATA)
            json.decodeFromString(StoredSettings.serializer(), text).toAppSettings()
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("Settings can't be decrypted", e)
        } catch (e: SerializationException) {
            throw CorruptionException("Settings can't be parsed", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Settings can't be parsed", e)
        }
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        val text = json.encodeToString(StoredSettings.serializer(), t.toStored())
        output.write(cipher.encryptString(text, ASSOCIATED_DATA))
    }

    private companion object {
        const val ASSOCIATED_DATA = "settings:app"
        val json = Json { ignoreUnknownKeys = true }
    }
}
