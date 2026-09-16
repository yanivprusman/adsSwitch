package com.automatelinux.adsSwitch.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.adsSwitch.ui.theme.Palette
import com.automatelinux.adsSwitch.ui.theme.modeGlow

private const val UNDO_MS = 8000

/**
 * The undo for the switch that just happened, right under the switch.
 *
 * It replaced a snackbar: the system decides a snackbar's lifetime, and on this
 * phone it was gone in about four seconds — too short to notice, read and
 * reach. This one lasts eight seconds and shows the time running out, so there
 * is no guessing whether the chance is still there.
 */
@Composable
fun UndoStrip(nowMode: String, backTo: String, key: Int, onUndo: () -> Unit, onExpire: () -> Unit) {
    val left = remember(key) { Animatable(1f) }
    LaunchedEffect(key) {
        left.animateTo(0f, tween(UNDO_MS, easing = LinearEasing))
        onExpire()
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Ink),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(modeGlow(nowMode)))
            Spacer(Modifier.width(10.dp))
            Text(
                when (nowMode) {
                    "local" -> "עבר לאזור שלך"
                    "nationwide" -> "עבר לכל הארץ"
                    "off" -> "כל המודעות כובו"
                    else -> "עודכן"
                },
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onUndo)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, tint = Palette.LocalGlow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "ביטול · חזרה ל${modeShort(backTo)}",
                    color = Palette.LocalGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp)) {
            Box(Modifier.fillMaxWidth(left.value).height(3.dp).background(modeGlow(nowMode).copy(alpha = 0.8f)))
        }
    }
}
