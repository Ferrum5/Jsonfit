package com.github.waterpeak.jsonfit

import okhttp3.*
import java.lang.reflect.Type

interface JCall<T> {
    fun getReturnType(): Type
    fun enqueue(callback: Callback)
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