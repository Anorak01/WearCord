package top.anorak01.wearcord.presentation.screens.login

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.SurfaceCard
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.components.WearTextField
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun ManualTokenScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var token by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }

    fun submitToken() {
        if (isLoading || token.isBlank()) return
        scope.launch {
            isLoading = true
            failure = null
            when (val result = fetchMe(token.trim())) {
                is ApiResult.Success -> {
                    me = result.data
                    saveToken(context, token.trim())
                    token = ""
                    navController.popBackStack("home", false)
                }
                is ApiResult.Failure -> failure = result
            }
            isLoading = false
        }
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Account token") }
        item {
            SurfaceCard {
                Text("Advanced sign-in", style = MaterialTheme.typography.title3)
                Text(
                    "Use your own token only. Never share it with anyone.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
        item {
            WearTextField(
                value = token,
                onValueChange = { token = it; failure = null },
                label = "Discord token",
                placeholder = "Paste token",
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { submitToken() }),
            )
        }
        failure?.let { error ->
            item { RequestError(failure = error, onRetry = ::submitToken) }
        }
        item {
            ActionChip(
                label = if (isLoading) "Validating…" else "Use this token",
                icon = Icons.Default.Lock,
                enabled = !isLoading && token.isNotBlank(),
                onClick = ::submitToken,
            )
        }
    }
}
