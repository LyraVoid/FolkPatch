package me.bmax.apatch.ui.screen.settings

import android.content.ActivityNotFoundException
import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.screen.settings.multimedia.MultimediaDialogs
import me.bmax.apatch.ui.component.SliderSettingCard
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.MusicConfig
import me.bmax.apatch.ui.theme.SoundEffectConfig
import me.bmax.apatch.ui.theme.VibrationConfig
import me.bmax.apatch.util.MusicManager
import me.bmax.apatch.util.SoundEffectManager
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

@Composable
fun formatTime(millis: Int): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaSettingsContent(
    snackBarHost: SnackbarHostState,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val pickMusicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = MusicConfig.saveMusicFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_music_saved))
                    MusicManager.reload()
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_music_save_error))
                }
            }
        }
    }

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

    val musicTitle = stringResource(id = R.string.settings_background_music)
    val musicSummary = stringResource(id = R.string.settings_background_music_summary)
    val musicEnabledText = stringResource(id = R.string.settings_background_music_enabled)
    val musicPlayingText = if (MusicConfig.musicFilename != null) stringResource(id = R.string.settings_background_music_playing, MusicConfig.musicFilename!!) else ""

    val selectMusicTitle = stringResource(id = R.string.settings_select_music_file)
    val musicSelectedText = stringResource(id = R.string.settings_music_selected)

    val autoPlayTitle = stringResource(id = R.string.settings_music_auto_play)
    val autoPlaySummary = stringResource(id = R.string.settings_music_auto_play_summary)

    val loopingTitle = stringResource(id = R.string.settings_music_looping)
    val loopingSummary = stringResource(id = R.string.settings_music_looping_summary)

    val musicVolumeTitle = stringResource(id = R.string.settings_music_volume)

    val playbackControlTitle = stringResource(id = R.string.settings_music_playback_control)

    val clearMusicTitle = stringResource(id = R.string.settings_clear_music)

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

    // --- State variables for dialogs (must be declared before SplicedColumnGroup) ---

    // Music playback state
    val currentPosition by MusicManager.currentPosition.collectAsStateWithLifecycle(initialValue = 0)
    val duration by MusicManager.duration.collectAsStateWithLifecycle(initialValue = 0)
    val isPlaying by MusicManager.isPlaying.collectAsStateWithLifecycle(initialValue = false)

    // Clear music dialog
    val clearMusicDialog = rememberConfirmDialog(
        onConfirm = {
            MusicConfig.clearMusic(context)
            MusicManager.stop()
            scope.launch {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_music_cleared))
            }
        }
    )

    // Sound effect source dialog
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

    SplicedColumnGroup(flat = flat, highlightKey = highlightKey) {

        // --- Background Music Toggle ---
        item(key = "multimedia_bg_music") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.MusicNote,
                title = musicTitle,
                description = if (MusicConfig.isMusicEnabled) {
                    if (MusicConfig.musicFilename != null) {
                        musicPlayingText
                    } else {
                        musicEnabledText
                    }
                } else {
                    musicSummary
                },
                checked = MusicConfig.isMusicEnabled,
                onCheckedChange = {
                    MusicConfig.setMusicEnabledState(it)
                    MusicConfig.save(context)
                    MusicManager.reload()
                }
            )
        }

        // --- Music: Select Music File ---
        item(key = "multimedia_select_music", visible = MusicConfig.isMusicEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    try {
                        pickMusicLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectMusicTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (MusicConfig.musicFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = musicSelectedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Music: Auto Play Toggle ---
        item(key = "multimedia_music_auto_play", visible = MusicConfig.isMusicEnabled) {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.PlayArrow,
                title = autoPlayTitle,
                description = autoPlaySummary,
                checked = MusicConfig.isAutoPlayEnabled,
                onCheckedChange = {
                    MusicConfig.setAutoPlayEnabledState(it)
                    MusicConfig.save(context)
                }
            )
        }

        // --- Music: Looping Toggle ---
        item(key = "multimedia_music_looping", visible = MusicConfig.isMusicEnabled) {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Repeat,
                title = loopingTitle,
                description = loopingSummary,
                checked = MusicConfig.isLoopingEnabled,
                onCheckedChange = {
                    MusicConfig.setLoopingEnabledState(it)
                    MusicConfig.save(context)
                    MusicManager.updateLooping(it)
                }
            )
        }

        // --- Music: Volume Slider ---
        item(key = "multimedia_music_volume", visible = MusicConfig.isMusicEnabled) {
            SliderSettingCard(
                flat = flat,
                title = musicVolumeTitle,
                value = MusicConfig.volume,
                onValueChange = {
                    MusicConfig.setVolumeValue(it)
                    MusicManager.updateVolume(it)
                },
                onValueChangeFinished = { MusicConfig.save(context) },
            )
        }

        // --- Music: Playback Control ---
        item(key = "multimedia_playback_control", visible = MusicConfig.isMusicEnabled && MusicConfig.musicFilename != null) {
            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = playbackControlTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            MusicManager.seekTo(it.toInt())
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { MusicManager.toggle() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.settings_music_playback_control)
                            )
                        }
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // --- Music: Clear Music ---
        item(key = "multimedia_clear_music", visible = MusicConfig.isMusicEnabled && MusicConfig.musicFilename != null) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    clearMusicDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_music),
                        content = context.getString(R.string.settings_clear_music_confirm)
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = clearMusicTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // --- Sound Effect Toggle ---
        item(key = "multimedia_sound_effect") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.SurroundSound,
                title = soundEffectTitle,
                description = if (SoundEffectConfig.isSoundEffectEnabled) {
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
            ExpressiveCard(
                flat = flat,
                onClick = { showSoundEffectSourceDialogState.value = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Input, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = soundEffectSourceTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) soundEffectSourceLocal else soundEffectSourcePreset,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Sound Effect: Select Local File (local source) ---
        item(key = "multimedia_select_sound_effect", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    try {
                        pickSoundEffectLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectSoundEffectTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (SoundEffectConfig.soundEffectFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = soundEffectSelectedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Sound Effect: Clear Sound Effect (local source with file) ---
        item(key = "multimedia_clear_sound_effect", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.soundEffectFilename != null) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    clearSoundEffectDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_sound_effect),
                        content = context.getString(R.string.settings_clear_sound_effect_confirm)
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = clearSoundEffectTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // --- Sound Effect: Preset Selector (preset source) ---
        item(key = "multimedia_sound_effect_preset", visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            ExpressiveCard(
                flat = flat,
                onClick = { showSoundEffectPresetDialogState.value = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = soundEffectPresetTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SoundEffectConfig.presetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Sound Effect: Scope Selector ---
        item(key = "multimedia_sound_effect_scope", visible = SoundEffectConfig.isSoundEffectEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    showSoundEffectScopeDialogState.value = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = soundEffectScopeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.scope == SoundEffectConfig.SCOPE_GLOBAL)
                                stringResource(R.string.settings_sound_effect_scope_global)
                            else
                                stringResource(R.string.settings_sound_effect_scope_bottom_bar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Startup Sound Toggle ---
        item(key = "multimedia_startup_sound") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Alarm,
                title = startupSoundTitle,
                description = if (SoundEffectConfig.isStartupSoundEnabled) {
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
            ExpressiveCard(
                flat = flat,
                onClick = { showStartupSourceDialogState.value = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Input, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = startupSourceTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) startupSourceLocal else startupSourcePreset,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Startup Sound: Select Local File (local source) ---
        item(key = "multimedia_select_startup_sound", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    try {
                        pickStartupSoundLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectStartupSoundTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (SoundEffectConfig.startupSoundFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = startupSoundSelectedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Startup Sound: Clear Startup Sound (local source with file) ---
        item(key = "multimedia_clear_startup_sound", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.startupSoundFilename != null) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    clearStartupSoundDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_startup_sound),
                        content = context.getString(R.string.settings_clear_startup_sound_confirm)
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = clearStartupSoundTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // --- Startup Sound: Preset Selector (preset source) ---
        item(key = "multimedia_startup_sound_preset", visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            ExpressiveCard(
                flat = flat,
                onClick = { showStartupPresetDialogState.value = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = startupPresetTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SoundEffectConfig.startupPresetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Vibration Toggle ---
        item(key = "multimedia_vibration") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Vibration,
                title = vibrationTitle,
                description = if (VibrationConfig.isVibrationEnabled) vibrationEnabledText else vibrationSummary,
                checked = VibrationConfig.isVibrationEnabled,
                onCheckedChange = {
                    VibrationConfig.setEnabledState(it)
                    VibrationConfig.save(context)
                }
            )
        }

        // --- Vibration: Scope Selector ---
        item(key = "multimedia_vibration_scope", visible = VibrationConfig.isVibrationEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    showVibrationScopeDialogState.value = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vibrationScopeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (VibrationConfig.scope == VibrationConfig.SCOPE_GLOBAL)
                                stringResource(R.string.settings_vibration_scope_global)
                            else
                                stringResource(R.string.settings_vibration_scope_bottom_bar),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Vibration: Intensity Slider ---
        item(key = "multimedia_vibration_intensity", visible = VibrationConfig.isVibrationEnabled) {
            SliderSettingCard(
                flat = flat,
                title = vibrationIntensityTitle,
                value = VibrationConfig.vibrationIntensity,
                onValueChange = {
                    VibrationConfig.setIntensityValue(it)
                },
                onValueChangeFinished = { VibrationConfig.save(context) },
            )
        }
    }

    // --- Dialogs ---
    MultimediaDialogs(
        showSoundEffectSourceDialog = showSoundEffectSourceDialogState,
        showSoundEffectPresetDialog = showSoundEffectPresetDialogState,
        showSoundEffectScopeDialog = showSoundEffectScopeDialogState,
        showStartupSourceDialog = showStartupSourceDialogState,
        showStartupPresetDialog = showStartupPresetDialogState,
        showVibrationScopeDialog = showVibrationScopeDialogState,
    )
}
