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
            delay(1_000)
            searchSeconds++
        }
    }
    val minutes = searchSeconds / 60
    val seconds = searchSeconds % 60

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        world.getPlayer(1)?.let { healthBar(it, Color.Red) }
        Text(
            text = "%d:%02d".format(minutes, seconds),
            color = Color.White,
            fontSize = 32.sp,
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
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 64.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "⟳  Searching for opponent…",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier =
                    Modifier
                        .alpha(alpha)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "Cancel",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier =
                Modifier
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
        )
    }
}
