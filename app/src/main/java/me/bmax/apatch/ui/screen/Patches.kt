package me.bmax.apatch.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

private const val TAG = "Patches"

@Destination<RootGraph>
@Composable
fun Patches(mode: PatchesViewModel.PatchMode) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<PatchesViewModel>()
    LaunchedEffect(mode) {
        viewModel.prepare(mode)
    }

    Scaffold(topBar = {
        TopBar()
    }, floatingActionButton = {
        if (viewModel.needReboot) {
            val reboot = stringResource(id = R.string.reboot)
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            reboot()
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Refresh, reboot) },
                text = { Text(text = reboot) },
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
            )
        }
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                val permissions = arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                val permissionsToRequest = permissions.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (permissionsToRequest.isNotEmpty()) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        permissionsToRequest.toTypedArray(),
                        1001
                    )
                }
            }

            PatchMode(mode)
            ErrorView(viewModel.error)
            KernelPatchImageView(viewModel.kpimgInfo)
            CustomKPImgView(viewModel)

            // select boot.img
            if ((mode == PatchesViewModel.PatchMode.PATCH_ONLY || mode == PatchesViewModel.PatchMode.RESTORE) && viewModel.kimgInfo.banner.isEmpty()) {
                SelectFileButton(
                    text = stringResource(id = R.string.patch_select_bootimg_btn),
                    onSelected = { data, uri ->
                        Log.d(TAG, "select boot.img, data: $data, uri: $uri")
                        viewModel.copyAndParseBootimg(uri)
                    }
                )
            }

            if (viewModel.bootSlot.isNotEmpty() || viewModel.bootDev.isNotEmpty()) {
                BootimgView(slot = viewModel.bootSlot, boot = viewModel.bootDev)
            }

            if (viewModel.kimgInfo.banner.isNotEmpty()) {
                KernelImageView(viewModel.kimgInfo)
            }

            if (viewModel.useCustomKPImg && !viewModel.patching && !viewModel.patchdone) {
                SelectFileButton(
                    text = stringResource(id = R.string.patch_select_kpimg_btn),
                    onSelected = { _, uri ->
                        viewModel.setCustomKPImg(uri)
                    }
                )
            }

            // existed extras
            if (mode == PatchesViewModel.PatchMode.PATCH_AND_INSTALL || mode == PatchesViewModel.PatchMode.INSTALL_TO_NEXT_SLOT) {
                viewModel.existedExtras.forEach(action = {
                    ExtraItem(extra = it, true, onDelete = {
                        viewModel.existedExtras.remove(it)
                    })
                })
            }

            // add new extras
            if (mode != PatchesViewModel.PatchMode.UNPATCH && mode != PatchesViewModel.PatchMode.RESTORE) {
                viewModel.newExtras.forEach(action = {
                    ExtraItem(extra = it, false, onDelete = {
                        val idx = viewModel.newExtras.indexOf(it)
                        viewModel.newExtras.remove(it)
                        viewModel.newExtrasFileName.removeAt(idx)
                    })
                })
            }

            // add new KPM
            if (!viewModel.patching && !viewModel.patchdone && mode != PatchesViewModel.PatchMode.UNPATCH && mode != PatchesViewModel.PatchMode.RESTORE) {

                SelectFileButton(
                    text = stringResource(id = R.string.patch_embed_kpm_btn),
                    onSelected = { data, uri ->
                        Log.d(TAG, "select kpm, data: $data, uri: $uri")
                        viewModel.embedKPM(uri)
                    }
                )
            }

            // do patch, update, unpatch
            if (!viewModel.patching && !viewModel.patchdone) {
                // patch start
                if (mode != PatchesViewModel.PatchMode.UNPATCH && mode != PatchesViewModel.PatchMode.RESTORE) {
                    StartButton(stringResource(id = R.string.patch_start_patch_btn)) {
                        viewModel.doPatch(mode, false)
                    }
                }
                // unpatch
                if (mode == PatchesViewModel.PatchMode.UNPATCH) {
                    StartButton(stringResource(id = R.string.patch_start_unpatch_btn)) { viewModel.doUnpatch() }
                }
            }

            // patch log
            if (viewModel.patching || viewModel.patchdone) {
                SelectionContainer {
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = viewModel.patchLog,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                    )
                }
                LaunchedEffect(viewModel.patchLog) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // loading progress
            if (viewModel.running) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .align(Alignment.CenterHorizontally)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}


@Composable
private fun StartButton(text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f)
            ),
            content = {
                Text(text = text)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraConfigDialog(kpmInfo: KPModel.KPMInfo, onDismiss: () -> Unit) {
    var event by remember { mutableStateOf(kpmInfo.event) }
    var args by remember { mutableStateOf(kpmInfo.args) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
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
        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
    }
}

@Composable
private fun ExtraItem(extra: KPModel.IExtraInfo, existed: Boolean, onDelete: () -> Unit) {
    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog && extra is KPModel.KPMInfo) {
        ExtraConfigDialog(extra, onDismiss = { showConfigDialog = false })
    }

    ExpressiveCard(flat = true) {
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
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Config",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showConfigDialog = true }
                    )
                }

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
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
private fun KernelPatchImageView(kpImgInfo: KPModel.KPImgInfo) {
    if (kpImgInfo.version.isEmpty()) return
    ExpressiveCard(flat = true) {
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
private fun CustomKPImgView(viewModel: PatchesViewModel) {
    if (!viewModel.useCustomKPImg) return
    ExpressiveCard(flat = true) {
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
private fun BootimgView(slot: String, boot: String) {
    ExpressiveCard(flat = true) {
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
private fun KernelImageView(kImgInfo: KPModel.KImgInfo) {
    ExpressiveCard(flat = true) {
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
private fun SelectFileButton(text: String, onSelected: (data: Intent, uri: Uri) -> Unit) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                selectFileLauncher.launch(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f)
            ),
            content = { Text(text = text) }
        )
    }
}

@Composable
private fun ErrorView(error: String) {
    if (error.isEmpty()) return
    ExpressiveCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.patch_item_error),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PatchMode(mode: PatchesViewModel.PatchMode) {
    ExpressiveCard(flat = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = mode.sId), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    TopAppBar(title = { Text(stringResource(R.string.patch_config_title)) })
}