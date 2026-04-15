package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.heyteam.ufg.domain.config.GameConstants.COLOR_FLOOR
import com.heyteam.ufg.domain.config.GameConstants.COLOR_STAGE_BACKGROUND
import com.heyteam.ufg.domain.config.GameConstants.COLOR_STAGE_MARGIN
import com.heyteam.ufg.domain.config.GameConstants.FLOOR_Y
import com.heyteam.ufg.domain.config.GameConstants.STAGE_HEIGHT
import com.heyteam.ufg.domain.config.GameConstants.STAGE_MARGIN
import com.heyteam.ufg.domain.config.GameConstants.STAGE_WIDTH
import com.heyteam.ufg.domain.entity.World

@Composable
fun stageCanvas(world: World) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Stage background
        drawRect(
            color = Color(COLOR_STAGE_BACKGROUND),
            topLeft = Offset(0f, 0f),
            size = Size(STAGE_WIDTH.toFloat(), STAGE_HEIGHT.toFloat()),
        )

        // Stage margin boundaries (left and right walls)
        drawLine(
            color = Color(COLOR_STAGE_MARGIN),
            start = Offset(STAGE_MARGIN.toFloat(), 0f),
            end = Offset(STAGE_MARGIN.toFloat(), STAGE_HEIGHT.toFloat()),
            strokeWidth = 1f,
        )
        drawLine(
            color = Color(COLOR_STAGE_MARGIN),
            start = Offset((STAGE_WIDTH - STAGE_MARGIN).toFloat(), 0f),
            end = Offset((STAGE_WIDTH - STAGE_MARGIN).toFloat(), STAGE_HEIGHT.toFloat()),
            strokeWidth = 1f,
        )

        // Floor
        drawRect(
            color = Color(COLOR_FLOOR),
            topLeft = Offset(0f, FLOOR_Y.toFloat()),
            size = Size(STAGE_WIDTH.toFloat(), (STAGE_HEIGHT - FLOOR_Y).toFloat()),
        )
        drawLine(
            color = Color.White,
            start = Offset(0f, FLOOR_Y.toFloat()),
            end = Offset(STAGE_WIDTH.toFloat(), FLOOR_Y.toFloat()),
            strokeWidth = 2f,
        )

        // Later we'll replace these with sprites, but rectangles are the right starting point for a
        // fighting game (we can see the hitboxes).

        // Draw each player as a rectangle (their hurtbox)
        world.players.values.forEach { player ->
            val color = if (player.id == 1) Color.Blue else Color.Red
            drawRect(
                color = color,
                topLeft =
                    Offset(
                        player.topLeft.x.toFloat(),
                        player.topLeft.y.toFloat(),
                    ),
                size =
                    Size(
                        player.hurtBox.width.toFloat(),
                        player.hurtBox.height.toFloat(),
                    ),
            )
            // Hitbox — only visible during ACTIVE phase, drawn as outline
            player.attackState?.activeHitBox(player.topLeft)?.let { box ->
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
