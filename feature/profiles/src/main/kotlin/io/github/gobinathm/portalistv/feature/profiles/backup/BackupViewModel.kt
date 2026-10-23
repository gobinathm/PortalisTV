package io.github.gobinathm.portalistv.feature.profiles.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.data.backup.BackupFiles
import io.github.gobinathm.portalistv.core.data.backup.FolderBackup
import io.github.gobinathm.portalistv.core.data.backup.ImportResult
import io.github.gobinathm.portalistv.core.security.UnsupportedFormatException
import io.github.gobinathm.portalistv.core.security.WrongPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupStatus {
    data object Idle : BackupStatus

    /** Deriving the key takes a few seconds on purpose. */
    data object Working : BackupStatus

    /** [path] is set when the backup went to the app folder instead of a picked document. */
    data class Exported(val count: Int, val path: String? = null) : BackupStatus

    data class Imported(val result: ImportResult) : BackupStatus

    data object WrongPassword : BackupStatus

    data object NotABackup : BackupStatus

    /** Neither a file picker nor the app folder is available. */
    data object NoFilePicker : BackupStatus

    /** No file picker; the user chooses among the backups in the app folder. */
    data class ChooseFolderBackup(val backups: List<FolderBackup>, val folder: String?) : BackupStatus

    data object Failed : BackupStatus
}

@HiltViewModel
class BackupViewModel @Inject constructor(private val files: BackupFiles) : ViewModel() {
    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    /** Checked once: whether to use the system file picker or the app folder. */
    val hasDocumentPicker: Boolean = files.hasDocumentPicker()

    fun export(uri: Uri, password: String) = run { BackupStatus.Exported(files.export(uri, password.toCharArray())) }

    fun exportToFolder(password: String) = run {
        val (count, path) = files.exportToFolder(password.toCharArray())
        BackupStatus.Exported(count, path)
    }

    fun import(uri: Uri, password: String) = run { BackupStatus.Imported(files.import(uri, password.toCharArray())) }

    /** Lists the app folder so the user can pick a backup to restore. */
    fun chooseFolderBackup() {
        viewModelScope.launch {
            val folder = files.folder?.absolutePath
            _status.value = if (folder == null) {
                BackupStatus.NoFilePicker
            } else {
                BackupStatus.ChooseFolderBackup(files.folderBackups(), folder)
            }
        }
    }

    fun importFromFolder(path: String, password: String) =
        run { BackupStatus.Imported(files.importFromFolder(path, password.toCharArray())) }

    /** The device has no app that can create or open documents, which is common on TVs. */
    fun noFilePicker() {
        _status.value = BackupStatus.NoFilePicker
    }

    fun dismiss() {
        _status.value = BackupStatus.Idle
    }

    private fun run(block: suspend () -> BackupStatus) {
        if (_status.value == BackupStatus.Working) return
        _status.value = BackupStatus.Working
        viewModelScope.launch {
            _status.value = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: WrongPasswordException) {
                BackupStatus.WrongPassword
            } catch (_: UnsupportedFormatException) {
                BackupStatus.NotABackup
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(TAG, e) { "Backup failed" }
                BackupStatus.Failed
            }
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val FILE_NAME = "portalis-profiles.ptvb"
        const val MIME_TYPE = "application/octet-stream"
        private const val TAG = "Backup"
    }
}
