package com.mysafe.lib_serial_port.thread

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.mysafe.lib_serial_port.*
import com.mysafe.lib_serial_port.util.DataUtils
import com.mysafe.lib_serial_port.util.GsonUtil
import java.io.IOException
import java.io.InputStream

class ReadThread(
        private val handler: Handler,
        private var inputStream: InputStream?)
    : Thread() {
    private var data: Map<String, String>? = null
    private var isRead = true
    private var content = ""
    private var readContentState = TAG_READING_WAITING
    private val sb by lazy { StringBuilder() }
    private var strLength = 0
    fun close() {
        isRead = false
        //待机状态清空数据
        if (sb != null && sb.length > 0) {
            sb.delete(0, sb.length)
        }
    }

    fun setInputStream(inputStream: InputStream?) {
        this.inputStream = inputStream
    }

    override fun run() {
        while (isRead) {
            try {
                if (inputStream != null && inputStream!!.available() > 0) {
                    if (readContentState == TAG_READING_WAITING && sb.isNotEmpty()) {
                        //待机状态清空数据
                        //待机状态清空数据
                        if (sb != null && sb.length > 0) {
                            sb.delete(0, sb.length)
                        }
                    }

                    val bytes = ByteArray(Math.min(inputStream!!.available(), StandardState.MaxLengthOfTheReadContent))
                    strLength = inputStream!!.read(bytes)
                    if (strLength > 0) {
                        //byte数组转HexString
                        val hex = DataUtils.ByteArrToHex(bytes, 0, strLength)
                        //HexString转String
                        Log.d("TAG_READ_CONTENT", hex)
                        //合并16进制
                        mergeHexData(hex)
                        if (readContentState == TAG_READING_FINISH) {
                            val serialString = sb.toString()
                            Log.i("TAG_READ_FINISH", serialString)

                            if (serialString.length >= TAG_BEGINNING_HEX.length + TAG_ENDING_HEX.length) {
                                val singleStr = serialString.substring(TAG_BEGINNING_HEX.length, serialString.length - TAG_ENDING_HEX.length)
                                //16进制转String
                                val readData = DataUtils.hexStr2Str(singleStr)
                                getDataByTag(readData)
                            }

                            readContentState = TAG_READING_WAITING
                        }
                    }
                } else {
                    SystemClock.sleep(50)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun mergeHexData(readHexData: String) {
        when (getNowReadConditional(readHexData)) {
            TAG_READING_START, TAG_READING_CENTRE -> {
                readContentState = TAG_READING_WORKING
            }
            TAG_READING_END, TAG_READING_CONTENT -> {
                readContentState = TAG_READING_FINISH
            }
        }
    }

    //获取当前数据状态
    fun getNowReadConditional(content: String): Int {
        sb.append(content)
//        Log.i("TAG_READ_sb", "thread：${currentThread().name} $sb")
        return when {
            sb.startsWith(TAG_BEGINNING_HEX) && sb.endsWith(TAG_ENDING_HEX) -> {
                TAG_READING_CONTENT
            }
            !sb.contains(TAG_BEGINNING_HEX) && !sb.contains(TAG_ENDING_HEX) -> {
                TAG_READING_CENTRE
            }
            sb.startsWith(TAG_BEGINNING_HEX) -> {
                TAG_READING_START
            }
            sb.endsWith(TAG_ENDING_HEX) -> {
                TAG_READING_END
            }
            sb.contains(TAG_BE_GLUED_HEX) -> {
                TAG_READING_EMPTY
            }
            else -> 0
        }
    }

    //数据完整之后，数据分发状态
    private fun getDataByTag(readData: String) {
        val message = Message.obtain()
        val tag = getTagByData(readData)
        val bundleFace = Bundle()
        bundleFace.putString(tag.toString(), content)
        message.what = tag
        message.data = bundleFace
        handler.sendMessage(message)
    }

    private fun getTagByData(str: String): Int {
        try {
            data = GsonUtil.getInstance().jsonToMap(str)
        } catch (e: Exception) {
            if (str.contains("\"Tag\":\"19\"")) {
                //发起串口请求时,对端也在发送数据,导致数据冲突出现乱码
                content = "error"
                return TAG_SERIAL_ERROR
            }
            e.printStackTrace()
            Log.i("TAG_READ_THREAD", e.message)
        }
        if (data != null) {
            content = data!![KEY_CONTENT].toString()
            return data!![KEY_TAG]!!.toInt()
        }
        return -1
    }
}