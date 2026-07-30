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
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    if (onClick != null) {
        Surface(onClick = onClick) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
                        imageVector = Icons.Filled.Download,
                        contentDescription = downloadContentDescription,
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
