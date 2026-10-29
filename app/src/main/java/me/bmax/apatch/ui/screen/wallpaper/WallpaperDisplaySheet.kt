package me.bmax.apatch.ui.screen.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedChoiceRow
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.wallpaper.WallpaperDevice

/** Bottom sheet holding the gallery's display scheme: device pool + layout style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WallpaperDisplaySheet(
    device: WallpaperDevice,
    layoutIndex: Int,
    onDeviceSelected: (WallpaperDevice) -> Unit,
    onLayoutSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val devices = WallpaperDevice.entries
    val layoutLabels = listOf(
        stringResource(R.string.wallpaper_layout_waterfall),
        stringResource(R.string.wallpaper_layout_stack),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FolkSettingsDimens.ScreenPadding)
                .padding(bottom = FolkSettingsDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(FolkSettingsDimens.SectionTitleSpacing),
        ) {
            Text(
                text = stringResource(R.string.wallpaper_display_options),
                style = MaterialTheme.typography.titleMedium,
            )
            SegmentedChoiceRow(
                items = devices.map { stringResource(it.labelRes) },
                selectedIndex = devices.indexOf(device),
                onItemSelection = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDeviceSelected(devices[index])
                },
            )
            Text(
                text = stringResource(R.string.wallpaper_display_layout),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedChoiceRow(
                items = layoutLabels,
                selectedIndex = layoutIndex,
                onItemSelection = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLayoutSelected(index)
                },
            )
        }
    }
}
