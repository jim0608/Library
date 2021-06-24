package com.mysafe.lib_mqtt.receive

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:处理mqtt其他客户端发送过来的指令
 */
class ReceiveOrder {
    private val TAG = "TAG_ReceiveOrder"
    /**
     * 消息获取
     */
    fun receive(obtainDeviceInfo: ExecutiveOrder) {
        obtainDeviceInfo.work()
    }
}