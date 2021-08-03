package com.library.lib_bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import com.library.lib_bluetooth.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/10
 * @Describe:
 */
open class BaseBluetooth {
    private val TAG = "TAG_BaseBluetooth"
    private var mDeviceAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val sb by lazy { StringBuilder() }

    // 蓝牙服务端socket
    lateinit var mServerSocket: BluetoothServerSocket

    private lateinit var bluetoothState: BluetoothBroadReceiver


    // 蓝牙客户端socket
    private lateinit var mClientSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    private val bytes = ByteArray(2048)
    private var length: Int = 0
    private val bundle = Bundle()

    private var mContext: Context? = null
    private var mHandler: Handler? = null

    private var deviceListener: IDeviceReceiveListener? = null
    private lateinit var turnListener: IBluetoothListener
    private var isRegisterBroad = false
    var clientIsClosed = false

    //蓝牙连接状态
    var state = WAIT

    fun getDeviceAdapter(): BluetoothAdapter {
        return mDeviceAdapter!!
    }

    /**
     * 设备是否支持蓝牙
     * @return true该设备不支持蓝牙
     */
    fun isNotSupport(): Boolean {
        return mDeviceAdapter == null
    }

    /**
     * 是否开启蓝牙
     * @return true 已开启蓝牙
     */
    fun isOpenBluetooth(): Boolean {
        return mDeviceAdapter?.isEnabled == true
    }

    fun init(mContext: Context, mHandler: Handler) {
        this.mContext = mContext
        this.mHandler = mHandler
        clientIsClosed = false
    }

    fun setDeviceListener(deviceListener: IDeviceReceiveListener?) {
        this.deviceListener = deviceListener
    }

    fun setTurnListener(turnListener: IBluetoothListener) {
        this.turnListener = turnListener
    }

    fun setClientSocket(mClientSocket: BluetoothSocket) {
        this.mClientSocket = mClientSocket
        outputStream = mClientSocket.outputStream
        inputStream = mClientSocket.inputStream

    }

    fun checkBluetooth(): Boolean {
        initBroadcast()
        if (isNotSupport()) {
            Toast.makeText(mContext, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!isOpenBluetooth()) {
            openBluetooth()
//            return false
        }
        return true
    }

    /**
     * 发送消息
     */
    fun sendMsg(msg: String) {
        //string转HexString
        val hex = DataUtils.str2HexStr(msg)
        Log.i("TAG_WRITE_THREAD", hex)
        //Hex String转byte数组
        val strBytes = DataUtils.HexToByteArr(hex)
        if (mClientSocket.isConnected) {
            outputStream.write(strBytes)
            outputStream.flush()
        }
    }

    /**
     * 开启蓝牙
     */
    fun openBluetooth() {
        //蓝牙可以无限期被其他设备发现
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0)
        mContext?.startActivity(discoverableIntent)
    }

    fun readMsg() {
        try {
            while (true) {
                if (sb.isNotEmpty() && inputStream.available() == 0) {
                    val str = sb.toString()
                    Log.i("TAG", "run: ${inputStream.available()} $str")
                    val readData = DataUtils.hexStr2Str(str)
                    receiveListener(readData, RECEIVE_MSG, BLUETOOTH_RECEIVE_MSG)
                    sb.clear()
                }

                length = inputStream.read(bytes)
                if (length > 0) {
                    val hex = DataUtils.ByteArrToHex(bytes, 0, length)
                    sb.append(hex)
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("bt socket closed") == true) {
                throw IOException("socket closed")
            } else {
                bundle.clear()
                bundle.putString(RECEIVE_MSG, "ERROR:${e.message}")
                receiveListener(bundle, BLUETOOTH_RECEIVE_MSG)
            }
        } finally {
            closeSocket()
        }
    }


    fun receiveListener(str: String, key: String = STATE_DATA, what: Int = BLUETOOTH_STATE) {
        bundle.clear()
        bundle.putString(key, str)
        receiveListener(bundle, what)
    }


    fun receiveListener(data: Bundle, what: Int) {
        val msg: Message? = mHandler?.obtainMessage()
        msg?.what = what
        msg?.data = data
        mHandler?.sendMessage(msg)
    }

    private fun initBroadcast() {
        //注册蓝牙广播
        bluetoothState = BluetoothBroadReceiver(deviceListener, turnListener)
        val intel = IntentFilter()
        intel.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        intel.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intel.addAction(BluetoothDevice.ACTION_FOUND)
        mContext?.registerReceiver(bluetoothState, intel)
        isRegisterBroad = true
    }

    open fun closeBluetooth() {
        clientIsClosed = true

        closeSocket()
        if (this::bluetoothState.isInitialized && isRegisterBroad) {
            isRegisterBroad = false
            mContext?.unregisterReceiver(bluetoothState)
        }
        if (mHandler != null) {
            mHandler?.removeCallbacksAndMessages(null)
        }
    }

    fun closeSocket() {
        if (this::inputStream.isInitialized) {
            inputStream.close()
        }
        if (this::outputStream.isInitialized) {
            outputStream.close()
        }
        if (this::mClientSocket.isInitialized) {
            mClientSocket.close()
        }
        if (this::mServerSocket.isInitialized) {
            mServerSocket.close()
        }
    }
}