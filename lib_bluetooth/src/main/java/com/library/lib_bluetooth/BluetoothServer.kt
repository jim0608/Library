package com.library.lib_bluetooth

import android.os.Build
import android.os.Bundle
import android.util.Log
import java.util.*

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/10
 * @Describe:
 */
class BluetoothServer : BaseBluetooth() {
    private val TAG = "TAG_BluetoothServer"
    private var dataInfo = ""

    fun initBluetoothServer(dataInfo: String) {
        this.dataInfo = dataInfo
        setTurnListener(isStateListener)
        getDeviceAdapter().name = "配餐端(${Build.SERIAL})"
        if (checkBluetooth()) {
            openBlueServer()
        }
    }

    fun openBlueServer() {
        try {
            receiveListener("蓝牙[${getDeviceAdapter().name}]等待连接...")
            // 创建一个蓝牙服务器 参数分别：服务器名称、UUID
            mServerSocket = getDeviceAdapter().listenUsingRfcommWithServiceRecord(
                "btspp",
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )!!
            val mClientSocket = mServerSocket.accept()
            setClientSocket(mClientSocket)
            //连接成功之后关闭ServerSocket，这样其他设备就不会连接到该通路了，
            //并且mClientSocket并不会被关掉
            mServerSocket.close()
            //连接成功将remoteMac返回
            receiveListener(mClientSocket.remoteDevice.address, REMOTE_MAC, BLUETOOTH_REMOTE_MAC)

            readMsg()
            Log.i(TAG, "bluetoothServerSocket: 连接成功")
        } catch (e: Exception) {
            closeSocket()
            e.printStackTrace()
            Log.i(TAG, "bluetoothServerSocket: 连接失败:${e.message}")
            if (e.message.equals("socket closed") && !clientIsClosed) {
                openBlueServer()
            }
        }
    }

    private val isStateListener = object : IBluetoothListener {
        override fun onStateListener(isClose: Boolean) {
            if (isClose) {
                openBluetooth()
            } else {
                openBlueServer()
            }
        }
    }

    companion object {
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            BluetoothServer()
        }
    }

}