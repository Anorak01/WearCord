package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.userToken

@Composable
fun Greeting(navController: NavHostController) {
    var displayName by remember { mutableStateOf(me?.global_name ?: me?.username.orEmpty()) }
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount) {
        val currentUser = me
        if (currentUser != null) {
            displayName = currentUser.global_name ?: currentUser.username
            return@LaunchedEffect
        }
        if (userToken.isNullOrBlank()) {
            navController.navigate("qr") { launchSingleTop = true }
            return@LaunchedEffect
        }

        isLoading = true
        failure = null
        when (val result = fetchMe()) {
            is ApiResult.Success -> {
                me = result.data
                displayName = result.data.global_name ?: result.data.username
            }
            is ApiResult.Failure -> {
                if (result.reason == ApiResult.Reason.UNAUTHORIZED) {
                    navController.navigate("qr") { launchSingleTop = true }
                } else {
                    // Connectivity/server failures are retryable and must not log the user out.
                    failure = result
                }
            }
        }
        isLoading = false
    }

    SwipeToDismissBox(onDismissed = {}, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = "Welcome to WearCord",
            )
            if (displayName.isNotBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = displayName,
                )
            }
            if (isLoading) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Connecting to Discord…",
                )
            }
            failure?.let {
                RequestError(failure = it, onRetry = { retryCount++ })
            }
        }
    }
}
