package com.heyteam.ufg.infrastructure.adapter.gui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Button dimensions ────────────────────────────────────────────────────────
private const val BUTTON_WIDTH_DP = 240
private const val BUTTON_HEIGHT_DP = 72
private const val BUTTON_BODY_HEIGHT_DP = 64
private const val BUTTON_3D_OFFSET_DP = 6
private const val BUTTON_PRESS_OFFSET_DP = 4

// ── Elevation ────────────────────────────────────────────────────────────────
private const val ELEVATION_PRESSED_DP = 2
private const val ELEVATION_NORMAL_DP = 8

// ── Shape ────────────────────────────────────────────────────────────────────
private const val SHAPE_CORNER_RADIUS = 50
private const val BORDER_WIDTH_DP = 2

// ── Gloss sheen ──────────────────────────────────────────────────────────────
private const val GLOSS_WIDTH_DP = 200
private const val GLOSS_HEIGHT_DP = 28
private const val GLOSS_OFFSET_Y_DP = -10

// ── Text ─────────────────────────────────────────────────────────────────────
private const val TEXT_FONT_SIZE_SP = 28
private const val TEXT_LETTER_SPACING_SP = 4

// ── Shadow ───────────────────────────────────────────────────────────────────
private const val SHADOW_BLUR_RADIUS_F = 4f
private const val SHADOW_OFFSET_X_F = 2f
private const val SHADOW_OFFSET_Y_F = 3f

// ── Button colors ────────────────────────────────────────────────────────────
// Each hex value is broken into a named constant so Color(0x...) has no magic numbers.
private const val COLOR_BUTTON_TOP_ARGB = 0xFFFF4444L
private const val COLOR_BUTTON_BOTTOM_ARGB = 0xFFCC0000L
private const val COLOR_BUTTON_3D_BASE_ARGB = 0xFF7A0D10L
private const val COLOR_BORDER_TOP_ARGB = 0x99FF9999L
private const val COLOR_BORDER_BOTTOM_ARGB = 0x33880000L
private const val COLOR_SHADOW_ARGB = 0xFF660000L
private const val COLOR_GLOSS_TOP_ARGB = 0x55FFFFFFL
private const val COLOR_GLOSS_BOTTOM_ARGB = 0x00FFFFFFL

private val BUTTON_COLOR_TOP = Color(COLOR_BUTTON_TOP_ARGB)
private val BUTTON_COLOR_BOTTOM = Color(COLOR_BUTTON_BOTTOM_ARGB)
private val BUTTON_COLOR_3D_BASE = Color(COLOR_BUTTON_3D_BASE_ARGB)
private val BUTTON_BORDER_TOP = Color(COLOR_BORDER_TOP_ARGB)
private val BUTTON_BORDER_BOTTOM = Color(COLOR_BORDER_BOTTOM_ARGB)
private val SHADOW_COLOR = Color(COLOR_SHADOW_ARGB)
private val GLOSS_COLOR_TOP = Color(COLOR_GLOSS_TOP_ARGB)
private val GLOSS_COLOR_BOTTOM = Color(COLOR_GLOSS_BOTTOM_ARGB)

/**
 * Arcade-style PLAY button matching the UFG title screen design:
 * red pill shape, 3D depth shadow, gloss sheen, press animation.
 */
@Composable
fun playButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffsetY = if (isPressed) BUTTON_PRESS_OFFSET_DP.dp else 0.dp
    val elevation = if (isPressed) ELEVATION_PRESSED_DP.dp else ELEVATION_NORMAL_DP.dp
    val shape = RoundedCornerShape(SHAPE_CORNER_RADIUS)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(BUTTON_WIDTH_DP.dp)
                .height(BUTTON_HEIGHT_DP.dp),
    ) {
        button3DBase(shape)
        buttonSurface(onClick, pressOffsetY, elevation, shape, interactionSource)
    }
}

@Composable
private fun button3DBase(shape: RoundedCornerShape) {
    Box(
        modifier =
            Modifier
                .width(BUTTON_WIDTH_DP.dp)
                .height(BUTTON_BODY_HEIGHT_DP.dp)
                .offset(y = BUTTON_3D_OFFSET_DP.dp)
                .clip(shape)
                .background(BUTTON_COLOR_3D_BASE),
    )
}

@Composable
private fun buttonSurface(
    onClick: () -> Unit,
    pressOffsetY: Dp,
    elevation: Dp,
    shape: RoundedCornerShape,
    interactionSource: MutableInteractionSource,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .width(BUTTON_WIDTH_DP.dp)
                .height(BUTTON_BODY_HEIGHT_DP.dp)
                .offset(y = pressOffsetY - BUTTON_PRESS_OFFSET_DP.dp)
                .shadow(elevation, shape)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BUTTON_COLOR_TOP, BUTTON_COLOR_BOTTOM),
                    ),
                ).border(
                    width = BORDER_WIDTH_DP.dp,
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(BUTTON_BORDER_TOP, BUTTON_BORDER_BOTTOM),
                        ),
                    shape = shape,
                ).clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        glossSheen()
        playText(pressOffsetY)
    }
}

@Composable
private fun glossSheen() {
    Box(
        modifier =
            Modifier
                .width(GLOSS_WIDTH_DP.dp)
                .height(GLOSS_HEIGHT_DP.dp)
                .offset(y = GLOSS_OFFSET_Y_DP.dp)
                .clip(RoundedCornerShape(SHAPE_CORNER_RADIUS))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GLOSS_COLOR_TOP, GLOSS_COLOR_BOTTOM),
                    ),
                ),
    )
}

@Composable
private fun playText(pressOffsetY: Dp) {
    Text(
        text = "PLAY",
        fontSize = TEXT_FONT_SIZE_SP.sp,
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Italic,
        color = Color.White,
        textAlign = TextAlign.Center,
        letterSpacing = TEXT_LETTER_SPACING_SP.sp,
        modifier = Modifier.offset(y = pressOffsetY),
        style =
            TextStyle(
                shadow =
                    Shadow(
                        color = SHADOW_COLOR,
                        offset = Offset(SHADOW_OFFSET_X_F, SHADOW_OFFSET_Y_F),
                        blurRadius = SHADOW_BLUR_RADIUS_F,
                    ),
            ),
    )
}
