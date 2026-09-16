package com.automatelinux.adsSwitch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.automatelinux.adsSwitch.data.Center
import com.automatelinux.adsSwitch.data.GeoCity
import com.automatelinux.adsSwitch.data.ISRAEL_OUTLINE
import com.automatelinux.adsSwitch.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** The map's own aspect ratio (width / height), so callers can size it exactly. */
const val MAP_ASPECT = 0.40f

private const val LON_MIN = 34.20
private const val LON_MAX = 35.98
private const val LAT_MIN = 29.42
private const val LAT_MAX = 33.36
private val LON_SCALE = cos(31.4 * PI / 180)

/**
 * Where the ads run, drawn.
 *
 * - The country fills with the nationwide colour when the nationwide set runs.
 * - The two local circles fill and breathe when the local set runs, and stay as
 *   a dashed outline otherwise, so "local" is always shown as a place you
 *   could switch to.
 * - Dots are the cities clicks came from over the chosen period, sized by
 *   spend: green inside a circle, pale outside. They answer the question the
 *   switch exists for — is the money landing near home?
 * - While a switch is on its way to Google, the whole drawing pulses.
 */
@Composable
fun AreaMap(
    mode: String,
    pending: Boolean,
    centers: List<Center>,
    radiusKm: Double,
    cities: List<GeoCity>,
    modifier: Modifier = Modifier,
) {
    val countryFill by animateColorAsState(
        when (mode) {
            "nationwide" -> Palette.National.copy(alpha = 0.42f)
            "mixed" -> Palette.Mixed.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        tween(600, easing = FastOutSlowInEasing),
    )
    val countryEdge by animateColorAsState(
        if (mode == "nationwide") Palette.NationalGlow.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.32f),
        tween(600),
    )
    val circleFill by animateFloatAsState(if (mode == "local") 1f else 0f, tween(600, easing = FastOutSlowInEasing))
    val dim by animateFloatAsState(if (mode == "off") 0.45f else 1f, tween(600))

    val loop = rememberInfiniteTransition()
    val breath by loop.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
    )
    val pulse by loop.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )

    val maxCost = remember(cities) { cities.maxOfOrNull { it.costIls } ?: 0.0 }

    Canvas(modifier) {
        val alpha = dim * (if (pending) pulse else 1f)
        // Fit the bounding box into the canvas, keeping true proportions.
        val boxW = (LON_MAX - LON_MIN) * LON_SCALE
        val boxH = LAT_MAX - LAT_MIN
        val scale = min(size.width / boxW, size.height / boxH).toFloat()
        val offX = (size.width - boxW.toFloat() * scale) / 2
        val offY = (size.height - boxH.toFloat() * scale) / 2
        fun at(lat: Double, lon: Double) = Offset(
            offX + ((lon - LON_MIN) * LON_SCALE).toFloat() * scale,
            offY + (LAT_MAX - lat).toFloat() * scale,
        )

        val country = Path().apply {
            var i = 0
            while (i < ISRAEL_OUTLINE.size) {
                val p = at(ISRAEL_OUTLINE[i + 1], ISRAEL_OUTLINE[i])
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                i += 2
            }
            close()
        }
        drawPath(country, countryFill.copy(alpha = countryFill.alpha * alpha), style = Fill)
        drawPath(
            country, countryEdge.copy(alpha = countryEdge.alpha * alpha),
            style = Stroke(width = 1.2f * density, join = StrokeJoin.Round),
        )

        val radiusPx = (radiusKm / 111.0).toFloat() * scale
        for (c in centers) {
            val p = at(c.lat, c.lon)
            localCircle(p, radiusPx, circleFill, breath, alpha)
        }

        // Biggest spend drawn first, so small dots stay visible on top.
        for (city in cities) {
            if (maxCost <= 0.0) break
            val p = at(city.lat, city.lon)
            val weight = sqrt(city.costIls / maxCost).toFloat()
            // Kept small: the centre of the country holds dozens of cities, and
            // big halos merged them into one white blob.
            val r = (1.3f + 3.9f * weight) * density
            val color = if (city.inArea) Palette.LocalGlow else Color(0xFFDCE4EC)
            drawCircle(color.copy(alpha = 0.16f * alpha), r * 1.6f, p)
            drawCircle(color.copy(alpha = 0.9f * alpha), r, p)
        }

        for (c in centers) {
            drawCircle(Color.White.copy(alpha = alpha), 2.4f * density, at(c.lat, c.lon))
        }
    }
}

private fun DrawScope.localCircle(p: Offset, r: Float, on: Float, breath: Float, alpha: Float) {
    // Off: a dashed outline — a place you could switch to.
    val dashed = Stroke(
        width = 1.4f * density,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 3f * density)),
        cap = StrokeCap.Round,
    )
    drawCircle(Palette.LocalGlow.copy(alpha = 0.55f * (1 - on) * alpha), r, p, style = dashed)

    if (on <= 0f) return
    // On: filled, with a ring that slowly expands and fades — "broadcasting".
    drawCircle(Palette.Local.copy(alpha = 0.38f * on * alpha), r, p)
    drawCircle(Palette.LocalGlow.copy(alpha = 0.95f * on * alpha), r, p, style = Stroke(1.8f * density))
    val ringR = r * (1f + 0.55f * breath)
    drawCircle(
        Palette.LocalGlow.copy(alpha = 0.5f * (1f - breath) * on * alpha),
        max(ringR, r), p, style = Stroke(1.2f * density),
    )
}
