package me.bmax.apatch.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import me.bmax.apatch.apApp
import androidx.core.content.ContextCompat
import java.io.File

fun getSafeDownloadsDir(context: Context): File {
    return try {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    } catch (_: SecurityException) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download")
    }
}

@SuppressLint("Range")
fun download(
    context: Context,
    url: String,
    fileName: String,
    description: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {}
) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val query = DownloadManager.Query()
    query.setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_PENDING)
    downloadManager.query(query).use { cursor ->
        while (cursor.moveToNext()) {
            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))
            val localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val columnTitle = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE))
            if (url == uri || fileName == columnTitle) {
                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                    onDownloading()
                    return
                } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    onDownloaded(Uri.parse(localUri))
                    return
                }
            }
        }
    }

    val request = DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setMimeType("application/zip").setTitle(fileName).setDescription(description)

    try {
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS, fileName
        )
    } catch (_: SecurityException) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (dir != null) {
            request.setDestinationUri(Uri.fromFile(java.io.File(dir, fileName)))
        } else {
            request.setDestinationInExternalFilesDir(
                context, Environment.DIRECTORY_DOWNLOADS, fileName
            )
        }
    }

    downloadManager.enqueue(request)
}

@Composable
fun DownloadListener(context: Context, onDownloaded: (Uri) -> Unit) {
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("Range")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val id = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, -1
                    )
                    val query = DownloadManager.Query().setFilterById(id)
                    val downloadManager =
                        context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        )
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uri = cursor.getString(
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            )
                            onDownloaded(Uri.parse(uri))
                        }
                    }
                }
            }
        }
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            context,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}
