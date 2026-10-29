package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.navigation.BottomBarDestination
import me.bmax.apatch.util.BottomBarIconConfig
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import me.bmax.apatch.ui.component.SegmentedColumnScope

fun SegmentedColumnScope.appearanceNavCustomIconsItem(
    flat: Boolean,
    context: Context,
    prefs: SharedPreferences,
    scope: CoroutineScope,
    snackBarHost: SnackbarHostState,
) {
    item(key = "appearance_nav_custom_icons") {
        val customNavIconsEnabled = remember { mutableStateOf(prefs.getBoolean("nav_icon_custom_enabled", false)) }
        var editingDestName by remember { mutableStateOf<String?>(null) }
        // Observe config revision so the previews below refresh immediately after pick/clear.
        val iconRevision by BottomBarIconConfig.revision.collectAsStateWithLifecycle()
        val iconPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            val dest = editingDestName ?: return@rememberLauncherForActivityResult
            if (uri != null) {
                // Copy the picked image into internal storage so it survives app
                // restarts (content:// read grants are only temporary).
                scope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        BottomBarIconConfig.saveCustomIcon(context, dest, uri)
                    }
                    snackBarHost.showSnackbar(
                        context.getString(
                            if (saved) R.string.nav_icon_set
                            else R.string.nav_icon_set_failed
                        )
                    )
                }
            }
            editingDestName = null
        }

        SegmentedSwitchItem(
            icon = Icons.Outlined.Image,
            title = stringResource(R.string.settings_nav_custom_icons),
            summary = stringResource(R.string.settings_nav_custom_icons_summary),
            checked = customNavIconsEnabled.value,
            onCheckedChange = {
                customNavIconsEnabled.value = it
                BottomBarIconConfig.isEnabled = it
            }
        )

        if (customNavIconsEnabled.value) {
            Spacer(Modifier.height(FolkDimens.Size8))
            val navDestinations = listOf(
                Triple("Home", R.string.nav_icon_home, BottomBarDestination.Home.iconSelected),
                Triple("KModule", R.string.nav_icon_kpm, BottomBarDestination.KModule.iconSelected),
                Triple("SuperUser", R.string.nav_icon_superuser, BottomBarDestination.SuperUser.iconSelected),
                Triple("AModule", R.string.nav_icon_apm, BottomBarDestination.AModule.iconSelected),
                Triple("Settings", R.string.nav_icon_settings, BottomBarDestination.Settings.iconSelected),
            )

            Column {
                navDestinations.forEach { (destName, labelRes, defaultIcon) ->
                    val customUri = remember(iconRevision, destName) { prefs.getString("nav_icon_$destName", null) }
                    TonalCard(
                        flat = flat,
                        onClick = {
                            editingDestName = destName
                            try { iconPickerLauncher.launch("image/*") } catch (_: Throwable) {}
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(FolkDimens.Size16),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (customUri != null) {
                                AsyncImage(
                                    model = customUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(FolkDimens.Size36).clip(RoundedCornerShape(FolkDimens.Size6)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = defaultIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(FolkDimens.Size36)
                                )
                            }
                            Spacer(Modifier.width(FolkDimens.Size16))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(labelRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (customUri != null) stringResource(R.string.nav_icon_custom_selected)
                                    else stringResource(R.string.nav_icon_default),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (customUri != null) {
                                IconButton(
                                    onClick = {
                                        BottomBarIconConfig.clearCustomIcon(context, destName)
                                        scope.launch {
                                            snackBarHost.showSnackbar(context.getString(R.string.nav_icon_cleared))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.Close, stringResource(R.string.nav_icon_clear), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(FolkDimens.Size4))
                }
            }
        }
    }
}
