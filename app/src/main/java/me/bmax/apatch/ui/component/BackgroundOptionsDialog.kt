package me.bmax.apatch.ui.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils

/**
 * 模块信息数据类
 */
data class ModuleInfoData(
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val description: String = ""
)

/**
 * 通用背景选择/清除 + 模块信息编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundOptionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String,
    // Banner 相关
    selectLabel: String,
    clearLabel: String,
    hasExisting: Boolean = true,
    showBannerSection: Boolean = true,
    onSelectImage: () -> Unit,
    onClearImage: () -> Unit,
    onRestoreDefault: (() -> Unit)? = null,   // 改为可空，默认 null
    restoreLabel: String = "",                 // 改为默认空字符串
    // 模块信息相关（可选，不提供时不显示模块信息区域）
    customInfoTitle: String = "",
    customInfoNameLabel: String = "",
    customInfoVersionLabel: String = "",
    customInfoAuthorLabel: String = "",
    customInfoDescriptionLabel: String = "",
    saveLabel: String = "",
    resetLabel: String = "",
    // 模块信息初始值
    initialModuleInfo: ModuleInfoData = ModuleInfoData(),
    // 磁盘上是否已存在与原始数据不同的自定义信息（用于启用重置按钮）
    hasSavedCustomInfo: Boolean = false,
    // 重载计数器（由调用方传入，保存/重置时自增以触发数据刷新）
    customInfoReloadKey: MutableState<Int>? = null,
    // 模块信息回调（nullable，为 null 时不显示模块信息区域）
    onSaveModuleInfo: ((ModuleInfoData) -> Unit)? = null,
    onResetModuleInfo: (() -> Unit)? = null,
) {
    if (showDialog) {
        // The theme applies custom background opacity to some color roles (surface,
        // secondaryContainer, surfaceContainer). Restore full opacity inside this
        // dialog so the "Module Editor" / "Select image" layout is not affected
        // by the background transparency feature.
        val scheme = MaterialTheme.colorScheme
        MaterialTheme(
            colorScheme = scheme.copy(
                surface = scheme.surface.copy(alpha = 1f),
                secondaryContainer = scheme.secondaryContainer.copy(alpha = 1f),
                surfaceContainer = scheme.surfaceContainer.copy(alpha = 1f),
            )
        ) {
            BasicAlertDialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    decorFitsSystemWindows = true,
                    usePlatformDefaultWidth = false,
                )
            ) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    color = AlertDialogDefaults.containerColor,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // 标题
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        // 可滚动内容区
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Banner 区域
                            if (showBannerSection) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 选择图片按钮 — FilledTonal 风格
                                    FilledTonalButton(
                                        onClick = {
                                            onDismiss()
                                            onSelectImage()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = selectLabel,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    //  恢复默认壁纸按钮（和"选择图片"风格一致）
                                    if (onRestoreDefault != null) {
                                        FilledTonalButton(
                                            onClick = {
                                                onDismiss()
                                                onRestoreDefault()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Text(text = restoreLabel, modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                    // 清除按钮 — Outlined 风格，仅有已存在图片时显示
                                    if (hasExisting) {
                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                onClearImage()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = clearLabel,
                                                modifier = Modifier.padding(start = 8.dp),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
    
                                // Banner 与模块信息区域的分隔线
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
    
                            if (onSaveModuleInfo != null && onResetModuleInfo != null) {
                                // 分隔标题 — 模块信息
                                Text(
                                    text = customInfoTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
    
                                // 模块信息输入字段
                                var name by remember { mutableStateOf(initialModuleInfo.name) }
                                var version by remember { mutableStateOf(initialModuleInfo.version) }
                                var author by remember { mutableStateOf(initialModuleInfo.author) }
                                var description by remember { mutableStateOf(initialModuleInfo.description) }
    
                                // 上次保存/加载的基线值（用于变更检测）
                                var baselineName by remember { mutableStateOf(initialModuleInfo.name) }
                                var baselineVersion by remember { mutableStateOf(initialModuleInfo.version) }
                                var baselineAuthor by remember { mutableStateOf(initialModuleInfo.author) }
                                var baselineDescription by remember { mutableStateOf(initialModuleInfo.description) }
    
                                LaunchedEffect(initialModuleInfo) {
                                    name = initialModuleInfo.name
                                    version = initialModuleInfo.version
                                    author = initialModuleInfo.author
                                    description = initialModuleInfo.description
                                    baselineName = initialModuleInfo.name
                                    baselineVersion = initialModuleInfo.version
                                    baselineAuthor = initialModuleInfo.author
                                    baselineDescription = initialModuleInfo.description
                                }
    
                                val textFieldShape = RoundedCornerShape(50f)
    
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text(customInfoNameLabel) },
                                    singleLine = true,
                                    shape = textFieldShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
    
                                OutlinedTextField(
                                    value = version,
                                    onValueChange = { version = it },
                                    label = { Text(customInfoVersionLabel) },
                                    singleLine = true,
                                    shape = textFieldShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
    
                                OutlinedTextField(
                                    value = author,
                                    onValueChange = { author = it },
                                    label = { Text(customInfoAuthorLabel) },
                                    singleLine = true,
                                    shape = textFieldShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                )
    
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text(customInfoDescriptionLabel) },
                                    maxLines = 2,
                                    shape = textFieldShape,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )
    
                                // 底部按钮行
                                val hasChanges = name != baselineName || version != baselineVersion ||
                                        author != baselineAuthor || description != baselineDescription
                                val hasNonBlank = name.isNotBlank() || version.isNotBlank() ||
                                        author.isNotBlank() || description.isNotBlank()
    
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = onDismiss) {
                                        Text(stringResource(android.R.string.cancel))
                                    }
                                    TextButton(
                                        onClick = {
                                            customInfoReloadKey?.value = (customInfoReloadKey?.value ?: 0) + 1
                                            onDismiss()
                                            onResetModuleInfo()
                                        },
                                        enabled = hasChanges || hasSavedCustomInfo
                                    ) {
                                        Text(resetLabel)
                                    }
                                    TextButton(
                                        onClick = {
                                            customInfoReloadKey?.value = (customInfoReloadKey?.value ?: 0) + 1
                                            onDismiss()
                                            onSaveModuleInfo(ModuleInfoData(name, version, author, description))
                                        },
                                        enabled = hasNonBlank && hasChanges
                                    ) {
                                        Text(saveLabel)
                                    }
                                }
                            } else {
                                // 仅取消按钮
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = onDismiss) {
                                        Text(stringResource(android.R.string.cancel))
                                    }
                                }
                            }
                        }
                    }
    
                    // 背景模糊
                    val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                    APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
                }
                }
        }
    }
}
