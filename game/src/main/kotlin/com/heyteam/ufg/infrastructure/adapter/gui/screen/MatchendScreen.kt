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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.infrastructure.adapter.gui.UiTheme

@Composable
fun matchEndScreen(
    winnerId: Int,
    winnerName: String,
    onBackToMenu: () -> Unit,
) {
    val winnerColor = if (winnerId == 1) UiTheme.p1 else UiTheme.p2

    Box(
        modifier = Modifier.fillMaxSize().background(UiTheme.background),
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
                colors = ButtonDefaults.buttonColors(backgroundColor = UiTheme.buttonBg),
            ) {
                Text(
                    text = "Back to Menu",
                    color = UiTheme.text,
                    fontSize = 20.sp,
                )
            }
        }
    }
}
