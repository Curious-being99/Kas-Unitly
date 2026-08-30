package com.example

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Response Models
data class KaspaPriceResponse(
    val price: Double = 0.0
)

data class MexcPriceResponse(
    val symbol: String? = null,
    val price: String? = null
)

data class GateTickerResponse(
    val currency_pair: String? = null,
    val last: String? = null
)

data class CoinPaprikaUsd(
    val price: Double? = null
)

data class CoinPaprikaQuotes(
    val USD: CoinPaprikaUsd? = null
)

data class CoinPaprikaResponse(
    val quotes: CoinPaprikaQuotes? = null
)

data class CryptoComparePriceResponse(
    val USD: Double? = null
)

data class FiatRatesResponse(
    val result: String? = null,
    val base_code: String? = null,
    val rates: Map<String, Double>? = null
)

data class ExchangeRateApiV4Response(
    val base: String? = null,
    val rates: Map<String, Double>? = null
)

data class CurrencyApiJsDelivrResponse(
    val date: String? = null,
    val usd: Map<String, Double>? = null
)

interface KaspaApi {
    // 1. Kaspa Official Network Node / Indexer
    @GET("https://api.kaspa.org/info/price")
    suspend fun getOfficialKaspaPrice(): KaspaPriceResponse

    // 2. MEXC Global Spot Market Ticker (High liquidity KAS hub)
    @GET("https://api.mexc.com/api/v3/ticker/price?symbol=KASUSDT")
    suspend fun getMexcKaspaPrice(): MexcPriceResponse

    // 3. Gate.io Global Spot Market Ticker
    @GET("https://api.gateio.ws/api/v4/spot/tickers?currency_pair=KAS_USDT")
    suspend fun getGateKaspaPrice(): List<GateTickerResponse>

    // 4. CoinPaprika Open Decentralized Crypto Index
    @GET("https://api.coinpaprika.com/v1/tickers/kas-kaspa")
    suspend fun getCoinPaprikaKaspaPrice(): CoinPaprikaResponse

    // 5. CryptoCompare Multi-Exchange Index
    @GET("https://min-api.cryptocompare.com/data/price?fsym=KAS&tsyms=USD")
    suspend fun getCryptoComparePrice(): CryptoComparePriceResponse

    // 6. CoinGecko Global Price Matrix
    @GET("simple/price?ids=kaspa")
    suspend fun getKaspaPrice(
        @Query("vs_currencies") currencies: String
    ): Map<String, Map<String, Double>>

    // --- Fiat Multi-Source Redundancy Endpoints ---
    // Fiat Source A: Open ER-API (open mirror)
    @GET("https://open.er-api.com/v6/latest/USD")
    suspend fun getFiatRatesOpenEr(): FiatRatesResponse

    // Fiat Source B: ExchangeRate-API v4 (redundant provider)
    @GET("https://api.exchangerate-api.com/v4/latest/USD")
    suspend fun getFiatRatesExchangeRateApi(): ExchangeRateApiV4Response

    // Fiat Source C: Decentralized jsDelivr Edge CDN Currency Feed
    @GET("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
    suspend fun getFiatRatesJsDelivr(): CurrencyApiJsDelivrResponse

    // Fiat Source D: Cloudflare Pages Edge CDN Mirror
    @GET("https://latest.currency-api.pages.dev/v1/currencies/usd.json")
    suspend fun getFiatRatesCloudflare(): CurrencyApiJsDelivrResponse

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3/"

        fun create(): KaspaApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .writeTimeout(6, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(KaspaApi::class.java)
        }
    }
}

