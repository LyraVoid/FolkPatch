package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.misc.BannerApiService

import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.ModuleLabel
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole
import me.bmax.apatch.ui.theme.bannerFadeColor
import androidx.compose.material3.ButtonDefaults
import me.bmax.apatch.util.kpmBannerStorage
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.util.kpmCustomModuleInfoStorage
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.ModuleInfoData
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun KPModuleItem(
    module: KPModel.KPMInfo,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    showMoreModuleInfo: Boolean,
    simpleListBottomBar: Boolean,
    foldSystemModule: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    isEmbedded: Boolean? = null
) {
    val moduleAuthor = stringResource(id = R.string.kpm_author)
    val moduleArgs = stringResource(id = R.string.kpm_args)
    val decoration = TextDecoration.None
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val folkBannerTitle = stringResource(R.string.apm_folk_banner_title)
    val folkBannerSelect = stringResource(R.string.apm_folk_banner_select)
    val folkBannerClear = stringResource(R.string.apm_folk_banner_clear)
    val folkBannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val folkBannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val folkBannerFailed = stringResource(R.string.apm_folk_banner_failed)
    var showFolkBannerDialog by remember { mutableStateOf(false) }
    var hasFolkBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by remember { mutableStateOf(0) }
    val customInfoReloadKeyState = remember { mutableStateOf(0) }
    var customInfoReloadKey by customInfoReloadKeyState
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                kpmBannerStorage.read(module.name) != null
            }
        }
    }

    val pickFolkBannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val result = withContext(Dispatchers.IO) {
                    runCatching { kpmBannerStorage.write(context, module.name, it) }.getOrNull()
                }
                loadingDialog.hide()
                val message = if (result != null) {
                    bannerReloadKey++
                    folkBannerSaved.format(module.name)
                } else {
                    folkBannerFailed.format(module.name)
                }
                showToast(context, message)
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
    }
    
    val wallpaper = LocalFolkWallpaperTokens.current
    val cardColor = if (wallpaper != null) {
        wallpaper.material(WallpaperSurfaceRole.Group, minimumAlpha = 0.35f).fill
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    val bannerImageAlpha = if (BackgroundConfig.isBannerCustomOpacityEnabled) {
        BackgroundConfig.bannerCustomOpacity
    } else {
        if (isWallpaperMode) {
            (0.35f + (opacity - 0.2f) * 0.5f).coerceIn(0.25f, 0.6f)
        } else {
            0.18f
        }
    }

    val cachedBanner = if (BackgroundConfig.isBannerApiModeEnabled && BackgroundConfig.getEffectiveBannerApiSource().isNotBlank()) {
        BannerApiService.loadSync(context, "kpm_${module.name}", BackgroundConfig.getEffectiveBannerApiSource())
    } else null

    val bannerData by produceState<ByteArray?>(
        initialValue = cachedBanner,
        module.name,
        BackgroundConfig.isBannerEnabled,
        BackgroundConfig.isFolkBannerEnabled,
        BackgroundConfig.isBannerApiModeEnabled,
        BackgroundConfig.bannerApiSource,
        bannerReloadKey
    ) {
        if (!BackgroundConfig.isBannerEnabled) {
            value = null
            return@produceState
        }

        KPModuleViewModel.bannerSemaphore.withPermit {
            val effectiveApiSource = BackgroundConfig.getEffectiveBannerApiSource()

        if (BackgroundConfig.isBannerApiModeEnabled && effectiveApiSource.isNotBlank()) {
            val apiBanner = withContext(Dispatchers.IO) {
                BannerApiService.getModuleBanner(
                    context = context,
                    moduleId = "kpm_${module.name}",
                    source = effectiveApiSource
                )
            }
            if (apiBanner != null) {
                value = apiBanner
                return@produceState
            }
        }

        value = if (BackgroundConfig.isFolkBannerEnabled) {
            withContext(Dispatchers.IO) { kpmBannerStorage.read(module.name) }
        } else {
            null
        }
        }
    }

    val customInfo by produceState(initialValue = null as CustomModuleInfo?, key1 = module.name, key2 = customInfoReloadKey) {
        value = withContext(Dispatchers.IO) {
            kpmCustomModuleInfoStorage.read(module.name)
        }
    }

    val insideSplicedGroup = me.bmax.apatch.ui.component.LocalInsideSplicedGroup.current

    val cardShape = FolkShape.Corner20

    val cardInteractionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
        .navOrigin(
            cornerRadius = FolkDimens.Size20,
            color = cardColor,
            interactionSource = cardInteractionSource,
        )
        .folkPressScale(cardInteractionSource)
        .combinedClickable(
            interactionSource = cardInteractionSource,
            indication = null,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (foldSystemModule) {
                    onExpandToggle()
                }
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showFolkBannerDialog = true
            }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bannerData != null) {
                val fadeColor = bannerFadeColor()

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bannerData)
                            .build(),
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
                        val hasAnyLabel = showMoreModuleInfo || isEmbedded != null
                        if (hasAnyLabel) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size6),
                                modifier = Modifier.padding(bottom = FolkDimens.Size8)
                            ) {
                                 if (showMoreModuleInfo) {
                                     ModuleLabel(
                                        text = "KPM",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                     )

                                     if (module.args.isNotBlank()) {
                                         ModuleLabel(
                                            text = "Args",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                         )
                                     }
                                 }

                                 isEmbedded?.let { embedded ->
                                     ModuleLabel(
                                        text = stringResource(if (embedded) R.string.kpm_embedded else R.string.kpm_loaded),
                                        containerColor = if (embedded) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        contentColor = if (embedded) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                     )
                                 }
                            }
                        }
                    
                        Text(
                            text = customInfo?.name?.takeIf { it.isNotBlank() } ?: module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = decoration
                        )

                        Text(
                            text = customInfo?.version?.takeIf { it.isNotBlank() } ?: module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )

                        Text(
                            text = customInfo?.author?.takeIf { it.isNotBlank() } ?: module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )
                        
                        if (showMoreModuleInfo && module.args.isNotBlank()) {
                             Text(
                                text = "$moduleArgs: ${module.args}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = decoration,
                                modifier = Modifier.padding(top = FolkDimens.Size2)
                            )
                        }
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (simpleListBottomBar) FolkDimens.Size12 else FolkDimens.Size8)
                    ) {
                        FilledTonalButton(
                            onClick = { onControl(module) },
                            enabled = true,
                            contentPadding = if (simpleListBottomBar) PaddingValues(FolkDimens.Size12) else PaddingValues(horizontal = FolkDimens.Size12),
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(FolkDimens.Size36),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(FolkDimens.Size20),
                                painter = painterResource(id = R.drawable.settings),
                                contentDescription = stringResource(id = R.string.kpm_control)
                            )
                            if (!simpleListBottomBar) {
                                Spacer(modifier = Modifier.width(FolkDimens.Size8))
                                Text(stringResource(id = R.string.kpm_control))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (module.installed && module.loadSource != "embedded") {
                            ExpressiveSwitch(checked = !module.disabled, onCheckedChange = onToggle)
                            Spacer(modifier = Modifier.width(FolkDimens.Size8))
                        }

                        FilledTonalButton(
                            onClick = { onUninstall(module) },
                            enabled = true,
                            contentPadding = if (simpleListBottomBar) PaddingValues(FolkDimens.Size12) else PaddingValues(horizontal = FolkDimens.Size12),
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(FolkDimens.Size36),
                            colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) else ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(FolkDimens.Size20),
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = stringResource(id = R.string.kpm_unload)
                            )
                            if (!simpleListBottomBar) {
                                Spacer(modifier = Modifier.width(FolkDimens.Size8))
                                Text(stringResource(id = R.string.kpm_unload))
                            }
                        }
                    }
                }
            }
        }
    }

    // Render: inside spliced group → no Surface wrapper; standalone → Surface card
    if (insideSplicedGroup) {
        Box(modifier = modifier.then(clickModifier)) {
            contentBlock()
        }
    } else {
        Surface(
            modifier = modifier
                .clip(cardShape)
                .then(clickModifier),
            shape = cardShape,
            color = cardColor,
            tonalElevation = FolkDimens.Size0
        ) {
            contentBlock()
        }
    }

    // 自定义模块信息状态
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    // 弹窗打开时加载自定义信息
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            val info = withContext(Dispatchers.IO) {
                kpmCustomModuleInfoStorage.read(module.name)
            }
            customName = info?.name?.takeIf { it.isNotBlank() } ?: module.name
            customVersion = info?.version?.takeIf { it.isNotBlank() } ?: module.version
            customAuthor = info?.author?.takeIf { it.isNotBlank() } ?: module.author
            customDescription = info?.description?.takeIf { it.isNotBlank() } ?: module.description
        }
    }

    val customInfoTitle = stringResource(R.string.folk_banner_custom_info_title)
    val customInfoNameLabel = stringResource(R.string.folk_banner_custom_info_name)
    val customInfoVersionLabel = stringResource(R.string.folk_banner_custom_info_version)
    val customInfoAuthorLabel = stringResource(R.string.folk_banner_custom_info_author)
    val customInfoDescriptionLabel = stringResource(R.string.folk_banner_custom_info_description)
    val customInfoSaveLabel = stringResource(R.string.folk_banner_custom_info_save)
    val customInfoResetLabel = stringResource(R.string.folk_banner_custom_info_reset)
    val customInfoSavedMsg = stringResource(R.string.folk_banner_custom_info_saved)
    val customInfoResetMsg = stringResource(R.string.folk_banner_custom_info_reset_done)

    BackgroundOptionsDialog(
        showDialog = showFolkBannerDialog,
        onDismiss = { showFolkBannerDialog = false },
        title = folkBannerTitle,
        showBannerSection = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled,
        selectLabel = folkBannerSelect,
        clearLabel = folkBannerClear,
        hasExisting = hasFolkBanner,
        onSelectImage = {
            pickFolkBannerLauncher.launch("image/*")
        },
        onClearImage = {
            scope.launch {
                loadingDialog.show()
                val success = withContext(Dispatchers.IO) {
                    runCatching { kpmBannerStorage.clear(module.name) }.getOrDefault(false)
                }
                loadingDialog.hide()
                val message = if (success) {
                    bannerReloadKey++
                    folkBannerCleared.format(module.name)
                } else {
                    folkBannerFailed.format(module.name)
                }
                showToast(context, message)
            }
        },
        customInfoTitle = customInfoTitle,
        customInfoNameLabel = customInfoNameLabel,
        customInfoVersionLabel = customInfoVersionLabel,
        customInfoAuthorLabel = customInfoAuthorLabel,
        customInfoDescriptionLabel = customInfoDescriptionLabel,
        saveLabel = customInfoSaveLabel,
        resetLabel = customInfoResetLabel,
        initialModuleInfo = ModuleInfoData(
            name = customName,
            version = customVersion,
            author = customAuthor,
            description = customDescription
        ),
        hasSavedCustomInfo = customInfo?.hasAnyInfo() == true,
        customInfoReloadKey = customInfoReloadKeyState,
        onSaveModuleInfo = { info ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    kpmCustomModuleInfoStorage.write(module.name, CustomModuleInfo(
                        name = info.name.takeIf { it.isNotBlank() },
                        version = info.version.takeIf { it.isNotBlank() },
                        author = info.author.takeIf { it.isNotBlank() },
                        description = info.description.takeIf { it.isNotBlank() },
                    ))
                }
                showToast(context, customInfoSavedMsg.format(module.name))
            }
        },
        onResetModuleInfo = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    kpmCustomModuleInfoStorage.clear(module.name)
                }
                showToast(context, customInfoResetMsg.format(module.name))
            }
        }
    )
}
