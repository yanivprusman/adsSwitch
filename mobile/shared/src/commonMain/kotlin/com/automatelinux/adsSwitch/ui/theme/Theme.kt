package com.automatelinux.adsSwitch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * One colour per state, used everywhere that state appears — the map, the
 * switch, the campaign pills — so "green" always means "running near home"
 * and never has to be read.
 */
object Palette {
    val Page = Color(0xFFF3F4F1)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF14181B)
    val InkSoft = Color(0xFF5B6268)
    val InkFaint = Color(0xFF979DA3)
    val Line = Color(0xFFE3E5E1)

    // The hero is dark so the map can glow.
    val Night = Color(0xFF0E1714)
    val NightLine = Color(0xFF2A3833)

    val Local = Color(0xFF16A34A)
    val LocalGlow = Color(0xFF4ADE80)
    val LocalSoft = Color(0xFFE3F5E9)
    val National = Color(0xFF2563EB)
    val NationalGlow = Color(0xFF60A5FA)
    val NationalSoft = Color(0xFFE4ECFD)
    val Off = Color(0xFF6B7280)
    val OffGlow = Color(0xFFB4BAC1)
    val OffSoft = Color(0xFFECEEF0)
    val Mixed = Color(0xFFD97706)
    val MixedSoft = Color(0xFFFCEFD9)
    val Danger = Color(0xFFB42318)
    val DangerSoft = Color(0xFFFDECEA)
}

fun modeColor(mode: String): Color = when (mode) {
    "local" -> Palette.Local
    "nationwide" -> Palette.National
    "mixed" -> Palette.Mixed
    else -> Palette.Off
}

fun modeGlow(mode: String): Color = when (mode) {
    "local" -> Palette.LocalGlow
    "nationwide" -> Palette.NationalGlow
    "mixed" -> Palette.Mixed
    else -> Palette.OffGlow
}

fun modeSoft(mode: String): Color = when (mode) {
    "local" -> Palette.LocalSoft
    "nationwide" -> Palette.NationalSoft
    "mixed" -> Palette.MixedSoft
    else -> Palette.OffSoft
}

private val Colors = lightColorScheme(
    primary = Palette.Local,
    background = Palette.Page,
    surface = Palette.Card,
    onSurface = Palette.Ink,
    onBackground = Palette.Ink,
    error = Palette.Danger,
)

private val Type = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
)

// Multiplatform theme (no Android-only dynamic colour, so it compiles for iOS too).
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type, content = content)
}
