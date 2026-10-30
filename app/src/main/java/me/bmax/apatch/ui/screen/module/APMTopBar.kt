package me.bmax.apatch.ui.screen.module

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import com.ramcosta.composedestinations.generated.destinations.OnlineModuleScreenDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.MoreVert
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
    onToggleModuleBanner: () -> Unit,
    onSortClick: () -> Unit,
) {
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val disableAllTitle = stringResource(R.string.apm_disable_all_title)
    val disableAllConfirm = stringResource(R.string.apm_disable_all_confirm)
    val confirm = stringResource(android.R.string.ok)
    val cancel = stringResource(android.R.string.cancel)
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }

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
            androidx.compose.material3.IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = stringResource(R.string.module_sort),
                )
            }
            androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(CoreR.string.core_action_more))
                DropdownMenuPopup(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                        SelectableDropdownMenuItem(
                            text = { Text(stringResource(R.string.apm_disable_all_title)) },
                            selected = false,
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
                            },
                            shapes = MenuDefaults.itemShape(index = 0, count = 2),
                        )
                        SelectableDropdownMenuItem(
                            text = { Text(stringResource(R.string.apm_copy_list_title)) },
                            selected = false,
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
                            },
                            shapes = MenuDefaults.itemShape(index = 1, count = 2),
                        )
                    }
                }
            }
        }
    )
}
