package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heyteam.ufg.domain.model.GameState
import com.heyteam.ufg.domain.model.Player
import com.heyteam.ufg.domain.model.RenderPort
import com.heyteam.ufg.domain.service.GameConstants
import kotlinx.coroutines.delay

// ── UI presentation constants ─────────────────────────────────────────────────

private const val UI_POLL_INTERVAL_MS = 16L
private const val FLOOR_BAR_HEIGHT_DP = 4
private const val CONTENT_PADDING_DP = 16
private const val DEBUG_PADDING_DP = 12
private const val CARD_ELEVATION_DP = 4

private val P1_COLOR = Color.Blue
private val P2_COLOR = Color.Red
private val FLOOR_COLOR = Color.Green

class GUIAdapter : RenderPort {
    @Volatile private var latestState: GameState? = null

    override fun render(state: GameState) {
        latestState = state
    }

    @Composable
    fun gameApp() {
        var gameState by remember { mutableStateOf<GameState?>(null) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(UI_POLL_INTERVAL_MS)
                gameState = latestState
            }
        }

        val state = gameState ?: return

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            gameHeader(state)
            stageView(state)
            debugPanel(state)
        }
    }

    @Composable
    private fun gameHeader(state: GameState) {
        Column(
            modifier = Modifier.padding(CONTENT_PADDING_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Frame ${state.frameNumber}",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
            )
            Text("${state.gameStatus}  |  Timer: ${state.roundTimer}s")
        }
    }

// ── Stage view ────────────────────────────────────────────────────────────────

    @Suppress("Indentation")
    @Composable
    private fun stageView(state: GameState) {
        Card(
            modifier =
                Modifier
                    .size(GameConstants.STAGE_WIDTH.dp, GameConstants.STAGE_HEIGHT.dp)
                    .padding(vertical = CONTENT_PADDING_DP.dp),
            elevation = CARD_ELEVATION_DP.dp,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(FLOOR_BAR_HEIGHT_DP.dp)
                            .align(Alignment.BottomStart)
                            .background(FLOOR_COLOR),
                )
                state.players.values.forEach { player -> playerSprite(player) }
            }
        }
    }

// ── Player sprite ─────────────────────────────────────────────────────────────

    @Suppress("Indentation")
    @Composable
    private fun playerSprite(player: Player) {
        val spriteW = player.hurtBox.width
        val spriteH = player.hurtBox.height
        val uiX = player.position.x.dp
        val uiY = (GameConstants.STAGE_HEIGHT - player.position.y - spriteH).dp
        val color = if (player.id == 1) P1_COLOR else P2_COLOR

        Card(
            modifier =
                Modifier
                    .size(spriteW.dp, spriteH.dp)
                    .offset(x = uiX, y = uiY),
            backgroundColor = color,
            elevation = CARD_ELEVATION_DP.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "P${player.id}\n${player.health.current}HP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

// ── Debug panel ───────────────────────────────────────────────────────────────

    @Suppress("Indentation")
    @Composable
    private fun debugPanel(state: GameState) {
        Card(modifier = Modifier.fillMaxWidth().padding(CONTENT_PADDING_DP.dp)) {
            Column(modifier = Modifier.padding(DEBUG_PADDING_DP.dp)) {
                Text("Debug", style = MaterialTheme.typography.h6)
                state.players[1]?.let { p ->
                    Text(
                        text =
                            "P1  x=${p.position.x.toInt()}  y=${p.position.y.toInt()}" +
                                "  vx=${"%.2f".format(p.nextMove.speedX)}" +
                                "  vy=${"%.2f".format(p.nextMove.speedY)}",
                    )
                }
            }
        }
    }
}
