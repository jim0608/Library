package com.mysafe.lib_mqtt.entity

import com.google.gson.Gson

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/18
 * @Describe:
 */
class MqttFunctionBean {
    private val TAG = "TAG_MqttFunctionBean"
    var Api:String = ""
    var MsgPara:String = ""
    var Pid:String = ""
    var AppType:String = ""

    override fun toString(): String {
        Gson()
        return super.toString()
    }
}