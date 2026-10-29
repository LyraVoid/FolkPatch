package me.bmax.apatch.ui.screen.patches

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.WarningCard
import me.bmax.apatch.ui.theme.tokens.FolkMotion
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.isJailbreakPatchBlocked
import me.bmax.apatch.util.reboot

private const val TAG = "Patches"

@Destination<RootGraph>
@Composable
fun Patches(mode: PatchesViewModel.PatchMode) {
    var jailbreakBlocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        jailbreakBlocked = withContext(Dispatchers.IO) { isJailbreakPatchBlocked() }
    }

    if (jailbreakBlocked) {
        FolkScaffold(
            title = stringResource(R.string.patch_config_title),
            titleStyle = FolkTitleStyle.Inline,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(12.dp)
            ) {
                WarningCard(
                    message = stringResource(R.string.jailbreak_no_patch),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        return
    }

    val scrollState = rememberScrollState()
    val logScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<PatchesViewModel>()
    var needKey by remember {
        mutableStateOf(
            APApplication.sharedPreferences.getBoolean("patch_custom_superkey_enabled", false)
        )
    }
    LaunchedEffect(mode) {
        viewModel.prepare(mode)
    }

    FolkScaffold(
        title = stringResource(R.string.patch_config_title),
        titleStyle = FolkTitleStyle.Inline,
        floatingActionButton = {
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
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        opaque = true,
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

                if (
                    mode != PatchesViewModel.PatchMode.UNPATCH &&
                    mode != PatchesViewModel.PatchMode.RESTORE &&
                    viewModel.kimgInfo.banner.isNotEmpty() &&
                    !viewModel.patching &&
                    !viewModel.patchdone
                ) {
                    PatchSuperKeySection(
                        viewModel = viewModel,
                        checked = needKey,
                        onCheckedChange = { checked ->
                            needKey = checked
                            if (!checked) {
                                viewModel.superkey = ""
                            }
                            APApplication.sharedPreferences.edit()
                                .putBoolean("patch_custom_superkey_enabled", checked)
                                .apply()
                        },
                    )
                }

                if (viewModel.useCustomKPImg && !viewModel.patching && !viewModel.patchdone) {
                    SelectFileButton(
                        text = stringResource(id = R.string.patch_select_kpimg_btn),
                        opaque = true,
                        onSelected = { _, uri -> viewModel.setCustomKPImg(uri) }
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
                        opaque = true,
                        onSelected = { data, uri ->
                            Log.d(TAG, "select kpm, data: $data, uri: $uri")
                            viewModel.embedKPM(uri)
                        }
                    )
                }

                // patch log
                if (viewModel.patching || viewModel.patchdone) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 360.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                modifier = Modifier
                                    .verticalScroll(logScrollState)
                                    .padding(16.dp),
                                text = viewModel.patchLog,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                            )
                        }
                    }
                    LaunchedEffect(viewModel.patchLog) {
                        kotlinx.coroutines.yield()
                        logScrollState.animateScrollTo(
                            logScrollState.maxValue,
                            animationSpec = FolkMotion.ScrollIntoView
                        )
                    }
                }

                // loading progress
                if (viewModel.running) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            val keyReady = mode == PatchesViewModel.PatchMode.UNPATCH ||
                !needKey ||
                (viewModel.superkey.isNotEmpty() &&
                    viewModel.checkSuperKeyValidation(viewModel.superkey))
            val canStart = keyReady && !viewModel.running && !viewModel.patching && !viewModel.patchdone &&
                viewModel.kimgInfo.banner.isNotEmpty() &&
                mode != PatchesViewModel.PatchMode.RESTORE
            if (canStart) {
                val actionText = if (mode == PatchesViewModel.PatchMode.UNPATCH) {
                    stringResource(id = R.string.patch_start_unpatch_btn)
                } else {
                    stringResource(id = R.string.patch_start_patch_btn)
                }
                StartButton(
                    text = actionText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                ) {
                    if (mode == PatchesViewModel.PatchMode.UNPATCH) {
                        viewModel.doUnpatch()
                    } else {
                        viewModel.doPatch(mode, needKey)
                    }
                }
            }
        }
    }
}


@Composable
private fun PatchMode(mode: PatchesViewModel.PatchMode) {
    TonalCard(flat = true) {
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

