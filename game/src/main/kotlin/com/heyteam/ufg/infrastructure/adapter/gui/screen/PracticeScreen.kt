package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.domain.entity.World

/**
 * Practice screen shown while matchmaking is in progress.
 *
 * Reuses the exact same [stageCanvas] + [hud] stack as the real [gameScreen], so the
 * player experiences the full game feel with live input — only P2 stands idle because
 * [LocalNetworkPort] never delivers remote inputs.
 *
 * A "Searching for opponent…" overlay pulses at the top and a controls cheatsheet sits
 * at the bottom so new players learn the keys naturally.
 */
@Composable
fun practiceScreen(world: World) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ── Layer 1 & 2: identical to gameScreen ──────────────────────────
        stageCanvas(world)
        hud(world)

        // ── Layer 3: matchmaking status overlay ───────────────────────────
        searchingOverlay()

        // ── Layer 4: controls hint ────────────────────────────────────────
        controlsHint()
    }
}

// ── Overlays ──────────────────────────────────────────────────────────────────

@Composable
private fun searchingOverlay() {
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
