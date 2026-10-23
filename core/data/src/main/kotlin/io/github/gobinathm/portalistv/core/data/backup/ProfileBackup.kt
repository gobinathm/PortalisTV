package io.github.gobinathm.portalistv.core.data.backup

import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.normalized
import io.github.gobinathm.portalistv.core.data.validate
import io.github.gobinathm.portalistv.core.security.PasswordCipher
import io.github.gobinathm.portalistv.core.security.UnsupportedFormatException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** What an import changed. Invalid entries are skipped rather than failing the whole file. */
data class ImportResult(val added: Int, val replaced: Int, val skipped: Int)

/**
 * Exports all profiles to a password-encrypted file and imports them back, on this or another
 * device. Imported profiles replace those with the same uuid and are added otherwise.
 */
class ProfileBackup(
    private val repository: ProfileRepository,
    private val cipher: PasswordCipher,
    private val clock: () -> Long,
) {
    @Inject
    constructor(repository: ProfileRepository) : this(repository, PasswordCipher(), System::currentTimeMillis)

    /** @return the number of profiles written. */
    suspend fun export(output: OutputStream, password: CharArray): Int {
        val profiles = repository.profiles.first()
        val document = BackupDocument(BackupDocument.CURRENT_VERSION, clock(), profiles.map { it.toBackup() })
        val plaintext = json.encodeToString(BackupDocument.serializer(), document).encodeToByteArray()
        // Key derivation is deliberately slow; keep it off the main thread.
        val encrypted = withContext(Dispatchers.Default) { cipher.encrypt(plaintext, password) }
        plaintext.fill(0)
        withContext(Dispatchers.IO) { output.write(encrypted) }
        return profiles.size
    }

    /**
     * @throws io.github.gobinathm.portalistv.core.security.WrongPasswordException for a wrong password.
     * @throws UnsupportedFormatException for files that are not backups, too large, or too new.
     */
    suspend fun import(input: InputStream, password: CharArray): ImportResult {
        val data = withContext(Dispatchers.IO) { input.readBounded() }
        val plaintext = withContext(Dispatchers.Default) { cipher.decrypt(data, password) }
        val document = try {
            json.decodeFromString(BackupDocument.serializer(), plaintext.decodeToString())
        } catch (e: SerializationException) {
            throw UnsupportedFormatException("Backup contents are damaged").apply { initCause(e) }
        } finally {
            plaintext.fill(0)
        }
        if (document.version > BackupDocument.CURRENT_VERSION) {
            throw UnsupportedFormatException("Backup was made by a newer version of the app")
        }
        var added = 0
        var replaced = 0
        var skipped = 0
        for (entry in document.profiles) {
            val incoming = entry.toModel().normalized()
            val existing = repository.getByUuid(incoming.uuid)
            when {
                incoming.validate().isNotEmpty() -> skipped++
                existing == null -> repository.save(incoming).also { added++ }
                else -> repository.save(incoming.copy(id = existing.id)).also { replaced++ }
            }
        }
        return ImportResult(added, replaced, skipped)
    }

    private fun InputStream.readBounded(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > MAX_BACKUP_BYTES) throw UnsupportedFormatException("File is too large to be a backup")
        }
        return out.toByteArray()
    }

    private companion object {
        /** Thousands of profiles fit in far less; the cap guards against reading a huge file into memory. */
        const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
        const val BUFFER_BYTES = 8 * 1024

        val json = Json {
            // Lets an older app read a same-version backup that gained optional fields.
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
