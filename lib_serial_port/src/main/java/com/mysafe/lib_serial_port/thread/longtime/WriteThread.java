package com.mysafe.lib_serial_port.thread.longtime;

import android.util.Log;

import com.mysafe.lib_serial_port.util.BytesUtils;
import com.mysafe.lib_serial_port.util.DataUtils;
import com.mysafe.lib_serial_port.util.GsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import static com.mysafe.lib_serial_port.StandardState.MaxLengthOfTheReadContent;
import static com.mysafe.lib_serial_port.Tags.TAG_END;
import static com.mysafe.lib_serial_port.Tags.TAG_START;

/**
 * @author 张晋铭
 * 原始版本：支持长连接
 */
public class WriteThread extends Thread {
    private Map<String, String> data;
    private static boolean isStartWrite = false;
    /**
     * false 空闲,true 忙碌
     */
    public static boolean isWriting = false;
    private Object objLock;
    private OutputStream outputStream;

    public WriteThread(OutputStream outputStream) {
        setOutputStream(outputStream);
        isStartWrite = true;
    }

    public void setOutputStream(OutputStream outputStream) {
        if (objLock == null) {
            objLock = new Object();
        }
        this.outputStream = outputStream;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
        //唤醒写线程
        if (objLock == null) {
            objLock = new Object();
        }
        synchronized (objLock) {
            isWriting = true;
            objLock.notify();
        }

    }

    @Override
    public void run() {
        while (isStartWrite) {
            try {
                if (!isWriting) {
                    synchronized (objLock) {
                        //阻塞写线程
                        objLock.wait();
                        sleep(200);
                    }
                }

                String str = TAG_START + GsonUtil.getInstance().jsonToString(data) + TAG_END;
                writeStep(str);
                //写数据操作完成
                isWriting = false;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                //写数据操作完成
                isWriting = false;
            }
        }
    }

    //执行写操作
    private void writeStep(String str) throws IOException {
        Log.i("TAG_WRITE_THREAD", str);
        //string转HexString
        String hex = DataUtils.str2HexStr(str);
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

    public void closeWriteThread() throws IOException {
        isStartWrite = false;

        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
        if (objLock != null) {
            //唤醒写线程
            synchronized (objLock) {
                isWriting = true;
                objLock.notify();
            }
            objLock = null;
        }
    }

}