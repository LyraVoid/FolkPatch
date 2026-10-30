package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.core.content.edit
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.folk.folkGroupColor
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.rememberSystemCropLauncher
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.screen.home.getDeviceInfo
import me.bmax.apatch.ui.screen.settings.ResolvedEntry
import me.bmax.apatch.ui.screen.settings.SettingCategory
import me.bmax.apatch.ui.screen.settings.SettingsRegistry
import me.bmax.apatch.ui.screen.settings.SettingsSearchResults
import me.bmax.apatch.ui.screen.settings.SettingsSearchShortcut
import me.bmax.apatch.ui.screen.settings.SettingsSearchShortcuts
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.getBugreportFile
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import java.io.File
import java.io.FileOutputStream
import com.ramcosta.composedestinations.generated.destinations.PluginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FaqScreenDestination
import com.ramcosta.composedestinations.generated.destinations.WallpaperGalleryScreenDestination
import androidx.compose.ui.semantics.Role
import me.bmax.apatch.ui.theme.tokens.FolkShape

private const val FEEDBACK_URL = "https://github.com/LyraVoid/FolkPatch/issues/new/choose"

internal const val PROFILE_AVATAR_FILE = "profile_avatar"

/**
 * Copies the picked image into app storage and returns a cache-busted URI.
 *
 * The square crop itself is not re-implemented here: [me.bmax.apatch.util.BottomBarIconConfig]
 * already centre-crops custom nav icons, and the avatar reuses that.
 */
private suspend fun persistProfileAvatar(context: android.content.Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.filesDir, PROFILE_AVATAR_FILE)
            SafeUriResolver.openInputStream(context, uri).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(target).buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
                .toString()
        }.getOrNull()
    }

private data class SecondaryEntry(
    val category: SettingCategory,
    val icon: ImageVector,
    val label: String,
    val iconSize: Dp = FolkDimens.Size24,
    val onClick: () -> Unit,
)

private fun openSettingEntry(navigator: DestinationsNavigator, entry: ResolvedEntry) {
    val key = entry.entry.key
    when (entry.entry.category) {
        SettingCategory.GENERAL -> navigator.navigate(GeneralSettingsScreenDestination(key))
        SettingCategory.APPEARANCE -> navigator.navigate(AppearanceSettingsScreenDestination(key))
        SettingCategory.BEHAVIOR -> navigator.navigate(BehaviorSettingsScreenDestination(key))
        SettingCategory.SECURITY -> navigator.navigate(SecuritySettingsScreenDestination(key))
        SettingCategory.BACKUP -> navigator.navigate(BackupSettingsScreenDestination(key))
        SettingCategory.MODULE -> navigator.navigate(ModuleSettingsScreenDestination(key))
        SettingCategory.MULTIMEDIA -> navigator.navigate(MultimediaSettingsScreenDestination(key))
        SettingCategory.FUNCTION -> navigator.navigate(FunctionSettingsScreenDestination(key))
    }
}

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val canAuthenticate = remember { BiometricUtils.isBiometricAvailable(context) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val resolvedEntries = remember(context.resources) {
        SettingsRegistry.resolveAll(context.resources)
    }

    // Local-only personalisation. Nothing here needs an account: the avatar,
    // nickname and signature are stored in shared preferences and the picked
    // image is copied into app storage.
    val prefs = APApplication.sharedPreferences
    var profileNickname by remember { mutableStateOf(prefs.getString("profile_nickname", "").orEmpty()) }
    var profileSignature by remember { mutableStateOf(prefs.getString("profile_signature", "").orEmpty()) }
    var profileAvatar by remember { mutableStateOf(prefs.getString("profile_avatar", "").orEmpty()) }
    var profileAvatarOpacity by remember { mutableStateOf(prefs.getFloat("profile_avatar_opacity", 1f)) }
    var showProfileEditor by rememberSaveable { mutableStateOf(false) }
    var pendingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var showCropChoice by remember { mutableStateOf(false) }
    var showAvatarSource by remember { mutableStateOf(false) }
    var avatarSourceInput by remember { mutableStateOf<AvatarSource?>(null) }

    fun applyAvatar(uri: Uri) {
        scope.launch {
            persistProfileAvatar(context, uri)?.let { stored ->
                profileAvatar = stored
                prefs.edit { putString("profile_avatar", stored) }
            }
        }
    }

    // Same platform crop flow the appearance settings use, locked to a square.
    val avatarCropLauncher = rememberSystemCropLauncher(
        cacheName = "profile_avatar_crop_cache",
        aspectX = 1,
        aspectY = 1,
        outputSize = 512,
    ) { uri -> applyAvatar(uri) }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingAvatarUri = uri
            showCropChoice = true
        }
    }

    var showDevDialog by rememberSaveable { mutableStateOf(false) }
    DeveloperInfo(
        showDialog = showDevDialog
    ) {
        showDevDialog = false
    }

    val disclaimerDialogState = remember { mutableStateOf(false) }

    // The icon grid holds our secondary entries - the settings categories. The
    // bottom bar already covers Home / KPModule / SuperUser / APModule / Settings,
    // so nothing here duplicates it.
    val secondaryEntries = buildList {
        add(
            SecondaryEntry(
                category = SettingCategory.GENERAL,
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.settings_category_general),
                onClick = { navigator.navigate(GeneralSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                category = SettingCategory.APPEARANCE,
                icon = Icons.Outlined.Brush,
                label = stringResource(R.string.settings_category_appearance),
                onClick = { navigator.navigate(AppearanceSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                category = SettingCategory.BEHAVIOR,
                icon = Icons.Outlined.TouchApp,
                label = stringResource(R.string.settings_category_behavior),
                onClick = { navigator.navigate(BehaviorSettingsScreenDestination(null)) },
            )
        )
        add(
            SecondaryEntry(
                category = SettingCategory.FUNCTION,
                icon = Icons.Outlined.Handyman,
                label = stringResource(R.string.settings_category_function),
                onClick = { navigator.navigate(FunctionSettingsScreenDestination(null)) },
            )
        )
        if (canAuthenticate) {
            add(
                SecondaryEntry(
                    category = SettingCategory.SECURITY,
                    icon = Icons.Outlined.Lock,
                    label = stringResource(R.string.settings_category_security),
                    onClick = { navigator.navigate(SecuritySettingsScreenDestination(null)) },
                )
            )
        }
        if (aPatchReady) {
            add(
                SecondaryEntry(
                    category = SettingCategory.BACKUP,
                    icon = Icons.Outlined.CloudUpload,
                    label = stringResource(R.string.settings_category_backup),
                    onClick = { navigator.navigate(BackupSettingsScreenDestination(null)) },
                )
            )
            add(
                SecondaryEntry(
                    category = SettingCategory.MODULE,
                    icon = Icons.Outlined.Extension,
                    label = stringResource(R.string.settings_category_module),
                    onClick = { navigator.navigate(ModuleSettingsScreenDestination(null)) },
                )
            )
        }
        add(
            SecondaryEntry(
                category = SettingCategory.MULTIMEDIA,
                icon = Icons.Outlined.LibraryMusic,
                label = stringResource(R.string.settings_category_multimedia),
                onClick = { navigator.navigate(MultimediaSettingsScreenDestination(null)) },
            )
        )
    }

    val searchShortcuts = secondaryEntries.map { entry ->
        SettingsSearchShortcut(
            label = entry.label,
            icon = entry.icon,
            onOpen = entry.onClick,
        )
    }
    val visibleCategories = secondaryEntries.mapTo(mutableSetOf()) { it.category }

    FolkScaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                searchPlaceholder = stringResource(R.string.settings_search_title),
                actions = {
                    IconButton(
                        onClick = dropUnlessResumed { navigator.navigate(PluginScreenDestination) },
                        modifier = Modifier.navOrigin(captureBitmap = true),
                    ) {
                        Icon(Icons.Outlined.Extension, contentDescription = stringResource(R.string.plugin_title))
                    }
                    IconButton(onClick = { showDevDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about))
                    }
                },
                defaultContent = { bottomPadding, closeSearch ->
                    SettingsSearchShortcuts(
                        shortcuts = searchShortcuts,
                        onOpen = { shortcut ->
                            shortcut.onOpen()
                            closeSearch()
                        },
                        contentPadding = PaddingValues(bottom = bottomPadding),
                    )
                },
                searchContent = { bottomPadding, closeSearch ->
                    SettingsSearchResults(
                        query = searchText,
                        entries = resolvedEntries,
                        categories = visibleCategories,
                        onSelect = { entry ->
                            openSettingEntry(navigator, entry)
                            closeSearch()
                        },
                        contentPadding = PaddingValues(bottom = bottomPadding),
                    )
                },
            )
        },
        addBottomClearance = false,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "identity_header") {
                ProfileHeader(
                    nickname = profileNickname.ifBlank { "FolkPatch" },
                    signature = profileSignature,
                    deviceName = getDeviceInfo().trim(),
                    avatarUri = profileAvatar,
                    avatarOpacity = profileAvatarOpacity,
                    onAvatarClick = { showProfileEditor = true },
                )
            }

            item(key = "secondary_entries") {
                Spacer(Modifier.height(FolkDimens.Size20))
                SettingsIconGrid(entries = secondaryEntries)
            }

            item(key = "utility_rows") {
                Spacer(Modifier.height(FolkDimens.Size16))
                SegmentedColumn(shape = FolkShape.Corner12) {
                    item(key = "utility_wallpaper") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.Wallpaper,
                            title = stringResource(R.string.settings_wallpaper_gallery),
                            onClick = { navigator.navigate(WallpaperGalleryScreenDestination) },
                        )
                    }
                    item(key = "utility_faq") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.Quiz,
                            title = stringResource(R.string.settings_faq),
                            onClick = { navigator.navigate(FaqScreenDestination) },
                        )
                    }
                    item(key = "utility_feedback") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(R.string.settings_bug_feedback),
                            onClick = { uriHandler.openUri(FEEDBACK_URL) },
                        )
                    }
                    item(key = "utility_disclaimer") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.Policy,
                            title = stringResource(R.string.settings_disclaimer),
                            onClick = { disclaimerDialogState.value = true },
                        )
                    }
                    item(key = "utility_send_log") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.Description,
                            title = stringResource(R.string.send_log),
                            onClick = {
                                scope.launch {
                                    val bugreport = loadingDialog.withLoading { getBugreportFile(context) }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                        bugreport,
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        type = "application/gzip"
                                        clipData = android.content.ClipData.newRawUri(null, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, context.getString(R.string.send_log))
                                    )
                                }
                            },
                        )
                    }
                    item(key = "utility_about") {
                        SegmentedNavigationItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            onClick = { navigator.navigate(AboutScreenDestination) },
                        )
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(Modifier.height(FolkDimens.Size16))
                NavigationBarsSpacer()
            }
        }
    }

    if (disclaimerDialogState.value) {
        SettingsDisclaimerDialog(disclaimerDialogState)
    }

    val pendingCrop = pendingAvatarUri
    if (showCropChoice && pendingCrop != null) {
        AlertDialog(
            onDismissRequest = {
                showCropChoice = false
                pendingAvatarUri = null
            },
            title = { Text(stringResource(R.string.profile_crop_avatar)) },
            text = { Text(stringResource(R.string.settings_crop_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showCropChoice = false
                    pendingAvatarUri = null
                    try {
                        avatarCropLauncher.launch(pendingCrop)
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, context.getString(R.string.settings_crop_not_supported))
                        applyAvatar(pendingCrop)
                    }
                }) {
                    Text(stringResource(R.string.settings_crop_dialog_crop))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCropChoice = false
                    pendingAvatarUri = null
                    applyAvatar(pendingCrop)
                }) {
                    Text(stringResource(R.string.settings_crop_dialog_direct))
                }
            },
        )
    }

    if (showProfileEditor) {
        ProfileEditSheet(
            nickname = profileNickname.ifBlank { "FolkPatch" },
            signature = profileSignature,
            avatarUri = profileAvatar,
            avatarOpacity = profileAvatarOpacity,
            onPickAvatar = { showAvatarSource = true },
            onUseDefaultAvatar = {
                profileAvatar = ""
                runCatching { File(context.filesDir, PROFILE_AVATAR_FILE).delete() }
                prefs.edit { remove("profile_avatar") }
            },
            onAvatarOpacityChange = { value ->
                profileAvatarOpacity = value
                prefs.edit { putFloat("profile_avatar_opacity", value) }
            },
            onRestoreDefault = {
                profileNickname = ""
                profileSignature = ""
                prefs.edit {
                    remove("profile_nickname")
                    remove("profile_signature")
                }
            },
            onDismiss = { showProfileEditor = false },
            onSave = { name, sign ->
                profileNickname = name
                profileSignature = sign
                prefs.edit {
                    if (name.isBlank()) remove("profile_nickname") else putString("profile_nickname", name)
                    if (sign.isBlank()) remove("profile_signature") else putString("profile_signature", sign)
                }
                showProfileEditor = false
            },
        )
    }

    AvatarSourceDialog(
        showDialog = showAvatarSource,
        onDismiss = { showAvatarSource = false },
        onSelect = { source ->
            showAvatarSource = false
            when (source) {
                AvatarSource.Local -> pickAvatarLauncher.launch("image/*")
                AvatarSource.Qq, AvatarSource.Gravatar -> avatarSourceInput = source
            }
        },
    )

    avatarSourceInput?.let { source ->
        AvatarIdDialog(
            title = stringResource(
                if (source == AvatarSource.Qq) R.string.profile_avatar_source_qq
                else R.string.profile_avatar_source_gravatar
            ),
            label = stringResource(
                if (source == AvatarSource.Qq) R.string.profile_avatar_qq_hint
                else R.string.profile_avatar_gravatar_hint
            ),
            onDismiss = { avatarSourceInput = null },
            onConfirm = { value ->
                avatarSourceInput = null
                val url = if (source == AvatarSource.Qq) qqAvatarUrl(value) else gravatarUrl(value)
                scope.launch {
                    val stored = downloadProfileAvatar(context, url)
                    if (stored != null) {
                        profileAvatar = stored
                        prefs.edit { putString("profile_avatar", stored) }
                    } else {
                        Toast.makeText(context, R.string.profile_avatar_fetch_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}


/**
 * Four-column icon grid, the shortcut block of the settings hub. Cells are
 * sized so an icon + label pair sits comfortably with generous vertical air.
 */
@Composable
private fun SettingsIconGrid(entries: List<SecondaryEntry>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FolkDimens.Size16),
        shape = FolkShape.Corner12,
        color = folkGroupColor(),
        tonalElevation = FolkDimens.Size0,
    ) {
        Column(modifier = Modifier.padding(vertical = FolkDimens.Size8)) {
            entries.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { entry ->
                        GridEntry(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridEntry(
    entry: SecondaryEntry,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(FolkShape.Corner12)
            .navOrigin(
                cornerRadius = FolkDimens.Size12,
                color = folkGroupColor(),
                interactionSource = interactionSource,
            )
            .folkPressScale(interactionSource)
            .clickable(role = Role.Button, 
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    entry.onClick()
                },
            )
            .padding(vertical = FolkDimens.Size8, horizontal = FolkDimens.Size4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(entry.iconSize),
        )
        Spacer(Modifier.height(FolkDimens.Size10))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

