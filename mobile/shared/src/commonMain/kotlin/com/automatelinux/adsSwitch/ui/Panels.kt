package com.automatelinux.adsSwitch.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.adsSwitch.data.AreaState
import com.automatelinux.adsSwitch.data.Campaign
import com.automatelinux.adsSwitch.data.GeoReport
import com.automatelinux.adsSwitch.ui.theme.Palette
import com.automatelinux.adsSwitch.ui.theme.modeColor
import com.automatelinux.adsSwitch.ui.theme.modeGlow

// ── hero ────────────────────────────────────────────────────────────────────

/**
 * The first thing on screen answers two questions at a glance: where are my
 * ads running, and what have they cost today. The map beside it shows the
 * first; the one big number shows the second.
 */
@Composable
fun Hero(state: AreaState, geo: GeoReport?, pendingMode: String?) {
    val mode = state.mode
    val glow by animateColorAsState(modeGlow(mode), tween(500))
    val running = when (mode) {
        "local" -> state.sets.local
        "nationwide" -> state.sets.nationwide
        else -> null
    }
    val todayCost = state.sets.local.todayCostIls + state.sets.nationwide.todayCostIls
    val todayClicks = state.sets.local.todayClicks + state.sets.nationwide.todayClicks

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Palette.Night)
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (pendingMode != null) "מעביר ל${modeShort(pendingMode)}…" else "המודעות רצות עכשיו",
                color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            AnimatedContent(
                mode,
                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
            ) { m ->
                Column {
                    Text(modeTitle(m), color = modeGlow(m), style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when (m) {
                            "local" -> {
                                val names = state.centers.joinToString(" ו") { it.he }
                                "${state.radiusKm.toInt()} ק״מ סביב $names"
                            }
                            "nationwide" -> "כל ישראל. המעגלים המקווקווים הם האזור שלך."
                            "off" -> "אף מודעה לא מוצגת, ולא יורד כסף."
                            else -> "חלק מהקמפיינים הופעלו ידנית. בחירה למטה תסדר את כולם."
                        },
                        color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, lineHeight = 19.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text("היום", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    shekels(todayCost, forceAgorot = todayCost < 100),
                    color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black,
                )
                if (running != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "מתוך ${shekels(running.dailyBudgetIls)}",
                        color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            if (running != null && running.dailyBudgetIls > 0) {
                Spacer(Modifier.height(8.dp))
                Meter(
                    (todayCost / running.dailyBudgetIls).toFloat(),
                    fill = glow, track = Color.White.copy(alpha = 0.12f), height = 6.dp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (todayClicks) {
                    0 -> "עוד אין קליקים היום"
                    1 -> "קליק אחד · ${shekels(todayCost, true)} לקליק"
                    else -> "${count(todayClicks)} קליקים · ${shekels(todayCost / todayClicks, true)} לקליק"
                },
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "עודכן ${clock(state.updatedAt)}",
                color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AreaMap(
                mode = mode,
                pending = pendingMode != null,
                centers = state.centers,
                radiusKm = state.radiusKm,
                cities = geo?.cities ?: emptyList(),
                modifier = Modifier.height(292.dp).width(292.dp * MAP_ASPECT),
            )
            if (geo != null && geo.cities.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LegendDot(Palette.LocalGlow, "קליקים מהאזור")
                LegendDot(Color(0xFFE8EEF5), "קליקים מרחוק")
                Text(
                    "${geo.days} ימים אחרונים",
                    color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
fun Meter(fraction: Float, fill: Color, track: Color, height: Dp, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(fill),
            )
        }
    }
}

// ── where the clicks came from ───────────────────────────────────────────────

val GEO_PERIODS = listOf(7, 30, 90)

/**
 * The question the local/nationwide split exists for, answered with the
 * account's own history: what share of the money came from near home, and
 * which cities took the rest.
 */
@Composable
fun GeoCard(
    geo: GeoReport?,
    days: Int,
    loading: Boolean,
    error: String,
    onDays: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("מאיפה הגיעו הקליקים", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            GEO_PERIODS.forEach { d ->
                Spacer(Modifier.width(6.dp))
                PeriodChip("$d ימים", selected = d == days) { onDays(d) }
            }
        }
        Spacer(Modifier.height(14.dp))

        when {
            geo == null && error.isNotEmpty() -> InlineError(error, onRetry)
            geo == null -> SkeletonLines(4)
            geo.totals.clicks == 0 && geo.totals.costIls == 0.0 ->
                Text("אין קליקים ב‑$days הימים האחרונים.", color = Palette.InkSoft)
            else -> GeoBody(geo, loading)
        }
    }
}

@Composable
private fun GeoBody(geo: GeoReport, loading: Boolean) {
    val alpha by animateFloatAsState(if (loading) 0.45f else 1f, tween(200))
    val share = percent(geo.inArea.costIls, geo.totals.costIls)
    val outside = geo.totals.costIls - geo.inArea.costIls - geo.unlocated.costIls

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "$share%",
                color = Palette.Local.copy(alpha = alpha), fontSize = 40.sp, fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "מהכסף הגיע מקליקים באזור שלך",
                color = Palette.Ink.copy(alpha = alpha), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            "${shekels(geo.inArea.costIls)} מתוך ${shekels(geo.totals.costIls)} · " +
                "${count(geo.inArea.clicks)} מתוך ${count(geo.totals.clicks)} קליקים",
            color = Palette.InkSoft.copy(alpha = alpha), fontSize = 13.sp,
        )
        Spacer(Modifier.height(12.dp))
        StackedBar(
            listOf(
                geo.inArea.costIls to Palette.Local,
                outside to Color(0xFF9AA5B1),
                geo.unlocated.costIls to Palette.Line,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Swatch(Palette.Local, "באזור (עד ${geo.radiusKm.toInt()} ק״מ)")
            Spacer(Modifier.width(14.dp))
            Swatch(Color(0xFF9AA5B1), "מחוץ לאזור")
        }

        Spacer(Modifier.height(16.dp))
        val top = geo.cities.take(6)
        val max = top.maxOfOrNull { it.costIls } ?: 0.0
        top.forEach { c ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(9.dp).clip(CircleShape)
                        .background(if (c.inArea) Palette.Local else Color(0xFF9AA5B1)),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.he.ifEmpty { c.name }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (c.inArea) "באזור" else "${c.distanceKm} ק״מ",
                            fontSize = 12.sp, color = if (c.inArea) Palette.Local else Palette.InkFaint,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Meter(
                        if (max > 0) (c.costIls / max).toFloat() else 0f,
                        fill = if (c.inArea) Palette.Local else Color(0xFFB9C2CC),
                        track = Color.Transparent, height = 4.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(shekels(c.costIls), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    Text("${count(c.clicks)} קליקים", fontSize = 11.sp, color = Palette.InkFaint)
                }
            }
        }
        if (geo.unlocated.costIls >= 1) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${shekels(geo.unlocated.costIls)} מקליקים שגוגל לא ידע לשייך לעיר",
                fontSize = 12.sp, color = Palette.InkFaint,
            )
        }
    }
}

@Composable
private fun StackedBar(parts: List<Pair<Double, Color>>) {
    val total = parts.sumOf { it.first }
    Row(
        Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(Palette.OffSoft),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total <= 0) return@Row
        parts.filter { it.first > 0 }.forEach { (v, color) ->
            Box(Modifier.weight((v / total).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(color))
        }
    }
}

@Composable
private fun Swatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = Palette.InkSoft)
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Palette.Ink else Color.Transparent, tween(200))
    val fg by animateColorAsState(if (selected) Color.White else Palette.InkSoft, tween(200))
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) Palette.Ink else Palette.Line, RoundedCornerShape(50))
            .clickable(enabled = !selected, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── campaigns ────────────────────────────────────────────────────────────────

/** Each campaign once, with whichever of its two copies is running. */
@Composable
fun CampaignsCard(state: AreaState) {
    val local = state.sets.local.campaigns
    val national = state.sets.nationwide.campaigns
    SectionCard {
        Text("הקמפיינים", style = MaterialTheme.typography.titleMedium)
        Text(
            "לכל קמפיין יש עותק ארצי ועותק אזורי. המתג מפעיל אחד מהם.",
            fontSize = 12.sp, color = Palette.InkFaint,
        )
        Spacer(Modifier.height(8.dp))
        local.forEachIndexed { i, l ->
            val n = national.getOrNull(i)
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
            CampaignRow(l, n)
        }
    }
}

@Composable
private fun CampaignRow(local: Campaign, national: Campaign?) {
    val both = local.enabled && national?.enabled == true
    val runMode = when {
        both -> "mixed"
        local.enabled -> "local"
        national?.enabled == true -> "nationwide"
        else -> "off"
    }
    val running = if (national?.enabled == true && !local.enabled) national else local
    val cost = local.todayCostIls + (national?.todayCostIls ?: 0.0)
    val clicks = local.todayClicks + (national?.todayClicks ?: 0)

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                local.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(runMode)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Meter(
                if (running.budgetIls > 0) (cost / running.budgetIls).toFloat() else 0f,
                fill = modeColor(runMode), track = Palette.OffSoft, height = 5.dp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${shekels(cost, cost in 0.01..99.99)} / ${shekels(running.budgetIls)}",
                fontSize = 12.sp, color = Palette.InkSoft, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            if (clicks == 0) "אין קליקים היום" else "${count(clicks)} קליקים היום",
            fontSize = 12.sp, color = Palette.InkFaint,
        )
    }
}

@Composable
private fun StatusPill(mode: String) {
    val (label, fg, bg) = when (mode) {
        "local" -> Triple("פועל באזור", Palette.Local, Palette.LocalSoft)
        "nationwide" -> Triple("פועל בכל הארץ", Palette.National, Palette.NationalSoft)
        "mixed" -> Triple("שני העותקים פועלים", Palette.Mixed, Palette.MixedSoft)
        else -> Triple("מושהה", Palette.Off, Palette.OffSoft)
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

// ── building blocks ─────────────────────────────────────────────────────────

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) { content() }
}

@Composable
fun InlineError(message: String, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.DangerSoft).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Palette.Danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = Palette.Danger, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            "נסה שוב", color = Palette.Danger, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRetry).padding(8.dp),
        )
    }
}

/** A connection problem before anything has loaded gets the whole screen. */
@Composable
fun FullError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Palette.DangerSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Palette.Danger, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("אי אפשר לדעת מה רץ עכשיו", style = MaterialTheme.typography.titleLarge, color = Palette.Ink)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Palette.InkSoft, fontSize = 15.sp)
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Palette.Ink)
                .clickable(onClick = onRetry)
                .padding(horizontal = 28.dp, vertical = 12.dp),
        ) {
            Text("נסה שוב", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Shimmer(modifier: Modifier) {
    val t = rememberInfiniteTransition()
    val a by t.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(800), RepeatMode.Reverse))
    Box(modifier.background(Palette.Line.copy(alpha = a)))
}

@Composable
fun SkeletonLines(lines: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Shimmer(Modifier.width(120.dp).height(34.dp).clip(RoundedCornerShape(8.dp)))
        repeat(lines) { i ->
            Shimmer(Modifier.fillMaxWidth(if (i % 2 == 0) 1f else 0.7f).height(14.dp).clip(RoundedCornerShape(7.dp)))
        }
    }
}

/** First load: the hero and switch in outline, so the layout does not jump. */
@Composable
fun HeroSkeleton() {
    val t = rememberInfiniteTransition()
    val a by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse))
    Box(
        Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(28.dp))
            .background(Palette.Night.copy(alpha = a)),
        contentAlignment = Alignment.Center,
    ) {
        Text("בודק מה רץ עכשיו…", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
    }
}
