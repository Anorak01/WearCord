package top.anorak01.wearcord.presentation.screens.login

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun ManualTokenScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }

    fun submitToken() {
        if (isLoading || token.isBlank()) return
        scope.launch {
            isLoading = true
            failure = null
            when (val result = fetchMe(token)) {
                is ApiResult.Success -> {
                    me = result.data
                    saveToken(context, token)
                    token = ""
                    navController.popBackStack("home", false)
                }
                is ApiResult.Failure -> failure = result
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Manual token input")
        TextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            enabled = !isLoading,
        )
        failure?.let { RequestError(failure = it, onRetry = ::submitToken) }
        Button(
            onClick = ::submitToken,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && token.isNotBlank(),
        ) {
            Text(if (isLoading) "Validating…" else "Send")
        }
    }
}
