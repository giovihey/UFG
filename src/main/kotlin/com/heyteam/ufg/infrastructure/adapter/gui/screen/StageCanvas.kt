package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.heyteam.ufg.domain.config.GameConstants.FLOOR_Y
import com.heyteam.ufg.domain.entity.World

@Composable
fun stageCanvas(world: World) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw the floor
        drawLine(
            color = Color.White,
            start = Offset(0f, FLOOR_Y.toFloat()),
            end = Offset(size.width, FLOOR_Y.toFloat()),
            strokeWidth = 2f,
        )

        // Later we'll replace these with sprites, but rectangles are the right starting point for a
        // fighting game (we can see the hitboxes).

        // Draw each player as a rectangle (their hurtbox)
        world.players.values.forEach { player ->
            drawRect(
                color = Color.Red,
                topLeft =
                    Offset(
                        player.position.x.toFloat(),
                        player.position.y.toFloat(),
                    ),
                size =
                    Size(
                        player.hurtBox.width.toFloat(),
                        player.hurtBox.height.toFloat(),
                    ),
            )
            // Hitbox — only visible during ACTIVE phase, drawn as outline
            player.attackState?.activeHitBox(player.position)?.let { box ->
                drawRect(
                    color = Color.Yellow,
                    topLeft = Offset(box.x.toFloat(), box.y.toFloat()),
                    size = Size(box.width.toFloat(), box.height.toFloat()),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}
