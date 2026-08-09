package me.bmax.apatch.ui

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import android.widget.Toast
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.APatchTheme
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Shizuku 授权确认界面。
 *
 * Shizuku Server（app_process 进程）在第三方应用请求权限时，
 * 通过广播拉起本界面，用户选择允许/仅一次/拒绝后，
 * 通过 Shizuku binder 将结果回传 Server。
 */
class ShizukuPermissionActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ShizukuPerm"
    }

    private var uid = -1
    private var pid = -1
    private var requestCode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ai = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("applicationInfo", ApplicationInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("applicationInfo")
        }
        uid = intent.getIntExtra("uid", -1)
        pid = intent.getIntExtra("pid", -1)
        requestCode = intent.getIntExtra("requestCode", -1)
        Log.i(TAG, "onCreate: action=${intent.action} uid=$uid pid=$pid requestCode=$requestCode pkg=${ai?.packageName}")
        if (uid == -1 || pid == -1 || ai == null) {
            Log.w(TAG, "invalid request intent, finishing")
            finish()
            return
        }

        val label = try {
            ai.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            ai.packageName
        }
        val icon = try {
            ai.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }

        setContent {
            APatchTheme(allowCustomBackground = false) {
                PermissionDialog(
                    label = label,
                    icon = icon,
                    onAllow = { reply(allowed = true, onetime = false) },
                    onAllowOnce = { reply(allowed = true, onetime = true) },
                    onDeny = { reply(allowed = false, onetime = true) },
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun reply(allowed: Boolean, onetime: Boolean) {
        val data = Bundle()
        data.putBoolean(ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
        data.putBoolean(ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        try {
            if (!waitForBinder()) {
                Log.e(TAG, "binder not available, cannot dispatch result")
                Toast.makeText(this, R.string.shizuku_permission_binder_timeout, Toast.LENGTH_LONG).show()
                finish()
                return
            }
            Shizuku.dispatchPermissionConfirmationResult(uid, pid, requestCode, data)
        } catch (t: Throwable) {
            Log.e(TAG, "dispatchPermissionConfirmationResult failed", t)
        }
        finish()
    }

    /** Server 可能在拉起弹窗后才完成 binder 投递，这里等待它就绪（最多 5 秒）。 */
    private fun waitForBinder(): Boolean {
        if (Shizuku.pingBinder()) return true
        val latch = CountDownLatch(1)
        val listener = object : Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() {
                Shizuku.removeBinderReceivedListener(this)
                latch.countDown()
            }
        }
        Shizuku.addBinderReceivedListenerSticky(listener)
        if (Shizuku.pingBinder()) {
            latch.countDown()
        }
        return try {
            latch.await(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PermissionDialog(
    label: String,
    icon: Drawable?,
    onAllow: () -> Unit,
    onAllowOnce: () -> Unit,
    onDeny: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Image(
                            painter = BitmapPainter(icon.toBitmap().asImageBitmap()),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shizuku_permission_title, label),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.shizuku_permission_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                PermissionOption(
                    icon = Icons.Filled.Check,
                    title = stringResource(R.string.shizuku_permission_allow),
                    subtitle = stringResource(R.string.shizuku_permission_allow_summary),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onAllow,
                )
                PermissionOption(
                    icon = Icons.Outlined.Timelapse,
                    title = stringResource(R.string.shizuku_permission_allow_once),
                    subtitle = stringResource(R.string.shizuku_permission_allow_once_summary),
                    container = MaterialTheme.colorScheme.surfaceContainer,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onAllowOnce,
                )
                PermissionOption(
                    icon = Icons.Filled.Block,
                    title = stringResource(R.string.shizuku_permission_deny),
                    subtitle = stringResource(R.string.shizuku_permission_deny_summary),
                    container = MaterialTheme.colorScheme.surfaceContainer,
                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onDeny,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PermissionOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f),
                )
            }
        }
    }
}
