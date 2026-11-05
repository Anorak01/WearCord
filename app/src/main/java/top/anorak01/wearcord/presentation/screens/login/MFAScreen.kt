package top.anorak01.wearcord.presentation.screens.login

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.components.WearTextField
import top.anorak01.wearcord.presentation.login.send2FA
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun MFAScreen(navController: NavHostController, ticket: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }

    fun submitCode() {
        if (isLoading || code.isBlank()) return
        scope.launch {
            isLoading = true
            failure = null
            when (val result = send2FA(ticket, code)) {
                is ApiResult.Success -> {
                    saveToken(context, result.data)
                    me = null
                    code = ""
                    navController.popBackStack("home", false)
                }
                is ApiResult.Failure -> failure = result
            }
            isLoading = false
        }
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Two-factor login") }
        item {
            WearTextField(
                value = code,
                onValueChange = {
                    if (it.length <= 10) {
                        code = it.filter(Char::isDigit)
                        failure = null
                    }
                },
                label = "Authentication code",
                placeholder = "6-digit code",
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { submitCode() }),
            )
        }
        failure?.let { error ->
            item { RequestError(failure = error, onRetry = ::submitCode) }
        }
        item {
            ActionChip(
                label = if (isLoading) "Checking…" else "Confirm code",
                icon = Icons.Default.Check,
                enabled = !isLoading && code.length == 6,
                onClick = ::submitCode,
            )
        }
    }
}
