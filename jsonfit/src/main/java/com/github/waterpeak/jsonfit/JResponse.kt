package com.github.waterpeak.jsonfit

import androidx.annotation.Keep

const val RESPONSE_CODE_SUCCESS = 100

const val RESPONSE_HTTP_UNKNOWN = -1
const val RESPONSE_HTTP_OVERTIME = -2
const val RESPONSE_HTTP_EXCEPTION = -3

@Keep
open class JResponse(
    var code: Int,
    var httpCode: Int,
    var message: String?
) {
    val success: Boolean
        get() = code == RESPONSE_CODE_SUCCESS && httpSuccess

    val businessSuccess: Boolean
    get() = code == RESPONSE_CODE_SUCCESS

    val httpSuccess: Boolean
        get() = httpCode in 200..299

    override fun toString(): String {
        return "{\"code\":${code},\"message\":\"${message}\"}"
    }
}

@Keep
open class JResponseTyped<T>(
    code: Int,
    httpCode: Int,
    message: String?
) : JResponse(code, httpCode, message) {
    var content: T? = null
}

@Keep
class JResponseRawString(
    code: Int,
    httpCode: Int,
    message: String?
) : JResponse(code, httpCode, message)

