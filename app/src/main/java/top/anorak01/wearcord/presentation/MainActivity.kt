/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package top.anorak01.wearcord.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.anorak01.wearcord.presentation.fetchers.fetchMe
import top.anorak01.wearcord.presentation.screens.ChannelScreen
import top.anorak01.wearcord.presentation.screens.DMScreen
import top.anorak01.wearcord.presentation.screens.Greeting
import top.anorak01.wearcord.presentation.screens.GuildScreen
import top.anorak01.wearcord.presentation.screens.MessageScreen
import top.anorak01.wearcord.presentation.screens.login.LoginScreen
import top.anorak01.wearcord.presentation.screens.login.MFAScreen
import top.anorak01.wearcord.presentation.screens.login.ManualTokenScreen
import top.anorak01.wearcord.presentation.screens.login.QRScreen
import top.anorak01.wearcord.presentation.theme.WearCordTheme
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp("Android Hehe")
        }
    }
}

var userToken: String? = ""
var me: Me? = null

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(DelicateCoroutinesApi::class)
@Composable
fun WearApp(greetingName: String) {
    // load stored user token
    val sharedPreferences =
        LocalContext.current.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    userToken = sharedPreferences.getString("user_token", "")


    // do something to verify it works, maybe fetch something, probably @me endpoint
    if (userToken?.isNotEmpty() == true) {
        println("Fetching with token $userToken")
        GlobalScope.launch(Dispatchers.IO) {
            val e = fetchMe()
            withContext(Dispatchers.Main) {
                if (e != null) {
                    me = e
                }
            }
        }
    }


    var isDrawerOpen by remember { mutableStateOf(false) }

    val myName by remember { mutableStateOf(me?.username ?: "") }

    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = "home") {
        composable("home") { Greeting(navController, myName, false) }
        composable("guilds") { GuildScreen(navController) }
        composable("dms") { DMScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("qr") { QRScreen(navController) }
        composable("manualtoken") { ManualTokenScreen(navController) }
        composable("channels/{guildId}") { backStackEntry ->
            ChannelScreen(navController, backStackEntry.arguments?.getString("guildId")!!)
        }
        composable("messages/{channelId}") { backStackEntry ->
            MessageScreen(navController, backStackEntry.arguments?.getString("channelId")!!)
        }
        composable("mfa/{ticket}") { backStackEntry ->
            MFAScreen(navController, backStackEntry.arguments?.getString("ticket")!!)
        }
    }



    WearCordTheme {
        Scaffold {
            // Settings menu button
            Box(
                contentAlignment = Alignment.CenterEnd, modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Button(
                    onClick = {
                        isDrawerOpen = true
                    },
                    content = { Text("M") },
                    modifier = Modifier.fillMaxSize(0.15f)

                )
            }



            AnimatedVisibility(
                visible = isDrawerOpen,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Color.Black)
            )
            {
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),

                    ) {
                    item {
                        Text("DMs", color = Color.White, modifier = Modifier.clickable {
                            navController.navigate("dms")
                            isDrawerOpen = false
                        })
                    }
                    item {
                        Text("Guilds", color = Color.White, modifier = Modifier.clickable {
                            navController.navigate("guilds")
                            isDrawerOpen = false
                        })
                    }
                    item {
                        Text(
                            "Shutdown", color = Color.Green,
                            modifier = Modifier.clickable {
                                exitProcess(0)
                            }
                        )
                    }
                    item {
                        Text(
                            "Close", color = Color.Red,
                            modifier = Modifier.clickable { isDrawerOpen = false })
                    }
                }
            }
        }
    }
}


fun saveToken(context: Context, token: String) {
    userToken = token

    val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("user_token", userToken) }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}