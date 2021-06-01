package com.library.lib_network.retrofit.netObserve

/**
 * 2020/12/03
 * 将网络传输过来的参数判断是否是true or false，false时返回对应的message
 */

suspend inline fun <T> netResult(block: suspend () -> BaseModel<T>): BaseModel<T?> {
    return try {
        block()
    } catch (e: Exception) {
        BaseModel(false, -1, e.message ?: "", null)
    }
}

/**
 * 将使用携程进行网络请求返回的错误问题进行赋值
 * continuation.resumeWithException(e)
 * @Deprecated()
 */
@Deprecated(message = "弃用，因为该方式是对原有的对象进行扩展，并不能屏蔽崩溃问题")
inline fun <T> BaseModel<T>.result(): BaseModel<T> {
    return try {
        this
    } catch (e: Exception) {
        //continuation.resumeWithException(e)报错
        BaseModel(false, -1, e.message ?: "", null)
    }
}