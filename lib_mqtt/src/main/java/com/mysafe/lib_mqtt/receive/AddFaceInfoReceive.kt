package com.mysafe.lib_mqtt.receive

import android.content.Context
import android.util.Log
import com.mysafe.lib_base.router_service.identify_engine.IdentifyEngineImplWrap
import com.mysafe.lib_base.sqlite.FaceDatabase
import com.mysafe.lib_base.sqlite.entity.FaceEntity
import com.mysafe.lib_mqtt.net.repository.SyncFaceDataRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:数据库新增人脸数据
 */
class AddFaceInfoReceive(val mContext: Context, val id: String) : ExecutiveOrder {
    private val TAG = "TAG_AddFaceInfo"
    private lateinit var database: FaceDatabase
    private var repository: SyncFaceDataRepository? = null
    val byte = ByteArray(2)
    override fun work() {
        if (repository == null) {
            database = FaceDatabase.getDatabase(mContext)
            repository = SyncFaceDataRepository(database.faceDao())
        }

        GlobalScope.launch {
            getFaceEntity(id)
        }
    }

    private suspend fun getFaceEntity(id: String) {
        repository?.getFaceById(id)?.run {
            if (isSuccess) {
                val face = FaceEntity(
                    id.toInt(),
                    data?.userName,
                    data?.userNum,
                    data?.imagePath,
                    data?.featureData,
                    data?.registerTime ?: 0
                )
                repository?.insertFace(face)
                IdentifyEngineImplWrap.addFaceInfo(face)
            } else {
                Log.i(TAG, "getFaceEntity: id:$id message:$message ")
            }
        }
    }

    private fun insertFace() {
        val byte = ByteArray(2)
        for (item in 0..15) {
            val face = FaceEntity(
                item,
                "name_$item",
                "num_$item",
                "path_$item",
                byte, 123
            )
            repository?.insertFace(face)
        }

    }
}