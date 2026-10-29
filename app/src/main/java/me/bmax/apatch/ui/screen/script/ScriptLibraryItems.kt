package me.bmax.apatch.ui.screen.script

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.misc.BannerApiService

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.data.ScriptInfo
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole
import me.bmax.apatch.ui.theme.bannerFadeColor
import me.bmax.apatch.util.ModuleShortcut
import me.bmax.apatch.util.scriptBannerStorage
import me.bmax.apatch.util.ui.showToast
import java.io.File
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication

private val scriptBannerSemaphore = Semaphore(4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptLabel(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(FolkDimens.Size8),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = FolkDimens.Size6, vertical = FolkDimens.Size2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptItem(
    script: ScriptInfo,
    enableShortcut: Boolean,
    simpleListBottomBar: Boolean,
    foldCard: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by rememberSaveable(script.id) { mutableStateOf(script.alias) }
    var shortcutIconUri by remember { mutableStateOf<String?>(null) }
    val appIcon = remember(context) { context.packageManager.getApplicationIcon(context.packageName) }
    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        shortcutIconUri = uri?.toString()
    }

    val shortcutPreviewBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = shortcutIconUri) {
        value = if (shortcutIconUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, shortcutIconUri)
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
    } else if (isWallpaperMode) {
        (0.35f + (opacity - 0.2f) * 0.5f).coerceIn(0.25f, 0.6f)
    } else {
        0.18f
    }
    var showBannerDialog by remember { mutableStateOf(false) }
    var hasBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by rememberSaveable(script.id) { mutableStateOf(0) }
    val loadingDialog = rememberLoadingDialog()
    val bannerTitle = stringResource(R.string.apm_folk_banner_title)
    val bannerSelect = stringResource(R.string.apm_folk_banner_select)
    val bannerClear = stringResource(R.string.apm_folk_banner_clear)
    val bannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val bannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val bannerFailed = stringResource(R.string.apm_folk_banner_failed)

    LaunchedEffect(showBannerDialog) {
        if (showBannerDialog) {
            hasBanner = withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) != null }
        }
    }

    val pickBannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val data = withContext(Dispatchers.IO) {
                    runCatching { scriptBannerStorage.write(context, script.id, it) }.getOrNull()
                }
                loadingDialog.hide()
                if (data != null) {
                    bannerReloadKey++
                    showToast(context, bannerSaved.format(script.alias))
                } else {
                    showToast(context, bannerFailed.format(script.alias))
                }
            }
        }
    }

    val bannerData by produceState<ByteArray?>(
        initialValue = null,
        script.id,
        BackgroundConfig.isBannerEnabled,
        BackgroundConfig.isBannerApiModeEnabled,
        BackgroundConfig.bannerApiSource,
        BackgroundConfig.isFolkBannerEnabled,
        bannerReloadKey
    ) {
        if (!BackgroundConfig.isBannerEnabled) {
            value = null
            return@produceState
        }
        scriptBannerSemaphore.withPermit {
            val apiSource = BackgroundConfig.getEffectiveBannerApiSource()
            value = if (BackgroundConfig.isBannerApiModeEnabled && apiSource.isNotBlank()) {
                BannerApiService.getModuleBanner(context, "script_${script.id}", apiSource)
                    ?: if (BackgroundConfig.isFolkBannerEnabled) withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) } else null
            } else if (BackgroundConfig.isFolkBannerEnabled) {
                withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) }
            } else null
        }
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val pressed by cardInteractionSource.collectIsPressedAsState()
    val cardCorner by animateDpAsState(
        targetValue = when {
            expanded && foldCard -> FolkDimens.Size28
            pressed -> FolkDimens.Size24
            else -> FolkDimens.Size20
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "scriptCardCorner",
    )
    val cardShape = ContinuousCornerShape(cardCorner)
    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec())
        .combinedClickable(
            interactionSource = cardInteractionSource,
            indication = LocalIndication.current,
            onClick = { if (foldCard) onExpandToggle() else onRun() },
            onLongClick = { showBannerDialog = true }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bannerData != null) {
                val fadeColor = bannerFadeColor()
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context).data(bannerData).build(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = bannerImageAlpha
                )
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(fadeColor.copy(alpha = 0f), fadeColor.copy(alpha = if (isWallpaperMode) 0.5f else 0.8f))
                        )
                    )
                )
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size6),
                            modifier = Modifier.padding(bottom = FolkDimens.Size8)
                        ) {
                            ScriptLabel(
                                text = "Shell",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = script.alias,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = File(script.path).name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FolkDimens.Size12))

                Text(
                    text = script.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(FolkDimens.Size16))
                AnimatedVisibility(
                    visible = !foldCard || expanded,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)) {
                    FilledTonalButton(
                        onClick = onRun,
                        contentPadding = if (simpleListBottomBar) PaddingValues(FolkDimens.Size12) else ButtonDefaults.TextButtonContentPadding,
                        modifier = if (simpleListBottomBar) Modifier else Modifier.height(FolkDimens.Size36),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18))
                        if (!simpleListBottomBar) {
                            Spacer(Modifier.width(FolkDimens.Size8))
                            Text(stringResource(R.string.script_library_run))
                        }
                    }

                    if (enableShortcut) {
                        FilledTonalButton(
                            onClick = {
                                shortcutName = script.alias
                                shortcutIconUri = null
                                showShortcutDialog = true
                            },
                            contentPadding = if (simpleListBottomBar) PaddingValues(FolkDimens.Size12) else ButtonDefaults.TextButtonContentPadding,
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(FolkDimens.Size36),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18))
                            if (!simpleListBottomBar) {
                                Spacer(Modifier.width(FolkDimens.Size8))
                                Text(stringResource(R.string.module_shortcut_add))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = onDelete,
                        contentPadding = if (simpleListBottomBar) PaddingValues(FolkDimens.Size12) else ButtonDefaults.TextButtonContentPadding,
                        modifier = if (simpleListBottomBar) Modifier else Modifier.height(FolkDimens.Size36),
                        colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) else ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18))
                        if (!simpleListBottomBar) {
                            Spacer(Modifier.width(FolkDimens.Size8))
                            Text(stringResource(R.string.script_library_delete))
                        }
                    }
                }
            }
        }
    }
    }

    if (LocalInsideSplicedGroup.current) {
        Box(modifier = clickModifier) { contentBlock() }
    } else {
        Surface(
            modifier = Modifier.clip(cardShape).then(clickModifier),
            shape = cardShape,
            color = cardColor,
            tonalElevation = FolkDimens.Size0,
        ) { contentBlock() }
    }

    BackgroundOptionsDialog(
        showDialog = showBannerDialog,
        onDismiss = { showBannerDialog = false },
        title = bannerTitle,
        selectLabel = bannerSelect,
        clearLabel = bannerClear,
        hasExisting = hasBanner,
        onSelectImage = { pickBannerLauncher.launch("image/*") },
        onClearImage = {
            scope.launch {
                loadingDialog.show()
                val cleared = withContext(Dispatchers.IO) { scriptBannerStorage.clear(script.id) }
                loadingDialog.hide()
                if (cleared) {
                    bannerReloadKey++
                    showToast(context, bannerCleared.format(script.alias))
                } else {
                    showToast(context, bannerFailed.format(script.alias))
                }
            }
        }
    )

    if (showShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showShortcutDialog = false },
            title = { Text(stringResource(R.string.module_shortcut_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        label = { Text(stringResource(R.string.module_shortcut_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(FolkDimens.Size12))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.module_shortcut_icon))
                        Spacer(Modifier.width(FolkDimens.Size12))
                        if (shortcutPreviewBitmap != null) {
                            Image(
                                bitmap = shortcutPreviewBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(FolkDimens.Size36)
                            )
                        } else if (shortcutIconUri != null) {
                            AsyncImage(
                                model = shortcutIconUri,
                                contentDescription = null,
                                modifier = Modifier.size(FolkDimens.Size36)
                            )
                        } else {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(FolkDimens.Size36)
                            )
                        }
                    }
                    Spacer(Modifier.height(FolkDimens.Size8))
                    Row(horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12)) {
                        TextButton(onClick = { pickShortcutIconLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.module_shortcut_icon_select))
                        }
                        TextButton(onClick = { shortcutIconUri = null }) {
                            Text(stringResource(R.string.module_shortcut_icon_default))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = shortcutName.ifBlank { script.alias }
                    ModuleShortcut.createScriptShortcut(
                        context,
                        script.id,
                        name,
                        shortcutIconUri
                    )
                    showShortcutDialog = false
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShortcutDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

