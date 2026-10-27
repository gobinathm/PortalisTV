package io.github.gobinathm.portalistv.feature.profiles.backup

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceDialog
import io.github.gobinathm.portalistv.core.designsystem.component.ChoiceOption
import io.github.gobinathm.portalistv.core.designsystem.component.TextInputDialog
import io.github.gobinathm.portalistv.core.designsystem.component.tapToClick
import io.github.gobinathm.portalistv.core.designsystem.theme.Spacing
import io.github.gobinathm.portalistv.feature.profiles.R
import io.github.gobinathm.portalistv.feature.profiles.component.MessageDialog

/** Where the backup flow is waiting for the user. */
private sealed interface BackupPrompt {
    /** Export asks for the password first, then where to save. */
    data class ExportPassword(val tooShort: Boolean = false) : BackupPrompt

    /** Import picks the file first (a document [uri] or a [path] in the app folder), then asks for its password. */
    data class ImportPassword(val uri: Uri? = null, val path: String? = null) : BackupPrompt
}

/** Buttons to back up all profiles to a password-protected file and to restore one. */
@Composable
fun BackupActions(
    canExport: Boolean,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf<BackupPrompt?>(null) }
    // Held only between the password dialog and the file picker returning.
    var exportPassword by remember { mutableStateOf("") }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupViewModel.MIME_TYPE),
    ) { uri ->
        if (uri != null) viewModel.export(uri, exportPassword)
        exportPassword = ""
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) prompt = BackupPrompt.ImportPassword(uri = uri)
    }

    BackupButtons(
        canExport = canExport,
        onExport = { prompt = BackupPrompt.ExportPassword() },
        onImport = {
            if (viewModel.hasDocumentPicker) {
                launchOrReport(viewModel) { openDocument.launch(arrayOf("*/*")) }
            } else {
                viewModel.chooseFolderBackup()
            }
        },
        modifier = modifier,
    )
    PromptDialog(
        prompt = prompt,
        onExportPassword = { password ->
            when {
                password.length < BackupViewModel.MIN_PASSWORD_LENGTH -> {
                    prompt = BackupPrompt.ExportPassword(tooShort = true)
                    return@PromptDialog
                }
                viewModel.hasDocumentPicker -> {
                    exportPassword = password
                    launchOrReport(viewModel) { createDocument.launch(BackupViewModel.FILE_NAME) }
                }
                else -> viewModel.exportToFolder(password)
            }
            prompt = null
        },
        onImportPassword = { source, password ->
            prompt = null
            source.uri?.let { viewModel.import(it, password) }
            source.path?.let { viewModel.importFromFolder(it, password) }
        },
        onDismiss = { prompt = null },
    )
    BackupStatusDialog(
        status = status,
        onChoose = { path ->
            viewModel.dismiss()
            prompt = BackupPrompt.ImportPassword(path = path)
        },
        onDismiss = viewModel::dismiss,
    )
}

@Composable
private fun PromptDialog(
    prompt: BackupPrompt?,
    onExportPassword: (String) -> Unit,
    onImportPassword: (BackupPrompt.ImportPassword, String) -> Unit,
    onDismiss: () -> Unit,
) {
    when (prompt) {
        null -> Unit
        is BackupPrompt.ExportPassword -> TextInputDialog(
            title = stringResource(R.string.backup_password_title),
            initial = "",
            secret = true,
            hint = stringResource(
                if (prompt.tooShort) R.string.backup_password_too_short else R.string.backup_password_hint,
            ),
            onConfirm = onExportPassword,
            onDismiss = onDismiss,
        )
        is BackupPrompt.ImportPassword -> TextInputDialog(
            title = stringResource(R.string.backup_password_title),
            initial = "",
            secret = true,
            hint = stringResource(R.string.backup_restore_password_hint),
            onConfirm = { onImportPassword(prompt, it) },
            onDismiss = onDismiss,
        )
    }
}

private inline fun launchOrReport(viewModel: BackupViewModel, launch: () -> Unit) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        viewModel.noFilePicker()
    }
}

@Composable
private fun BackupButtons(canExport: Boolean, onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        if (canExport) {
            OutlinedButton(onClick = onExport, modifier = Modifier.tapToClick(onExport)) {
                Text(stringResource(R.string.backup_export))
            }
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.tapToClick(onImport)) {
            Text(stringResource(R.string.backup_import))
        }
    }
}

@Composable
private fun BackupStatusDialog(status: BackupStatus, onChoose: (path: String) -> Unit, onDismiss: () -> Unit) {
    when {
        status == BackupStatus.Idle -> Unit
        status == BackupStatus.Working -> MessageDialog(stringResource(R.string.backup_working), onDismiss = null)
        status is BackupStatus.ChooseFolderBackup && status.backups.isNotEmpty() -> ChoiceDialog(
            title = stringResource(R.string.backup_choose),
            options = status.backups.map { ChoiceOption(it.path, it.name) },
            selectedKey = null,
            onSelect = onChoose,
            onDismiss = onDismiss,
        )
        else -> MessageDialog(resultMessage(status), onDismiss)
    }
}

@Composable
private fun resultMessage(status: BackupStatus): String = when (status) {
    is BackupStatus.Exported -> {
        val done = pluralStringResource(R.plurals.backup_exported, status.count, status.count)
        status.path?.let { done + "\n\n" + stringResource(R.string.backup_saved_to, it) } ?: done
    }
    is BackupStatus.ChooseFolderBackup -> stringResource(R.string.backup_none_found, status.folder.orEmpty())
    is BackupStatus.Imported -> with(status.result) {
        stringResource(R.string.backup_imported, added, replaced, skipped)
    }
    BackupStatus.WrongPassword -> stringResource(R.string.backup_wrong_password)
    BackupStatus.NotABackup -> stringResource(R.string.backup_not_a_backup)
    BackupStatus.NoFilePicker -> stringResource(R.string.backup_no_file_picker)
    BackupStatus.Idle, BackupStatus.Working, BackupStatus.Failed -> stringResource(R.string.backup_failed)
}
