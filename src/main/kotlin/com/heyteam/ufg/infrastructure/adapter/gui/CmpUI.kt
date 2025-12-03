package com.heyteam.ufg.infrastructure.adapter.gui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun startUI() =
    application {
        println("Fight in UFG")
        Window(onCloseRequest = ::exitApplication) {
            app() // We extract the UI into a separate function
        }
    }

// this is made purely with AI, it's a good boilerplate
@Composable
fun app() {
    // 1. STATE
    // "remember" means: Keep this variable alive even if the UI redraws.
    // "mutableStateOf" means: Watch this variable. If it changes, redraw the UI.
    var packetCount by remember { mutableStateOf(0) }

    // 2. LAYOUT
    // "Column" is like a vertical stack (Flexbox column)
    Column(modifier = Modifier.padding(16.dp)) {
        // 3. UI ELEMENTS
        Text(text = "Packets Processed: $packetCount")

        Button(
            onClick = {
                // 4. EVENT
                packetCount++ // We change state, UI updates automatically
            },
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text("Simulate Incoming Packet")
        }
    }
}
