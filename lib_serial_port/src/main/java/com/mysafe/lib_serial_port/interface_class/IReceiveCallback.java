package com.mysafe.lib_serial_port.interface_class;

public interface IReceiveCallback {
    //正式数据返回
    void onDataReceive(int tag, String content);
}

