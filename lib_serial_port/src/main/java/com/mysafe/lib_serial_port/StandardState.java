package com.mysafe.lib_serial_port;

/**
 * 标准状态集
 */
public class StandardState {
    /**
     * 一条数据的最大值
     */
    public static final int MaxLengthOfTheReadContent = 2048;
    public static final int OpenSerialPort_SecurityException = 0x00;

    public static final int OpenSerialPort_Success = 0x1;
    public static final int OpenSerialPort_IOException = 0x2;
    public static final int CloseSerialPort_Success = 0x3;
    public static final int CloseSerialPort_SerialObjectNull = 0x4;
    public static final int CloseSerialPort_CloseException = 0x5;

    public static final int ReadContentState_Reading = 0x6;
    public static final int ReadContentState_Finish = 0x7;
    public static final int ReadContentState_Standby = 0x8;

    public static final int WriteContentState_Writing = 0x9;
    public static final int WriteContentState_Standby = 0x10;

    //获取的串口数据开头部分
    public static final int ReadContentConditional_Start = 11;
    //获取的串口数据中间部分
    public static final int ReadContentConditional_Reading = 12;
    //获取的串口数据结尾部分
    public static final int ReadContentConditional_End = 13;
    //简单数据，read线程读取一次就获取完毕
    public static final int ReadContentConditional_SingleContent = 14;
    //发送串口未携带数据
    public static final int ReadContentConditional_GluedContent = 15;
    public static final int ReadContentConditional_ConnectionNormal = 16;

    public static final int TAG_SERIAL_TEST_CONNECTING = 17;//串口连接中
    public static final int TAG_SERIAL_TEST_CONNECTED = 18;//串口连接完成

    public static final int TAG_SERIAL_READY_SENDING = 19;//发送数据前——通知对方准备接受数据
    public static final int TAG_SERIAL_READY_WAITING = 20;//发送数据前——通知对方，己方准备完毕，可以开始发送
    public static final int TAG_SERIAL_SENDING_DATA_INTERACTION = 21;//发送数据中——发送端和接收端,存在数据交互
    public static final int TAG_SERIAL_SENDING_DATA_SINGLE = 22;//发送数据中——只有一端(A端或者B端)发送数据,另外一端只接收数据
    public static final int TAG_SERIAL_SENDING_OVER = 23;//发送数据中——通知对方发送的数据已接收完毕

    public static final int TAG_SERIAL_SEND_DATA = 25;//发送数据



}
