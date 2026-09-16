package com.automatelinux.adsSwitch.ui

import kotlin.math.abs
import kotlin.math.roundToLong

/** ₪ with thousands separators; agorot only when there are any and the sum is small. */
fun shekels(v: Double, forceAgorot: Boolean = false): String {
    val negative = v < 0
    val cents = (abs(v) * 100).roundToLong()
    val whole = cents / 100
    val frac = cents % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val showAgorot = forceAgorot || (frac != 0L && whole < 100)
    val body = if (showAgorot) "$grouped.${frac.toString().padStart(2, '0')}" else grouped
    return (if (negative) "-" else "") + "₪" + body
}

fun count(n: Int): String = n.toString().reversed().chunked(3).joinToString(",").reversed()

fun percent(part: Double, whole: Double): Int =
    if (whole <= 0.0) 0 else ((part / whole) * 100).roundToLong().toInt()

/** "2026-09-17T00:00:03+03:00" → "00:00". The server stamps local Israel time. */
fun clock(iso: String): String = if (iso.length >= 16) iso.substring(11, 16) else ""

fun modeTitle(mode: String): String = when (mode) {
    "local" -> "באזור שלך"
    "nationwide" -> "בכל הארץ"
    "off" -> "כבוי"
    "mixed" -> "מעורב"
    else -> ""
}

fun modeShort(mode: String): String = when (mode) {
    "local" -> "אזורי"
    "nationwide" -> "כל הארץ"
    "off" -> "כבוי"
    else -> "מעורב"
}
