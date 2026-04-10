package me.bmax.apatch.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import me.bmax.apatch.ui.component.AppLoadingIndicator
import me.bmax.apatch.ui.model.ApiMarketplaceItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.viewmodel.ApiMarketplaceViewModel

@Destination<RootGraph>
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
                Toast.makeText(
                    context,
                    context.getString(R.string.apm_api_apply_success),
                    Toast.LENGTH_SHORT
                ).show()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apm_api_marketplace_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    AppLoadingIndicator()
                }
            }
            viewModel.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text(stringResource(R.string.apm_api_retry))
                    }
                }
            }
            viewModel.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.apm_api_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.2f)
    } else {
        1f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.getLocalizedDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            val buttonOpacity = (opacity + 0.3f).coerceAtMost(1f)

            FilledTonalButton(
                onClick = onPreview,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = buttonOpacity)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.2f)
    } else {
        1f
    }

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

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                // API Preview Image
                Text(
                    text = stringResource(R.string.apm_api_preview_title),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
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
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Error state
                    if (imageLoadState is AsyncImagePainter.State.Error) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = verificationState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            val buttonOpacity = (opacity + 0.3f).coerceAtMost(1f)

            when (verificationState) {
                is ApiMarketplaceViewModel.VerificationState.Loading -> {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = buttonOpacity)
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.apm_api_verifying))
                    }
                }
                is ApiMarketplaceViewModel.VerificationState.Error -> {
                    FilledTonalButton(
                        onClick = { onApply(item.url) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = buttonOpacity)
                        )
                    ) {
                        Text(stringResource(R.string.apm_api_retry))
                    }
                }
                is ApiMarketplaceViewModel.VerificationState.Success -> {
                    FilledTonalButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = buttonOpacity)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.apm_api_apply_success))
                    }
                }
                else -> {
                    FilledTonalButton(
                        onClick = { onApply(item.url) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = buttonOpacity)
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
