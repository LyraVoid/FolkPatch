package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.misc.BannerApiService

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.ui.draw.clip
import com.topjohnwu.superuser.io.SuFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.ModuleInfoData
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.ModuleShortcut
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.apmBannerStorage
import me.bmax.apatch.util.resolveModuleDir
import me.bmax.apatch.util.readModulePropBanner
import me.bmax.apatch.util.clearLegacyFolkBanner
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.util.apmCustomModuleInfoStorage
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState

@Composable
fun ModuleItem(
    navigator: DestinationsNavigator,
    module: APModuleViewModel.ModuleInfo,
    isChecked: Boolean,
    updateUrl: String,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    enableModuleShortcutAdd: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onUndoUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val context = LocalContext.current
    val viewModel = viewModel<APModuleViewModel>()
    val snackBarHost = LocalSnackbarHost.current
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
    var bannerReloadKey by rememberSaveable(module.id) { mutableStateOf(0) }
    val customInfoReloadKeyState = remember { mutableStateOf(0) }
    var customInfoReloadKey by customInfoReloadKeyState
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                apmBannerStorage.read(module.id) != null
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
                    runCatching {
                        val data = apmBannerStorage.write(context, module.id, it)
                        if (data != null) {
                            runCatching {
                                val rootShell = getRootShell(true)
                                val resolvedDir = resolveModuleDir(rootShell, module.id)
                                clearLegacyFolkBanner(rootShell, resolvedDir)
                            }
                        }
                        data
                    }.getOrNull()
                }
                loadingDialog.hide()
                if (result != null) {
                    viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(result, null))
                    bannerReloadKey++
                    snackBarHost.showSnackbar(folkBannerSaved.format(module.name))
                } else {
                    snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                }
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
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
    
    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by rememberSaveable(module.id) { mutableStateOf(module.name) }
    var shortcutIconUri by remember { mutableStateOf<String?>(null) }
    var shortcutType by rememberSaveable(module.id) { mutableStateOf(if (module.hasWebUi) "webui" else "action") }
    val appIcon = remember(context) { context.packageManager.getApplicationIcon(context.packageName) }
    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        shortcutIconUri = uri?.toString()
    }

    fun toSuIconUri(path: String?): String? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("su://", true)) trimmed else "su://$trimmed"
    }

    val moduleDefaultIconPath = remember(
        module.id,
        shortcutType,
        module.webuiIcon,
        module.actionIcon
    ) {
        val preferred = if (shortcutType == "webui") module.webuiIcon else module.actionIcon
        preferred?.takeIf { it.isNotBlank() }
            ?: module.webuiIcon?.takeIf { it.isNotBlank() }
            ?: module.actionIcon?.takeIf { it.isNotBlank() }
    }
    val moduleDefaultIconUri = remember(moduleDefaultIconPath) { toSuIconUri(moduleDefaultIconPath) }
    val effectiveShortcutIconUri = shortcutIconUri ?: moduleDefaultIconUri

    val shortcutPreviewBitmap by produceState<Bitmap?>(initialValue = null, key1 = if (showShortcutDialog) effectiveShortcutIconUri else null) {
        if (!showShortcutDialog || effectiveShortcutIconUri.isNullOrBlank()) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, effectiveShortcutIconUri)
            }
        }
    }
    
    val customInfo by produceState(initialValue = null as CustomModuleInfo?, key1 = module.id, key2 = customInfoReloadKey) {
        value = withContext(Dispatchers.IO) {
            apmCustomModuleInfoStorage.read(module.id)
        }
    }

    val sizeStr = if (showMoreModuleInfo) viewModel.getModuleSize(module.id) else "0 KB"

    val bannerInfo by produceState<APModuleViewModel.BannerInfo?>(
        initialValue = viewModel.getBannerInfo(module.id),
        module.id,
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

        viewModel.bannerSemaphore.withPermit {
            val effectiveApiSource = BackgroundConfig.getEffectiveBannerApiSource()

        if (BackgroundConfig.isBannerApiModeEnabled && effectiveApiSource.isNotBlank()) {
            val apiBanner = withContext(Dispatchers.IO) {
                BannerApiService.getModuleBanner(
                    context = context,
                    moduleId = module.id,
                    source = effectiveApiSource
                )
            }
            if (apiBanner != null) {
                viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(apiBanner, null))
                value = APModuleViewModel.BannerInfo(apiBanner, null)
                return@produceState
            }
        }

        val cached = viewModel.getBannerInfo(module.id)
        if (cached != null && (cached.bytes != null || cached.url != null)) {
            value = cached
            return@produceState
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val folkBanner = if (BackgroundConfig.isFolkBannerEnabled) apmBannerStorage.read(module.id) else null
                if (folkBanner != null) {
                    return@withContext APModuleViewModel.BannerInfo(folkBanner, null)
                }
                val rootShell = getRootShell(true)
                val suFile = { path: String ->
                    SuFile(path).apply { shell = rootShell }
                }
                val resolvedDir = resolveModuleDir(rootShell, module.id)
                val propBanner = readModulePropBanner(rootShell, resolvedDir)

                if (!propBanner.isNullOrEmpty() && propBanner.startsWith("http", true)) {
                    return@withContext APModuleViewModel.BannerInfo(null, propBanner)
                }

                val candidates = buildList {
                    if (!propBanner.isNullOrEmpty()) {
                        add(propBanner)
                    }
                    addAll(listOf("banner", "banner.png", "banner.jpg", "banner.jpeg", "banner.webp"))
                }.distinct()

                for (name in candidates) {
                    val file = if (name.startsWith("/")) {
                        suFile(name)
                    } else {
                        suFile("$resolvedDir/$name")
                    }
                    if (file.exists()) {
                        return@withContext APModuleViewModel.BannerInfo(file.newInputStream().use { it.readBytes() }, null)
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

        if (loaded != null) {
            viewModel.putBannerInfo(module.id, loaded)
            value = loaded
        } else if (cached != null) {
            value = cached
        } else {
            viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(null, null))
            value = APModuleViewModel.BannerInfo(null, null)
        }
        }
    }

    val insideSplicedGroup = LocalInsideSplicedGroup.current

    val wallpaper = LocalFolkWallpaperTokens.current
    val cardColor = if (wallpaper != null) {
        wallpaper.material(WallpaperSurfaceRole.Group, minimumAlpha = 0.35f).fill
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val cardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardCorner by animateDpAsState(
        targetValue = when {
            expanded -> FolkDimens.Size28
            cardPressed -> FolkDimens.Size24
            else -> FolkDimens.Size20
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "moduleCardCorner",
    )
    val cardShape = ContinuousCornerShape(cardCorner)

    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
        .navOrigin(
            cornerRadius = cardCorner,
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
                } else {
                    onClick(module)
                }
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showFolkBannerDialog = true
            }
        )

    val contentBlock: @Composable () -> Unit = {
        ModuleItemContent(
            module = module,
            isChecked = isChecked,
            updateUrl = updateUrl,
            showMoreModuleInfo = showMoreModuleInfo,
            foldSystemModule = foldSystemModule,
            simpleListBottomBar = simpleListBottomBar,
            enableModuleShortcutAdd = enableModuleShortcutAdd,
            expanded = expanded,
            opacity = opacity,
            bannerInfo = bannerInfo,
            bannerImageAlpha = bannerImageAlpha,
            isWallpaperMode = isWallpaperMode,
            sizeStr = sizeStr,
            customInfo = customInfo,
            onCheckChanged = onCheckChanged,
            onClick = onClick,
            onUpdate = onUpdate,
            onUninstall = onUninstall,
            onUndoUninstall = onUndoUninstall,
            onNavigateAction = {
                navigator.navigate(ExecuteAPMActionScreenDestination(it.id))
                viewModel.markNeedRefresh()
            },
            onAddShortcut = {
                shortcutName = it.name
                shortcutIconUri = null
                shortcutType = if (it.hasWebUi) "webui" else "action"
                showShortcutDialog = true
            }
        )
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

    ModuleShortcutDialog(
        showDialog = showShortcutDialog,
        onDismiss = { showShortcutDialog = false },
        context = context,
        module = module,
        shortcutName = shortcutName,
        onShortcutNameChange = { shortcutName = it },
        shortcutIconUri = shortcutIconUri,
        onShortcutIconUriChange = { shortcutIconUri = it },
        shortcutType = shortcutType,
        onShortcutTypeChange = { shortcutType = it },
        shortcutPreviewBitmap = shortcutPreviewBitmap,
        appIcon = appIcon,
        effectiveShortcutIconUri = effectiveShortcutIconUri,
        onPickIcon = { pickShortcutIconLauncher.launch("image/*") }
    )

    // 自定义模块信息状态
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    // 弹窗打开时加载自定义信息
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            val info = withContext(Dispatchers.IO) {
                apmCustomModuleInfoStorage.read(module.id)
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
                    runCatching {
                        val localCleared = apmBannerStorage.clear(module.id)
                        val legacyCleared = runCatching {
                            val rootShell = getRootShell(true)
                            val resolvedDir = resolveModuleDir(rootShell, module.id)
                            clearLegacyFolkBanner(rootShell, resolvedDir)
                        }.getOrDefault(false)
                        localCleared || legacyCleared
                    }.getOrDefault(false)
                }
                loadingDialog.hide()
                if (success) {
                    viewModel.removeBannerInfo(module.id)
                    bannerReloadKey++
                    snackBarHost.showSnackbar(folkBannerCleared.format(module.name))
                } else {
                    snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                }
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
                    apmCustomModuleInfoStorage.write(module.id, CustomModuleInfo(
                        name = info.name.takeIf { it.isNotBlank() },
                        version = info.version.takeIf { it.isNotBlank() },
                        author = info.author.takeIf { it.isNotBlank() },
                        description = info.description.takeIf { it.isNotBlank() },
                    ))
                }
                snackBarHost.showSnackbar(
                    customInfoSavedMsg.format(module.name)
                )
            }
        },
        onResetModuleInfo = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    apmCustomModuleInfoStorage.clear(module.id)
                }
                snackBarHost.showSnackbar(
                    customInfoResetMsg.format(module.name)
                )
            }
        }
    )
}
