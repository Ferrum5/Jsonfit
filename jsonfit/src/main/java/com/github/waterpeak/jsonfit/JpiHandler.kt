package com.github.waterpeak.jsonfit

import android.os.Build
import okhttp3.*
import org.json.JSONObject
import java.lang.reflect.*
import java.util.concurrent.Executor


typealias Call0 = JCall<JResponse>
typealias CallT<T> = JCall<JResponseTyped<T>>
typealias CallList<T> = JCall<JResponseTyped<List<T>>>
typealias CallRaw = JCall<JResponseRawString>

private val mediaTypeJson = MediaType.parse("application/json")

object JpiHandler{
    var client: OkHttpClient? = null
    var jsonConverter: JsonConverter? = null
    var mainExecutor: Executor? = null

    fun initHandler(client: OkHttpClient,
                    json: JsonConverter,
                    mainExecutor: Executor){
        this.client = client
        this.jsonConverter = json
        this.mainExecutor = mainExecutor
    }
}

inline fun <reified T> createJpi(rootUrl: String): T {
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        InvocationHandlerImpl(rootUrl)
    ) as T
}

class InvocationHandlerImpl(
    private val rootUrl: String
) : InvocationHandler {

    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>?): Any? {
        //无需处理的返回值
        if (method.declaringClass === Object::class.java || method.declaringClass === Any::class.java) {
            return method.invoke(this, args)
        }
        //默认方法不处理
        if (Build.VERSION.SDK_INT >= 24 && method.isDefault) {
            return null
        }
        //Get,Post, Json,Multipart
        val requestMethod = method.annotations[0]
        var url = when (requestMethod) {
            is Json -> requestMethod.path
            is Get -> requestMethod.path
            is Multipart -> requestMethod.path
            else -> (requestMethod as Post).path
        }

        //返回值，JCall<T>的范型T
        val type = (method.genericReturnType as ParameterizedType).actualTypeArguments[0]

        //无参数
        if (args == null || args.isEmpty()) {
            val requestBuilder = Request.Builder()
                .url("$rootUrl$url")
            when (requestMethod) {
                is Json -> {
                    requestBuilder.post(RequestBody.create(mediaTypeJson, "{}"))
                        .addHeader("Content-Type", "application/json;charset=UTF-8")
                }
                is Post -> requestBuilder.post(RequestBody.create(null, ""))
                is Multipart -> requestBuilder.post(MultipartBody.create(null, ""))
            }
            return JCallImpl(type, requestBuilder.build())
        }

        //参数列表
        val params = mutableListOf<KeyValue>()
        method.parameterAnnotations.forEachIndexed { index, arrayOfAnnotations ->
            val annotation = arrayOfAnnotations[0]
            val value = args[index]
            if (annotation is Path) {//替换path
                url = url.replace("{${annotation.part}}", value?.toString() ?: "")
            } else if (value != null) {//提交参数
                params.add(KeyValue(Key(index, annotation), value))
            }
        }

        val url2 = "$rootUrl$url"
        val request = when (requestMethod) {
            is Json -> json(url2, params)
            is Get -> form(url2, "GET", params)
            is Multipart -> multipart(url2, params)
            else -> form(url2, "POST", params)
        }

        return JCallImpl(type, request)
    }

    private fun json(
        path: String,
        params: List<KeyValue>
    ): Request {
        val jsonString = if (params.size == 1 && params[0].key.isBody) {
            JpiHandler.jsonConverter?.toJson(params[0].value)?:"{}"
        } else {
            val obj = JSONObject()
            params.forEach { (k, v) ->
                when {
                    v is List<*> -> obj.put(k.name, JpiHandler.jsonConverter?.toJson(v))
                    v is Array<*> -> obj.put(k.name, JpiHandler.jsonConverter?.toJson(v))
                    else -> obj.put(k.name, v)
                }

            }
            obj.toString()
        }
        val requestBody = RequestBody.create(mediaTypeJson, jsonString)
        return Request.Builder()
            .url(path)
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .post(requestBody)
            .build()
    }


    private fun form(path: String, method: String, params: List<KeyValue>): Request {
        val requestBodyBuilder = FormBody.Builder()
        params.forEach { (k, v) ->
            requestBodyBuilder.add(k.name, v.toString())
        }
        return Request.Builder()
            .url(path)
            .method(method, requestBodyBuilder.build())
            .build()
    }

    private fun multipart(path: String, params: List<KeyValue>): Request {
        val builder = MultipartBody.Builder()
        params.forEach { (k, v) ->
            when (v) {
                is RequestBody -> builder.addPart(v)
                is MultipartFile -> builder.addFormDataPart(k.name, v.filename, v.body)
                else -> builder.addFormDataPart(k.name, v.toString())
            }
        }
        return Request.Builder()
            .url(path)
            .post(builder.build())
            .build()
    }
}

private class KeyValue(val key: Key, val value: Any) {
    operator fun component1(): Key = key
    operator fun component2(): Any = value
}

private class Key(
    private val index: Int,
    private val annotation: Annotation?
) : Comparable<Key> {

    val isBody: Boolean
        get() = annotation is Body || annotation == null

    val name = when (annotation) {
        is Param -> annotation.name
        else -> ""
    }

    override fun hashCode(): Int = index
    override fun equals(other: Any?): Boolean {
        return (other as? Key)?.index == index
    }

    override fun compareTo(other: Key): Int {
        return index - other.index
    }

    override fun toString(): String = name

}

