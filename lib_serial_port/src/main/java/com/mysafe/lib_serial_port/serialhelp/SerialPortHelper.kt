package com.mysafe.lib_serial_port.serialhelp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.util.Log
import android_serialport_api.SerialPort
import com.mysafe.lib_serial_port.*
import com.mysafe.lib_serial_port.interface_class.IMiddleReceiveCallback
import com.mysafe.lib_serial_port.interface_class.IReceiveCallback
import com.mysafe.lib_serial_port.interface_class.ISendResultCallback
import com.mysafe.lib_serial_port.thread.ReadThread
import com.mysafe.lib_serial_port.thread.SendThread
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Create By 张晋铭
 * @Date on 2021/3/18
 * @Describe:
 */
class SerialPortHelper {
    private val TAG = "TAG_SerialPortHelper"
    private var mSerialPort: SerialPort? = null
    private var mOutputStream: OutputStream? = null
    private var mInputStream: InputStream? = null
    private var mReadThread: ReadThread? = null
    private var mSendThread: SendThread? = null
    private var receivedListener: IReceiveCallback? = null
    private var receivedMiddleListener: IMiddleReceiveCallback? = null
    private val mapData by lazy { mutableMapOf<String, String>() }
    private val sPort = "/dev/ttyS3"
    private val iBaudRate = 9600
    private val flags = 0
    private var _isOpen = false

    /*
                  配餐端                             取餐端
            1. 串口连接
   (开始发起)TAG_SERIAL_CONNECT          ->      TAG_SERIAL_CONNECT
            TAG_SERIAL_OVER             <-      TAG_SERIAL_OVER(返回结果)
            2.人员信息同步
            TAG_SERIAL_FACE             <-      TAG_SERIAL_FACE
            TAG_SERIAL_OVER             ->      TAG_SERIAL_OVER
     */
    @SuppressLint("HandlerLeak")
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val tag = msg.what ?: 0
            val dataUser = msg.data.getString(tag.toString())
            receivedListener?.onDataReceive(tag, dataUser)
            receivedMiddleListener?.onDataReceive(tag, dataUser)
            Log.i("TAG_SERIAL_FOOD", "this is serial food $tag dataUser $dataUser")
        }
    }

    fun openSerialPort() {
        if (mSerialPort == null) {
            mSerialPort = SerialPort(File(sPort), iBaudRate, flags)
            mOutputStream = mSerialPort?.outputStream
            mInputStream = mSerialPort?.inputStream
            openThread()
        } else {
            mSerialPort?.close()
            mSerialPort = null
            openSerialPort()
        }
    }

    /**
     * 关闭串口
     */
    fun closeSerial() {
        mSerialPort?.close()
        mSerialPort = null
        mReadThread?.close()
        mReadThread = null
        mSendThread?.close()
        mSendThread = null
        mHandler.removeCallbacksAndMessages(null)
    }

    fun getReceiveListener(receivedListener: IReceiveCallback?, receivedMiddleListener: IMiddleReceiveCallback?) {
        this.receivedListener = receivedListener
        this.receivedMiddleListener = receivedMiddleListener
    }

    fun sendData(tag: Int, content: String, sendListener: ISendResultCallback) {
        mSendThread?.setDataListener(sendListener)
        if (mSendThread?.suspendFlag == true) {
            startWriteThread(tag, content)
        } else {
            sendListener.onResult("SENDING")
        }
    }

    /**
     * 开启读写线程
     */
    private fun openThread() {
        if (mReadThread == null) {
            mReadThread = ReadThread(mHandler, mInputStream)
            mReadThread?.start()
        } else {
            mReadThread?.setInputStream(mInputStream)
        }
        if (mSendThread == null) {
            mSendThread = SendThread(mOutputStream)
            mSendThread?.start()
        } else {
            mSendThread?.setOutputStream(mOutputStream)
        }
    }

    /**
     * 开启写线程（传输数据）
     *
     * @param tag     发送内容的格式
     * @param content 发送的内容
     */
    private fun startWriteThread(tag: Int, content: String) {
        mapData.clear()
        mapData[KEY_TAG] = tag.toString()
        mapData[KEY_CONTENT] = content
        mSendThread?.setResume(mapData)
        Log.i("TAG_Serial", "$tag:正在发送:$content")
    }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            SerialPortHelper()
        }
    }
}