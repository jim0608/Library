package com.library.lib_network.download

import com.library.lib_network.PublicRequestNetwork
import com.library.lib_network.retrofit.ServiceCreate
import com.library.lib_network.retrofit.interceptor.LogHttpInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/28
 * @Describe:
 */
class DownLoadRepository : PublicRequestNetwork() {
    private val TAG = "TAG_RetrofitUpDownFile"

    suspend fun downLoadFile(
        url: String,
        downloadBuild: DownloadBuild,
    ): Flow<DownloadStatus> {
        val retrofit = ServiceCreate.create(RetrofitUpDownLoadService::class.java)
        ServiceCreate.setInterceptorLevel(LogHttpInterceptor.Level.NONE)
        val response = retrofit.downLoadFile(url)
        val downloadUtil = DownloadFileUtil()
        return downloadUtil.download(downloadBuild, response)
    }

    suspend fun downLoadFile(
        url: String,
        downloadBuild: DownloadBuild,
        func: (it: DownloadStatus) -> Unit
    ) {
        val retrofit = ServiceCreate.create(RetrofitUpDownLoadService::class.java)
        ServiceCreate.setInterceptorLevel(LogHttpInterceptor.Level.NONE)
        val response = retrofit.downLoadFile(url)
        val downloadUtil = DownloadFileUtil()
        downloadUtil.download(downloadBuild, response).collect {
            func(it)
        }
    }


}