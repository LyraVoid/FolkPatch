package me.bmax.apatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.util.SuAuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuAuditLogScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var kernelEntries by remember { mutableStateOf(SuAuditLog.getKernelEntries()) }
    var appEntries by remember { mutableStateOf(SuAuditLog.getAppEntries()) }

    if (showClearDialog) {
        SuAuditClearDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                SuAuditLog.clearEntries()
                kernelEntries = SuAuditLog.getKernelEntries()
                appEntries = SuAuditLog.getAppEntries()
                showClearDialog = false
            }
        )
    }

    val tabs = listOf(
        stringResource(R.string.su_audit_tab_usage),
        stringResource(R.string.su_audit_tab_operations),
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.su_audit_log_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (kernelEntries.isNotEmpty() || appEntries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.su_audit_log_clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }

            when (selectedTab) {
                0 -> KernelAuditList(entries = kernelEntries)
                1 -> AppAuditList(entries = appEntries)
            }
        }
    }
}

@Composable
private fun KernelAuditList(entries: List<SuAuditLog.AuditEntry.KernelEntry>) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.su_audit_log_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { it.timestamp }) { entry ->
                val pm = LocalContext.current.packageManager
                val appLabel = remember(entry.uid) {
                    try {
                        val packages = pm.getPackagesForUid(entry.uid)
                        if (packages != null && packages.isNotEmpty()) {
                            pm.getApplicationInfo(packages[0], 0).loadLabel(pm).toString()
                        } else {
                            "UID ${entry.uid}"
                        }
                    } catch (_: Exception) {
                        "UID ${entry.uid}"
                    }
                }

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            buildAnnotatedString {
                                append(appLabel)
                                append("  ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(if (entry.toUid == 0) "root" else "uid ${entry.toUid}")
                                }
                            }
                        )
                    },
                    supportingContent = {
                        Text(
                            "${entry.comm}  PID ${entry.pid}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        val appInfo = remember(entry.uid) {
                            try {
                                val packages = pm.getPackagesForUid(entry.uid)
                                if (packages != null && packages.isNotEmpty()) {
                                    pm.getPackageInfo(packages[0], 0)
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (appInfo != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(appInfo)
                                    .crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = entry.uid.toString().take(2),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AppAuditList(entries: List<SuAuditLog.AuditEntry.AppEntry>) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.su_audit_log_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries, key = { "${it.packageName}_${it.timestamp}" }) { entry ->
                val pm = LocalContext.current.packageManager
                val appLabel = remember(entry.packageName) {
                    try {
                        pm.getApplicationInfo(entry.packageName, 0).loadLabel(pm).toString()
                    } catch (_: Exception) {
                        entry.packageName
                    }
                }

                val dateFormat = remember {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                }

                val (actionLabel, actionColor) = when (entry.action) {
                    "GRANT" -> stringResource(R.string.su_audit_action_grant) to MaterialTheme.colorScheme.primary
                    "REVOKE" -> stringResource(R.string.su_audit_action_revoke) to MaterialTheme.colorScheme.error
                    else -> stringResource(R.string.su_audit_action_exclude) to MaterialTheme.colorScheme.tertiary
                }

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(
                            buildAnnotatedString {
                                append(appLabel)
                                append("  ")
                                withStyle(SpanStyle(color = actionColor, fontWeight = FontWeight.Medium)) {
                                    append(actionLabel)
                                }
                            }
                        )
                    },
                    supportingContent = {
                        Text(
                            dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        val appInfo = remember(entry.packageName) {
                            try {
                                pm.getPackageInfo(entry.packageName, 0)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (appInfo != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(appInfo)
                                    .crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = entry.packageName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuAuditClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier.width(320.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = androidx.compose.material3.AlertDialogDefaults.TonalElevation,
            color = androidx.compose.material3.AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.su_audit_log_clear),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.su_audit_log_clear_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(text = android.R.string.cancel.let { stringResource(it) })
                    }
                    androidx.compose.material3.TextButton(onClick = onConfirm) {
                        Text(text = stringResource(R.string.su_audit_log_clear))
                    }
                }
            }
        }
    }
}
