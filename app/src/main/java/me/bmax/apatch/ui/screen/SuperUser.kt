package me.bmax.apatch.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.SwitchItem
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.SuAuditLog
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils.Companion.setupWindowBlurListener
import me.bmax.apatch.util.ui.showToast


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
    var showSuperUserMenu by remember { mutableStateOf(false) }
    var showBatchConfirmDialog by remember { mutableStateOf<SuperUserViewModel.BatchAction?>(null) }

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
                        showToast(context, context.getString(R.string.su_app_action_launch_success, selectedApp!!.label))
                    }
                } else {
                    scope.launch {
                        showToast(context, context.getString(R.string.su_app_action_failed, selectedApp!!.label))
                    }
                }
                showAppActionDialog = false
            },
            onForceStop = {
                val success = viewModel.forceStopApp(selectedApp!!.packageName)
                if (success) {
                    scope.launch {
                        showToast(context, context.getString(R.string.su_app_action_force_stop_success, selectedApp!!.label))
                    }
                } else {
                    scope.launch {
                        showToast(context, context.getString(R.string.su_app_action_failed, selectedApp!!.label))
                    }
                }
                showAppActionDialog = false
            }
        )
    }

    if (showBatchConfirmDialog != null) {
        val action = showBatchConfirmDialog!!
        val count = viewModel.selectedCount
        val selectedUids = viewModel.selectionMap.entries.filter { it.value }.map { it.key }
        
        BatchActionConfirmDialog(
            action = action,
            count = count,
            onDismiss = { showBatchConfirmDialog = null },
            onConfirm = {
                when (action) {
                    SuperUserViewModel.BatchAction.GRANT_ROOT -> viewModel.batchGrantRoot(selectedUids)
                    SuperUserViewModel.BatchAction.REVOKE_ROOT -> viewModel.batchRevokeRoot(selectedUids)
                    SuperUserViewModel.BatchAction.EXCLUDE -> viewModel.batchExclude(selectedUids)
                }
                viewModel.exitSelectionMode()
                showBatchConfirmDialog = null
            }
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.appList.isEmpty()) {
            viewModel.fetchAppList()
        }
    }

    BackHandler(enabled = viewModel.isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    val filteredApps = viewModel.appList

    Scaffold(
        topBar = {
            if (viewModel.isSelectionMode) {
                SelectionTopBar(
                    selectedCount = viewModel.selectedCount,
                    onClose = { viewModel.exitSelectionMode() },
                    onGrantRoot = { showBatchConfirmDialog = SuperUserViewModel.BatchAction.GRANT_ROOT },
                    onRevokeRoot = { showBatchConfirmDialog = SuperUserViewModel.BatchAction.REVOKE_ROOT },
                    onExclude = { showBatchConfirmDialog = SuperUserViewModel.BatchAction.EXCLUDE },
                )
            } else {
                SearchAppBar(
                    title = { Text(stringResource(R.string.su_title)) },
                    searchText = viewModel.search,
                    onSearchTextChange = { viewModel.updateSearch(it) },
                    onClearClick = { viewModel.updateSearch("") },
                    leadingActions = {
                        IconButton(onClick = {
                            viewModel.enterSelectionMode()
                        }) {
                            Icon(Icons.Filled.Deselect, contentDescription = stringResource(R.string.su_multi_select_enter))
                        }
                        IconButton(onClick = {
                            showBatchExcludeDialog = true
                        }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = stringResource(R.string.su_batch_exclude_title))
                        }
                    },
                    dropdownContent = {
                        Box {
                            IconButton(onClick = { showSuperUserMenu = !showSuperUserMenu }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(id = R.string.settings)
                                )
                            }

                            WallpaperAwareDropdownMenu(
                                expanded = showSuperUserMenu,
                                onDismissRequest = { showSuperUserMenu = false },
                            ) {
                                WallpaperAwareDropdownMenuItem(
                                    text = { Text(stringResource(R.string.su_refresh)) },
                                    onClick = {
                                        showSuperUserMenu = false
                                        scope.launch { viewModel.fetchAppList() }
                                    },
                                )
                                WallpaperAwareDropdownMenuItem(
                                    text = { Text(
                                        if (viewModel.showSystemApps) stringResource(R.string.su_hide_system_apps)
                                        else stringResource(R.string.su_show_system_apps)
                                    ) },
                                    onClick = {
                                        showSuperUserMenu = false
                                        viewModel.showSystemApps = !viewModel.showSystemApps
                                    },
                                )
                                WallpaperAwareDropdownMenuItem(
                                    text = { Text(stringResource(R.string.su_backup_list)) },
                                    onClick = {
                                        showSuperUserMenu = false
                                        backupLauncher.launch("FolkPatch_list_backup.json")
                                    },
                                )
                                WallpaperAwareDropdownMenuItem(
                                    text = { Text(stringResource(R.string.su_restore_list)) },
                                    onClick = {
                                        showSuperUserMenu = false
                                        restoreLauncher.launch(arrayOf("application/json", "*/*"))
                                    },
                                )
                            }
                        }
                    },
                )
            }
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
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                                    } else {
                                        Icon(Icons.Filled.History, contentDescription = stringResource(R.string.su_audit_log_title))
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
                                    contentDescription = stringResource(R.string.script_library),
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
                                    contentDescription = stringResource(R.string.su_audit_log_title),
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
        Box(modifier = Modifier.padding(innerPadding)) {
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
            onRefresh = { scope.launch { viewModel.fetchAppList() } },
            isRefreshing = viewModel.isRefreshing,
            state = pullToRefreshState,
            indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) { PaddingValues(bottom = bottomClearance) }
                }
            ) {
                if (useLegacySuPage) {
                    items(
                        filteredApps,
                        key = { it.packageName + it.uid }
                    ) { app ->
                        AppItemLegacy(
                            app = app,
                            isSelected = viewModel.isUidSelected(app.uid),
                            selectionMode = viewModel.isSelectionMode,
                            onToggleSelection = { viewModel.toggleSelection(app.uid) },
                            onLongPress = {
                                viewModel.enterSelectionMode()
                                viewModel.toggleSelection(app.uid)
                            }
                        )
                    }
                } else {
                    item { Spacer(Modifier.height(8.dp)) }
                    splicedLazyColumnGroup(
                        items = filteredApps,
                        key = { _, app -> app.packageName + app.uid },
                        contentType = { _, _ -> "AppItem" },
                    ) { _, app ->
                        AppItemM3E(
                            app = app,
                            isSelected = viewModel.isUidSelected(app.uid),
                            selectionMode = viewModel.isSelectionMode,
                            onClick = {
                                if (viewModel.isSelectionMode) {
                                    viewModel.toggleSelection(app.uid)
                                } else {
                                    navigator.navigate(AppProfileScreenDestination(app.packageName, app.uid))
                                }
                            },
                            onLongClick = {
                                if (!viewModel.isSelectionMode) {
                                    viewModel.enterSelectionMode()
                                    viewModel.toggleSelection(app.uid)
                                } else {
                                    selectedApp = app
                                    showAppActionDialog = true
                                }
                            },
                            onToggleSelection = { viewModel.toggleSelection(app.uid) }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp)) // bottom clearance handled by contentPadding
                    }
                }
            }
        }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onGrantRoot: () -> Unit,
    onRevokeRoot: () -> Unit,
    onExclude: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(stringResource(R.string.su_multi_select_count, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onGrantRoot,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = stringResource(R.string.su_multi_select_grant_root),
                )
            }
            IconButton(
                onClick = onRevokeRoot,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = stringResource(R.string.su_multi_select_revoke_root),
                )
            }
            IconButton(
                onClick = onExclude,
                enabled = selectedCount > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = stringResource(R.string.su_multi_select_exclude),
                )
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

// ── M3E App Item ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItemM3E(
    app: SuperUserViewModel.AppInfo,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: (() -> Unit)? = null,
) {
    val config = app.config
    val rootGranted = config.allow != 0
    val excludeApp = config.exclude == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected && selectionMode) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(app.packageInfo)
                .crossfade(true)
                .build(),
            contentDescription = app.label,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                if (rootGranted) {
                    LabelText(label = "ROOT", containerColor = MaterialTheme.colorScheme.primaryContainer)
                }
                if (excludeApp) {
                    LabelText(
                        label = stringResource(id = R.string.su_pkg_excluded_label),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
        }

        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection?.invoke() },
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Legacy App Item ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItemLegacy(
    app: SuperUserViewModel.AppInfo,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val config = app.config
    var showEditProfile by remember { mutableStateOf(false) }
    var rootGranted by remember { mutableStateOf(config.allow != 0) }
    var excludeApp by remember { mutableIntStateOf(config.exclude) }

    ListItem(
        modifier = Modifier
            .background(
                if (isSelected && selectionMode)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection?.invoke()
                    } else {
                        // Tapping the row only toggles the profile editor;
                        // granting or revoking root goes through the Switch
                        // alone so a stray tap can never strip an app's root
                        // access.
                        showEditProfile = !showEditProfile
                    }
                },
                onLongClick = {
                    if (!selectionMode) {
                        onLongPress?.invoke()
                    }
                }
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
                    if (excludeApp == 1) {
                        LabelText(
                            label = stringResource(id = R.string.su_pkg_excluded_label),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        )
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
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection?.invoke() },
                )
            } else {
                ExpressiveSwitch(checked = rootGranted, onCheckedChange = {
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
            }
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

// ── Label Text Badge ──────────────────────────────────────────────────────

@Composable
fun LabelText(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = contentColorFor(containerColor),
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Options Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuperUserOptionsSheet(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSystemApps: () -> Unit,
    showSystemApps: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.su_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Refresh
            Surface(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_refresh),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Show/Hide System Apps
            Surface(
                onClick = onToggleSystemApps,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showSystemApps) {
                            stringResource(R.string.su_hide_system_apps)
                        } else {
                            stringResource(R.string.su_show_system_apps)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Backup
            Surface(
                onClick = onBackup,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_backup_list),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Restore
            Surface(
                onClick = onRestore,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_restore_list),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────

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
fun BatchActionConfirmDialog(
    action: SuperUserViewModel.BatchAction,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.su_multi_select_title)
    val content = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_confirm_grant, count)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_confirm_revoke, count)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_confirm_exclude, count)
    }
    val confirmText = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_grant_root)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_revoke_root)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_exclude)
    }
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
                    TextButton(onClick = onConfirm) {
                        Text(text = confirmText)
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
