package com.automatelinux.adsSwitch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.automatelinux.adsSwitch.ui.theme.Palette
import com.automatelinux.adsSwitch.ui.theme.modeColor

private data class Segment(val mode: String, val label: String, val icon: ImageVector)

private val SEGMENTS = listOf(
    Segment("local", "אזורי", Icons.Rounded.MyLocation),
    Segment("nationwide", "כל הארץ", Icons.Rounded.Public),
    Segment("off", "כבוי", Icons.Rounded.PowerSettingsNew),
)

/**
 * The switch. Three segments, one sliding pill in the running state's colour.
 *
 * The pill only ever moves to where GOOGLE says the ads are — a tap puts a
 * spinner in the tapped segment and leaves the pill where it is until the
 * answer lands, so the control never shows a state that is not true yet.
 */
@Composable
fun ModeSwitch(
    mode: String,
    pendingMode: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = SEGMENTS.indexOfFirst { it.mode == mode }
    val pillColor by animateColorAsState(modeColor(mode), tween(350))
    val pillAlpha by animateFloatAsState(if (index >= 0) 1f else 0f, tween(250))

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Card)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .padding(6.dp),
    ) {
        val segmentWidth = maxWidth / SEGMENTS.size
        val pillOffset by animateDpAsState(
            segmentWidth * (if (index >= 0) index else 0),
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        )
        Box(
            Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .alpha(pillAlpha)
                .clip(RoundedCornerShape(19.dp))
                .background(pillColor),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            SEGMENTS.forEach { seg ->
                val selected = seg.mode == mode
                val busy = seg.mode == pendingMode
                val press = remember { MutableInteractionSource() }
                val content by animateColorAsState(
                    if (selected) Color.White else Palette.InkSoft, tween(300),
                )
                val scale by animateFloatAsState(if (busy) 0.94f else 1f, tween(160))
                Box(
                    Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(
                            interactionSource = press,
                            indication = androidx.compose.material3.ripple(),
                            enabled = enabled && !selected,
                            role = Role.Tab,
                        ) { onSelect(seg.mode) }
                        .semantics {
                            this.selected = selected
                            contentDescription = if (selected) "${seg.label} — פועל עכשיו" else "העבר ל${seg.label}"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.scale(scale),
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (busy) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    color = modeColor(seg.mode),
                                    strokeWidth = 2.5.dp,
                                )
                            } else {
                                Icon(seg.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            seg.label,
                            color = content,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
