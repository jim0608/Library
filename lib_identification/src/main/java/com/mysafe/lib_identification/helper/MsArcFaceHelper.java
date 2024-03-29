package com.mysafe.lib_identification.helper;

import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.LivenessInfo;
import com.mysafe.lib_identification.camera.FaceRectHelper;
import com.mysafe.lib_identification.enums.LivenessType;
import com.mysafe.lib_identification.enums.RequestFeatureStatus;
import com.mysafe.lib_identification.enums.RequestLivenessStatus;
import com.mysafe.lib_identification.face_engin.arc_engine.ArcInitEngine;
import com.mysafe.lib_identification.model.CompareResult;
import com.mysafe.lib_identification.model.FacePreviewInfo;
import com.mysafe.lib_identification.config.RecognizeConfiguration;
import com.mysafe.lib_identification.model.FaceSearchResult;
import com.mysafe.lib_identification.model.RecognizeInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.mysafe.lib_identification.enums.RequestFeatureStatus.FAILED;
import static com.mysafe.lib_identification.enums.RequestFeatureStatus.SEARCHING;

public class MsArcFaceHelper implements IFaceListener {
    private static final String TAG = "FaceHelper";

    /**
     * 识别结果回调
     */
    private IRecognizeCallBack recognizeCallback;

    /**
     * 用于记录人脸识别过程信息
     */
    private ConcurrentHashMap<Integer, RecognizeInfo> recognizeInfoMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();

    /**
     * 转换方式，用于IR活体检测
     */
    private IDualCameraFaceInfoTransformer dualCameraFaceInfoTransformer;

    /**
     * 线程池正在处理任务
     */
    private static final int ERROR_BUSY = -1;
    /**
     * 特征提取引擎为空
     */
    private static final int ERROR_FR_ENGINE_IS_NULL = -2;
    /**
     * 活体检测引擎为空
     */
    private static final int ERROR_FL_ENGINE_IS_NULL = -3;
    /**
     * 人脸追踪引擎
     */
    private FaceEngine ftEngine;
    /**
     * 特征提取引擎
     */
    private FaceEngine frEngine;
    /**
     * 活体检测引擎
     */
    private FaceEngine flEngine;


    private List<FaceInfo> faceInfoList = new CopyOnWriteArrayList<>();
    private List<Float> imageQualityList = new CopyOnWriteArrayList<>();
    /**
     * 特征提取线程池
     */
    private ExecutorService frExecutor;
    /**
     * 活体检测线程池
     */
    private ExecutorService flExecutor;
    /**
     * 特征提取线程队列
     */
    private LinkedBlockingQueue<Runnable> frThreadQueue = null;
    /**
     * 活体检测线程队列
     */
    private LinkedBlockingQueue<Runnable> flThreadQueue = null;

    private FaceRectHelper rgbFaceRectTransformer;
    private FaceRectHelper irFaceRectTransformer;
    /**
     * 控制可识别区域（相对于View），若未设置，则是全部区域
     */
    private Rect recognizeArea = new Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /**
     * 上次应用退出时，记录的该App检测过的人脸数了
     */
    private int trackedFaceCount = 0;
    /**
     * 本次打开引擎后的最大faceId
     */
    private int currentMaxFaceId = 0;
    /**
     * 相机中是否存在人脸
     */
    private boolean isHaveFace = true;

    /**
     * 预览页是否有人脸
     */
    private int faceSize = -1;
    /**
     * 识别成功的时间，当前时间和successTime相差5s后将Success状态置为RE_TRY
     */
    private long successTime = 0;

    /**
     * 识别成功后手动控制，是否重置识别装填
     */
    private boolean isRecognize = false;

    /**
     * 识别的配置项
     */
    private RecognizeConfiguration recognizeConfiguration;

    private List<Integer> currentTrackIdList = new ArrayList<>();
    private List<FacePreviewInfo> facePreviewInfoList = new ArrayList<>();


    private MsArcFaceHelper(Builder builder) {
        ftEngine = builder.ftEngine;
        trackedFaceCount = builder.trackedFaceCount;
        frEngine = builder.frEngine;
        flEngine = builder.flEngine;
        recognizeCallback = builder.recognizeCallback;
        recognizeConfiguration = builder.recognizeConfiguration;
        dualCameraFaceInfoTransformer = builder.dualCameraFaceInfoTransformer;
        /**
         * fr 线程队列大小
         */
        int frQueueSize = recognizeConfiguration.getMaxDetectFaces();
        if (builder.frQueueSize > 0) {
            frQueueSize = builder.frQueueSize;
        } else {
            Log.e(TAG, "frThread num must > 0, now using default value:" + frQueueSize);
        }
        frThreadQueue = new LinkedBlockingQueue<>(frQueueSize);
        frExecutor = new ThreadPoolExecutor(1, frQueueSize, 0, TimeUnit.MILLISECONDS, frThreadQueue, r -> {
            Thread t = new Thread(r);
            t.setName("frThread-" + t.getId());
            return t;
        });

        /**
         * fl 线程队列大小
         */
        int flQueueSize = recognizeConfiguration.getMaxDetectFaces();
        if (builder.flQueueSize > 0) {
            flQueueSize = builder.flQueueSize;
        } else {
            Log.e(TAG, "flThread num must > 0, now using default value:" + flQueueSize);
        }
        flThreadQueue = new LinkedBlockingQueue<Runnable>(flQueueSize);
        flExecutor = new ThreadPoolExecutor(1, flQueueSize, 0, TimeUnit.MILLISECONDS, flThreadQueue, r -> {
            Thread t = new Thread(r);
            t.setName("flThread-" + t.getId());
            return t;
        });

    }

    /**
     * 请求获取人脸特征数据
     *
     * @param nv21     图像数据
     * @param faceInfo 人脸信息
     * @param width    图像宽度
     * @param height   图像高度
     * @param format   图像格式
     * @param trackId  请求人脸特征的唯一请求码，一般使用trackId
     */
    public void RequestFaceFeature(byte[] nv21, FaceInfo faceInfo, int width, int height, int format, Integer trackId) {
        if (frEngine != null && frThreadQueue.remainingCapacity() > 0) {
            frExecutor.execute(new FaceRecognizeRunnable(nv21, faceInfo, width, height, format, trackId));
        } else {
            onFaceFeatureInfoGet(null, trackId, ERROR_BUSY);
        }
    }

    /**
     * 请求获取活体检测结果，需要传入活体的参数，以下参数同
     *
     * @param nv21         NV21格式的图像数据
     * @param faceInfo     人脸信息
     * @param width        图像宽度
     * @param height       图像高度
     * @param format       图像格式
     * @param trackId      请求人脸特征的唯一请求码，一般使用trackId
     * @param livenessType 活体检测类型
     * @param waitLock
     */
    public void RequestFaceLiveness(byte[] nv21, FaceInfo faceInfo, int width, int height, int format, Integer trackId, int livenessType, Object waitLock) {
        if (flEngine != null && flThreadQueue.remainingCapacity() > 0) {
            flExecutor.execute(new FaceLivenessDetectRunnable(nv21, faceInfo, width, height, format, trackId, livenessType, waitLock));
        } else {
            onFaceLivenessInfoGet(null, trackId, ERROR_BUSY);
        }
    }

    /**
     * 释放对象
     */
    public void release() {
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (!frExecutor.isShutdown()) {
            frExecutor.shutdownNow();
            frThreadQueue.clear();
        }
        if (!flExecutor.isShutdown()) {
            flExecutor.shutdownNow();
            flThreadQueue.clear();
        }
        if (faceInfoList != null) {
            faceInfoList.clear();
        }
        if (frThreadQueue != null) {
            frThreadQueue.clear();
            frThreadQueue = null;
        }
        if (flThreadQueue != null) {
            flThreadQueue.clear();
            flThreadQueue = null;
        }
        faceInfoList = null;
    }

    public void setRecognize() {
        isRecognize = true;
    }

    /**
     * 处理帧数据,将相机的帧数据进行人脸引擎识别并转换
     *
     * @param rgbNV21     可见光相机预览回传的NV21数据
     * @param irNV21      红外相机预览回传的NV21数据
     * @param doRecognize 是否进行识别
     * @return 实时人脸处理结果，封装添加了一个trackId，trackId的获取依赖于faceId，用于记录人脸序号并保存
     */
    public List<FacePreviewInfo> onPreviewFrame(@NonNull byte[] rgbNV21, @Nullable byte[] irNV21, int widthSize, int heightSize, boolean doRecognize) {
        if (ftEngine != null) {
            faceInfoList.clear();
            facePreviewInfoList.clear();

            int nv21Size = widthSize * heightSize * 3 / 2;
            if (rgbNV21.length != nv21Size && nv21Size == 0) {
                return facePreviewInfoList;
            }
            int code = ftEngine.detectFaces(rgbNV21, widthSize, heightSize, FaceEngine.CP_PAF_NV21, faceInfoList);
            if (code != ErrorInfo.MOK) {
                onFail(new Exception("ft failed,code is " + code));
            } else {
//                    Log.i(TAG, "onPreviewFrame: ft costTime = " + (System.currentTimeMillis() - ftStartTime) + "ms");
            }

            if (recognizeConfiguration.isKeepMaxFace()) {
                keepMaxFace(faceInfoList);
            }
            refreshTrackId(faceInfoList);

            for (int i = 0; i < faceInfoList.size(); i++) {
                FacePreviewInfo facePreviewInfo = new FacePreviewInfo(faceInfoList.get(i), currentTrackIdList.get(i));
                if (recognizeArea != null) {
                    Rect rect = faceInfoList.get(i).getRect();
                    facePreviewInfo.setRgbTransformedRect(rect);
                    facePreviewInfo.setRecognizeAreaValid(recognizeArea.contains(rect));
                }
                if (irFaceRectTransformer != null && recognizeArea != null) {
                    FaceInfo faceInfo = faceInfoList.get(i);
                    if (dualCameraFaceInfoTransformer != null) {
                        faceInfo = dualCameraFaceInfoTransformer.transformFaceInfo(faceInfo);
                    }
                    facePreviewInfo.setFaceInfoIr(faceInfo);
                    facePreviewInfo.setIrTransformedRect(irFaceRectTransformer.adjustRect(faceInfo.getRect()));
                    Rect rect = irFaceRectTransformer.adjustRect(faceInfoList.get(i).getRect());
                    facePreviewInfo.setIrTransformedRect(rect);
                    facePreviewInfo.setRecognizeAreaValid(recognizeArea.contains(rect));
                }
                facePreviewInfoList.add(facePreviewInfo);
            }
            clearLeftFace(facePreviewInfoList);
            if (doRecognize) {
                if (recognizeConfiguration.isEnableImageQuality()) {
                    int imageQualityDetectCode = ftEngine.imageQualityDetect(rgbNV21, widthSize, heightSize, FaceEngine.CP_PAF_NV21, faceInfoList, imageQualityList);
                    if (imageQualityDetectCode == ErrorInfo.MOK) {
                        for (int i = 0; i < imageQualityList.size(); i++) {
                            facePreviewInfoList.get(i).setImageQuality(imageQualityList.get(i));
                        }
                    }
//                    else {
//                        Log.e(TAG, "imageQualityDetect code: " + imageQualityDetectCode);
//                    }
                }
                DoRecognize(rgbNV21, irNV21, widthSize, heightSize, facePreviewInfoList);
            }
            return facePreviewInfoList;
        } else {
            facePreviewInfoList.clear();
            return facePreviewInfoList;
        }

    }

    public void setRgbFaceRectTransformer(FaceRectHelper rgbFaceRectTransformer) {
        this.rgbFaceRectTransformer = rgbFaceRectTransformer;
    }

    public void setIrFaceRectTransformer(FaceRectHelper irFaceRectTransformer) {
        this.irFaceRectTransformer = irFaceRectTransformer;
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
        }
        Enumeration<Integer> keys = recognizeInfoMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                RecognizeInfo recognizeInfo = recognizeInfoMap.remove(key);
                if (recognizeInfo != null) {
                    // 人脸离开时，通知特征提取线程，避免一直等待活体结果
                    synchronized (recognizeInfo.getWaitLock()) {
                        recognizeInfo.getWaitLock().notifyAll();
                    }
                }
            }
        }
    }

    private void isHavePersonFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (faceSize != facePreviewInfoList.size()) {
            faceSize = facePreviewInfoList.size();
//            recognizeCallback.onFaceSearch(rgbDrawInfoList.size() > 0);
        }
    }

    /**
     * 执行识别逻辑
     *
     * @param nv21                图片流
     * @param irNv21              IR图片流
     * @param facePreviewInfoList
     */
    private void DoRecognize(byte[] nv21, byte[] irNv21, int widthSize, int heightSize, List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && widthSize > 0) {
            for (int i = 0; i < facePreviewInfoList.size(); i++) {
                //备注：识别成功之后RequestFeatureStatus.SUCCEED，这里面就执行不到任何逻辑了
                FacePreviewInfo facePreviewInfo = facePreviewInfoList.get(i);
                if (recognizeConfiguration.isEnableImageQuality() && facePreviewInfo.getImageQuality() < recognizeConfiguration.getImageQualityThreshold()) {
                    Log.d("TAG_FACE", "face image quality is low:" + facePreviewInfo.getImageQuality());
                    continue;//人脸图像的质量是否达到标准
                }

                if (!facePreviewInfo.isRecognizeAreaValid()) {//识别区域是否合法
                    Log.d("TAG_FACE", "face area is illegal");
                    continue;
                }
//                Log.d("TAG_FACE","face image quality is Ok:"+facePreviewInfo.getImageQuality());
                RecognizeInfo recognizeInfo = GetRecognizeInfo(recognizeInfoMap, facePreviewInfo.getTrackId());
                int status = recognizeInfo.getRecognizeStatus();
                /**
                 * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                 */

                if (recognizeConfiguration.isEnableLiveness() && status != RequestFeatureStatus.SUCCEED) {
                    int liveness = recognizeInfo.getLiveness();
                    if (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING) {
                        ChangeLiveness(facePreviewInfo.getTrackId(), RequestLivenessStatus.ANALYZING);
                        RequestFaceLiveness(
                                irNv21 == null ? nv21 : irNv21,
                                facePreviewInfo.getFaceInfoRgb(),
                                widthSize, heightSize,
                                FaceEngine.CP_PAF_NV21,
                                facePreviewInfo.getTrackId(),
                                irNv21 == null ? LivenessType.RGB : LivenessType.IR,
                                recognizeInfo.getWaitLock()
                        );
                    }
                } else if (status == RequestFeatureStatus.SUCCEED) {
                    //识别成功20s后，重置状态
                    long time = System.currentTimeMillis() - successTime;
                    if (time > 20000 || isRecognize) {
                        Log.i(TAG, "DoRecognize: isRecognize:"+isRecognize +" time > 20000 :"+time);
                        isRecognize = false;
                        RetryRecognizeDelayed(facePreviewInfo.getTrackId(), SEARCHING);
                    }
                }
                /**
                 * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                 * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                 */
                if (status == RequestFeatureStatus.TO_RETRY) {
                    ChangeRecognizeStatus(facePreviewInfo.getTrackId(), SEARCHING);
                    RequestFaceFeature(
                            nv21, facePreviewInfo.getFaceInfoRgb(),
                            widthSize, heightSize,
                            FaceEngine.CP_PAF_NV21, facePreviewInfo.getTrackId()
                    );
                }
            }
            isHaveFace = true;
        } else if (isHaveFace) {
            isHaveFace = false;
            recognizeCallback.onHaveNotFace();
        }
    }

    @Override
    public void onFail(Exception e) {

    }

    /**
     * 获取识别信息，识别信息为空则创建一个新的
     *
     * @param recognizeInfoMap 存放识别信息的map
     * @param trackId          人脸唯一标识
     * @return 识别信息
     */
    public RecognizeInfo GetRecognizeInfo(Map<Integer, RecognizeInfo> recognizeInfoMap, int trackId) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        if (recognizeInfo == null) {
            recognizeInfo = new RecognizeInfo();
            recognizeInfoMap.put(trackId, recognizeInfo);
        }
        return recognizeInfo;
    }

    @Override
    public void onFaceFeatureInfoGet(@Nullable FaceFeature faceFeature, Integer
            trackId, Integer errorCode) {
        //FR成功
        if (faceFeature != null) {
            RecognizeInfo recognizeInfo = GetRecognizeInfo(recognizeInfoMap, trackId);
            // 人脸已离开，不用处理
            if (recognizeInfo == null) {
                return;
            }
            //不做活体检测的情况，直接搜索
            if (!recognizeConfiguration.isEnableLiveness()) {
                SearchFace(faceFeature, trackId);
            }
            //活体检测通过，搜索特征
            else if (recognizeInfo.getLiveness() == LivenessInfo.ALIVE) {
                SearchFace(faceFeature, trackId);
            }
            //活体检测未出结果，或者非活体，等待
            else {
                synchronized (recognizeInfo.getWaitLock()) {
                    try {
                        recognizeInfo.getWaitLock().wait();
                        if (recognizeInfoMap.containsKey(trackId)) {
                            onFaceFeatureInfoGet(faceFeature, trackId, errorCode);
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "onFaceFeatureInfoGet: 等待活体结果时退出界面会执行，正常现象，可注释异常代码块");
                        e.printStackTrace();
                    }
                }
            }

        }
        //特征提取失败时，为了及时提示做个UI反馈，将name修改为"ExtractCode:${errorCode}"，再重置状态
        else {
            RecognizeInfo recognizeInfo = GetRecognizeInfo(recognizeInfoMap, trackId);

            if (recognizeInfo.increaseAndGetExtractErrorRetryCount() > recognizeConfiguration.getExtractRetryCount()) {
                recognizeInfo.setExtractErrorRetryCount(0);
                String msg = "ExtractCode:" + errorCode;
                setName(trackId, msg);
                // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                ChangeRecognizeStatus(trackId, FAILED);
                RetryRecognizeDelayed(trackId,SEARCHING);
            } else {
                ChangeRecognizeStatus(trackId, RequestFeatureStatus.TO_RETRY);
            }
        }

    }

    /**
     * 延迟 {@link RecognizeConfiguration#getLivenessFailedRetryInterval()}后，重新进行活体检测
     *
     * @param trackId 人脸ID
     */
    private void RetryLivenessDetectDelayed(final Integer trackId) {
        Observable.timer(recognizeConfiguration.getLivenessFailedRetryInterval(), TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        setName(trackId, Integer.toString(trackId));
                        ChangeLiveness(trackId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 {@link RecognizeConfiguration#getRecognizeFailedRetryInterval()}后，重新进行人脸识别
     *
     * @param trackId 人脸ID
     */
    private void RetryRecognizeDelayed(final Integer trackId,int newStatus) {
        ChangeRecognizeStatus(trackId, newStatus);
        Observable.timer(recognizeConfiguration.getRecognizeFailedRetryInterval(), TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        setName(trackId, Integer.toString(trackId));
                        ChangeRecognizeStatus(trackId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    @Override
    public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, Integer
            trackId, Integer errorCode) {
        if (livenessInfo != null) {
            int liveness = livenessInfo.getLiveness();
            ChangeLiveness(trackId, liveness);
            // 非活体，重试
            if (liveness == LivenessInfo.NOT_ALIVE) {
                setName(trackId, "NOT_ALIVE");
                // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                RetryLivenessDetectDelayed(trackId);
            }
        } else {
            RecognizeInfo recognizeInfo = GetRecognizeInfo(recognizeInfoMap, trackId);
            // 连续多次活体检测失败（接口调用回传值非0），将活体检测值重置为未知，会在帧回调中重新进行活体检测
            if (recognizeInfo.increaseAndGetLivenessErrorRetryCount() > recognizeConfiguration.getLivenessRetryCount()) {
                recognizeInfo.setLivenessErrorRetryCount(0);
                String msg = "ProcessCode:" + errorCode;
                setName(trackId, msg);
                RetryLivenessDetectDelayed(trackId);
            } else {
                ChangeLiveness(trackId, LivenessInfo.UNKNOWN);
            }
        }
    }

    private void SearchFace(final FaceFeature faceFeature, final Integer trackId) {
        Observable
                .create((ObservableOnSubscribe<FaceSearchResult>) emitter -> {
                    CompareResult compareResult = ArcInitEngine.Companion.getINSTANCE().getTopOfFaceLib(faceFeature);
                    FaceSearchResult result = new FaceSearchResult(compareResult);
                    if (faceFeature.getFeatureData().length > 0 && compareResult == null) {
                        result.setSuccess(false);
                    } else {
                        result.setSuccess(true);
                    }
                    emitter.onNext(result);
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FaceSearchResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FaceSearchResult searchResult) {
                        if (!searchResult.isSuccess() || searchResult.getResult() == null || searchResult.getResult().getFaceEntity() == null) {
                            ChangeRecognizeStatus(trackId, FAILED);
                            recognizeCallback.onRecognized(false, null, null, false);
                        } else {
                            searchResult.getResult().setTrackId(trackId);
                            boolean pass = searchResult.getResult().getSimilar() > recognizeConfiguration.getSimilarThreshold();
                            Log.d("TAG_FACE_SIMILAR", "Similar:" + searchResult.getResult().getSimilar());
                            if (pass) {
                                recognizeCallback.onRecognized(true, searchResult.getResult(), GetRecognizeInfo(recognizeInfoMap, trackId).getLiveness(), pass);
                                setName(trackId, "通过：" + searchResult.getResult().getFaceEntity().getUserNum());
                                ChangeRecognizeStatus(trackId, RequestFeatureStatus.SUCCEED);
                                successTime = System.currentTimeMillis();
                                isRecognize = false;
                            } else {//在这里相同的trackId失败三次提示识别失败
                                setName(trackId, "未通过：NOT_REGISTERED");
                                RetryRecognizeDelayed(trackId,FAILED);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        setName(trackId, "未通过：NOT_REGISTERED");
                        RetryRecognizeDelayed(trackId,FAILED);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 刷新trackId
     *
     * @param ftFaceList 传入的人脸列表
     */
    private void refreshTrackId(List<FaceInfo> ftFaceList) {
        currentTrackIdList.clear();
        for (FaceInfo faceInfo : ftFaceList) {
            currentTrackIdList.add(faceInfo.getFaceId() + trackedFaceCount);
        }
        if (ftFaceList.size() > 0) {
            currentMaxFaceId = ftFaceList.get(ftFaceList.size() - 1).getFaceId();
        }
    }

    /**
     * 获取当前的最大trackID,可用于退出时保存
     *
     * @return 当前trackId
     */
    public int getTrackedFaceCount() {
        // 引擎的人脸下标从0开始，因此需要+1
        return trackedFaceCount + currentMaxFaceId + 1;
    }

    /**
     * 新增搜索成功的人脸
     *
     * @param trackId 指定的trackId
     * @param name    trackId对应的人脸
     */
    public void setName(int trackId, String name) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        if (recognizeInfo != null) {
            recognizeInfo.setName(name);
        }
    }

    /**
     * 设置转换方式，用于IR活体检测
     *
     * @param transformer 转换方式
     */
    public void setDualCameraFaceInfoTransformer(IDualCameraFaceInfoTransformer transformer) {
        this.dualCameraFaceInfoTransformer = transformer;
    }


    public String getName(int trackId) {
        RecognizeInfo recognizeInfo = recognizeInfoMap.get(trackId);
        return recognizeInfo == null ? null : recognizeInfo.getName();
    }


    /**
     * 设置可识别区域（相对于View）
     *
     * @param recognizeArea 可识别区域
     */
    public void setRecognizeArea(Rect recognizeArea) {
        this.recognizeArea = recognizeArea;
    }

    @IntDef(value = {
            FAILED,
            SEARCHING,
            RequestFeatureStatus.SUCCEED,
            RequestFeatureStatus.TO_RETRY
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface RequestFaceFeatureStatus {
    }

    @IntDef(value = {
            LivenessInfo.ALIVE,
            LivenessInfo.NOT_ALIVE,
            LivenessInfo.UNKNOWN,
            LivenessInfo.FACE_NUM_MORE_THAN_ONE,
            LivenessInfo.FACE_TOO_SMALL,
            LivenessInfo.FACE_ANGLE_TOO_LARGE,
            LivenessInfo.FACE_BEYOND_BOUNDARY,
            RequestLivenessStatus.ANALYZING
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface RequestFaceLivenessStatus {
    }

    /**
     * 修改人脸识别的状态
     *
     * @param trackId   根据VIDEO模式人脸检测获取的人脸的唯一标识
     * @param newStatus 新的识别状态，详见{@link RequestFeatureStatus}中的定义
     */
    public void ChangeRecognizeStatus(int trackId, @RequestFaceFeatureStatus int newStatus) {
        GetRecognizeInfo(recognizeInfoMap, trackId).setRecognizeStatus(newStatus);
    }

    /**
     * 修改活体活体值或活体检测状态
     *
     * @param trackId     根据VIDEO模式人脸检测获取的人脸的唯一标识
     * @param newLiveness 新的活体值或活体检测状态
     */
    public void ChangeLiveness(int trackId, @RequestFaceLivenessStatus int newLiveness) {
        GetRecognizeInfo(recognizeInfoMap, trackId).setLiveness(newLiveness);
    }

    /**
     * 获取活体值或活体检测状态
     *
     * @param trackId 根据VIDEO模式人脸检测获取的人脸的唯一标识
     * @return 活体值或活体检测状态
     */
    public Integer GetLiveness(int trackId) {
        return GetRecognizeInfo(recognizeInfoMap, trackId).getLiveness();
    }

    /**
     * 获取人脸识别状态
     *
     * @param trackId 根据VIDEO模式人脸检测获取的人脸的唯一标识
     * @return 人脸识别状态
     */
    public Integer GetRecognizeStatus(int trackId) {
        return GetRecognizeInfo(recognizeInfoMap, trackId).getRecognizeStatus();
    }

    /**
     * 保留ftFaceList中最大的人脸
     *
     * @param ftFaceList 人脸追踪时，一帧数据的人脸信息
     */
    private static void keepMaxFace(List<FaceInfo> ftFaceList) {
        if (ftFaceList == null || ftFaceList.size() <= 1) {
            return;
        }
        FaceInfo maxFaceInfo = ftFaceList.get(0);
        for (FaceInfo faceInfo : ftFaceList) {
            if (faceInfo.getRect().width() > maxFaceInfo.getRect().width()) {
                maxFaceInfo = faceInfo;
            }
        }
        ftFaceList.clear();
        ftFaceList.add(maxFaceInfo);
    }

    /**
     * 人脸特征提取线程
     */
    public class FaceRecognizeRunnable implements Runnable {
        private FaceInfo faceInfo;
        private int width;
        private int height;
        private int format;
        private Integer trackId;
        private byte[] nv21Data;

        /**
         * 异步特征提取任务的构造函数
         *
         * @param nv21Data 可见光图像数据
         * @param faceInfo 人脸信息
         * @param width    图像宽度
         * @param height   图像高度
         * @param format   图像格式
         * @param trackId  人脸对应的trackId
         */
        private FaceRecognizeRunnable(byte[] nv21Data, FaceInfo faceInfo, int width, int height, int format, Integer trackId) {
            if (nv21Data == null) {
                return;
            }
            this.nv21Data = nv21Data;
            this.faceInfo = new FaceInfo(faceInfo);
            this.width = width;
            this.height = height;
            this.format = format;
            this.trackId = trackId;
        }


        @Override
        public void run() {
            if (nv21Data != null) {
                if (frEngine != null) {
                    FaceFeature faceFeature = new FaceFeature();
                    long frStartTime = System.currentTimeMillis();
                    int frCode;
                    synchronized (frEngine) {
                        Log.i(TAG, "run1: "+System.currentTimeMillis());
                        frCode = frEngine.extractFaceFeature(nv21Data, width, height, format, faceInfo, faceFeature);
                        Log.i(TAG, "run2: "+System.currentTimeMillis());
                    }
                    if (frCode == ErrorInfo.MOK) {
                        onFaceFeatureInfoGet(faceFeature, trackId, frCode);
                    } else {
                        onFaceFeatureInfoGet(null, trackId, frCode);
                        onFail(new Exception("fr failed errorCode is " + frCode));
                    }
                } else {
                    onFaceFeatureInfoGet(null, trackId, ERROR_FR_ENGINE_IS_NULL);
                    onFail(new Exception("fr failed ,frEngine is null"));
                }
            }
            nv21Data = null;
        }
    }

    /**
     * 活体检测线程
     */
    public class FaceLivenessDetectRunnable implements Runnable {
        private FaceInfo faceInfo;
        private int width;
        private int height;
        private int format;
        private Integer trackId;
        private byte[] nv21Data;
        private int livenessType;
        private Object waitLock;

        /**
         * 异步活体任务的构造函数
         *
         * @param nv21Data     可见光或红外图像数据
         * @param faceInfo     可见光人脸检测得到的人脸信息
         * @param width        图像宽度
         * @param height       图像高度
         * @param format       图像格式
         * @param trackId      人脸对应的trackId
         * @param livenessType 活体检测类型，可以是可见光活体检测{@link com.mysafe.lib_identification.enums.LivenessType#RGB}或红外活体检测{@link com.mysafe.lib_identification.enums.LivenessType#IR}
         * @param waitLock     活体检测通过后，调用该对象的notifyAll函数，通知识别线程活体已通过
         */
        private FaceLivenessDetectRunnable(byte[] nv21Data, FaceInfo faceInfo, int width, int height, int format, Integer trackId, int livenessType, Object waitLock) {
            if (nv21Data == null) {
                return;
            }
            this.nv21Data = nv21Data;
            this.faceInfo = new FaceInfo(faceInfo);
            this.width = width;
            this.height = height;
            this.format = format;
            this.trackId = trackId;
            this.livenessType = livenessType;
            this.waitLock = waitLock;
        }

        @Override
        public void run() {
            if (nv21Data != null) {
                if (flEngine != null) {
                    List<LivenessInfo> livenessInfoList = new ArrayList<>();
                    int flCode;
                    synchronized (flEngine) {
                        if (livenessType == LivenessType.RGB) {
                            // RGB活体检测
                            flCode = flEngine.process(nv21Data, width, height, format, Arrays.asList(faceInfo), FaceEngine.ASF_LIVENESS);
                        } else {
                            // IR活体检测，若有设置双目偏移，则先进行人脸框映射
                            if (dualCameraFaceInfoTransformer != null) {
                                faceInfo = dualCameraFaceInfoTransformer.transformFaceInfo(faceInfo);
                            }
                            flCode = flEngine.processIr(nv21Data, width, height, format, Arrays.asList(faceInfo), FaceEngine.ASF_IR_LIVENESS);
                        }
                    }
                    if (flCode == ErrorInfo.MOK) {
                        if (livenessType == LivenessType.RGB) {
                            flCode = flEngine.getLiveness(livenessInfoList);
                        } else {
                            flCode = flEngine.getIrLiveness(livenessInfoList);
                        }
                    }

                    if (flCode == ErrorInfo.MOK && livenessInfoList.size() > 0) {
                        onFaceLivenessInfoGet(livenessInfoList.get(0), trackId, flCode);
                        if (livenessInfoList.get(0).getLiveness() == LivenessInfo.ALIVE) {
                            synchronized (waitLock) {
                                waitLock.notifyAll();
                            }
                        }
                    } else {
                        onFaceLivenessInfoGet(null, trackId, flCode);
                        onFail(new Exception("fl failed errorCode is " + flCode));
                    }
                } else {
                    onFaceLivenessInfoGet(null, trackId, ERROR_FL_ENGINE_IS_NULL);
                    onFail(new Exception("fl failed ,frEngine is null"));
                }
            }
            nv21Data = null;
        }
    }

    public static final class Builder {
        private FaceEngine ftEngine;
        private FaceEngine frEngine;
        private FaceEngine flEngine;
        private RecognizeConfiguration recognizeConfiguration;
        private IRecognizeCallBack recognizeCallback;
        private IDualCameraFaceInfoTransformer dualCameraFaceInfoTransformer;
        private int frQueueSize;
        private int flQueueSize;
        private int trackedFaceCount;

        public Builder() {
        }


        public Builder recognizeConfiguration(RecognizeConfiguration val) {
            recognizeConfiguration = val;
            return this;
        }

        public Builder dualCameraFaceInfoTransformer(IDualCameraFaceInfoTransformer val) {
            dualCameraFaceInfoTransformer = val;
            return this;
        }

        public Builder recognizeCallback(IRecognizeCallBack val) {
            recognizeCallback = val;
            return this;
        }

        public Builder ftEngine(FaceEngine val) {
            ftEngine = val;
            return this;
        }

        public Builder frEngine(FaceEngine val) {
            frEngine = val;
            return this;
        }

        public Builder flEngine(FaceEngine val) {
            flEngine = val;
            return this;
        }

        //线程数
        public Builder frQueueSize(int val) {
            frQueueSize = val;
            return this;
        }

        public Builder flQueueSize(int val) {
            flQueueSize = val;
            return this;
        }

        public Builder trackedFaceCount(int val) {
            trackedFaceCount = val;
            return this;
        }

        public MsArcFaceHelper build() {
            return new MsArcFaceHelper(this);
        }
    }

}
