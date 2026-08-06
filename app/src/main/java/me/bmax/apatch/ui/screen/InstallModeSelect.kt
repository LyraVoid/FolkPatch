package me.bmax.apatch.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.getFileNameFromUri
import me.bmax.apatch.util.isABDevice
import me.bmax.apatch.util.isJailbreakPatchBlocked
import me.bmax.apatch.util.rootAvailable

var selectedBootImage: Uri? = null
var selectedKPImg: Uri? = null

@Destination<RootGraph>
@Composable
fun InstallModeSelectScreen(navigator: DestinationsNavigator) {
    var installMethod by remember {
        mutableStateOf<InstallMethod?>(null)
    }

    Scaffold(topBar = {
        TopBar(
            onBack = dropUnlessResumed { navigator.popBackStack() },
        )
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            SelectInstallMethod(
                onSelected = { method ->
                    installMethod = method
                },
                navigator = navigator
            )

        }
    }
}

sealed class InstallMethod {
    data class SelectFile(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.mode_select_page_select_file,
    ) : InstallMethod()

    data class SelectKPImg(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.mode_select_page_select_kpimg,
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.mode_select_page_patch_and_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.mode_select_page_install_inactive_slot
    }

    // Placeholder for Restore functionality
    data class Restore(
        val uri: Uri? = null,
        @param:StringRes override val label: Int = R.string.restore_select_file, // Reuse string or create new one
    ) : InstallMethod()

    abstract val label: Int
    open val summary: String? = null
}

@Composable
private fun SelectInstallMethod(
    onSelected: (InstallMethod) -> Unit = {},
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val rootAvailable = rootAvailable()
    val isAbDevice = isABDevice()
    var jailbreakBlocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        jailbreakBlocked = withContext(Dispatchers.IO) { isJailbreakPatchBlocked() }
    }

    // KP Install Options
    val kpOptions = mutableListOf<InstallMethod>(InstallMethod.SelectFile(), InstallMethod.SelectKPImg())
    if (rootAvailable) {
        kpOptions.add(InstallMethod.DirectInstall)
        if (isAbDevice) {
            kpOptions.add(InstallMethod.DirectInstallToInactiveSlot)
        }
    }

    // Restore Options
    val restoreOptions = mutableListOf<InstallMethod>(InstallMethod.Restore())


    var selectedOption by remember { mutableStateOf<InstallMethod?>(null) }
    var showInitBootWarning by remember { mutableStateOf(false) }
    var pendingBootUri by remember { mutableStateOf<Uri?>(null) }
    
    // Launcher for KP Patching (SelectFile)
    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val fileName = getFileNameFromUri(context, uri) ?: ""
                if (fileName.contains("init_boot", ignoreCase = true) || fileName.contains("vendor_boot", ignoreCase = true)) {
                    pendingBootUri = uri
                    showInitBootWarning = true
                } else {
                    val option = InstallMethod.SelectFile(uri)
                    selectedOption = option
                    onSelected(option)
                    selectedBootImage = option.uri
                }
            }
        }
    }

    // Launcher for custom KPimg selection
    val selectKPImgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.SelectKPImg(uri)
                selectedOption = option
                onSelected(option)
                selectedKPImg = option.uri
            }
        }
    }

    // Launcher for Restore (SelectFile for now)
    val selectRestoreImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                val option = InstallMethod.Restore(uri)
                selectedOption = option
                onSelected(option)
                selectedBootImage = option.uri
            }
        }
    }

    val confirmDialog = rememberConfirmDialog(onConfirm = {
        selectedOption = InstallMethod.DirectInstallToInactiveSlot
        onSelected(InstallMethod.DirectInstallToInactiveSlot)
    }, onDismiss = null)
    val dialogTitle = stringResource(id = android.R.string.dialog_alert_title)
    val dialogContent = stringResource(id = R.string.mode_select_page_install_inactive_slot_warning)

    // init_boot warning dialog
    if (showInitBootWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showInitBootWarning = false },
            title = { Text(stringResource(R.string.mode_select_page_init_boot_warning_title)) },
            text = { Text(stringResource(R.string.mode_select_page_init_boot_warning_message)) },
            confirmButton = {
                Button(onClick = {
                    showInitBootWarning = false
                    pendingBootUri?.let { uri ->
                        val option = InstallMethod.SelectFile(uri)
                        selectedOption = option
                        onSelected(option)
                        selectedBootImage = option.uri
                    }
                    pendingBootUri = null
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showInitBootWarning = false
                    pendingBootUri = null
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    val onClick = { option: InstallMethod ->
        when (option) {
            is InstallMethod.SelectFile -> {
                selectedBootImage = null
                selectImageLauncher.launch(
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/octet-stream"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                )
            }

            is InstallMethod.SelectKPImg -> {
                selectedKPImg = null
                selectKPImgLauncher.launch(
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/octet-stream"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                )
            }

            is InstallMethod.DirectInstall -> {
                selectedOption = option
                onSelected(option)
            }

            is InstallMethod.DirectInstallToInactiveSlot -> {
                confirmDialog.showConfirm(dialogTitle, dialogContent)
            }
            is InstallMethod.Restore -> {
                selectedBootImage = null
                selectRestoreImageLauncher.launch(
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/octet-stream"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                )
            }
        }
    }

    val onNext = {
        selectedOption?.let { option ->
            when (option) {
                is InstallMethod.SelectFile -> {
                    if (selectedBootImage != null) {
                         navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                    }
                }
                is InstallMethod.SelectKPImg -> {
                    if (selectedKPImg != null) {
                         navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                    }
                }
                is InstallMethod.DirectInstall -> {
                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL))
                }
                 is InstallMethod.DirectInstallToInactiveSlot -> {
                     navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.INSTALL_TO_NEXT_SLOT))
                }
                is InstallMethod.Restore -> {
                    if (selectedBootImage != null) {
                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.RESTORE))
                    }
                }
            }
        }
    }

    var kpExpanded by remember { mutableStateOf(true) }
    var restoreExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (jailbreakBlocked) {
            InstallStatusItem(
                text = stringResource(R.string.jailbreak_no_patch),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (!rootAvailable) {
            InstallStatusItem(
                text = stringResource(R.string.home_install_unknown_summary),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SplicedColumnGroup {
                // KernelPatch Patching/Installing
                if (!jailbreakBlocked) item(key = "kp_install") {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.kp_install_methods),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = if (kpExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable { kpExpanded = !kpExpanded }
                    )

                    AnimatedVisibility(
                        visible = kpExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 12.dp
                            )
                        ) {
                            kpOptions.forEach { option ->
                                InstallMethodOption(
                                    option = option,
                                    selectedOption = selectedOption,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                }

                // Select a boot to restore to boot partition
                // TODO(TEMPORARY): re-enable the restore entry when jailbreak mode is exited.
                if (!jailbreakBlocked) item(key = "restore") {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Icon(
                                Icons.Filled.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = {
                            Text(
                                stringResource(R.string.restore_boot_methods),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = if (restoreExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable { restoreExpanded = !restoreExpanded }
                    )

                    AnimatedVisibility(
                        visible = restoreExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = 12.dp
                            )
                        ) {
                            restoreOptions.forEach { option ->
                                InstallMethodOption(
                                    option = option,
                                    selectedOption = selectedOption,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            enabled = selectedOption != null,
            onClick = { onNext() },
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Text(
                stringResource(id = R.string.home_patch_next_step),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InstallStatusItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        ListItem(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .padding(vertical = 6.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
        )
    }
}

@Composable
fun InstallMethodOption(
    option: InstallMethod,
    selectedOption: InstallMethod?,
    onClick: (InstallMethod) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isSelected = option.javaClass == selectedOption?.javaClass
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    onClick = { onClick(option) },
                    role = Role.RadioButton,
                    indication = LocalIndication.current,
                    interactionSource = interactionSource
                )
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                interactionSource = interactionSource,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(id = option.label),
                    style = MaterialTheme.typography.bodyLarge
                )
                option.summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}) {
    TopAppBar(
        title = { Text(stringResource(R.string.mode_select_page_title)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
        },
    )
}
