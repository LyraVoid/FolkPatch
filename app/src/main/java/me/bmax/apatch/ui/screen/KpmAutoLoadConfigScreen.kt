package me.bmax.apatch.ui.screen

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KpmAutoLoadConfig
import me.bmax.apatch.ui.component.KpmAutoLoadEntry
import me.bmax.apatch.ui.component.KpmAutoLoadManager
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.showToast

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpmAutoLoadConfigScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(KpmAutoLoadManager.isEnabled.value) }
    var jsonString by remember { mutableStateOf(KpmAutoLoadManager.getConfigJson()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isValidJson by remember { mutableStateOf(true) }
    var isVisualMode by remember { mutableStateOf(true) }
    var kpmEntriesList by remember { mutableStateOf(KpmAutoLoadManager.kpmPaths.value.toList()) }
    var showFirstTimeDialog by remember { mutableStateOf(KpmAutoLoadManager.isFirstTime(context)) }
    var dontShowAgain by remember { mutableStateOf(false) }

    var editingEntry by remember { mutableStateOf<KpmAutoLoadEntry?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    // 获取URI的真实路径
    fun getPathFromUri(context: Context, uri: android.net.Uri): String {
        // 简化处理，在实际应用中应该更完善地处理不同类型的URI
        return when (uri.scheme) {
            "content" -> {
                // 对于content URI，尝试从文件名获取一个合理的路径
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "unknown.kpm"
                "/storage/emulated/0/Download/$fileName"
            }
            "file" -> uri.path ?: ""
            else -> uri.toString()
        }
    }
    
    // 根据路径列表更新JSON字符串
    fun updateJsonString(entries: List<KpmAutoLoadEntry>, enabled: Boolean, onUpdate: (String) -> Unit) {
        val config = KpmAutoLoadConfig(enabled, entries)
        onUpdate(KpmAutoLoadManager.getConfigJson(config))
    }
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 导入KPM文件到内部存储
            val importedPath = KpmAutoLoadManager.importKpm(context, it)
            
            if (importedPath != null && importedPath.endsWith(".kpm", ignoreCase = true) && importedPath !in kpmEntriesList.map { it.path }) {
                kpmEntriesList = kpmEntriesList + KpmAutoLoadEntry(path = importedPath)
                updateJsonString(kpmEntriesList, isEnabled) { newJson ->
                    jsonString = newJson
                }
                showToast(context, context.getString(R.string.kpm_autoload_save_success))
            } else if (importedPath == null) {
                showToast(context, context.getString(R.string.kpm_autoload_file_not_found))
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val config = KpmAutoLoadManager.loadConfig(context)
        isEnabled = config.enabled
        jsonString = KpmAutoLoadManager.getConfigJson()
        kpmEntriesList = config.kpmPaths
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kpm_autoload_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 功能启用开关
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kpm_autoload_enabled),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.kpm_autoload_enabled_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            updateJsonString(kpmEntriesList, it) { newJson ->
                                jsonString = newJson
                            }
                        }
                    )
                }
            }

            // 可视化模式或JSON模式
            if (isVisualMode) {
                // 可视化模式
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.kpm_autoload_kpm_list_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(
                                onClick = {
                                    filePickerLauncher.launch("application/octet-stream")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(stringResource(R.string.kpm_autoload_add_kpm))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (kpmEntriesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.kpm_autoload_no_kpm_added),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(kpmEntriesList, key = { it.path }) { entry ->
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = entry.path.substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        supportingContent = {
                                            Column {
                                                Text(
                                                    text = entry.path,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (entry.event != "service" || entry.args.isNotEmpty()) {
                                                    Text(
                                                        text = buildString {
                                                            if (entry.event != "service") append("${stringResource(R.string.kpm_autoload_event_label).trimEnd(':')} ${entry.event}")
                                                            if (entry.args.isNotEmpty()) {
                                                                if (entry.event != "service") append(" | ")
                                                                append("${stringResource(R.string.kpm_autoload_args_label).trimEnd(':')} ${entry.args}")
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        editingEntry = entry
                                                        showEditDialog = true
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = stringResource(R.string.kpm_autoload_edit_kpm),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        kpmEntriesList = kpmEntriesList.filter { it.path != entry.path }
                                                        updateJsonString(kpmEntriesList, isEnabled) { newJson ->
                                                            jsonString = newJson
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = stringResource(R.string.kpm_autoload_remove_kpm),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // JSON配置编辑框
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.kpm_autoload_json_config),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = jsonString,
                            onValueChange = { 
                                jsonString = it
                                isValidJson = KpmAutoLoadManager.parseConfigFromJson(it) != null
                                if (isValidJson) {
                                    KpmAutoLoadManager.parseConfigFromJson(it)?.let { config ->
                                        kpmEntriesList = config.kpmPaths
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            label = { Text(stringResource(R.string.kpm_autoload_json_label)) },
                            placeholder = { Text(stringResource(R.string.kpm_autoload_json_placeholder)) },
                            isError = !isValidJson,
                            supportingText = {
                                if (!isValidJson) {
                                    Text(stringResource(R.string.kpm_autoload_json_error))
                                } else {
                                    Text(stringResource(R.string.kpm_autoload_json_helper))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 可视化模式/JSON模式切换按钮
                Button(
                    onClick = {
                        isVisualMode = !isVisualMode
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isVisualMode) stringResource(R.string.kpm_autoload_json_mode) else stringResource(R.string.kpm_autoload_visual_mode))
                }
                
                // 保存按钮
                Button(
                    onClick = {
                        showSaveDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    enabled = if (isVisualMode) kpmEntriesList.isNotEmpty() else isValidJson
                ) {
                    Text(stringResource(R.string.kpm_autoload_save))
                }
            }
        }
    }

    // 保存确认对话框
    if (showSaveDialog) {
        BasicAlertDialog(
            onDismissRequest = { showSaveDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kpm_autoload_save_confirm),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val config = if (isVisualMode) {
                                KpmAutoLoadConfig(enabled = isEnabled, kpmPaths = kpmEntriesList)
                            } else {
                                KpmAutoLoadConfig(enabled = isEnabled, kpmPaths =
                                    KpmAutoLoadManager.parseConfigFromJson(jsonString)?.kpmPaths ?: emptyList()
                                )
                            }
                            
                            val success = KpmAutoLoadManager.saveConfig(context, config)
                            if (success) {
                                showToast(context, context.getString(R.string.kpm_autoload_save_success))
                                navigator.navigateUp()
                            } else {
                                showToast(context, context.getString(R.string.kpm_autoload_save_failed))
                            }
                            showSaveDialog = false
                        }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }
    
    // 编辑 KPM 对话框
    if (showEditDialog && editingEntry != null) {
        KpmEditDialog(
            entry = editingEntry!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedEntry ->
                kpmEntriesList = kpmEntriesList.map { if (it.path == updatedEntry.path) updatedEntry else it }
                updateJsonString(kpmEntriesList, isEnabled) { jsonString = it }
                showEditDialog = false
            }
        )
    }

    // 首次使用提示对话框
    if (showFirstTimeDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    KpmAutoLoadManager.setFirstTimeShown(context)
                }
                showFirstTimeDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(350.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.kpm_autoload_first_time_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.kpm_autoload_first_time_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.kpm_autoload_do_not_show_again),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            if (dontShowAgain) {
                                KpmAutoLoadManager.setFirstTimeShown(context)
                            }
                            showFirstTimeDialog = false
                        }) {
                            Text(stringResource(R.string.kpm_autoload_first_time_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KpmEditDialog(
    entry: KpmAutoLoadEntry,
    onDismiss: () -> Unit,
    onConfirm: (KpmAutoLoadEntry) -> Unit
) {
    var selectedEvent by remember { mutableStateOf(entry.event) }
    var argsValue by remember { mutableStateOf(entry.args) }
    var showEventDropdown by remember { mutableStateOf(false) }

    val eventOptions = listOf("service", "post-fs-data")
    val eventLabels = mapOf(
        "service" to stringResource(R.string.kpm_autoload_event_service),
        "post-fs-data" to stringResource(R.string.kpm_autoload_event_post_fs_data)
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(320.dp).padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${stringResource(R.string.kpm_autoload_edit_dialog_title)}: ${entry.path.substringAfterLast("/")}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.kpm_autoload_event_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = eventLabels[selectedEvent] ?: selectedEvent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEventDropdown = true },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = showEventDropdown,
                        onDismissRequest = { showEventDropdown = false }
                    ) {
                        eventOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(eventLabels[option] ?: option) },
                                onClick = {
                                    selectedEvent = option
                                    showEventDropdown = false
                                }
                            )
    }
}
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.kpm_autoload_args_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = argsValue,
                    onValueChange = { argsValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onConfirm(entry.copy(event = selectedEvent, args = argsValue))
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}