package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import com.ramcosta.composedestinations.generated.destinations.OnlineModuleScreenDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.core.ui.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    snackBarHost: SnackbarHostState,
    searchQuery: String,
    checkStrongBiometric: suspend () -> Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleModuleBanner: () -> Unit
) {
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val disableAllTitle = stringResource(R.string.apm_disable_all_title)
    val disableAllConfirm = stringResource(R.string.apm_disable_all_confirm)
    val confirm = stringResource(android.R.string.ok)
    val cancel = stringResource(android.R.string.cancel)
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderedModules by remember { mutableStateOf(viewModel.moduleList) }

    SearchAppBar(
        title = { Text(stringResource(R.string.apm)) },
        searchText = searchQuery,
        onSearchTextChange = onSearchQueryChange,
        onClearClick = { onSearchQueryChange("") },
        dropdownContent = {
            androidx.compose.material3.IconButton(onClick = {
                navigator.navigate(OnlineModuleScreenDestination)
            }) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = stringResource(R.string.online_module_title)
                )
            }
            androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(CoreR.string.core_action_more))
                WallpaperAwareDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (viewModel.moduleList.isNotEmpty()) {
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.apm_custom_order)) },
                            onClick = {
                                showMenu = false
                                orderedModules = viewModel.moduleList
                                showOrderDialog = true
                            }
                        )
                    }
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_disable_all_title)) },
                        onClick = {
                            showMenu = false
                            scope.launch {
                                if (!checkStrongBiometric()) return@launch
                                val result = confirmDialog.awaitConfirm(
                                    title = disableAllTitle,
                                    content = disableAllConfirm,
                                    confirm = confirm,
                                    dismiss = cancel
                                )
                                if (result == ConfirmResult.Confirmed) {
                                    viewModel.disableAllModules()
                                }
                            }
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_copy_list_title)) },
                        onClick = {
                            showMenu = false
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val moduleNames = viewModel.moduleList.joinToString("\n") { it.name }
                            val clip = ClipData.newPlainText("Module List", moduleNames)
                            clipboardManager.setPrimaryClip(clip)
                            scope.launch {
                                snackBarHost.showSnackbar(
                                    message = context.getString(R.string.apm_copy_list_success),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    )

    if (showOrderDialog) {
        val reorderThreshold = with(LocalDensity.current) { FolkDimens.Size40.toPx() }
        val dragToReorderDescription = stringResource(R.string.apm_drag_to_reorder)
        var draggedModuleId by remember { mutableStateOf<String?>(null) }
        var draggedDistance by remember { mutableStateOf(0f) }
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text(stringResource(R.string.apm_custom_order)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = FolkDimens.Size480)) {
                    itemsIndexed(orderedModules, key = { _, module -> module.id }) { _, module ->
                        val isDragging = draggedModuleId == module.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .zIndex(1f)
                                            .graphicsLayer { translationY = draggedDistance }
                                    } else {
                                        Modifier.animateItem(
                                            placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                                        )
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = module.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = FolkDimens.Size12, end = FolkDimens.Size4)
                                    .semantics { contentDescription = dragToReorderDescription }
                                    .pointerInput(module.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedModuleId = module.id
                                                draggedDistance = 0f
                                            },
                                            onDragEnd = {
                                                draggedModuleId = null
                                                draggedDistance = 0f
                                            },
                                            onDragCancel = {
                                                draggedModuleId = null
                                                draggedDistance = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            draggedDistance += dragAmount.y
                                            val currentIndex = orderedModules.indexOfFirst { it.id == module.id }
                                            val targetIndex = when {
                                                draggedDistance > reorderThreshold -> currentIndex + 1
                                                draggedDistance < -reorderThreshold -> currentIndex - 1
                                                else -> currentIndex
                                            }
                                            if (currentIndex >= 0 && targetIndex in orderedModules.indices && targetIndex != currentIndex) {
                                                orderedModules = orderedModules.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                                viewModel.setCustomModuleOrder(orderedModules.map { it.id })
                                                draggedDistance -= if (targetIndex > currentIndex) {
                                                    reorderThreshold
                                                } else {
                                                    -reorderThreshold
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = FolkDimens.Size12),
                                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size4)
                            ) {
                                repeat(2) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = FolkDimens.Size24, height = FolkDimens.Size3)
                                            .clip(RoundedCornerShape(FolkDimens.Size2))
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetCustomModuleOrder()
                    orderedModules = viewModel.moduleList
                }) {
                    Text(stringResource(R.string.apm_reset_order))
                }
            }
        )
    }
}
