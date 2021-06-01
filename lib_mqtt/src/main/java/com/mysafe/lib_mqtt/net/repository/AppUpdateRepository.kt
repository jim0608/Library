package com.mysafe.lib_mqtt.net.repository

import com.library.lib_network.retrofit.netObserve.BaseModel
import com.mysafe.lib_base.base.VersionConfigBean
import com.mysafe.lib_mqtt.net.MqttNetworkResponse

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/27
 * @Describe:
 */
class AppUpdateRepository {
    private val TAG = "TAG_AppUpdateRepository"

    suspend fun checkVersion(): BaseModel<VersionConfigBean?> {
        val response = MqttNetworkResponse()
        return response.checkVersion()
    }
}