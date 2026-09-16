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

/** `mode` is local | nationwide | off | mixed (campaigns toggled by hand). */
@Serializable
data class AreaState(
    val ok: Boolean = false,
    val error: String? = null,
    val mode: String = "",
    val changedFrom: String? = null,
    val radiusKm: Int? = null,
    val centers: List<String> = emptyList(),
    val sets: Sets = Sets(),
)

@Serializable
private data class SwitchRequest(val mode: String)

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * The app's whole conversation with the backend: read the state, switch it.
 * Failures come back as values with a Hebrew sentence, never as exceptions.
 */
class AdsApi(baseUrl: String, private val token: String) {
    private val base = baseUrl.trimEnd('/')

    suspend fun state(): AreaState = call("GET", null)

    suspend fun switchTo(mode: String): AreaState =
        call("POST", json.encodeToString(SwitchRequest.serializer(), SwitchRequest(mode)))

    private suspend fun call(method: String, body: String?): AreaState {
        if (token.isEmpty()) return AreaState(error = "הגרסה הזו נבנתה בלי טוקן — בנה מחדש עם mobile/.env")
        val r = httpRequest(method, "$base/api/area", token, body)
        return runCatching { json.decodeFromString(AreaState.serializer(), r.body) }
            .getOrElse {
                AreaState(
                    error = when (r.code) {
                        401 -> "המכשיר לא מורשה מול השרת (טוקן שגוי)"
                        0 -> "אין חיבור לשרת — בדוק שה‑VPN פעיל"
                        else -> "השרת החזיר תשובה לא צפויה (${r.code})"
                    },
                )
            }
    }
}
