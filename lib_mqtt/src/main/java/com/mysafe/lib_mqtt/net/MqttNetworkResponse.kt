package com.mysafe.lib_mqtt.net

import com.library.lib_network.PublicRequestNetwork
import com.library.lib_network.retrofit.ServiceCreate
import com.library.lib_network.retrofit.netObserve.netResult

/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
class MqttNetworkResponse : PublicRequestNetwork() {
    private var service = ServiceCreate.create(SyncDeviceInfoImpl::class.java)

    /**
     * 同步设备信息
     */
    @Suppress("MISSING_DEPENDENCY_CLASS")
    suspend fun syncDeviceInfo(map: Map<String, String>) =
        netResult { service.uploadDevicesInfo(map).await() }

    /**
     * 检查更新
     */
    suspend fun checkVersion() =
        netResult { service.checkVersion().await() }

    /**
     * 更新人脸数据
     */
    suspend fun upDateSqlFace(body: MutableMap<String, Any>) =
        netResult { service.updateFaceData(body).await() }

    /**
     * 根据学生ID获取学生人脸信息
     */
    suspend fun getFaceById(body: MutableMap<String, Any>) =
        netResult { service.getFaceById(body).await() }
}