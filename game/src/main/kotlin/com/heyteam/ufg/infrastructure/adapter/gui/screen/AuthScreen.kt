package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AuthScreen — login/register UI for the game.
 *
 * @param errorMessage optional error from a failed login/register attempt
 * @param onLogin called when user clicks "Login"
 * @param onRegister called when user clicks "Register"
 */
@Composable
fun authScreen(
    errorMessage: String? = null,
    onLogin: (username: String, password: String) -> Unit = { _, _ -> },
    onRegister: (username: String, password: String) -> Unit = { _, _ -> },
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFF5c6bc0.toInt())),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.4f)
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isRegisterMode) "Create Account" else "Login",
                fontSize = 32.sp,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Username TextField
            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password TextField
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
            )

            // Error message (if any)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login/Register Button
            Button(
                onClick = {
                    if (isRegisterMode) {
                        onRegister(username, password)
                    } else {
                        onLogin(username, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isRegisterMode) "Register" else "Login")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toggle between login and register
            Button(
                onClick = { isRegisterMode = !isRegisterMode },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isRegisterMode) {
                        "Already have an account? Login"
                    } else {
                        "Don't have an account? Register"
                    },
                )
            }
        }
    }
}
