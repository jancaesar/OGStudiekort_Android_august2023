package com.ogstudiekort

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object PaymentRetrofitClient {
    private const val PAYMENT_BASE_URL = "https://pay.ordrup-gym.dk/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(PAYMENT_BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
