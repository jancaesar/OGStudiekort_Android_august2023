package com.ogstudiekort

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @FormUrlEncoded
    @POST("getinfo.asp?type=appauthnew")
    fun authenticateUser(@Field("usr") username: String, @Field("pwd") password: String): Call<String>

    @GET("getinfo.asp?type=getsaldo")
    fun getSaldo(@Query("usr") username: String): Call<String>

    @GET("payapi.asp")
    fun initiatePayment(
        @Query("username") username: String,
        @Query("fullname") fullname: String,
        @Query("amount") amount: Double,
        @Query("system") system: String = "android"
    ): Call<String>

}
