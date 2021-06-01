package com.mysafe.lib_mqtt.receive

import android.util.Log
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap
import com.mysafe.lib_base.util.deviceUtil.DeviceControllerUtil

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:清除缓存
 */
class CleanFileCache():ExecutiveOrder{
    private val TAG = "TAG_CleanFileCache"
    override fun work() {
        Log.i(TAG, "work: ")
        val packageName = BuildConfigImplWrap.getPackage()
        DeviceControllerUtil.clearAppData(packageName)
    }
}