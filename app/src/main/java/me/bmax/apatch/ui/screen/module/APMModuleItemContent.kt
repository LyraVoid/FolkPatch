package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material3.MaterialTheme
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.ModuleLabel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.AdaptiveModuleButtonRow
import me.bmax.apatch.ui.component.ModuleButtonConfig
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import androidx.compose.material.icons.Icons
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.ui.theme.bannerFadeColor

@Composable
fun ModuleItemContent(
    module: APModuleViewModel.ModuleInfo,
    isChecked: Boolean,
    updateUrl: String,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    enableModuleShortcutAdd: Boolean,
    expanded: Boolean,
    opacity: Float,
    bannerInfo: APModuleViewModel.BannerInfo?,
    bannerImageAlpha: Float,
    isWallpaperMode: Boolean,
    sizeStr: String,
    customInfo: CustomModuleInfo?,
    onCheckChanged: (Boolean) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onUndoUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onNavigateAction: (APModuleViewModel.ModuleInfo) -> Unit,
    onAddShortcut: (APModuleViewModel.ModuleInfo) -> Unit,
) {
    val context = LocalContext.current
    val shortcutAdd = stringResource(id = R.string.module_shortcut_add)
    Box(modifier = Modifier.fillMaxWidth()) {
        val bannerUrl = bannerInfo?.url
        val bannerData = bannerInfo?.bytes
        val hasBannerUrl = !bannerUrl.isNullOrEmpty()
        if (bannerData != null || hasBannerUrl) {
            val fadeColor = bannerFadeColor()

            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = if (hasBannerUrl) {
                        bannerUrl
                    } else {
                        ImageRequest.Builder(context)
                            .data(bannerData)
                            .build()
                     },
                     contentDescription = null,
                     modifier = Modifier.fillMaxSize(),
                     contentScale = ContentScale.Crop,
                     alpha = bannerImageAlpha
                 )
                val gradientAlpha = if (isWallpaperMode) 0.5f else 0.8f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    fadeColor.copy(alpha = 0.0f),
                                    fadeColor.copy(alpha = gradientAlpha)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FolkDimens.Size16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val hasAnyLabel = showMoreModuleInfo || module.remove || (updateUrl.isNotEmpty() && !module.update) || module.update
                    if (hasAnyLabel) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size6),
                            modifier = Modifier.padding(bottom = FolkDimens.Size8)
                        ) {
                            if (showMoreModuleInfo) {
                                ModuleLabel(
                                    text = sizeStr,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                ModuleLabel(
                                    text = module.id,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (module.remove) {
                                ModuleLabel(
                                    text = stringResource(R.string.apm_remove),
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else if (updateUrl.isNotEmpty() && !module.update) {
                                ModuleLabel(
                                    text = stringResource(R.string.apm_update),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            } else if (module.update) {
                                ModuleLabel(
                                    text = "Updated",
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            
                            if (showMoreModuleInfo && module.hasWebUi && module.enabled && !module.remove) {
                                ModuleLabel(
                                    text = "WebUI",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (showMoreModuleInfo && module.hasActionScript && module.enabled && !module.remove) {
                                ModuleLabel(
                                    text = "Action",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            if (module.isMetamodule && !module.remove) {
                                ModuleLabel(
                                    text = "META",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = customInfo?.name?.takeIf { it.isNotBlank() } ?: module.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                    )

                    Text(
                        text = customInfo?.version?.takeIf { it.isNotBlank() } ?: module.version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                    )

                    Text(
                        text = customInfo?.author?.takeIf { it.isNotBlank() } ?: module.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                ExpressiveSwitch(
                    enabled = !module.update,
                    checked = isChecked,
                    onCheckedChange = onCheckChanged
                )
            }

            Spacer(modifier = Modifier.height(FolkDimens.Size12))

            Text(
                text = customInfo?.description?.takeIf { it.isNotBlank() } ?: module.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(FolkDimens.Size16))

            AnimatedVisibility(
                visible = !foldSystemModule || expanded,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
       
                val buttons = mutableListOf<ModuleButtonConfig>()
                
                if (module.hasWebUi && module.enabled && !module.remove) {
                    buttons.add(ModuleButtonConfig(
                        icon = Icons.AutoMirrored.Outlined.Wysiwyg,
                        text = stringResource(R.string.apm_webui_open),
                        contentDescription = stringResource(R.string.apm_webui_open),
                        onClick = { onClick(module) }
                    ))
                }
                
                if (module.hasActionScript && module.enabled && !module.remove) {
                    buttons.add(ModuleButtonConfig(
                        icon = Icons.Outlined.Terminal,
                        text = stringResource(R.string.apm_action),
                        contentDescription = stringResource(R.string.apm_action),
                        onClick = { onNavigateAction(module) }
                    ))
                }

                val hasUpdateButton = updateUrl.isNotEmpty() && !module.remove && !module.update
                
                if (enableModuleShortcutAdd && module.enabled && !module.remove && (module.hasWebUi || module.hasActionScript) && !hasUpdateButton) {
                    buttons.add(ModuleButtonConfig(
                        icon = Icons.Outlined.Add,
                        text = shortcutAdd,
                        contentDescription = shortcutAdd,
                        onClick = { onAddShortcut(module) }
                    ))
                }
                
                if (hasUpdateButton) {
                    buttons.add(ModuleButtonConfig(
                        icon = Icons.Outlined.Download,
                        text = stringResource(R.string.apm_update),
                        contentDescription = stringResource(R.string.apm_update),
                        onClick = { onUpdate(module) }
                    ))
                }
                

                val deleteButton = ModuleButtonConfig(
                    icon = if (module.remove) Icons.Outlined.Restore else Icons.Outlined.Delete,
                    text = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                    contentDescription = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                    onClick = {
                        if (module.remove) {
                            onUndoUninstall(module)
                        } else {
                            onUninstall(module)
                        }
                    },
                    colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) else if (module.remove) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) else ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                
                AdaptiveModuleButtonRow(
                    buttons = buttons,
                    trailingButton = deleteButton,
                    simpleListBottomBar = simpleListBottomBar,
                    spacing = if (simpleListBottomBar) 12 else 8,
                    opacity = opacity
                )
            }
        }
    }
}
