package com.mysafe.lib_mqtt

import android.content.Context
import android.util.Log
import com.mysafe.lib_mqtt.entity.MQTTEnum
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException


/**
 * @author Create By 张晋铭
 * @Date on 2021/4/9
 * @Describe:Mqtt 控制器 mqtt的初始化，连接、订阅、发布、立遗嘱等
 */
class MQTTController(
    clientId: String,
    iMqttActionListener: IMqttActionListener,
    url: String? = "ssl://tcloudmq.cancanan.cn:8222",
//    url: String? = "tcp://mqttt.cancanan.cn:8222",
//    url: String? = "tcp://192.168.0.59:1883",
    userName: String? = "admin",
    password: String? = "password",
    isCleanSession: Boolean? = true,
    connectionTimeout: Int? = 10,
    keepAliveInterval: Int? = 20
) {
    private val TAG = "TAG_MQTTController"
    private var mMQTTAndroidClient: MqttAndroidClient? = null
    private var mMQTTConnectOptions: MqttConnectOptions? = null
    private var iMqttActionListener = iMqttActionListener
    private val mUrl = url
    private val mUserName = userName
    private val mPassword = password
    private val mClientId = clientId
    private val cleanSession = isCleanSession ?: false
    private val mConnectTimeout = connectionTimeout ?: 10
    private val mKeepAliveInterval = keepAliveInterval ?: 20

    /**
     * init
     */
    fun initMQTT(context: Context, callback: MqttCallback) {
        val caCrtFileI = context.resources.openRawResource(R.raw.cancanan)
        mMQTTAndroidClient = MqttAndroidClient(context, mUrl, mClientId)
        mMQTTAndroidClient?.setCallback(callback)
        mMQTTConnectOptions = MqttConnectOptions()
        mMQTTConnectOptions?.apply {
            userName = mUserName
            password = mPassword?.toCharArray()

            // 设置超时时间，单位：秒
            connectionTimeout = mConnectTimeout
            // 心跳包发送间隔，单位：秒
            keepAliveInterval = mKeepAliveInterval
            //tls验证
            if (mUrl != null) {
                if (mUrl.contains("ssl")) {
                    val socketFactoryOptions = MySocketFactory.SocketFactoryOptions()
                    try {
                        socketFactoryOptions.withCaInputStream(caCrtFileI)
                        mMQTTConnectOptions?.socketFactory = MySocketFactory(socketFactoryOptions)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: NoSuchAlgorithmException) {
                        e.printStackTrace()
                    } catch (e: KeyStoreException) {
                        e.printStackTrace()
                    } catch (e: CertificateException) {
                        e.printStackTrace()
                    } catch (e: KeyManagementException) {
                        e.printStackTrace()
                    } catch (e: UnrecoverableKeyException) {
                        e.printStackTrace()
                    }
                }
            }
//            sslProperties  = getSingleSocketFactory(caCrtFileI)
            //是否自动重连
            isAutomaticReconnect = true
            // 清除缓存
            isCleanSession = true
        }
    }


    /**
     * connect
     * @param connectTag 连接mqtt成功或失败，监听的userContext
     */
    fun connectMQTT(connectTag: MQTTEnum? = MQTTEnum.CONNECT) {
        try {
            mMQTTAndroidClient?.connect(mMQTTConnectOptions, connectTag, iMqttActionListener)
        } catch (e: Exception) {
            Log.i(TAG, "Exception Occured", e)
            iMqttActionListener.onFailure(null, e)
        }
    }

    /**
     * publish
     * 消息发布
     * @param publishTopic 将消息发布给publishTopic
     * @param message 发布的消息
     * @param qos 发布的消息质量
     * 0：消息至多发布一次
     * 1：消息至少发布一次
     * 2：消息仅发布一次
     * @param retained 服务器是否保留该消息
     * @param publishTag {@link MQTTEnum.PUBLISH}
     */
    fun publish(
        publishTopic: String,
        message: String,
        qos: Int? = 0,
        retained: Boolean? = false,
        publishTag: MQTTEnum? = MQTTEnum.PUBLISH
    ) {
        //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
        mMQTTAndroidClient?.publish(
            publishTopic,
            message.toByteArray(),
            qos ?: 0,
            retained ?: false,
            publishTag ?: MQTTEnum.PUBLISH,
            iMqttActionListener
        )
    }

    /**
     * set will
     * 设置遗嘱（断开连接时的提示）
     */
    fun setWill(publishTopic: String) {
        // last will message
        val message = "{\"terminal_uid\":\"" + mClientId + "\"}"
        val qos = 2
        val retained = false
        if (message != "" || publishTopic != "") {
            // 最后的遗嘱
            try {
                mMQTTConnectOptions?.setWill(publishTopic, message.toByteArray(), qos, retained)
            } catch (e: Exception) {
                Log.i(TAG, "Exception Occured", e)
                iMqttActionListener.onFailure(null, e)
            }
        }
    }

    /**
     * subscribe 消息订阅
     * @param subscribeTopic 订阅subscribeTopic主题，所有向该主题发送的消息，他都能收到
     * @param qos 消息发布质量
     * @param subscribeTag 发布订阅消息结果监听标签
     *
     */
    fun subscribe(
        subscribeTopic: String,
        qos: Int? = 0,
        subscribeTag: MQTTEnum? = MQTTEnum.SUBSCRIBE
    ) {
        mMQTTAndroidClient?.subscribe(subscribeTopic, qos ?: 0, subscribeTag, iMqttActionListener)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        mMQTTAndroidClient?.disconnect()
    }
}