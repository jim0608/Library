package com.mysafe.lib_identification.face_engin.arc_engine

import android.content.Context
import android.util.Log
import com.mysafe.lib_identification.face_engin.CameraFaceEngine
import com.mysafe.lib_base.base.identify.FaceRectInfo
import com.mysafe.lib_identification.helper.IRecogniseResultCallback

/**
 * @author Create By 张晋铭
 * @Date on 2021/2/6
 * @Describe:虹软人脸识别引擎调用,引擎初始化，销毁，数据传递，结果返回
 */
class ArcIdentifyEngine(private val mContext: Context) : CameraFaceEngine() {
    private var arcEngineHelper: ArcIdentifyEngineHelper? = null
    private val TAG = "TAG_ArcIdentifyEngine"
    override fun initEngine() {
//        Runnable {  }
        Log.i(TAG, "initEngine: ")
        arcEngineHelper = ArcIdentifyEngineHelper(mContext)
        arcEngineHelper!!.initEngine()
        arcEngineHelper?.initFaceHelper()
    }

    override fun setPreviewFrame(nv21: ByteArray, widthSize: Int, heightSize: Int): MutableList<FaceRectInfo>? {
//        Log.i(TAG, "setPreviewFrame: ")

        return arcEngineHelper?.setPreviewFrame(nv21, widthSize, heightSize, true)
    }

    override fun setIdentifyResult(recogniseCallback: IRecogniseResultCallback) {
        arcEngineHelper?.setRecognizeCallBack(recogniseCallback)
        Log.i(TAG, "setIdentifyResult: ")
    }

    override fun releaseEngine() {
        Log.i(TAG, "releaseEngine: ")
        arcEngineHelper?.releaseEngine()
    }

    fun setRecognize(){
        arcEngineHelper?.setRecognize()
    }
}