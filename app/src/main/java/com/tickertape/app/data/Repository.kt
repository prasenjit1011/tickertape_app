package com.tickertape.app.data

import android.app.Application
import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class ApiException(message: String) : Exception(message)

class Repository(app: Application) {
    private val prefs = app.getSharedPreferences("tickertape", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _url = MutableStateFlow(readUrl())
    val url: StateFlow<String> = _url.asStateFlow()

    @Volatile
    private var api: TickertapeApi = build(_url.value)

    private val stocksBySid = linkedMapOf<String, Stock>()

    fun updateUrl(raw: String) {
        val next = normalize(raw)
        prefs.edit().putString(URL_KEY, next).apply()
        api = build(next)
        _url.value = next
    }

    fun cachedStock(sid: String): Stock? = synchronized(stocksBySid) {
        stocksBySid[sid.trim().uppercase()]
    }

    fun cacheStocks(stocks: List<Stock>) {
        synchronized(stocksBySid) {
            stocks.forEach { stock ->
                if (stock.sid.isNotBlank()) stocksBySid[stock.sid.trim().uppercase()] = stock
            }
        }
    }

    suspend fun stocks(): List<Stock> = call { stocks() }
    suspend fun bandRuns(): List<BandRun> = call { bandRuns() }
    suspend fun sync(sid: String): List<Stock> = call { sync(sid) }
    suspend fun refresh(sid: String): Stock = call { refresh(sid) }
    suspend fun syncAll(band: String): SyncMessage = call { syncAll(band) }
    suspend fun rangeUpdate(band: String): SyncMessage = call { rangeUpdate(band) }
    suspend fun syncDue(): SyncMessage = call { syncDue() }
    suspend fun yearly(sid: String): List<YearlyRange> = call { yearly(sid) }
    suspend fun monthly(sid: String): List<MonthlyHigh> = call { monthly(sid) }
    suspend fun holdings(): List<Holding> = call { holdings() }
    suspend fun orders(): List<Order> = call { orders() }
    suspend fun dayProfits(): DayProfitHistory = call { dayProfits() }
    suspend fun sidQuotes(): List<SidQuote> = call { sidQuotes() }
    suspend fun products(page: Int): ProductPage = call { products(page, 40) }
    suspend fun product(id: String): Product = call { product(id) }
    suspend fun productBySku(sku: String): Product = call { productBySku(sku) }
    suspend fun createProduct(body: ProductBody): Product = call { createProduct(body) }
    suspend fun updateProduct(id: String, body: ProductBody): Product = call { updateProduct(id, body) }

    suspend fun deleteProduct(id: String) {
        val response = call { deleteProduct(id) }
        if (!response.isSuccessful) {
            throw ApiException("Server returned ${response.code()}")
        }
    }

    private fun build(base: String): TickertapeApi {
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TickertapeApi::class.java)
    }

    private suspend fun <T> call(block: suspend TickertapeApi.() -> T): T {
        try {
            return api.block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw ApiException(friendly(error))
        }
    }

    private fun readUrl(): String {
        val stored = prefs.getString(URL_KEY, null)?.trim().orEmpty()
        val legacy = stored.isEmpty() || stored.trimEnd('/') == LEGACY_URL.trimEnd('/')
        return normalize(if (legacy) DEFAULT_URL else stored)
    }

    companion object {
        const val DEFAULT_URL =
            "https://ju3pe55wqwr3rdhqvvyfsxph7u0ibajk.lambda-url.ap-south-1.on.aws/api/v1/"
        private const val LEGACY_URL = "http://10.0.2.2:8080/"
        private const val URL_KEY = "baseUrl"

        fun normalize(raw: String): String {
            var value = raw.trim().trimEnd('/')
            if (value.isEmpty()) value = DEFAULT_URL.trimEnd('/')
            if (!value.startsWith("http://") && !value.startsWith("https://")) {
                value = "https://$value"
            }
            if (!value.endsWith("/api/v1")) {
                value = "$value/api/v1"
            }
            return "$value/"
        }

        private fun friendly(error: Throwable): String = when (error) {
            is ApiException -> error.message ?: "Request failed"
            is HttpException -> {
                val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                    .orEmpty()
                    .trim()
                    .take(180)
                body.ifBlank { "Server returned ${error.code()}" }
            }
            is UnknownHostException, is ConnectException ->
                "Cannot reach the API. Set the host in Settings."
            is SocketTimeoutException -> "The API took too long to respond."
            is SerializationException -> "The API response did not match this app."
            is IOException -> error.message ?: "Network error"
            else -> error.message ?: "Something went wrong"
        }
    }
}
