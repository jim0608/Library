package com.mysafe.lib_mqtt

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.jimz.mqtt", appContext.packageName)
    }

    @Test
    fun getMacTest(){
        val  ss = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address")
        val `is`: InputStream = ss.getInputStream()
        val isr = InputStreamReader(`is`)

        print(isr)
        print(getMac())
    }

    fun getMac(): String {
        try {
            val all: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!"wlan0".equals(nif.getName(), ignoreCase = true)) {
                    continue
                }
                val macBytes: ByteArray = nif.getHardwareAddress()
                if (macBytes == null || macBytes.size == 0) {
                    continue
                }
                val result = StringBuilder()
                for (b in macBytes) {
                    result.append(String.format("%02X", b))
                }
                return result.toString().toUpperCase()
            }
        } catch (x: Exception) {
            x.printStackTrace()
        }
        return ""
    }
}