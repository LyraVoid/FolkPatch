package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.util.ui.showToast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.viewmodel.safeKpmModuleId
import me.bmax.apatch.util.rootShellForResult
import androidx.compose.ui.platform.LocalConfiguration
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.component.folk.FolkStateTone
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Warning

private lateinit var targetKPMToControl: KPModel.KPMInfo

private data class UninstallResult(
    val unloaded: Boolean,
    val removed: Boolean,
)

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KPModuleList(
    viewModel: KPModuleViewModel,
    moduleList: List<KPModel.KPMInfo>,
    modifier: Modifier = Modifier,
    scrollConnection: NestedScrollConnection? = null,
    state: LazyListState,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    splicedCardGroup: Boolean,
    showKpmStatusBadge: Boolean,
    checkStrongBiometric: suspend () -> Boolean
) {
    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val embeddedUnloadInvalid = stringResource(id = R.string.kpm_embedded_unload_invalid)
    val uninstall = stringResource(id = R.string.kpm_unload)
    val cancel = stringResource(id = android.R.string.cancel)

    var expandedModuleId by remember { mutableStateOf<String?>(null) }

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()
    val outMsgStringRes = stringResource(id = R.string.kpm_control_outMsg)
    val okStringRes = stringResource(id = R.string.kpm_control_ok)
    val failedStringRes = stringResource(id = R.string.kpm_control_failed)

    // Run on the caller's (ViewModel) scope: KPMControlDialog leaves
    // composition on OK, cancelling any scope it owns.
    suspend fun onModuleControl(module: KPModel.KPMInfo, param: String) {
        lateinit var controlResult: Natives.KPMCtlRes
        loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                controlResult = Natives.kernelPatchModuleControl(module.name, param)
            }
        }

        if (controlResult.rc >= 0) {
            showToast(apApp, "$okStringRes\n${outMsgStringRes}: ${controlResult.outMsg}")
        } else {
            showToast(apApp, "$failedStringRes\n${outMsgStringRes}: ${controlResult.outMsg}")
        }
    }

    val showKPMControlDialog = remember { mutableStateOf(false) }
    if (showKPMControlDialog.value) {
        KPMControlDialog(showDialog = showKPMControlDialog, onConfirm = { param ->
            viewModel.viewModelScope.launch { onModuleControl(targetKPMToControl, param) }
        })
    }

    suspend fun onModuleUninstall(module: KPModel.KPMInfo) {
        if (!checkStrongBiometric()) return
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = if (module.loadSource == "embedded") {
                embeddedUnloadInvalid
            } else {
                moduleUninstallConfirm.format(module.name)
            },
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val result = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                val unloaded = module.loadSource.isBlank() || Natives.unloadKernelPatchModule(module.name) == 0L
                val removed = if (module.installed && module.loadSource != "embedded") {
                    val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                    val dir = "${APApplication.KPMS_DIR}$id"
                    rootShellForResult("rm -rf '$dir' && test ! -e '$dir'").isSuccess
                } else true
                UninstallResult(unloaded, removed)
            }
        }

        // Refresh even when the live kernel instance could not be unloaded:
        // the persistent file may still have been removed and must not remain
        // represented as installed in the UI.
        if (result.removed) {
            viewModel.fetchModuleList()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList(forceEmbeddedRefresh = true) },
        isRefreshing = viewModel.isRefreshing,
        state = pullToRefreshState,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
    ) {
        val configuration = LocalConfiguration.current
        val isWideScreen = configuration.screenWidthDp >= 600

        if (isWideScreen) {
            TwoColumnGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scrollConnection != null) {
                            Modifier.nestedScroll(scrollConnection)
                        } else {
                            Modifier
                        }
                    ),
                items = if (moduleList.isEmpty()) emptyList() else moduleList,
                key = { module -> module.name },
                verticalSpacing = FolkDimens.Size16,
                horizontalSpacing = FolkDimens.Size16,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = FolkDimens.Size16,
                            top = FolkDimens.Size16,
                            end = FolkDimens.Size16,
                            bottom = bottomClearance
                        )
                    }
                },
                beforeItems = {
                    if (moduleList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = FolkDimens.Size300),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.errorMessage != null && !viewModel.isRefreshing) {
                                FolkStateView(
                                    title = viewModel.errorMessage ?: stringResource(R.string.kpm_load_failed),
                                    icon = Icons.Outlined.Warning,
                                    tone = FolkStateTone.Critical,
                                    action = {
                                        Button(onClick = { viewModel.fetchModuleList() }) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    },
                                )
                            } else {
                                FolkStateView(
                                    title = stringResource(R.string.kpm_apm_empty),
                                    icon = Icons.Outlined.Extension,
                                )
                            }
                        }
                    }
                },
                itemContent = { module ->
                    val scope = rememberCoroutineScope()
                    KPModuleItem(
                        module,
                        onUninstall = {
                            scope.launch { onModuleUninstall(module) }
                        },
                        onControl = {
                            scope.launch {
                                if (checkStrongBiometric()) {
                                    targetKPMToControl = module
                                    showKPMControlDialog.value = true
                                }
                            }
                        },
                        onToggle = { enabled ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                    if (enabled) {
                                        rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                    } else {
                                        rootShellForResult("touch '${APApplication.KPMS_DIR}$id/disable'")
                                    }
                                }
                                viewModel.updateModuleDisabled(module.moduleId, !enabled)
                                viewModel.markNeedRefresh()
                                viewModel.fetchModuleList()
                            }
                        },
                        showMoreModuleInfo = showMoreModuleInfo,
                        simpleListBottomBar = simpleListBottomBar,
                        foldSystemModule = foldSystemModule,
                        expanded = expandedModuleId == module.name,
                        onExpandToggle = {
                            expandedModuleId = if (expandedModuleId == module.name) null else module.name
                        },
                        isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                    )
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (scrollConnection != null) {
                            Modifier.nestedScroll(scrollConnection)
                        } else {
                            Modifier
                        }
                    ),
                state = state,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = FolkDimens.Size0,
                            top = FolkDimens.Size16,
                            end = FolkDimens.Size0,
                            bottom = bottomClearance
                        )
                    }
                },
            ) {
                when {
                    viewModel.errorMessage != null && !viewModel.isRefreshing -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(FolkDimens.Size48)
                                    )
                                    Spacer(Modifier.height(FolkDimens.Size12))
                                    Text(
                                        text = viewModel.errorMessage ?: stringResource(R.string.kpm_load_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = FolkDimens.Size16)
                                    )
                                    Spacer(Modifier.height(FolkDimens.Size12))
                                    Button(onClick = { viewModel.fetchModuleList() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }

                    moduleList.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                FolkStateView(
                                    title = stringResource(R.string.kpm_apm_empty),
                                    icon = Icons.Outlined.Extension,
                                )
                            }
                        }
                    }

                    else -> {
                        if (splicedCardGroup) {
                            item { Spacer(Modifier.height(FolkDimens.Size8)) }
                            splicedLazyColumnGroup(
                                items = moduleList,
                                key = { _, module -> module.name },
                                contentType = { _, _ -> "KPModuleItem" },
                            ) { _, module ->
                                val scope = rememberCoroutineScope()
                                KPModuleItem(
                                    module,
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onControl = {
                                        scope.launch {
                                            if (checkStrongBiometric()) {
                                                targetKPMToControl = module
                                                showKPMControlDialog.value = true
                                            }
                                        }
                                    },
                                    onToggle = { enabled ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                                if (enabled) {
                                                    rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                                } else {
                                                    rootShellForResult("mkdir -p '${APApplication.KPMS_DIR}$id' && touch '${APApplication.KPMS_DIR}$id/disable'")
                                                }
                                            }
                                            viewModel.markNeedRefresh()
                                            viewModel.fetchModuleList()
                                        }
                                    },
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    simpleListBottomBar = simpleListBottomBar,
                                    foldSystemModule = foldSystemModule,
                                    expanded = expandedModuleId == module.name,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.name) null else module.name
                                    },
                                    isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                                )
                            }
                            item { Spacer(Modifier.height(FolkDimens.Size8)) } // bottom clearance handled by contentPadding
                        } else {
                            item { Spacer(Modifier.height(FolkDimens.Size8)) }
                            itemsIndexed(moduleList, key = { _, module -> module.name }) { _, module ->
                                Box(modifier = Modifier.padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size8)) {
                                val scope = rememberCoroutineScope()
                                KPModuleItem(
                                    module,
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onControl = {
                                        scope.launch {
                                            if (checkStrongBiometric()) {
                                                targetKPMToControl = module
                                                showKPMControlDialog.value = true
                                            }
                                        }
                                    },
                                    onToggle = { enabled ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                                if (enabled) {
                                                    rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                                } else {
                                                    rootShellForResult("mkdir -p '${APApplication.KPMS_DIR}$id' && touch '${APApplication.KPMS_DIR}$id/disable'")
                                                }
                                            }
                                            viewModel.markNeedRefresh()
                                            viewModel.fetchModuleList()
                                        }
                                    },
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    simpleListBottomBar = simpleListBottomBar,
                                    foldSystemModule = foldSystemModule,
                                    expanded = expandedModuleId == module.name,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.name) null else module.name
                                    },
                                    isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                                )
                                }
                            }
                            item { Spacer(Modifier.height(FolkDimens.Size8)) } // bottom clearance handled by contentPadding
                        }
                    }
                }
            }
        }
    }
}

