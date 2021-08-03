package com.library.lib_bluetooth

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/7
 * @Describe:
 */
class BluetoothDevicesBean {
    private val TAG = "TAG_BluetoothDevices"
    var name = ""
    var macAddress = ""

    constructor(name: String, macAddress: String) {
        this.name = name
        this.macAddress = macAddress
    }
}