package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 通用背景选择/清除对话框，用于长按卡片后弹出背景选项
 *
 * @param showDialog 是否显示对话框
 * @param onDismiss 对话框关闭回调
 * @param title 对话框标题
 * @param selectLabel 选择背景按钮文本
 * @param clearLabel 清除背景按钮文本
 * @param hasExisting 是否已有背景（控制是否显示清除按钮）
 * @param onSelectImage 选择图片回调
 * @param onClearImage 清除背景回调
 */
@Composable
fun BackgroundOptionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    title: String,
    selectLabel: String,
    clearLabel: String,
    hasExisting: Boolean = true,
    onSelectImage: () -> Unit,
    onClearImage: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onSelectImage()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectLabel)
                    }
                    if (hasExisting) {
                        Button(
                            onClick = {
                                onDismiss()
                                onClearImage()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(clearLabel)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
