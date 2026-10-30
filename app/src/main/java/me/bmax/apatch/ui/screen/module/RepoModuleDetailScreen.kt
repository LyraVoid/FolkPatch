package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.ModuleLabel
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkInsetColor
import me.bmax.apatch.ui.viewmodel.RepoModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import me.bmax.apatch.core.ui.R as CoreR

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun RepoModuleDetailScreen(
    navigator: DestinationsNavigator,
    moduleId: String
) {
    val viewModel = viewModel<RepoModuleViewModel>()
    val context = LocalContext.current
    val module = viewModel.selectedModule

    LaunchedEffect(moduleId) {
        viewModel.selectModule(moduleId)
    }

    if (module == null) {
        LaunchedEffect(Unit) { navigator.popBackStack() }
        return
    }

    FolkScaffold(
        title = module.name,
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.popBackStack() },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = FolkDimens.Size16),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
        ) {
            // Section 1: Module basic info
            item(key = "info") {
                TonalCard(flat = true) {
                    Column(
                        modifier = Modifier.padding(FolkDimens.Size16),
                        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
                    ) {
                        // Label row
                        Row(horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)) {
                            ModuleLabel(
                                text = "APM",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            ModuleLabel(
                                text = module.version,
                                containerColor = folkInsetColor(),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (module.license.isNotEmpty()) {
                                ModuleLabel(
                                    text = module.license,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        // Name
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        // Author
                        Text(
                            text = stringResource(R.string.repo_module_detail_by_author, module.author),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Description
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Section 2: Links
            val hasLinks = module.homepage.isNotEmpty() || module.source.isNotEmpty() || module.support.isNotEmpty()
            if (hasLinks) {
                item(key = "links") {
                    SegmentedColumn(title = stringResource(R.string.repo_module_detail_links)) {
                        item(key = "homepage", visible = module.homepage.isNotEmpty()) {
                            TonalCard(flat = true, onClick = { openUrl(context, module.homepage) }) {
                                Row(
                                    modifier = Modifier.padding(FolkDimens.Size16),
                                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Language, contentDescription = null)
                                    Text(stringResource(R.string.repo_module_detail_homepage))
                                }
                            }
                        }
                        item(key = "source", visible = module.source.isNotEmpty()) {
                            TonalCard(flat = true, onClick = { openUrl(context, module.source) }) {
                                Row(
                                    modifier = Modifier.padding(FolkDimens.Size16),
                                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.Code, contentDescription = null)
                                    Text(stringResource(R.string.repo_module_detail_source))
                                }
                            }
                        }
                        item(key = "support", visible = module.support.isNotEmpty()) {
                            TonalCard(flat = true, onClick = { openUrl(context, module.support) }) {
                                Row(
                                    modifier = Modifier.padding(FolkDimens.Size16),
                                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
                                    Text(stringResource(R.string.repo_module_detail_support))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Version history
            if (module.versions.isNotEmpty()) {
                item(key = "versions_header") {
                    SegmentedColumn(title = stringResource(R.string.repo_module_detail_versions)) {
                        module.versions.forEach { ver ->
                            item(key = ver.versionCode) {
                                TonalCard(
                                    flat = true,
                                    onClick = {
                                        download(
                                            context,
                                            ver.zipUrl,
                                            "${module.name}-${ver.version}.zip",
                                            module.description
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(FolkDimens.Size16),
                                        horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ver.version, style = MaterialTheme.typography.bodyLarge)
                                            if (ver.timestamp > 0) {
                                                Text(
                                                    formatTimestamp(ver.timestamp),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        FilledTonalIconButton(onClick = {
                                            download(
                                                context,
                                                ver.zipUrl,
                                                "${module.name}-${ver.version}.zip",
                                                module.description
                                            )
                                        }) {
                                            Icon(Icons.Outlined.Download, contentDescription = stringResource(CoreR.string.core_action_download))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Main download button
            item(key = "download_install") {
                Button(
                    onClick = {
                        val latestVersion = module.versions.firstOrNull()
                        if (latestVersion != null) {
                            download(
                                context,
                                latestVersion.zipUrl,
                                "${module.name}-${module.version}.zip",
                                module.description
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FolkDimens.Size16)
                ) {
                    Text(stringResource(R.string.repo_module_detail_download))
                }
            }
        }
    }

    // DownloadListener - navigate to install screen after download completes
    DownloadListener(context = context, onDownloaded = { uri ->
        navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
    })
}

private fun formatTimestamp(timestamp: Double): String {
    val instant = Instant.ofEpochSecond(timestamp.toLong())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
