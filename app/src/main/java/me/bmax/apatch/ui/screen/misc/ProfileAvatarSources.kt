package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedPreferenceItem
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/** Where a custom profile avatar comes from. */
internal enum class AvatarSource(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Local(R.string.profile_avatar_source_local, Icons.Outlined.Image),
    Qq(R.string.profile_avatar_source_qq, Icons.Outlined.Public),
    Gravatar(R.string.profile_avatar_source_gravatar, Icons.Outlined.AlternateEmail),
}

/** Asks which source the custom avatar should be pulled from. */
@Composable
internal fun AvatarSourceDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AvatarSource) -> Unit,
) {
    if (!showDialog) return
    ExpressiveDialog(onDismissRequest = onDismiss, width = FolkDimens.Size340) {
        Column(modifier = Modifier.padding(vertical = FolkDimens.Size8)) {
            Text(
                text = stringResource(R.string.profile_avatar_source_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = FolkDimens.Size24, vertical = FolkDimens.Size12),
            )
            AvatarSource.entries.forEach { source ->
                SegmentedPreferenceItem(
                    title = stringResource(source.titleRes),
                    icon = source.icon,
                    onClick = { onSelect(source) },
                )
            }
        }
    }
}

/** Collects the QQ number or email that identifies a remote avatar. */
@Composable
internal fun AvatarIdDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    ExpressiveDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(FolkDimens.Size16))
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(FolkDimens.Size20))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.cancel)) }
                Spacer(Modifier.width(FolkDimens.Size8))
                Button(
                    onClick = { onConfirm(value.trim()) },
                    enabled = value.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(),
                ) { Text(text = stringResource(android.R.string.ok)) }
            }
        }
    }
}

/** QQ exposes a head image by number. */
internal fun qqAvatarUrl(qq: String): String =
    "https://q.qlogo.cn/headimg_dl?dst_uin=${Uri.encode(qq)}&spec=640&img_type=jpg"

/**
 * Gravatar keys off the md5 of the lower-cased address. `d=404` makes an
 * unknown address fail instead of returning a placeholder, so the caller can
 * report the failure.
 */
internal fun gravatarUrl(email: String): String {
    val digest = MessageDigest.getInstance("MD5")
        .digest(email.trim().lowercase().toByteArray(Charsets.UTF_8))
    val hash = digest.joinToString("") { "%02x".format(it) }
    return "https://www.gravatar.com/avatar/$hash?s=640&d=404"
}

/**
 * Downloads a remote avatar into the same internal file the local flow uses,
 * so everything downstream keeps reading a local URI and works offline.
 *
 * @return the stored URI string, or null when the download failed
 */
internal suspend fun downloadProfileAvatar(context: Context, url: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.filesDir, PROFILE_AVATAR_FILE)
            URL(url).openStream().use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            if (!target.exists() || target.length() == 0L) return@runCatching null
            Uri.fromFile(target).buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
                .toString()
        }.getOrNull()
    }
