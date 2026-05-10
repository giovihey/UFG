package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen VS splash shown for [START_BUFFER_MS] while both peers synchronise their
 * start epoch. Displayed between the practice loop stopping and the real game loop
 * starting, so it costs nothing — the delay was already there for the handshake.
 */
@Composable
fun vsSplashScreen(
    p1Name: String,
    p2Name: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            playerTag(name = p1Name, color = Color.Blue) // P1 blue
            Text(
                text = "VS",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            playerTag(name = p2Name, color = Color.Red) // P2 red
        }
    }
}

@Composable
private fun playerTag(
    name: String,
    color: Color,
) {
    Text(
        text = name,
        color = color,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
    )
}
