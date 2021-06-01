package com.library.lib_network

import retrofit2.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
open class PublicRequestNetwork {

    //region 协程网络请求，并获取返回结果
    suspend fun <T : Any> Call<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body == null) {
                            val invocation = call.request().tag(Invocation::class.java)!!
                            val method = invocation.method()
                            val e = KotlinNullPointerException("Response from " +
                                    method.declaringClass.name +
                                    '.' +
                                    method.name +
                                    " was null but response body type was declared as non-null")
                            continuation.resumeWithException(e)
                        } else {
                            continuation.resume(body)
                        }
                    } else {
                        continuation.resumeWithException(HttpException(response))
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    @JvmName("awaitNullable")
    suspend fun <T : Any> Call<T?>.await(): T? {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }
            enqueue(object : Callback<T?> {
                override fun onResponse(call: Call<T?>, response: Response<T?>) {
                    if (response.isSuccessful) {
                        continuation.resume(response.body())
                    } else {
                        continuation.resumeWithException(HttpException(response))
                    }
                }

                override fun onFailure(call: Call<T?>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    //endregion
}

