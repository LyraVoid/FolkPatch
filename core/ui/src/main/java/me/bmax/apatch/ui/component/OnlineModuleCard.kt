package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.component.folk.LocalWallpaperSupportingColor
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

@Composable
fun OnlineModuleCard(
    name: String,
    version: String,
    description: String,
    typeLabel: String,
    versionLabel: String,
    capabilityLabel: String? = null,
    downloadContentDescription: String,
    onDownload: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val wallpaper = LocalFolkWallpaperTokens.current
    if (onClick != null) {
        Surface(
            onClick = onClick,
            color = if (wallpaper != null) Color.Transparent else FolkTheme.palette.groupedSurface,
        ) {
            OnlineModuleCardContent(
                name = name,
                version = version,
                description = description,
                typeLabel = typeLabel,
                versionLabel = versionLabel,
                capabilityLabel = capabilityLabel,
                downloadContentDescription = downloadContentDescription,
                onDownload = onDownload,
            )
        }
    } else {
        OnlineModuleCardContent(
            name = name,
            version = version,
            description = description,
            typeLabel = typeLabel,
            versionLabel = versionLabel,
            capabilityLabel = capabilityLabel,
            downloadContentDescription = downloadContentDescription,
            onDownload = onDownload,
        )
    }
}

@Composable
private fun OnlineModuleCardContent(
    name: String,
    version: String,
    description: String,
    typeLabel: String,
    versionLabel: String,
    capabilityLabel: String? = null,
    downloadContentDescription: String,
    onDownload: () -> Unit,
) {
    val wallpaper = LocalFolkWallpaperTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = wallpaper?.group?.fill ?: FolkTheme.palette.groupedSurface,
            contentColor = wallpaper?.group?.contentColor ?: MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModuleLabel(
                            text = typeLabel,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        ModuleLabel(
                            text = "$versionLabel $version",
                            containerColor = folkInsetColor(),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        capabilityLabel?.let {
                            ModuleLabel(
                                text = it,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(12.dp))

                FilledTonalIconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = downloadContentDescription,
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalWallpaperSupportingColor.current
                    ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
