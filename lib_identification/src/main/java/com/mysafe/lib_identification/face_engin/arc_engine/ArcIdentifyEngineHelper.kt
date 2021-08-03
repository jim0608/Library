package com.mysafe.lib_identification.face_engin.arc_engine

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.util.Size
import com.arcsoft.face.*
import com.arcsoft.face.enums.DetectFaceOrientPriority
import com.arcsoft.face.enums.DetectMode
import com.mysafe.lib_identification.config.DefaultConfig
import com.mysafe.lib_identification.config.RecognizeConfiguration
import com.mysafe.lib_identification.enums.LivenessType
import com.mysafe.lib_identification.enums.RecognizeColor
import com.mysafe.lib_identification.enums.RequestFeatureStatus
import com.mysafe.lib_identification.face_engin.EngineCodeEnum
import com.mysafe.lib_base.base.identify.FaceRectInfo
import com.mysafe.lib_identification.helper.*
import com.mysafe.lib_identification.model.CompareResult
import com.mysafe.lib_identification.model.FacePreviewInfo

/**
 * @author Create By 张晋铭
 * @Date on 2021/2/5
 * @Describe:虹软人脸识别引擎Helper，初始化等各种对象创建，各种逻辑处理
 */
class ArcIdentifyEngineHelper(private val mContext: Context) : IRecognizeCallBack {
    private val TAG = "TAG_FaceIdentifyHelper"

    //region 虹软操作
    /**
     * 人脸操作辅助类，推帧即可，内部会进行特征提取、识别
     */
    private var faceHelper: MsArcFaceHelper? = null

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪及图像质量检测
     */
    private var ftEngine: FaceEngine? = null

    /**
     * 用于特征提取的引擎
     */
    private var frEngine: FaceEngine? = null

    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private var flEngine: FaceEngine? = null

    /**
     * 相机预览的分辨率
     */
    private val previewSize: Size? = null

    /**
     * 当前活体检测的检测类型
     */
    private val livenessType = LivenessType.RGB

    // 填入在设置界面设置好的配置信息
    //活体检测
    private var enableLiveness = true

    //图像质量检查
    private var enableFaceQualityDetect = true

    //人脸识别结果监听
    private var recogniseCallback: IRecogniseResultCallback? = null

    //人脸数据信息
    private var faceInfoList: MutableList<FaceRectInfo> = ArrayList()
    private var faceSize: Int = -1

    //endregion
    /**
     * 人脸识别引擎枚举类
     */


    /**
     *
     * 初始化引擎
     */
    fun initEngine(initEngineCallback: IInitEngineCallback? = null) {
        val context: Context = mContext.applicationContext
        Log.d("TAG_Identify", "init: initEngine 1: " + SystemClock.currentThreadTimeMillis())
        //查看虹软是否激活

        Log.d("TAG_Identify", "init: initEngine 2: " + SystemClock.currentThreadTimeMillis())
        Log.d("TAG_Identify", "init: initEngine 3: " + SystemClock.currentThreadTimeMillis())
        initFtEngine(context, initEngineCallback)
        Log.d("TAG_Identify", "init: initEngine 3.1: " + SystemClock.currentThreadTimeMillis())
        initFrEngine(context, initEngineCallback)
        Log.d("TAG_Identify", "init: initEngine 3.2: " + SystemClock.currentThreadTimeMillis())
        // 启用活体检测时，才初始化活体引擎
        initFlEngine(context, initEngineCallback)
        Log.d("TAG_Identify", "init: initEngine 4: " + SystemClock.currentThreadTimeMillis())
    }

    private fun initFtEngine(context: Context, initEngineCallback: IInitEngineCallback?) {
        ftEngine = FaceEngine()
        var ftEngineMask = FaceEngine.ASF_FACE_DETECT
        if (enableFaceQualityDetect) {
            ftEngineMask = ftEngineMask or FaceEngine.ASF_IMAGEQUALITY
        }
        val ftInitCode = ftEngine!!.init(
            context,
            DetectMode.ASF_DETECT_MODE_VIDEO,
            DetectFaceOrientPriority.ASF_OP_ALL_OUT,
            16,
            DefaultConfig.DEFAULT_FACE_SIZE * 2,
            ftEngineMask
        )
        if (initEngineCallback != null) {
            if (ftInitCode != ErrorInfo.MOK) {
                initEngineCallback.onFailed(ftInitCode)
            } else {
                initEngineCallback.onSuccess(EngineCodeEnum.FT_ENGINE)
            }
        }
    }

    private fun initFrEngine(context: Context, initEngineCallback: IInitEngineCallback?) {
        frEngine = FaceEngine()
        val frInitCode = frEngine!!.init(
            context,
            DetectMode.ASF_DETECT_MODE_IMAGE,
            DetectFaceOrientPriority.ASF_OP_0_ONLY,
            16,
            DefaultConfig.DEFAULT_FACE_SIZE * 2,
            FaceEngine.ASF_FACE_RECOGNITION
        )
        if (initEngineCallback != null) {
            if (frInitCode != ErrorInfo.MOK) {
                initEngineCallback.onFailed(frInitCode)
            } else {
                initEngineCallback.onSuccess(EngineCodeEnum.FR_ENGINE)
            }
        }
    }

    private fun initFlEngine(context: Context, initEngineCallback: IInitEngineCallback?) {
        if (enableLiveness) {
            flEngine = FaceEngine()
            val flInitCode = flEngine!!.init(
                context,
                DetectMode.ASF_DETECT_MODE_IMAGE,
                DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16,
                DefaultConfig.DEFAULT_FACE_SIZE * 2,
                if (livenessType == LivenessType.RGB) FaceEngine.ASF_LIVENESS else FaceEngine.ASF_IR_LIVENESS
            )
            if (initEngineCallback != null) {
                if (flInitCode != ErrorInfo.MOK) {
                    initEngineCallback.onFailed(flInitCode)
                } else {
                    initEngineCallback.onSuccess(EngineCodeEnum.FL_ENGINE)
                }
            }
        }
    }

    /**
     * 加载引擎
     */
    fun initFaceHelper() {
        if (faceHelper == null) {
            var trackedFaceCount: Int? = null
            // 记录切换时的人脸序号
            if (faceHelper != null) {
                trackedFaceCount = faceHelper!!.getTrackedFaceCount()
                faceHelper!!.release()
            }


            Log.d("TAG_Identify", "init: initEngine 2.1: " + SystemClock.currentThreadTimeMillis())
            val configuration = RecognizeConfiguration.Builder()
                .enableLiveness(enableLiveness)
                .enableImageQuality(enableFaceQualityDetect)
                .maxDetectFaces(DefaultConfig.DEFAULT_FACE_SIZE)
                .keepMaxFace(DefaultConfig.DEFAULT_MAX_FACE)
                .similarThreshold(0.80f)
                .imageQualityThreshold(0.35f)
                .livenessParam(LivenessParam(0.50f, 0.70f))
                .build()

            faceHelper = MsArcFaceHelper.Builder()
                .ftEngine(ftEngine)
                .frEngine(frEngine)
                .flEngine(flEngine)
                .frQueueSize(DefaultConfig.DEFAULT_FACE_SIZE)
                .flQueueSize(DefaultConfig.DEFAULT_FACE_SIZE)
                .recognizeCallback(this)
                .recognizeConfiguration(configuration)
                .trackedFaceCount(trackedFaceCount ?: 0)
                .dualCameraFaceInfoTransformer { faceInfo: FaceInfo? ->
                    val irFaceInfo = FaceInfo(faceInfo)
                    irFaceInfo.rect.offset(0, 0)
                    irFaceInfo
                }
                .build()

        }
    }


    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    fun releaseEngine() {
        if (ftEngine != null) {
            synchronized(ftEngine!!) {
                ftEngine!!.unInit()
                ftEngine = null
            }
        }
        if (frEngine != null) {
            synchronized(frEngine!!) {
                frEngine!!.unInit()
                frEngine = null
            }
        }
        if (flEngine != null) {
            synchronized(flEngine!!) {
                flEngine!!.unInit()
                flEngine = null
            }
        }
        ArcInitEngine.INSTANCE.release()
    }

    /**
     * 传入可见光相机预览数据
     *
     * @param nv21        可见光相机预览数据
     * @param doRecognize 是否进行识别
     * @return 当前帧的检测结果信息
     */
    fun setPreviewFrame(
        nv21: ByteArray,
        widthSize: Int,
        heightSize: Int,
        doRecognize: Boolean
    ): MutableList<FaceRectInfo>? =
        if (faceHelper != null) {
            if (livenessType == LivenessType.IR) {
                null
            } else getFaceInfo(nv21, widthSize, heightSize, doRecognize)
        } else {
            null
        }

    private fun getFaceInfo(
        nv21: ByteArray,
        widthSize: Int,
        heightSize: Int,
        doRecognize: Boolean
    ): MutableList<FaceRectInfo> {

        val previewInfoList =
            faceHelper!!.onPreviewFrame(nv21, null, widthSize, heightSize, doRecognize)
        faceInfoList.clear()
        if (previewInfoList != null) {
            recogniseCallback?.onFaceSearch(previewInfoList.size > 0)
            faceSize = previewInfoList.size
        }
        if (previewInfoList != null && previewInfoList.size > 0) {
            for (previewInfo in previewInfoList) {
                faceInfoList.add(getFaceRectInfo(previewInfo))
            }
        }
        return faceInfoList
    }

    private fun getFaceRectInfo(previewInfo: FacePreviewInfo): FaceRectInfo {
        val trackId = previewInfo.trackId
        val name = faceHelper!!.getName(trackId)
        val liveness = faceHelper!!.GetLiveness(trackId)
        val recognizeStatus = faceHelper!!.GetRecognizeStatus(trackId)

        // 根据识别结果和活体结果设置颜色
        var color = RecognizeColor.Color_Unknown
        if (recognizeStatus != null) {
            if (recognizeStatus == RequestFeatureStatus.FAILED) {
                color = RecognizeColor.Color_Failed
            }
            if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                color = RecognizeColor.Color_Success
            }
        }
        if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
            color = RecognizeColor.Color_Failed
        }

        return FaceRectInfo(
            previewInfo.rgbTransformedRect,
            0,
            0,
            GenderInfo.UNKNOWN,
            AgeInfo.UNKNOWN_AGE,
            LivenessInfo.UNKNOWN,
            color,
            name ?: ""
        )
    }

    /**
     * 人脸识别Result监听
     */
    fun setRecognizeCallBack(recogniseCallback: IRecogniseResultCallback) {
        this.recogniseCallback = recogniseCallback
    }

    fun setRecognize() {
        faceHelper?.setRecognize()
    }

    override fun onRecognized(
        isHavePerson: Boolean,
        compareResult: CompareResult?,
        liveness: Int?,
        similarPass: Boolean
    ) {
        //识别未查询到人脸

        //识别未查询到人脸
        if (!isHavePerson) {
            recogniseCallback?.onFaceFailed()
        } else {
            //如果通过识别阈值
            val userNum = compareResult?.faceEntity?.userNum ?: ""
            val userName = compareResult?.faceEntity?.userName ?: ""
            recogniseCallback?.onFaceSuccess(
                compareResult?.trackId ?: 0,
                liveness == LivenessInfo.ALIVE,
                userNum,
                userName,
                similarPass
            )
        }
    }

    override fun onHaveNotFace() {
        recogniseCallback?.onNotFindFace()
        Log.i(TAG, "onHaveNotFace: ")
    }


}