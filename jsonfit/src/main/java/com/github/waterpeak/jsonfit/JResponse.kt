package com.github.waterpeak.jsonfit

const val RESPONSE_CODE_SUCCESS = 100

const val RESPONSE_HTTP_UNKNOWN = -1
const val RESPONSE_HTTP_OVERTIME = -2
const val RESPONSE_HTTP_EXCEPTION = -3

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonKey(val key: String)

open class JResponse(
    @JsonKey("code")
    var code: Int,
    var httpCode: Int,
    @JsonKey("message")
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

open class JResponseTyped<T>(
    code: Int,
    httpCode: Int,
    message: String?
) : JResponse(code, httpCode, message) {
    @JsonKey("content")
    var content: T? = null
}

class JResponseRawString(
    code: Int,
    httpCode: Int,
    message: String?
) : JResponse(code, httpCode, message)

