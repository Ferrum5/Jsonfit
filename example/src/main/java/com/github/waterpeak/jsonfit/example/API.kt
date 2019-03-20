package com.github.waterpeak.jsonfit.example

import com.github.waterpeak.jsonfit.CallT
import com.github.waterpeak.jsonfit.Json
import com.github.waterpeak.jsonfit.Param
import com.github.waterpeak.jsonfit.createJpi

val API: Api = createJpi("http://www.example.com")

interface Api{
    @Json("account/login")
    fun login(@Param("uname") userName: String, @Param("pwd")password: String): CallT<LoginResultEntity>
}

data class LoginResultEntity(var token: String? = null,
                             var nane: String? = null,
                             var point: String? = null)