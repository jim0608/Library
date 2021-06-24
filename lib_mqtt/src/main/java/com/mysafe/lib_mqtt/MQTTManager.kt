package com.mysafe.lib_mqtt

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mysafe.lib_base.router_service.build_config.BuildConfigImplWrap
import com.mysafe.lib_base.router_service.sync_data.SyncUserMealImplWrap

import com.mysafe.lib_mqtt.entity.MQTTEnum
import com.mysafe.lib_mqtt.entity.MqttFunctionBean
import com.mysafe.lib_mqtt.receive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import java.lang.Exception

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
    private var mqttBean: MqttFunctionBean? = null
    private var publishResult: IPublishResult? = null
    private var scope: Job? = null
    private var pck: String = ""
    private var clientId: String = ""
    private var remoteTopic: String = ""
    private var platformId: String = ""
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
        this.platformId = platformId
        this.platgroup = "xcy/$appType"
        this.plattopic = "xcy/$clientId"
        this.mytopic = "xcy/$platformId/$clientId"
        this.mygroup = "xcy/$platformId/$appType"
        mContext = context
        scope = GlobalScope.launch(Dispatchers.IO) {
            mqttController = MQTTController("$platformId/$appType/$clientId", iMqttActionListener)
            mqttController?.initMQTT(context, receiveCallBack)
            mqttController?.connectMQTT()
        }

    }

    fun setPublishResultListener(publishResult: IPublishResult) {
        if (remoteTopic.isEmpty()) {
            throw Exception("haven't remoteTopic")
        }
        this.publishResult = publishResult
    }

    fun setRemoteTopic(remoteClientId: String) {
        this.remoteTopic = "xcy/$platformId/$remoteClientId"
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
    fun publishMsg(apiType: String, content: String) {
        if (remoteTopic.isEmpty()) {
            return
        }
        if (mqttBean == null) {
            mqttBean = MqttFunctionBean()
            mqttBean?.let {
                it.Pid = clientId
                it.AppType = "AndroidPhone"
            }
        }
        mqttBean?.Api = apiType
        mqttBean?.MsgPara = "$apiType$content"
        val msg = Gson().toJson(mqttBean)
        mqttController?.publish(remoteTopic, msg)
    }

    /**
     * 断开mqtt连接
     */
    fun disconnectMqtt() {
        mqttController?.disconnect()
        scope?.cancel()
    }

    /**
     * 处理mqtt其他客户端发送过来的指令
     */
    fun receive(mqttMessage: MqttFunctionBean) {
        Log.i(TAG, "receive: as")
        when (mqttMessage.Api) {
            "CleanFileCache" -> {//清除缓存
                receiveOrder.receive(CleanFileCacheReceive())
            }
            "DeviceRestart" -> {//设备重启
                receiveOrder.receive(DeviceRestartReceive())
            }
            "SyncDevice" -> {//同步设备信息
                receiveOrder.receive(SyncDeviceInfoReceive(clientId))
            }
            "SyncFaceData" -> {//同步人脸数据
                val staff = "com.mysafe.msmealorder_staff"
                val isStaff = BuildConfigImplWrap.getPackage().contentEquals(staff)
                if (!isStaff) {
                    receiveOrder.receive(SyncFaceDataReceive(mContext))
                }
            }
            "ApplicationUpdate" -> {//更新
                receiveOrder.receive(ApplicationUpdateReceive(mContext))
            }
            "UploadLogs" -> {//上传日志
                receiveOrder.receive(UploadLogsReceive())
            }
            "SyncCreateNewStudent" -> {//新增人脸
                receiveOrder.receive(AddFaceInfoReceive(mContext, mqttMessage.MsgPara))
            }
            "SyncDeleteStudent" -> {//删除人脸
                receiveOrder.receive(DeleteFaceInfoReceive(mContext, mqttMessage.MsgPara))
            }
            "TAGCustomFaceId" -> {
                var str = mqttMessage.MsgPara
                str = str.replace("TAGCustomFaceId", "")
                SyncUserMealImplWrap.fromCustomer(str)
            }
            "TAGStaffMealsContent" -> {
                var str = mqttMessage.MsgPara
                str = str.replace("TAGStaffMealsContent", "")
                SyncUserMealImplWrap.fromStaff(str)
            }
            "TAGStaffMealsFinish" -> {
                var str = mqttMessage.MsgPara
                str = str.replace("TAGStaffMealsFinish", "")
                SyncUserMealImplWrap.fromStaff(str)
            }
        }
    }

    /**
     * 发送结果监听
     */
    private val iMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            Log.i(TAG, "onSuccess: ${asyncActionToken?.userContext}")
            when (asyncActionToken?.userContext) {
                MQTTEnum.CONNECT -> {
                    mqttController?.subscribe(platgroup)
                    mqttController?.subscribe(plattopic)
                    mqttController?.subscribe(mytopic)
                    mqttController?.subscribe(mygroup)
                }
                MQTTEnum.PUBLISH -> {
                    publishResult?.result(mqttBean?.Api ?: "")
                }
            }

        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.i(TAG, "onFailure: $exception")
        }
    }

    /**
     * 接收消息监听
     */
    private val receiveCallBack = object : MqttCallbackExtended {
        override fun connectionLost(cause: Throwable?) {
            Log.i(TAG, "connectionLost: cause :${cause?.cause}")
            cause?.printStackTrace()
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.i(TAG, "messageArrived: \ntopic:$topic\nmessage:$message")
            try {
                val mqttMessage = Gson().fromJson(message.toString(), MqttFunctionBean::class.java)
                receive(mqttMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {

            Log.i(TAG, "deliveryComplete: ${token?.message}")
        }

        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.i(TAG, "connectComplete: $reconnect $serverURI")
            if (reconnect) {
                mqttController?.subscribe(platgroup)
                mqttController?.subscribe(plattopic)
                mqttController?.subscribe(mytopic)
                mqttController?.subscribe(mygroup)
            }
        }
    }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MQTTManager()
        }
    }
}