package com.example

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class KaspaPriceResponse(
    val price: Double
)

data class FiatRatesResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

interface KaspaApi {
    @GET("simple/price?ids=kaspa")
    suspend fun getKaspaPrice(
        @Query("vs_currencies") currencies: String
    ): Map<String, Map<String, Double>>

    @GET("https://api.kaspa.org/info/price")
    suspend fun getOfficialKaspaPrice(): KaspaPriceResponse

    @GET("https://open.er-api.com/v6/latest/USD")
    suspend fun getFiatRates(): FiatRatesResponse

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3/"

        fun create(): KaspaApi {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(KaspaApi::class.java)
        }
    }
}
