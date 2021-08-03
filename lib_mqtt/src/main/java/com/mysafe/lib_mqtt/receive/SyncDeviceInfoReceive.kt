package com.mysafe.lib_mqtt.receive

import android.util.Log

import com.mysafe.lib_mqtt.net.repository.SyncDeviceInfoRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:同步设备信息
 */
class SyncDeviceInfoReceive(private  val clientId: String) : ExecutiveOrder {
    private val TAG = "TAG_SyncDeviceInfo"
    override fun work() {
        GlobalScope.launch {
            val repository = SyncDeviceInfoRepository()
            val result = repository.syncDevice(clientId)
            if (result.isSuccess){
                Log.i(TAG, "work: 同步成功")
            }else{
                Log.i(TAG, "work: 同步失败")
            }
        }
    }
}