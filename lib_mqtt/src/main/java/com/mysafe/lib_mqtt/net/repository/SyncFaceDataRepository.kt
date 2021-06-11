package com.mysafe.lib_mqtt.net.repository

import com.library.lib_network.retrofit.netObserve.BaseModel
import com.mysafe.lib_base.sqlite.dao.FaceDao
import com.mysafe.lib_base.sqlite.entity.FaceEntity
import com.mysafe.lib_base.sqlite.entity.ServiceFaceEntity
import com.mysafe.lib_base.util.UtilHelper
import com.mysafe.lib_mqtt.net.MqttNetworkResponse

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/25
 * @Describe:
 */
class SyncFaceDataRepository(private val dao: FaceDao) {
    private val TAG = "TAG_SyncFaceDataRepository"
    private val repository = MqttNetworkResponse()

    /**
     * 人脸同步
     */
    suspend fun updateData(date: String): BaseModel<MutableList<FaceEntity>> {
        val data = hashMapOf<String, Any>()
        data["date"] = date
        val sqlFace = repository.upDateSqlFace(data)

        return faceInfosToFaceEntity(sqlFace)
    }

    /**
     * 通过ID获取人脸信息
     */
    suspend fun getFaceById(id: String): BaseModel<FaceEntity> {
        val data = hashMapOf<String, Any>()
        data["studentId"] = id
        val sqlFace = repository.getFaceById(data)
        return faceInfoToFaceEntity(sqlFace)
    }

    fun getFaceCount(): Int = dao.faceCount

    fun getAllFaces(): MutableList<FaceEntity>? = dao.allFaces

    fun getFaceIds(): List<Int> = dao.getFaceIds()

    fun insertAllFace(list: MutableList<FaceEntity>) = dao.insertAll(list)

    fun insertFace(faceEntity: FaceEntity) = dao.insert(faceEntity)

    fun deleteFaces(ids: List<Int>) = dao.deleteById(ids)


    /**
     * 将对应的ServiceFaceEntity类型数据转换为FaceEntity类型数据
     */
    private fun faceInfosToFaceEntity(sqlFace: BaseModel<MutableList<ServiceFaceEntity>?>): BaseModel<MutableList<FaceEntity>> {
        val faceData: MutableList<FaceEntity> = ArrayList()
        if (sqlFace.data == null) {
            return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, faceData)
        }
        for (e in sqlFace.data!!) {
            val feature = UtilHelper.base64String2ByteFun(e.featureData)
            val faceEntity =
                FaceEntity(e.id, e.userName, e.userNum, e.imagePath, feature, e.registerTime)
            faceData.add(faceEntity)
        }

        return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, faceData)
    }

    private fun faceInfoToFaceEntity(sqlFace: BaseModel<ServiceFaceEntity?>): BaseModel<FaceEntity> {
        if (sqlFace.data == null) {
            return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, null)
        }
        val faceEntity = sqlFace.data!!.let {
            val feature = UtilHelper.base64String2ByteFun(it.featureData)
            FaceEntity(it.id, it.userName, it.userNum, it.imagePath, feature, it.registerTime)
        }
        return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, faceEntity)
    }
}