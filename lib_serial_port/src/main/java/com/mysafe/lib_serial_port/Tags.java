package com.mysafe.lib_serial_port;

public class Tags {
    public static final String KEY_TAG = "Tag";
    public static final String KEY_Content = "Content";

    //防沾包串口数据前缀与后缀（前缀）
    public static final String TAG_START = "#Hajime#";
    // （前缀）16进制：23 48 61 6A 69 6D 65 23
    public static final String TAG_START_HEX = "2348616A696D6523";

    //防沾包串口数据前缀与后缀（后缀）
    public static final String TAG_END = "#Owari#";
    // （后缀）16进制：23 4F 77 61 72 69 23
    public static final String TAG_END_HEX = "234F7761726923";


    public static final String TAG_SE_GLUED = TAG_END + TAG_START;
    public static final String TAG_SE_GLUED_HEX = TAG_END_HEX + TAG_START_HEX;

    public static final String TAG_FLAG_DATA_COMPLETE = "#_Data_Complete_#";
    public static final String TAG_FLAG_DATA_TEST_CLEAR = "#_PORT_OK_#";

    public static final String TAG_SERIAL_BASE = "TAG_SERIAL_BASE";//测试通讯
    public static final String TAG_SERIAL_TEST = "TAG_SERIAL_TEST";//测试通讯
    public static final String TAG_SERIAL_REQUEST = "TAG_SERIAL_REQUEST";//请求通讯
    public static final String TAG_SERIAL_OVER = "TAG_SERIAL_OVER";//空闲
    public static final String TAG_SERIAL_DATA = "TAG_SERIAL_DATA";//传输数据
}
