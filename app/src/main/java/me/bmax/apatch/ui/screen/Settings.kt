package me.bmax.apatch.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.ui.NavigationBarsSpacer

import com.ramcosta.composedestinations.generated.destinations.GeneralSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.AppearanceSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BehaviorSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SecuritySettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.BackupSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModuleSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.FunctionSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MultimediaSettingsScreenDestination
import androidx.compose.foundation.background

@Destination<RootGraph>
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val aPatchReady =
        (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

    var showDevDialog by rememberSaveable { mutableStateOf(false) }

    DeveloperInfo(
        showDialog = showDevDialog
    ) {
        showDevDialog = false
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { showDevDialog = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SettingsListItem(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.settings_category_general),
                    summary = stringResource(R.string.settings_category_general_summary),
                    onClick = { navigator.navigate(GeneralSettingsScreenDestination) },
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Filled.Palette,
                    title = stringResource(R.string.settings_category_appearance),
                    summary = stringResource(R.string.settings_category_appearance_summary),
                    onClick = { navigator.navigate(AppearanceSettingsScreenDestination) },
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Filled.Visibility,
                    title = stringResource(R.string.settings_category_behavior),
                    summary = stringResource(R.string.settings_category_behavior_summary),
                    onClick = { navigator.navigate(BehaviorSettingsScreenDestination) },
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.settings_category_security),
                    summary = stringResource(R.string.settings_category_security_summary),
                    onClick = { navigator.navigate(SecuritySettingsScreenDestination) },
                )
            }

            if (aPatchReady) {
                item {
                    SettingsListItem(
                        icon = Icons.Filled.Cloud,
                        title = stringResource(R.string.settings_category_backup),
                        summary = stringResource(R.string.settings_category_backup_summary),
                        onClick = { navigator.navigate(BackupSettingsScreenDestination) },
                    )
                }

                item {
                    SettingsListItem(
                        icon = Icons.Filled.Extension,
                        title = stringResource(R.string.settings_category_module),
                        summary = stringResource(R.string.settings_category_module_summary),
                        onClick = { navigator.navigate(ModuleSettingsScreenDestination) },
                    )
                }
            }

            item {
                SettingsListItem(
                    icon = Icons.Filled.Tune,
                    title = stringResource(R.string.settings_category_function),
                    summary = stringResource(R.string.settings_category_function_summary),
                    onClick = { navigator.navigate(FunctionSettingsScreenDestination) },
                )
            }

            item {
                SettingsListItem(
                    icon = Icons.Filled.MusicNote,
                    title = stringResource(R.string.settings_category_multimedia),
                    summary = stringResource(R.string.settings_category_multimedia_summary),
                    onClick = { navigator.navigate(MultimediaSettingsScreenDestination) },
                    showDivider = false,
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                NavigationBarsSpacer()
            }
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfo(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/LyraVoid/FolkPatch"
    val telegramUrl = "https://t.me/FolkPatch"
    val sociabuzzUrl = "https://ifdian.net/a/matsuzaka_yuki"

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("http://q.qlogo.cn/headimg_dl?dst_uin=3231515355&spec=640&img_type=jpg")
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Developer Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Matsuzaka Yuki",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.developer_and_maintainer),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "\"美しい世界を見てきましょう\"",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.github),
                            contentDescription = stringResource(R.string.github),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.github))
                    }

                    FilledTonalButton(
                        onClick = { uriHandler.openUri(telegramUrl) },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(R.string.telegram),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.telegram))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = { uriHandler.openUri(sociabuzzUrl) },
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Coffee,
                        contentDescription = stringResource(R.string.support_or_donate),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_or_donate))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
