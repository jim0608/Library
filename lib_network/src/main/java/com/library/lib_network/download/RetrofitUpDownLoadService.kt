package com.library.lib_network.download

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/28
 * @Describe:retrofit 文件下载上传
 */
interface RetrofitUpDownLoadService {
    @Streaming
    @GET
    suspend fun downLoadFile(@Url url: String):Response<ResponseBody>
}