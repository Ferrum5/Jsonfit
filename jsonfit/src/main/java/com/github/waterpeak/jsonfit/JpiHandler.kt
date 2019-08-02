package com.github.waterpeak.jsonfit

import android.os.Build
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.*
import java.util.concurrent.Executor


typealias Call0 = JCall<JResponse>
typealias CallT<T> = JCall<JResponseTyped<T>>
typealias CallList<T> = JCall<JResponseTyped<List<T>>>
typealias CallRaw = JCall<JResponseRawString>

val MEDIATYPE_JSON = MediaType.parse("application/json")

object JpiHandler {
    var jsonKeyCode = "code"
    var jsonKeyMessage = "message"
    var jsonKeyContent = "content"
    var client: OkHttpClient? = null
    var jsonConverter: JsonConverter? = null
    var mainExecutor: Executor? = null
    var successCode = 100
    var jResponseInterceptor: ((response: JResponse) -> Unit)? = null

    fun initHandler(
        client: OkHttpClient,
        json: JsonConverter,
        mainExecutor: Executor
    ) {
        this.client = client
        this.jsonConverter = json
        this.mainExecutor = mainExecutor
    }

    var log: (log: String) -> Unit = {}
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
                    requestBuilder.post(RequestBody.create(MEDIATYPE_JSON, "{}"))
                        .addHeader("Content-Type", "application/json;charset=UTF-8")
                }
                is Post -> requestBuilder.post(RequestBody.create(null, ""))
                is Multipart -> requestBuilder.post(MultipartBody.create(null, ""))
            }
            return JCallImpl(type, requestBuilder.build())
        }

        //参数列表
        val params = mutableListOf<KeyValue>()
        val headers = mutableListOf<KeyValue>()
        method.parameterAnnotations.forEachIndexed { index, arrayOfAnnotations ->
            val annotation = arrayOfAnnotations[0]
            val value = args[index]
            if (annotation is Path) {//替换path
                url = url.replace("{${annotation.part}}", value?.toString() ?: "")
            } else if (value != null) {//提交参数
                if (annotation is Header) {
                    headers.add(KeyValue(Key(index, annotation), value))
                } else {
                    params.add(KeyValue(Key(index, annotation), value))
                }
            }
        }

        val url2 = "$rootUrl$url"
        val request = when (requestMethod) {
            is Json -> json(url2, headers, params)
            is Get -> get(url2, headers, params)
            is Multipart -> multipart(requestMethod.type, url2, headers, params)
            else -> form(url2, "POST", headers, params)
        }

        return JCallImpl(type, request)
    }

    private fun json(
        path: String,
        headers: List<KeyValue>,
        params: List<KeyValue>
    ): Request {
        val jsonString = if (params.size == 1 && params[0].key.isBody) {
            JpiHandler.jsonConverter?.toJson(params[0].value) ?: "{}"
        } else {
            val obj = JSONObject()
            for ((k, v) in params) {
                when {
                    v is List<*> -> {
                        val jArray = JSONArray()
                        for(i in v){
                            if(i!=null){
                                jArray.put(i)
                            }
                        }
                        obj.put(k.name, jArray)
                    }
                    v is Array<*> -> {
                        val jArray = JSONArray()
                        for(i in v){
                            if(i!=null){
                                jArray.put(i)
                            }
                        }
                        obj.put(k.name, jArray)
                    }
                    else -> obj.put(k.name, v)
                }
            }
            obj.toString()
        }
        val requestBody = RequestBody.create(MEDIATYPE_JSON, jsonString)

        val builder = Request.Builder()
        for ((k, v) in headers) {
            builder.addHeader(k.name, v.toString())
        }
        return builder.url(path)
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .post(requestBody)
            .build()
    }

    private fun get(path: String, headers: List<KeyValue>, params: List<KeyValue>): Request {
        val url = HttpUrl.parse(path)!!.newBuilder()
        for ((k, v) in params) {
            url.addQueryParameter(k.name, v.toString())
        }
        val builder = Request.Builder()
        for ((k, v) in headers) {
            builder.addHeader(k.name, v.toString())
        }
        return builder.url(url.build())
            .get()
            .build()
    }

    private fun form(path: String, method: String, headers: List<KeyValue>, params: List<KeyValue>): Request {
        val requestBodyBuilder = FormBody.Builder()
        for ((k, v) in params) {
            requestBodyBuilder.add(k.name, v.toString())
        }
        val builder = Request.Builder()
        for ((k, v) in headers) {
            builder.addHeader(k.name, v.toString())
        }
        return builder.url(path)
            .method(method, requestBodyBuilder.build())
            .build()
    }

    private fun multipart(type: Int, path: String, headers: List<KeyValue>, params: List<KeyValue>): Request {
        val builder = MultipartBody.Builder()
        builder.setType(
            when (type) {
                Multipart.MIXED -> MultipartBody.MIXED
                Multipart.ALTERNATIVE -> MultipartBody.ALTERNATIVE
                Multipart.DIGEST -> MultipartBody.DIGEST
                Multipart.PARALLEL -> MultipartBody.PARALLEL
                else -> MultipartBody.FORM
            }
        )
        for ((k, v) in params) {
            when (v) {
                is RequestBody -> builder.addPart(v)
                is MultipartFile -> builder.addFormDataPart(k.name, v.filename, v.body)
                else -> builder.addFormDataPart(k.name, v.toString())
            }
        }
        val requestBuilder = Request.Builder()
            .url(path)
            .post(builder.build())
        for ((k, v) in headers) {
            requestBuilder.addHeader(k.name, v.toString())
        }
        return requestBuilder.build()
    }
}

private class KeyValue(val key: Key, val value: Any) {
    operator fun component1(): Key = key
    operator fun component2(): Any = value

    override fun toString(): String = "$key=$value"
}

private class Key(
    private val index: Int,
    private val annotation: Annotation?
) : Comparable<Key> {

    val isBody: Boolean
        get() = annotation is Body || annotation == null

    val name = when (annotation) {
        is Param -> annotation.name
        is Header -> annotation.name
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

