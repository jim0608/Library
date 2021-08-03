package com.mysafe.lib_mqtt.receive

import android.util.Log
import com.mysafe.lib_base.router_service.sync_data.SyncUserMealImplWrap

/**
 * @author Create By 张晋铭
 * @Date on 2021/6/16
 * @Describe:
 */
class SyncMealDataReceive(var str: String) : ExecutiveOrder {
    private val TAG = "TAG_SyncMealDataReceive"
    override fun work() {
        Log.i(TAG, "work: $str")
        if (str.contains("staff_meal")){
            str = str.replace("staff_meal:","")
            SyncUserMealImplWrap.fromStaff(str)
        }else{
            str = str.replace("custom_faceId:","")
            SyncUserMealImplWrap.fromCustomer(str)
        }
    }
}