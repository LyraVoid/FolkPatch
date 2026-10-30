@file:OptIn(ExperimentalMaterial3Api::class)

package me.bmax.apatch.ui.screen.settings.general

import me.bmax.apatch.ui.theme.tokens.FolkDimens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.util.*
import me.bmax.apatch.util.ui.showToast
import java.io.File
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSUPathDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suPath by remember { mutableStateOf("/system/bin/su") }
    LaunchedEffect(Unit) {
        suPath = withContext(Dispatchers.IO) {
            runCatching { me.bmax.apatch.Natives.suPath() }.getOrDefault("/system/bin/su")
        }
    }
    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Box(
                Modifier
                    .padding(PaddingValues(bottom = FolkDimens.Size16))
                    .align(Alignment.Start)
            ) {
                Text(
                    text = stringResource(id = R.string.setting_reset_su_path),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(PaddingValues(bottom = FolkDimens.Size12))
                    .align(Alignment.Start)
            ) {
                OutlinedTextField(
                    value = suPath,
                    onValueChange = {
                        suPath = it
                    },
                    label = { Text(stringResource(id = R.string.setting_reset_su_new_path)) },
                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {

                    Text(stringResource(id = android.R.string.cancel))
                }

                Button(enabled = suPath.startsWith("/") && suPath.trim().length > 1, onClick = {
                    showDialog.value = false
                    val newPath = suPath.trim()
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            runCatching {
                                val reset = me.bmax.apatch.Natives.resetSuPath(newPath)
                                if (reset) {
                                    rootShellForResult(
                                        "printf %s ${newPath.shellSingleQuoted()} > ${APApplication.SU_PATH_FILE}"
                                    )
                                }
                                reset
                            }.getOrDefault(false)
                        }
                        showToast(context, if (success) R.string.success else R.string.failure)
                    }
                }, colors = ButtonDefaults.buttonColors()) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

private fun String.shellSingleQuoted(): String {
    return "'" + replace("'", "'\\''") + "'"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanStorageDialog(
    showDialog: MutableState<Boolean>,
    titleRes: Int = R.string.settings_clean_storage,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Text(
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )
            Text(
                text = stringResource(id = R.string.settings_clean_storage_confirm),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size12)
            )

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }

                Button(onClick = {
                    showDialog.value = false
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            runCatching {
                                // 删除容易因残留/占位文件导致下载或应用失败的目录，随后重建空目录
                                listOf("themes", "music", "sound_effects").forEach { name ->
                                    val dir = File(context.filesDir, name)
                                    if (dir.exists()) dir.deleteRecursively()
                                    dir.mkdirs()
                                }
                                context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
                            }.isSuccess
                        }
                        showToast(context, if (success) R.string.settings_clean_storage_done else R.string.failure)
                    }
                }, colors = ButtonDefaults.buttonColors()) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}
