package me.bmax.apatch.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import me.bmax.apatch.APApplication

object LauncherIconUtils {
    private const val MAIN_ACTIVITY = ".ui.MainActivityDefault"
    private const val ALIAS_ACTIVITY = ".ui.MainActivityAlias"
    private const val ALIAS_ACTIVITY_SU = ".ui.MainActivityAliasSu"
    private const val ALIAS_ACTIVITY_ALT_SU = ".ui.MainActivityAliasAltSu"

    fun updateLauncherState(context: Context) {
        val prefs = APApplication.sharedPreferences
        val useAlt = prefs.getBoolean("use_alt_icon", false)
        val appName = prefs.getString("desktop_app_name", "FolkPatch")
        val isSu = appName == "FPatch"

        val pm = context.packageManager
        val basePackage = APApplication::class.java.`package`?.name ?: "me.bmax.apatch"
        
        val mainComponent = ComponentName(context.packageName, basePackage + MAIN_ACTIVITY)
        val aliasComponent = ComponentName(context.packageName, basePackage + ALIAS_ACTIVITY)
        val aliasSuComponent = ComponentName(context.packageName, basePackage + ALIAS_ACTIVITY_SU)
        val aliasAltSuComponent = ComponentName(context.packageName, basePackage + ALIAS_ACTIVITY_ALT_SU)


        val targetComponent = when {
            useAlt && isSu -> aliasAltSuComponent
            useAlt && !isSu -> aliasComponent
            !useAlt && isSu -> aliasSuComponent
            else -> mainComponent
        }

        val allComponents = listOf(mainComponent, aliasComponent, aliasSuComponent, aliasAltSuComponent)

        try {
            // Enable target
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Disable others
            allComponents.filter { it != targetComponent }.forEach {
                pm.setComponentEnabledSetting(
                    it,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Deprecated but kept for compatibility if needed, redirects to updateLauncherState
    fun toggleLauncherIcon(context: Context, useAlt: Boolean) {
        updateLauncherState(context)
    }

    fun applySaved(context: Context) {
        updateLauncherState(context)
    }
}
