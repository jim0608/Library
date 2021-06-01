package com.mysafe.lib_identification.helper;

import com.arcsoft.face.FaceInfo;

public interface IDualCameraFaceInfoTransformer {
    /**
     * 将RGB Camera帧数据检测到的人脸信息用于IR Camera帧数据活体检测时的转换方式
     *
     * @param faceInfo RGB Camera帧数据检测到的人脸信息
     * @return 转换后，用于IR活体检测的FaceInfo
     */
    FaceInfo transformFaceInfo(FaceInfo faceInfo);
}
