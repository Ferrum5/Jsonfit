package com.github.waterpeak.jsonfit

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.lang.reflect.Type

interface JsonConverter {
    fun toJson(obj: Any): String
    fun fromJson(json: String, type: Type):Any
}

class GsonConverter(private val gson: Gson) : JsonConverter {
    override fun fromJson(json: String, type: Type): Any {
        return gson.getAdapter(TypeToken.get(type)).fromJson(json)
    }

    override fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

}