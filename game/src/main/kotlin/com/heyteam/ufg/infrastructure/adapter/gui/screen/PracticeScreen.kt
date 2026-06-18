package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.domain.entity.World
import kotlinx.coroutines.delay

private const val SEARCH_TIMER_UPDATE_MS = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val SEARCH_ANIMATION_DURATION_MS = 900
private const val HUD_PADDING_DP = 16
private const val HUD_TEXT_SIZE_SP = 32

// Searching overlay
private const val OVERLAY_TOP_PADDING_DP = 64
private const val OVERLAY_COLUMN_SPACING_DP = 12
private const val OVERLAY_TEXT_SIZE_SP = 15
private const val OVERLAY_TEXT_CANCEL_SIZE_SP = 13
private const val OVERLAY_PADDING_HORIZONTAL_DP = 20
private const val OVERLAY_PADDING_VERTICAL_DP = 8
private const val OVERLAY_BACKGROUND_ALPHA = 0.8f

// Controls hint
private const val CONTROLS_BOTTOM_PADDING_DP = 16
private const val CONTROLS_PADDING_HORIZONTAL_DP = 20
private const val CONTROLS_PADDING_VERTICAL_DP = 10
private const val CONTROLS_SPACING_DP = 28
private const val CONTROL_KEY_TEXT_SIZE_SP = 13
private const val CONTROL_LABEL_TEXT_SIZE_SP = 11

@Composable
fun practiceScreen(
    world: World,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        stageCanvas(world)
        practiceHud(world)
        searchingOverlay(onCancel)
        controlsHint()
    }
}

@Composable
private fun practiceHud(world: World) {
    var searchSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SEARCH_TIMER_UPDATE_MS)
            searchSeconds++
        }
    }
    val minutes = searchSeconds / SECONDS_PER_MINUTE
    val seconds = searchSeconds % SECONDS_PER_MINUTE

    Row(
        modifier = Modifier.fillMaxWidth().padding(HUD_PADDING_DP.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        world.getPlayer(1)?.let { healthBar(it, Color.Red) }
        Text(
            text = "%d:%02d".format(minutes, seconds),
            color = Color.White,
            fontSize = HUD_TEXT_SIZE_SP.sp,
        )
        world.getPlayer(2)?.let { healthBar(it, Color.Blue) }
    }
}

@Composable
private fun searchingOverlay(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = SEARCH_ANIMATION_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = OVERLAY_TOP_PADDING_DP.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OVERLAY_COLUMN_SPACING_DP.dp),
        ) {
            Text(
                text = "⟳  Searching for opponent…",
                color = Color.White,
                fontSize = OVERLAY_TEXT_SIZE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .alpha(alpha)
                        .background(Color.Black.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
                        .padding(
                            horizontal = OVERLAY_PADDING_HORIZONTAL_DP.dp,
                            vertical = OVERLAY_PADDING_VERTICAL_DP.dp,
                        ),
            )
            Text(
                text = "Cancel",
                color = Color.LightGray,
                fontSize = OVERLAY_TEXT_CANCEL_SIZE_SP.sp,
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
                        .clickable(onClick = onCancel)
                        .padding(
                            horizontal = OVERLAY_PADDING_HORIZONTAL_DP.dp,
                            vertical = OVERLAY_PADDING_VERTICAL_DP.dp,
                        ),
            )
        }
    }
}

@Composable
private fun controlsHint() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = CONTROLS_BOTTOM_PADDING_DP.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier =
                Modifier
                    .background(Color.Black.copy(alpha = OVERLAY_BACKGROUND_ALPHA))
                    .padding(
                        horizontal = CONTROLS_PADDING_HORIZONTAL_DP.dp,
                        vertical = CONTROLS_PADDING_VERTICAL_DP.dp,
                    ),
            horizontalArrangement = Arrangement.spacedBy(CONTROLS_SPACING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            controlKey("A / D", "Move")
            controlKey("W", "Jump")
            controlKey("S", "Crouch")
            controlKey("P", "Punch")
            controlKey("K", "Kick")
            controlKey("↓ + P/K", "Down attack")
        }
    }
}

@Composable
private fun controlKey(
    key: String,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = key,
            color = Color.Yellow,
            fontSize = CONTROL_KEY_TEXT_SIZE_SP.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = CONTROL_LABEL_TEXT_SIZE_SP.sp,
        )
    }
}
