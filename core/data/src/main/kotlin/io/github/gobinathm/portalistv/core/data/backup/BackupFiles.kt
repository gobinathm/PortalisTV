package io.github.gobinathm.portalistv.core.data.backup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** A backup file in the app's backup folder. */
data class FolderBackup(val name: String, val path: String)

/**
 * Reads and writes backups either at document URIs chosen with the system file picker or, on
 * devices without one (Google TV and many other TV builds), in the app's own folder on shared
 * storage. That folder needs no permission and is reachable with `adb pull` and `adb push`.
 */
class BackupFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backup: ProfileBackup,
) {
    /** The folder used without a file picker, or null if shared storage is unavailable. */
    val folder: File? get() = context.getExternalFilesDir(FOLDER_NAME)

    /**
     * Whether a real document picker exists. TV builds often route the picker intents to a stub
     * that only reports the feature as unsupported.
     */
    fun hasDocumentPicker(): Boolean {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .any { it.activityInfo.packageName !in STUB_PACKAGES }
    }

    /** @return the number of profiles written. */
    suspend fun export(uri: Uri, password: CharArray): Int = withContext(Dispatchers.IO) {
        // "wt" truncates, so overwriting a longer file leaves no stale bytes behind.
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw FileNotFoundException("$uri")
        output.use { backup.export(it, password) }
    }

    suspend fun import(uri: Uri, password: CharArray): ImportResult = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException("$uri")
        input.use { backup.import(it, password) }
    }

    /** Writes a new, timestamped file to [folder]. @return the profile count and the file's path. */
    suspend fun exportToFolder(password: CharArray): Pair<Int, String> = withContext(Dispatchers.IO) {
        val dir = folder?.also { it.mkdirs() } ?: throw FileNotFoundException("Shared storage unavailable")
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "portalis-profiles-$stamp$EXTENSION")
        val count = file.outputStream().use { backup.export(it, password) }
        count to file.absolutePath
    }

    /** Backups in [folder], newest first. */
    suspend fun folderBackups(): List<FolderBackup> = withContext(Dispatchers.IO) {
        folder?.listFiles { file -> file.isFile && file.name.endsWith(EXTENSION) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { FolderBackup(it.name, it.absolutePath) }
    }

    suspend fun importFromFolder(path: String, password: CharArray): ImportResult = withContext(Dispatchers.IO) {
        val file = File(path)
        // Only files the app listed itself; never an arbitrary path.
        require(file.parentFile?.canonicalPath == folder?.canonicalPath) { "Not in the backup folder" }
        file.inputStream().use { backup.import(it, password) }
    }

    companion object {
        const val EXTENSION = ".ptvb"
        private const val FOLDER_NAME = "backups"
        private val STUB_PACKAGES = setOf("com.android.tv.frameworkpackagestubs")
    }
}
