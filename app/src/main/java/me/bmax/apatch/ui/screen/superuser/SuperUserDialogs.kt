package me.bmax.apatch.ui.screen.superuser

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.ui.viewmodel.SuperUserViewModel
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils.Companion.setupWindowBlurListener
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchExcludeDialog(
    onDismiss: () -> Unit,
    onExclude: () -> Unit,
    onReverseExclude: () -> Unit
) {
    val title = stringResource(R.string.su_batch_exclude_title)
    val content = stringResource(R.string.su_batch_exclude_content)
    val excludeText = stringResource(R.string.su_exclude_btn)
    val reverseText = stringResource(R.string.su_exclude_reverse_btn)
    val cancelText = stringResource(android.R.string.cancel)

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        width = FolkDimens.Size320,
        shape = FolkShape.Corner20,
        dialogProperties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOff, dismissOnClickOutside = false),
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Box(
                Modifier
                    .padding(PaddingValues(bottom = FolkDimens.Size16))
                    .align(Alignment.Start)
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
            }
            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(PaddingValues(bottom = FolkDimens.Size24))
                    .align(Alignment.Start)
            ) {
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = cancelText)
                }
                TextButton(onClick = onExclude) {
                    Text(text = excludeText)
                }
                TextButton(onClick = onReverseExclude) {
                    Text(text = reverseText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchActionConfirmDialog(
    action: SuperUserViewModel.BatchAction,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.su_multi_select_title)
    val content = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_confirm_grant, count)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_confirm_revoke, count)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_confirm_exclude, count)
    }
    val confirmText = when (action) {
        SuperUserViewModel.BatchAction.GRANT_ROOT -> stringResource(R.string.su_multi_select_grant_root)
        SuperUserViewModel.BatchAction.REVOKE_ROOT -> stringResource(R.string.su_multi_select_revoke_root)
        SuperUserViewModel.BatchAction.EXCLUDE -> stringResource(R.string.su_multi_select_exclude)
    }
    val cancelText = stringResource(android.R.string.cancel)

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        width = FolkDimens.Size320,
        shape = FolkShape.Corner20,
        dialogProperties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOff, dismissOnClickOutside = false),
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Box(
                Modifier
                    .padding(PaddingValues(bottom = FolkDimens.Size16))
                    .align(Alignment.Start)
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
            }
            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(PaddingValues(bottom = FolkDimens.Size24))
                    .align(Alignment.Start)
            ) {
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = cancelText)
                }
                TextButton(onClick = onConfirm) {
                    Text(text = confirmText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionDialog(
    app: SuperUserViewModel.AppInfo,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onForceStop: () -> Unit
) {
    val title = stringResource(R.string.su_app_action_title)
    val content = stringResource(R.string.su_app_action_content)
    val launchText = stringResource(R.string.su_app_action_launch)
    val forceStopText = stringResource(R.string.su_app_action_force_stop)
    val cancelText = stringResource(android.R.string.cancel)

    ExpressiveDialog(
        onDismissRequest = onDismiss,
        width = FolkDimens.Size320,
        shape = FolkShape.Corner20,
        dialogProperties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOff, dismissOnClickOutside = true),
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Box(
                Modifier
                    .padding(PaddingValues(bottom = FolkDimens.Size16))
                    .align(Alignment.Start)
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
            }
            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(PaddingValues(bottom = FolkDimens.Size24))
                    .align(Alignment.Start)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = cancelText)
                }
                TextButton(onClick = onLaunch) {
                    Text(text = launchText)
                }
                TextButton(onClick = onForceStop) {
                    Text(text = forceStopText)
                }
            }
        }
    }
}
