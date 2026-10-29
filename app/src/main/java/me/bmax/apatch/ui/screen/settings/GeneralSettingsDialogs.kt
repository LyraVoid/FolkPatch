package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.util.*
import me.bmax.apatch.ui.screen.settings.general.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelinuxHideWarningDialog(
    showDialog: MutableState<Boolean>,
    kernelVersion: Int?,
    isGki: Boolean,
    onConfirm: () -> Unit,
) {
    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
        blurBehind = false,
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Text(
                text = stringResource(id = R.string.settings_selinux_hide_warning_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16),
            )
            if ((kernelVersion ?: 0) < 510) {
                Text(
                    text = stringResource(id = R.string.settings_selinux_hide_warning_below_5_10),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = FolkDimens.Size8),
                )
            }
            if (!isGki) {
                Text(
                    text = stringResource(id = R.string.settings_selinux_hide_warning_non_gki),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                Spacer(Modifier.width(FolkDimens.Size8))
                Button(onClick = {
                    showDialog.value = false
                    onConfirm()
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}
