package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun homeLayoutStyleToString(style: String): Int {
    return when (style) {
        "kernelsu" -> R.string.settings_home_layout_grid
        "focus" -> R.string.settings_home_layout_focus
        "circle" -> R.string.settings_home_layout_circle
        "dashboard_ui" -> R.string.settings_home_layout_dashboard_pro
        "stats" -> R.string.settings_home_layout_stats
        else -> R.string.settings_home_layout_default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutChooseDialog(showDialog: MutableState<Boolean>, onLayoutSelected: (String) -> Unit) {
    val prefs = APApplication.sharedPreferences

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.settings_home_layout_style),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )

            val currentStyle = prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)

            Surface(
                shape = FolkShape.Corner12,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = FolkDimens.Size2
            ) {
                Column {
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_default),
                        selected = currentStyle == "default",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "default").apply()
                            onLayoutSelected("default")
                            showDialog.value = false
                        }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_grid),
                        selected = currentStyle == "kernelsu",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "kernelsu").apply()
                            onLayoutSelected("kernelsu")
                            showDialog.value = false
                        }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_focus),
                        selected = currentStyle == "focus",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "focus").apply()
                            onLayoutSelected("focus")
                            showDialog.value = false
                        }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_circle),
                        selected = currentStyle == "circle",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "circle").apply()
                            onLayoutSelected("circle")
                            showDialog.value = false
                        }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_dashboard_pro),
                        selected = currentStyle == "dashboard_ui",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "dashboard_ui").apply()
                            onLayoutSelected("dashboard_ui")
                            showDialog.value = false
                        }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_home_layout_stats),
                        selected = currentStyle == "stats",
                        onClick = {
                            prefs.edit().putString("home_layout_style", "stats").apply()
                            onLayoutSelected("stats")
                            showDialog.value = false
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    }
}

