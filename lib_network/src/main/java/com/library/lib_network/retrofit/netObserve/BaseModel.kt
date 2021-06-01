package com.library.lib_network.retrofit.netObserve

/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
data class BaseModel<out T>(
        /**
         * 是否成功
         */
        val isSuccess: Boolean = false,

        /**
         * 错误码
         */
        val code: Int = 0,

        /**
         * 信息
         */
        var message: String? = "",

        /**
         * 部分接口会携带的返回数据,数据类型根据对应的接口来定
         */
        val `data`: T? =null
//        val data: T? = null
)