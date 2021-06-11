package com.mysafe.lib_mqtt.net

import com.library.lib_network.retrofit.netObserve.BaseModel
import com.mysafe.lib_base.base.BaseDataEntity
import com.mysafe.lib_base.base.VersionConfigBean
import com.mysafe.lib_base.sqlite.entity.ServiceFaceEntity
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/25
 * @Describe:
 */
interface SyncDeviceInfoImpl {

    /**
     * 上传设备相关信息
     */
    @POST("ordermeal/om_uploadinfo")
    fun uploadDevicesInfo(@Body body: Map<String, String>): Call<BaseModel<BaseDataEntity>>

    /**
     * 检查更新
     */
    @POST("ordermeal/om_checkver")
    fun checkVersion():Call<BaseModel<VersionConfigBean>>

    /**
     * 更新人脸信息
     */
    @POST("ordermeal/om_downloadfaceddata")
    fun updateFaceData(@Body body: MutableMap<String, Any>):
            Call<BaseModel<MutableList<ServiceFaceEntity>>>

    /**
     * 根据学生ID获取学生人脸信息
     */
    @POST("ordermeal/om_getfacefromid")
    fun getFaceById(@Body body: MutableMap<String, Any>):Call<BaseModel<ServiceFaceEntity>>
}