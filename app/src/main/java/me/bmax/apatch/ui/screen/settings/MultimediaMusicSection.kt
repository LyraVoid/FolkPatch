package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.MusicConfig
import me.bmax.apatch.util.MusicManager

@Composable
fun formatTime(millis: Int): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultimediaMusicSection(
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

    val currentPosition by MusicManager.currentPosition.collectAsStateWithLifecycle(initialValue = 0)
    val duration by MusicManager.duration.collectAsStateWithLifecycle(initialValue = 0)
    val isPlaying by MusicManager.isPlaying.collectAsStateWithLifecycle(initialValue = false)

    val clearMusicDialog = rememberConfirmDialog(
        onConfirm = {
            MusicConfig.clearMusic(context)
            MusicManager.stop()
            scope.launch {
                snackBarHost.showSnackbar(message = context.getString(R.string.settings_music_cleared))
            }
        }
    )


    SegmentedSection(title = stringResource(R.string.settings_section_multimedia_music)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        // --- Background Music Toggle ---
        item(key = "multimedia_bg_music") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.MusicNote,
                title = musicTitle,
                summary = if (MusicConfig.isMusicEnabled) {
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
            SegmentedValueItem(
                icon = Icons.Outlined.AudioFile,
                title = selectMusicTitle,
                summary = if (MusicConfig.musicFilename != null) musicSelectedText else null,
                onClick = {
                    try {
                        pickMusicLauncher.launch("audio/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                },
            )
        }

        // --- Music: Auto Play Toggle ---
        item(key = "multimedia_music_auto_play", visible = MusicConfig.isMusicEnabled) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.PlayArrow,
                title = autoPlayTitle,
                summary = autoPlaySummary,
                checked = MusicConfig.isAutoPlayEnabled,
                onCheckedChange = {
                    MusicConfig.setAutoPlayEnabledState(it)
                    MusicConfig.save(context)
                }
            )
        }

        // --- Music: Looping Toggle ---
        item(key = "multimedia_music_looping", visible = MusicConfig.isMusicEnabled) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Repeat,
                title = loopingTitle,
                summary = loopingSummary,
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
            SegmentedSliderItem(
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = FolkSettingsDimens.ItemHorizontalPadding,
                            vertical = FolkSettingsDimens.ItemVerticalPadding,
                        )
                ) {
                    Text(
                        text = playbackControlTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(FolkDimens.Size8))
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
                                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
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

        // --- Music: Clear Music ---
        item(key = "multimedia_clear_music", visible = MusicConfig.isMusicEnabled && MusicConfig.musicFilename != null) {
            SegmentedNavigationItem(
                icon = Icons.Outlined.Delete,
                title = clearMusicTitle,
                onClick = {
                    clearMusicDialog.showConfirm(
                        title = context.getString(R.string.settings_clear_music),
                        content = context.getString(R.string.settings_clear_music_confirm)
                    )
                },
            )
        }

        }
    }
}
