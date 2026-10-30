package me.bmax.apatch.ui.screen.module

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedCheckboxItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.viewmodel.APModuleViewModel

@Composable
fun ModuleSortSheet(
    show: Boolean,
    groups: Set<ModuleSortGroup>,
    enabledFirst: Boolean,
    customOrder: List<String>,
    modules: List<APModuleViewModel.ModuleInfo>,
    onDismiss: () -> Unit,
    onGroupChange: (ModuleSortGroup, Boolean) -> Unit,
    onEnabledFirstChange: (Boolean) -> Unit,
    onCustomOrderChange: (List<String>) -> Unit,
    onResetCustomOrder: () -> Unit,
) {
    if (!show) return

    var orderSnapshot by remember { mutableStateOf<List<APModuleViewModel.ModuleInfo>>(emptyList()) }
    var showOrderDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FolkDimens.Size24)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
        ) {
            Text(
                text = stringResource(R.string.module_sort),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(
                    start = FolkDimens.Size24,
                    end = FolkDimens.Size24,
                    top = FolkDimens.Size8,
                ),
            )
            SegmentedColumn(title = stringResource(R.string.module_sort_priority)) {
                ModuleSortPriorityGroups.forEach { group ->
                    item(key = group.value) {
                        SegmentedCheckboxItem(
                            title = stringResource(group.titleRes()),
                            summary = stringResource(group.summaryRes()),
                            checked = group in groups,
                            onCheckedChange = { onGroupChange(group, it) },
                        )
                    }
                }
            }
            SegmentedColumn {
                item {
                    SegmentedSwitchItem(
                        title = stringResource(R.string.module_sort_enabled_first),
                        checked = enabledFirst,
                        onCheckedChange = onEnabledFirstChange,
                    )
                }
            }
            SegmentedColumn {
                item {
                    SegmentedNavigationItem(
                        title = stringResource(R.string.module_sort_custom_order),
                        summary = stringResource(R.string.module_sort_custom_order_summary),
                        onClick = {
                            orderSnapshot = modules
                            showOrderDialog = true
                        },
                    )
                }
                if (customOrder.isNotEmpty()) {
                    item {
                        SegmentedNavigationItem(
                            title = stringResource(R.string.module_sort_reset_custom_order),
                            summary = stringResource(R.string.module_sort_reset_custom_order_summary),
                            onClick = onResetCustomOrder,
                        )
                    }
                }
            }
        }
    }

    if (showOrderDialog) {
        ModuleCustomOrderDialog(
            modules = orderSnapshot,
            onDismiss = { showOrderDialog = false },
            onOrderChange = onCustomOrderChange,
            onReset = {
                onResetCustomOrder()
                showOrderDialog = false
            },
        )
    }
}

@Composable
private fun ModuleCustomOrderDialog(
    modules: List<APModuleViewModel.ModuleInfo>,
    onDismiss: () -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onReset: () -> Unit,
) {
    var ordered by remember(modules) { mutableStateOf(modules) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var draggedDistance by remember { mutableStateOf(0f) }
    val reorderThreshold = with(LocalDensity.current) { FolkDimens.Size40.toPx() }
    val dragToReorder = stringResource(R.string.module_sort_drag_to_reorder)

    ExpressiveDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.padding(FolkDimens.Size16),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
        ) {
            Text(
                text = stringResource(R.string.module_sort_custom_order),
                style = MaterialTheme.typography.titleLarge,
            )
            LazyColumn(modifier = Modifier.heightIn(max = FolkDimens.Size480)) {
                itemsIndexed(ordered, key = { _, module -> module.id }) { _, module ->
                    val isDragging = draggedId == module.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDragging) {
                                    Modifier
                                        .zIndex(1f)
                                        .graphicsLayer { translationY = draggedDistance }
                                } else {
                                    Modifier.animateItem()
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = module.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = dragToReorder,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(
                                    start = FolkDimens.Size12,
                                    end = FolkDimens.Size4,
                                    top = FolkDimens.Size12,
                                    bottom = FolkDimens.Size12,
                                )
                                .semantics { contentDescription = dragToReorder }
                                .pointerInput(module.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedId = module.id
                                            draggedDistance = 0f
                                        },
                                        onDragEnd = {
                                            draggedId = null
                                            draggedDistance = 0f
                                        },
                                        onDragCancel = {
                                            draggedId = null
                                            draggedDistance = 0f
                                        },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        draggedDistance += dragAmount.y
                                        val currentIndex = ordered.indexOfFirst { it.id == module.id }
                                        val targetIndex = when {
                                            draggedDistance > reorderThreshold -> currentIndex + 1
                                            draggedDistance < -reorderThreshold -> currentIndex - 1
                                            else -> currentIndex
                                        }
                                        if (currentIndex >= 0 &&
                                            targetIndex in ordered.indices &&
                                            targetIndex != currentIndex
                                        ) {
                                            ordered = ordered.toMutableList().apply {
                                                add(targetIndex, removeAt(currentIndex))
                                            }
                                            onOrderChange(ordered.map { it.id })
                                            draggedDistance -= if (targetIndex > currentIndex) {
                                                reorderThreshold
                                            } else {
                                                -reorderThreshold
                                            }
                                        }
                                    }
                                },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.module_sort_reset_custom_order))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

private fun ModuleSortGroup.titleRes(): Int = when (this) {
    ModuleSortGroup.MetaModule -> R.string.module_sort_group_metamodule
    ModuleSortGroup.Zygisk -> R.string.module_sort_group_zygisk
    ModuleSortGroup.LSPosed -> R.string.module_sort_group_lsposed
    ModuleSortGroup.WebUi -> R.string.module_sort_group_webui
    ModuleSortGroup.ActionScript -> R.string.module_sort_group_action
}

private fun ModuleSortGroup.summaryRes(): Int = when (this) {
    ModuleSortGroup.MetaModule -> R.string.module_sort_group_metamodule_summary
    ModuleSortGroup.Zygisk -> R.string.module_sort_group_zygisk_summary
    ModuleSortGroup.LSPosed -> R.string.module_sort_group_lsposed_summary
    ModuleSortGroup.WebUi -> R.string.module_sort_group_webui_summary
    ModuleSortGroup.ActionScript -> R.string.module_sort_group_action_summary
}
