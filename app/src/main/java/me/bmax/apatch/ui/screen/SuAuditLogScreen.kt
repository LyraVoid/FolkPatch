package me.bmax.apatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.SuAuditLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SuAuditLogScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(SuAuditLog.getEntries()) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        SuAuditClearDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                SuAuditLog.clearEntries()
                entries = emptyList()
                showClearDialog = false
            }
        )
    }

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
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = stringResource(R.string.su_audit_log_clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.su_audit_log_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(entries, key = { it.timestamp }) { entry ->
                    val pm = context.packageManager
                    val appLabel = remember(entry.packageName) {
                        try {
                            pm.getApplicationInfo(entry.packageName, 0)
                                .loadLabel(pm).toString()
                        } catch (_: Exception) {
                            entry.packageName
                        }
                    }

                    val actionLabel = when (entry.action) {
                        "GRANT" -> stringResource(R.string.su_audit_action_grant)
                        "REVOKE" -> stringResource(R.string.su_audit_action_revoke)
                        else -> stringResource(R.string.su_audit_action_exclude)
                    }

                    val actionColor = when (entry.action) {
                        "GRANT" -> Color(0xFF4CAF50)
                        "REVOKE" -> Color(0xFFF44336)
                        else -> Color(0xFFFF9800)
                    }

                    val dateFormat = remember {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    }

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(appLabel) },
                        supportingContent = {
                            Column {
                                Text(
                                    entry.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    dateFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
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
                                    model = ImageRequest.Builder(context)
                                        .data(appInfo)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = appLabel,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(48.dp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            RoundedCornerShape(8.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = entry.packageName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            AuditActionBadge(label = actionLabel, color = actionColor)
                        },
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AuditActionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = Color.White,
        )
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
                androidx.compose.foundation.layout.Row(
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
