package me.bmax.apatch.ui.screen.settings

import androidx.compose.material.icons.automirrored.outlined.Input
import android.content.ActivityNotFoundException
import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.screen.settings.multimedia.MultimediaDialogs
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.SoundEffectConfig
import me.bmax.apatch.ui.theme.VibrationConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaSoundSection(
    snackBarHost: SnackbarHostState,
    flat: Boolean = false,
    highlightKey: String? = null,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val pickSoundEffectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = SoundEffectConfig.saveSoundEffectFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_sound_effect_selected))
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_sound_effect_save_failed))
                }
            }
        }
    }

    val pickStartupSoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = SoundEffectConfig.saveStartupSoundFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_startup_sound_selected))
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_startup_sound_save_failed))
                }
            }
        }
    }

    val soundEffectTitle = stringResource(id = R.string.settings_sound_effect)
    val soundEffectSummary = stringResource(id = R.string.settings_sound_effect_summary)
    val soundEffectEnabledText = stringResource(id = R.string.settings_sound_effect_enabled)
    val soundEffectPlayingText = if (SoundEffectConfig.soundEffectFilename != null) stringResource(id = R.string.settings_sound_effect_playing, SoundEffectConfig.soundEffectFilename!!) else ""

    val selectSoundEffectTitle = stringResource(id = R.string.settings_select_sound_effect)
    val soundEffectSelectedText = stringResource(id = R.string.settings_sound_effect_selected)

    val soundEffectScopeTitle = stringResource(id = R.string.settings_sound_effect_scope)

    val startupSoundTitle = stringResource(id = R.string.settings_startup_sound)
    val startupSoundSummary = stringResource(id = R.string.settings_startup_sound_summary)
    val startupSoundEnabledText = stringResource(id = R.string.settings_startup_sound_enabled)
    val startupSoundPlayingText = if (SoundEffectConfig.startupSoundFilename != null) stringResource(id = R.string.settings_startup_sound_playing, SoundEffectConfig.startupSoundFilename!!) else ""

    val selectStartupSoundTitle = stringResource(id = R.string.settings_select_startup_sound)
    val startupSoundSelectedText = stringResource(id = R.string.settings_startup_sound_selected)

    val vibrationTitle = stringResource(id = R.string.settings_vibration)
    val vibrationSummary = stringResource(id = R.string.settings_vibration_summary)
    val vibrationEnabledText = stringResource(id = R.string.settings_vibration_enabled)

    val vibrationIntensityTitle = stringResource(id = R.string.settings_vibration_intensity)
    val vibrationScopeTitle = stringResource(id = R.string.settings_vibration_scope)

    val soundEffectSourceTitle = stringResource(id = R.string.settings_sound_effect_source)
    val soundEffectSourceLocal = stringResource(id = R.string.settings_sound_effect_source_local)
    val soundEffectSourcePreset = stringResource(id = R.string.settings_sound_effect_source_preset)
    val showSoundEffectSourceDialogState = remember { mutableStateOf(false) }

    // Sound effect preset dialog
    val soundEffectPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    val showSoundEffectPresetDialogState = remember { mutableStateOf(false) }

    // Clear sound effect dialog
    val clearSoundEffectTitle = stringResource(id = R.string.settings_clear_sound_effect)
    val clearSoundEffectDialog = rememberConfirmDialog(
        onConfirm = {
            SoundEffectConfig.clearSoundEffect(context)
            scope.launch {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_sound_effect_cleared))
            }
        }
    )

    // Sound effect scope dialog
    val showSoundEffectScopeDialogState = remember { mutableStateOf(false) }

    // Startup sound source dialog
    val startupSourceTitle = stringResource(id = R.string.settings_sound_effect_source)
    val startupSourceLocal = stringResource(id = R.string.settings_sound_effect_source_local)
    val startupSourcePreset = stringResource(id = R.string.settings_sound_effect_source_preset)
    val showStartupSourceDialogState = remember { mutableStateOf(false) }

    // Startup sound preset dialog
    val startupPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    val showStartupPresetDialogState = remember { mutableStateOf(false) }

    // Clear startup sound dialog
    val clearStartupSoundTitle = stringResource(id = R.string.settings_clear_startup_sound)
    val clearStartupSoundDialog = rememberConfirmDialog(
        onConfirm = {
            SoundEffectConfig.clearStartupSound(context)
            scope.launch {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_startup_sound_cleared))
            }
        }
    )

    // Vibration scope dialog
    val showVibrationScopeDialogState = remember { mutableStateOf(false) }


    SegmentedSection(title = stringResource(R.string.settings_section_multimedia_sound)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        // --- Sound Effect Toggle ---
        item(key = "multimedia_sound_effect") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.SurroundSound,
                title = soundEffectTitle,
                summary = if (SoundEffectConfig.isSoundEffectEnabled) {
                    if (SoundEffectConfig.soundEffectFilename != null) {
                        soundEffectPlayingText
                    } else {
                        soundEffectEnabledText
                    }
                } else {
                    soundEffectSummary
                },
                checked = SoundEffectConfig.isSoundEffectEnabled,
                onCheckedChange = {
                    SoundEffectConfig.setEnabledState(it)
                    SoundEffectConfig.save(context)
                }
            )
        }

        // --- Sound Effect: Source Selector ---
        item(key = "multimedia_sound_effect_source", visible = SoundEffectConfig.isSoundEffectEnabled) {
            SegmentedValueItem(
                icon = Icons.AutoMirrored.Outlined.Input,
                title = soundEffectSourceTitle,
                summary = if (SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) soundEffectSourceLocal else soundEffectSourcePreset,
                onClick = { showSoundEffectSourceDialogState.value = true },
            )
        }

        // --- Sound Effect: Select Local File (local source) ---
        item(key = "multimedia_select_sound_effect", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
            SegmentedValueItem(
                icon = Icons.Outlined.AudioFile,
                title = selectSoundEffectTitle,
                summary = if (SoundEffectConfig.soundEffectFilename != null) soundEffectSelectedText else null,
                onClick = {
                    try {
                        pickSoundEffectLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                },
            )
        }

        // --- Sound Effect: Clear Sound Effect (local source with file) ---
        item(key = "multimedia_clear_sound_effect", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.soundEffectFilename != null) {
            SegmentedNavigationItem(
                icon = Icons.Outlined.Delete,
                title = clearSoundEffectTitle,
                onClick = {
                    clearSoundEffectDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_sound_effect),
                        content = context.getString(R.string.settings_clear_sound_effect_confirm)
                    )
                },
            )
        }

        // --- Sound Effect: Preset Selector (preset source) ---
        item(key = "multimedia_sound_effect_preset", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            SegmentedValueItem(
                icon = Icons.Outlined.MusicNote,
                title = soundEffectPresetTitle,
                summary = SoundEffectConfig.presetName,
                onClick = { showSoundEffectPresetDialogState.value = true },
            )
        }

        // --- Sound Effect: Scope Selector ---
        item(key = "multimedia_sound_effect_scope", visible = SoundEffectConfig.isSoundEffectEnabled) {
            SegmentedValueItem(
                icon = Icons.Outlined.Tune,
                title = soundEffectScopeTitle,
                summary = if (SoundEffectConfig.scope == SoundEffectConfig.SCOPE_GLOBAL)
                    stringResource(R.string.settings_sound_effect_scope_global)
                else
                    stringResource(R.string.settings_sound_effect_scope_bottom_bar),
                onClick = { showSoundEffectScopeDialogState.value = true },
            )
        }

        // --- Startup Sound Toggle ---
        item(key = "multimedia_startup_sound") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Alarm,
                title = startupSoundTitle,
                summary = if (SoundEffectConfig.isStartupSoundEnabled) {
                    if (SoundEffectConfig.startupSoundFilename != null) {
                        startupSoundPlayingText
                    } else {
                        startupSoundEnabledText
                    }
                } else {
                    startupSoundSummary
                },
                checked = SoundEffectConfig.isStartupSoundEnabled,
                onCheckedChange = {
                    SoundEffectConfig.setStartupEnabledState(it)
                    SoundEffectConfig.save(context)
                }
            )
        }

        // --- Startup Sound: Source Selector ---
        item(key = "multimedia_startup_sound_source", visible = SoundEffectConfig.isStartupSoundEnabled) {
            SegmentedValueItem(
                icon = Icons.AutoMirrored.Outlined.Input,
                title = startupSourceTitle,
                summary = if (SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) startupSourceLocal else startupSourcePreset,
                onClick = { showStartupSourceDialogState.value = true },
            )
        }

        // --- Startup Sound: Select Local File (local source) ---
        item(key = "multimedia_select_startup_sound", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
            SegmentedValueItem(
                icon = Icons.Outlined.AudioFile,
                title = selectStartupSoundTitle,
                summary = if (SoundEffectConfig.startupSoundFilename != null) startupSoundSelectedText else null,
                onClick = {
                    try {
                        pickStartupSoundLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                },
            )
        }

        // --- Startup Sound: Clear Startup Sound (local source with file) ---
        item(key = "multimedia_clear_startup_sound", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.startupSoundFilename != null) {
            SegmentedNavigationItem(
                icon = Icons.Outlined.Delete,
                title = clearStartupSoundTitle,
                onClick = {
                    clearStartupSoundDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_startup_sound),
                        content = context.getString(R.string.settings_clear_startup_sound_confirm)
                    )
                },
            )
        }

        // --- Startup Sound: Preset Selector (preset source) ---
        item(key = "multimedia_startup_sound_preset", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            SegmentedValueItem(
                icon = Icons.Outlined.MusicNote,
                title = startupPresetTitle,
                summary = SoundEffectConfig.startupPresetName,
                onClick = { showStartupPresetDialogState.value = true },
            )
        }

        // --- Vibration Toggle ---
        item(key = "multimedia_vibration") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Vibration,
                title = vibrationTitle,
                summary = if (VibrationConfig.isVibrationEnabled) vibrationEnabledText else vibrationSummary,
                checked = VibrationConfig.isVibrationEnabled,
                onCheckedChange = {
                    VibrationConfig.setEnabledState(it)
                    VibrationConfig.save(context)
                }
            )
        }

        // --- Vibration: Scope Selector ---
        item(key = "multimedia_vibration_scope", visible = VibrationConfig.isVibrationEnabled) {
            SegmentedValueItem(
                icon = Icons.Outlined.Tune,
                title = vibrationScopeTitle,
                summary = if (VibrationConfig.scope == VibrationConfig.SCOPE_GLOBAL)
                    stringResource(R.string.settings_vibration_scope_global)
                else
                    stringResource(R.string.settings_vibration_scope_bottom_bar),
                onClick = { showVibrationScopeDialogState.value = true },
            )
        }

        // --- Vibration: Intensity Slider ---
        item(key = "multimedia_vibration_intensity", visible = VibrationConfig.isVibrationEnabled) {
            SegmentedSliderItem(
                title = vibrationIntensityTitle,
                value = VibrationConfig.vibrationIntensity,
                onValueChange = {
                    VibrationConfig.setIntensityValue(it)
                },
                onValueChangeFinished = { VibrationConfig.save(context) },
            )
        }
        }
    }


    MultimediaDialogs(
        showSoundEffectSourceDialog = showSoundEffectSourceDialogState,
        showSoundEffectPresetDialog = showSoundEffectPresetDialogState,
        showSoundEffectScopeDialog = showSoundEffectScopeDialogState,
        showStartupSourceDialog = showStartupSourceDialogState,
        showStartupPresetDialog = showStartupPresetDialogState,
        showVibrationScopeDialog = showVibrationScopeDialogState,
    )
}
