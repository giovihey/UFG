package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.heyteam.ufg.infrastructure.adapter.gui.component.playButton
import com.heyteam.ufg.infrastructure.adapter.gui.util.loadGifFrames
import kotlinx.coroutines.delay

@Composable
fun titleScreen(onPlay: () -> Unit) {
    val frames = remember { loadGifFrames("ufg_video_clip.gif") }
    var frameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(frames) {
        if (frames.size > 1) {
            while (true) {
                delay(frames[frameIndex].delayMs)
                frameIndex = (frameIndex + 1) % frames.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated GIF background
        Image(
            painter = frames[frameIndex].painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Arcade PLAY button — bottom center
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
        ) {
            playButton(onClick = onPlay)
        }
    }
}
