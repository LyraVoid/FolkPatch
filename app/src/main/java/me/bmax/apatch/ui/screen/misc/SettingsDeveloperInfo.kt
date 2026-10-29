package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.component.folk.folkOverlayColor
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import me.bmax.apatch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperInfo(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/LyraVoid/FolkPatch"
    val telegramUrl = "https://t.me/FolkPatch"
    val sociabuzzUrl = "https://ifdian.net/a/matsuzaka_yuki"

    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = folkOverlayColor(),
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FolkDimens.Size24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(FolkDimens.Size110)
                        .shadow(FolkDimens.Size8, CircleShape)
                        .clip(CircleShape)
                        .background(folkInsetColor()),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("http://q.qlogo.cn/headimg_dl?dst_uin=3231515355&spec=640&img_type=jpg")
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = stringResource(R.string.developer_profile_picture),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(folkInsetColor())
                    )
                }

                Spacer(modifier = Modifier.height(FolkDimens.Size16))

                Text(
                    text = "Matsuzaka Yuki",
                    style = MaterialTheme.typography.titleLargeEmphasized
                )
                Text(
                    text = stringResource(R.string.developer_and_maintainer),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(FolkDimens.Size16))

                Text(
                    text = "\"美しい世界を見てきましょう\"",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = FolkDimens.Size16)
                )

                Spacer(modifier = Modifier.height(FolkDimens.Size24))

                Row(
                    modifier = Modifier.padding(top = FolkDimens.Size16),
                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
                ) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri(githubUrl) },
                        modifier = Modifier.height(FolkDimens.Size38)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.github),
                            contentDescription = stringResource(R.string.github),
                            modifier = Modifier.size(FolkDimens.Size20)
                        )
                        Spacer(Modifier.width(FolkDimens.Size8))
                        Text(stringResource(R.string.github))
                    }

                    FilledTonalButton(
                        onClick = { uriHandler.openUri(telegramUrl) },
                        modifier = Modifier.height(FolkDimens.Size38)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.telegram),
                            contentDescription = stringResource(R.string.telegram),
                            modifier = Modifier.size(FolkDimens.Size20)
                        )
                        Spacer(Modifier.width(FolkDimens.Size8))
                        Text(stringResource(R.string.telegram))
                    }
                }
                Spacer(modifier = Modifier.height(FolkDimens.Size16))

                FilledTonalButton(
                    onClick = { uriHandler.openUri(sociabuzzUrl) },
                    modifier = Modifier.height(FolkDimens.Size38)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Coffee,
                        contentDescription = stringResource(R.string.support_or_donate),
                        modifier = Modifier.size(FolkDimens.Size20)
                    )
                    Spacer(Modifier.width(FolkDimens.Size8))
                    Text(stringResource(R.string.support_or_donate))
                }

                Spacer(modifier = Modifier.height(FolkDimens.Size16))
            }
        }
    }
}
