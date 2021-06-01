package com.mysafe.lib_serial_port.interface_class;

/**
 * 串口连接状态接听
 */
public interface IConnectStateCallback {
    void onConnectResult(boolean isConnect);
}

