package com.tickertape.app.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val stampFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
private val zone = ZoneId.of("Asia/Kolkata")

fun money(value: Double?, digits: Int = 2): String {
    if (value == null || value.isNaN()) return "—"
    val sign = if (value < 0) "-" else ""
    return "$sign₹${grouped(abs(value), digits)}"
}

fun grouped(value: Double, digits: Int = 2): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    format.minimumFractionDigits = digits
    format.maximumFractionDigits = digits
    format.isGroupingUsed = true
    return format.format(value)
}

fun signedNumber(value: Double?, digits: Int = 2): String {
    if (value == null || value.isNaN()) return "—"
    val sign = when {
        value > 0 -> "+"
        value < 0 -> "-"
        else -> ""
    }
    return sign + grouped(abs(value), digits)
}

fun signedPct(value: Double?, digits: Int = 2): String {
    val number = signedNumber(value, digits)
    return if (number == "—") number else "$number%"
}

fun plain(value: Double?, digits: Int = 2): String {
    if (value == null || value.isNaN()) return "—"
    return grouped(value, digits)
}

fun prettyDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val day = value.take(10)
    return runCatching { LocalDate.parse(day).format(dayFormat) }.getOrDefault(day)
}

fun prettyInstant(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return runCatching {
        Instant.parse(value).atZone(zone).format(stampFormat)
    }.getOrElse { prettyDate(value) }
}

fun rankLabel(value: Double?): String {
    if (value == null) return "—"
    return "#${value.toInt()}"
}
