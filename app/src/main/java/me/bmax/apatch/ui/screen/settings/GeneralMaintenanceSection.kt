package me.bmax.apatch.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedValueItem
import me.bmax.apatch.util.getBugreportFile

@Composable
fun GeneralMaintenanceSection(
    flat: Boolean,
    highlightKey: String?,
    logTitle: String,
    cleanStorageTitle: String,
    cleanStorageSummary: String,
    showCleanStorageDialog: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    SegmentedSection(title = stringResource(R.string.settings_section_general_maintenance)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        item(key = "general_send_log") {
            SegmentedNavigationItem(
                icon = Icons.Outlined.BugReport,
                title = logTitle,
                onClick = {
                    scope.launch {
                        val bugreport = loadingDialog.withLoading {
                            withContext(Dispatchers.IO) {
                                getBugreportFile(context)
                            }
                        }

                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                            bugreport
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = "application/gzip"
                            clipData = android.content.ClipData.newRawUri(null, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                context.getString(R.string.send_log)
                            )
                        )
                    }
                },
            )
        }

        item(key = "general_clean_storage") {
            SegmentedValueItem(
                icon = Icons.Outlined.CleaningServices,
                title = cleanStorageTitle,
                summary = cleanStorageSummary,
                onClick = { showCleanStorageDialog.value = true },
            )
        }

     }
    }
}
