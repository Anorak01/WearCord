package top.anorak01.wearcord.presentation.screens.login

import QRAuth.discordqr.DiscordQrAuthClient
import QRAuth.discordqr.exc.TimedOutException
import QRAuth.discordqr.exc.UserCancelledException
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.SurfaceCard
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.login.generateQrCodeBitmap
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken
import java.util.concurrent.CompletableFuture
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutionException
import java.util.function.Consumer
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun QRScreen(navController: NavHostController) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var state by remember { mutableStateOf(QRState.Start) }
    var qrUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var retryCount by remember { mutableIntStateOf(0) }
    var authClient by remember { mutableStateOf<DiscordQrAuthClient?>(null) }

    LaunchedEffect(retryCount) {
        state = QRState.Start
        errorMessage = ""
        authClient?.close()
        try {
            val client = withContext(Dispatchers.IO) {
                DiscordQrAuthClient(Consumer { error ->
                    Log.w(QR_LOG_TAG, "QR handshake error", error)
                })
            }
            authClient = client
            client.codeFuture.thenAccept { fingerprint ->
                if (!fingerprint.isNullOrBlank()) {
                    launch {
                        qrUrl = fingerprint
                        state = QRState.QRScan
                    }
                }
            }
            withContext(Dispatchers.IO) { client.start() }
            val token = client.tokenFuture.await()
            if (token.isBlank()) throw IllegalStateException("Discord returned no token")

            saveToken(context, token)
            me = null
            navController.popBackStack("home", false)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(QR_LOG_TAG, "QR login failed", error)
            errorMessage = qrFailureMessage(error)
            state = QRState.Error
        }
    }

    DisposableEffect(Unit) { onDispose { authClient?.close() } }

    WearListScaffold(listState = listState) {
        item { ScreenHeader("QR login") }
        when (state) {
            QRState.Start -> {
                item { LoadingState("Preparing a secure QR code…") }
            }
            QRState.QRScan -> {
                item {
                    SurfaceCard {
                        Text("Continue on your phone", style = MaterialTheme.typography.title3)
                        Text(
                            "Open Discord’s QR scanner, scan this code, then approve login on your phone.",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                item {
                    val bitmap = remember(qrUrl) { generateQrCodeBitmap(qrUrl, 384, 384) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Discord login QR code",
                            modifier = Modifier
                                .size(172.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(10.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text("Couldn’t generate the QR code", color = MaterialTheme.colors.error)
                    }
                }
            }
            QRState.Error -> {
                item {
                    RequestError(
                        failure = ApiResult.Failure(ApiResult.Reason.NETWORK, errorMessage),
                        onRetry = { retryCount++ },
                    )
                }
            }
        }
        item {
            ActionChip(
                label = "Use a token instead",
                icon = Icons.Default.Lock,
                onClick = { navController.navigate("manualtoken") },
            )
        }
    }
}

private fun qrFailureMessage(error: Exception): String {
    val cause = generateSequence<Throwable>(error) { current ->
        when (current) {
            is ExecutionException, is java.util.concurrent.CompletionException -> current.cause
            else -> null
        }
    }.last()
    return when (cause) {
        is UserCancelledException -> "QR login was declined on your phone. Generate a new code to try again."
        is TimedOutException -> "This QR code expired. Generate a new code and scan it promptly."
        is SocketTimeoutException -> "Discord took too long to respond. Retry, keeping the watch awake and connected."
        is SSLException -> "Discord’s secure connection failed. Retry, or check the watch’s date and time."
        is IOException -> "Discord connection failed (${cause.javaClass.simpleName}). Check Wi-Fi and retry."
        else -> "QR login failed (${cause.javaClass.simpleName}). Retry or sign in another way."
    }
}

private const val QR_LOG_TAG = "WearCordQrAuth"

private suspend fun <T> CompletableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        whenComplete { value, throwable ->
            if (throwable == null) {
                continuation.resume(value)
            } else {
                val cause = (throwable as? ExecutionException)?.cause ?: throwable
                continuation.resumeWithException(cause)
            }
        }
        continuation.invokeOnCancellation { cancel(true) }
    }

private enum class QRState {
    Start,
    QRScan,
    Error,
}
