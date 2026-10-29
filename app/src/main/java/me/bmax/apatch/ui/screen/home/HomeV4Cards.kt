package me.bmax.apatch.ui.screen.home

import android.os.Build
import android.system.Os
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.copyableInfo
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.util.SystemInfoCollector
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.Version.getManagerVersion

private val managerVersion = getManagerVersion()

@Composable
fun InstallProgressDialog(
    showDialog: MutableState<Boolean>,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    if (!showDialog.value) return

    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("准备安装...") }

    LaunchedEffect(Unit) {
        // 模拟安装进度
        for (i in 1..100) {
            delay(50)
            progress = i / 100f
            statusText = when {
                i < 20 -> "正在准备..."
                i < 40 -> "正在备份..."
                i < 60 -> "正在写入..."
                i < 80 -> "正在验证..."
                else -> "正在完成..."
            }
        }
        showDialog.value = false
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.kpm_install)) },
        text = {
            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { }
    )
}

/**
 * 系统信息卡片
 */
@Composable
internal fun SystemInfoCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    zygiskImplement: String,
    mountImplement: String,
    modifier: Modifier = Modifier
) {
    val uname = Os.uname()
    val prefs = APApplication.sharedPreferences
    
    var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
    var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
    var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
    var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
    var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }

    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_kpatch_info_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 信息列表
            InfoItem(Icons.Outlined.PhoneAndroid, stringResource(R.string.home_device_info), getDeviceInfo())
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion) {
                InfoItem(Icons.Outlined.Extension, stringResource(R.string.home_kpatch_version), Version.installedKPVString())
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath) {
                InfoItem(Icons.Outlined.Code, stringResource(R.string.home_su_path), Natives.suPath())
            }
            if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                InfoItem(Icons.Outlined.Android, stringResource(R.string.home_apatch_version), managerVersion.second.toString())
            }
            InfoItem(Icons.Outlined.DeveloperBoard, stringResource(R.string.home_kernel), uname.release)
            InfoItem(Icons.Outlined.Info, stringResource(R.string.home_system_version), getSystemVersion())
            if (!hideFingerprint) {
                InfoItem(Icons.Outlined.Fingerprint, stringResource(R.string.home_fingerprint), Build.FINGERPRINT)
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk) {
                InfoItem(Icons.Outlined.Layers, stringResource(R.string.home_zygisk_implement), zygiskImplement)
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount) {
                InfoItem(Icons.Outlined.SdStorage, stringResource(R.string.home_mount_implement), mountImplement)
            }
            InfoItem(Icons.Outlined.Shield, stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}

/**
 * 信息项
 */
@Composable
fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .copyableInfo(label, value)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 存储信息卡片
 */
@Composable
fun StorageInfoCard(modifier: Modifier = Modifier) {
    var storageStatus by remember { mutableStateOf(SystemInfoCollector.StorageStatus()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            storageStatus = SystemInfoCollector.collectStorageStatus()
            delay(5000)
        }
    }

    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SdStorage,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_storage_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 存储进度
            StorageProgressBar(
                label = stringResource(R.string.home_storage_internal),
                used = storageStatus.storageUsed,
                total = storageStatus.storageTotal,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            StorageProgressBar(
                label = stringResource(R.string.home_storage_ram),
                used = storageStatus.ramUsed,
                total = storageStatus.ramTotal,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * 存储进度条
 */
@Composable
fun StorageProgressBar(
    label: String,
    used: Long,
    total: Long,
    color: Color
) {
    val context = LocalContext.current
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val usedStr = android.text.format.Formatter.formatFileSize(context, used)
    val totalStr = android.text.format.Formatter.formatFileSize(context, total)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$usedStr / $totalStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

/**
 * 了解更多卡片 V4
 */
@Composable
internal fun LearnMoreCardV4() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri("https://fp.mysqil.com/") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
