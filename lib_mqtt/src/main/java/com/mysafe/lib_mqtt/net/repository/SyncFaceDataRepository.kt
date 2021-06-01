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
    fun syncFaceData() {
    }
    suspend fun updateData(date: String): BaseModel<MutableList<FaceEntity>> {
        val data = hashMapOf<String, Any>()
        data["date"] = date
        val sqlFace = MqttNetworkResponse().upDateSqlFace(data)

        return strBase64ToByte(sqlFace)
    }

    suspend fun getFaceCount(): Int = dao.faceCount


    suspend fun insertAllFace(list: MutableList<FaceEntity>) {
        dao.insertAll(list)
//        Log.i("TAG_insertAllFace", "insertAllFace2: ${System.currentTimeMillis()}")
    }

    suspend fun getAllFaces(): MutableList<FaceEntity>? {
        return dao.allFaces
    }
    suspend fun getFacesCount(): Int {
        return dao.faceCount
    }

    private fun strBase64ToByte(sqlFace: BaseModel<MutableList<ServiceFaceEntity>?>): BaseModel<MutableList<FaceEntity>> {
        val faceData: MutableList<FaceEntity> = ArrayList()
        if(sqlFace.data == null){
            return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, faceData)
        }
        for (e in sqlFace.data!!) {
            val feature = UtilHelper.base64String2ByteFun(e.featureData)
            val faceEntity = FaceEntity(e.id, e.userName, e.userNum, e.imagePath, feature, e.registerTime)
            faceData.add(faceEntity)
        }

        return BaseModel(sqlFace.isSuccess, sqlFace.code, sqlFace.message, faceData)
    }
}