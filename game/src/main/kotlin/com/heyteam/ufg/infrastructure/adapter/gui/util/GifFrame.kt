package com.heyteam.ufg.infrastructure.adapter.gui.util

import androidx.compose.ui.graphics.painter.BitmapPainter

data class GifFrame(
    val painter: BitmapPainter,
    val delayMs: Long,
)
