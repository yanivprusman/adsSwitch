package com.automatelinux.adsSwitch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.adsSwitch.data.AdsApi
import com.automatelinux.adsSwitch.data.AreaState
import com.automatelinux.adsSwitch.ui.theme.AppTheme
import kotlinx.coroutines.launch

/**
 * מודעות — one screen, three buttons.
 *
 * Every Google Ads campaign exists twice: nationwide, and a local twin limited
 * to 35 km around Be'er Sheva and Midreshet Ben-Gurion. The buttons pick which
 * SET runs; the daemon switches all of them in one atomic change, so there is
 * no state here where half the campaigns are local and half are not.
 *
 * Nothing is decided on the phone. The state shown is always what Google
 * reported back — after a switch too — never what was tapped.
 */

private val Green = Color(0xFF1B7F3B)
private val Blue = Color(0xFF1A56DB)
private val Grey = Color(0xFF5F6368)
private val Amber = Color(0xFFB45309)
private val Red = Color(0xFFB3261E)

private fun money(v: Double): String {
    val cents = kotlin.math.round(v * 100).toLong()
    return if (cents % 100 == 0L) "₪${cents / 100}"
    else "₪${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

private data class ModeInfo(val title: String, val subtitle: String, val color: Color)

private fun modeInfo(mode: String): ModeInfo = when (mode) {
    "local" -> ModeInfo("פועל: אזורי", "באר שבע + מדרשת בן‑גוריון · 35 ק״מ", Green)
    "nationwide" -> ModeInfo("פועל: כל הארץ", "המודעות מוצגות בכל ישראל", Blue)
    "off" -> ModeInfo("כבוי", "אף מודעה לא רצה עכשיו", Grey)
    "mixed" -> ModeInfo(
        "מעורב",
        "חלק מהקמפיינים הופעלו ידנית — לחיצה על כפתור תסדר את כולם",
        Amber,
    )
    else -> ModeInfo("", "", Grey)
}

private fun modeName(mode: String): String = when (mode) {
    "local" -> "אזורי"
    "nationwide" -> "כל הארץ"
    "off" -> "כבוי"
    else -> "מעורב"
}

@Composable
fun App(baseUrl: String, token: String, resumeKey: Int = 0) {
    val api = remember(baseUrl, token) { AdsApi(baseUrl, token) }
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<AreaState?>(null) }
    var loading by remember { mutableStateOf(true) }
    // A connection problem is a STATE: it disappears by itself on the next
    // successful read. A switch result is an EVENT, kept apart from it.
    var loadError by remember { mutableStateOf("") }
    var switching by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Pair<String, Color>?>(null) }

    suspend fun refresh() {
        loading = true
        val s = api.state()
        if (s.ok) {
            state = s
            loadError = ""
        } else {
            loadError = s.error ?: "שגיאה לא ידועה"
        }
        loading = false
    }

    fun switchTo(mode: String) {
        if (switching != null) return
        switching = mode
        result = null
        scope.launch {
            val s = api.switchTo(mode)
            if (s.ok) {
                state = s
                loadError = ""
                result = if (s.changedFrom != null)
                    "עבר מ‑${modeName(s.changedFrom)} ל‑${modeName(s.mode)}" to modeInfo(s.mode).color
                else
                    "כבר היה ${modeName(s.mode)} — לא שונה דבר" to Grey
            } else {
                result = "לא הוחלף: ${s.error ?: "שגיאה לא ידועה"}" to Red
            }
            switching = null
        }
    }

    LaunchedEffect(resumeKey) { refresh() }

    AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF6F7F9)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Header(loading = loading || switching != null, onRefresh = {
                        scope.launch { refresh() }
                    })

                    StatusCard(state, loading)

                    if (loadError.isNotEmpty()) Notice(loadError, Red)
                    result?.let { (text, color) -> Notice(text, color) }

                    val current = state?.mode
                    val sets = state?.sets
                    ModeButton(
                        title = "אזורי",
                        subtitle = "רק באזור שלך — באר שבע + מדרשת בן‑גוריון",
                        detail = sets?.let { "תקציב עד ${money(it.local.dailyBudgetIls)} ליום" },
                        color = Green,
                        active = current == "local",
                        busy = switching == "local",
                        enabled = state != null && switching == null,
                        onClick = { switchTo("local") },
                    )
                    ModeButton(
                        title = "כל הארץ",
                        subtitle = "המודעות מוצגות בכל ישראל",
                        detail = sets?.let { "תקציב עד ${money(it.nationwide.dailyBudgetIls)} ליום" },
                        color = Blue,
                        active = current == "nationwide",
                        busy = switching == "nationwide",
                        enabled = state != null && switching == null,
                        onClick = { switchTo("nationwide") },
                    )
                    ModeButton(
                        title = "לכבות הכל",
                        subtitle = "אף מודעה לא רצה, לא יורד כסף",
                        detail = null,
                        color = Grey,
                        active = current == "off",
                        busy = switching == "off",
                        enabled = state != null && switching == null,
                        onClick = { switchTo("off") },
                    )

                    state?.let { CampaignList(it) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(loading: Boolean, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("מודעות Google", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("איפה המודעות שלך רצות", fontSize = 14.sp, color = Grey)
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "רענון")
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: AreaState?, loading: Boolean) {
    val info = state?.let { modeInfo(it.mode) }
    val color = info?.color ?: Grey
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (state == null) {
                Text(
                    if (loading) "בודק מה רץ עכשיו…" else "לא ידוע",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                )
                return@Column
            }
            Text(info!!.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(info.subtitle, color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            val cost = state.sets.local.todayCostIls + state.sets.nationwide.todayCostIls
            val clicks = state.sets.local.todayClicks + state.sets.nationwide.todayClicks
            Text(
                "היום: ${money(cost)} · $clicks קליקים",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun Notice(text: String, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    detail: String?,
    color: Color,
    active: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !active, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) color.copy(alpha = 0.10f) else Color.White,
        ),
        border = BorderStroke(if (active) 2.5.dp else 1.dp, if (active) color else Color(0xFFDADCE0)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, fontSize = 14.sp, color = Color(0xFF3C4043))
                if (detail != null) Text(detail, fontSize = 13.sp, color = Grey)
            }
            Spacer(Modifier.width(12.dp))
            when {
                busy -> CircularProgressIndicator(Modifier.size(28.dp), color = color, strokeWidth = 3.dp)
                active -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
                    Text("פועל עכשיו", fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CampaignList(state: AreaState) {
    val local = state.sets.local.campaigns
    val national = state.sets.nationwide.campaigns
    if (local.isEmpty()) return
    Text(
        "קמפיינים",
        fontSize = 17.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            local.forEachIndexed { i, l ->
                val n = national.getOrNull(i)
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(l.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip("אזורי", l.enabled, Green)
                        Spacer(Modifier.width(6.dp))
                        if (n != null) Chip("ארצי", n.enabled, Blue)
                        Spacer(Modifier.weight(1f))
                        val cost = l.todayCostIls + (n?.todayCostIls ?: 0.0)
                        val clicks = l.todayClicks + (n?.todayClicks ?: 0)
                        Text(
                            "${money(l.budgetIls)} ליום · היום ${money(cost)}, $clicks קליקים",
                            fontSize = 12.sp, color = Grey,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, on: Boolean, color: Color) {
    Box(
        Modifier
            .background(if (on) color else Color(0xFFEDEEF0), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            if (on) "$text · פועל" else "$text · מושהה",
            fontSize = 12.sp,
            color = if (on) Color.White else Grey,
            fontWeight = FontWeight.Medium,
        )
    }
}
