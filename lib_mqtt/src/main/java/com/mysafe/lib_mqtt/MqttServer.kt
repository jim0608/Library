package com.mysafe.lib_mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tencent.mmkv.MMKV
import org.eclipse.paho.client.mqttv3.internal.wire.MqttReceivedMessage

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:
 */
class MqttServer: Service() {
    private val TAG = "TAG_MqttServer"
    private val mmkv = MMKV.defaultMMKV()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val clientId = mmkv.decodeString("deviceNo")
//        MQTTManager.instance.initMqtt(this, "100100", clientId)
        Log.i(TAG, "onStartCommand: ")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: ")
        MQTTManager.instance.disconnectMqtt()
    }
}