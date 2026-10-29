package me.bmax.apatch.ui.screen.misc

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.ExpressiveDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDisclaimerDialog(showDialog: MutableState<Boolean>) {
    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Text(
                text = stringResource(id = R.string.settings_disclaimer),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )
            Text(
                text = stringResource(id = R.string.settings_disclaimer_message),
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}
