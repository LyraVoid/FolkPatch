package me.bmax.apatch.ui.screen.superuser

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CheckableDropdownMenuItem
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AppProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScriptLibraryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuAuditLogScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
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
    val haptic = LocalHapticFeedback.current

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

    FolkScaffold(
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
                            Icon(Icons.Outlined.Deselect, contentDescription = stringResource(R.string.su_multi_select_enter))
                        }
                        IconButton(onClick = {
                            showBatchExcludeDialog = true
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.PlaylistAddCheck, contentDescription = stringResource(R.string.su_batch_exclude_title))
                        }
                    },
                    dropdownContent = {
                        Box {
                            IconButton(onClick = { showSuperUserMenu = !showSuperUserMenu }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(id = R.string.settings)
                                )
                            }

                            DropdownMenuPopup(
                                expanded = showSuperUserMenu,
                                onDismissRequest = { showSuperUserMenu = false },
                            ) {
                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    SelectableDropdownMenuItem(
                                        text = { Text(stringResource(R.string.su_refresh)) },
                                        selected = false,
                                        onClick = {
                                            showSuperUserMenu = false
                                            scope.launch { viewModel.fetchAppList() }
                                        },
                                        shapes = MenuDefaults.itemShape(index = 0, count = 1),
                                    )
                                }

                                Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    CheckableDropdownMenuItem(
                                        text = {
                                            Text(
                                                if (viewModel.showSystemApps) {
                                                    stringResource(R.string.su_hide_system_apps)
                                                } else {
                                                    stringResource(R.string.su_show_system_apps)
                                                }
                                            )
                                        },
                                        checked = viewModel.showSystemApps,
                                        checkedLeadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                            )
                                        },
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                            showSuperUserMenu = false
                                            viewModel.showSystemApps = !viewModel.showSystemApps
                                        },
                                        shapes = MenuDefaults.itemShape(index = 0, count = 1),
                                    )
                                }

                                Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                                DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                                    SelectableDropdownMenuItem(
                                        text = { Text(stringResource(R.string.su_backup_list)) },
                                        selected = false,
                                        onClick = {
                                            showSuperUserMenu = false
                                            backupLauncher.launch("FolkPatch_list_backup.json")
                                        },
                                        shapes = MenuDefaults.itemShape(index = 0, count = 2),
                                    )
                                    SelectableDropdownMenuItem(
                                        text = { Text(stringResource(R.string.su_restore_list)) },
                                        selected = false,
                                        onClick = {
                                            showSuperUserMenu = false
                                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        shapes = MenuDefaults.itemShape(index = 1, count = 2),
                                    )
                                }
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
                                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                    label = "fabIconCrossfade"
                                ) { isExpanded ->
                                    if (isExpanded) {
                                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.close))
                                    } else {
                                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.su_audit_log_title))
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
                                    Icons.Outlined.Terminal,
                                    contentDescription = stringResource(R.string.script_library),
                                    modifier = Modifier.size(FolkDimens.Size18)
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
                                    Icons.Outlined.History,
                                    contentDescription = stringResource(R.string.su_audit_log_title),
                                    modifier = Modifier.size(FolkDimens.Size18)
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
                    targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else FolkDimens.Size0,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
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
        },
        // The list already reserves room for the FAB and the floating bar via
        // fabNavBottomClearance, so the scaffold must not add more.
        addBottomClearance = false,
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
                    item { Spacer(Modifier.height(FolkDimens.Size8)) }
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
                        Spacer(Modifier.height(FolkDimens.Size8)) // bottom clearance handled by contentPadding
                    }
                }
            }
        }

        }
    }
}

