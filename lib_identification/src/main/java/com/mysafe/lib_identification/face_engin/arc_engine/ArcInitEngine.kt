package com.mysafe.lib_identification.face_engin.arc_engine

import android.content.Context
import android.util.Log
import com.arcsoft.face.ErrorInfo
import com.arcsoft.face.FaceEngine
import com.arcsoft.face.FaceFeature
import com.arcsoft.face.FaceSimilar
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.mysafe.lib_base.sqlite.FaceDatabase
import com.mysafe.lib_base.sqlite.entity.FaceEntity
import com.mysafe.lib_identification.model.CompareResult

/**
 * @author Create By 张晋铭
 * @Date on 2021/3/10
 * @Describe:判断设备是否激活，同步人脸库
 */
class ArcInitEngine {
    private val TAG = "TAG_ArcInitEngine"
    private var faceEngine: FaceEngine? = null
    private var faceRegisterInfoList: MutableList<FaceEntity>? = null
    private val searchLock = Any()
    private var activeKey: String? = null
    private var appId: String? = null
    private var sdkKey: String? = null

    //region 单例
    @Volatile
    private var faceServer: ArcInitEngine? = null

    fun setEngineKey(activeKey: String, appId: String, sdkKey: String) {
        this.activeKey = activeKey
        this.appId = appId
        this.sdkKey = sdkKey
    }
//    GmNP65iNArFrr5i4vK4yB1Fegeg6TqyLeyvpZbBw3qJe
    //endregion
    @Synchronized
    fun activeOnlineEngine(context: Context): Int {
//        var activeResult = FaceEngine.activeOnline(context,
//                Constants.ARC_ACTIVE_KEY_CUSTOM, Constants.ARC_APP_ID, Constants.ARC_SDK_KEY)
        var activeResult = FaceEngine.activeOnline(context,
                activeKey,
                appId,
                sdkKey)
        Log.i(TAG, "activeOnlineEngine: code = $activeResult")
        when (activeResult) {
            ErrorInfo.MOK, ErrorInfo.MERR_ASF_ALREADY_ACTIVATED -> {
                Log.i(TAG, "activeOnlineEngine code :$activeResult")
                activeResult = initFaceEngine(context)
            }
            else -> {
                return activeResult
                Log.i(TAG, "activeOnline failed, code is : $activeResult")
            }
        }
        return activeResult

    }

    private fun initFaceEngine(context: Context): Int {
        if (faceEngine == null) {
            faceEngine = FaceEngine()
            val engineCode = faceEngine?.init(context,
                    DetectMode.ASF_DETECT_MODE_VIDEO,
                    DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                    16,
                    1,
                    FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT)
            Log.i(TAG, "initFaceEngine: code = $engineCode")

            //引擎已激活
            when (engineCode) {
                ErrorInfo.MOK, ErrorInfo.MERR_ASF_ALREADY_ACTIVATED -> {
                    //引擎打开成功
                    initFaceList(context)
                    Log.i(TAG, "init: success code = $engineCode")
                }
                //其他问题
                else -> {
                    //反馈出去
                    faceEngine?.unInit()
                    faceEngine = null
                    return engineCode!!
                    Log.e(TAG, "init: failed code = $engineCode")
                }
            }
        }
        return 0
    }

    private fun initFaceList(context: Context) {
        faceRegisterInfoList = FaceDatabase.getDatabase(context).faceDao().allFaces
//            for (list in faceRegisterInfoList!!){
//                list.featureData = UtilHelper.base64String2ByteFun("")
//            }
    }

    fun setFaceList(faceRegisterInfoList: MutableList<FaceEntity>) {
        this.faceRegisterInfoList?.clear()
        this.faceRegisterInfoList?.addAll(faceRegisterInfoList)
    }

    /**
     * 在特征库中搜索
     *
     * @param faceFeature 传入特征数据
     * @return 比对结果
     */
    fun getTopOfFaceLib(faceFeature: FaceFeature?): CompareResult? {
        if (faceEngine == null || faceFeature == null || faceRegisterInfoList == null || faceRegisterInfoList!!.isEmpty()) {
            return null
        }
        val start = System.currentTimeMillis()
        val tempFaceFeature = FaceFeature()
        val faceSimilar = FaceSimilar()
        var maxSimilar = 0f
        var maxSimilarIndex = -1
        var code = ErrorInfo.MOK
        synchronized(searchLock) {
            for (i in faceRegisterInfoList!!.indices) {
                tempFaceFeature.featureData = faceRegisterInfoList!![i].featureData
//                String str  = "{\"featureData\":[0,-128,-6,68,0,0,-96,65,-69,-49,77,61,-57,91,50,-71,22,30,-42,-67,-107,84,-122,61,-101,48,-111,60,-94,107,16,61,82,-49,-20,-68,38,-107,2,61,57,-117,122,-70,55,-71,83,-67,72,41,83,-67,-18,-64,13,62,-107,19,-78,-67,-87,-69,43,61,-13,98,-81,61,-65,-42,96,61,-48,38,-98,61,-70,-88,12,62,-23,44,38,-67,-17,102,-124,-68,-10,-34,-27,-67,-119,-28,29,-67,24,-18,50,61,-11,10,102,-67,-3,55,89,-68,75,-25,49,-68,-105,7,31,60,-127,76,81,-68,-125,-115,5,61,-54,-81,51,-66,-83,80,-49,60,123,-6,-83,61,104,-55,-84,-67,-19,-45,89,-67,75,-64,125,61,38,-41,26,59,-32,-114,8,61,-43,-122,-6,61,-64,41,118,61,-18,-71,6,-66,-8,-70,-96,-67,-17,-11,-123,60,37,-91,-128,60,-86,-99,-123,61,120,25,-4,-68,-121,84,-88,-67,39,94,4,62,10,49,-116,-68,62,57,-95,-67,-51,-55,7,-67,123,55,35,61,-113,-55,4,-66,-58,13,8,-66,11,-62,-83,-68,-17,-101,-77,-68,38,95,26,61,38,115,17,60,-47,94,-111,60,-28,-37,-127,60,98,10,-15,58,-82,76,-62,-68,-15,-9,-85,-67,94,36,-53,-68,24,-19,63,-67,-98,31,113,-67,-59,17,-9,60,81,22,104,61,79,91,120,61,26,46,-29,-69,-115,-124,21,-67,-121,123,60,-70,-93,124,-26,60,-126,-67,97,61,-86,-101,-109,61,83,1,-122,59,99,-19,27,-67,93,-6,-50,61,38,-51,101,-67,-1,14,-106,-67,-79,82,24,61,-111,61,-57,61,-13,60,12,-67,122,23,-38,-67,-95,36,-6,-68,-118,67,113,-67,-62,-81,-102,60,24,51,-73,-68,89,69,-97,61,-77,2,-63,-70,-102,-110,-65,61,-103,71,-97,-67,-118,29,-96,61,18,-14,-52,57,38,13,-107,61,119,-28,6,61,2,114,-120,-68,66,45,69,-67,42,28,6,-69,21,-91,-121,61,-97,61,1,-68,-115,-78,85,-68,-69,80,-114,-67,-69,70,-93,-67,18,90,-42,-68,56,92,35,-66,-45,-27,-104,-68,-104,15,81,61,21,93,-117,59,-63,12,21,-66,46,-125,68,-67,58,-102,8,-66,53,45,-15,-68,-119,-24,-14,-68,118,9,23,-68,40,-61,-59,61,-42,37,84,61,-13,-88,-52,59,107,100,-43,-68,106,87,37,61,-51,-78,50,61,-34,109,65,-68,-79,-104,-64,-67,51,-126,-128,61,-29,-33,23,60,19,67,-27,60,115,42,1,-66,-34,84,-11,-67,5,-49,-64,61,-35,126,126,-68,75,118,41,-67,-10,-122,107,60,-61,47,-82,-67,-1,37,-78,61,17,-32,7,61,-105,63,124,-67,40,-7,72,61,-21,103,-70,-71,-30,-98,96,61,62,127,90,-67,85,-102,-109,60,-41,-70,52,-67,-42,-24,118,60,-121,120,-33,-68,75,42,-60,60,114,-127,56,-67,-109,-8,-78,61,80,-37,94,60,-126,13,-59,-68,33,-39,-83,61,55,25,105,-67,-125,-28,-14,-67,38,111,-21,61,-89,111,-80,-69,34,-11,-99,-67,-101,-38,-89,-67,106,10,-53,-68,6,55,-80,-68,97,-51,-90,61,120,-82,-108,-67,30,-9,-102,-70,-43,-26,117,-67,40,-104,31,61,-66,111,38,-67,-107,-104,76,60,70,114,-100,-67,119,121,-108,61,-83,-7,-78,-68,-75,-121,-86,-68,109,-66,-77,-68,-75,-101,97,58,-42,70,-100,61,11,-38,25,61,-55,-37,-43,61,-93,-61,22,60,-27,-113,-111,-68,-94,55,-69,-71,88,2,-35,60,25,-13,24,61,0,43,61,-68,51,8,-82,-68,-71,58,104,-67,-118,-121,22,61,98,-26,-27,60,115,-124,116,61,-13,71,-116,61,-110,-64,-9,60,107,64,7,-69,2,-110,-12,59,23,-73,-127,60,-85,-49,-101,61,79,-119,13,62,-98,-4,87,-68,21,110,78,61,81,-23,39,61,-73,-29,31,61,94,49,-57,-68,-33,8,118,61,-91,-93,-16,-67,39,84,-107,-68,81,-68,118,-68,117,110,97,-68,-38,-9,-82,-68,18,34,-97,-69,-41,-14,-27,60,107,-21,52,61,-53,-67,-7,59,-59,-73,-25,61,77,-11,29,61,0,-28,40,60,6,70,73,-67,-95,-118,46,-67,-13,-51,-16,-68,-69,24,-110,-68,97,-10,76,-66,-25,44,98,60,-123,-49,64,-67,5,-45,-14,60,-86,73,62,61,41,-72,-58,60,102,-98,-113,60,-78,100,-25,58,106,-31,-79,61,-16,27,64,-67,66,46,-3,-68,-114,-16,5,-67,82,21,-66,-67,14,88,94,-67,-101,98,-91,61,-105,-76,63,61,56,39,26,61,99,-51,-70,61,67,48,-82,61,96,6,-36,60,-90,-114,-121,59,70,99,-110,-67,-31,96,-89,61,21,-23,-105,-68,-80,-108,-24,60,105,24,-78,-67,-71,87,-106,-67,-80,-21,-52,61,-38,4,118,61,3,20,-33,61,16,55,-38,-67,-70,-22,-26,-69,-40,32,-79,61,-13,-24,120,-67,22,-48,-125,61,69,-52,-72,-68,-35,-101,14,-66,66,34,91,61,-10,28,-64,-67,-120,23,83,60,-118,121,-51,-67,49,-59,103,-68,-98,86,-71,-69]}";
//
//                faceFeature = new Gson().fromJson(str,FaceFeature.class);

                //视频流比对，相比而言更加快速
                code = faceEngine!!.compareFaceFeature(faceFeature, tempFaceFeature, faceSimilar)
                if (faceSimilar.score > maxSimilar) {
                    maxSimilar = faceSimilar.score
                    maxSimilarIndex = i
                }
            }
        }
        return if (maxSimilarIndex != -1) {
            CompareResult(faceRegisterInfoList!![maxSimilarIndex], maxSimilar, code, System.currentTimeMillis() - start)
        } else null
    }


    /**
     * 销毁
     */
    @Synchronized
    fun release() {
        if (faceRegisterInfoList != null) {
            faceRegisterInfoList!!.clear()
            faceRegisterInfoList = null
        }
        if (faceEngine != null) {
            synchronized(faceEngine!!) { faceEngine!!.unInit() }
            faceEngine = null
        }
        faceServer = null
    }

    companion object {
        val INSTANCE by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ArcInitEngine()
        }
    }
}