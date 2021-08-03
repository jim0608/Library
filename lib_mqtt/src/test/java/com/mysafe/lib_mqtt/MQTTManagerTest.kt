package com.mysafe.lib_mqtt

import com.google.gson.Gson
import com.mysafe.lib_mqtt.entity.MqttFunctionBean
import org.junit.Test

import org.junit.Assert.*

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/2
 * @Describe:
 */
class MQTTManagerTest {

    @Test
    fun receive() {
        val mqtt = MQTTManager()
//        val message = "{\"Api\":\"SyncDeleteStudent\",\"MsgPara\":\"\\\"[12,123,12]\\\"\",\"Pid\":\"subserver100100\",\"AppType\":\"nutriweb\"}"
        val message = "{\"Api\":\"SyncMealAbout\",\"MsgPara\":\"{\\\"faceId\\\":\\\"10234\\\"}\",\"Pid\":\"bd935ae5bf219897241078f4c71c3c1a0110_1\",\"AppType\":\"AndroidPhone\"}"
            //        val message = "{\"Api\":\"SyncMealAbout\",\"MsgPara\":\"{\"faceId\":\"10234\"}\",\"Pid\":\"bd935ae5bf219897241078f4c71c3c1a0110_1\",\"AppType\":\"AndroidPhone\"}"

        val mqttMessage = Gson().fromJson(message, MqttFunctionBean::class.java)
        print(mqttMessage.MsgPara)
    }
}