package me.bmax.apatch.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.PkgConfig
import me.bmax.apatch.util.SuAuditLog
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AppProfileScreen(
    navigator: DestinationsNavigator,
    packageName: String,
    uid: Int
) {
    val viewModel = viewModel<SuperUserViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val appInfoState = remember(packageName, uid) {
        derivedStateOf {
            SuperUserViewModel.apps.find { it.packageName == packageName && it.uid == uid }
        }
    }
    val appInfo = appInfoState.value
    if (appInfo == null) {
        navigator.popBackStack()
        return
    }

    val config = appInfo.config
    
    // 0: ROOT, 1: NO ROOT, 2: Exclude
    var selectedIndex by remember(config) { 
        mutableIntStateOf(
            when {
                config.allow == 1 -> 0
                config.exclude == 1 -> 2
                else -> 1
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.su_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val success = viewModel.launchApp(context, appInfo.packageName)
                        scope.launch {
                            showToast(
                                context,
                                if (success) {
                                    context.getString(R.string.su_app_action_launch_success, appInfo.label)
                                } else {
                                    context.getString(R.string.su_app_action_failed, appInfo.label)
                                }
                            )
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = stringResource(R.string.su_app_action_launch))
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .padding(vertical = 12.dp),
            ) {
                fun selectMode(index: Int) {
                    if (index == selectedIndex) return

                    selectedIndex = index

                    // 先同步更新内存配置保证 UI 即时响应
                    when (index) {
                        0 -> { // ROOT
                            config.allow = 1
                            config.exclude = 0
                            config.profile.scontext = APApplication.MAGISK_SCONTEXT
                        }
                        1 -> { // NO ROOT
                            config.allow = 0
                            config.exclude = 0
                        }
                        2 -> { // Exclude
                            config.allow = 0
                            config.exclude = 1
                            config.profile.scontext = APApplication.DEFAULT_SCONTEXT
                        }
                    }
                    config.profile.uid = appInfo.uid

                    // 内核调用与配置文件读写移出主线程，避免切换权限时卡顿
                    scope.launch(Dispatchers.IO) {
                        when (index) {
                            0 -> {
                                Natives.grantSu(appInfo.uid, 0, config.profile.scontext)
                                Natives.setUidExclude(appInfo.uid, 0)
                                SuAuditLog.logGrant(appInfo.packageName, appInfo.uid)
                            }
                            1 -> {
                                Natives.revokeSu(appInfo.uid)
                                Natives.setUidExclude(appInfo.uid, 0)
                                SuAuditLog.logRevoke(appInfo.packageName, appInfo.uid)
                            }
                            2 -> {
                                Natives.revokeSu(appInfo.uid)
                                Natives.setUidExclude(appInfo.uid, 1)
                                SuAuditLog.logExclude(appInfo.packageName, appInfo.uid)
                            }
                        }
                        PkgConfig.changeConfig(config)
                        // 本地触发列表重新排序，返回 SuperUser 页无需全量重拉
                        SuperUserViewModel.apps = ArrayList(SuperUserViewModel.apps)
                    }
                }

                SplicedColumnGroup {
                    item(key = "app") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(appInfo.packageInfo)
                                    .crossfade(true).build(),
                                contentDescription = appInfo.label,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appInfo.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = appInfo.packageName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "UID ${appInfo.uid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }

                    item(key = "root") {
                        AuthorizationOption(
                            title = stringResource(R.string.su_pkg_root_setting_title),
                            summary = stringResource(R.string.su_pkg_root_setting_summary),
                            icon = Icons.Filled.Security,
                            selected = selectedIndex == 0,
                            onClick = { selectMode(0) },
                        )
                    }

                    item(key = "normal") {
                        AuthorizationOption(
                            title = stringResource(R.string.su_pkg_normal_setting_title),
                            summary = stringResource(R.string.su_pkg_normal_setting_summary),
                            icon = Icons.Filled.Info,
                            selected = selectedIndex == 1,
                            onClick = { selectMode(1) },
                        )
                    }

                    item(key = "excluded") {
                        AuthorizationOption(
                            title = stringResource(R.string.su_pkg_excluded_setting_title),
                            summary = stringResource(R.string.su_pkg_excluded_setting_summary),
                            icon = Icons.Filled.RemoveCircle,
                            selected = selectedIndex == 2,
                            onClick = { selectMode(2) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorizationOption(
    title: String,
    summary: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        label = "authorizationOptionColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .size(20.dp),
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(12.dp))
        RadioButton(selected = selected, onClick = null)
    }
}
