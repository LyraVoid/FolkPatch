package me.bmax.apatch.ui.screen.settings.appearance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceThemeSection(
    flat: Boolean,
    highlightKey: String?,
    onNavigateToThemeStore: () -> Unit,
    themeStoreMode: String?,
    onThemeStoreModeChanged: ((String) -> Unit)?,
    showExportDialog: MutableState<Boolean>,
    showFilePicker: MutableState<Boolean>,
    snackBarHost: SnackbarHostState,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = APApplication.sharedPreferences

    SplicedColumnGroup(title = stringResource(R.string.settings_appearance_theme), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_theme_store") {
            ExpressiveCard(flat = flat, onClick = { onNavigateToThemeStore() }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.theme_store_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        if (themeStoreMode == "compat") {
                            Text(
                                text = stringResource(R.string.theme_mode_compat_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item(key = "appearance_theme_store_mode") {
            val modeName = when (themeStoreMode) {
                "compat" -> stringResource(R.string.theme_mode_compat)
                else -> stringResource(R.string.theme_mode_builtin)
            }
            val showModeSwitchDialog = remember { mutableStateOf(false) }
            ExpressiveCard(flat = flat, onClick = { showModeSwitchDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_theme_mode),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.theme_mode_current, modeName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showModeSwitchDialog.value) {
                BasicAlertDialog(
                    onDismissRequest = { showModeSwitchDialog.value = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                        modifier = Modifier.width(320.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(R.string.theme_mode_switch_title),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_mode_switch_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val builtinLabel = stringResource(R.string.theme_mode_builtin_label)
                            val compatLabel = stringResource(R.string.theme_mode_compat_label)
                            listOf("builtin" to builtinLabel, "compat" to compatLabel).forEach { (mode, label) ->
                                Surface(
                                    onClick = {
                                        prefs.edit { putString("theme_mode", mode) }
                                        onThemeStoreModeChanged?.invoke(mode)
                                        showModeSwitchDialog.value = false
                                        scope.launch {
                                            snackBarHost.showSnackbar(
                                                context.getString(R.string.theme_mode_switched, label)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (themeStoreMode == mode) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = themeStoreMode == mode,
                                            onClick = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                if (mode == "compat") stringResource(R.string.theme_mode_compat_desc)
                                                else stringResource(R.string.theme_mode_builtin_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            TextButton(
                                onClick = { showModeSwitchDialog.value = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(android.R.string.cancel))
                            }
                        }
                    }
                }
            }
        }

        item(key = "appearance_save_theme") {
            ExpressiveCard(flat = flat, onClick = { showExportDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(text = stringResource(id = R.string.settings_save_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item(key = "appearance_import_theme") {
            ExpressiveCard(flat = flat, onClick = { showFilePicker.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(text = stringResource(id = R.string.settings_import_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item(key = "appearance_reset_theme") {
            val resetThemeDialog = rememberConfirmDialog(
                onConfirm = {
                    scope.launch {
                        loadingDialog.show()
                        val success = ThemeManager.resetTheme(context)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_reset) else context.getString(R.string.settings_theme_reset_failed)
                        )
                    }
                }
            )
            val resetThemeTitle = stringResource(id = R.string.settings_reset_theme)
            val resetThemeConfirm = context.getString(R.string.settings_reset_theme_confirm)
            ExpressiveCard(
                flat = flat,
                onClick = {
                    resetThemeDialog.showConfirm(
                        title = resetThemeTitle,
                        content = resetThemeConfirm,
                    )
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(text = resetThemeTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
