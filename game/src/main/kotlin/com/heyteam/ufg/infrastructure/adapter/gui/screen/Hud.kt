package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.domain.config.GameConstants
import com.heyteam.ufg.domain.entity.Player
import com.heyteam.ufg.domain.entity.World

@Composable
fun hud(world: World) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Main HUD row: health bars + timer
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            world.getPlayer(1)?.let { healthBar(it, Color.Red) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${world.roundTimer}",
                    color = Color.White,
                    fontSize = 32.sp,
                )
                Text(
                    text = "Round ${world.roundNumber}",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                )
            }

            world.getPlayer(2)?.let { healthBar(it, Color.Blue) }
        }

        // Round win pips row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            roundWinPips(wins = world.roundWins.getOrDefault(1, 0), color = Color.Red)
            roundWinPips(wins = world.roundWins.getOrDefault(2, 0), color = Color.Blue)
        }
    }
}

/** Two small circles — filled for each round won. */
@Composable
private fun roundWinPips(
    wins: Int,
    color: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(GameConstants.ROUNDS_TO_WIN) { index ->
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(
                            color = if (index < wins) color else Color.DarkGray,
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
            )
        }
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
                .background(Color.DarkGray),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color),
        )
    }
}
