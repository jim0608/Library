package com.mysafe.lib_mqtt.receive

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:策略模式：根据获取到的不同的工作分发
 */
interface ExecutiveOrder {
    fun work()
}