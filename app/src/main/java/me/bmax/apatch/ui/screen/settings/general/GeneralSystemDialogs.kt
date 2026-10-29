@file:OptIn(ExperimentalMaterial3Api::class)
package me.bmax.apatch.ui.screen.settings.general

import me.bmax.apatch.ui.theme.tokens.FolkDimens

import android.app.Activity
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.util.*
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape


@Composable
fun NewAppProfileModeDialog(
    showDialog: MutableState<Boolean>,
    initialMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val currentMode = remember(initialMode) { mutableIntStateOf(initialMode) }
    val options = listOf(
        0 to R.string.settings_new_app_profile_normal,
        1 to R.string.settings_new_app_profile_root,
        2 to R.string.settings_new_app_profile_exclude,
    )

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.settings_new_app_profile_mode),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )

            Surface(
                shape = FolkShape.Corner12,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = FolkDimens.Size2
            ) {
                Column {
                    options.forEach { (mode, labelId) ->
                        SegmentedSelectableItem(
                            title = stringResource(labelId),
                            selected = currentMode.intValue == mode,
                            onClick = {
                                val result = Natives.setNewAppProfileMode(mode)
                                if (result == 0L) {
                                    currentMode.intValue = mode
                                    onModeChanged(mode)
                                    showDialog.value = false
                                } else {
                                    showToast(context, context.getString(R.string.settings_new_app_profile_update_failed, result.toString()))
                                }
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    }
}

internal suspend fun loadNewAppProfileMode(prefs: SharedPreferences): Int = withContext(Dispatchers.IO) {
    val prefsValue = runCatching {
        prefs.getInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, 0)
    }.getOrDefault(0)
    val nativeMode = runCatching {
        Natives.getNewAppProfileMode()
    }.getOrDefault(prefsValue)
    when {
        // Native has an explicit non-zero value — trust it as authoritative
        nativeMode != 0 -> {
            if (nativeMode != prefsValue) {
                prefs.edit { putInt(APApplication.PREF_AUTO_EXCLUDE_NEW_APPS, nativeMode) }
            }
            nativeMode
        }
        // Native returned 0 (default or read failure), but prefs has a saved preference — restore native
        prefsValue != 0 -> {
            runCatching { Natives.setNewAppProfileMode(prefsValue) }
            prefsValue
        }
        // Both are 0 — true default
        else -> 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiChooseDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val activity = context as? Activity

    val savedDpi = DPIUtils.currentDpi
    var tempDpi by remember { mutableIntStateOf(if (savedDpi == DPIUtils.DEFAULT_DPI) DPIUtils.systemDpi else savedDpi) }
    var isSystemDefault by remember { mutableStateOf(savedDpi == DPIUtils.DEFAULT_DPI) }

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
        width = FolkDimens.Size340,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(id = R.string.settings_app_dpi),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(FolkDimens.Size16))

            // System default toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FolkShape.Corner12)
                    .background(
                        if (isSystemDefault) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        isSystemDefault = !isSystemDefault
                        if (isSystemDefault) {
                            tempDpi = DPIUtils.systemDpi
                        }
                    }
                    .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.system_default),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSystemDefault)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (isSystemDefault) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FolkDimens.Size20),
                    )
                }
            }

            AnimatedVisibility(visible = !isSystemDefault) {
                Column {
                    Spacer(Modifier.height(FolkDimens.Size16))

                    // Slider
                    val sliderValue by animateFloatAsState(
                        targetValue = tempDpi.toFloat(),
                        label = "DpiSlider",
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            tempDpi = newValue.toInt()
                        },
                        valueRange = DPIUtils.DPI_MIN.toFloat()..DPIUtils.DPI_MAX.toFloat(),
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )

                    // Current value display
                    Text(
                        text = "${DPIUtils.getDpiFriendlyName(tempDpi)} ($tempDpi DPI)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = FolkDimens.Size12),
                    )

                    // Preset pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = FolkDimens.Size8),
                        horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size6),
                    ) {
                        DPIUtils.presets.forEach { preset ->
                            val isSelected = tempDpi == preset.value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(FolkDimens.Size8))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { tempDpi = preset.value }
                                    .padding(vertical = FolkDimens.Size8, horizontal = FolkDimens.Size4),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(FolkDimens.Size16))

            // Apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(FolkDimens.Size8))
                Button(
                    onClick = {
                        val finalDpi = if (isSystemDefault) DPIUtils.DEFAULT_DPI else tempDpi
                        showDialog.value = false
                        DPIUtils.setDpi(context, finalDpi)
                        activity?.recreate()
                    },
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FolkDimens.Size18),
                    )
                    Spacer(Modifier.width(FolkDimens.Size8))
                    Text(stringResource(R.string.dpi_apply_settings))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SELinuxModeDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(currentMode) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.settings_selinux_mode),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )

            Surface(
                shape = FolkShape.Corner12,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = FolkDimens.Size2
            ) {
                Column {
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_selinux_mode_enforcing),
                        summary = stringResource(R.string.settings_selinux_mode_enforcing_summary),
                        selected = selectedMode == "Enforcing",
                        onClick = { selectedMode = "Enforcing" }
                    )

                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_selinux_mode_permissive),
                        summary = stringResource(R.string.settings_selinux_mode_permissive_summary),
                        selected = selectedMode == "Permissive",
                        onClick = { selectedMode = "Permissive" }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }

                Button(
                    onClick = {
                        showConfirmationDialog = true
                    },
                    enabled = selectedMode != currentMode,
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }

    if (showConfirmationDialog) {
        val isPermissive = selectedMode == "Permissive"
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(id = R.string.settings_selinux_mode)) },
            text = {
                if (isPermissive) {
                    Text(stringResource(id = R.string.msg_selinux_permissive_warning))
                } else {
                    Text(stringResource(id = R.string.msg_selinux_enforcing_confirm))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = setSELinuxMode(selectedMode == "Enforcing")
                        if (success) {
                            onModeChanged(selectedMode)
                        }
                        showDialog.value = false
                        showConfirmationDialog = false
                    }
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmationDialog = false }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}
