package com.cyansmoke.converter

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
//интерфейс Retrofit'a для запросов к API
    @GET("currencies")
    fun getListOfCurrencies(): Call<JsonObject>

    @GET("convert?compact=ultra")
    fun getCourse(@Query("q") q: String): Call<JsonObject>

}