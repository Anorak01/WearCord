package top.anorak01.wearcord.presentation.screens.login

import QRAuth.discordqr.DiscordQrAuthClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.login.generateQrCodeBitmap
import top.anorak01.wearcord.presentation.me
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.saveToken
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Consumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun QRScreen(navController: NavHostController) {
    val context = LocalContext.current
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
                DiscordQrAuthClient(Consumer { _: Throwable? -> })
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
            errorMessage = "Could not start QR login. Check your connection and retry."
            state = QRState.Error
        }
    }

    DisposableEffect(Unit) {
        onDispose { authClient?.close() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            QRState.Start -> {
                Text("Waiting for QR code", textAlign = TextAlign.Center)
                Button(onClick = { navController.navigate("manualtoken") }) {
                    Text("Use token")
                }
            }
            QRState.QRScan -> {
                Text("Please scan QR code", textAlign = TextAlign.Center)
                val bitmap = remember(qrUrl) { generateQrCodeBitmap(qrUrl) }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Discord login QR code",
                        modifier = Modifier.fillMaxSize(0.5f)
                            .clip(RoundedCornerShape(CornerSize(6.dp))),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text("Could not generate QR code")
                }
            }
            QRState.Error -> {
                RequestError(
                    failure = ApiResult.Failure(ApiResult.Reason.NETWORK, errorMessage),
                    onRetry = { retryCount++ },
                )
                Button(
                    onClick = { navController.navigate("manualtoken") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use token instead")
                }
            }
        }
    }
}

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
