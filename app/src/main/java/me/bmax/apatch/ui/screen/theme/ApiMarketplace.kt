package me.bmax.apatch.ui.screen.theme

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.util.ui.showToast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkLoadingIndicator
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.model.ApiMarketplaceItem
import me.bmax.apatch.ui.viewmodel.ApiMarketplaceViewModel
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.component.folk.FolkStateTone
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Warning
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiMarketplaceScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = viewModel<ApiMarketplaceViewModel>()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf<ApiMarketplaceItem?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Fetch items on first load
    LaunchedEffect(Unit) {
        if (viewModel.items.isEmpty()) {
            viewModel.fetchMarketplaceItems()
        }
    }

    // Handle verification state changes
    LaunchedEffect(viewModel.verificationState) {
        when (val state = viewModel.verificationState) {
            is ApiMarketplaceViewModel.VerificationState.Success -> {
                showToast(
                    context,
                    context.getString(R.string.apm_api_apply_success)
                )
                navigator.popBackStack()
            }
            is ApiMarketplaceViewModel.VerificationState.Error -> {
                // Error is shown in the dialog
            }
            else -> {}
        }
    }

    // Preview Dialog
    if (showPreviewDialog && selectedItem != null) {
        ApiPreviewDialog(
            item = selectedItem!!,
            verificationState = viewModel.verificationState,
            onDismiss = {
                showPreviewDialog = false
                selectedItem = null
                viewModel.resetVerificationState()
            },
            onApply = { url ->
                viewModel.verifyAndApplyApi(url)
            }
        )
    }

    FolkScaffold(
        title = stringResource(R.string.apm_api_marketplace_title),
        titleStyle = FolkTitleStyle.Flexible,
        subtitle = stringResource(R.string.apm_api_marketplace_subtitle),
        onBack = { navigator.popBackStack() },
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    FolkLoadingIndicator(
                        text = stringResource(R.string.loading_apis)
                    )
                }
            }
            viewModel.errorMessage != null -> {
                FolkStateView(
                    title = viewModel.errorMessage ?: "Unknown error",
                    modifier = Modifier.padding(paddingValues),
                    icon = Icons.Outlined.Warning,
                    tone = FolkStateTone.Critical,
                    action = {
                        Button(onClick = { viewModel.retry() }) {
                            Text(stringResource(R.string.retry))
                        }
                    },
                )
            }
            viewModel.items.isEmpty() -> {
                FolkStateView(
                    title = stringResource(R.string.apm_api_empty),
                    modifier = Modifier.padding(paddingValues),
                    icon = Icons.Outlined.Api,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(FolkDimens.Size16),
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
                ) {
                    items(
                        items = viewModel.items,
                        key = { it.url }
                    ) { item ->
                        ApiMarketplaceItemCard(
                            item = item,
                            onPreview = {
                                selectedItem = item
                                showPreviewDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiMarketplaceItemCard(
    item: ApiMarketplaceItem,
    onPreview: () -> Unit
) {
    // Same styling as ScriptLibrary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FolkShape.Corner20,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = FolkDimens.Size1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FolkDimens.Size16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(FolkDimens.Size4))
                Text(
                    text = item.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(FolkDimens.Size12))

            FilledTonalButton(
                onClick = onPreview,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.height(FolkDimens.Size36),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(FolkDimens.Size18)
                )
                Spacer(modifier = Modifier.width(FolkDimens.Size8))
                Text(stringResource(R.string.apm_api_preview))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiPreviewDialog(
    item: ApiMarketplaceItem,
    verificationState: ApiMarketplaceViewModel.VerificationState,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var imageLoadState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }
    // Add cache-busting parameter to ensure fresh API request each time
    val cacheBuster = remember { System.currentTimeMillis() }
    val imageUrlWithCacheBuster = remember(item.url, cacheBuster) {
        val separator = if (item.url.contains("?")) "&" else "?"
        "${item.url}${separator}t=$cacheBuster"
    }

    // Same styling as ScriptLibrary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Description
                Text(
                    text = item.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(FolkDimens.Size8))

                // URL
                Text(
                    text = stringResource(R.string.apm_api_url_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(FolkDimens.Size16))

                // API Preview Image
                Text(
                    text = stringResource(R.string.apm_api_preview_title),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(FolkDimens.Size8))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FolkDimens.Size150)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrlWithCacheBuster,
                        contentDescription = stringResource(R.string.apm_api_preview_title),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state -> imageLoadState = state }
                    )

                    // Loading state
                    if (imageLoadState is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(FolkDimens.Size32)
                        )
                    }

                    // Error state
                    if (imageLoadState is AsyncImagePainter.State.Error) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(FolkDimens.Size32)
                            )
                            Spacer(modifier = Modifier.height(FolkDimens.Size4))
                            Text(
                                text = stringResource(R.string.apm_api_preview_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Verification error message
                if (verificationState is ApiMarketplaceViewModel.VerificationState.Error) {
                    Spacer(modifier = Modifier.height(FolkDimens.Size12))
                    Text(
                        text = verificationState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            when (verificationState) {
                is ApiMarketplaceViewModel.VerificationState.Loading -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(FolkDimens.Size16),
                            strokeWidth = FolkDimens.Size2
                        )
                        Spacer(modifier = Modifier.width(FolkDimens.Size8))
                        Text(stringResource(R.string.apm_api_verifying))
                    }
                }
                is ApiMarketplaceViewModel.VerificationState.Error -> {
                    FilledTonalButton(
                        onClick = { onApply(item.url) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.apm_api_retry))
                    }
                }
                is ApiMarketplaceViewModel.VerificationState.Success -> {
                    FilledTonalButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(FolkDimens.Size4))
                        Text(stringResource(R.string.apm_api_apply_success))
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = { onApply(item.url) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(stringResource(R.string.apm_api_apply))
                    }
                }
            }
        },
        dismissButton = {
            if (verificationState !is ApiMarketplaceViewModel.VerificationState.Success) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    )
}
