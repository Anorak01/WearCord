package top.anorak01.wearcord.presentation.screens.login

import android.net.Uri
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.components.WearTextField
import top.anorak01.wearcord.presentation.login.sendLogin
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun LoginScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberScalingLazyListState()
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

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Discord login") }
        item {
            WearTextField(
                value = email,
                onValueChange = { email = it; failure = null },
                label = "Email",
                placeholder = "name@example.com",
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
        }
        item {
            WearTextField(
                value = password,
                onValueChange = { password = it; failure = null },
                label = "Password",
                placeholder = "Your Discord password",
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { submitLogin() }),
            )
        }
        failure?.let { error ->
            item { RequestError(failure = error, onRetry = ::submitLogin) }
        }
        item {
            ActionChip(
                label = if (isLoading) "Signing in…" else "Sign in",
                secondaryLabel = "Two-factor authentication is supported",
                icon = Icons.Default.Person,
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                onClick = ::submitLogin,
            )
        }
    }
}
