package com.library.lib_network

import com.library.lib_network.retrofit.netObserve.BaseModel
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
interface PublicRequestService {
    @Streaming
    @GET
    fun download(@Url url: String): Call<ResponseBody>

    @Headers("Content-Type:application/json")
    @POST
    fun uploadFile(@Url url: String,
                   @Body body: MutableMap<String, Any>
    ):Call<BaseModel<String>>

}