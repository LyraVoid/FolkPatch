package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.theme.tokens.FolkDimens
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ShizukuLogScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.screen.superuser.LabelText
import me.bmax.apatch.util.ShizukuServiceManager
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import androidx.compose.material.icons.outlined.*
import me.bmax.apatch.ui.component.folk.FolkStateView
import androidx.compose.material.icons.outlined.Apps

private data class ShizukuApp(
    val packageInfo: PackageInfo,
    val uid: Int,
    val allowed: Boolean,
    val shellOnly: Boolean,
)

private data class ShizukuLoadResult(
    val serverIsRoot: Boolean,
    val apps: List<ShizukuApp>,
)

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun ShizukuManagementScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var available by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var serverIsRoot by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(emptyList<ShizukuApp>()) }

    suspend fun loadApps() {
        loading = true
        try {
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
                    val packageInfos = ShizukuServiceManager.getApplications() ?: return@withContext null
                    val rootServer = ShizukuServiceManager.isRootServer()
                    val loadedApps = packageInfos
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
                            runCatching {
                                app.packageInfo.applicationInfo
                                    ?.loadLabel(context.packageManager)
                                    ?.toString()
                                    ?.lowercase()
                                    .orEmpty()
                            }.getOrDefault("")
                        }
                    ShizukuLoadResult(rootServer, loadedApps)
                }
            }
            available = result != null
            loadFailed = false
            if (result != null) {
                serverIsRoot = result.serverIsRoot
                apps = result.apps
            } else {
                apps = emptyList()
            }
        } catch (t: Throwable) {
            Log.e("ShizukuMgr", "loadApps failed", t)
            available = false
            loadFailed = true
            apps = emptyList()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadApps() }

    val scope = rememberCoroutineScope()

    FolkScaffold(
        title = stringResource(R.string.shizuku_management_title),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = navigator::popBackStack,
        actions = {
            IconButton(
                onClick = { navigator.navigate(ShizukuLogScreenDestination) },
                modifier = androidx.compose.ui.Modifier.navOrigin(captureBitmap = true),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Article,
                    contentDescription = stringResource(R.string.shizuku_log_title),
                )
            }
        },
    ) { padding ->
        when {
            loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator(modifier = Modifier.padding(FolkDimens.Size32)) }
            loadFailed -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(FolkDimens.Size24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.shizuku_management_load_failed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(FolkDimens.Size16))
                Button(onClick = { scope.launch { loadApps() } }) {
                    Text(stringResource(R.string.retry))
                }
            }
            !available -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(FolkDimens.Size24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.shizuku_management_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(FolkDimens.Size16))
                Button(onClick = { scope.launch { loadApps() } }) {
                    Text(stringResource(R.string.retry))
                }
            }
            apps.isEmpty() -> FolkStateView(
                title = stringResource(R.string.shizuku_management_empty),
                modifier = Modifier.padding(padding),
                icon = Icons.Outlined.Apps,
                action = {
                    Button(onClick = { scope.launch { loadApps() } }) {
                        Text(stringResource(R.string.retry))
                    }
                },
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = FolkDimens.Size8, bottom = FolkDimens.Size24),
            ) {
                items(apps, key = { it.uid }) { app ->
                    val info = app.packageInfo.applicationInfo ?: return@items
                    val label = remember(app.packageInfo.packageName) {
                        runCatching { info.loadLabel(context.packageManager).toString() }
                            .getOrDefault(app.packageInfo.packageName)
                    }
                    // One group per app: keep a gap between the cards (the
                    // settings sub-pages get this from SegmentedSection).
                    SegmentedColumn(
                        flat = true,
                        modifier = Modifier.padding(bottom = FolkDimens.Size10),
                    ) {
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
                                modifier = Modifier.padding(horizontal = FolkDimens.Size16),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                        item(key = "allow") {
                            SegmentedSwitchItem(
                                icon = Icons.Outlined.Shield,
                                title = stringResource(R.string.shizuku_management_allowed_title),
                                summary = if (app.allowed) {
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
                                SegmentedSwitchItem(
                                    icon = Icons.Outlined.Lock,
                                    title = stringResource(R.string.shizuku_management_root_access),
                                    summary = stringResource(R.string.shizuku_management_root_access_desc),
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
            .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(packageInfo)
                .crossfade(true)
                .build(),
            contentDescription = label,
            modifier = Modifier
                .size(FolkDimens.Size48)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = FolkDimens.Size16),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            FlowRow(modifier = Modifier.padding(top = FolkDimens.Size4)) {
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
