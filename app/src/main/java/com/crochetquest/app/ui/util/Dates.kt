package com.crochetquest.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

fun formatEpochDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

/** Whole days between two timestamps, at least 1 (so "took 1 day" never reads "0 days"). */
fun daysBetween(startMillis: Long, endMillis: Long): Long =
    ((endMillis - startMillis) / 86_400_000L).coerceAtLeast(1L)
