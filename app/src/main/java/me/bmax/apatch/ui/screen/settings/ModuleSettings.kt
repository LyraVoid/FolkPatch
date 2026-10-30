package me.bmax.apatch.ui.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Dock
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import androidx.compose.material.icons.outlined.*

@Composable
fun ModuleSettingsContent(
    aPatchReady: Boolean,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val prefs = APApplication.sharedPreferences

    val disableModuleUpdateCheckTitle = stringResource(id = R.string.settings_disable_module_update_check)
    val disableModuleUpdateCheckSummary = stringResource(id = R.string.settings_disable_module_update_check_summary)

    val moreInfoTitle = stringResource(id = R.string.settings_show_more_module_info)
    val moreInfoSummary = stringResource(id = R.string.settings_show_more_module_info_summary)

    val foldSystemModuleTitle = stringResource(id = R.string.settings_fold_system_module)
    val foldSystemModuleSummary = stringResource(id = R.string.settings_fold_system_module_summary)

    val apmBatchInstallFullProcessTitle = stringResource(id = R.string.apm_batch_install_full_process)
    val apmBatchInstallFullProcessSummary = stringResource(id = R.string.apm_batch_install_full_process_summary)

    val simpleListBottomBarTitle = stringResource(id = R.string.settings_simple_list_bottom_bar)
    val simpleListBottomBarSummary = stringResource(id = R.string.settings_simple_list_bottom_bar_summary)

    val splicedCardGroupTitle = stringResource(id = R.string.settings_spliced_card_group)
    val splicedCardGroupSummary = stringResource(id = R.string.settings_spliced_card_group_summary)

    val showKpmStatusBadgeTitle = stringResource(id = R.string.settings_show_kpm_status_badge)
    val showKpmStatusBadgeSummary = stringResource(id = R.string.settings_show_kpm_status_badge_summary)

    var disableModuleUpdateCheck by remember { mutableStateOf(prefs.getBoolean("disable_module_update_check", false)) }
    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var apmBatchInstallFullProcess by remember { mutableStateOf(prefs.getBoolean("apm_batch_install_full_process", false)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }
    var showKpmStatusBadge by remember { mutableStateOf(prefs.getBoolean("show_kpm_status_badge", true)) }

    SegmentedSection(title = stringResource(R.string.settings_section_module_display)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "module_more_info") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Info,
                    title = moreInfoTitle,
                    summary = moreInfoSummary,
                    checked = showMoreModuleInfo,
                    onCheckedChange = {
                        showMoreModuleInfo = it
                        prefs.edit().putBoolean("show_more_module_info", it).apply()
                    },
                )
            }

            item(key = "module_fold_system") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Folder,
                    title = foldSystemModuleTitle,
                    summary = foldSystemModuleSummary,
                    checked = foldSystemModule,
                    onCheckedChange = {
                        foldSystemModule = it
                        prefs.edit().putBoolean("fold_system_module", it).apply()
                    },
                )
            }

            item(key = "module_kpm_status_badge") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Archive,
                    title = showKpmStatusBadgeTitle,
                    summary = showKpmStatusBadgeSummary,
                    checked = showKpmStatusBadge,
                    onCheckedChange = {
                        showKpmStatusBadge = it
                        prefs.edit().putBoolean("show_kpm_status_badge", it).apply()
                    },
                )
            }
        }
    }

    SegmentedSection(title = stringResource(R.string.settings_section_module_update)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "module_disable_update") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Update,
                    title = disableModuleUpdateCheckTitle,
                    summary = disableModuleUpdateCheckSummary,
                    checked = disableModuleUpdateCheck,
                    onCheckedChange = {
                        disableModuleUpdateCheck = it
                        prefs.edit().putBoolean("disable_module_update_check", it).apply()
                    },
                )
            }

            item(key = "module_batch_install") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Download,
                    title = apmBatchInstallFullProcessTitle,
                    summary = apmBatchInstallFullProcessSummary,
                    checked = apmBatchInstallFullProcess,
                    onCheckedChange = {
                        apmBatchInstallFullProcess = it
                        prefs.edit().putBoolean("apm_batch_install_full_process", it).apply()
                    },
                )
            }
        }
    }

    SegmentedSection(title = stringResource(R.string.settings_section_general_interface)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "module_simple_list") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Dock,
                    title = simpleListBottomBarTitle,
                    summary = simpleListBottomBarSummary,
                    checked = simpleListBottomBar,
                    onCheckedChange = {
                        simpleListBottomBar = it
                        prefs.edit().putBoolean("simple_list_bottom_bar", it).apply()
                    },
                )
            }

            item(key = "module_spliced_card") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.ViewAgenda,
                    title = splicedCardGroupTitle,
                    summary = splicedCardGroupSummary,
                    checked = splicedCardGroup,
                    onCheckedChange = {
                        splicedCardGroup = it
                        prefs.edit().putBoolean("spliced_card_group", it).apply()
                    },
                )
            }
        }
    }
}
