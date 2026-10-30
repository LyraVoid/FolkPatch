package me.bmax.apatch.ui.screen.settings.general

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.*
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTitleChooseDialog(showDialog: MutableState<Boolean>, onTitleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val currentTitle = remember { prefs.getString("app_title", "folkpatch") }
    val titles = listOf(
        "custom" to stringResource(R.string.app_title_custom),
        "fpatch" to stringResource(R.string.app_title_fpatch),
        "apatch_folk" to stringResource(R.string.app_title_apatch_folk),
        "apatchx" to stringResource(R.string.app_title_apatchx),
        "apatch" to stringResource(R.string.app_title_apatch),
        "folkpatch" to stringResource(R.string.app_title_folkpatch),
        "kernelpatch" to stringResource(R.string.app_title_kernelpatch),
        "kernelsu" to stringResource(R.string.app_title_kernelsu),
        "supersu" to stringResource(R.string.app_title_supersu),
        "folksu" to stringResource(R.string.app_title_fpatch),
        "superuser" to stringResource(R.string.app_title_superuser),
        "superpatch" to stringResource(R.string.app_title_superpatch),
        "magicpatch" to stringResource(R.string.app_title_magicpatch)
    )

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        LazyColumn {
            items(titles.size, key = { it }) { index ->
                val (key, displayName) = titles[index]
                ListItem(
                    headlineContent = { Text(text = displayName) },
                    modifier = Modifier.clickable {
                        showDialog.value = false
                        prefs.edit { putString("app_title", key) }
                        onTitleChanged(key)
                    },
                    trailingContent = {
                        if (currentTitle == key) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppTitleDialog(showDialog: MutableState<Boolean>, snackBarHost: SnackbarHostState, onTitleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    var customTitle by remember {
        mutableStateOf(prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch")
    }

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(
            modifier = Modifier.padding(FolkDimens.Size16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
        ) {
            Text(
                text = stringResource(R.string.custom_app_title_dialog_title),
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                placeholder = { Text(stringResource(R.string.custom_app_title_dialog_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = {
                    val trimmed = customTitle.trim()
                    if (trimmed.isEmpty()) {
                        showDialog.value = false
                        return@TextButton
                    }
                    prefs.edit { putString("custom_app_title", trimmed) }
                    onTitleChanged(trimmed)
                    showDialog.value = false
                }) {
                    Text(stringResource(R.string.custom_app_title_dialog_confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopAppNameChooseDialog(showDialog: MutableState<Boolean>, onNameChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val currentName = remember { prefs.getString("desktop_app_name", "FolkPatch") }
    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        LazyColumn {
            item {
                ListItem(
                    headlineContent = { Text(text = "FolkPatch") },
                    modifier = Modifier.clickable {
                        showDialog.value = false
                        prefs.edit {
                            putString("desktop_app_name", "FolkPatch")
                        }
                        onNameChanged("FolkPatch")
                        LauncherIconUtils.applySaved(context)
                    },
                    trailingContent = {
                        if (currentName == "FolkPatch" || currentName == null) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(text = "FPatch") },
                    modifier = Modifier.clickable {
                        showDialog.value = false
                        prefs.edit {
                            putString("desktop_app_name", "FPatch")
                        }
                        onNameChanged("FPatch")
                        LauncherIconUtils.applySaved(context)
                    },
                    trailingContent = {
                        if (currentName == "FPatch") {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherIconStyleDialog(showDialog: MutableState<Boolean>, onStyleChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.settings_launcher_icon),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )

            val currentStyle = remember {
                prefs.getString(LauncherIconUtils.PREF_ICON_STYLE, null)
                    ?: if (prefs.getBoolean("use_alt_icon", false))
                        LauncherIconUtils.ICON_STYLE_APATCH
                    else
                        LauncherIconUtils.ICON_STYLE_ANIME
            }

            Surface(
                shape = FolkShape.Corner12,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = FolkDimens.Size2
            ) {
                Column {
                    listOf(
                        LauncherIconUtils.ICON_STYLE_ANIME to R.string.launcher_icon_style_anime,
                        LauncherIconUtils.ICON_STYLE_GEOMETRY to R.string.launcher_icon_style_geometry,
                        LauncherIconUtils.ICON_STYLE_APATCH to R.string.launcher_icon_style_apatch
                    ).forEach { (style, labelId) ->
                        SegmentedSelectableItem(
                            title = stringResource(labelId),
                            selected = currentStyle == style,
                            onClick = {
                                LauncherIconUtils.setStyle(context, style)
                                onStyleChanged(style)
                                showDialog.value = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListLoadingSchemeDialog(showDialog: MutableState<Boolean>, onSchemeChanged: (String) -> Unit = {}) {
    val prefs = APApplication.sharedPreferences

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.settings_app_list_loading_scheme),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )

            val currentScheme = remember { prefs.getString("app_list_loading_scheme", "root_service") }

            Surface(
                shape = FolkShape.Corner12,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = FolkDimens.Size2
            ) {
                Column {
                    val schemes = listOf(
                        "root_service" to R.string.app_list_loading_scheme_root_service,
                        "package_manager" to R.string.app_list_loading_scheme_package_manager
                    )

                    schemes.forEach { (scheme, labelId) ->
                        SegmentedSelectableItem(
                            title = stringResource(labelId),
                            selected = currentScheme == scheme,
                            onClick = {
                                prefs.edit { putString("app_list_loading_scheme", scheme) }
                                onSchemeChanged(scheme)
                                showDialog.value = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    }
}

