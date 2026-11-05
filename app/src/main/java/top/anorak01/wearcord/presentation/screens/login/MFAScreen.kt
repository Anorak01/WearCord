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
import top.anorak01.wearcord.presentation.login.send2FA
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken

@Composable
fun MFAScreen(navController: NavHostController, ticket: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("2FA Code") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            enabled = !isLoading,
        )
        failure?.let { RequestError(failure = it, onRetry = ::submitCode) }
        Button(
            onClick = ::submitCode,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && code.isNotBlank(),
        ) {
            Text(if (isLoading) "Checking…" else "Confirm")
        }
    }
}
