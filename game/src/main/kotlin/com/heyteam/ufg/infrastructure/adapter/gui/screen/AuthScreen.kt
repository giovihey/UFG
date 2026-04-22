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

private val AUTH_BACKGROUND_COLOR = Color.Cyan
private val AUTH_TEXT_COLOR = Color.White
private val AUTH_ERROR_COLOR = Color.Red
private const val AUTH_FORM_WIDTH = 0.4f
private const val AUTH_TITLE_SIZE = 32
private const val AUTH_FORM_SPACING = 24
private const val AUTH_INPUT_SPACING = 12
private const val AUTH_ERROR_SIZE = 12

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
                .background(AUTH_BACKGROUND_COLOR),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(AUTH_FORM_WIDTH)
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            authScreenTitle(isRegisterMode)
            Spacer(modifier = Modifier.height(AUTH_FORM_SPACING.dp))
            authScreenForm(
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                errorMessage = errorMessage,
            )
            Spacer(modifier = Modifier.height(AUTH_FORM_SPACING.dp))
            authScreenButtons(
                isRegisterMode = isRegisterMode,
                onToggleMode = { isRegisterMode = !isRegisterMode },
                onLogin = { onLogin(username, password) },
                onRegister = { onRegister(username, password) },
            )
        }
    }
}

@Composable
private fun authScreenTitle(isRegisterMode: Boolean) {
    Text(
        text = if (isRegisterMode) "Create Account" else "Login",
        fontSize = AUTH_TITLE_SIZE.sp,
        color = AUTH_TEXT_COLOR,
    )
}

@Composable
private fun authScreenForm(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    errorMessage: String? = null,
) {
    TextField(
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))

    TextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
    )

    if (errorMessage != null) {
        Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))
        Text(
            text = errorMessage,
            color = AUTH_ERROR_COLOR,
            fontSize = AUTH_ERROR_SIZE.sp,
        )
    }
}

@Composable
private fun authScreenButtons(
    isRegisterMode: Boolean,
    onToggleMode: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    Button(
        onClick = { if (isRegisterMode) onRegister() else onLogin() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (isRegisterMode) "Register" else "Login")
    }

    Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))

    Button(
        onClick = onToggleMode,
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
