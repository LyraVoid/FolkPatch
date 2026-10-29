package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.component.folk.folkOverlayColor
import android.content.Intent
import me.bmax.apatch.util.ui.showToast
import androidx.core.content.FileProvider
import me.bmax.apatch.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.theme.BackupConfig
import me.bmax.apatch.util.BackupLogManager
import me.bmax.apatch.util.WebDavUtils
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.*

@Composable
fun BackupSettingsContent(
    autoBackupModule: Boolean,
    onAutoBackupModuleChange: (Boolean) -> Unit,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current

    val showWebDavDialog = remember { mutableStateOf(false) }

    SegmentedSection(title = stringResource(R.string.settings_section_backup_local)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "backup_local") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Save,
                    title = stringResource(id = R.string.settings_enable_local_backup),
                    summary = stringResource(id = R.string.settings_enable_local_backup_summary),
                    checked = autoBackupModule,
                    onCheckedChange = {
                        onAutoBackupModuleChange(it)
                        prefs.edit().putBoolean("auto_backup_module", it).apply()
                    },
                )
            }

            item(key = "backup_boot") {
                var autoBackupBoot by remember { mutableStateOf(prefs.getBoolean("auto_backup_boot", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.RestartAlt,
                    title = stringResource(id = R.string.settings_auto_backup_boot),
                    summary = stringResource(id = R.string.settings_auto_backup_boot_summary),
                    checked = autoBackupBoot,
                    onCheckedChange = {
                        autoBackupBoot = it
                        prefs.edit().putBoolean("auto_backup_boot", it).apply()
                    },
                )
            }

            item(key = "backup_open_dir", visible = autoBackupModule) {
                SegmentedNavigationItem(
                    icon = Icons.Outlined.FolderOpen,
                    title = stringResource(id = R.string.settings_open_backup_dir),
                    onClick = {
                        val backupDir = java.io.File(me.bmax.apatch.util.getSafeDownloadsDir(context), "FolkPatch/ModuleBackups")
                        if (!backupDir.exists()) backupDir.mkdirs()

                        try {
                            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", backupDir)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(uri, "resource/folder")
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                try {
                                    context.startActivity(intent)
                                } catch (e2: Exception) {
                                    val intent2 = Intent(Intent.ACTION_VIEW)
                                    intent2.setDataAndType(uri, "*/*")
                                    intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(Intent.createChooser(intent2, context.getString(R.string.settings_open_backup_dir)))
                                }
                            } catch (e3: Exception) {
                                showToast(context, R.string.backup_dir_open_failed)
                            }
                        }
                    },
                )
            }
        }
    }

    SegmentedSection(title = stringResource(R.string.settings_section_backup_cloud)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "backup_cloud") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Cloud,
                    title = stringResource(id = R.string.settings_enable_cloud_backup),
                    summary = stringResource(id = R.string.settings_enable_cloud_backup_summary),
                    checked = BackupConfig.isBackupEnabled,
                    onCheckedChange = {
                        BackupConfig.isBackupEnabled = it
                        BackupConfig.save(context)
                    },
                )
            }

            item(key = "backup_webdav", visible = BackupConfig.isBackupEnabled) {
                SegmentedNavigationItem(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(id = R.string.settings_configure_webdav),
                    onClick = { showWebDavDialog.value = true },
                )
            }
        }
    }

    if (showWebDavDialog.value) {
        WebDavConfigDialog(showWebDavDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavConfigDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(BackupConfig.webdavUrl) }
    var username by remember { mutableStateOf(BackupConfig.webdavUsername) }
    var password by remember { mutableStateOf(BackupConfig.webdavPassword) }
    var path by remember { mutableStateOf(BackupConfig.webdavPath) }
    var isTesting by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(FolkDimens.Size400)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = folkOverlayColor()
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = stringResource(R.string.webdav_config_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.webdav_url)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = FolkDimens.Size8),
                    singleLine = true
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.webdav_username)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = FolkDimens.Size8),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.webdav_password)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = FolkDimens.Size8),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text(stringResource(R.string.webdav_path_label)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = FolkDimens.Size16),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    TextButton(onClick = { showLogDialog = true }) {
                        Text(stringResource(R.string.webdav_view_logs))
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                val result = WebDavUtils.testConnection(url, username, password)
                                isTesting = false
                                if (result.isSuccess) {
                                    showToast(context, context.getString(R.string.webdav_test_success))
                                } else {
                                    showToast(context, context.getString(R.string.webdav_test_failed, result.exceptionOrNull()?.message))
                                }
                            }
                        },
                        enabled = !isTesting,
                        colors = ButtonDefaults.textButtonColors()
                    ) {
                        Text(stringResource(R.string.test))
                    }

                    Button(onClick = {
                        BackupConfig.webdavUrl = url
                        BackupConfig.webdavUsername = username
                        BackupConfig.webdavPassword = password
                        BackupConfig.webdavPath = path
                        BackupConfig.save(context)
                        showDialog.value = false
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        BackupLogDialog(showDialog = remember { mutableStateOf(true) }, onDismiss = { showLogDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupLogDialog(showDialog: MutableState<Boolean>, onDismiss: () -> Unit) {
    var logs by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        logs = BackupLogManager.readLogs()
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(FolkDimens.Size350)
                .height(FolkDimens.Size500),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = folkOverlayColor()
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = stringResource(R.string.webdav_backup_logs_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(FolkDimens.Size8),
                    color = folkInsetColor()
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = logs.ifEmpty { stringResource(R.string.webdav_no_logs) },
                        modifier = Modifier
                            .padding(FolkDimens.Size8)
                            .verticalScroll(scrollState),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = FolkDimens.Size16),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        scope.launch {
                            BackupLogManager.clearLogs()
                            logs = ""
                        }
                    }) {
                        Text(stringResource(R.string.webdav_clear_logs))
                    }
                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}
