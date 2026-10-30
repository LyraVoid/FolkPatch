package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.folk.folkGroupColor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem

@Composable
fun AppearanceBannerSection(
    flat: Boolean,
    highlightKey: String?,
    onNavigateToApiMarketplace: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SegmentedColumn(title = stringResource(R.string.settings_appearance_banner), flat = flat, highlightKey = highlightKey) {
        item(key = "appearance_banner") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Campaign,
                title = stringResource(id = R.string.apm_enable_module_banner),
                summary = stringResource(id = R.string.apm_enable_module_banner_summary),
                checked = BackgroundConfig.isBannerEnabled,
                onCheckedChange = {
                    BackgroundConfig.setBannerEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isBannerEnabled) {
            item(key = "appearance_folk_banner") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Image,
                    title = stringResource(id = R.string.apm_enable_folk_banner),
                    summary = stringResource(id = R.string.apm_enable_folk_banner_summary),
                    checked = BackgroundConfig.isFolkBannerEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setFolkBannerEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isFolkBannerEnabled) {
                item(key = "appearance_banner_api_mode") {
                    SegmentedSwitchItem(
                        icon = Icons.Outlined.Api,
                        title = stringResource(id = R.string.apm_banner_api_mode),
                        summary = stringResource(id = R.string.apm_banner_api_mode_summary),
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
                            SegmentedValueItem(
                icon = Icons.Outlined.Api,
                title = stringResource(id = R.string.apm_banner_api_source),
                summary = apiSourceSummary,
                onClick = { showBannerApiConfigDialog.value = true },
            )

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
                                            me.bmax.apatch.ui.screen.misc.BannerApiService.clearAllCache(context)
                                            loadingDialog.hide()
                                            showToast(context, context.getString(R.string.apm_banner_cache_cleared))
                                        }
                                    }
                                )
                            }

                            SegmentedValueItem(
                icon = Icons.Outlined.Store,
                title = stringResource(id = R.string.apm_api_marketplace_title),
                modifier = androidx.compose.ui.Modifier.navOrigin(color = folkGroupColor()),
                onClick = { onNavigateToApiMarketplace() },
            )
                        }
                    }
                }
            }

            item(key = "appearance_banner_opacity") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Opacity,
                    title = stringResource(id = R.string.settings_banner_custom_opacity),
                    summary = stringResource(id = R.string.settings_banner_custom_opacity_summary),
                    checked = BackgroundConfig.isBannerCustomOpacityEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerCustomOpacityEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            if (BackgroundConfig.isBannerCustomOpacityEnabled) {
                item(key = "appearance_banner_opacity_slider") {
                    SegmentedSliderItem(
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
