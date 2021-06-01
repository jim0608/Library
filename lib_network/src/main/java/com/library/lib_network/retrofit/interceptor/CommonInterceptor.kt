package com.library.lib_network.retrofit.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * 添加Header
 */
class CommonInterceptor(private val deviceNo: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = addHeaders(request.newBuilder())
        return chain.proceed(builder)

    }

    private fun addHeaders(builder: Request.Builder): Request {
        return builder
                .addHeader("Content_Type", "application/json")
                .addHeader("charset", "UTF-8")
                .addHeader("deviceType","3")
                .addHeader("deviceNo",deviceNo)
                .build()
    }
}