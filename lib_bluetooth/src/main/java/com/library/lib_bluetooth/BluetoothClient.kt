package com.library.lib_bluetooth

import android.os.CountDownTimer
import android.util.Log
import com.library.lib_bluetooth.BLUETOOTH_REMOTE_MAC
import com.library.lib_bluetooth.BaseBluetooth
import com.library.lib_bluetooth.IBluetoothListener
import com.library.lib_bluetooth.REMOTE_MAC
import kotlinx.coroutines.*
import java.util.*

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/10
 * @Describe:
 */
class BluetoothClient : BaseBluetooth() {
    private val TAG = "TAG_BluetoothClient"
    private var work: Job? = null
    private var mac:String = ""

    fun initBluetoothClient(deviceListener: IDeviceReceiveListener) {
        setDeviceListener(deviceListener)
        setTurnListener(isStateListener)
        if (checkBluetooth()) {
            scanBluetooth()
        }
    }

    /**
     * 扫描蓝牙列表
     */
    private fun scanBluetooth() {
        getDeviceAdapter().startDiscovery()
    }

    /**
     * 连接服务端
     */
    fun connectServer(mac: String) {
        Log.i(TAG, "connectServer: $mac")
        this.mac = mac
        work?.cancel()
        work = GlobalScope.launch(Dispatchers.IO) {
            receiveListener("连接中...")
            if (getDeviceAdapter().isDiscovering){
                getDeviceAdapter().cancelDiscovery()
            }
            val mDevice = getDeviceAdapter().getRemoteDevice(mac)
            if (mDevice != null) {
                try {
                    val mClientSocket = mDevice.createInsecureRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )
                    setClientSocket(mClientSocket)
                    Log.i(TAG, "bluetoothSocket: 连接中")
                    mClientSocket.connect()
                    Log.i(TAG, "bluetoothSocket: 连接成功")
                    //回调remoteMac地址
                    receiveListener(
                        mClientSocket.remoteDevice.address,
                        REMOTE_MAC,
                        BLUETOOTH_REMOTE_MAC
                    )
                    // 开启读
                    readMsg()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.i(TAG, "bluetoothSocket: 连接失败:${e.message}")
                    closeSocket()
                    if (e.message?.contains("socket might closed or timeout") == true){
                        delay(5000)
                        connectServer(mac)
                    }
                }
            }
        }
    }

    override fun closeBluetooth() {
        work?.cancel()
        super.closeBluetooth()
    }

    private val isStateListener = object : IBluetoothListener {
        override fun onStateListener(isClose: Boolean) {
            if (isClose) {
                openBluetooth()
            } else {
                scanBluetooth()
            }
        }
    }
    companion object {
        val instance by lazy ( LazyThreadSafetyMode.SYNCHRONIZED ){
            BluetoothClient()
        }
    }
}