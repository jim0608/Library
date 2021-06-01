package com.library.lib_network.download

import android.content.Context
import android.net.Uri
import java.io.File

abstract class IDownloadBuild {
    open fun getFileName(): String? = null
    open fun getUri(contentType: String): Uri? = null
    open fun getDownloadFile(): File? = null
    abstract fun getContext(): Context //贪方便的话，返回Application就行
}
 
