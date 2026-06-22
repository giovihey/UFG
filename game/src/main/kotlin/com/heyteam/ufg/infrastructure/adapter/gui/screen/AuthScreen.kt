package com.heyteam.ufg.infrastructure.adapter.gui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heyteam.ufg.infrastructure.adapter.gui.UiTheme

private const val AUTH_FORM_WIDTH = 0.4f
private const val AUTH_TITLE_SIZE = 32
private const val AUTH_FORM_SPACING = 24
private const val AUTH_INPUT_SPACING = 12
private const val AUTH_ERROR_SIZE = 12

private data class AuthFormState(
    val username: String,
    val password: String,
    val passwordVisible: Boolean,
    val errorMessage: String? = null,
)

private data class AuthFormCallbacks(
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onPasswordVisibilityToggle: () -> Unit,
)

@Composable
fun authScreen(
    errorMessage: String? = null,
    onLogin: (username: String, password: String) -> Unit = { _, _ -> },
    onRegister: (username: String, password: String) -> Unit = { _, _ -> },
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(UiTheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(AUTH_FORM_WIDTH)
                    .background(UiTheme.surface, RoundedCornerShape(12.dp))
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            authScreenTitle(isRegisterMode)
            Spacer(modifier = Modifier.height(AUTH_FORM_SPACING.dp))
            authScreenForm(
                state =
                    AuthFormState(
                        username = username,
                        password = password,
                        passwordVisible = passwordVisible,
                        errorMessage = errorMessage,
                    ),
                callbacks =
                    AuthFormCallbacks(
                        onUsernameChange = { username = it },
                        onPasswordChange = { password = it },
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    ),
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
        text = "UFG",
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = UiTheme.p1,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (isRegisterMode) "Create Account" else "Login",
        fontSize = AUTH_TITLE_SIZE.sp,
        fontWeight = FontWeight.Bold,
        color = UiTheme.text,
    )
}

@Composable
private fun authScreenForm(
    state: AuthFormState,
    callbacks: AuthFormCallbacks,
) {
    val fieldColors =
        TextFieldDefaults.textFieldColors(
            textColor = UiTheme.text,
            backgroundColor = UiTheme.buttonBg,
            cursorColor = UiTheme.p1,
            focusedIndicatorColor = UiTheme.p1,
            unfocusedIndicatorColor = UiTheme.border,
            focusedLabelColor = UiTheme.p1,
            unfocusedLabelColor = UiTheme.muted,
            trailingIconColor = UiTheme.muted,
        )

    TextField(
        value = state.username,
        onValueChange = callbacks.onUsernameChange,
        label = { Text("Username") },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
    )

    Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))

    TextField(
        value = state.password,
        onValueChange = callbacks.onPasswordChange,
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors,
        visualTransformation =
            if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = callbacks.onPasswordVisibilityToggle) {
                Icon(
                    imageVector =
                        if (state.passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (state.passwordVisible) "Hide password" else "Show password",
                )
            }
        },
    )

    if (state.errorMessage != null) {
        Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))
        Text(
            text = state.errorMessage,
            color = UiTheme.p1,
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
        colors = ButtonDefaults.buttonColors(backgroundColor = UiTheme.p1, contentColor = UiTheme.text),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = if (isRegisterMode) "Register" else "Login",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(AUTH_INPUT_SPACING.dp))

    Button(
        onClick = onToggleMode,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(backgroundColor = UiTheme.buttonBg, contentColor = UiTheme.muted),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = if (isRegisterMode) "Already have an account? Login" else "Don't have an account? Register",
            fontSize = 14.sp,
        )
    }
}
