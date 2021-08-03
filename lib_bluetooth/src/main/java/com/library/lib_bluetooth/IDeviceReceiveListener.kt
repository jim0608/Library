package com.library.lib_bluetooth

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/8
 * @Describe:
 */
interface IDeviceReceiveListener {
    fun onBluetoothDevice(devicesBean: BluetoothDevicesBean)
    fun onScanFinish()
}