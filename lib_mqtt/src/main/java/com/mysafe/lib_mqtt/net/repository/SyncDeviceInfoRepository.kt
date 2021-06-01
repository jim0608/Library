package com.mysafe.lib_mqtt.net.repository

import com.library.lib_network.retrofit.netObserve.BaseModel

import com.mysafe.lib_base.base.BaseDataEntity
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap
import com.mysafe.lib_base.util.deviceUtil.DeviceMacInfo
import com.mysafe.lib_mqtt.net.MqttNetworkResponse

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/25
 * @Describe:
 */
class SyncDeviceInfoRepository {
    private val TAG = "TAG_SyncDeviceInfoRepository"

    suspend fun syncDevice(clientId: String): BaseModel<BaseDataEntity?> {
        val response = MqttNetworkResponse()
        return response.syncDeviceInfo(getMap(clientId))

    }


    private fun getMap(clientId: String): Map<String, String> {
        val map = hashMapOf<String, String>()
        map["macAdress"] = DeviceMacInfo.getMac()
        map["deviceNumber"] = clientId
        map["applicationVersionName"] = BuildConfigImplWrap.getVersionName()
        map["applicationVersionCode"] = BuildConfigImplWrap.getVersionCode()
        map["schoolId"] = "926"
        return map
    }
}