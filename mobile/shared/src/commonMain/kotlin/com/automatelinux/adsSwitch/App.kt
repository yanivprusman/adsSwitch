package com.automatelinux.adsSwitch

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.adsSwitch.data.AdsApi
import com.automatelinux.adsSwitch.data.AreaState
import com.automatelinux.adsSwitch.data.GeoReport
import com.automatelinux.adsSwitch.ui.CampaignsCard
import com.automatelinux.adsSwitch.ui.FullError
import com.automatelinux.adsSwitch.ui.GeoCard
import com.automatelinux.adsSwitch.ui.Hero
import com.automatelinux.adsSwitch.ui.HeroSkeleton
import com.automatelinux.adsSwitch.ui.InlineError
import com.automatelinux.adsSwitch.ui.ModeSwitch
import com.automatelinux.adsSwitch.ui.SectionCard
import com.automatelinux.adsSwitch.ui.SiteTabs
import com.automatelinux.adsSwitch.ui.SkeletonLines
import com.automatelinux.adsSwitch.ui.UndoStrip
import com.automatelinux.adsSwitch.ui.modeShort
import com.automatelinux.adsSwitch.ui.theme.AppTheme
import com.automatelinux.adsSwitch.ui.theme.Palette
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * מודעות — where the Google Ads run, and whether that is working.
 *
 * Every campaign exists once per version: nationwide, local (35 km around Be'er
 * Sheva and Midreshet Ben-Gurion), south, center and north. Each site has its own
 * switch, and the daemon flips that site's campaigns in one atomic change.
 *
 * Nothing is decided or assumed on the phone. What is shown as running is what
 * Google reported back — after a switch too — never what was tapped. A switch
 * is one tap and is undoable for a few seconds, instead of asking "are you
 * sure?" every time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(baseUrl: String, token: String, resumeKey: Int = 0) {
    val api = remember(baseUrl, token) { AdsApi(baseUrl, token) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<AreaState?>(null) }
    var stateError by remember { mutableStateOf("") }
    var geo by remember { mutableStateOf<GeoReport?>(null) }
    var geoError by remember { mutableStateOf("") }
    var geoDays by remember { mutableIntStateOf(30) }
    var geoLoading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<String?>(null) }
    // The undo offer for the last switch: which mode to go back to, and a key
    // that restarts its countdown when a newer switch replaces it.
    var undoTo by remember { mutableStateOf<String?>(null) }
    var undoSite by remember { mutableStateOf("") }
    var selectedSite by remember { mutableStateOf("pigeons") }
    var undoKey by remember { mutableIntStateOf(0) }

    suspend fun loadState() {
        val s = api.state()
        if (s.ok) {
            state = s
            stateError = ""
        } else {
            stateError = s.error ?: "שגיאה לא ידועה"
        }
    }

    suspend fun loadGeo(days: Int) {
        geoLoading = true
        val askedFor = selectedSite
        val g = api.geo(days, askedFor)
        // A slower answer for a period or site the user has since left must not win.
        if (days == geoDays && askedFor == selectedSite) {
            if (g.ok) {
                geo = g
                geoError = ""
            } else {
                geoError = g.error ?: "שגיאה לא ידועה"
            }
        }
        geoLoading = false
    }

    suspend fun refreshAll() {
        refreshing = true
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        val a = scope.async { loadState() }
        val b = scope.async { loadGeo(geoDays) }
        a.await()
        b.await()
        // A refresh that answers in 80 ms looks like it was ignored; hold the
        // indicator long enough to register that it ran.
        val left = 650 - started.elapsedNow().inWholeMilliseconds
        if (left > 0) delay(left)
        refreshing = false
    }

    fun switchTo(siteId: String, target: String, isUndo: Boolean = false) {
        if (pending != null) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // An undo offer for the previous switch no longer describes the state.
        undoTo = null
        snackbar.currentSnackbarData?.dismiss()
        pending = target
        scope.launch {
            val s = api.switchTo(siteId, target)
            pending = null
            if (!s.ok) {
                snackbar.showSnackbar("לא הוחלף: ${s.error ?: "שגיאה לא ידועה"}", duration = SnackbarDuration.Long)
                return@launch
            }
            state = s
            stateError = ""
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val from = s.changedFrom
            if (!isUndo && from != null && from != "mixed") {
                undoTo = from
                undoSite = siteId
                undoKey++
            } else if (isUndo) {
                snackbar.showSnackbar("בוטל — חזר ל${modeShort(s.mode)}")
            }
        }
    }

    LaunchedEffect(resumeKey) { refreshAll() }

    AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Palette.Page,
                snackbarHost = {
                    SnackbarHost(snackbar) { data ->
                        Snackbar(
                            data,
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Palette.Ink,
                            contentColor = Color.White,
                            actionColor = Palette.LocalGlow,
                        )
                    }
                },
            ) { inner ->
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { scope.launch { refreshAll() } },
                    modifier = Modifier.fillMaxSize().padding(inner),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        TopBar(refreshing = refreshing || pending != null) { scope.launch { refreshAll() } }

                        val s = state
                        when {
                            s == null && stateError.isNotEmpty() ->
                                FullError(stateError) { scope.launch { refreshAll() } }
                            s == null -> {
                                HeroSkeleton()
                                SectionCard { SkeletonLines(3) }
                            }
                            else -> {
                                if (stateError.isNotEmpty()) InlineError(stateError) { scope.launch { refreshAll() } }

                                val site = s.sites.firstOrNull { it.id == selectedSite } ?: s.sites.first()

                                SiteTabs(s.sites, site.id) { id ->
                                    selectedSite = id
                                    geo = null
                                    scope.launch { loadGeo(geoDays) }
                                }

                                Hero(s, site, geo, pending)

                                Column(Modifier.animateContentSize()) {
                                    ModeSwitch(
                                        mode = site.mode,
                                        pendingMode = pending,
                                        enabled = pending == null,
                                        onSelect = { switchTo(site.id, it) },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    val offer = undoTo
                                    if (offer != null && pending == null && undoSite == site.id) {
                                        UndoStrip(
                                            nowMode = site.mode,
                                            backTo = offer,
                                            key = undoKey,
                                            onUndo = { switchTo(site.id, offer, isUndo = true) },
                                            onExpire = { undoTo = null },
                                        )
                                    } else {
                                        Text(
                                            "הבחירה חלה רק על ${site.label}, ואפשר לבטל מיד אחרי.",
                                            fontSize = 12.sp, color = Palette.InkFaint,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                        )
                                    }
                                }

                                GeoCard(
                                    geo = geo,
                                    days = geoDays,
                                    loading = geoLoading,
                                    error = geoError,
                                    onDays = { d ->
                                        geoDays = d
                                        scope.launch { loadGeo(d) }
                                    },
                                    onRetry = { scope.launch { loadGeo(geoDays) } },
                                )

                                CampaignsCard(site)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(refreshing: Boolean, onRefresh: () -> Unit) {
    val spin = rememberInfiniteTransition()
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(900), RepeatMode.Restart))
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("מודעות", style = MaterialTheme.typography.titleLarge, color = Palette.Ink, fontWeight = FontWeight.Black)
            Text("Google Ads · איפה הן רצות ומה זה מביא", fontSize = 13.sp, color = Palette.InkFaint)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "רענון",
                tint = Palette.InkSoft,
                modifier = if (refreshing) Modifier.rotate(angle) else Modifier,
            )
        }
    }
}
