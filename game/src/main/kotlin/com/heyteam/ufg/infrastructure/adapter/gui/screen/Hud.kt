package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

// The HUD (health bars, timer)
@Composable
fun hud(world: World) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // P1 health bar (left side)
        world.getPlayer(1)?.let { healthBar(it, Color.Red) }

        // Round timer (center)
        Text(
            text = "${world.roundTimer}",
            color = Color.White,
            fontSize = 32.sp,
        )

        // P2 health bar (right side, if you add player 2)
        world.getPlayer(2)?.let { healthBar(it, Color.Blue) }
    }
}

@Composable
fun healthBar(
    player: Player,
    color: Color,
) {
    val fraction = player.health.current.toFloat() / player.health.max.toFloat()
    Box(
        modifier =
            Modifier
                .width(200.dp)
                .height(20.dp)
                .background(Color.DarkGray), // empty bar background
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction) // fills proportionally to health
                    .background(color),
        )
    }
}
