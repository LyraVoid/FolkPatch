package me.bmax.apatch.ui.screen.theme

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.viewmodel.ThemeStoreViewModel
import java.io.File
import me.bmax.apatch.util.DownloadProgress
import me.bmax.apatch.util.DownloadStatus
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeDownloadDialog(
    theme: ThemeStoreViewModel.RemoteTheme,
    progress: DownloadProgress,
    onCancel: () -> Unit,
    onPause: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.theme_download_title)) },
        text = {
            Column {
                // 主题信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 预览图（正方形加圆角）
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(theme.previewUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = theme.name,
                        modifier = Modifier
                            .size(FolkDimens.Size64)
                            .padding(end = FolkDimens.Size12)
                            .clip(RoundedCornerShape(FolkDimens.Size8)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = theme.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(FolkDimens.Size16))
                
                // 总进度条
                Text(
                    text = "${stringResource(R.string.theme_download_progress)}: ${(progress.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(FolkDimens.Size8))
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.overallProgress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "DownloadProgress"
                )
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(FolkDimens.Size16))
                
                // 文件进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.theme_download_file)}: ${(progress.fileProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${stringResource(R.string.theme_download_image)}: ${(progress.imageProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 暂停提示
                if (progress.status == DownloadStatus.PAUSED) {
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(
                        text = stringResource(R.string.theme_download_paused),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 错误信息
                if (progress.errorMessage != null) {
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(
                        text = progress.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.theme_download_cancel))
            }
        },
        dismissButton = {
            val paused = progress.status == DownloadStatus.PAUSED
            OutlinedButton(
                onClick = onPause,
                enabled = paused || progress.status == DownloadStatus.DOWNLOADING
            ) {
                Text(
                    stringResource(
                        if (paused) R.string.theme_download_resume else R.string.theme_download_pause
                    )
                )
            }
        }
    )
}

/**
 * 下载完成对话框
 */
@Composable
fun ThemeDownloadCompleteDialog(
    theme: ThemeStoreViewModel.RemoteTheme,
    onApply: () -> Unit,
    onGoToMyThemes: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.theme_download_completed))
            }
        },
        text = {
            Column {
                Text(
                    text = "${theme.name} ${stringResource(R.string.theme_download_finalizing)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text(stringResource(R.string.theme_download_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onGoToMyThemes) {
                Text(stringResource(R.string.theme_download_go_to_my_themes))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeFilterSheetContent(
    currentAuthor: String,
    currentSource: String,
    currentTypePhone: Boolean,
    currentTypeTablet: Boolean,
    onApply: (String, String, Boolean, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var author by remember { mutableStateOf(currentAuthor) }
    var source by remember { mutableStateOf(currentSource) }
    var typePhone by remember { mutableStateOf(currentTypePhone) }
    var typeTablet by remember { mutableStateOf(currentTypeTablet) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(FolkDimens.Size24)
    ) {
        Text(
            text = stringResource(R.string.theme_store_filter_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = FolkDimens.Size16)
        )

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text(stringResource(R.string.theme_store_filter_author)) },
            placeholder = { Text(stringResource(R.string.theme_store_filter_author_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(FolkDimens.Size16))

        Text(
            text = stringResource(R.string.theme_store_filter_source),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(FolkDimens.Size8))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
            FilterChip(
                selected = source == "all",
                onClick = { source = "all" },
                label = { Text(stringResource(R.string.theme_store_filter_source_all)) },
                colors = chipColors
            )
            FilterChip(
                selected = source == "official",
                onClick = { source = "official" },
                label = { Text(stringResource(R.string.theme_source_official)) },
                colors = chipColors
            )
            FilterChip(
                selected = source == "third_party",
                onClick = { source = "third_party" },
                label = { Text(stringResource(R.string.theme_source_third_party)) },
                colors = chipColors
            )
        }

        Spacer(modifier = Modifier.height(FolkDimens.Size16))

        Text(
            text = stringResource(R.string.theme_store_filter_type),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(FolkDimens.Size8))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
            FilterChip(
                selected = typePhone,
                onClick = { typePhone = !typePhone },
                label = { Text(stringResource(R.string.theme_type_phone)) },
                colors = chipColors
            )
            FilterChip(
                selected = typeTablet,
                onClick = { typeTablet = !typeTablet },
                label = { Text(stringResource(R.string.theme_type_tablet)) },
                colors = chipColors
            )
        }

        Spacer(modifier = Modifier.height(FolkDimens.Size24))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    onReset()
                    author = ""
                    source = "all"
                    typePhone = true
                    typeTablet = true
                }
            ) {
                Text(stringResource(R.string.theme_store_filter_reset))
            }
            Spacer(modifier = Modifier.width(FolkDimens.Size8))
            Button(
                onClick = { onApply(author, source, typePhone, typeTablet) }
            ) {
                Text(stringResource(R.string.theme_store_filter_apply))
            }
        }
        Spacer(modifier = Modifier.height(FolkDimens.Size16))
    }
}

@Composable
fun ThemeGridItem(
    theme: ThemeStoreViewModel.RemoteTheme,
    localPreviewPath: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val previewFile = localPreviewPath?.let { File(it) }
    val imageModel = if (previewFile != null && previewFile.exists()) {
        Uri.fromFile(previewFile)
    } else {
        theme.previewUrl
    }

    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .folkPressScale(interactionSource)
            .clickable(role = Role.Button, 
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = FolkDimens.Size4)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = theme.name,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentScale = ContentScale.FillWidth
        )
    }
}
