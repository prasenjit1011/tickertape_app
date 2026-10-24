package com.tickertape.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Stock(
    val id: String = "",
    val name: String = "",
    val ticker: String = "",
    val sid: String = "",
    val sector: String = "",
    val type: String = "",
    val exchange: String = "",
    val ttmPe: Double? = null,
    val breco: Double? = null,
    @SerialName("52wpct") val week52Pct: Double? = null,
    val marketCap: Double? = null,
    val divYield: Double? = null,
    @SerialName("52wHigh") val week52High: Double? = null,
    @SerialName("52wLow") val week52Low: Double? = null,
    @SerialName("4wpct") val week4Pct: Double? = null,
    val pe: Double? = null,
    val pb: Double? = null,
    val lastPrice: Double? = null,
    val price: Double? = null,
    val risk: Double? = null,
    val mrktCapRank: Double? = null,
    val eps: Double? = null,
    val roe: Double? = null,
    val marketCapLabel: String = "",
    val change: Double? = null,
    val dyChange: Double? = null,
    val wkChange: Double? = null,
    val mnChange: Double? = null,
    val away52wH: Double? = null,
    val away52wL: Double? = null,
    @SerialName("HPrice_2020") val hPrice2020: Double? = null,
    @SerialName("LPrice_2020") val lPrice2020: Double? = null,
    @SerialName("HPrice_2021") val hPrice2021: Double? = null,
    @SerialName("LPrice_2021") val lPrice2021: Double? = null,
    @SerialName("H_2020") val h2020: Double? = null,
    @SerialName("L_2020") val l2020: Double? = null,
    @SerialName("H_2021") val h2021: Double? = null,
    @SerialName("L_2021") val l2021: Double? = null,
    val updatedAt: String = "",
) {
    val quote: Double? get() = lastPrice ?: price
}

@Serializable
data class BandRun(
    val id: String = "",
    val updatedAt: String = "",
)

@Serializable
data class SyncMessage(
    val updated: Int = 0,
    val started: Boolean = false,
    val due: Int = 0,
    val message: String = "",
    val updatedAt: String = "",
)

@Serializable
data class YearlyRange(
    val year: Int = 0,
    val low: Double = 0.0,
    val lowWeek: String = "",
    val high: Double = 0.0,
    val highWeek: String = "",
)

@Serializable
data class MonthlyHigh(
    val month: String = "",
    val low: Double = 0.0,
    val lowDate: String = "",
    val high: Double = 0.0,
    val highDate: String = "",
)

@Serializable
data class Holding(
    val symbol: String = "",
    val company: String = "",
    val isin: String = "",
    val qty: Double? = null,
    val avgCost: Double? = null,
    val price: Double? = null,
    val dayPct: Double? = null,
    val costValue: Double? = null,
    val marketValue: Double? = null,
    val realized: Double? = null,
    val unrealized: Double? = null,
    val unrealizedPct: Double? = null,
)

@Serializable
data class Order(
    val date: String = "",
    val symbol: String = "",
    val action: String = "",
    val qty: Double? = null,
    val price: Double? = null,
    val value: Double? = null,
    val charges: Double? = null,
    val exchange: String = "",
)

@Serializable
data class DayProfitHistory(
    val totalInvested: Double = 0.0,
    val totalProfit: Double = 0.0,
    val days: List<DayProfit> = emptyList(),
)

@Serializable
data class DayProfit(
    val date: String = "",
    val invested: Double = 0.0,
    val profit: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalProfit: Double = 0.0,
)

@Serializable
data class SidQuote(
    val sid: String = "",
    val name: String = "",
    val quotes: List<QuotePoint> = emptyList(),
)

@Serializable
data class QuotePoint(
    val breco: Double? = null,
    val price: Double? = null,
    val date: String = "",
)

@Serializable
data class Product(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val active: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ProductPage(
    val content: List<Product> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
)

@Serializable
data class ProductBody(
    val sku: String,
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int,
)
