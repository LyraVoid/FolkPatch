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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
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
                    snackBarHost.showSnackbar(message = "Failed to save sound effect")
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
                    snackBarHost.showSnackbar(message = "Failed to save startup sound")
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
    val currentPosition by MusicManager.currentPosition.collectAsState(initial = 0)
    val duration by MusicManager.duration.collectAsState(initial = 0)
    val isPlaying by MusicManager.isPlaying.collectAsState(initial = false)

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
    var showSoundEffectSourceDialog by remember { mutableStateOf(false) }

    // Sound effect preset dialog
    val soundEffectPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    var showSoundEffectPresetDialog by remember { mutableStateOf(false) }

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
    var showSoundEffectScopeDialog by remember { mutableStateOf(false) }

    // Startup sound source dialog
    val startupSourceTitle = stringResource(id = R.string.settings_sound_effect_source)
    val startupSourceLocal = stringResource(id = R.string.settings_sound_effect_source_local)
    val startupSourcePreset = stringResource(id = R.string.settings_sound_effect_source_preset)
    var showStartupSourceDialog by remember { mutableStateOf(false) }

    // Startup sound preset dialog
    val startupPresetTitle = stringResource(id = R.string.settings_sound_effect_preset_title)
    var showStartupPresetDialog by remember { mutableStateOf(false) }

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
    var showVibrationScopeDialog by remember { mutableStateOf(false) }

    SplicedColumnGroup(flat = flat) {

        // --- Background Music Toggle ---
        item {
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
        item(visible = MusicConfig.isMusicEnabled) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (MusicConfig.musicFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = musicSelectedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Music: Auto Play Toggle ---
        item(visible = MusicConfig.isMusicEnabled) {
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
        item(visible = MusicConfig.isMusicEnabled) {
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
        item(visible = MusicConfig.isMusicEnabled) {
            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = musicVolumeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = MusicConfig.volume,
                        onValueChange = {
                            MusicConfig.setVolumeValue(it)
                            MusicManager.updateVolume(it)
                        },
                        onValueChangeFinished = { MusicConfig.save(context) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                        )
                    )
                }
            }
        }

        // --- Music: Playback Control ---
        item(visible = MusicConfig.isMusicEnabled && MusicConfig.musicFilename != null) {
            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = playbackControlTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
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
                                contentDescription = null
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
        item(visible = MusicConfig.isMusicEnabled && MusicConfig.musicFilename != null) {
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // --- Sound Effect Toggle ---
        item {
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
        item(visible = SoundEffectConfig.isSoundEffectEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = { showSoundEffectSourceDialog = true }
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) soundEffectSourceLocal else soundEffectSourcePreset,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Sound Effect: Select Local File (local source) ---
        item(visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (SoundEffectConfig.soundEffectFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = soundEffectSelectedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Sound Effect: Clear Sound Effect (local source with file) ---
        item(visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.soundEffectFilename != null) {
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // --- Sound Effect: Preset Selector (preset source) ---
        item(visible = SoundEffectConfig.isSoundEffectEnabled && SoundEffectConfig.sourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            ExpressiveCard(
                flat = flat,
                onClick = { showSoundEffectPresetDialog = true }
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SoundEffectConfig.presetName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Sound Effect: Scope Selector ---
        item(visible = SoundEffectConfig.isSoundEffectEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    showSoundEffectScopeDialog = true
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.scope == SoundEffectConfig.SCOPE_GLOBAL)
                                stringResource(R.string.settings_sound_effect_scope_global)
                            else
                                stringResource(R.string.settings_sound_effect_scope_bottom_bar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Startup Sound Toggle ---
        item {
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
        item(visible = SoundEffectConfig.isStartupSoundEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = { showStartupSourceDialog = true }
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) startupSourceLocal else startupSourcePreset,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Startup Sound: Select Local File (local source) ---
        item(visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (SoundEffectConfig.startupSoundFilename != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = startupSoundSelectedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // --- Startup Sound: Clear Startup Sound (local source with file) ---
        item(visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && SoundEffectConfig.startupSoundFilename != null) {
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // --- Startup Sound: Preset Selector (preset source) ---
        item(visible = SoundEffectConfig.isStartupSoundEnabled && SoundEffectConfig.startupSourceType == SoundEffectConfig.SOURCE_TYPE_PRESET) {
            ExpressiveCard(
                flat = flat,
                onClick = { showStartupPresetDialog = true }
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = SoundEffectConfig.startupPresetName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Vibration Toggle ---
        item {
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
        item(visible = VibrationConfig.isVibrationEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    showVibrationScopeDialog = true
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (VibrationConfig.scope == VibrationConfig.SCOPE_GLOBAL)
                                stringResource(R.string.settings_vibration_scope_global)
                            else
                                stringResource(R.string.settings_vibration_scope_bottom_bar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // --- Vibration: Intensity Slider ---
        item(visible = VibrationConfig.isVibrationEnabled) {
            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = vibrationIntensityTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = VibrationConfig.vibrationIntensity,
                        onValueChange = {
                            VibrationConfig.setIntensityValue(it)
                        },
                        onValueChangeFinished = { VibrationConfig.save(context) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                        )
                    )
                }
            }
        }
    }

    // --- Dialogs (outside SplicedColumnGroup) ---

    // Sound Effect Source Dialog
    if (showSoundEffectSourceDialog) {
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectSourceDialog = false },
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
                                    showSoundEffectSourceDialog = false
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
                                    showSoundEffectSourceDialog = false
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
                        TextButton(onClick = { showSoundEffectSourceDialog = false }) {
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
    if (showSoundEffectPresetDialog) {
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectPresetDialog = false },
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
                            items(SoundEffectConfig.PRESETS.size) { index ->
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
                                        showSoundEffectPresetDialog = false
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
                        TextButton(onClick = { showSoundEffectPresetDialog = false }) {
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
    if (showSoundEffectScopeDialog) {
        BasicAlertDialog(
            onDismissRequest = { showSoundEffectScopeDialog = false },
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
                                    showSoundEffectScopeDialog = false
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
                                    showSoundEffectScopeDialog = false
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
                        TextButton(onClick = { showSoundEffectScopeDialog = false }) {
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
    if (showStartupSourceDialog) {
        BasicAlertDialog(
            onDismissRequest = { showStartupSourceDialog = false },
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
                                    showStartupSourceDialog = false
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
                                    showStartupSourceDialog = false
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
                        TextButton(onClick = { showStartupSourceDialog = false }) {
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
    if (showStartupPresetDialog) {
        BasicAlertDialog(
            onDismissRequest = { showStartupPresetDialog = false },
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
                            items(SoundEffectConfig.STARTUP_PRESETS.size) { index ->
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
                                        showStartupPresetDialog = false
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
                        TextButton(onClick = { showStartupPresetDialog = false }) {
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
    if (showVibrationScopeDialog) {
        BasicAlertDialog(
            onDismissRequest = { showVibrationScopeDialog = false },
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
                                    showVibrationScopeDialog = false
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
                                    showVibrationScopeDialog = false
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
                        TextButton(onClick = { showVibrationScopeDialog = false }) {
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
