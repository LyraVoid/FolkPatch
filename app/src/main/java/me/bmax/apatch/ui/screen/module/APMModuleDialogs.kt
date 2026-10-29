package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.ModuleShortcut

@Composable
fun ModuleShortcutDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    context: Context,
    module: APModuleViewModel.ModuleInfo,
    shortcutName: String,
    onShortcutNameChange: (String) -> Unit,
    shortcutIconUri: String?,
    onShortcutIconUriChange: (String?) -> Unit,
    shortcutType: String,
    onShortcutTypeChange: (String) -> Unit,
    shortcutPreviewBitmap: Bitmap?,
    appIcon: Drawable,
    effectiveShortcutIconUri: String?,
    onPickIcon: () -> Unit,
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.module_shortcut_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = shortcutName,
                    onValueChange = onShortcutNameChange,
                    label = { Text(stringResource(R.string.module_shortcut_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(FolkDimens.Size12))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.module_shortcut_icon))
                    Spacer(Modifier.width(FolkDimens.Size12))
                    if (shortcutPreviewBitmap != null) {
                        Image(
                            bitmap = shortcutPreviewBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size36)
                        )
                    } else if (shortcutIconUri != null) {
                        AsyncImage(
                            model = shortcutIconUri,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size36)
                        )
                    } else {
                        AsyncImage(
                            model = appIcon,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size36)
                        )
                    }
                }
                Spacer(Modifier.height(FolkDimens.Size8))
                Row(horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12)) {
                    TextButton(onClick = onPickIcon) {
                        Text(stringResource(R.string.module_shortcut_icon_select))
                    }
                    TextButton(onClick = { onShortcutIconUriChange(null) }) {
                        Text(stringResource(R.string.module_shortcut_icon_default))
                    }
                }
                if (module.hasWebUi && module.hasActionScript) {
                    Spacer(Modifier.height(FolkDimens.Size12))
                    Text(stringResource(R.string.module_shortcut_type))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = shortcutType == "webui",
                            onClick = { onShortcutTypeChange("webui") }
                        )
                        Text(stringResource(R.string.module_shortcut_type_webui))
                        Spacer(Modifier.width(FolkDimens.Size24))
                        RadioButton(
                            selected = shortcutType == "action",
                            onClick = { onShortcutTypeChange("action") }
                        )
                        Text(stringResource(R.string.module_shortcut_type_action))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (shortcutType == "webui" && module.hasWebUi) {
                    ModuleShortcut.createModuleWebUiShortcut(
                        context,
                        module.id,
                        shortcutName.ifEmpty { module.name },
                        effectiveShortcutIconUri
                    )
                } else if (module.hasActionScript) {
                    ModuleShortcut.createModuleActionShortcut(
                        context,
                        module.id,
                        shortcutName.ifEmpty { module.name },
                        effectiveShortcutIconUri
                    )
                }
                onDismiss()
            }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}
