package com.mysafe.lib_mqtt.receive

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mysafe.lib_base.expansion.EX_Json
import com.mysafe.lib_base.sqlite.FaceDatabase
import com.mysafe.lib_mqtt.net.repository.SyncFaceDataRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Create By 张晋铭
 * @Date on 2021/4/20
 * @Describe:数据库删除人脸数据
 */
class DeleteFaceInfoReceive(val mContext: Context, var id: String) : ExecutiveOrder {
    private val TAG = "TAG_DeleteFaceInfo"
    private lateinit var database: FaceDatabase
    private var repository: SyncFaceDataRepository? = null

    override fun work() {
        if (repository == null) {
            database = FaceDatabase.getDatabase(mContext)
            repository = SyncFaceDataRepository(database.faceDao())
        }
        id = id.replace("\"", "")
        val ids: List<Int> = Gson().fromJson(id, object : TypeToken<List<Int>>() {}.type)
        Log.i(TAG, "work: ${ids.toString()}")

        GlobalScope.launch {
            repository!!.deleteFaces(ids)
        }
    }


}