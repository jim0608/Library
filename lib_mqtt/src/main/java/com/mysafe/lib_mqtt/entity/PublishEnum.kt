package com.mysafe.lib_mqtt.entity

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:发布消息的枚举类
 */
enum class PublishEnum {
    WILL,
    STATE,//离现、在线
    FACE_USER_ID,//用户人脸id
    FACE_MEAL_INFO//用户餐次信息
}