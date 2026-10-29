package me.bmax.apatch.ui.theme

import com.ramcosta.composedestinations.generated.destinations.APModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PluginLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RepoModuleDetailScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuAuditLogScreenDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperRouteTest {
    @Test
    fun settingsChildUsesSettingsWallpaper() {
        assertEquals(
            SettingScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf(
                    HomeScreenDestination.route,
                    SettingScreenDestination.route,
                    AppearanceSettingsScreenDestination.route,
                ),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
    }

    @Test
    fun unmappedChildInheritsNearestParent() {
        assertEquals(
            SettingScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf(
                    HomeScreenDestination.route,
                    SettingScreenDestination.route,
                    "temporary_preview",
                ),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
    }

    @Test
    fun routeWithItsOwnSectionOverridesVisibleParent() {
        assertEquals(
            SuperUserScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf(HomeScreenDestination.route, SuAuditLogScreenDestination.route),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
        assertEquals(
            APModuleScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf(
                    HomeScreenDestination.route,
                    RepoModuleDetailScreenDestination.route,
                ),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
    }

    @Test
    fun pluginChildrenStayWithSettingsBecauseFolkPatchHasNoPluginWallpaperSlot() {
        assertEquals(
            SettingScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf(PluginLogScreenDestination.route),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
    }

    @Test
    fun fallbackIsUsedWhenNoRouteOwnsAWallpaper() {
        assertEquals(
            HomeScreenDestination.route,
            wallpaperRouteFor(
                backStack = listOf("temporary_preview"),
                fallbackRoute = HomeScreenDestination.route,
            ),
        )
    }
}
