package com.mysafe.lib_mqtt.receive

import com.mysafe.lib_base.util.deviceUtil.DeviceControllerUtil


/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:设备重启
 */
class DeviceRestart : ExecutiveOrder {
    private val TAG = "TAG_DeviceRestart"
    override fun work() {
        DeviceControllerUtil.restartDevice()
    }
}