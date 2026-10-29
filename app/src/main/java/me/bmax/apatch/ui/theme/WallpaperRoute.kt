package me.bmax.apatch.ui.theme

import com.ramcosta.composedestinations.generated.destinations.APModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ApiMarketplaceScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FaqScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KPModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LanguagePickerScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MyThemesScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineKPMScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlinePluginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineScriptScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.generated.destinations.PluginLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PluginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RepoModuleDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScriptExecutionLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScriptLibraryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ShizukuLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ShizukuManagementScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuAuditLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeStoreScreenDestination
import com.ramcosta.composedestinations.generated.destinations.WallpaperGalleryScreenDestination

/**
 * The page route whose wallpaper is inherited by the current destination.
 *
 * A route with its own wallpaper wins. Otherwise the nearest mapped route in the back stack is
 * used, which keeps detail flows on the wallpaper of the page they were opened from.
 */
internal fun wallpaperRouteFor(
    backStack: List<String?>,
    fallbackRoute: String?,
): String? =
    backStack.asReversed().firstNotNullOfOrNull { it?.wallpaperOwnerRoute() } ?: fallbackRoute

private fun String.wallpaperOwnerRoute(): String? = when (this) {
    HomeScreenDestination.route -> HomeScreenDestination.route
    KPModuleScreenDestination.route -> KPModuleScreenDestination.route
    SuperUserScreenDestination.route -> SuperUserScreenDestination.route
    APModuleScreenDestination.route -> APModuleScreenDestination.route
    SettingScreenDestination.route -> SettingScreenDestination.route

    PatchesDestination.route,
    InstallModeSelectScreenDestination.route,
    -> HomeScreenDestination.route

    OnlineKPMScreenDestination.route -> KPModuleScreenDestination.route

    OnlineModuleScreenDestination.route,
    RepoModuleDetailScreenDestination.route,
    ApmBulkInstallScreenDestination.route,
    ExecuteAPMActionScreenDestination.route,
    -> APModuleScreenDestination.route

    AppProfileScreenDestination.route,
    SuAuditLogScreenDestination.route,
    ScriptLibraryScreenDestination.route,
    ScriptExecutionLogScreenDestination.route,
    OnlineScriptScreenDestination.route,
    -> SuperUserScreenDestination.route

    AboutScreenDestination.route,
    ApiMarketplaceScreenDestination.route,
    AppearanceSettingsScreenDestination.route,
    BackupSettingsScreenDestination.route,
    BehaviorSettingsScreenDestination.route,
    FaqScreenDestination.route,
    FunctionSettingsScreenDestination.route,
    GeneralSettingsScreenDestination.route,
    LanguagePickerScreenDestination.route,
    ModuleSettingsScreenDestination.route,
    MultimediaSettingsScreenDestination.route,
    MyThemesScreenDestination.route,
    PluginScreenDestination.route,
    PluginLogScreenDestination.route,
    OnlinePluginScreenDestination.route,
    SecuritySettingsScreenDestination.route,
    ShizukuLogScreenDestination.route,
    ShizukuManagementScreenDestination.route,
    ThemeStoreScreenDestination.route,
    WallpaperGalleryScreenDestination.route,
    -> SettingScreenDestination.route

    else -> null
}
