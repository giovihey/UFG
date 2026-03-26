package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.heyteam.ufg.domain.entity.World

// A fighting game screen has layers:
//    - Background (stage floor, sky)
//    - Players (drawn at their positions)
//    - UI overlay (health bars, timer, round info)
@Composable
fun gameScreen(world: World) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: stage background + players (drawn on a Canvas)
        stageCanvas(world)
        // Layer 2: HUD overlay on top
        hud(world)
    }
}
