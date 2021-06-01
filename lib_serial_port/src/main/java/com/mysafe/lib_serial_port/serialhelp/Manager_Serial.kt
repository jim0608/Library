package com.mysafe.lib_serial_port.serialhelp

import android.os.*
import android.util.Log
import android_serialport_api.SerialPort
import com.mysafe.lib_serial_port.StandardState
import com.mysafe.lib_serial_port.Tags
import com.mysafe.lib_serial_port.interface_class.IConnectStateCallback
import com.mysafe.lib_serial_port.interface_class.IReceiveCallback
import com.mysafe.lib_serial_port.interface_class.ISendResultCallback
import com.mysafe.lib_serial_port.thread.longtime.ReadThread
import com.mysafe.lib_serial_port.thread.longtime.WriteThread
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class Manager_Serial {
    private val data: MutableMap<String, String?> = HashMap()
    private val bundle = Bundle()
    private var writeThread: WriteThread? = null
    private var readThread: ReadThread? = null
    private var serialPort: SerialPort? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    //A:发送端对应的接口 ,B:接收端对应的接口
    private var connectCallback //串口连接成功
            : IConnectStateCallback? = null
    private var receiveCallback_B //接收端接收到的数据回调
            : IReceiveCallback? = null
    private var sendCallback_A //数据发送成功
            : ISendResultCallback? = null
    private var timeTag = 0
    private var timeDelayed = 0 //延时时间

    //发送数据的类型 TAG_SERIAL_SENDING_DATA_SINGLE:只能一端发送数据,TAG_SERIAL_SENDING_DATA_INTERACTION:两端都可以发送数据
    private var tagData = 0

    /**
     * 串口是否开启
     */
    private var isStart = false

    /**
     * 串口是否连接
     */
    private var canWriting = true //是否发送数据 ,false:不能执行写操作,true:可以执行写操作
    private var infoJson = "CONNECTING"//设备登录成功之后，基础数据
    private var sendData: String? = null//发送的数据

    //endregion

    //region 超时管理
    val timer = object : CountDownTimer(8000, 1000) {
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            Log.i("TAG_TIME", "onFinish: 轮询")
            singleton.requestConnect(infoJson)
            cancel()
            start()
        }
    }

    private val downTimer = object : CountDownTimer(5000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            if (!WriteThread.isWriting) {
                Log.i("TAG_TIME", millisUntilFinished.toString() + "")
            } else {
                cancel()
                start()
            }
        }
        override fun onFinish() {
            canWriting = true
            Log.i("TAG_TIME", "超时")
            if (timeTag == StandardState.TAG_SERIAL_TEST_CONNECTING) {
                //连接失败
                connectCallback?.onConnectResult(false)
            } else {
                //数据发送失败
                sendCallback_A?.onResult( "发送失败：请开启对端设备!")
            }
            timeTag = -1
            cancel()
        }
    }

    //endregion
    //region handler操作分发
    private val handler = object : Handler() {
        private var tag = 0
        private var content: String? = null

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                StandardState.TAG_SERIAL_TEST_CONNECTING -> {
                    canWriting = true
                    Log.i(Tags.TAG_SERIAL_TEST, "serial is connecting")
                    //告诉发送方,我已连接上
                    sendData(StandardState.TAG_SERIAL_TEST_CONNECTED, "Connected")
                    //接收端接收到数据，连接成功
                    val baseData = msg.data.getString(Tags.TAG_SERIAL_BASE, false.toString())
//                    receiveCallback_B?.OnDataReceive(Tags.TAG_SERIAL_BASE, baseData)
                }
                StandardState.TAG_SERIAL_TEST_CONNECTED -> {
                    closeCountDown()
                    Log.i(Tags.TAG_SERIAL_TEST, "serial is connected")
                    //获取到接收方返回的连接状态,确认当前串口已连接
                    connectCallback?.onConnectResult(true)
                }

                StandardState.TAG_SERIAL_READY_SENDING -> {
                    downTimer.cancel()
                    Log.i("TAG_SERIAL_READY_SEND", "this is serial ready")
                    canWriting = true
                    val isStop = msg.data.getString(Tags.TAG_SERIAL_REQUEST, false.toString())
                    if (isStop == "true") {
                        timer.cancel()
                    }
                    sendMsg(StandardState.TAG_SERIAL_READY_WAITING, "Ready Sending")

                }
                StandardState.TAG_SERIAL_READY_WAITING -> {
                    closeCountDown()
                    //开始发送数据
                    sendMsg()
                }
                StandardState.TAG_SERIAL_SENDING_DATA_SINGLE -> {
                    Log.i(Tags.TAG_SERIAL_DATA, "this is serial content")
                    val dataSingle = msg.data.getString(Tags.TAG_SERIAL_DATA)
//                    receiveCallback_B?.OnDataReceive(Tags.TAG_SERIAL_TEST, dataSingle)
                    canWriting = true

                    //数据传输完毕之后，通知发送端，数据传输完毕
                    //告诉发送方,当前已接收完数据,返回TAG_SERIAL_LEISURE_STATE(空闲状态)
                    sendData(StandardState.TAG_SERIAL_SENDING_OVER, "now is over")
                }
                StandardState.TAG_SERIAL_SENDING_DATA_INTERACTION -> {
                    Log.i(Tags.TAG_SERIAL_DATA, "this is serial content")
                    canWriting = true
                    val dataInteraction = msg.data.getString(Tags.TAG_SERIAL_DATA)
//                    receiveCallback_B?.OnDataReceive(Tags.TAG_SERIAL_TEST, dataInteraction)
                }
                StandardState.TAG_SERIAL_SENDING_OVER -> {
                    closeCountDown()
                    Log.i(Tags.TAG_SERIAL_OVER, "serial is over")
                    //获取数据接收方的返回的空闲状态,告诉用户,数据发送成功
                    sendCallback_A?.onResult( "通信结束：" + Tags.TAG_SERIAL_OVER)
                }
                StandardState.TAG_SERIAL_SEND_DATA -> {
                    tag = bundle.getInt("TAG_SERIAL")
                    content = bundle.getString("Content")
                    Log.i(Tags.TAG_SERIAL_DATA, "$tag $content".trimIndent())
                    sendData(tag, content)
                }
            }
        }
    }
    //endregion
    /**
     * 获取当前串口连接状态
     *
     * @param connectCallback 连接成功回调
     */
    fun requestConnect(connectCallback: IConnectStateCallback?) {
        this.connectCallback = connectCallback
        sendMsgToHandler(StandardState.TAG_SERIAL_TEST_CONNECTING, infoJson)
        openCountDown()
    }

    /**
     * 连接串口，传输baseUrl
     *
     * @param baseURL 设备登录成功之后同步baseUrl
     * @param connectCallback 连接成功回调
     */
    fun requestConnect(dataSource: String) {
        infoJson = dataSource
        sendMsgToHandler(StandardState.TAG_SERIAL_TEST_CONNECTING, infoJson)
        openCountDown()
    }

    /**
     * @param isOver       true   只能一端发送数据`tagData = TAG_SERIAL_SENDING_DATA_SINGLE`;
     *                     false  两端都可以发送数据`tagData = TAG_SERIAL_SENDING_DATA_INTERACTION`
     * @param isStop       是否需要主动操作停止对端的功能:true 对端接收到消息后有自己的操作需要处理,false 对端当前处于空闲没有其他操作
     * @param sendData     发送的数据体
     * @param sendCallback 发送数据的结果
     */
    fun startSending(isOver: Boolean, isStop: Boolean, sendData: String?, sendCallback: ISendResultCallback?) {
        this.sendData = sendData
        sendCallback_A = sendCallback
        tagData = if (isOver) StandardState.TAG_SERIAL_SENDING_DATA_SINGLE else StandardState.TAG_SERIAL_SENDING_DATA_INTERACTION
        openCountDown()
        sendMsgToHandler(StandardState.TAG_SERIAL_READY_SENDING, isStop.toString())
    }

    //发送正式数据
    fun sendMsg(isOver: Boolean, sendData: String, callback: ISendResultCallback?) {
        this.sendData = sendData
        sendCallback_A = callback
        tagData = if (isOver) StandardState.TAG_SERIAL_SENDING_DATA_SINGLE else StandardState.TAG_SERIAL_SENDING_DATA_INTERACTION
        sendMsgToHandler(tagData, sendData.trim { it <= ' ' })
    }

    fun sendMsg(tag: Int, sendData: String) {
        this.sendData = sendData
        sendMsgToHandler(tag, sendData.trim { it <= ' ' })
    }

    private fun sendMsg() {
        sendData?.trim { it <= ' ' }?.let { sendMsgToHandler(tagData, it) }
    }

    //可自定义延时发送
    private fun sendMsgToHandler(tag: Int, content: String) {
        timeTag = tag
        bundle.clear()
        bundle.putInt("TAG_SERIAL", tag)
        bundle.putString("Content", content)
        val msg = handler.obtainMessage()
        msg.what = StandardState.TAG_SERIAL_SEND_DATA
        msg.data = bundle
        //防止
        handler.sendMessageDelayed(msg, timeDelayed.toLong())
        //        timer.start();
    }

    /**
     * 判断是否开启写线程（传输数据）
     *
     * @param tag     发送内容的格式
     * @param content 发送的内容
     */
    private var identifyTimes = 0
    private fun sendData(tag: Int, content: String?) {
        if (isStart && canWriting) {
            if (tag != StandardState.TAG_SERIAL_SENDING_OVER && tag != StandardState.TAG_SERIAL_TEST_CONNECTED) {
                canWriting = false
            }
            identifyTimes = 0
            startWriteThread(tag, content)
        } else if (!isStart) {
            Log.i("TAG_Serial", "$tag:串口未连接!")
        } else {
            if (sendCallback_A == null && connectCallback != null) {
                connectCallback?.onConnectResult(false)
            } else if (sendCallback_A != null) {
                sendCallback_A?.onResult( "正在发送数据,请稍后!")
            }
            if (identifyTimes >= 5) {
                //超过5次发送失败，开启10s倒计时，10s内仍然未接收到数据或者发送成功，通知用户，排查串口连接
                downTimer.cancel()
                downTimer.start()
                Log.d("TAG_COUNT_TIME", identifyTimes.toString() + "")
            }
            identifyTimes++
            Log.i("TAG_Serial", "$tag:串口正在占用!")
        }
    }

    /**
     * 开启写线程（传输数据）
     *
     * @param tag     发送内容的格式
     * @param content 发送的内容
     */
    private fun startWriteThread(tag: Int, content: String?) {
        if (data != null) {
            data.clear()
            data[Tags.KEY_TAG] = tag.toString()
            data[Tags.KEY_Content] = content
            Log.i("TAG_Serial", "writeThread.isWriting:" + WriteThread.isWriting)
            if (!WriteThread.isWriting) {
                writeThread?.setData(data)
            } else {
                Log.i("TAG_Serial", "$tag:正在发送中!$content")
            }
        }
    }

    /**
     * 开启串口发送倒计时（如果超时则提示：串口未连接）
     */
    private fun openCountDown() {
        if (canWriting) {
            downTimer.cancel()
            downTimer.start()
        }
    }

    /**
     * 关闭串口发送倒计时（如果超时则提示：串口未连接）
     */
    private fun closeCountDown() {
        canWriting = true
        downTimer.cancel()
    }

    //region 基本不会改变的模块
    fun setTimeDelayed(timeDelayed: Int) {
        this.timeDelayed = timeDelayed
    }

    fun setConnectCallback(connectCallback: IConnectStateCallback?) {
        this.connectCallback = connectCallback
    }

    fun setSendCallback(sendCallback: ISendResultCallback?) {
        sendCallback_A = sendCallback
    }

    fun setReceiveCallback(receiveCallback_B: IReceiveCallback?) {
        this.receiveCallback_B = receiveCallback_B
    }

    /**
     * 打开串口
     *
     * @param serialPath 串口地址(/dev/ttysX)
     * @param baud       波特率(两边波特率要保持一致)
     * @return 打开的结果, 参考
     * [打开成功][StandardState.OpenSerialPort_Success]
     * [打开失败,IO异常][StandardState.OpenSerialPort_IOException]
     */
    fun openSerial(serialPath: String?, baud: Int): Int {
        if (serialPort == null) {
            try {
                serialPort = SerialPort(File(serialPath), baud, 0)
                inputStream = serialPort?.inputStream
                outputStream = serialPort?.outputStream
                openThread()
            } catch (e: IOException) {
                return StandardState.OpenSerialPort_IOException
            } catch (se: SecurityException) {
                return StandardState.OpenSerialPort_SecurityException
            }
        } else {
            serialPort?.closeSerial()
            serialPort = null
            isStart = false
            closeCountDown()
            return openSerial(serialPath, baud)
        }
        isStart = true
        return StandardState.OpenSerialPort_Success
    }

    /**
     * 开启读线程
     */
    fun openThread() {
        if (readThread == null) {
            readThread = ReadThread(handler, inputStream)
            readThread?.start()
            readThread!!.interrupt()
        } else {
            readThread?.setInputStream(inputStream)
        }
        if (writeThread == null) {
            writeThread = WriteThread(outputStream)
            writeThread?.start()
        } else {
            writeThread?.setOutputStream(outputStream)
        }
    }

    /**
     * 关闭串口
     *
     * @return 关闭的结果(不过好像没有结果)
     */
    fun closeSerial(): Int {
        try {
            if (serialPort != null) {
                serialPort?.closeSerial()
                serialPort = null
            }
            if (readThread != null) {
                readThread?.closeReadThread()
            }
            if (writeThread != null) {
                writeThread?.closeWriteThread()
            }
            handler.removeCallbacksAndMessages(null)
            isStart = false
            closeCountDown()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            return StandardState.CloseSerialPort_CloseException
        }
        return StandardState.CloseSerialPort_Success
    } //endregion

    companion object {
        //region 变量
        @JvmStatic
        val singleton by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Manager_Serial()
        }
    }
}