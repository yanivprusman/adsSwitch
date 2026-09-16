package com.automatelinux.adsSwitch.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One HTTP round trip; Android supplies it with the JDK client. */
expect suspend fun httpRequest(
    method: String,
    url: String,
    token: String,
    jsonBody: String?,
): HttpResult

data class HttpResult(val code: Int, val body: String, val transportError: String? = null)

@Serializable
data class Campaign(
    val id: String,
    val label: String,
    val status: String,
    val budgetIls: Double = 0.0,
    val todayCostIls: Double = 0.0,
    val todayClicks: Int = 0,
) {
    val enabled: Boolean get() = status == "ENABLED"
}

@Serializable
data class AdSet(
    val on: Boolean = false,
    val enabledCount: Int = 0,
    val campaigns: List<Campaign> = emptyList(),
    val dailyBudgetIls: Double = 0.0,
    val todayCostIls: Double = 0.0,
    val todayClicks: Int = 0,
)

@Serializable
data class Sets(val local: AdSet = AdSet(), val nationwide: AdSet = AdSet())

@Serializable
data class Center(val name: String, val he: String = "", val lat: Double, val lon: Double)

/** `mode` is local | nationwide | off | mixed (campaigns toggled by hand). */
@Serializable
data class AreaState(
    val ok: Boolean = false,
    val error: String? = null,
    val mode: String = "",
    val changedFrom: String? = null,
    val updatedAt: String = "",
    val radiusKm: Double = 35.0,
    val centers: List<Center> = emptyList(),
    val sets: Sets = Sets(),
)

@Serializable
data class Spend(val costIls: Double = 0.0, val clicks: Int = 0)

@Serializable
data class GeoCity(
    val id: String,
    val name: String = "",
    val he: String = "",
    val lat: Double,
    val lon: Double,
    val distanceKm: Int = 0,
    val inArea: Boolean = false,
    val costIls: Double = 0.0,
    val clicks: Int = 0,
)

/** Where the ad money went over the last [days], by the searcher's city. */
@Serializable
data class GeoReport(
    val ok: Boolean = false,
    val error: String? = null,
    val days: Int = 0,
    val radiusKm: Double = 35.0,
    val centers: List<Center> = emptyList(),
    val totals: Spend = Spend(),
    val inArea: Spend = Spend(),
    val unlocated: Spend = Spend(),
    val cities: List<GeoCity> = emptyList(),
)

@Serializable
private data class SwitchRequest(val mode: String)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The app's whole conversation with the backend. Failures come back as values
 * carrying a Hebrew sentence, never as exceptions thrown at the UI.
 */
class AdsApi(baseUrl: String, private val token: String) {
    private val base = baseUrl.trimEnd('/')

    suspend fun state(): AreaState =
        call("GET", "/api/area", null, AreaState.serializer()) { AreaState(error = it) }

    suspend fun switchTo(mode: String): AreaState =
        call(
            "POST", "/api/area",
            json.encodeToString(SwitchRequest.serializer(), SwitchRequest(mode)),
            AreaState.serializer(),
        ) { AreaState(error = it) }

    suspend fun geo(days: Int): GeoReport =
        call("GET", "/api/area/geo?days=$days", null, GeoReport.serializer()) { GeoReport(error = it) }

    private suspend fun <T> call(
        method: String,
        path: String,
        body: String?,
        serializer: kotlinx.serialization.KSerializer<T>,
        failure: (String) -> T,
    ): T {
        if (token.isEmpty()) return failure("הגרסה הזו נבנתה בלי טוקן — בנה מחדש עם mobile/.env")
        val r = httpRequest(method, "$base$path", token, body)
        return runCatching { json.decodeFromString(serializer, r.body) }
            .getOrElse {
                failure(
                    when (r.code) {
                        401 -> "המכשיר לא מורשה מול השרת (טוקן שגוי)"
                        0 -> "אין חיבור לשרת — בדוק שה‑VPN פעיל"
                        else -> "השרת החזיר תשובה לא צפויה (${r.code})"
                    },
                )
            }
    }
}
