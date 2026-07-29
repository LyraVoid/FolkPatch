package me.bmax.apatch.ui.screen.settings.appearance

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.ui.showToast

@Composable
fun AppearanceFontSection(
    flat: Boolean,
    highlightKey: String?,
    customFontEnabled: Boolean,
    onCustomFontEnabledChange: (Boolean) -> Unit,
    pickFontLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    snackBarHost: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SplicedColumnGroup(title = stringResource(R.string.settings_appearance_font), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_custom_font") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.FormatSize,
                title = stringResource(id = R.string.settings_custom_font),
                description = if (customFontEnabled) {
                    if (FontConfig.customFontFilename != null) stringResource(id = R.string.settings_font_selected) else stringResource(id = R.string.settings_custom_font_enabled)
                } else {
                    stringResource(id = R.string.settings_custom_font_summary)
                },
                checked = customFontEnabled,
                onCheckedChange = {
                    onCustomFontEnabledChange(it)
                    FontConfig.setCustomFontEnabledState(it)
                    FontConfig.save(context)
                    refreshTheme.value = true
                },
            )
        }

        if (FontConfig.isCustomFontEnabled) {
            item(key = "appearance_select_font") {
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        try {
                            pickFontLauncher.launch("*/*")
                        } catch (e: ActivityNotFoundException) {
                            showToast(context, e.message ?: "")
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Filled.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(text = stringResource(id = R.string.settings_select_font_file), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (FontConfig.customFontFilename != null) {
                item(key = "appearance_clear_font") {
                    val clearFontDialog = rememberConfirmDialog(
                        onConfirm = {
                            FontConfig.clearFont(context)
                            refreshTheme.value = true
                            scope.launch {
                                snackBarHost.showSnackbar(message = context.getString(R.string.settings_font_cleared))
                            }
                        }
                    )
                    val clearFontTitle = stringResource(id = R.string.settings_clear_font)
                    val clearFontConfirm = context.getString(R.string.settings_clear_font_confirm)
                    ExpressiveCard(
                        flat = flat,
                        onClick = {
                            clearFontDialog.showConfirm(
                                title = clearFontTitle,
                                content = clearFontConfirm,
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(text = clearFontTitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
