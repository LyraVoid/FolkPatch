package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkType
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

@Composable
internal fun HomeCountCard(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    available: Boolean = true,
) {
    val supporting = LocalFolkWallpaperTokens.current?.group?.supportingColor
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    TonalCard(
        modifier = modifier,
        onClick = onClick.takeIf { available },
        flat = true,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(
                horizontal = FolkDimens.Size20,
                vertical = FolkDimens.Size16,
            ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FolkDimens.Size20),
                    tint = supporting,
                )
                Spacer(Modifier.width(FolkDimens.Size16))
                Column {
                    Text(
                        text = label,
                        style = FolkType.Caption,
                        color = supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(FolkDimens.Size4))
                    Text(
                        text = if (available) count.toString() else "—",
                        style = FolkType.Title,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
