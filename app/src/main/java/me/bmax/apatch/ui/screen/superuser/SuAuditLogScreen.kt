package me.bmax.apatch.ui.screen.superuser

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkLogEmptyState
import me.bmax.apatch.ui.component.folk.folkLogTextStyle
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.component.SegmentedChoiceRow
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.util.SuAuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun SuAuditLogScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var kernelEntries by remember { mutableStateOf<List<SuAuditLog.AuditEntry.KernelEntry>>(emptyList()) }
    var appEntries by remember { mutableStateOf<List<SuAuditLog.AuditEntry.AppEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            kernelEntries = SuAuditLog.getKernelEntries()
            appEntries = SuAuditLog.getAppEntries()
        }
    }

    if (showClearDialog) {
        SuAuditClearDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                SuAuditLog.clearEntries()
                kernelEntries = emptyList()
                appEntries = emptyList()
                showClearDialog = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        kernelEntries = SuAuditLog.getKernelEntries()
                        appEntries = SuAuditLog.getAppEntries()
                    }
                }
            }
        )
    }

    val tabs = listOf(
        stringResource(R.string.su_audit_tab_usage),
        stringResource(R.string.su_audit_tab_operations),
    )

    FolkScaffold(
        title = stringResource(R.string.su_audit_log_title),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.popBackStack() },
        actions = {
            if (kernelEntries.isNotEmpty() || appEntries.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.su_audit_log_clear))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SegmentedChoiceRow(
                items = tabs,
                selectedIndex = selectedTab,
                onItemSelection = { selectedTab = it },
                modifier = Modifier.padding(vertical = FolkDimens.Size8),
            )

            Crossfade(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                label = "auditTab",
            ) { tab ->
                when (tab) {
                    0 -> KernelAuditList(entries = kernelEntries)
                    1 -> AppAuditList(entries = appEntries)
                }
            }
        }
    }
}

@Composable
private fun KernelAuditList(entries: List<SuAuditLog.AuditEntry.KernelEntry>) {
    if (entries.isEmpty()) {
        EmptyAuditView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = FolkDimens.Size8, bottom = FolkDimens.Size16),
        ) {
            splicedLazyColumnGroup(
                items = entries,
                key = { _, entry -> entry.timestamp },
                contentType = { _, _ -> "KernelEntry" },
            ) { _, entry ->
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon
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
                            modifier = Modifier.size(FolkDimens.Size40).clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(FolkDimens.Size40)
                                .clip(CircleShape)
                                .background(folkInsetColor()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.uid.toString().take(2),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(FolkDimens.Size16))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            buildAnnotatedString {
                                append(appLabel)
                                append("  ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(if (entry.toUid == 0) "root" else "uid ${entry.toUid}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "${entry.comm}  PID ${entry.pid}",
                            style = folkLogTextStyle(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppAuditList(entries: List<SuAuditLog.AuditEntry.AppEntry>) {
    if (entries.isEmpty()) {
        EmptyAuditView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = FolkDimens.Size8, bottom = FolkDimens.Size16),
        ) {
            splicedLazyColumnGroup(
                items = entries,
                key = { _, entry -> "${entry.packageName}_${entry.timestamp}" },
                contentType = { _, _ -> "AppEntry" },
            ) { _, entry ->
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Icon
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
                            modifier = Modifier.size(FolkDimens.Size40).clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(FolkDimens.Size40)
                                .clip(CircleShape)
                                .background(folkInsetColor()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.packageName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(FolkDimens.Size16))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            buildAnnotatedString {
                                append(appLabel)
                                append("  ")
                                withStyle(SpanStyle(color = actionColor, fontWeight = FontWeight.Medium)) {
                                    append(actionLabel)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            dateFormat.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAuditView() {
    FolkLogEmptyState(
        icon = Icons.AutoMirrored.Outlined.Article,
        title = stringResource(R.string.su_audit_log_empty),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuAuditClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        width = FolkDimens.Size320,
        shape = FolkShape.Corner20,
        blurBehind = false,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.su_audit_log_clear),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(FolkDimens.Size16))
            Text(
                text = stringResource(R.string.su_audit_log_clear_confirm),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(FolkDimens.Size24))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = android.R.string.cancel.let { stringResource(it) })
                }
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(R.string.su_audit_log_clear))
                }
            }
        }
    }
}
