package com.mysafe.lib_serial_port.thread.longtime;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.mysafe.lib_serial_port.util.DataUtils;
import com.mysafe.lib_serial_port.util.GsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.mysafe.lib_serial_port.StandardState.MaxLengthOfTheReadContent;
import static com.mysafe.lib_serial_port.StandardState.ReadContentConditional_End;
import static com.mysafe.lib_serial_port.StandardState.ReadContentConditional_GluedContent;
import static com.mysafe.lib_serial_port.StandardState.ReadContentConditional_Reading;
import static com.mysafe.lib_serial_port.StandardState.ReadContentConditional_SingleContent;
import static com.mysafe.lib_serial_port.StandardState.ReadContentConditional_Start;
import static com.mysafe.lib_serial_port.StandardState.ReadContentState_Finish;
import static com.mysafe.lib_serial_port.StandardState.ReadContentState_Reading;
import static com.mysafe.lib_serial_port.StandardState.ReadContentState_Standby;
import static com.mysafe.lib_serial_port.StandardState.TAG_SERIAL_READY_SENDING;
import static com.mysafe.lib_serial_port.StandardState.TAG_SERIAL_SENDING_DATA_INTERACTION;
import static com.mysafe.lib_serial_port.StandardState.TAG_SERIAL_SENDING_DATA_SINGLE;
import static com.mysafe.lib_serial_port.StandardState.TAG_SERIAL_TEST_CONNECTING;
import static com.mysafe.lib_serial_port.Tags.KEY_Content;
import static com.mysafe.lib_serial_port.Tags.KEY_TAG;
import static com.mysafe.lib_serial_port.Tags.TAG_END_HEX;
import static com.mysafe.lib_serial_port.Tags.TAG_SERIAL_BASE;
import static com.mysafe.lib_serial_port.Tags.TAG_SERIAL_DATA;
import static com.mysafe.lib_serial_port.Tags.TAG_SERIAL_REQUEST;
import static com.mysafe.lib_serial_port.Tags.TAG_SE_GLUED_HEX;
import static com.mysafe.lib_serial_port.Tags.TAG_START_HEX;

public class ReadThread extends Thread {
    private Map<String, String> data;
    private String content = "";
    private static boolean isReading = false;
    private int readContentState = ReadContentState_Standby;
    private StringBuilder sb = new StringBuilder();
    private String tempContent = "";
    private String halfContent = "";
    private int strLength = 0;
    private InputStream inputStream;
    private Handler handler;

    public ReadThread(Handler handler, InputStream inputStream) {
        this.handler = handler;
        this.inputStream = inputStream;
        isReading = true;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        while (!isReading) {
            try {
                if (inputStream != null && inputStream.available() > 0) {
                    byte[] bytes = new byte[Math.min(inputStream.available(), MaxLengthOfTheReadContent)];
                    strLength = inputStream.read(bytes);
                    if (strLength > 0) {
                        //byte数组转HexString
                        String hex = DataUtils.ByteArrToHex(bytes, 0, strLength);
                        //HexString转String
                        Log.d("TAG_TAG", hex);

                        //合并16进制
                        mergeHexData(hex);
                        if (readContentState == ReadContentState_Finish) {
                            //16进制转String
                            String readData = DataUtils.hexStr2Str(sb.toString());
                            Log.i("TAG_TAG", readData);
                            getDataByTag(readData);
                            //待机状态清空数据
                            if (sb != null && sb.length() > 0) {
                                sb.delete(0, sb.length());
                            }
                            readContentState = ReadContentState_Standby;
                        }
                    }
                } else {
                    SystemClock.sleep(50);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void mergeHexData(String readHexData) {
        switch (getNowReadConditional(readHexData)) {
            case ReadContentConditional_Start:
                readContentState = ReadContentState_Reading;
                String startStr = readHexData.replace(TAG_START_HEX, "");
                sb.append(startStr);
                break;
            case ReadContentConditional_Reading:
                readContentState = ReadContentState_Reading;
                sb.append(readHexData);
                break;
            case ReadContentConditional_End:
                String endStr = readHexData.replace(TAG_END_HEX, "");
                sb.append(endStr);
                readContentState = ReadContentState_Finish;
                break;
            case ReadContentConditional_SingleContent:
                if (readHexData.length() >= (TAG_START_HEX.length() + TAG_END_HEX.length())) {
                    String singleStr = readHexData.substring(TAG_START_HEX.length(), readHexData.length() - TAG_END_HEX.length());
                    sb.append(singleStr);
                }
                readContentState = ReadContentState_Finish;
                break;

        }
    }

    //获取当前数据状态
    private int getNowReadConditional(String content) {
        if (content.startsWith(TAG_START_HEX) && content.endsWith(TAG_END_HEX))
            return ReadContentConditional_SingleContent;
        if (content.startsWith(TAG_START_HEX))
            return ReadContentConditional_Start;
        if (content.endsWith(TAG_END_HEX))
            return ReadContentConditional_End;
        if (!content.contains(TAG_START_HEX) && !content.contains(TAG_END_HEX)) {
            return ReadContentConditional_Reading;
        }
        if (content.contains(TAG_SE_GLUED_HEX))
            return ReadContentConditional_GluedContent;

        return 0;
    }

    //数据完整之后，数据分发状态
    private void getDataByTag(String readData) {
        Message message = Message.obtain();
        int tag = getTagByData(readData);
        message.what = tag;
        switch (tag) {
            case TAG_SERIAL_READY_SENDING:
                Bundle bundleSending = new Bundle();
                bundleSending.putString(TAG_SERIAL_REQUEST, content);
                message.setData(bundleSending);
                break;
            case TAG_SERIAL_TEST_CONNECTING:
                Bundle bundleConnecting = new Bundle();
                bundleConnecting.putString(TAG_SERIAL_BASE, content);
                message.setData(bundleConnecting);
                break;
            case TAG_SERIAL_SENDING_DATA_INTERACTION:
            case TAG_SERIAL_SENDING_DATA_SINGLE:
                Bundle bundle = new Bundle();
                bundle.putString(TAG_SERIAL_DATA, content);
                message.setData(bundle);
                break;
        }
        handler.sendMessage(message);
    }


    private int getTagByData(String str) {
        try {
            data = GsonUtil.getInstance().jsonToMap(str);
        } catch (Exception e) {
            if (str.contains("\"Tag\":\"19\"")) {
                //发起串口请求时,对端也在发送数据,导致数据冲突出现乱码
                content = "true";
                return TAG_SERIAL_READY_SENDING;
            }
            e.printStackTrace();
            Log.i("TAG_READ_THREAD", e.getMessage());
        }
        if (data != null) {
            content = String.valueOf(data.get(KEY_Content));
            return Integer.parseInt(data.get(KEY_TAG));
        }
        return -1;
    }


    public void closeReadThread() throws IOException {
        isReading = false;
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
    }
}
