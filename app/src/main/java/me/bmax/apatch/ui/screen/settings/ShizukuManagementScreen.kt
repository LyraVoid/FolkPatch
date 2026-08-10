package me.bmax.apatch.ui.screen.settings

import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.screen.LabelText
import me.bmax.apatch.util.ShizukuServiceManager

private data class ShizukuApp(
    val packageInfo: PackageInfo,
    val uid: Int,
    val allowed: Boolean,
    val shellOnly: Boolean,
)

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuManagementScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var available by remember { mutableStateOf(true) }
    var serverIsRoot by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<ShizukuApp>()) }

    suspend fun loadApps() {
        loading = true
        val result = withContext(Dispatchers.IO) {
            // 服务可能刚从设置页启动、binder 尚未完全就绪，短暂等待后再判定。
            var ready = ShizukuServiceManager.isServerRunning()
            var waited = 0
            while (!ready && waited < 3000) {
                Thread.sleep(200L)
                waited += 200
                ready = ShizukuServiceManager.isServerRunning()
            }
            if (!ready) {
                null
            } else {
                serverIsRoot = ShizukuServiceManager.isRootServer()
                ShizukuServiceManager.getApplications()
                    .mapNotNull { packageInfo ->
                        val uid = packageInfo.applicationInfo?.uid ?: return@mapNotNull null
                        ShizukuApp(
                            packageInfo = packageInfo,
                            uid = uid,
                            allowed = ShizukuServiceManager.isAllowed(uid),
                            shellOnly = ShizukuServiceManager.getShellOnly(uid),
                        )
                    }
                    .distinctBy { it.uid }
                    .sortedBy { app ->
                        app.packageInfo.applicationInfo?.loadLabel(context.packageManager).toString().lowercase()
                    }
            }
        }
        available = result != null
        apps = result.orEmpty()
        loading = false
    }

    LaunchedEffect(Unit) { loadApps() }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shizuku_management_title)) },
                navigationIcon = {
                    IconButton(onClick = navigator::popBackStack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        when {
            loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator(modifier = Modifier.padding(32.dp)) }
            !available -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.shizuku_management_unavailable))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { scope.launch { loadApps() } }) {
                    Text(stringResource(R.string.retry))
                }
            }
            apps.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.shizuku_management_empty))
                Spacer(Modifier.height(16.dp))
                Button(onClick = { scope.launch { loadApps() } }) {
                    Text(stringResource(R.string.retry))
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(apps, key = { it.uid }) { app ->
                    val info = app.packageInfo.applicationInfo ?: return@items
                    val label = remember(app.packageInfo.packageName) {
                        info.loadLabel(context.packageManager).toString()
                    }
                    SplicedColumnGroup(flat = true) {
                        item(key = "header") {
                            ShizukuAppHeader(
                                packageInfo = app.packageInfo,
                                label = label,
                                uid = app.uid,
                                allowed = app.allowed,
                                shellOnly = app.shellOnly,
                                serverIsRoot = serverIsRoot,
                            )
                        }
                        item(key = "divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                        item(key = "allow") {
                            ToggleSettingCard(
                                flat = true,
                                icon = Icons.Outlined.Shield,
                                title = stringResource(R.string.shizuku_management_allowed_title),
                                description = if (app.allowed) {
                                    stringResource(R.string.shizuku_management_granted)
                                } else {
                                    stringResource(R.string.shizuku_management_denied)
                                },
                                checked = app.allowed,
                                onCheckedChange = { allowed ->
                                    try {
                                        ShizukuServiceManager.setAllowed(app.uid, allowed)
                                        apps = apps.map { if (it.uid == app.uid) it.copy(allowed = allowed) else it }
                                    } catch (t: Throwable) {
                                        Log.w("ShizukuMgr", "setAllowed failed", t)
                                        Toast.makeText(context, R.string.shizuku_management_update_failed, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                        if (serverIsRoot) {
                            item(key = "root") {
                                ToggleSettingCard(
                                    flat = true,
                                    icon = Icons.Filled.Lock,
                                    title = stringResource(R.string.shizuku_management_root_access),
                                    description = stringResource(R.string.shizuku_management_root_access_desc),
                                    checked = !app.shellOnly,
                                    onCheckedChange = { root ->
                                        try {
                                            ShizukuServiceManager.setShellOnly(app.uid, !root)
                                            apps = apps.map { if (it.uid == app.uid) it.copy(shellOnly = !root) else it }
                                        } catch (t: Throwable) {
                                            Log.w("ShizukuMgr", "setShellOnly failed", t)
                                            Toast.makeText(context, R.string.shizuku_management_update_failed, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShizukuAppHeader(
    packageInfo: PackageInfo,
    label: String,
    uid: Int,
    allowed: Boolean,
    shellOnly: Boolean,
    serverIsRoot: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(packageInfo)
                .crossfade(true)
                .build(),
            contentDescription = label,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${packageInfo.packageName} · UID $uid",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                LabelText(
                    label = if (allowed) {
                        stringResource(R.string.shizuku_management_badge_allowed)
                    } else {
                        stringResource(R.string.shizuku_management_badge_denied)
                    },
                    containerColor = if (allowed) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                if (serverIsRoot) {
                    LabelText(
                        label = if (shellOnly) {
                            stringResource(R.string.shizuku_management_badge_shell)
                        } else {
                            stringResource(R.string.shizuku_management_badge_root)
                        },
                        containerColor = if (!shellOnly) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                }
            }
        }
    }
}
