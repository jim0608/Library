package com.library.lib_bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.library.lib_bluetooth.IBluetoothListener


/**
 * @author Create By 张晋铭
 * @Date on 2021/6/4
 * @Describe:
 */
class BluetoothBroadReceiver(
    private val deviceListener: IDeviceReceiveListener? = null,
    private val turnListener: IBluetoothListener? = null
) : BroadcastReceiver() {
    private val TAG = "TAG_BluetoothBroadReceiver"
    override fun onReceive(context: Context?, intent: Intent) {
        //获取蓝牙扫描数据
        if (deviceListener != null) {
            scanList(intent)
        }

        //蓝牙开启关闭监听
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
            BluetoothAdapter.STATE_ON -> {
                turnListener?.onStateListener(false)
            }
            BluetoothAdapter.STATE_OFF -> {
                turnListener?.onStateListener(true)
            }
        }
    }

    /**
     * 扫描蓝牙列表
     */
    private fun scanList(intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device?.name?.contains("") != true) {
                    return
                }
                Log.i(TAG, "scanList: name:${device.name} , mac:${device.address}")
                // 获得设备信息
                deviceListener?.onBluetoothDevice(
                    BluetoothDevicesBean(
                        device.name ?: "",
                        device.address ?: ""
                    )
                )
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                deviceListener?.onScanFinish()
            }
        }
    }
}