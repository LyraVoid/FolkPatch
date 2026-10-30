package me.bmax.apatch.ui.screen.settings

import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.ui.theme.refreshTheme

@Composable
fun GeneralInterfaceSection(
    flat: Boolean,
    highlightKey: String?,
    predictiveBackTitle: String,
    predictiveBackSummary: String,
    predictiveBackEnabled: Boolean,
    onPredictiveBackEnabledChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences

    SegmentedSection(title = stringResource(R.string.settings_section_general_interface)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        item(key = "general_predictive_back", visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                title = predictiveBackTitle,
                summary = predictiveBackSummary,
                checked = predictiveBackEnabled,
                onCheckedChange = {
                    onPredictiveBackEnabledChange(it)
                    prefs.edit { putBoolean("predictive_back_enabled", it) }
                    (context as? Activity)?.recreate()
                },
            )
        }

        item(key = "general_expressive_motion") {
            var expressiveMotion by remember { mutableStateOf(prefs.getBoolean("expressive_motion", false)) }
            SegmentedSwitchItem(
                icon = Icons.Outlined.Waves,
                title = stringResource(R.string.settings_expressive_motion),
                summary = stringResource(R.string.settings_expressive_motion_summary),
                checked = expressiveMotion,
                onCheckedChange = {
                    expressiveMotion = it
                    prefs.edit { putBoolean("expressive_motion", it) }
                    refreshTheme.value = true
                },
            )
        }

     }
    }
}
