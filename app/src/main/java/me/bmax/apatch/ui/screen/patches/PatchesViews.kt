@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package me.bmax.apatch.ui.screen.patches

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.PaddingValues
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.foundation.layout.size
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun StartButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        // Material 3 Expressive's medium size: the primary action reads as the
        // biggest touch target on the page.
        contentPadding = ButtonDefaults.MediumContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraConfigDialog(kpmInfo: KPModel.KPMInfo, onDismiss: () -> Unit) {
    var event by remember { mutableStateOf(kpmInfo.event) }
    var args by remember { mutableStateOf(kpmInfo.args) }

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        blurBehind = false,
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
            Text(
                text = stringResource(id = R.string.kpm_control_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = event,
                onValueChange = {
                    event = it
                    kpmInfo.event = it
                },
                label = { Text(stringResource(id = R.string.patch_item_extra_event)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = args,
                onValueChange = {
                    args = it
                    kpmInfo.args = it
                },
                label = { Text(stringResource(id = R.string.patch_item_extra_args)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

@Composable
fun ExtraItem(extra: KPModel.IExtraInfo, existed: Boolean, onDelete: () -> Unit) {
    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog && extra is KPModel.KPMInfo) {
        ExtraConfigDialog(extra, onDismiss = { showConfigDialog = false })
    }

    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    text = stringResource(
                        id =
                            if (existed) R.string.patch_item_existed_extra_kpm else R.string.patch_item_new_extra_kpm
                    ) +
                            " " + extra.type.toString().uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )

                if (extra.type == KPModel.ExtraType.KPM) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(CoreR.string.core_action_config),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showConfigDialog = true }
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(CoreR.string.core_action_delete),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { onDelete() })
            }
            if (extra.type == KPModel.ExtraType.KPM) {
                val kpmInfo: KPModel.KPMInfo = extra as KPModel.KPMInfo
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_name) + " "} ${kpmInfo.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_version) + " "} ${kpmInfo.version}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_kpm_license) + " "} ${kpmInfo.license}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_author) + " "} ${kpmInfo.author}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_kpm_desciption) + " "} ${kpmInfo.description}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun KernelPatchImageView(kpImgInfo: KPModel.KPImgInfo) {
    if (kpImgInfo.version.isEmpty()) return
    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_kpimg),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_version) + " " + Version.uInt2String(
                    kpImgInfo.version.substring(2).toUInt(16)
                ), style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_comile_time) + " " + kpImgInfo.compileTime,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_config) + " " + kpImgInfo.config,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PatchSuperKeySection(
    viewModel: PatchesViewModel,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    TonalCard(flat = true) {
        Column {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Key,
                title = stringResource(R.string.patch_custom_superkey),
                summary = stringResource(R.string.patch_custom_superkey_optional),
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            AnimatedVisibility(
                visible = checked,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )
                    SetSuperKeyView(viewModel)
                }
            }
        }
    }
}

@Composable
fun SetSuperKeyView(viewModel: PatchesViewModel) {
    var superKey by remember { mutableStateOf(viewModel.superkey) }
    var superKeyConfirm by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val invalid = superKey.isNotEmpty() && !viewModel.checkSuperKeyValidation(superKey)
    val mismatch = superKeyConfirm.isNotEmpty() && superKey != superKeyConfirm

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.patch_item_set_skey_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = superKey,
            onValueChange = { value ->
                superKey = value
                viewModel.superkey =
                    if (
                        viewModel.checkSuperKeyValidation(value) &&
                        (superKeyConfirm.isEmpty() || superKeyConfirm == value)
                    ) value else ""
            },
            label = { Text(stringResource(R.string.patch_set_superkey)) },
            singleLine = true,
            isError = invalid,
            shape = FolkShape.Corner16,
            visualTransformation = if (keyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            colors = superKeyFieldColors(),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                    )
                }
            },
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = superKeyConfirm,
            onValueChange = { value ->
                superKeyConfirm = value
                viewModel.superkey =
                    if (
                        viewModel.checkSuperKeyValidation(superKey) &&
                        superKey == value
                    ) superKey else ""
            },
            label = { Text(stringResource(R.string.patch_confirm_superkey)) },
            singleLine = true,
            isError = mismatch,
            supportingText = if (mismatch) {
                { Text(stringResource(R.string.patch_skey_mismatch)) }
            } else null,
            shape = FolkShape.Corner16,
            visualTransformation = if (confirmVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            colors = superKeyFieldColors(),
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        imageVector = if (confirmVisible) Icons.Outlined.Visibility
                        else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@Composable
fun superKeyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.Transparent,
    errorBorderColor = MaterialTheme.colorScheme.error,
)

@Composable
fun CustomKPImgView(viewModel: PatchesViewModel) {
    if (!viewModel.useCustomKPImg) return
    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_custom_kpimg_label),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (viewModel.customKPImgFileName.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.patch_custom_kpimg_file, viewModel.customKPImgFileName),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun BootimgView(slot: String, boot: String) {
    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_bootimg),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (slot.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.patch_item_bootimg_slot) + " " + slot,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_bootimg_dev) + " " + boot,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun KernelImageView(kImgInfo: KPModel.KImgInfo) {
    TonalCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.patch_item_kernel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(text = kImgInfo.banner, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@Composable
fun SelectFileButton(
    text: String,
    opaque: Boolean = false,
    onSelected: (data: Intent, uri: Uri) -> Unit
) {
    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        val data = it.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        onSelected(data, uri)
    }

    // When a custom wallpaper is enabled the secondaryContainer token is made
    // translucent (only the alpha channel changes), so restoring alpha to 1f
    // keeps this button opaque with its original color.
    val colors = if (opaque) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 1f)
        )
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }

    FilledTonalButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            selectFileLauncher.launch(intent)
        },
        shape = MaterialTheme.shapes.large,
        colors = colors
    ) {
        Text(text = text)
    }
}

@Composable
fun ErrorView(error: String) {
    if (error.isEmpty()) return
    TonalCard(flat = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.patch_item_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(text = error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

