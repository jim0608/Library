package com.library.lib_network.retrofit.interceptor

import okio.Buffer
import okio.EOFException

internal fun Buffer.isProbablyUtf8(): Boolean {
  try {
    val prefix = Buffer()
    val byteCount = size.coerceAtMost(64)
    copyTo(prefix, 0, byteCount)
    for (i in 0 until 16) {
      if (prefix.exhausted()) {
        break
      }
      val codePoint = prefix.readUtf8CodePoint()
      if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
        return false
      }
    }
    return true
  } catch (_: EOFException) {
    return false // Truncated UTF-8 sequence.
  }
}
