package com.library.lib_network.download

import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * @author Create By 张晋铭
 * @Date on 2021/5/31
 * @Describe:
 */
class DownloadFileUtil {
    //下载的长度
    var currentLength = 0
    //写入文件
    val bufferSize = 1024 * 8
    val buffer = ByteArray(bufferSize)
    var readLength = 0

    fun download(build: IDownloadBuild, response: Response<ResponseBody>) =
        flow {
            response.body()?.let { body ->
                val length = body.contentLength()
                val contentType = body.contentType().toString()
                val ios = body.byteStream()
                val info = try {
                    downloadBuildToOutputStream(build, contentType)
                } catch (e: Exception) {
                    emit(DownloadStatus.Error(e))
                    DownloadInfo(null)
                    return@flow
                }
                val ops = info.ops
                if (ops == null) {
                    emit(DownloadStatus.Error(RuntimeException("下载出错")))
                    return@flow
                }

                val bufferedInputStream = BufferedInputStream(ios, bufferSize)
                try {
                    while (bufferedInputStream.read(buffer, 0, bufferSize)
                            .also { readLength = it } != -1
                    ) {
                        ops.write(buffer, 0, readLength)
                        currentLength += readLength
                        emit(
                            DownloadStatus.Process(
                                currentLength.toLong(),
                                length,
                                currentLength.toFloat() / length.toFloat()
                            )
                        )
                    }
                } catch (e: Exception) {
                    emit(DownloadStatus.Error(e))
                    DownloadInfo(null)
                    return@flow
                }

                bufferedInputStream.close()
                ops.close()
                ios.close()
                if (info.uri != null)
                    emit(DownloadStatus.Success(info.uri))
                else emit(DownloadStatus.Success(Uri.fromFile(info.file)))

            } ?: kotlin.run {
                emit(DownloadStatus.Error(RuntimeException("下载出错")))
            }
        }.flowOn(Dispatchers.IO)

    /**
     * 设置下载文件的文件信息
     */
    private fun downloadBuildToOutputStream(
        build: IDownloadBuild,
        contentType: String
    ): DownloadInfo {
        val context = build.getContext()
        val uri = build.getUri(contentType)
        if (build.getDownloadFile() != null) {
            val file = build.getDownloadFile()!!
            return DownloadInfo(FileOutputStream(file), file)
        } else if (uri != null) {
            return DownloadInfo(context.contentResolver.openOutputStream(uri), uri = uri)
        } else {
            val name = build.getFileName()
            var fileName = if (!name.isNullOrBlank()) name else "${System.currentTimeMillis()}"
            fileName = "$fileName.${
                MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(contentType)
            }"
            val file = File(context.getExternalFilesDir("apk"), fileName)
            return DownloadInfo(FileOutputStream(file), file)
        }
    }
}
