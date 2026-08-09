package me.bmax.apatch.ui.screen.settings

import android.content.pm.PackageInfo
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.util.ShizukuServiceManager

private data class ShizukuApp(
    val packageInfo: PackageInfo,
    val uid: Int,
    val allowed: Boolean,
)

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuManagementScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var available by remember { mutableStateOf(true) }
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
                ShizukuServiceManager.getApplications()
                    .mapNotNull { packageInfo ->
                        val uid = packageInfo.applicationInfo?.uid ?: return@mapNotNull null
                        ShizukuApp(packageInfo, uid, ShizukuServiceManager.isAllowed(uid))
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
            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(apps, key = { it.uid }) { app ->
                    val info = app.packageInfo.applicationInfo ?: return@items
                    val label = remember(app.packageInfo.packageName) {
                        info.loadLabel(context.packageManager).toString()
                    }
                    val icon = remember(app.packageInfo.packageName) {
                        info.loadIcon(context.packageManager).toBitmap().asImageBitmap()
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = {
                            Text(
                                "${app.packageInfo.packageName} · ${context.getString(R.string.shizuku_management_uid, app.uid)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        leadingContent = {
                            Image(BitmapPainter(icon), contentDescription = null, modifier = Modifier.size(40.dp))
                        },
                        trailingContent = {
                            ExpressiveSwitch(
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
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}
