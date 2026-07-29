package me.bmax.apatch.ui.screen.settings.appearance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.SliderSettingCard
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.showToast

@Composable
fun AppearanceBannerSection(
    flat: Boolean,
    highlightKey: String?,
    onNavigateToApiMarketplace: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SplicedColumnGroup(title = stringResource(R.string.settings_appearance_banner), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_banner") {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.Campaign,
                title = stringResource(id = R.string.apm_enable_module_banner),
                description = stringResource(id = R.string.apm_enable_module_banner_summary),
                checked = BackgroundConfig.isBannerEnabled,
                onCheckedChange = {
                    BackgroundConfig.setBannerEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isBannerEnabled) {
            item(key = "appearance_folk_banner") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Image,
                    title = stringResource(id = R.string.apm_enable_folk_banner),
                    description = stringResource(id = R.string.apm_enable_folk_banner_summary),
                    checked = BackgroundConfig.isFolkBannerEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setFolkBannerEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isFolkBannerEnabled) {
                item(key = "appearance_banner_api_mode") {
                    ToggleSettingCard(
                        flat = flat,
                        icon = Icons.Filled.Api,
                        title = stringResource(id = R.string.apm_banner_api_mode),
                        description = stringResource(id = R.string.apm_banner_api_mode_summary),
                        checked = BackgroundConfig.isBannerApiModeEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setBannerApiModeEnabledState(it)
                            BackgroundConfig.save(context)
                        },
                    )
                }

                if (BackgroundConfig.isBannerApiModeEnabled) {
                    item(key = "appearance_banner_api_source") {
                        val showBannerApiConfigDialog = remember { mutableStateOf(false) }
                        val apiSourceSummary = if (BackgroundConfig.bannerApiSource.isNotBlank()) {
                            if (BackgroundConfig.bannerApiSource.startsWith("/")) {
                                context.getString(R.string.apm_banner_local_dir_configured)
                            } else {
                                context.getString(R.string.apm_banner_api_url_configured)
                            }
                        } else {
                            context.getString(R.string.apm_banner_api_source_not_configured)
                        }

                        Column {
                            ExpressiveCard(flat = flat, onClick = { showBannerApiConfigDialog.value = true }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(imageVector = Icons.Filled.Api, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = R.string.apm_banner_api_source),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = apiSourceSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }

                            if (showBannerApiConfigDialog.value) {
                                BannerApiConfigDialog(
                                    showDialog = showBannerApiConfigDialog,
                                    currentSource = BackgroundConfig.bannerApiSource,
                                    onConfirm = { newSource ->
                                        BackgroundConfig.setBannerApiSourceValue(newSource)
                                        BackgroundConfig.save(context)
                                    },
                                    onClearCache = {
                                        scope.launch {
                                            loadingDialog.show()
                                            me.bmax.apatch.ui.screen.BannerApiService.clearAllCache(context)
                                            loadingDialog.hide()
                                            showToast(context, context.getString(R.string.apm_banner_cache_cleared))
                                        }
                                    }
                                )
                            }

                            ExpressiveCard(flat = flat, onClick = { onNavigateToApiMarketplace() }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(imageVector = Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(id = R.string.apm_api_marketplace_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "appearance_banner_opacity") {
                ToggleSettingCard(
                    flat = flat,
                    icon = Icons.Filled.Opacity,
                    title = stringResource(id = R.string.settings_banner_custom_opacity),
                    description = stringResource(id = R.string.settings_banner_custom_opacity_summary),
                    checked = BackgroundConfig.isBannerCustomOpacityEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerCustomOpacityEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isBannerCustomOpacityEnabled) {
                item(key = "appearance_banner_opacity_slider") {
                    SliderSettingCard(
                        flat = flat,
                        title = stringResource(id = R.string.settings_banner_opacity),
                        value = BackgroundConfig.bannerCustomOpacity,
                        onValueChange = { BackgroundConfig.setBannerCustomOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                    )
                }
            }
        }
    }
}
