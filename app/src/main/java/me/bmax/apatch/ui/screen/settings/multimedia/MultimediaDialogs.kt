package me.bmax.apatch.ui.screen.settings.multimedia

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.SoundEffectConfig
import me.bmax.apatch.ui.theme.VibrationConfig
import me.bmax.apatch.util.SoundEffectManager
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

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
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectSourceDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = soundEffectSourceTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(soundEffectSourceLocal) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    SoundEffectConfig.setSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_LOCAL)
                                    SoundEffectConfig.save(context)
                                    showSoundEffectSourceDialog.value = false
                                }
                            )

                            ListItem(
                                headlineContent = { Text(soundEffectSourcePreset) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_PRESET,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSoundEffectSourceDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }

    // Sound Effect Preset Dialog
    if (showSoundEffectPresetDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectPresetDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = soundEffectPresetTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp,
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(SoundEffectConfig.PRESETS.size, key = { it }) { index ->
                                val preset = SoundEffectConfig.PRESETS[index]
                                ListItem(
                                    headlineContent = { Text(preset) },
                                    leadingContent = {
                                        RadioButton(
                                            selected = SoundEffectConfig.presetName == preset,
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSoundEffectPresetDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }

    // Sound Effect Scope Dialog
    if (showSoundEffectScopeDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectScopeDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = soundEffectScopeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_sound_effect_scope_global)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.scope == SoundEffectConfig.SCOPE_GLOBAL,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    SoundEffectConfig.setScopeValue(SoundEffectConfig.SCOPE_GLOBAL)
                                    SoundEffectConfig.save(context)
                                    showSoundEffectScopeDialog.value = false
                                }
                            )

                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_sound_effect_scope_bottom_bar)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.scope == SoundEffectConfig.SCOPE_BOTTOM_BAR,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSoundEffectScopeDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }

    // Startup Sound Source Dialog
    if (showStartupSourceDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showStartupSourceDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = startupSourceTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(startupSourceLocal) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    SoundEffectConfig.setStartupSourceTypeValue(SoundEffectConfig.SOURCE_TYPE_LOCAL)
                                    SoundEffectConfig.save(context)
                                    showStartupSourceDialog.value = false
                                }
                            )

                            ListItem(
                                headlineContent = { Text(startupSourcePreset) },
                                leadingContent = {
                                    RadioButton(
                                        selected = SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_PRESET,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showStartupSourceDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }

    // Startup Sound Preset Dialog
    if (showStartupPresetDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showStartupPresetDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = startupPresetTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp,
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(SoundEffectConfig.STARTUP_PRESETS.size, key = { it }) { index ->
                                val preset = SoundEffectConfig.STARTUP_PRESETS[index]
                                ListItem(
                                    headlineContent = { Text(preset) },
                                    leadingContent = {
                                        RadioButton(
                                            selected = SoundEffectConfig.startupPresetName == preset,
                                            onClick = null
                                        )
                                    },
                                    modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showStartupPresetDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }

    // Vibration Scope Dialog
    if (showVibrationScopeDialog.value) {
        BasicAlertDialog(
            onDismissRequest = { showVibrationScopeDialog.value = false },
            properties = DialogProperties(
                decorFitsSystemWindows = true,
                usePlatformDefaultWidth = false,
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(30.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = vibrationScopeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_vibration_scope_global)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = VibrationConfig.scope == VibrationConfig.SCOPE_GLOBAL,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
                                    VibrationConfig.setScopeValue(VibrationConfig.SCOPE_GLOBAL)
                                    VibrationConfig.save(context)
                                    showVibrationScopeDialog.value = false
                                }
                            )

                            ListItem(
                                headlineContent = { Text(stringResource(R.string.settings_vibration_scope_bottom_bar)) },
                                leadingContent = {
                                    RadioButton(
                                        selected = VibrationConfig.scope == VibrationConfig.SCOPE_BOTTOM_BAR,
                                        onClick = null
                                    )
                                },
                                modifier = Modifier.clickable {
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
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showVibrationScopeDialog.value = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    }
                }
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }
}
