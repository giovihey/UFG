package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.ui.graphics.Color

object UiTheme {
    private const val COLOR_BACKGROUND_ARGB = 0xFF1A1A2EL // matches GameConstants.COLOR_STAGE_BACKGROUND
    private const val COLOR_SURFACE_ARGB = 0xFF16213EL
    private const val COLOR_BUTTON_BG_ARGB = 0xFF2A2A4AL // matches GameConstants.COLOR_FLOOR
    private const val COLOR_BORDER_ARGB = 0xFF444466L // matches GameConstants.COLOR_STAGE_MARGIN
    private const val COLOR_MUTED_ARGB = 0xFF9090A8L
    private const val COLOR_P1_ARGB = 0xFFEF5350L
    private const val COLOR_P2_ARGB = 0xFF42A5F5L

    val background = Color(COLOR_BACKGROUND_ARGB)
    val surface = Color(COLOR_SURFACE_ARGB)
    val buttonBg = Color(COLOR_BUTTON_BG_ARGB)
    val border = Color(COLOR_BORDER_ARGB)
    val muted = Color(COLOR_MUTED_ARGB)
    val text = Color.White
    val p1 = Color(COLOR_P1_ARGB)
    val p2 = Color(COLOR_P2_ARGB)
}
