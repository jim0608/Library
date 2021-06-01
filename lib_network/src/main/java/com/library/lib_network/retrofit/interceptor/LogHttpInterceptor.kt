package com.library.lib_network.retrofit.interceptor

import android.util.Log
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Create By 张晋铭
 * on 2020/12/2
 * Describe:
 */
class LogHttpInterceptor:Interceptor {
    @Volatile private var headersToRedact = emptySet<String>()


    @set:JvmName("level")
    @Volatile var level = Level.BODY

    enum class Level {
        /** No logs. */
        NONE,

        /**
         * Logs request and response lines.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * ```
         */
        BASIC,

        /**
         * Logs request and response lines and their respective headers.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * ```
         */
        HEADERS,

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * ```
         */
        BODY
    }

    fun redactHeader(name: String) {
        val newHeadersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER)
        newHeadersToRedact += headersToRedact
        newHeadersToRedact += name
        headersToRedact = newHeadersToRedact
    }


    @Deprecated(
            message = "Moved to var. Replace setLevel(...) with level(...) to fix Java",
            replaceWith = ReplaceWith(expression = "apply { this.level = level }"),
            level = DeprecationLevel.WARNING)
    fun setLevel(level: Level) = apply {
        this.level = level
    }

    @JvmName("-deprecated_level")
    @Deprecated(
            message = "moved to var",
            replaceWith = ReplaceWith(expression = "level"),
            level = DeprecationLevel.ERROR)
    fun getLevel(): Level = level


    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS

        val requestBody = request.body
        Log.i("TAG_HTTP_INTERCEPTOR","**************** request start ***************")
        val connection = chain.connection()
        var requestStartMessage = ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }

        if (logHeaders) {
            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                requestBody.contentType()?.let {
                    Log.i("TAG_HTTP_INTERCEPTOR","** Content-Type: $it")
                }
                if (requestBody.contentLength() != -1L) {
                    Log.i("TAG_HTTP_INTERCEPTOR","** Content-Length: ${requestBody.contentLength()}")
                }
            }

            val headers = request.headers
            for (i in 0 until headers.size) {
                val name = headers.name(i)
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equals(name, ignoreCase = true) &&
                        !"Content-Length".equals(name, ignoreCase = true)) {
                    logHeader(headers, i)
                }
            }

            if (!logBody || requestBody == null) {
                Log.i("TAG_HTTP_INTERCEPTOR","** --> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                Log.i("TAG_HTTP_INTERCEPTOR","** --> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                Log.i("TAG_HTTP_INTERCEPTOR","** --> END ${request.method} (duplex request body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                Log.i("TAG_HTTP_INTERCEPTOR","** ")
                if (buffer.isProbablyUtf8()) {
                    Log.i("TAG_HTTP_INTERCEPTOR","** $buffer.readString(charset)")
                    Log.i("TAG_HTTP_INTERCEPTOR","** --> END ${request.method} (${requestBody.contentLength()}-byte body)")
                } else {
                    Log.i("TAG_HTTP_INTERCEPTOR",
                            "**--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)")
                }
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.i("TAG_HTTP_INTERCEPTOR","** <-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        Log.i("TAG_HTTP_INTERCEPTOR",
                "** <-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})")

        if (logHeaders) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                logHeader(headers, i)
            }

            if (!logBody || !response.promisesBody()) {
                Log.i("TAG_HTTP_INTERCEPTOR","** <-- END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                Log.i("TAG_HTTP_INTERCEPTOR","** <-- END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer = source.buffer

                var gzippedLength: Long? = null
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

                if (!buffer.isProbablyUtf8()) {
                    Log.i("TAG_HTTP_INTERCEPTOR","** ")
                    Log.i("TAG_HTTP_INTERCEPTOR","** <-- END HTTP (binary ${buffer.size}-byte body omitted)")
                    return response
                }

                if (contentLength != 0L) {
                    Log.i("TAG_HTTP_INTERCEPTOR","** ")
                    Log.i("TAG_HTTP_INTERCEPTOR","** Response: "+buffer.clone().readString(charset))
                }

                if (gzippedLength != null) {
                    Log.i("TAG_HTTP_INTERCEPTOR","** <-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    Log.i("TAG_HTTP_INTERCEPTOR","** <-- END HTTP (${buffer.size}-byte body)")
                }
            }
        }
        Log.i("TAG_HTTP_INTERCEPTOR","**************** request end ***************")
        return response
    }

    private fun logHeader(headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        Log.i("TAG_HTTP_INTERCEPTOR","** "+headers.name(i) + ": " + value)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }
}