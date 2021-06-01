package com.mysafe.lib_mqtt.receive

import android.content.Context
import android.util.Log
import com.mysafe.lib_base.router_service.identify_engine.IdentifyEngineImplWrap
import com.mysafe.lib_base.sqlite.FaceDatabase
import com.mysafe.lib_base.sqlite.entity.FaceEntity

import com.mysafe.msmealorder_public.getNowDetailTime
import com.mysafe.lib_mqtt.net.repository.SyncFaceDataRepository
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/19
 * @Describe:同步人脸数据
 */
class SyncFaceData(private val context: Context) : ExecutiveOrder {
    private val TAG = "TAG_SyncFaceData"
    private val mmkv: MMKV = MMKV.defaultMMKV()
    private var faceRegisterInfoList: MutableList<FaceEntity>? = null
    private lateinit var database: FaceDatabase
    private var repository: SyncFaceDataRepository? = null
    override fun work() {
        if (repository == null) {
            database = FaceDatabase.getDatabase(context)
            repository = SyncFaceDataRepository(database.faceDao())
        }
        synchronousDataBase(context)

    }

    private fun synchronousDataBase(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            if (repository == null) {
                cancel()
            }
            val faceCount = repository?.getFaceCount()
            Log.i(TAG, "synchronousDataBase: $faceCount")
            val result = if (faceCount == 0) {
                repository!!.updateData("1970-01-01")
            } else {
                repository!!.updateData(getLastSQLDate())
            }
            result.run {
                if (isSuccess) {
                    val list = data
                    if (list != null) {
                        Log.i(TAG, "synchronousDataBase: ${System.currentTimeMillis()}")
                        repository!!.insertAllFace(list)
                        Log.i(
                            TAG,
                            "synchronousDataBase2: ${System.currentTimeMillis()} dataBaseList:${repository?.getFacesCount()}"
                        )
                        //将当前同步数据的时间保存
                        mmkv.encode("LastSQLDate", getNowDetailTime())

                        faceRegisterInfoList = repository!!.getAllFaces()
                        faceRegisterInfoList?.let { IdentifyEngineImplWrap.setFaceList(context,it) }
                        Log.i(TAG, "synchronousDataBase: ${System.currentTimeMillis()}")
                    }
                }
            }
        }
    }

    private fun getLastSQLDate(): String {
        return mmkv.decodeString("LastSQLDate")
    }
}