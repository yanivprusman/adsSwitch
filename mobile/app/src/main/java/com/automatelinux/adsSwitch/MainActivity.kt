package com.automatelinux.adsSwitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf

/**
 * Thin Android launcher — all UI lives in the shared commonMain App().
 *
 * The backend address and token are baked in at build time from mobile/.env.
 * [resumes] ticks on every onResume so the screen re-reads the ads' state each
 * time the app is brought back: a switch flipped from the terminal or Google's
 * own app must not leave a stale "on" showing here.
 */
class MainActivity : ComponentActivity() {
    private val resumes = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                baseUrl = BuildConfig.API_BASE_URL,
                token = BuildConfig.API_TOKEN,
                resumeKey = resumes.intValue,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        resumes.intValue++
    }
}
