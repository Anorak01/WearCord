package top.anorak01.wearcord.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.SurfaceCard
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.userToken

@Composable
fun Greeting(navController: NavHostController) {
    val listState = rememberScalingLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount) {
        if (me != null || userToken.isNullOrBlank()) return@LaunchedEffect
        isLoading = true
        failure = null
        when (val result = fetchMe()) {
            is ApiResult.Success -> me = result.data
            is ApiResult.Failure -> failure = result
        }
        isLoading = false
    }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("WearCord") }
        item {
            when {
                isLoading -> LoadingState("Connecting to Discord…")
                me != null -> SurfaceCard {
                    Text("Signed in", style = MaterialTheme.typography.caption1)
                    Text(
                        me?.global_name ?: me?.username.orEmpty(),
                        style = MaterialTheme.typography.title3,
                    )
                }
                userToken.isNullOrBlank() -> SurfaceCard {
                    Text("Welcome", style = MaterialTheme.typography.title3)
                    Text(
                        "Choose a sign-in method to get started.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        failure?.let { error ->
            item(key = "request_error") {
                RequestError(failure = error, onRetry = { retryCount++ })
            }
        }
        if (userToken.isNullOrBlank()) {
            item {
                ActionChip(
                    label = "Sign in to Discord",
                    secondaryLabel = "QR code, password, or token",
                    icon = Icons.Default.Person,
                    onClick = { navController.navigate("login-options") },
                )
            }
        } else {
            item {
                ActionChip(
                    label = "Direct messages",
                    icon = Icons.Default.Email,
                    onClick = { navController.navigate("dms") },
                )
            }
            item {
                ActionChip(
                    label = "Servers",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = { navController.navigate("guilds") },
                )
            }
            item {
                ActionChip(
                    label = "Account & sign-in",
                    secondaryLabel = "Switch account or sign in again",
                    icon = Icons.Default.Person,
                    onClick = { navController.navigate("login-options") },
                )
            }
        }
    }
}
