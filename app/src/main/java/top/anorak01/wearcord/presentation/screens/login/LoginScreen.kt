package top.anorak01.wearcord.presentation.screens.login

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.login.sendLogin
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }

    fun submitLogin() {
        if (isLoading || email.isBlank() || password.isBlank()) return
        scope.launch {
            isLoading = true
            failure = null
            when (val result = sendLogin(email, password)) {
                is ApiResult.Success -> {
                    val login = result.data
                    if (login.mfa) {
                        navController.navigate("mfa/${Uri.encode(login.ticket)}")
                    } else if (login.ticket.isNotBlank()) {
                        saveToken(context, login.ticket)
                        me = null
                        email = ""
                        password = ""
                        navController.popBackStack("home", false)
                    } else {
                        failure = ApiResult.Failure(
                            ApiResult.Reason.INVALID_RESPONSE,
                            "Discord did not return a login token.",
                        )
                    }
                }
                is ApiResult.Failure -> failure = result
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { androidx.wear.compose.material.Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            enabled = !isLoading,
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { androidx.wear.compose.material.Text("Password") },
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.background),
            visualTransformation = PasswordVisualTransformation(),
            maxLines = 1,
            enabled = !isLoading,
        )
        failure?.let { RequestError(failure = it, onRetry = ::submitLogin) }
        Button(
            onClick = ::submitLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
        ) {
            androidx.wear.compose.material.Text(if (isLoading) "Logging in…" else "Log In")
        }
    }
}
