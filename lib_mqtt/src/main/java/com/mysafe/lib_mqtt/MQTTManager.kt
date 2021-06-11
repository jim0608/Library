package com.mysafe.lib_mqtt

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap

import com.mysafe.lib_mqtt.entity.MQTTEnum
import com.mysafe.lib_mqtt.entity.MqttFunctionBean
import com.mysafe.lib_mqtt.entity.PublishEnum
import com.mysafe.lib_mqtt.receive.*
import org.eclipse.paho.client.mqttv3.*

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:Mqtt 管理器，对Mqtt的功能业务管理
 */
class MQTTManager {
    private val TAG = "TAG_MQTTManager"
    private val appType = "omdevice"
    private lateinit var mContext: Context
    private var mqttController: MQTTController? = null
    private var pck: String = ""
    private var clientId: String = ""
    private var platgroup: String = ""
    private var plattopic: String = ""
    private var mytopic: String = ""
    private var mygroup: String = ""
    private var receiveOrder = ReceiveOrder()

    /**
     * 初始化MQtt，
     */
    fun initMqtt(context: Context, platformId: String, clientId: String) {
        this.clientId = clientId
        this.platgroup = "xcy/$appType"
        this.plattopic = "xcy/$clientId"
        this.mytopic = "xcy/$platformId/$clientId"
        this.mygroup = "xcy/$platformId/$appType"
        mContext = context
        mqttController = MQTTController("$platformId/$appType/$clientId", iMqttActionListener)

        mqttController?.initMQTT(context, receiveCallBack)
        mqttController?.connectMQTT()

    }

    fun connect() {
        mqttController?.connectMQTT()
    }

    /**
     * 立遗嘱
     */
    fun setWill(publishTopic: String, msg: String) {
        mqttController?.setWill(publishTopic)
    }

    /**
     * 发布消息
     */
    fun publishMsg(enum: PublishEnum, msg: String) {
        when (enum) {
            PublishEnum.WILL -> {
                mqttController?.publish("", msg)
            }
            PublishEnum.STATE -> {

            }
            PublishEnum.FACE_USER_ID -> {
            }
            PublishEnum.FACE_MEAL_INFO -> {
            }
        }
    }

    /**
     * 断开mqtt连接
     */
    fun disconnectMqtt() {
        mqttController?.disconnect()
    }

    /**
     * 处理mqtt其他客户端发送过来的指令
     */
    fun receive(mqttMessage: MqttFunctionBean) {
        when (mqttMessage.Api) {
            "CleanFileCache" -> {//清除缓存
                receiveOrder.receive(CleanFileCache())
            }
            "DeviceRestart" -> {//设备重启
                receiveOrder.receive(DeviceRestart())
            }
            "SyncDevice" -> {//同步设备信息
                receiveOrder.receive(SyncDeviceInfo(clientId))
            }
            "SyncFaceData" -> {//同步人脸数据
                val staff = "com.mysafe.msmealorder_staff"
                val isStaff = BuildConfigImplWrap.getPackage().contentEquals(staff)
                if (!isStaff) {
                    receiveOrder.receive(SyncFaceData(mContext))
                }
            }
            "ApplicationUpdate" -> {//更新
                receiveOrder.receive(ApplicationUpdate(mContext))
            }
            "UploadLogs" -> {//上传日志
                receiveOrder.receive(UploadLogs())
            }
            "SyncCreateNewStudent" -> {//新增人脸
                receiveOrder.receive(AddFaceInfo(mContext, mqttMessage.MsgPara))
            }
            "SyncDeleteStudent" -> {//删除人脸
                receiveOrder.receive(DeleteFaceInfo(mContext, mqttMessage.MsgPara))
            }
        }
    }

    /**
     * 发送结果监听
     */
    private val iMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            Log.i(TAG, "onSuccess: ${asyncActionToken?.userContext}")
            if (asyncActionToken?.userContext == MQTTEnum.CONNECT) {
                mqttController?.subscribe(platgroup)
                mqttController?.subscribe(plattopic)
                mqttController?.subscribe(mytopic)
                mqttController?.subscribe(mygroup)
            }
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.i(TAG, "onFailure: $exception")
        }
    }

    /**
     * 接收消息监听
     */
    private val receiveCallBack = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            Log.i(TAG, "connectionLost: cause :${cause?.cause}")
            cause?.printStackTrace()
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.i(TAG, "messageArrived: \ntopic:$topic\nmessage:$message")
            val mqttMessage = Gson().fromJson(message.toString(), MqttFunctionBean::class.java)
            receive(mqttMessage)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.i(TAG, "deliveryComplete: ${token?.message}")
        }
    }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MQTTManager()
        }
    }
}