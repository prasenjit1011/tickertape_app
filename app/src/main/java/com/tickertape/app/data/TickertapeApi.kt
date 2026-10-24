package com.tickertape.app.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TickertapeApi {
    @GET("stocks")
    suspend fun stocks(): List<Stock>

    @GET("stocks/band-runs")
    suspend fun bandRuns(): List<BandRun>

    @POST("stocks/sync")
    suspend fun sync(@Query("sid") sid: String): List<Stock>

    @POST("stocks/{sid}/sync")
    suspend fun syncPath(@Path("sid") sid: String): List<Stock>

    @POST("stocks/{sid}/refresh")
    suspend fun refresh(@Path("sid") sid: String): Stock

    @POST("stocks/sync-all")
    suspend fun syncAll(@Query("band") band: String): SyncMessage

    @POST("stocks/range-update")
    suspend fun rangeUpdate(@Query("band") band: String): SyncMessage

    @POST("stocks/sync-due")
    suspend fun syncDue(): SyncMessage

    @GET("stocks/{sid}/yearly")
    suspend fun yearly(@Path("sid") sid: String): List<YearlyRange>

    @GET("stocks/{sid}/monthly")
    suspend fun monthly(@Path("sid") sid: String): List<MonthlyHigh>

    @GET("portfolio/holdings")
    suspend fun holdings(): List<Holding>

    @GET("portfolio/orders")
    suspend fun orders(): List<Order>

    @GET("portfolio/day-profits")
    suspend fun dayProfits(): DayProfitHistory

    @GET("sid-quotes")
    suspend fun sidQuotes(): List<SidQuote>

    @GET("products")
    suspend fun products(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String = "createdAt,desc",
    ): ProductPage

    @GET("products/{id}")
    suspend fun product(@Path("id") id: String): Product

    @GET("products/sku/{sku}")
    suspend fun productBySku(@Path("sku") sku: String): Product

    @POST("products")
    suspend fun createProduct(@Body body: ProductBody): Product

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body body: ProductBody): Product

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<ResponseBody>
}
