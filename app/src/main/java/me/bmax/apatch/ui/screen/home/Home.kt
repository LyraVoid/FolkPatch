package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import me.bmax.apatch.ui.theme.refreshTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.WelcomeGuideDialog
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.migrateStockBootBackup
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.HomeBottomSpacer

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

@Destination<RootGraph>(start = true, wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    var showPatchFloatAction by remember { mutableStateOf(true) }

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    // Pick up a stock boot backup left behind by a manually flashed PATCH_ONLY
    // install; see migrateStockBootBackup.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { migrateStockBootBackup() }
    }

    SideEffect {
        if (kpState != APApplication.State.UNKNOWN_STATE) {
            showPatchFloatAction = false
        }
    }

    var homeLayout by remember { mutableStateOf(APApplication.sharedPreferences.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)) }
    var showListInfoIcons by remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("list_info_show_icons", false)) }
    val homeRefreshObserver by refreshTheme.observeAsState(false)
    if (homeRefreshObserver) {
        homeLayout = APApplication.sharedPreferences.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)
        showListInfoIcons = APApplication.sharedPreferences.getBoolean("list_info_show_icons", false)
    }

    // 迁移旧版SignUI: 合并至ListUI + "信息区图标"开关
    if (homeLayout == "sign") {
        APApplication.sharedPreferences.edit()
            .putString("home_layout_style", "default")
            .putBoolean("list_info_show_icons", true)
            .apply()
        homeLayout = "default"
        showListInfoIcons = true
    }

    // 首次启动欢迎引导
    var showWelcomeGuide by remember {
        mutableStateOf(!APApplication.sharedPreferences.getBoolean("welcome_guide_shown", false))
    }
    if (showWelcomeGuide) {
        WelcomeGuideDialog(
            onDismiss = {
                APApplication.sharedPreferences.edit()
                    .putBoolean("welcome_guide_shown", true)
                    .apply()
                showWelcomeGuide = false
            }
        )
    }

    FolkScaffold(
        // Every home layout already ends with HomeBottomSpacer, so it keeps its
        // own bottom clearance.
        addBottomClearance = false,
        topBar = {
            HomeTopBar(onInstallClick = dropUnlessResumed {
                navigator.navigate(InstallModeSelectScreenDestination)
            }, navigator, kpState)
        },
    ) { innerPadding ->
        ProvideHomeJailbreakState {
            when (homeLayout) {
                "kernelsu" -> HomeScreenV2(innerPadding, navigator, kpState, apState)
                "focus" -> HomeScreenV3(innerPadding, navigator, kpState, apState)
                "circle" -> HomeScreenCircle(innerPadding, navigator, kpState, apState)
                "dashboard_ui" -> HomeScreenV4(innerPadding, navigator, kpState, apState)
                "stats" -> HomeScreenStats(innerPadding, navigator, kpState, apState)
                else -> HomeScreenV1(innerPadding, navigator, kpState, apState, showListInfoIcons)
            }
        }
    }
}

@Composable
fun HomeScreenV1(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State,
    showInfoIcons: Boolean = false
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = FolkDimens.Size16)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
    ) {
        Spacer(Modifier.height(FolkDimens.Size0))
        KStatusCard(kpState, apState, navigator)
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
            AStatusCard(apState)
        }
        ListInfoCard(kpState, apState, showInfoIcons)
        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCard()
        }
        HomeBottomSpacer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>, navigator: DestinationsNavigator) {
    if (!showDialog.value) return

    val options = remember { listOf(ApatchUninstallOption.PATCH_ONLY, ApatchUninstallOption.FULL) }
    var selectedOption by remember { mutableStateOf<ApatchUninstallOption?>(null) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text(
                    text = stringResource(R.string.home_dialog_uninstall_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = FolkDimens.Size8),
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
                ) {
                    options.forEach { option ->
                        val isSelected = selectedOption == option
                        val backgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            FolkColors.Transparent
                        }
                        val subtitleColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(backgroundColor)
                                .clickable { selectedOption = option }
                                .padding(vertical = FolkDimens.Size12, horizontal = FolkDimens.Size8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = FolkDimens.Size16)
                                    .size(FolkDimens.Size24)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(option.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(option.descRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = subtitleColor,
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Outlined.RadioButtonChecked
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(FolkDimens.Size24)
                            )
                        }
                    }
                }

                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                SideEffect {
                    APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedOption) {
                            ApatchUninstallOption.PATCH_ONLY -> {
                                showDialog.value = false
                                APApplication.uninstallApatch()
                            }

                            ApatchUninstallOption.FULL -> {
                                showDialog.value = false
                                APApplication.uninstallApatch()
                                navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                            }

                            null -> Unit
                        }
                    },
                    enabled = selectedOption != null,
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = FolkDimens.Size4,
        )
    }
}


fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}


@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current

    TonalCard(flat = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://fp.mysqil.com/")
                }
                .padding(FolkDimens.Size24), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(FolkDimens.Size4))
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
