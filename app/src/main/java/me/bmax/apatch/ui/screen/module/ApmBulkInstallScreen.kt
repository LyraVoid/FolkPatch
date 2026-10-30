package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE

import android.net.Uri
import android.provider.OpenableColumns
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.util.installModule
import me.bmax.apatch.util.reboot

import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.BulkInstallManager
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import me.bmax.apatch.ui.component.folk.FolkStateView
import androidx.compose.material.icons.outlined.Extension
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.component.folk.folkInsetColor

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApmBulkInstallScreen(navigator: DestinationsNavigator, initialUris: ArrayList<Uri>? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var moduleUris by remember { mutableStateOf<List<Uri>>(initialUris ?: emptyList()) }
    var isInstalling by remember { mutableStateOf(false) }
    var installLog by remember { mutableStateOf("") }
    var showLogDialog by remember { mutableStateOf(false) }
    
    // Helper function to get filename from Uri
    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: uri.toString()
    }
    
    // First use dialog state
    val prefs = remember { APApplication.sharedPreferences }
    var showFirstTimeDialog by remember { 
        mutableStateOf(!prefs.getBoolean("apm_bulk_install_first_use_shown", false)) 
    }
    var dontShowAgain by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        moduleUris = moduleUris + uris
    }

    // First Use Dialog
    if (showFirstTimeDialog) {
        ExpressiveDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    prefs.edit().putBoolean("apm_bulk_install_first_use_shown", true).apply()
                }
                showFirstTimeDialog = false
            },
            width = FolkDimens.Size350,
            shape = FolkShape.Corner20,
            blurBehind = false,
            dialogProperties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size16)) {
                Text(
                    text = stringResource(R.string.apm_bulk_install_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                Text(
                    text = stringResource(R.string.apm_bulk_install_first_use_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.dont_show_again),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = FolkDimens.Size8)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = {
                        if (dontShowAgain) {
                            prefs.edit().putBoolean("apm_bulk_install_first_use_shown", true).apply()
                        }
                        showFirstTimeDialog = false
                    }) {
                        Text(stringResource(R.string.got_it))
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { if (!isInstalling) showLogDialog = false },
            title = { Text(stringResource(R.string.apm_bulk_install_log_title)) },
            text = {
                Column {
                    Text(
                        text = installLog,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .height(FolkDimens.Size300)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                if (!isInstalling) {
                    Row {
                        TextButton(onClick = { showLogDialog = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                        TextButton(onClick = { 
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    reboot()
                                }
                            }
                         }) {
                            Text(stringResource(R.string.reboot))
                        }
                    }
                }
            }
        )
    }

    FolkScaffold(
        title = stringResource(R.string.apm_bulk_install_title),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.navigateUp() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(FolkDimens.Size16)
        ) {
            TonalCard(
                modifier = Modifier.weight(1f),
                flat = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(FolkDimens.Size16)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.apm_bulk_install_list_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.padding(end = FolkDimens.Size8)
                            )
                            Text(stringResource(R.string.apm_bulk_install_add))
                        }
                    }
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    if (moduleUris.isEmpty()) {
                        FolkStateView(
                            title = stringResource(R.string.apm_bulk_install_empty),
                            icon = Icons.Outlined.Extension,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
                        ) {
                            items(moduleUris, key = { it }) { uri ->
                                ListItem(
                                    modifier = Modifier
                                        .clip(FolkShape.Corner16)
                                        .background(folkInsetColor()),
                                    headlineContent = { 
                                        Text(
                                            text = getFileName(context, uri),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                moduleUris = moduleUris - uri
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = stringResource(R.string.apm_bulk_install_remove),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(FolkDimens.Size16))
            
            Button(
                onClick = {
                    scope.launch {
                        val prefs = APApplication.sharedPreferences
                        if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
                            val activity = context as? androidx.fragment.app.FragmentActivity
                            if (activity != null) {
                                if (!BiometricUtils.authenticate(activity)) return@launch
                            }
                        }

                        val batchInstallFullProcess = prefs.getBoolean("apm_batch_install_full_process", false)

                        if (batchInstallFullProcess) {
                            val uris = ArrayList(moduleUris)
                            BulkInstallManager.setQueue(uris)
                            val first = BulkInstallManager.popNext()
                            if (first != null) {
                                navigator.navigate(InstallScreenDestination(first, MODULE_TYPE.APM))
                            }
                        } else {
                            showLogDialog = true
                            isInstalling = true
                            val logStart = context.getString(R.string.apm_bulk_install_log_start)
                            installLog = "$logStart\n"
                            launch(Dispatchers.IO) {
                                moduleUris.forEachIndexed { index, uri ->
                                    val fileName = getFileName(context, uri)
                                    val installingMsg = context.getString(R.string.apm_bulk_install_log_installing, fileName)
                                    withContext(Dispatchers.Main) {
                                        installLog += "\n>>> $installingMsg\n"
                                    }

                                    installModule(
                                        uri = uri,
                                        type = MODULE_TYPE.APM,
                                        onFinish = { success ->
                                            // handled via return value in blocking call
                                        },
                                        onStdout = { msg ->
                                            // append log? might be too much text update on Main thread if fast
                                            // for now let's skip detailed logs to avoid UI lag, or just append
                                        },
                                        onStderr = { msg ->
                                            // same
                                        }
                                    )

                                    val installedMsg = context.getString(R.string.apm_bulk_install_log_installed, fileName)
                                    withContext(Dispatchers.Main) {
                                        installLog += "$installedMsg\n"
                                    }
                                }
                                val doneMsg = context.getString(R.string.apm_bulk_install_log_done)
                                withContext(Dispatchers.Main) {
                                    installLog += "\n$doneMsg"
                                    isInstalling = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = moduleUris.isNotEmpty()
            ) {
                Text(stringResource(R.string.apm_bulk_install_action))
            }
        }
    }
}
