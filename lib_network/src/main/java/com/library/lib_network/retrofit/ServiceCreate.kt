package com.library.lib_network.retrofit

import com.library.lib_network.retrofit.interceptor.CommonInterceptor
import com.library.lib_network.retrofit.interceptor.LogHttpInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.tencent.mmkv.MMKV

/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
object ServiceCreate {
    private val CONNECT_TIMEOUT = 30L
    private val READ_TIMEOUT = 40L
    private var retrofit: Retrofit? = null
    private val mmkv = MMKV.defaultMMKV()
    private val interceptor = LogHttpInterceptor()

    fun setRetrofit(baseUrl: String, deviceNo: String) {
        retrofit = Retrofit.Builder()
            .client(initHttpClient(deviceNo))
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .build()

    }

    private fun initHttpClient(deviceNo: String): OkHttpClient {
        return OkHttpClient()
            .newBuilder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(CommonInterceptor(deviceNo))
            .addInterceptor(interceptor)
            .build()
    }

    fun setInterceptorLevel(level: LogHttpInterceptor.Level) {
        interceptor.level = level
    }

    fun <T> create(service: Class<T>): T {
        if (retrofit == null) {
            val baseUrl = mmkv.decodeString("baseUrl")
            val deviceNo = mmkv.decodeString("deviceNo")
            setRetrofit(baseUrl, deviceNo)
        }
        if (interceptor.level != LogHttpInterceptor.Level.BODY){
            interceptor.level = LogHttpInterceptor.Level.BODY
        }
        return retrofit!!.create(service)
    }
}