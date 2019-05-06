package com.github.waterpeak.jsonfit

import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.lang.reflect.Type

interface JCall<T> {
    fun getReturnType(): Type
    fun enqueue(callback: Callback)
}

inline fun <reified T> Request.toJCall(): JCall<T>{
    return JCallImpl(object: TypeToken<T>(){}.type, this) as JCall<T>
}


class JCallImpl(
    private val returnType: Type,
    private val request: Request
) : JCall<Any> {

    override fun enqueue(callback: Callback) {
        JpiHandler.client?.newCall(request)?.enqueue(callback)
    }

    override fun getReturnType(): Type {
        return returnType
    }

}