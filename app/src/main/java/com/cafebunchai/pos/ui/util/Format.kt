package com.cafebunchai.pos.ui.util

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

fun paiseToRupeeLabel(paise: Int): String = inr.format(paise / 100.0)

fun qtyLabel(qty: Double, unit: String): String {
    val n = if (kotlin.math.abs(qty % 1.0) < 0.0001) qty.toInt().toString() else "%.2f".format(qty)
    return "$n $unit"
}

private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

fun dayBounds(date: LocalDate): Pair<Long, Long> = rangeBounds(date, date)

fun rangeBounds(from: LocalDate, to: LocalDate): Pair<Long, Long> {
    val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}

fun thisWeek(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(zone)
    val from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return from to from.plusDays(6)
}

fun thisMonth(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(zone)
    return today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
}

fun thisYear(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(zone)
    return LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
}

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(timeFmt)

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a"))

fun formatDate(date: LocalDate): String = date.format(dateFmt)
