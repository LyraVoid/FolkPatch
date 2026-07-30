package me.bmax.apatch.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.ModuleLabel
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.viewmodel.RepoModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module.name) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Module basic info
            item(key = "info") {
                ExpressiveCard(flat = true) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Label row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModuleLabel(
                                text = "APM",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            ModuleLabel(
                                text = module.version,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
                    SplicedColumnGroup(title = stringResource(R.string.repo_module_detail_links)) {
                        item(key = "homepage", visible = module.homepage.isNotEmpty()) {
                            ExpressiveCard(flat = true, onClick = { openUrl(context, module.homepage) }) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Language, contentDescription = null)
                                    Text(stringResource(R.string.repo_module_detail_homepage))
                                }
                            }
                        }
                        item(key = "source", visible = module.source.isNotEmpty()) {
                            ExpressiveCard(flat = true, onClick = { openUrl(context, module.source) }) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Code, contentDescription = null)
                                    Text(stringResource(R.string.repo_module_detail_source))
                                }
                            }
                        }
                        item(key = "support", visible = module.support.isNotEmpty()) {
                            ExpressiveCard(flat = true, onClick = { openUrl(context, module.support) }) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.HelpOutline, contentDescription = null)
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
                    SplicedColumnGroup(title = stringResource(R.string.repo_module_detail_versions)) {
                        module.versions.forEach { ver ->
                            item(key = ver.versionCode) {
                                ExpressiveCard(
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
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                            Icon(Icons.Filled.Download, contentDescription = "Download")
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
                        .padding(horizontal = 16.dp)
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
