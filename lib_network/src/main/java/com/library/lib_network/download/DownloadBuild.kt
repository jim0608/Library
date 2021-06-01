package com.library.lib_network.download

import android.content.Context

class DownloadBuild(private val cxt: Context, private val fileName:String): IDownloadBuild(){
    override fun getContext(): Context = cxt
    override fun getFileName(): String = fileName
}