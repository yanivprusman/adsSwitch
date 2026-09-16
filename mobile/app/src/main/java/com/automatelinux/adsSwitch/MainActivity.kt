package com.automatelinux.adsSwitch

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // Light page, dark system-bar icons — always. Left to guess, edge-to-edge
        // follows the device's night mode and draws white icons on a light page.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
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
