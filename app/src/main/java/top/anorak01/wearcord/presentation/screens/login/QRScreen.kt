package top.anorak01.wearcord.presentation.screens.login

import QRAuth.discordqr.DiscordQrAuthClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.login.generateQrCodeBitmap
import top.anorak01.wearcord.presentation.saveToken
import top.anorak01.wearcord.presentation.userToken
import java.util.function.Consumer

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun QRScreen(navController: NavHostController) {
    // QR code login
    val context = LocalContext.current

    // prepare a websocket
    var state by remember { mutableStateOf(QRState.Start) }
    var qrUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // set up websocket
        val discordAuth =
            DiscordQrAuthClient(Consumer { obj: Throwable? -> obj!!.printStackTrace() }) // create the client, logging any errors
        discordAuth.getCodeFuture()
            .thenAccept(Consumer { s: String? ->
                println("Got QR code link: https://discordapp.com/ra/" + s)
                if (s != null) {
                    qrUrl = s
                    state = QRState.QRScan
                }
            }) // print the full url to stdout when we get it
        discordAuth.getCodeScannedFuture()
            .thenAccept(Consumer { discordUser: DiscordQrAuthClient.DiscordUser? ->
                System.out.printf(
                    "User %s scanned qr code, waiting for confirmation%n",
                    discordUser
                )
            }) // print the user who scanned the qr code
        GlobalScope.launch {
            discordAuth.start() // start the client
            val s1 = discordAuth.getTokenFuture().get() // wait for the token to arrive
            if (s1.isEmpty()) {
                println("No token")
                withContext(Dispatchers.Main) {
                    navController.navigate("manualtoken")
                }
                return@launch
            }
            println("OK: " + s1) // print the token
            // set the token, save it and navigate away
            userToken = s1
            saveToken(context, s1)
            navController.clearBackStack("home")
        }
    }

    when (state) {
        QRState.Start -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("Waiting for QR code")
            }
        }

        QRState.QRScan -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column {
                    Text("Please scan QR code")
                    // QR code rendered here
                    // using the previous image rendering method
                    Image(
                        bitmap = generateQrCodeBitmap(qrUrl)!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .fillMaxSize(0.5f)
                            .clip(RoundedCornerShape(CornerSize(6.dp))),
                        contentScale = ContentScale.Fit // Or ContentScale.Crop
                    )
                }
            }
        }

        QRState.QRLogin -> {
            // e
        }

    }
}

enum class QRState {
    Start,
    QRScan,
    QRLogin
}