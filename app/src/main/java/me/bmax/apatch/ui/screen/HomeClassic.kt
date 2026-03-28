package me.bmax.apatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.generated.destinations.UninstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.isInDarkTheme
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val classicManagerVersion = getManagerVersion()

@Composable
fun ClassicHomeScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = MiuixScrollBehavior()

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    Scaffold(
        topBar = {
            TopBarList(
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BackupWarningCardList()
                    ClassicWorkCard(
                        kpState = kpState,
                        apState = apState,
                        navigator = navigator
                    )
                    if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCardList(apState)
                    }
                    InfoCardList(kpState, apState)
                    val hideAboutCard =
                        APApplication.sharedPreferences.getBoolean("hide_about_card", false)
                    if (!hideAboutCard) {
                        LearnMoreCardList()
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicWorkCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator
) {
    val showAuthFailedTipDialog = remember { mutableStateOf(false) }
    AuthFailedTipDialog(showAuthFailedTipDialog)

    val showAuthKeyDialog = remember { mutableStateOf(false) }
    AuthSuperKey(showAuthKeyDialog, showAuthFailedTipDialog)

    val prefs = APApplication.sharedPreferences
    val colorMode = prefs.getInt("color_mode", 0)
    val isMonet = colorMode >= 3
    val isDark = isInDarkTheme(colorMode)

    val cardBg = when (kpState) {
        APApplication.State.KERNELPATCH_INSTALLED -> when {
            isMonet -> colorScheme.primaryContainer
            isDark -> Color(0xFF1A3825)
            else -> Color(0xFFDFFAE4)
        }
        APApplication.State.KERNELPATCH_NEED_UPDATE -> colorScheme.secondaryContainer
        APApplication.State.KERNELPATCH_NEED_REBOOT -> colorScheme.errorContainer
        else -> when {
            isMonet -> colorScheme.secondaryContainer
            isDark -> Color(0xFF2A2A2A)
            else -> Color(0xFFF0F0F0)
        }
    }

    val contentColor = when (kpState) {
        APApplication.State.KERNELPATCH_INSTALLED -> when {
            isMonet -> colorScheme.onPrimaryContainer
            isDark -> Color(0xFFA8DABC)
            else -> Color(0xFF003920)
        }
        APApplication.State.KERNELPATCH_NEED_UPDATE -> colorScheme.onSecondaryContainer
        APApplication.State.KERNELPATCH_NEED_REBOOT -> colorScheme.onErrorContainer
        else -> when {
            isMonet -> colorScheme.onSecondaryContainer
            isDark -> Color(0xFFE0E0E0)
            else -> Color(0xFF333333)
        }
    }

    val decoIconColor = when (kpState) {
        APApplication.State.KERNELPATCH_INSTALLED -> if (isMonet) {
            colorScheme.primary.copy(alpha = 0.8f)
        } else {
            Color(0xFF36D167)
        }
        APApplication.State.KERNELPATCH_NEED_UPDATE -> colorScheme.secondary
        APApplication.State.KERNELPATCH_NEED_REBOOT -> colorScheme.error
        else -> colorScheme.outline
    }

    Card(
        modifier = Modifier.height(140.dp),
        colors = CardDefaults.defaultColors(color = cardBg),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(50.dp, 30.dp),
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
                    tint = decoIconColor,
                    contentDescription = null
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (kpState != APApplication.State.KERNELPATCH_INSTALLED &&
                    kpState != APApplication.State.UNKNOWN_STATE) {
                    Icon(
                        imageVector = when (kpState) {
                            APApplication.State.KERNELPATCH_NEED_UPDATE -> Icons.Outlined.SystemUpdate
                            APApplication.State.KERNELPATCH_NEED_REBOOT -> Icons.Filled.Refresh
                            else -> Icons.Rounded.ErrorOutline
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = contentColor
                    )

                    Spacer(Modifier.width(24.dp))
                }

                Column {
                    Text(
                        text = when (kpState) {
                            APApplication.State.KERNELPATCH_INSTALLED -> stringResource(R.string.home_working)
                            APApplication.State.KERNELPATCH_NEED_UPDATE -> stringResource(R.string.home_need_update)
                            APApplication.State.KERNELPATCH_NEED_REBOOT -> stringResource(R.string.home_ap_cando_reboot)
                            else -> stringResource(R.string.home_not_installed)
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = when (kpState) {
                            APApplication.State.KERNELPATCH_INSTALLED ->
                                "${classicManagerVersion.first} (${classicManagerVersion.second})"

                            APApplication.State.KERNELPATCH_NEED_UPDATE ->
                                "${Version.installedKPVString()} → ${Version.buildKPVString()}"

                            APApplication.State.UNKNOWN_STATE ->
                                stringResource(R.string.home_click_to_install)

                            else -> ""
                        },
                        fontSize = 16.sp,
                        color = contentColor,
                    )

                    Spacer(Modifier.height(27.dp))

                    if (kpState == APApplication.State.KERNELPATCH_INSTALLED) {
                        Text(
                            text = if (apState == APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                                "Pattern: KernelPatch"
                            } else {
                                "Pattern: Full"
                            },
                            fontSize = 16.8.sp,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}
