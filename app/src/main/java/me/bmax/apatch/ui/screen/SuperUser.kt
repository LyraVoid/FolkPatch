package me.bmax.apatch.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScriptLibraryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuAuditLogScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.LocalBottomBarVisible
import me.bmax.apatch.ui.LocalIsFloatingNavMode
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.SuAuditLog
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils.Companion.setupWindowBlurListener


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuperUserScreen(navigator: DestinationsNavigator) {
    val prefs = APApplication.sharedPreferences
    val useLegacySuPage = prefs.getBoolean("use_legacy_su_page", false)

    SuperUserScreenModern(navigator, useLegacySuPage)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SuperUserScreenModern(navigator: DestinationsNavigator, useLegacySuPage: Boolean) {
    val viewModel = viewModel<SuperUserViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.backupAppList(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreAppList(context, it) }
    }

    var showBatchExcludeDialog by remember { mutableStateOf(false) }
    var showAppActionDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<SuperUserViewModel.AppInfo?>(null) }

    if (showBatchExcludeDialog) {
        BatchExcludeDialog(
            onDismiss = { showBatchExcludeDialog = false },
            onExclude = {
                viewModel.excludeAll()
                showBatchExcludeDialog = false
            },
            onReverseExclude = {
                viewModel.reverseExcludeAll()
                showBatchExcludeDialog = false
            }
        )
    }

    if (showAppActionDialog && selectedApp != null) {
        AppActionDialog(
            app = selectedApp!!,
            onDismiss = { showAppActionDialog = false },
            onLaunch = {
                val success = viewModel.launchApp(context, selectedApp!!.packageName)
                if (success) {
                    scope.launch {
                        // Show toast for launch success
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.su_app_action_launch_success, selectedApp!!.label),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    scope.launch {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.su_app_action_failed, selectedApp!!.label),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                showAppActionDialog = false
            },
            onForceStop = {
                val success = viewModel.forceStopApp(selectedApp!!.packageName)
                if (success) {
                    scope.launch {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.su_app_action_force_stop_success, selectedApp!!.label),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    scope.launch {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.su_app_action_failed, selectedApp!!.label),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                showAppActionDialog = false
            }
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(stringResource(R.string.su_title)) },
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                onClearClick = { viewModel.search = "" },
                leadingActions = {
                    IconButton(onClick = {
                        showBatchExcludeDialog = true
                    }) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = stringResource(R.string.su_batch_exclude_title))
                    }
                },
                dropdownContent = {
                    var showDropdown by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showDropdown = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(id = R.string.settings)
                        )

                        WallpaperAwareDropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            WallpaperAwareDropdownMenuItem(
                                text = { Text(stringResource(R.string.su_refresh)) },
                                onClick = {
                                    scope.launch {
                                        viewModel.fetchAppList()
                                    }
                                    showDropdown = false
                                }
                            )

                            WallpaperAwareDropdownMenuItem(
                                text = {
                                    Text(
                                        if (viewModel.showSystemApps) {
                                            stringResource(R.string.su_hide_system_apps)
                                        } else {
                                            stringResource(R.string.su_show_system_apps)
                                        }
                                    )
                                },
                                onClick = {
                                    viewModel.showSystemApps = !viewModel.showSystemApps
                                    showDropdown = false
                                }
                            )

                            WallpaperAwareDropdownMenuItem(
                                text = { Text(stringResource(R.string.su_backup_list)) },
                                onClick = {
                                    backupLauncher.launch("FolkPatch_list_backup.json")
                                    showDropdown = false
                                }
                            )

                            WallpaperAwareDropdownMenuItem(
                                text = { Text(stringResource(R.string.su_restore_list)) },
                                onClick = {
                                    restoreLauncher.launch(arrayOf("application/json", "*/*"))
                                    showDropdown = false
                                }
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = run {
            {
                var fabExpanded by remember { mutableStateOf(false) }
                val isFloatingMode = LocalIsFloatingNavMode.current

                val fabContent: @Composable () -> Unit = {
                    FloatingActionButtonMenu(
                        expanded = fabExpanded,
                        button = {
                            FloatingActionButton(
                                onClick = { fabExpanded = !fabExpanded },
                                shape = CircleShape,
                                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ) {
                                Crossfade(
                                    targetState = fabExpanded,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "fabIconCrossfade"
                                ) { isExpanded ->
                                    if (isExpanded) {
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                    } else {
                                        Icon(Icons.Filled.History, contentDescription = null)
                                    }
                                }
                            }
                        },
                    ) {
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabExpanded = false
                                navigator.navigate(ScriptLibraryScreenDestination)
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text(stringResource(R.string.script_library), style = MaterialTheme.typography.bodyMedium) },
                        )
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabExpanded = false
                                navigator.navigate(SuAuditLogScreenDestination)
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = { Text(stringResource(R.string.su_audit_log_title), style = MaterialTheme.typography.bodyMedium) },
                        )
                    }
                }

                val bottomBarVisible = LocalBottomBarVisible.current.value
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val animatedOffset by animateDpAsState(
                    targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else 0.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "fabOffset"
                )
                if (isFloatingMode) {
                    Box(modifier = Modifier.offset(y = animatedOffset)) {
                        fabContent()
                    }
                } else {
                    fabContent()
                }
            }
        }
    ) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            onRefresh = { scope.launch { viewModel.fetchAppList() } },
            isRefreshing = viewModel.isRefreshing,
            state = pullToRefreshState,
            indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(viewModel.appList.filter { it.packageName != apApp.packageName }, key = { it.packageName + it.uid }) { app ->
                    if (useLegacySuPage) {
                        AppItemLegacy(app)
                    } else {
                        AppItem(
                            app = app,
                            onClick = {
                                navigator.navigate(AppProfileScreenDestination(app.packageName, app.uid))
                            },
                            onLongClick = {
                                selectedApp = app
                                showAppActionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: SuperUserViewModel.AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val config = app.config
    val rootGranted = config.allow != 0
    val excludeApp = config.exclude == 1

    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(app.label) },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(app.packageInfo)
                    .crossfade(true).build(),
                contentDescription = app.label,
                modifier = Modifier
                    .padding(4.dp)
                    .width(48.dp)
                    .height(48.dp)
            )
        },
        supportingContent = {
            Column {
                Text(app.packageName)
                FlowRow {
                    if (excludeApp) {
                        LabelText(label = stringResource(id = R.string.su_pkg_excluded_label))
                    }
                    if (rootGranted) {
                        // LabelText(label = config.profile.uid.toString())
                        LabelText(label = "ROOT")
                        // LabelText(label = config.profile.toUid.toString())
                        // LabelText(
                        //     label = when {
                        //         // todo: valid scontext ?
                        //         config.profile.scontext.isNotEmpty() -> config.profile.scontext
                        //         else -> stringResource(id = R.string.su_selinux_via_hook)
                        //     }
                        // )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItemLegacy(
    app: SuperUserViewModel.AppInfo,
) {
    val config = app.config
    var showEditProfile by remember { mutableStateOf(false) }
    var rootGranted by remember { mutableStateOf(config.allow != 0) }
    var excludeApp by remember { mutableIntStateOf(config.exclude) }

    ListItem(
        modifier = Modifier.clickable(onClick = {
            if (!rootGranted) {
                showEditProfile = !showEditProfile
            } else {
                rootGranted = false
                config.allow = 0
                Natives.revokeSu(app.uid)
                PkgConfig.changeConfig(config)
                SuAuditLog.logRevoke(app.packageName, app.uid)
            }
        }),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(app.label) },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(app.packageInfo)
                    .crossfade(true).build(),
                contentDescription = app.label,
                modifier = Modifier
                    .padding(4.dp)
                    .width(48.dp)
                    .height(48.dp)
            )
        },
        supportingContent = {
            Column {
                Text(app.packageName)
                FlowRow {
                    if (excludeApp == 1) {
                        LabelText(label = stringResource(id = R.string.su_pkg_excluded_label))
                    }
                    if (rootGranted) {
                        LabelText(label = config.profile.uid.toString())
                        LabelText(label = config.profile.toUid.toString())
                        LabelText(
                            label = when {
                                config.profile.scontext.isNotEmpty() -> config.profile.scontext
                                else -> stringResource(id = R.string.su_selinux_via_hook)
                            }
                        )
                    }
                }
            }
        },
        trailingContent = {
            Switch(checked = rootGranted, onCheckedChange = {
                rootGranted = !rootGranted
                if (rootGranted) {
                    excludeApp = 0
                    config.allow = 1
                    config.exclude = 0
                    config.profile.scontext = APApplication.MAGISK_SCONTEXT
                } else {
                    config.allow = 0
                }
                config.profile.uid = app.uid
                PkgConfig.changeConfig(config)
                if (config.allow == 1) {
                    Natives.grantSu(app.uid, 0, config.profile.scontext)
                    Natives.setUidExclude(app.uid, 0)
                    SuAuditLog.logGrant(app.packageName, app.uid)
                } else {
                    Natives.revokeSu(app.uid)
                    SuAuditLog.logRevoke(app.packageName, app.uid)
                }
            })
        },
    )

    AnimatedVisibility(
        visible = showEditProfile && !rootGranted,
        modifier = Modifier.fillMaxWidth()
    ) {
        SwitchItem(
            icon = Icons.Filled.Security,
            title = stringResource(id = R.string.su_pkg_excluded_setting_title),
            summary = stringResource(id = R.string.su_pkg_excluded_setting_summary),
            checked = excludeApp == 1,
            onCheckedChange = {
                if (it) {
                    excludeApp = 1
                    config.allow = 0
                    config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                    Natives.revokeSu(app.uid)
                    SuAuditLog.logExclude(app.packageName, app.uid)
                } else {
                    excludeApp = 0
                    SuAuditLog.logRevoke(app.packageName, app.uid)
                }
                config.exclude = excludeApp
                config.profile.uid = app.uid
                PkgConfig.changeConfig(config)
                Natives.setUidExclude(app.uid, excludeApp)
            },
        )
    }
}

@Composable
fun LabelText(label: String) {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp)
            .background(
                Color.Black, shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 5.dp),
            style = TextStyle(
                fontSize = 8.sp,
                color = Color.White,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExcludeDialog(
    onDismiss: () -> Unit,
    onExclude: () -> Unit,
    onReverseExclude: () -> Unit
) {
    val title = stringResource(R.string.su_batch_exclude_title)
    val content = stringResource(R.string.su_batch_exclude_content)
    val excludeText = stringResource(R.string.su_exclude_btn)
    val reverseText = stringResource(R.string.su_exclude_reverse_btn)
    val cancelText = stringResource(android.R.string.cancel)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = content, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = cancelText)
                    }
                    TextButton(onClick = onExclude) {
                        Text(text = excludeText)
                    }
                    TextButton(onClick = onReverseExclude) {
                        Text(text = reverseText)
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionDialog(
    app: SuperUserViewModel.AppInfo,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit
) {
    val title = stringResource(R.string.su_app_action_title)
    val content = stringResource(R.string.su_app_action_content)
    val launchText = stringResource(R.string.su_app_action_launch)
    val forceStopText = stringResource(R.string.su_app_action_force_stop)
    val cancelText = stringResource(android.R.string.cancel)

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.SecureOff,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                }
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(PaddingValues(bottom = 24.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = cancelText)
                    }
                    TextButton(onClick = onLaunch) {
                        Text(text = launchText)
                    }
                    TextButton(onClick = onForceStop) {
                        Text(text = forceStopText)
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
