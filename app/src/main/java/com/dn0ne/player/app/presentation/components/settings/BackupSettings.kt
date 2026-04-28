package com.dn0ne.player.app.presentation.components.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.data.backup.ExportResult
import com.dn0ne.player.app.data.backup.ImportResult
import com.dn0ne.player.app.presentation.components.NoteCard
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettings(
    onBackClick: () -> Unit,
    onExport: (Uri, (ExportResult) -> Unit) -> Unit,
    onImport: (Uri, (ImportResult) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        onExport(uri) { result ->
            val msg = when (result) {
                is ExportResult.Ok -> context.resources.getString(
                    R.string.backup_export_success,
                    result.playlists,
                    result.lovedTracks,
                )
                is ExportResult.Failure -> context.resources.getString(
                    R.string.backup_export_failure,
                    result.cause.localizedMessage ?: result.cause::class.simpleName.orEmpty(),
                )
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(context.resources.getString(R.string.backup_import_confirm_title)) },
            text = { Text(context.resources.getString(R.string.backup_import_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    onImport(uri) { result ->
                        val msg = when (result) {
                            is ImportResult.Ok -> context.resources.getString(
                                R.string.backup_import_success,
                                result.playlistsAdded,
                                result.playlistsSkipped,
                                result.lovedTracksAdded,
                                result.tracksUnresolved,
                            )
                            is ImportResult.Failure -> context.resources.getString(
                                R.string.backup_import_failure,
                                result.cause.localizedMessage ?: result.cause::class.simpleName.orEmpty(),
                            )
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text(context.resources.getString(R.string.backup_import_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(context.resources.getString(R.string.cancel))
                }
            },
        )
    }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back),
                )
            }
            Text(
                text = context.resources.getString(R.string.backup),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
            )
        },
        collapseFraction = { collapseFraction = it },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        NoteCard(
            label = context.resources.getString(R.string.backup_what_is_included),
            leadingIcon = Icons.Rounded.FileDownload,
        ) {
            Text(text = context.resources.getString(R.string.backup_what_is_included_body))
        }

        Spacer(Modifier.height(4.dp))

        FilledTonalButton(
            onClick = { createDocLauncher.launch(suggestedBackupFileName()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.FileDownload,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(context.resources.getString(R.string.backup_export))
        }

        FilledTonalButton(
            onClick = { openDocLauncher.launch(arrayOf("application/json", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(context.resources.getString(R.string.backup_import))
        }

        NoteCard(
            label = context.resources.getString(R.string.backup_import_policy_title),
            leadingIcon = Icons.Rounded.FileUpload,
        ) {
            Text(text = context.resources.getString(R.string.backup_import_policy_body))
        }
    }
}

private fun suggestedBackupFileName(): String {
    val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    return "lotus-backup-$ts.json"
}
