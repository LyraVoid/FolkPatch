package me.bmax.apatch.ui.screen.superuser

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.component.folk.folkOverlayColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onGrantRoot: () -> Unit,
    onRevokeRoot: () -> Unit,
    onExclude: () -> Unit,
) {
    // The bar sits on primaryContainer, where M3's default disabled alpha turns
    // the three action icons into a grey smudge. Lift just the disabled tint so
    // the icons stay readable while the enabled state is still clearly darker.
    val actionColors = IconButtonDefaults.iconButtonColors(
        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
    )
    TopAppBar(
        title = {
            Text(stringResource(R.string.su_multi_select_count, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                )
            }
        },
        actions = {
            IconButton(
                onClick = onGrantRoot,
                enabled = selectedCount > 0,
                colors = actionColors,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = stringResource(R.string.su_multi_select_grant_root),
                )
            }
            IconButton(
                onClick = onRevokeRoot,
                enabled = selectedCount > 0,
                colors = actionColors,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = stringResource(R.string.su_multi_select_revoke_root),
                )
            }
            IconButton(
                onClick = onExclude,
                enabled = selectedCount > 0,
                colors = actionColors,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = stringResource(R.string.su_multi_select_exclude),
                )
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

// ── M3E App Item ──────────────────────────────────────────────────────────

@Composable
fun LabelText(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Surface(
        modifier = Modifier.padding(end = FolkDimens.Size4),
        shape = RoundedCornerShape(FolkDimens.Size4),
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = FolkDimens.Size6, vertical = FolkDimens.Size1),
            style = MaterialTheme.typography.labelSmall,
            color = contentColorFor(containerColor),
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Options Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperUserOptionsSheet(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSystemApps: () -> Unit,
    showSystemApps: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = folkOverlayColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FolkDimens.Size16)
                .padding(bottom = FolkDimens.Size32),
        ) {
            Text(
                text = stringResource(R.string.su_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = FolkDimens.Size16),
            )

            // Refresh
            Surface(
                onClick = onRefresh,
                shape = FolkShape.Corner12,
                color = FolkColors.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_refresh),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Show/Hide System Apps
            Surface(
                onClick = onToggleSystemApps,
                shape = FolkShape.Corner12,
                color = FolkColors.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showSystemApps) {
                            stringResource(R.string.su_hide_system_apps)
                        } else {
                            stringResource(R.string.su_show_system_apps)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Backup
            Surface(
                onClick = onBackup,
                shape = FolkShape.Corner12,
                color = FolkColors.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_backup_list),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            // Restore
            Surface(
                onClick = onRestore,
                shape = FolkShape.Corner12,
                color = FolkColors.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolkDimens.Size12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.su_restore_list),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
