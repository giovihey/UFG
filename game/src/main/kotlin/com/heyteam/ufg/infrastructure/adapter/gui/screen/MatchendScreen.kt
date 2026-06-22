package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val COLOR_P1_WIN_ARGB = 0xFFEF5350L
private const val COLOR_P2_WIN_ARGB = 0xFF42A5F5L

private val p1WinColor = Color(COLOR_P1_WIN_ARGB)
private val p2WinColor = Color(COLOR_P2_WIN_ARGB)

@Composable
fun matchEndScreen(
    winnerId: Int,
    winnerName: String,
    onBackToMenu: () -> Unit,
) {
    val winnerColor = if (winnerId == 1) p1WinColor else p2WinColor

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "$winnerName WINS!",
                color = winnerColor,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBackToMenu,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray),
            ) {
                Text(
                    text = "Back to Menu",
                    color = Color.White,
                    fontSize = 20.sp,
                )
            }
        }
    }
}
