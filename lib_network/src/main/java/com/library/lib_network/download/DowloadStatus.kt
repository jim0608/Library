package com.library.lib_network.download

import android.net.Uri

sealed class DownloadStatus {
    data class Process(val currentLength: Long,
                       val length: Long,
                       val process: Float) : DownloadStatus()
    class Error(val t: Throwable) : DownloadStatus()
    class Success(val uri: Uri) : DownloadStatus()
}