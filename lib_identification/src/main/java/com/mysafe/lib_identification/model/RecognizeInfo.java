package com.mysafe.lib_identification.model;

import com.arcsoft.face.LivenessInfo;

public class RecognizeInfo {
    public static final int RequestFeatureStatus_SEARCHING = 0;
    public static final int RequestFeatureStatus_SUCCEED = 1;
    public static final int RequestFeatureStatus_FAILED = 2;
    public static final int RequestFeatureStatus_TO_RETRY = 3;

    /**
     * 用于记录人脸识别相关状态
     * @see #RequestFeatureStatus_SEARCHING 处理中
     * @see #RequestFeatureStatus_SUCCEED 识别成功
     * @see #RequestFeatureStatus_FAILED 识别失败
     * @see #RequestFeatureStatus_TO_RETRY 待重试
     */
    private int recognizeStatus = RequestFeatureStatus_TO_RETRY;

    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private int extractErrorRetryCount;
    /**
     * 用于存储活体值
     */
    private int liveness = LivenessInfo.UNKNOWN;
    /**
     * 用于存储活体检测出错重试次数
     */
    private int livenessErrorRetryCount;
    /**
     * 用户姓名，用于显示
     */
    private String name;
    /**
     * 特征等活体的lock
     */
    private Object waitLock = new Object();

    public int getRecognizeStatus() {
        return recognizeStatus;
    }

    public void setRecognizeStatus(int recognizeStatus) {
        this.recognizeStatus = recognizeStatus;
    }

    public void setLiveness(int liveness) {
        this.liveness = liveness;
    }

    public int increaseAndGetExtractErrorRetryCount() {
        return ++extractErrorRetryCount;
    }

    public int getLiveness() {
        return liveness;
    }

    public int increaseAndGetLivenessErrorRetryCount() {
        return ++livenessErrorRetryCount;
    }

    public void setExtractErrorRetryCount(int extractErrorRetryCount) {
        this.extractErrorRetryCount = extractErrorRetryCount;
    }

    public void setLivenessErrorRetryCount(int livenessErrorRetryCount) {
        this.livenessErrorRetryCount = livenessErrorRetryCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getWaitLock() {
        return waitLock;
    }
}
