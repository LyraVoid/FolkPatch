package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPMControlDialog(showDialog: MutableState<Boolean>, onConfirm: (String) -> Unit) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
        blurBehind = false,
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Box(
                Modifier
                    .padding(PaddingValues(bottom = FolkDimens.Size16))
                    .align(Alignment.Start)
            ) {
                Text(
                    text = stringResource(id = R.string.kpm_control_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Box(
                Modifier
                    .weight(weight = 1f, fill = false)
                    .align(Alignment.Start)
            ) {
                Text(
                    text = stringResource(id = R.string.kpm_control_dialog_content),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                contentAlignment = Alignment.CenterEnd,
            ) {
                OutlinedTextField(
                    value = controlParam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size6),
                    onValueChange = {
                        controlParam = it
                        enable = controlParam.isNotBlank()
                    },
                    shape = FolkShape.CornerFull,
                    label = { Text(stringResource(id = R.string.kpm_control_paramters)) },
                    visualTransformation = VisualTransformation.None,
                )
            }

            Spacer(modifier = Modifier.height(FolkDimens.Size12))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }

                Button(onClick = {
                    showDialog.value = false

                    // Run the control on the caller's scope: this dialog
                    // leaves composition here, cancelling any scope it owns.
                    onConfirm(controlParam)

                }, enabled = enable, colors = ButtonDefaults.buttonColors()) {
                    Text(stringResource(id = android.R.string.ok))
                }
            }
        }
    }
}

