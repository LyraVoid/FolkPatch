package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.os.Build
import android.system.Os
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.copyableInfo
import me.bmax.apatch.ui.component.folk.FolkWrapSafeText
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus

private val managerVersion = getManagerVersion()

@Composable
fun InfoCard(kpState: APApplication.State, apState: APApplication.State) {
    // 隐藏设定状态
    val hideSuPath = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_su_path", false)) }
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    val hideZygisk = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_zygisk", false)) }
    val hideMount = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_mount", false)) }

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    val suPath = remember { Natives.suPath() }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FolkDimens.Size24, top = FolkDimens.Size24, end = FolkDimens.Size24, bottom = FolkDimens.Size16)
        ) {
            val contents = StringBuilder()
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String) {
                contents.appendLine(label).appendLine(content).appendLine()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .copyableInfo(label, content)
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    FolkWrapSafeText(text = content, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value) {
                InfoCardItem(
                    stringResource(R.string.home_kpatch_version), Version.installedKPVString()
                )

                Spacer(Modifier.height(FolkDimens.Size16))
            }
            
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath.value) {
                InfoCardItem(stringResource(R.string.home_su_path), suPath)

                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    stringResource(R.string.home_apatch_version), managerVersion.second.toString()
                )
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            InfoCardItem(stringResource(R.string.home_device_info), getDeviceInfo())

            Spacer(Modifier.height(FolkDimens.Size16))
            InfoCardItem(stringResource(R.string.home_kernel), uname.release)

            Spacer(Modifier.height(FolkDimens.Size16))
            InfoCardItem(stringResource(R.string.home_system_version), getSystemVersion())

            Spacer(Modifier.height(FolkDimens.Size16))
            if (!hideFingerprint.value) {
                InfoCardItem(stringResource(R.string.home_fingerprint), Build.FINGERPRINT)

                Spacer(Modifier.height(FolkDimens.Size16))
            }
            
            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk.value) {
                InfoCardItem(stringResource(R.string.home_zygisk_implement), zygiskImplement)

                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount.value) {
                InfoCardItem(stringResource(R.string.home_mount_implement), mountImplement)

                Spacer(Modifier.height(FolkDimens.Size16))
            }

            InfoCardItem(stringResource(R.string.home_selinux_status), getSELinuxStatus())

        }
    }
}

@Composable
fun ListInfoCard(kpState: APApplication.State, apState: APApplication.State, showIcons: Boolean = false) {
    val hideSuPath = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_su_path", false)) }
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    val hideZygisk = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_zygisk", false)) }
    val hideMount = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_mount", false)) }

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    val suPath = remember { Natives.suPath() }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FolkDimens.Size24, top = FolkDimens.Size24, end = FolkDimens.Size24, bottom = FolkDimens.Size16)
        ) {
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(icon: ImageVector, label: String, content: String) {
                if (showIcons) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .copyableInfo(label, content),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = FolkDimens.Size2)
                                .size(FolkDimens.Size20),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(FolkDimens.Size12))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                            FolkWrapSafeText(text = content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .copyableInfo(label, content)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        FolkWrapSafeText(text = content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value) {
                InfoCardItem(Icons.Outlined.Extension, stringResource(R.string.home_kpatch_version), Version.installedKPVString())
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath.value) {
                InfoCardItem(Icons.Outlined.Code, stringResource(R.string.home_su_path), suPath)
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(Icons.Outlined.Android, stringResource(R.string.home_apatch_version), managerVersion.second.toString())
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            InfoCardItem(Icons.Outlined.PhoneAndroid, stringResource(R.string.home_device_info), getDeviceInfo())
            Spacer(Modifier.height(FolkDimens.Size16))

            InfoCardItem(Icons.Outlined.DeveloperBoard, stringResource(R.string.home_kernel), uname.release)
            Spacer(Modifier.height(FolkDimens.Size16))

            InfoCardItem(Icons.Outlined.Info, stringResource(R.string.home_system_version), getSystemVersion())
            Spacer(Modifier.height(FolkDimens.Size16))

            if (!hideFingerprint.value) {
                InfoCardItem(Icons.Outlined.Fingerprint, stringResource(R.string.home_fingerprint), Build.FINGERPRINT)
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk.value) {
                InfoCardItem(Icons.Outlined.Layers, stringResource(R.string.home_zygisk_implement), zygiskImplement)
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount.value) {
                InfoCardItem(Icons.Outlined.SdStorage, stringResource(R.string.home_mount_implement), mountImplement)
                Spacer(Modifier.height(FolkDimens.Size16))
            }

            InfoCardItem(Icons.Outlined.Shield, stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}
