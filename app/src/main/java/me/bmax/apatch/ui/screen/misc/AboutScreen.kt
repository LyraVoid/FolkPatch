package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import me.bmax.apatch.ui.component.SegmentedChevron
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.util.Version
import androidx.compose.ui.semantics.Role

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun AboutScreen(navigator: DestinationsNavigator) {
    val uriHandler = LocalUriHandler.current

    FolkScaffold(
        title = stringResource(R.string.about),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = dropUnlessResumed { navigator.popBackStack() },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(FolkDimens.Size24))

                    // App icon (transparent cutout, no background)
                    Image(
                        painter = painterResource(id = R.drawable.about_icon_anime),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(FolkDimens.Size78)
                    )

                    Spacer(modifier = Modifier.height(FolkDimens.Size14))

                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(FolkDimens.Size4))

                    Text(
                        text = stringResource(
                            id = R.string.about_app_version,
                            if (BuildConfig.VERSION_NAME.contains(BuildConfig.VERSION_CODE.toString())) "${BuildConfig.VERSION_CODE}" else "${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = stringResource(
                            id = R.string.about_powered_by,
                            "KernelPatch (${Version.buildKPVString()})"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = FolkDimens.Size2)
                    )

                    Spacer(modifier = Modifier.height(FolkDimens.Size24))

                    // Description card — M3E TonalCard
                    TonalCard(
                        modifier = Modifier.padding(horizontal = FolkDimens.Size16),
                        flat = true,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FolkDimens.Size20)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(FolkDimens.Size20)
                                )
                                Spacer(modifier = Modifier.width(FolkDimens.Size12))
                                Text(
                                    text = stringResource(id = R.string.about),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(modifier = Modifier.height(FolkDimens.Size12))
                            Text(
                                text = stringResource(id = R.string.about_app_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(FolkDimens.Size16))

                    // Links — M3E SegmentedColumn
                    SegmentedColumn(
                        modifier = Modifier.padding(horizontal = FolkDimens.Size0),
                    ) {
                        item {
                            SplicedLinkItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.github),
                                        contentDescription = null,
                                        modifier = Modifier.size(FolkDimens.Size20)
                                    )
                                },
                                title = stringResource(id = R.string.about_github),
                                onClick = { uriHandler.openUri("https://github.com/LyraVoid/FolkPatch") }
                            )
                        }
                        item {
                            SplicedLinkItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.telegram),
                                        contentDescription = null,
                                        modifier = Modifier.size(FolkDimens.Size20)
                                    )
                                },
                                title = stringResource(id = R.string.about_telegram_channel),
                                onClick = { uriHandler.openUri("https://t.me/FolkPatch") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(FolkDimens.Size32))
                }
            }
        }
    }
}

@Composable
private fun SplicedLinkItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FolkDimens.Size24, vertical = FolkDimens.Size16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(FolkDimens.Size20))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        SegmentedChevron()
    }
}
