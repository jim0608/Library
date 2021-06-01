package com.mysafe.lib_identification.face_engin

enum class EngineCodeEnum{
        /**
         * 人脸特征匹配引擎，用于对人脸特征值进行匹配
         */
        FC_ENGINE,
        /**
         * VIDEO模式人脸检测引擎，用于预览帧人脸追踪及图像质量检测
         */
        FT_ENGINE,

        /**
         * 用于特征提取的引擎
         */
        FR_ENGINE,

        /**
         * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
         */
        FL_ENGINE
    }