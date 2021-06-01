package com.library.lib_network.download

import android.net.Uri
import java.io.File
import java.io.OutputStream

class DownloadInfo(val ops: OutputStream?,
                   val file: File? = null,
                   val uri: Uri? = null)