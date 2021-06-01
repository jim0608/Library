package com.mysafe.lib_identification.helper

import com.mysafe.lib_identification.face_engin.EngineCodeEnum

/**
 * 初始化引擎失败成功回调
 */
interface IInitEngineCallback {

    /**
     * @param errorCode 错误码
     */
    fun onFailed(errorCode: Int)

    /**
     * 激活成功
     */
    fun onSuccess(engineCode: EngineCodeEnum)
}