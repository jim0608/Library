package com.mysafe.lib_serial_port.interface_class;

public interface IMiddleReceiveCallback {
    //中间层正式数据返回
    void onDataReceive(int tag, String content);
}

