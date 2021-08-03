package com.mysafe.lib_mqtt.receive

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.library.lib_network.download.DownLoadRepository
import com.library.lib_network.download.DownloadBuild
import com.library.lib_network.download.DownloadStatus
import com.mysafe.lib_base.base.VersionConfigBean
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap
import com.mysafe.lib_mqtt.net.repository.AppUpdateRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:更新应用
 */
class ApplicationUpdateReceive(private val mContext: Context) : ExecutiveOrder {
    private val TAG = "TAG_ApplicationUpdate"
    override fun work() {
        val repository = AppUpdateRepository()
        GlobalScope.launch {
            repository.checkVersion().run {
                if (isSuccess) {
                    updateApp(data)
                } else {
                    Toast.makeText(mContext, "暂无更新", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun updateApp(data: VersionConfigBean?) {
        if (data == null) {
            return
        }
        val netCode = data.version_Code
        val appCode = BuildConfigImplWrap.getVersionCode()
        val appName = BuildConfigImplWrap.getVersionName()
        if (netCode > appCode) {
            Log.i(TAG, "updateApp: 开始更新")
            downloadApk(data.version_DownloadUrl,appCode,appName)
        } else {
            Log.i(TAG, "updateApp: 无需更新")
        }
    }


    private suspend fun downloadApk(url: String, appCode: String, appName: String) {
        val fileName = "Public_${appCode}_$appName"
        val downloadBuild = DownloadBuild(mContext, fileName)
        val downloadRepository = DownLoadRepository()
        downloadRepository.downLoadFile(url, downloadBuild) {
            when (it) {
                is DownloadStatus.Error -> {
                    Log.i(TAG, "Error: ${it.t}")
//                    installLive.postValue("e:" + it.t)
                }
                is DownloadStatus.Success -> {
                    Log.i(TAG, "Success: ${it.uri}")
//                    installLive.postValue("更新完成，开始安装")
                    val uri = it.uri.path
//                    installLive.postValue(uri)
                }
                is DownloadStatus.Process -> {
                    val process = it.process*100
                    val percent = Formatter().format("%.2f", process).toString()
//                    installLive.postValue("更新中：$percent%")
                    Log.i(
                        TAG, "Process: " +
                                "length:${it.length} " +
                                "currentLength:${it.currentLength} " +
                                "process:${it.process}" +
                                "percent:$percent"
                    )
                }
            }
        }
    }
}