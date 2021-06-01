package com.mysafe.lib_mqtt.receive

import android.util.Log
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:上传日志
 */
class UploadLogs :ExecutiveOrder{
    private val TAG = "TAG_UploadLogs"
    override fun work() {
        val packageName = BuildConfigImplWrap.getPackage()
        Log.i(TAG, "receive: $packageName")
    }
}