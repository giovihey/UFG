package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

private const val MENU_WIDTH_FRACTION = 0.3f
private const val BUTTON_CORNER_RADIUS_DP = 8
private const val MENU_SPACING_DP = 24
private const val BUTTON_SPACING_DP = 12

@Composable
fun menuScreen(
    onPlay: () -> Unit,
    onOptions: () -> Unit,
    onHelp: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(UiTheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING_DP.dp),
            modifier = Modifier.fillMaxWidth(MENU_WIDTH_FRACTION),
        ) {
            Text(
                text = "UFG",
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UiTheme.p1,
            )
            Text(
                text = "Ultimate Fighting Game",
                fontSize = 14.sp,
                color = UiTheme.muted,
                letterSpacing = 3.sp,
            )

            Spacer(modifier = Modifier.height(MENU_SPACING_DP.dp))

            menuButton(label = "Play", onClick = onPlay, primary = true)
            menuButton(label = "Options", onClick = onOptions)
            menuButton(label = "How to Play", onClick = onHelp)
        }
    }
}

@Composable
private fun menuButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                backgroundColor = if (primary) UiTheme.p1 else UiTheme.buttonBg,
                contentColor = UiTheme.text,
            ),
        shape = RoundedCornerShape(BUTTON_CORNER_RADIUS_DP.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}
