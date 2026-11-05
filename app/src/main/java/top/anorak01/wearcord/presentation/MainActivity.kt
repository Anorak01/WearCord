package top.anorak01.wearcord.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import top.anorak01.wearcord.presentation.screens.ChannelScreen
import top.anorak01.wearcord.presentation.screens.ComposeMessageScreen
import top.anorak01.wearcord.presentation.screens.DMScreen
import top.anorak01.wearcord.presentation.screens.Greeting
import top.anorak01.wearcord.presentation.screens.GuildScreen
import top.anorak01.wearcord.presentation.screens.MessageScreen
import top.anorak01.wearcord.presentation.screens.login.LoginOptionsScreen
import top.anorak01.wearcord.presentation.screens.login.LoginScreen
import top.anorak01.wearcord.presentation.screens.login.MFAScreen
import top.anorak01.wearcord.presentation.screens.login.ManualTokenScreen
import top.anorak01.wearcord.presentation.screens.login.QRScreen
import top.anorak01.wearcord.presentation.theme.WearCordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        userToken = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
            .getString("user_token", "")

        setContent { WearApp() }
    }
}

var userToken: String? = ""
var me: Me? = null

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()
    WearCordTheme {
        SwipeDismissableNavHost(navController = navController, startDestination = "home") {
            composable("home") { Greeting(navController) }
            composable("guilds") { GuildScreen(navController) }
            composable("dms") { DMScreen(navController) }
            composable("login-options") { LoginOptionsScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("qr") { QRScreen(navController) }
            composable("manualtoken") { ManualTokenScreen(navController) }
            composable("channels/{guildId}") { entry ->
                entry.arguments?.getString("guildId")?.let { guildId ->
                    ChannelScreen(navController, guildId)
                }
            }
            composable("messages/{channelId}") { entry ->
                entry.arguments?.getString("channelId")?.let { channelId ->
                    MessageScreen(navController, channelId)
                }
            }
            composable("compose/{channelId}") { entry ->
                entry.arguments?.getString("channelId")?.let { channelId ->
                    ComposeMessageScreen(navController, channelId)
                }
            }
            composable("mfa/{ticket}") { entry ->
                entry.arguments?.getString("ticket")?.let { ticket ->
                    MFAScreen(navController, ticket)
                }
            }
        }
    }
}

fun saveToken(context: Context, token: String) {
    userToken = token
    context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        .edit { putString("user_token", token) }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
