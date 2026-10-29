package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 310.dp,
    shape: Shape = FolkShape.Dialog,
    blurBehind: Boolean = true,
    dialogProperties: DialogProperties = DialogProperties(
        decorFitsSystemWindows = true,
        usePlatformDefaultWidth = false,
    ),
    content: @Composable () -> Unit,
) {
    val wallpaper = LocalFolkWallpaperTokens.current
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        Surface(
            modifier = modifier
                .width(width)
                .wrapContentHeight(),
            shape = shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = wallpaper?.overlay?.fill ?: FolkTheme.palette.groupedInset,
            contentColor = wallpaper?.overlay?.contentColor ?: MaterialTheme.colorScheme.onSurface,
        ) {
            content()

            if (blurBehind) {
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }
}
