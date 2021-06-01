package com.mysafe.lib_identification.view;

class TempFaceEntity {
    /**
     * 人脸id，主键
     */
    private long faceId;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 图片路径
     */
    private String imagePath;
    /**
     * 人脸特征数据
     */
    private byte[] featureData;
    /**
     * 注册时间
     */
    private long registerTime;

    public long getFaceId() {
        return faceId;
    }

    public void setFaceId(long faceId) {
        this.faceId = faceId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public byte[] getFeatureData() {
        return featureData;
    }

    public void setFeatureData(byte[] featureData) {
        this.featureData = featureData;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }
}