package me.bmax.apatch.ui.screen.settings.appearance

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.SegmentedPreferenceItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.FontMode
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.ui.showToast

@Composable
fun AppearanceFontSection(
    flat: Boolean,
    highlightKey: String?,
    fontMode: FontMode,
    onFontModeChange: (FontMode) -> Unit,
    pickFontLauncher: ActivityResultLauncher<String>,
    snackBarHost: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SegmentedColumn(title = stringResource(R.string.settings_appearance_font), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_font_mode_app") {
            FontModePreference(
                title = stringResource(R.string.settings_font_mode_app),
                selected = fontMode == FontMode.APP_DEFAULT,
                onSelect = { onFontModeChange(FontMode.APP_DEFAULT) },
            )
        }

        item(key = "appearance_font_mode_system") {
            FontModePreference(
                title = stringResource(R.string.settings_font_mode_system),
                selected = fontMode == FontMode.SYSTEM_DEFAULT,
                onSelect = { onFontModeChange(FontMode.SYSTEM_DEFAULT) },
            )
        }

        item(key = "appearance_font_mode_custom") {
            FontModePreference(
                title = stringResource(R.string.settings_font_mode_custom),
                selected = fontMode == FontMode.CUSTOM,
                onSelect = { onFontModeChange(FontMode.CUSTOM) },
            )
        }

        if (fontMode == FontMode.CUSTOM) {
            item(key = "appearance_select_font") {
                SegmentedValueItem(
                    icon = Icons.Outlined.FontDownload,
                    title = stringResource(id = R.string.settings_select_font_file),
                    onClick = {
                        try {
                            pickFontLauncher.launch("*/*")
                        } catch (e: ActivityNotFoundException) {
                            showToast(context, e.message ?: "")
                        }
                    },
                )
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
                    SegmentedValueItem(
                        icon = Icons.Outlined.Delete,
                        title = clearFontTitle,
                        onClick = {
                            clearFontDialog.showConfirm(
                                title = clearFontTitle,
                                content = clearFontConfirm,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** A radio-style settings row for picking one of the three font modes. */
@Composable
private fun FontModePreference(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    SegmentedPreferenceItem(
        title = title,
        selected = selected,
        onClick = onSelect,
        trailing = {
            RadioButton(selected = selected, onClick = null)
        },
    )
}
