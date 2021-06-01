package com.mysafe.lib_serial_port.thread;

import android.util.Log;

import com.mysafe.lib_serial_port.interface_class.ISendResultCallback;
import com.mysafe.lib_serial_port.util.BytesUtils;
import com.mysafe.lib_serial_port.util.DataUtils;
import com.mysafe.lib_serial_port.util.GsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static com.mysafe.lib_serial_port.SerialTagsKt.TAG_BEGINNING;
import static com.mysafe.lib_serial_port.SerialTagsKt.TAG_ENDING;
import static com.mysafe.lib_serial_port.StandardState.MaxLengthOfTheReadContent;


/**
 * @author 张晋铭
 */
public class SendThread extends Thread {
    private Map<String, String> data;
    private ISendResultCallback listener;
    /**
     * 是否挂起线程
     */
    private boolean isSending = true;
    private boolean suspendFlag = true;
    private OutputStream outputStream;

    public SendThread(OutputStream outputStream) {
        setOutputStream(outputStream);
    }

    public void setDataListener(ISendResultCallback listener) {
        this.listener = null;
        this.listener = listener;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }


    public void close(){
        isSending = false;
        this.suspendFlag = false;
    }
    public Boolean getSuspendFlag() {
        return suspendFlag;
    }

    public synchronized void setResume(Map<String, String> data) {
        this.data = data;
        this.suspendFlag = false;
        notify();
    }


    @Override
    public void run() {
        while (isSending) {
            try {
                if (suspendFlag) {
                    synchronized (this) {
                        //阻塞写线程
                        wait();
                    }
                }
                if (!isSending){
                    return;
                }
                String str = TAG_BEGINNING + GsonUtil.getInstance().jsonToString(data) + TAG_ENDING;
                writeStep(str);
                if (listener != null){
                    listener.onResult("发送结果：" + data);
                    //挂起线程
                    suspendFlag = true;
                }
                //写数据操作完成
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                //写数据操作完成
            }
        }
    }

    /**
     * 执行写操作
     *
     * @param str 数据
     * @throws IOException
     */
    private void writeStep(String str) throws IOException {
        Log.i("TAG_WRITE_THREAD", str);
        //string转HexString
        String hex = DataUtils.str2HexStr(str);
        Log.i("TAG_WRITE_THREAD", hex);
        //Hex String转byte数组
        byte[] strBytes = DataUtils.HexToByteArr(hex);
        if (strBytes.length > 0) {
            if (strBytes.length > MaxLengthOfTheReadContent - 1) {
                byte[][] byteArray = BytesUtils.splitBytes(strBytes, MaxLengthOfTheReadContent);
                for (byte[] bytes : byteArray) {
                    if (outputStream != null) {
                        outputStream.write(bytes);
                        outputStream.flush();
                    }
                }
            } else {
                if (outputStream != null) {
                    outputStream.write(strBytes);
                    outputStream.flush();
                }
            }
        }
    }
}