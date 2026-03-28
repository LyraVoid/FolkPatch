package me.bmax.apatch.ui.screen

import android.os.Build
import android.system.Os
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import me.bmax.apatch.ui.theme.isInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.generated.destinations.UninstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.LatestVersionInfo
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.checkNewVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val managerVersion = getManagerVersion()

@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    val prefs = APApplication.sharedPreferences
    var homeLayout by remember { mutableStateOf(prefs.getString("home_layout_style", "default") ?: "default") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "home_layout_style") {
                homeLayout = prefs.getString("home_layout_style", "default") ?: "default"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    when (homeLayout) {
        "list" -> ListHomeScreen(navigator)
        "classic" -> ClassicHomeScreen(navigator)
        else -> MainHomeScreen(navigator)
    }
}

@Composable
fun MainHomeScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    Scaffold(
        topBar = {
            TopBar(
                onInstallClick = dropUnlessResumed {
                    navigator.navigate(InstallModeSelectScreenDestination)
                },
                navigator,
                kpState,
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = { },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 100.dp
            )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BackupWarningCard()
                    StatusCard(
                        kpState = kpState,
                        apState = apState,
                        navigator = navigator
                    )

                    // AndroidPatch Install Card (Only when KernelPatch installed but AndroidPatch not installed)
                    if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCard(
                            apState = apState,
                            kpState = kpState,
                            navigator = navigator
                        )
                    }

                    // Update checking is now handled in MainActivity with a dialog on app launch
                    // val checkUpdate =
                    //     APApplication.sharedPreferences.getBoolean("check_update", true)
                    // if (checkUpdate) {
                    //     UpdateCard()
                    // }
                    InfoCard(kpState, apState)

                    // About card - can be hidden in settings
                    var hideAboutCard by remember {
                        mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_about_card", true))
                    }
                    DisposableEffect(Unit) {
                        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                            if (key == "hide_about_card") {
                                hideAboutCard = prefs.getBoolean("hide_about_card", true)
                            }
                        }
                        APApplication.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                        onDispose { APApplication.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
                    }
                    if (!hideAboutCard) {
                        LearnMoreCard()
                    }
                }
            }
        }
    }
}

@Composable
fun AuthFailedTipDialog(showDialog: MutableState<Boolean>) {
    SuperDialog(
        title = stringResource(R.string.home_dialog_auth_fail_title),
        summary = stringResource(R.string.home_dialog_auth_fail_content),
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false },
    ) {
        Spacer(Modifier.height(12.dp))

        Row {
            TextButton(
                stringResource(android.R.string.ok),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}

val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
    superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
}

@Composable
fun AuthSuperKey(showDialog: MutableState<Boolean>, showFailedDialog: MutableState<Boolean>) {
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var enable by remember { mutableStateOf(false) }

    SuperDialog(
        show = showDialog.value,
        title = stringResource(R.string.home_auth_key_title),
        summary = stringResource(R.string.home_auth_key_desc),
        onDismissRequest = { showDialog.value = false }
    ) {

        Box(contentAlignment = Alignment.CenterEnd) {

            TextField(
                value = key,
                onValueChange = {
                    key = it
                    enable = checkSuperKeyValidation(key)
                },
                label = stringResource(R.string.super_key),
                visualTransformation =
                    if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                onClick = { keyVisible = !keyVisible }
            ) {
                Icon(
                    imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {

            TextButton(
                stringResource(id = android.R.string.cancel),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(id = android.R.string.ok),
                onClick = {
                    showDialog.value = false
                    val ok = Natives.nativeReady(key)
                    if (ok) APApplication.superKey = key
                    else showFailedDialog.value = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = enable
            )
        }
    }
}

@Composable
private fun TopBar(
    onInstallClick: () -> Unit,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    scrollBehavior: ScrollBehavior
) {
    val uriHandler = LocalUriHandler.current
    val showDropdownMoreOptions = remember { mutableStateOf(false) }
    val howDropdownReboot = remember { mutableStateOf(false) }

    val rebootItems = listOf(
        stringResource(R.string.reboot),
        stringResource(R.string.reboot_recovery),
        stringResource(R.string.reboot_bootloader),
        stringResource(R.string.reboot_download),
        stringResource(R.string.reboot_edl),
        stringResource(R.string.reboot_fastbootd),
    )

    val moreItems = listOf(
        stringResource(R.string.home_more_menu_feedback_or_suggestion),
        stringResource(R.string.home_more_menu_about)
    )

    TopAppBar(
        title = stringResource(R.string.app_name),
        actions = {
            IconButton(onClick = onInstallClick) {
                Icon(
                    imageVector = Icons.Filled.InstallMobile,
                    contentDescription = stringResource(id = R.string.mode_select_page_title)
                )
            }

            if (kpState != APApplication.State.UNKNOWN_STATE) {
                IconButton(onClick = {
                    howDropdownReboot.value = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.reboot)
                    )

                    SuperListPopup(
                        show = howDropdownReboot.value,
                        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                        alignment = PopupPositionProvider.Align.TopEnd,
                        onDismissRequest = { howDropdownReboot.value = false }
                    ) {
                        ListPopupColumn {
                            rebootItems.forEachIndexed { index, string ->
                                DropdownImpl(
                                    text = string,
                                    isSelected = false,
                                    optionSize = rebootItems.size,
                                    onSelectedIndexChange = {
                                        when (index) {
                                            0 -> reboot()
                                            1 -> reboot("recovery")
                                            2 -> reboot("bootloader")
                                            3 -> reboot("download")
                                            4 -> reboot("edl")
                                            5 -> reboot("fastboot")
                                        }
                                        howDropdownReboot.value = false
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showDropdownMoreOptions.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(id = R.string.settings)
                    )

                    SuperListPopup(
                        show = showDropdownMoreOptions.value,
                        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
                        alignment = PopupPositionProvider.Align.TopEnd,
                        onDismissRequest = { showDropdownMoreOptions.value = false }
                    ) {
                        ListPopupColumn {
                            moreItems.forEachIndexed { index, string ->
                                DropdownImpl(
                                    text = string,
                                    isSelected = false,
                                    optionSize = moreItems.size,
                                    onSelectedIndexChange = {
                                        when (index) {
                                            0 -> uriHandler.openUri("https://github.com/matsuzaka-yuki/FolkLite/issues/new/choose")
                                            1 -> navigator.navigate(AboutScreenDestination)
                                        }
                                        showDropdownMoreOptions.value = false
                                    },
                                    index = index
                                )
                            }
                        }
                    }
                }
            }
        }, scrollBehavior = scrollBehavior
    )
}


@Composable
private fun StatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator
) {
    val showAuthFailedTipDialog = remember { mutableStateOf(false) }
    AuthFailedTipDialog(showAuthFailedTipDialog)

    val showAuthKeyDialog = remember { mutableStateOf(false) }
    AuthSuperKey(showAuthKeyDialog, showAuthFailedTipDialog)

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

    val prefs = APApplication.sharedPreferences
    val colorMode = prefs.getInt("color_mode", 0)
    val isMonet = colorMode >= 3
    val isDark = isInDarkTheme(colorMode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Main Status Card (Left - Large)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.defaultColors(
                color = when (kpState) {
                    APApplication.State.KERNELPATCH_INSTALLED -> when {
                        isMonet -> colorScheme.primaryContainer
                        isDark -> Color(0xFF1A3825)
                        else -> Color(0xFFDFFAE4)
                    }
                    APApplication.State.KERNELPATCH_NEED_UPDATE -> colorScheme.secondaryContainer
                    APApplication.State.KERNELPATCH_NEED_REBOOT -> colorScheme.errorContainer
                    else -> colorScheme.secondaryContainer
                }
            ),
            onClick = {
                when (kpState) {
                    APApplication.State.UNKNOWN_STATE -> {
                        showAuthKeyDialog.value = true
                    }
                    APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                        if (Version.installedKPVUInt() < 0x900u) {
                            navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                        } else {
                            navigator.navigate(InstallModeSelectScreenDestination)
                        }
                    }
                    APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                        reboot()
                    }
                    else -> {
                        if (apState == APApplication.State.ANDROIDPATCH_INSTALLED || apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
                            navigator.navigate(UninstallModeSelectScreenDestination)
                        } else {
                            navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                        }
                    }
                }
            },
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Decorative icon at bottom-right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(38.dp, 45.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        modifier = Modifier.size(170.dp),
                        imageVector = when (kpState) {
                            APApplication.State.KERNELPATCH_INSTALLED -> Icons.Rounded.CheckCircleOutline
                            APApplication.State.KERNELPATCH_NEED_UPDATE -> Icons.Outlined.SystemUpdate
                            APApplication.State.KERNELPATCH_NEED_REBOOT -> Icons.Filled.Refresh
                            else -> Icons.Rounded.ErrorOutline
                        },
                        tint = when (kpState) {
                            APApplication.State.KERNELPATCH_INSTALLED -> if (isMonet) {
                                colorScheme.primary.copy(alpha = 0.8f)
                            } else {
                                Color(0xFF36D167)
                            }
                            APApplication.State.KERNELPATCH_NEED_UPDATE -> colorScheme.secondary
                            APApplication.State.KERNELPATCH_NEED_REBOOT -> colorScheme.error
                            else -> colorScheme.outline
                        },
                        contentDescription = null
                    )
                }

                // Status text at top-left
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(all = 16.dp)
                ) {
                    val statusText = when (kpState) {
                        APApplication.State.KERNELPATCH_INSTALLED -> {
                            val mode = if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "<Full>" else "<Half>"
                            "${stringResource(R.string.home_working)} $mode"
                        }
                        APApplication.State.KERNELPATCH_NEED_UPDATE -> stringResource(R.string.home_need_update)
                        APApplication.State.KERNELPATCH_NEED_REBOOT -> stringResource(R.string.home_ap_cando_reboot)
                        else -> stringResource(R.string.home_not_installed)
                    }

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = statusText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = when (kpState) {
                            APApplication.State.KERNELPATCH_INSTALLED ->
                                stringResource(R.string.kpatch_version, managerVersion.first)
                            APApplication.State.KERNELPATCH_NEED_UPDATE ->
                                "${Version.installedKPVString()} → ${Version.buildKPVString()}"
                            else -> stringResource(R.string.home_click_to_install)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        // Right Column with two cards
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // KernelPatch Info Card (Top Right)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = {
                    if (kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                        when (kpState) {
                            APApplication.State.UNKNOWN_STATE -> {
                                showAuthKeyDialog.value = true
                            }
                            APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                if (Version.installedKPVUInt() < 0x900u) {
                                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                                } else {
                                    navigator.navigate(InstallModeSelectScreenDestination)
                                }
                            }
                            else -> {
                                navigator.navigate(InstallModeSelectScreenDestination)
                            }
                        }
                    }
                },
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.kernel_patch),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = when (kpState) {
                            APApplication.State.UNKNOWN_STATE -> "Unknown"
                            else -> Version.installedKPVString()
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // AndroidPatch Status Card (Bottom Right)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                insideMargin = PaddingValues(16.dp),
                onClick = {
                    when (apState) {
                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            showUninstallDialog.value = true
                        }
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                            if (kpState == APApplication.State.KERNELPATCH_INSTALLED ||
                                kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
                                APApplication.installApatch()
                            }
                        }
                        else -> {
                            // Not installed - do nothing (no trigger effect)
                        }
                    }
                },
                showIndication = apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED,
                pressFeedbackType = PressFeedbackType.Tilt
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.android_patch),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = when (apState) {
                            APApplication.State.ANDROIDPATCH_INSTALLED,
                            APApplication.State.ANDROIDPATCH_NEED_UPDATE,
                            APApplication.State.ANDROIDPATCH_INSTALLING -> "Active"
                            else -> "Inactive"
                        },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (apState) {
                            APApplication.State.ANDROIDPATCH_INSTALLED,
                            APApplication.State.ANDROIDPATCH_NEED_UPDATE,
                            APApplication.State.ANDROIDPATCH_INSTALLING ->
                                if (isMonet) colorScheme.primary else Color(0xFF36D167)
                            else -> colorScheme.outline
                        },
                    )
                }
            }
        }
    }
}


@Composable
fun AStatusCard(
    apState: APApplication.State,
    kpState: APApplication.State,
    navigator: DestinationsNavigator
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> Icons.Outlined.SystemUpdate
                        else -> Icons.Outlined.Block
                    },
                    stringResource(
                        when (apState) {
                            APApplication.State.ANDROIDPATCH_NEED_UPDATE -> R.string.home_need_update
                            else -> R.string.home_not_installed
                        }
                    )
                )
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {

                    Text(
                        text = stringResource(
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> R.string.home_need_update
                                else -> R.string.home_not_installed
                            }
                        ),
                        style = MiuixTheme.textStyles.body2
                    )
                }
                if (apState != APApplication.State.UNKNOWN_STATE) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            onClick = {
                                when (apState) {
                                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED,
                                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                        if (kpState == APApplication.State.KERNELPATCH_INSTALLED ||
                                            kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
                                            APApplication.installApatch()
                                        }
                                    }

                                    else -> {}
                                }
                            },
                            content = {
                                when (apState) {
                                    APApplication.State.ANDROIDPATCH_INSTALLING,
                                    APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                        Icon(Icons.Outlined.Cached, contentDescription = "busy")
                                    }
                                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                        Text(text = stringResource(id = R.string.home_ap_cando_update), color = Color.White)
                                    }

                                    else -> {
                                        Text(text = stringResource(id = R.string.home_ap_cando_install), color = Color.White)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun BackupWarningCard() {
    val show = rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }
    if (show.value) {
        Card(
            colors = CardDefaults.defaultColors(run {
                MiuixTheme.colorScheme.error
            })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "warning", tint = Color.White)
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.patch_warnning),
                            color = Color.White
                        )

                        Spacer(Modifier.width(12.dp))

                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                apApp.updateBackupWarningState(false)
                                show.value = false
                            },
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

private fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}

@Composable
private fun InfoCard(kpState: APApplication.State, apState: APApplication.State) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val contents = StringBuilder()
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String) {
                contents.appendLine(label).appendLine(content).appendLine()
                Text(text = label, style = MiuixTheme.textStyles.body1)
                Text(text = content, style = MiuixTheme.textStyles.body2)
            }

            if (kpState != APApplication.State.UNKNOWN_STATE) {
                InfoCardItem(
                    stringResource(R.string.home_kpatch_version), Version.installedKPVString()
                )

                Spacer(Modifier.height(16.dp))
                InfoCardItem(stringResource(R.string.home_su_path), Natives.suPath())

                Spacer(Modifier.height(16.dp))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    stringResource(R.string.home_apatch_version), managerVersion.second.toString()
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_device_info), getDeviceInfo())

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_kernel), uname.release)

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_system_version), getSystemVersion())

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_fingerprint), Build.FINGERPRINT)

            Spacer(Modifier.height(16.dp))

            // Zygisk 检测
            var zygiskImplement by remember { mutableStateOf("None") }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None") {
                InfoCardItem(stringResource(R.string.home_zygisk_implement), zygiskImplement)
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_selinux_status), getSELinuxStatus())

        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MiuixTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.defaultColors(color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(32.dp)) {
            Text(
                text = message, style = MiuixTheme.textStyles.body1
            )
        }
    }
}

@Composable
fun UpdateCard() {
    val latestVersionInfo = LatestVersionInfo()
    val newVersion by produceState(initialValue = latestVersionInfo) {
        value = withContext(Dispatchers.IO) {
            checkNewVersion()
        }
    }
    val currentVersionCode = managerVersion.second
    val newVersionCode = newVersion.versionCode
    val newVersionUrl = newVersion.downloadUrl
    val changelog = newVersion.changelog

    val uriHandler = LocalUriHandler.current
    val title = stringResource(id = R.string.apm_changelog)
    val updateText = stringResource(id = R.string.apm_update)

    AnimatedVisibility(
        visible = newVersionCode > currentVersionCode,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val updateDialog = rememberConfirmDialog(onConfirm = { uriHandler.openUri(newVersionUrl) })
        WarningCard(
            message = stringResource(id = R.string.home_new_apatch_found).format(newVersionCode),
            MiuixTheme.colorScheme.outline
        ) {
            if (changelog.isEmpty()) {
                uriHandler.openUri(newVersionUrl)
            } else {
                updateDialog.showConfirm(
                    title = title, content = changelog, markdown = true, confirm = updateText
                )
            }
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://fp.mysqil.com/")
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MiuixTheme.textStyles.body2
                )
            }
        }
    }
}

private enum class ApatchUninstallOption(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
    val icon: ImageVector,
) {
    PATCH_ONLY(
        titleRes = R.string.home_dialog_uninstall_ap_only,
        descRes = R.string.home_dialog_uninstall_ap_only_desc,
        icon = Icons.Outlined.Delete
    ),
    FULL(
        titleRes = R.string.home_dialog_uninstall_all,
        descRes = R.string.home_dialog_uninstall_all_desc,
        icon = Icons.Outlined.DeleteForever
    ),
}

@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>, navigator: DestinationsNavigator) {
    if (!showDialog.value) return

    val options = remember { listOf(ApatchUninstallOption.PATCH_ONLY, ApatchUninstallOption.FULL) }
    var selectedOption by remember { mutableStateOf<ApatchUninstallOption?>(null) }

    SuperDialog(
        title = stringResource(R.string.home_dialog_uninstall_title),
        show = showDialog.value,
        onDismissRequest = { showDialog.value = false }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedOption = option },
                    colors = CardDefaults.defaultColors(
                        color = if (isSelected) {
                            colorScheme.secondaryContainer
                        } else {
                            colorScheme.surface
                        }
                    ),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) {
                                colorScheme.primary
                            } else {
                                Color(0xFF9E9E9E)
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(option.titleRes),
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(option.descRes),
                                style = MiuixTheme.textStyles.body2,
                                color = if (isSelected) {
                                    colorScheme.onSurfaceVariantSummary
                                } else {
                                    Color(0xFF9E9E9E)
                                },
                            )
                        }
                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.CheckCircle
                            },
                            contentDescription = null,
                            tint = if (isSelected) {
                                colorScheme.primary
                            } else {
                                Color(0xFF9E9E9E)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                stringResource(id = android.R.string.cancel),
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(20.dp))

            TextButton(
                stringResource(id = R.string.home_ap_cando_uninstall),
                onClick = {
                    when (selectedOption) {
                        ApatchUninstallOption.PATCH_ONLY -> {
                            showDialog.value = false
                            APApplication.uninstallApatch()
                        }

                        ApatchUninstallOption.FULL -> {
                            showDialog.value = false
                            APApplication.uninstallApatch()
                            navigator.navigate(UninstallModeSelectScreenDestination)
                        }

                        null -> {}
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = selectedOption != null
            )
        }
    }
}
