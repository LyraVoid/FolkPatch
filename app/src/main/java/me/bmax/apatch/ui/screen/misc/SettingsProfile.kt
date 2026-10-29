package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.folkGroupColor
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.Role
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.component.folk.folkOverlayColor
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape

/** Square size the avatar is decoded at, in pixels. */
private const val PROFILE_AVATAR_PX = 256

/**
 * Local profile editor: choose between the default avatar and a custom image,
 * and set a nickname and a short signature. "Restore defaults" resets only the
 * text, so the avatar can be restored on its own. No account, no network.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
    nickname: String,
    signature: String,
    avatarUri: String,
    avatarOpacity: Float,
    onPickAvatar: () -> Unit,
    onUseDefaultAvatar: () -> Unit,
    onAvatarOpacityChange: (Float) -> Unit,
    onRestoreDefault: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(nickname) { mutableStateOf(nickname) }
    var sign by remember(signature) { mutableStateOf(signature) }
    val context = LocalContext.current
    val avatarBitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            runCatching {
                me.bmax.apatch.util.BottomBarIconConfig
                    .loadIconBitmap(context, avatarUri, PROFILE_AVATAR_PX)
            }.getOrNull()?.asImageBitmap()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ContinuousCornerShape(topStart = FolkDimens.Size28, topEnd = FolkDimens.Size28),
        containerColor = folkOverlayColor(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FolkDimens.Size20)
                .padding(bottom = FolkDimens.Size28),
        ) {
            Text(
                text = stringResource(R.string.profile_edit_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(FolkDimens.Size16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                AvatarOptionTile(
                    label = stringResource(R.string.profile_avatar_default),
                    selected = avatarUri.isBlank(),
                    onClick = onUseDefaultAvatar,
                    opacity = avatarOpacity,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FolkDimens.Size58),
                    )
                }
                AvatarOptionTile(
                    label = stringResource(R.string.profile_avatar_custom),
                    selected = avatarUri.isNotBlank(),
                    onClick = onPickAvatar,
                    opacity = avatarOpacity,
                    modifier = Modifier.weight(1f),
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(FolkDimens.Size28),
                        )
                    }
                }
            }

            Spacer(Modifier.height(FolkDimens.Size16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.profile_avatar_opacity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(avatarOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Slider(
                value = avatarOpacity,
                onValueChange = onAvatarOpacityChange,
                valueRange = 0.1f..1f,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(FolkDimens.Size8))

            ProfileTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                label = stringResource(R.string.profile_nickname_label),
                singleLine = true,
            )

            Spacer(Modifier.height(FolkDimens.Size12))

            ProfileTextField(
                value = sign,
                onValueChange = { if (it.length <= 60) sign = it },
                label = stringResource(R.string.profile_signature_label),
                singleLine = false,
                minLines = 2,
            )

            Spacer(Modifier.height(FolkDimens.Size16))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onRestoreDefault) {
                    Text(stringResource(R.string.profile_restore_default))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(FolkDimens.Size8))
                Button(
                    onClick = { onSave(name.trim(), sign.trim()) },
                    shape = FolkShape.Corner16,
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

/**
 * Rounded, filled text field without the Material underline, so the editor
 * reads as part of FolkPatch rather than a stock Material form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        shape = FolkShape.Corner16,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedIndicatorColor = FolkColors.Transparent,
            unfocusedIndicatorColor = FolkColors.Transparent,
            disabledIndicatorColor = FolkColors.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
/**
 * Personal space header.
 *
 * The top of the page leads with a person-like block (avatar + nickname +
 * optional signature) and only then shows the device facts, so the technical
 * text no longer occupies the most expressive spot on the page.
 *
 * The nickname and signature read from local preferences; with no signature
 * set the second line simply does not exist. The avatar falls back to the app
 * mark until the user picks an image.
 */
@Composable
fun ProfileHeader(
    nickname: String,
    signature: String,
    deviceName: String,
    avatarUri: String,
    avatarOpacity: Float,
    onAvatarClick: () -> Unit,
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarUri) {
        if (avatarUri.isBlank()) {
            null
        } else {
            runCatching {
                me.bmax.apatch.util.BottomBarIconConfig
                    .loadIconBitmap(context, avatarUri, PROFILE_AVATAR_PX)
            }.getOrNull()?.asImageBitmap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FolkDimens.Size16)
            .padding(top = FolkDimens.Size16),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(FolkDimens.Size68)
                    // Alpha before clip/background so the whole avatar - circle
                    // fill included - fades together and the wallpaper shows through.
                    .alpha(avatarOpacity)
                    // Plain circle, no shadow and no coloured ring.
                    .clip(CircleShape)
                    .background(folkGroupColor().copy(alpha = 1f))
                    .clickable(role = Role.Button, onClick = onAvatarClick),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // The launcher vector carries a lot of transparent margin, so
                    // it is drawn oversized and clipped by the circle: the visible
                    // mark ends up ~29dp inside the 68dp avatar.
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FolkDimens.Size86),
                    )
                }
            }

            Spacer(Modifier.width(FolkDimens.Size16))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(FolkDimens.Size5))
                // A signature overwrites the device model on the same line; the
                // device stands in for the email other apps put here.
                Text(
                    text = signature.ifBlank { deviceName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
