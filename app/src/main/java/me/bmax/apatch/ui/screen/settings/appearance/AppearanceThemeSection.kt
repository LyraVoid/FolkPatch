package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

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

    SegmentedColumn(title = stringResource(R.string.settings_appearance_theme), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_theme_store") {
            SegmentedValueItem(
                icon = Icons.Outlined.Store,
                title = stringResource(id = R.string.theme_store_title),
                summary = if (themeStoreMode == "compat") stringResource(R.string.theme_mode_compat_desc) else null,
                modifier = Modifier.navOrigin(color = folkGroupColor()),
                onClick = { onNavigateToThemeStore() },
            )
        }

        item(key = "appearance_theme_store_mode") {
            val modeName = when (themeStoreMode) {
                "compat" -> stringResource(R.string.theme_mode_compat)
                else -> stringResource(R.string.theme_mode_builtin)
            }
            val showModeSwitchDialog = remember { mutableStateOf(false) }
            SegmentedValueItem(
                icon = Icons.Outlined.Tune,
                title = stringResource(R.string.settings_theme_mode),
                summary = stringResource(R.string.theme_mode_current, modeName),
                onClick = { showModeSwitchDialog.value = true },
            )

            if (showModeSwitchDialog.value) {
                ExpressiveDialog(
                    onDismissRequest = { showModeSwitchDialog.value = false },
                    width = FolkDimens.Size320,
                    blurBehind = false,
                ) {
                    Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                        Text(
                            text = stringResource(R.string.theme_mode_switch_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = FolkDimens.Size8)
                        )
                        Text(
                            text = stringResource(R.string.theme_mode_switch_msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = FolkDimens.Size16)
                        )

                        val builtinLabel = stringResource(R.string.theme_mode_builtin_label)
                        val compatLabel = stringResource(R.string.theme_mode_compat_label)
                        listOf("builtin" to builtinLabel, "compat" to compatLabel).forEach { (mode, label) ->
                            val modeInteractionSource = remember { MutableInteractionSource() }
                            val modeHaptics = LocalHapticFeedback.current
                            Surface(
                                shape = FolkShape.Corner12,
                                color = if (themeStoreMode == mode) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    FolkColors.Transparent
                                },
                                contentColor = if (themeStoreMode == mode) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .folkPressScale(modeInteractionSource, true)
                                    .selectable(
                                        selected = themeStoreMode == mode,
                                        interactionSource = modeInteractionSource,
                                        indication = null,
                                        role = Role.RadioButton,
                                        onClick = {
                                            modeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            prefs.edit { putString("theme_mode", mode) }
                                            onThemeStoreModeChanged?.invoke(mode)
                                            showModeSwitchDialog.value = false
                                            scope.launch {
                                                snackBarHost.showSnackbar(
                                                    context.getString(R.string.theme_mode_switched, label)
                                                )
                                            }
                                        },
                                    ),
                            ) {
                                Row(modifier = Modifier.padding(FolkDimens.Size12), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (themeStoreMode == mode) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            if (mode == "compat") stringResource(R.string.theme_mode_compat_desc)
                                            else stringResource(R.string.theme_mode_builtin_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (themeStoreMode == mode) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Spacer(Modifier.width(FolkDimens.Size12))
                                    RadioButton(selected = themeStoreMode == mode, onClick = null)
                                }
                            }
                            Spacer(Modifier.height(FolkDimens.Size8))
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

        item(key = "appearance_save_theme") {
            SegmentedValueItem(
                icon = Icons.Outlined.FileDownload,
                title = stringResource(id = R.string.settings_save_theme),
                onClick = { showExportDialog.value = true },
            )
        }

        item(key = "appearance_import_theme") {
            SegmentedValueItem(
                icon = Icons.Outlined.FileUpload,
                title = stringResource(id = R.string.settings_import_theme),
                onClick = { showFilePicker.value = true },
            )
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
            SegmentedValueItem(
                icon = Icons.Outlined.RestartAlt,
                title = resetThemeTitle,
                onClick = {                    resetThemeDialog.showConfirm(
                        title = resetThemeTitle,
                        content = resetThemeConfirm,
                    )
                },
            )
        }
    }
}
