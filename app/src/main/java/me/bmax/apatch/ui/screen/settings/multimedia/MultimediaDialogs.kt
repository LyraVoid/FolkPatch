package me.bmax.apatch.ui.screen.settings.multimedia

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.SoundEffectConfig
import me.bmax.apatch.ui.theme.VibrationConfig
import me.bmax.apatch.util.SoundEffectManager
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaDialogs(
    showSoundEffectSourceDialog: MutableState<Boolean>,
    showSoundEffectPresetDialog: MutableState<Boolean>,
    showSoundEffectScopeDialog: MutableState<Boolean>,
    showStartupSourceDialog: MutableState<Boolean>,
    showStartupPresetDialog: MutableState<Boolean>,
    showVibrationScopeDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current

    val soundEffectSourceTitle = stringResource(id = R.string.settings_sound_effect_source)
    val soundEffectSourceLocal = stringResource(id = R.string.settings_sound_effect_source_local)
    val soundEffectSourcePreset = stringResource(id = R.string.settings_sound_effect_preset_title)
    val soundEffectPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    val startupSourceTitle = stringResource(id = R.string.settings_sound_effect_source)
    val startupSourceLocal = stringResource(id = R.string.settings_sound_effect_source_local)
    val startupSourcePreset = stringResource(id = R.string.settings_sound_effect_preset_title)
    val startupPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    val soundEffectScopeTitle = stringResource(id = R.string.settings_sound_effect_scope)
    val vibrationScopeTitle = stringResource(id = R.string.settings_vibration_scope)


    // Sound Effect Source Dialog
    if (showSoundEffectSourceDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showSoundEffectSourceDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = soundEffectSourceTitle,
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
                            title = soundEffectSourceLocal,
                            selected = SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL,
                            onClick = {
                                SoundEffectConfig.setSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_LOCAL)
                                SoundEffectConfig.save(context)
                                showSoundEffectSourceDialog.value = false
                            }
                        )

                        SegmentedSelectableItem(
                            title = soundEffectSourcePreset,
                            selected = SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_PRESET,
                            onClick = {
                                SoundEffectConfig.setSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_PRESET)
                                SoundEffectConfig.save(context)
                                showSoundEffectSourceDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size24),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSoundEffectSourceDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }

    // Sound Effect Preset Dialog
    if (showSoundEffectPresetDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showSoundEffectPresetDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = soundEffectPresetTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                Surface(
                    shape = FolkShape.Corner12,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = FolkDimens.Size2,
                    modifier = Modifier.heightIn(max = FolkDimens.Size400)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(SoundEffectConfig.PRESETS.size, key = { it }) { index ->
                            val preset = SoundEffectConfig.PRESETS[index]
                            SegmentedSelectableItem(
                                title = preset,
                                selected = SoundEffectConfig.presetName == preset,
                                onClick = {
                                    SoundEffectConfig.setPresetNameValue(preset)
                                    SoundEffectConfig.save(context)
                                    showSoundEffectPresetDialog.value = false
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
                    TextButton(onClick = { showSoundEffectPresetDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }

    // Sound Effect Scope Dialog
    if (showSoundEffectScopeDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showSoundEffectScopeDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = soundEffectScopeTitle,
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
                            title = stringResource(R.string.settings_sound_effect_scope_global),
                            selected = SoundEffectConfig.scope == SoundEffectConfig.SCOPE_GLOBAL,
                            onClick = {
                                SoundEffectConfig.setScopeValue(SoundEffectConfig.SCOPE_GLOBAL)
                                SoundEffectConfig.save(context)
                                showSoundEffectScopeDialog.value = false
                            }
                        )

                        SegmentedSelectableItem(
                            title = stringResource(R.string.settings_sound_effect_scope_bottom_bar),
                            selected = SoundEffectConfig.scope == SoundEffectConfig.SCOPE_BOTTOM_BAR,
                            onClick = {
                                SoundEffectConfig.setScopeValue(SoundEffectConfig.SCOPE_BOTTOM_BAR)
                                SoundEffectConfig.save(context)
                                showSoundEffectScopeDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size24),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSoundEffectScopeDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }

    // Startup Sound Source Dialog
    if (showStartupSourceDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showStartupSourceDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = startupSourceTitle,
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
                            title = startupSourceLocal,
                            selected = SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL,
                            onClick = {
                                SoundEffectConfig.setStartupSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_LOCAL)
                                SoundEffectConfig.save(context)
                                showStartupSourceDialog.value = false
                            }
                        )

                        SegmentedSelectableItem(
                            title = startupSourcePreset,
                            selected = SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_PRESET,
                            onClick = {
                                SoundEffectConfig.setStartupSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_PRESET)
                                SoundEffectConfig.save(context)
                                showStartupSourceDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size24),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showStartupSourceDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }

    // Startup Sound Preset Dialog
    if (showStartupPresetDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showStartupPresetDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = startupPresetTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                Surface(
                    shape = FolkShape.Corner12,
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = FolkDimens.Size2,
                    modifier = Modifier.heightIn(max = FolkDimens.Size400)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(SoundEffectConfig.STARTUP_PRESETS.size, key = { it }) { index ->
                            val preset = SoundEffectConfig.STARTUP_PRESETS[index]
                            SegmentedSelectableItem(
                                title = preset,
                                selected = SoundEffectConfig.startupPresetName == preset,
                                onClick = {
                                    SoundEffectConfig.setStartupPresetNameValue(preset)
                                    SoundEffectConfig.save(context)
                                    showStartupPresetDialog.value = false
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
                    TextButton(onClick = { showStartupPresetDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }

    // Vibration Scope Dialog
    if (showVibrationScopeDialog.value) {
        ExpressiveDialog(
            onDismissRequest = { showVibrationScopeDialog.value = false },
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = vibrationScopeTitle,
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
                            title = stringResource(R.string.settings_vibration_scope_global),
                            selected = VibrationConfig.scope == VibrationConfig.SCOPE_GLOBAL,
                            onClick = {
                                VibrationConfig.setScopeValue(VibrationConfig.SCOPE_GLOBAL)
                                VibrationConfig.save(context)
                                showVibrationScopeDialog.value = false
                            }
                        )

                        SegmentedSelectableItem(
                            title = stringResource(R.string.settings_vibration_scope_bottom_bar),
                            selected = VibrationConfig.scope == VibrationConfig.SCOPE_BOTTOM_BAR,
                            onClick = {
                                VibrationConfig.setScopeValue(VibrationConfig.SCOPE_BOTTOM_BAR)
                                VibrationConfig.save(context)
                                showVibrationScopeDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size24),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showVibrationScopeDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
        }
    }
}
