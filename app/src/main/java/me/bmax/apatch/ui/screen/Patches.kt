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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.reboot
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator

import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.extra.SuperDialog

private const val TAG = "Patches"

@Destination<RootGraph>
@Composable
fun Patches(mode: PatchesViewModel.PatchMode) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val viewModel = viewModel<PatchesViewModel>()

    SideEffect {
        viewModel.prepare(mode)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = stringResource(R.string.patch_config_title),
            scrollBehavior = scrollBehavior
        )
    }, popupHost = {}
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                Column (
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)

                ) {
                    val context = LocalContext.current

                    // request permissions
                    val permissions = arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    val permissionsToRequest = permissions.filter {
                        ContextCompat.checkSelfPermission(
                            context,
                            it
                        ) != PackageManager.PERMISSION_GRANTED
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        ActivityCompat.requestPermissions(
                            context as Activity,
                            permissionsToRequest.toTypedArray(),
                            1001
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    PatchMode(mode)
                    ErrorView(viewModel.error)
                    KernelPatchImageView(viewModel.kpimgInfo)

                    if (mode == PatchesViewModel.PatchMode.PATCH_ONLY && selectedBootImage != null && viewModel.kimgInfo.banner.isEmpty()) {
                        viewModel.copyAndParseBootimg(selectedBootImage!!)
                        // Fix endless loop. It's not normal if (parse done && working thread is not working) but banner still null
                        // Leave user re-choose
                        if (!viewModel.running && viewModel.kimgInfo.banner.isEmpty()) {
                            selectedBootImage = null
                        }
                    }

                    // select boot.img
                    if (mode == PatchesViewModel.PatchMode.PATCH_ONLY && viewModel.kimgInfo.banner.isEmpty()) {
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

                    if (mode != PatchesViewModel.PatchMode.UNPATCH && viewModel.kimgInfo.banner.isNotEmpty()) {
                        SetSuperKeyView(viewModel)
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
                    if (mode != PatchesViewModel.PatchMode.UNPATCH) {
                        viewModel.newExtras.forEach(action = {
                            ExtraItem(extra = it, false, onDelete = {
                                val idx = viewModel.newExtras.indexOf(it)
                                viewModel.newExtras.remove(it)
                                viewModel.newExtrasFileName.removeAt(idx)
                            })
                        })
                    }

                    // add new KPM
                    if (viewModel.superkey.isNotEmpty() && !viewModel.patching && !viewModel.patchdone && mode != PatchesViewModel.PatchMode.UNPATCH) {
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
                        if (mode != PatchesViewModel.PatchMode.UNPATCH && viewModel.superkey.isNotEmpty()) {
                            StartButton(stringResource(id = R.string.patch_start_patch_btn)) {
                                viewModel.doPatch(mode)
                            }
                        }
                        // unpatch
                        if (mode == PatchesViewModel.PatchMode.UNPATCH && viewModel.kimgInfo.banner.isNotEmpty()) {
                            StartButton(stringResource(id = R.string.patch_start_unpatch_btn)) { viewModel.doUnpatch() }
                        }
                    }

                    // patch log
                    if (viewModel.patching || viewModel.patchdone) {
                        SelectionContainer {
                            Text(
                                modifier = Modifier.padding(8.dp),
                                text = viewModel.patchLog,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        LaunchedEffect(viewModel.patchLog) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.needReboot) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch { withContext(Dispatchers.IO) { reboot() } }
                            },
                            content = {
                                Text(text = stringResource(id = R.string.reboot))
                            }
                        )
                    }

                    // loading progress
                    if (viewModel.running) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(16.dp)
                            )
                        }
                    }
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
            content = {
                Text(text = text)
            }
        )
    }
}

@Composable
private fun ExtraConfigDialog(
    kpmInfo: KPModel.KPMInfo,
    show: MutableState<Boolean>
) {
    var event by remember { mutableStateOf(kpmInfo.event) }
    var args by remember { mutableStateOf(kpmInfo.args) }

    SuperDialog(
        show = show.value,
        title = stringResource(R.string.kpm_control_dialog_title),
        onDismissRequest = { show.value = false },
        content = {
            Column {
                TextField(
                    value = event,
                    label = stringResource(R.string.patch_item_extra_event),
                    onValueChange = {
                        event = it
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = args,
                    label = stringResource(id = R.string.patch_item_extra_args),
                    onValueChange = {
                        args = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = {
                            show.value = false
                        },
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(Modifier.width(20.dp))

                    TextButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            kpmInfo.event = event
                            kpmInfo.args = args
                            show.value = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    )
}

@Composable
private fun ExtraItem(extra: KPModel.IExtraInfo, existed: Boolean, onDelete: () -> Unit) {
    val showConfigDialog =  remember { mutableStateOf(false) }

    if (extra is KPModel.KPMInfo && showConfigDialog.value) {
        ExtraConfigDialog(
            kpmInfo = extra,
            show = showConfigDialog
        )
    }

    Card {
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
                    style = MiuixTheme.textStyles.body2,
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
                            .clickable { showConfigDialog.value = true }
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
                    style = MiuixTheme.textStyles.body2
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_version) + " "} ${kpmInfo.version}",
                    style = MiuixTheme.textStyles.body2
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_kpm_license) + " "} ${kpmInfo.license}",
                    style = MiuixTheme.textStyles.body2
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_author) + " "} ${kpmInfo.author}",
                    style = MiuixTheme.textStyles.body2
                )
                Text(
                    text = "${stringResource(id = R.string.patch_item_extra_kpm_desciption) + " "} ${kpmInfo.description}",
                    style = MiuixTheme.textStyles.body2
                )
            }
        }
    }
}


@Composable
private fun SetSuperKeyView(viewModel: PatchesViewModel) {
    var skey by remember { mutableStateOf(viewModel.superkey) }
    var showWarn by remember { mutableStateOf(!viewModel.checkSuperKeyValidation(skey)) }
    var keyVisible by remember { mutableStateOf(false) }

    Card {
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
                    text = stringResource(id = R.string.patch_item_skey),
                    style = MiuixTheme.textStyles.body1
                )
            }
            if (showWarn) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    color = Color.Red,
                    text = stringResource(id = R.string.patch_item_set_skey_label),
                    style = MiuixTheme.textStyles.body2
                )
            }

            Box (Modifier.padding(top = 6.dp)) {
                TextField(
                    value = skey,
                    label = stringResource(id = R.string.patch_set_superkey),
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    onValueChange = {
                        skey = it
                        if (viewModel.checkSuperKeyValidation(it)) {
                            viewModel.superkey = it
                            showWarn = false
                        } else {
                            viewModel.superkey = ""
                            showWarn = true
                        }
                    },
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp),
                    onClick = { keyVisible = !keyVisible }
                ) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun KernelPatchImageView(kpImgInfo: KPModel.KPImgInfo) {
    if (kpImgInfo.version.isEmpty()) return
    Card {
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
                    style = MiuixTheme.textStyles.body1
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_version) + " " + Version.uInt2String(
                    kpImgInfo.version.substring(2).toUInt(16)
                ), style = MiuixTheme.textStyles.body2
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_comile_time) + " " + kpImgInfo.compileTime,
                style = MiuixTheme.textStyles.body2
            )
            Text(
                text = stringResource(id = R.string.patch_item_kpimg_config) + " " + kpImgInfo.config,
                style = MiuixTheme.textStyles.body2
            )
        }
    }
}

@Composable
private fun BootimgView(slot: String, boot: String) {
    Card {
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
                    style = MiuixTheme.textStyles.body1
                )
            }
            if (slot.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.patch_item_bootimg_slot) + " " + slot,
                    style = MiuixTheme.textStyles.body2
                )
            }
            Text(
                text = stringResource(id = R.string.patch_item_bootimg_dev) + " " + boot,
                style = MiuixTheme.textStyles.body2
            )
        }
    }
}

@Composable
private fun KernelImageView(kImgInfo: KPModel.KImgInfo) {
    Card {
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
                    style = MiuixTheme.textStyles.body2
                )
            }
            Text(text = kImgInfo.banner, style = MiuixTheme.textStyles.body2)
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
                selectFileLauncher.launch(intent)
            },
            content = { Text(text = text) }
        )
    }
}

@Composable
private fun ErrorView(error: String) {
    if (error.isEmpty()) return
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.patch_item_error),
                style = MiuixTheme.textStyles.body2
            )
            Text(text = error, style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun PatchMode(mode: PatchesViewModel.PatchMode) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = mode.sId), style = MiuixTheme.textStyles.body2)
        }
    }
}
