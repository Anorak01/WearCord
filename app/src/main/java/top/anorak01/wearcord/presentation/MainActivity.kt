/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package top.anorak01.wearcord.presentation

import QRAuth.discordqr.DiscordQrAuthClient
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import coil.compose.rememberAsyncImagePainter
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import top.anorak01.wearcord.R
import top.anorak01.wearcord.presentation.theme.WearCordTheme
import java.util.function.Consumer
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

suspend fun fetchMessages(
    channelId: String = "1377655291490996335",
    messagesBeforeId: String = "",
    messageLimit: Int = 50
): List<Message> = withContext(Dispatchers.IO) {
    if (messageLimit <= 0 || messageLimit > 100) return@withContext emptyList()
    val client = OkHttpClient()
    var url = "https://discord.com/api/v9/channels/$channelId/messages?limit=50"
    if (messagesBeforeId.isNotEmpty()) {
        url += "&before=$messagesBeforeId"
    }
    val request = Request.Builder()
        .url(url)
        .headers(
            Headers.headersOf(
                "Authorization",
                "$userToken"
            )
        )
        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return@withContext emptyList()
        }

        println("Request succeeded")

        val body = response.body.string()
        println(body)
        if (body.isBlank()) return@withContext emptyList()

        val json = Json { ignoreUnknownKeys = true }

        val msgs = json.decodeFromString<List<Message>>(body)

        return@withContext msgs
    }
}

suspend fun fetchGuilds(): List<Guild> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    var url = "https://discord.com/api/v9/users/@me/guilds"
    val request = Request.Builder()
        .url(url)
        .headers(
            Headers.headersOf(
                "Authorization",
                "$userToken"
            )
        )
        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return@withContext emptyList()
        }

        println("Request succeeded")

        val body = response.body.string()
        println(body)
        if (body.isBlank()) return@withContext emptyList()

        val json = Json { ignoreUnknownKeys = true }

        val guilds = json.decodeFromString<List<Guild>>(body)

        return@withContext guilds
    }
}

suspend fun fetchGuildChannels(guildId: String): Map<String, MutableList<Channel>> =
    withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val url = "https://discord.com/api/v9/guilds/$guildId/channels"
        val request = Request.Builder()
            .url(url)
            .headers(
                Headers.headersOf(
                    "Authorization",
                    "$userToken"
                )
            )
            .build()

        println("request built")
        client.newCall(request).execute().use { response ->
            if (response.code != 200) {
                println("Request failed with code ${response.code}")
                println(response.body.string())
                println(response.headers)
                return@withContext emptyMap()
            }

            println("Request succeeded")

            val body = response.body.string()
            println(body)
            if (body.isBlank()) return@withContext emptyMap()

            val json = Json { ignoreUnknownKeys = true }

            var channels = json.decodeFromString<List<Channel>>(body)
            // parse the guilds, now preprocess them into expected nested list based on parent

            // sort them by position
            channels = channels.sortedBy { it.position }

            // make them a nested list/tree where parent is channel without parent_id and children are channels with the same parent_id
            val channelsMap = mutableMapOf<String, MutableList<Channel>>()
            for (channel in channels) {
                if (channel.parent_id == null) {
                    channelsMap[channel.id] = mutableListOf()
                } else {
                    channelsMap[channel.parent_id]?.add(channel)
                }
            }

            return@withContext channelsMap
        }
    }

/**
 * Returns *sorted* list of dms
 */
suspend fun fetchDms(): List<Channel> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    var url = "https://discord.com/api/v9/users/@me/channels"
    val request = Request.Builder()
        .url(url)
        .headers(
            Headers.headersOf(
                "Authorization",
                "$userToken"
            )
        )
        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return@withContext emptyList()
        }

        println("Request succeeded")

        val body = response.body.string()
        println(body)
        if (body.isBlank()) return@withContext emptyList()

        val json = Json { ignoreUnknownKeys = true }

        var channels = json.decodeFromString<List<Channel>>(body)

        channels.forEach { channel ->
            if (channel.recipients.isNotEmpty()) channel.name =
                channel.recipients[0].global_name ?: channel.recipients[0].username
        }

        channels = channels.sortedBy { toTimestamp(it.last_message_id ?: it.id) }.reversed()

        return@withContext channels
    }
}

fun fetchMe(token: String? = userToken): Me? {
    if (token == null) return null
    println("fetching me")
    val client = OkHttpClient()
    var url = "https://discord.com/api/v9/users/@me"
    val request = Request.Builder()
        .url(url)
        .headers(
            Headers.headersOf(
                "Authorization",
                token
            )
        )
        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return null
        }

        println("Request succeeded")

        val body = response.body.string()
        if (body.isBlank()) return null

        val json = Json { ignoreUnknownKeys = true }

        val me = json.decodeFromString<Me>(body)

        return me
    }
}

fun toTimestamp(id: String): Long {
    return id.toLong() shr 22
}

fun getIconUrl(guildId: String, iconId: String): String {
    return "https://cdn.discordapp.com/icons/$guildId/$iconId.png?size=128"
}

fun getUserUrl(userId: String, iconId: String): String {
    return "https://cdn.discordapp.com/avatars/$userId/$iconId.png?size=128"
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

@Composable
fun ChannelScreen(navController: NavHostController, guildId: String) {
    val scope = rememberCoroutineScope()

    val channels = remember { mutableStateMapOf<String, List<Channel>>() }

    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        val result = fetchGuildChannels(guildId)
        channels.clear()
        channels.putAll(result)
        println("fetched messages")
        listState.animateScrollToItem(0, 1)
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = false, // To show newest message at bottom
            scalingParams = ScalingLazyColumnDefaults.scalingParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minTransitionArea = 0.4f
            ),
            contentPadding = PaddingValues(vertical = 70.dp)
        ) {
            val parentChannels = channels.keys.toList()
            items(parentChannels) { parentChannel ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray)
                        .padding(horizontal = 4.dp),
                    //.padding(vertical = 0.dp),  // Only vertical spacing, no horizontal padding
                    contentAlignment = Alignment.Center,

                    ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally

                    ) {
                        val subChannels = channels[parentChannel]
                        subChannels?.forEach { channel ->
                            Text(
                                text = channel.name ?: "",
                                color = Color.White,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(2.dp)
                                    .background(Color.DarkGray)
                                    .clickable {
                                        navController.navigate("messages/${channel.id}")
                                    },
                                textAlign = TextAlign.Center,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )
                        }
                    }
                }
            }
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                println("clicked")
                scope.launch {
                    val result = fetchGuildChannels(guildId)
                    channels.clear()
                    channels.putAll(result)
                    println("fetched channels")
                    listState.animateScrollToItem(0, 1)
                }

            },
            content = { Text("R") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0, 1)
                }

            },
            content = { Text("D") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }
}

val guilds = mutableStateListOf<Guild>()

@Composable
fun GuildScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()

    //val guilds = remember { mutableStateListOf<Guild>() }

    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        val result = fetchGuilds()
        guilds.clear()
        guilds.addAll(result)
        println("fetched guilds")
        //listState.animateScrollToItem(0, 1)
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            reverseLayout = false, // To show newest message at bottom
            scalingParams = ScalingLazyColumnDefaults.scalingParams(
                edgeScale = 0.5f,
                edgeAlpha = 0.5f,
                minTransitionArea = 0.4f
            ),
            contentPadding = PaddingValues(vertical = 70.dp)
        ) {
            items(guilds) { guild ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray)
                        .padding(horizontal = 4.dp)
                        .clickable {
                            navController.navigate("channels/${guild.id}")
                        },
                    //.padding(vertical = 0.dp),  // Only vertical spacing, no horizontal padding
                    contentAlignment = Alignment.Center,

                    ) {
                    Column {
                        Text(
                            text = guild.name,
                            color = Color.Black,
                            modifier = Modifier
                                //.align(Alignment.CenterStart)
                                .padding(start = 0.dp), // Explicitly no padding
                            textAlign = TextAlign.Center,
                            fontSize = TextUnit(14.0f, TextUnitType.Sp),
                        )
                        if (guild.icon != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = getIconUrl(guild.id, guild.icon),
                                    placeholder = null
                                    // Optional: Add a placeholder and error drawable
                                    // placeholder = painterResource(id = R.drawable.placeholder),
                                    // error = painterResource(id = R.drawable.error_image)
                                ),
                                contentDescription = "guild image",
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .heightIn(80.dp) // Max height for image
                                    .clip(RoundedCornerShape(CornerSize(6.dp))),
                                contentScale = ContentScale.Fit // Or ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                println("clicked")
                scope.launch {
                    val result = fetchGuilds()
                    guilds.clear()
                    guilds.addAll(result)
                    println("fetched guilds")
                }

            },
            content = { Text("R") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0, 1)
                }

            },
            content = { Text("D") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }
}

val dms = mutableStateListOf<Channel>()

@Composable
fun DMScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()

    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        val result = fetchDms()
        dms.clear()
        dms.addAll(result)
        println("fetched dms")
        listState.animateScrollToItem(0, 1)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = false,
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minTransitionArea = 0.4f
                ),
                contentPadding = PaddingValues(vertical = 40.dp)
            ) {
                items(dms) { channel ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .padding(horizontal = 4.dp)
                            .clickable {
                                navController.navigate("messages/${channel.id}")
                            },
                        //.padding(vertical = 0.dp),  // Only vertical spacing, no horizontal padding
                        contentAlignment = Alignment.Center,

                        ) {
                        Column {
                            Text(
                                text = channel.name ?: "Idk something fucked up",
                                color = Color.Black,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(start = 0.dp) // Explicitly no padding
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )
                            if (channel.recipients.isNotEmpty() && channel.recipients[0].avatar?.isNotEmpty() == true) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = getUserUrl(
                                            channel.recipients[0].id,
                                            channel.recipients[0].avatar ?: ""
                                        ),
                                        placeholder = null
                                        // Optional: Add a placeholder and error drawable
                                        // placeholder = painterResource(id = R.drawable.placeholder),
                                        // error = painterResource(id = R.drawable.error_image)
                                    ),
                                    contentDescription = "user image",
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .heightIn(80.dp) // Max height for image
                                        .clip(RoundedCornerShape(CornerSize(6.dp))),
                                    contentScale = ContentScale.Fit // Or ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                println("clicked")
                scope.launch {
                    val result = fetchDms()
                    dms.clear()
                    dms.addAll(result)
                    println("fetched guilds")
                }

            },
            content = { Text("R") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Button(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0, 1)
                }

            },
            content = { Text("D") },
            modifier = Modifier.fillMaxSize(0.15f)

        )
    }
}

@Composable
fun MessageScreen(navController: NavHostController, channelId: String) {
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<Message>() }

    val listState = rememberScalingLazyListState()

    val isAtTop by remember {
        derivedStateOf { listState.centerItemIndex >= messages.size - 1 }
    }

    var newMessageText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val result = fetchMessages(channelId = channelId)
        messages.clear()
        messages.addAll(result)
        println("fetched messages")
        listState.animateScrollToItem(0, 1)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            LaunchedEffect(isAtTop) {
                if (isAtTop && messages.isNotEmpty()) {
                    println("reached the top")
                    val result = fetchMessages(
                        channelId = channelId,
                        messagesBeforeId = messages[messages.size - 1].id
                    )
                    messages.addAll(result)
                    println("fetched more messages")
                    // can load more messages here
                }
            }

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true, // To show newest message at bottom
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minTransitionArea = 0.4f
                ),
                contentPadding = PaddingValues(vertical = 70.dp)
            ) {
                items(messages) { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        println("long pressed")
                                    }
                                )
                            }
                            .padding(horizontal = 4.dp),
                        //.padding(vertical = 0.dp),  // Only vertical spacing, no horizontal padding
                        contentAlignment = Alignment.CenterStart,


                        ) {
                        Column {
                            Text(
                                text = message.author.username,
                                color = Color.Black,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(start = 0.dp), // Explicitly no padding
                                textAlign = TextAlign.Start,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )
                            /*Text(
                                text = message.content,
                                color = Color.White,
                                modifier = Modifier
                                    //.align(Alignment.CenterStart)
                                    .padding(start = 0.dp), // Explicitly no padding
                                textAlign = TextAlign.Start,
                                fontSize = TextUnit(14.0f, TextUnitType.Sp),
                            )*/
                            RichText(message.content)
                            if (message.attachments.isNotEmpty()) {
                                message.attachments.forEach { attachment ->
                                    // For now, let's assume all attachments are images
                                    // You might want to check attachment.contentType
                                    if (attachment.content_type.startsWith("image/")) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = attachment.url,
                                                // Optional: Add a placeholder and error drawable
                                                // placeholder = painterResource(id = R.drawable.placeholder),
                                                // error = painterResource(id = R.drawable.error_image)
                                            ),
                                            contentDescription = "Attachment: ${attachment.filename}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 150.dp) // Max height for image
                                                .clip(RoundedCornerShape(CornerSize(6.dp))),
                                            contentScale = ContentScale.Fit // Or ContentScale.Crop
                                        )
                                        Text( // Optional: display filename
                                            text = attachment.filename,
                                            fontSize = TextUnit(10f, TextUnitType.Sp),
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else {
                                        // Handle other attachment types (e.g., show filename or icon)
                                        Text(
                                            text = "Attachment: ${attachment.filename} (${attachment.content_type ?: "unknown type"})",
                                            color = Color.Cyan,
                                            fontSize = TextUnit(10f, TextUnitType.Sp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (message.type == 19) {
                                Text(
                                    text = "Reply: Jump to message",
                                    color = Color.Black,
                                    modifier = Modifier
                                        //.align(Alignment.CenterStart)
                                        .clickable {
                                            scope.launch {
                                                if (message.message_reference != null) {
                                                    jumpToMessage(
                                                        channelId,
                                                        message.message_reference.message_id,
                                                        listState,
                                                        messages
                                                    )
                                                }
                                            }
                                        }
                                        .padding(start = 0.dp), // Explicitly no padding
                                    textAlign = TextAlign.Start,
                                    fontSize = TextUnit(8.0f, TextUnitType.Sp),
                                )
                                // add actual reply handling
                                // 1. check if message is already in message list, if yes jump to it and return
                                // 2. check if message really exists /channels/<id>/messages/<id>, if no, return with error
                                // 3. fetch next 100 messages, check if id in them
                                // 4. add to message list
                                // 5. if message in newly fetched, jump to it and return, else jump to 3.
                            } else if (message.type != 0 || message.flags != 0) {
                                Text(
                                    text = "Part of this message is not supported",
                                    color = Color.Red,
                                    modifier = Modifier
                                        //.align(Alignment.CenterStart)
                                        .padding(start = 0.dp), // Explicitly no padding
                                    textAlign = TextAlign.Start,
                                    fontSize = TextUnit(8.0f, TextUnitType.Sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize()) {
        Row {
            var reloadEnabled by remember { mutableStateOf(true) }
            Button(
                onClick = {
                    println("clicked")
                    scope.launch {
                        reloadEnabled = false
                        val result = fetchMessages(channelId = channelId)
                        messages.clear()
                        messages.addAll(result)
                        println("fetched messages")
                        reloadEnabled = true
                    }

                },
                content = { Text("R") },
                modifier = Modifier.fillMaxSize(0.15f),
                enabled = reloadEnabled
            )


            Button(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0, 1)
                    }

                },
                content = { Text("D") },
                modifier = Modifier.fillMaxSize(0.15f)

            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
// Input field and Send Button - Bottom
        var buttonEnabled by remember { mutableStateOf(true) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background) // Add a background to overlay content
                .padding(horizontal = 16.dp, vertical = 3.dp)
        ) {
            TextField( // Using OutlinedTextField for better visibility on Wear
                value = newMessageText,
                onValueChange = { newMessageText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(0.75f),
                /*colors = TextFieldDefaults.colors( // Customize colors if needed
                textColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.Gray
            ),*/
                maxLines = 2 // Allow for multi-line input
            )
            Button(
                onClick = {
                    if (newMessageText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            buttonEnabled = false
                            // Call your function to send the message
                            // sendMessage(channelId, newMessageText) // Assuming you have a suspend function for this
                            println("Sending message: $newMessageText to channel $channelId")
                            // Optionally, clear the text field and refresh messages
                            // For optimistic updates, you can add the message to the list immediately
                            // val sentMessage = Message( /* ... create a temporary message object ... */)
                            // messages.add(0, sentMessage) // Add to the top for reverseLayout
                            // listState.animateScrollToItem(0)

                            // Simulate sending and then refresh or update based on actual send result
                            // For now, let's just clear and re-fetch as an example
                            var successfullySent = false
                            withContext(Dispatchers.IO) {
                                successfullySent = sendMessage(
                                    channelId,
                                    newMessageText
                                ) // Make sure this function exists
                            }
                            if (successfullySent) {
                                newMessageText = ""
                                val result =
                                    fetchMessages(channelId = channelId) // Re-fetch messages
                                messages.clear()
                                messages.addAll(result)
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(0, 1)
                                }
                                buttonEnabled = true
                            } else {
                                // Handle send failure (e.g., show a toast)
                                println("Failed to send message")
                                buttonEnabled = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize(0.25f)
                    .align(Alignment.CenterEnd),
                enabled = newMessageText.isNotBlank() && buttonEnabled // Disable button if text is empty
            ) {
                Text("Send")
            }
        }
    }
}

suspend fun jumpToMessage(
    channelID: String,
    messageID: String,
    listState: ScalingLazyListState,
    messageList: SnapshotStateList<Message>
): Boolean {
    // 1. check if message is already in message list, if yes jump to it and return
    // 2. check if message really exists /channels/<id>/messages/<id>, if no, return with error
    // 3. fetch next 100 messages, check if id in them
    // 4. add to message list
    // 5. if message in newly fetched, jump to it and return, else jump to 3.

    var iterLimit = 50 // max 50 iterations of while to prevent while(true) == 5k messages limit
    var message = messageList.isMessageInList(messageID)
    while (message == null) { // if message is already there, scroll to it
        if (iterLimit == 0) return false // not successful

        val result = fetchMessages(
            channelId = channelID,
            messagesBeforeId = messageList[messageList.size - 1].id,
            100
        )
        messageList.addAll(result)

        message = messageList.isMessageInList(messageID)

        iterLimit--
    }
    // message found, jump to it
    listState.animateScrollToItem(messageList.getMessageIndex(message))
    return true // for success
}

fun SnapshotStateList<Message>.isMessageInList(messageID: String): Message? {
    return this.find { it.id == messageID }
}

fun SnapshotStateList<Message>.getMessageIndex(message: Message): Int {
    return this.indexOf(message)
}

enum class QRState {
    Start,
    QRScan,
    QRLogin
}

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

fun generateQrCodeBitmap(text: String, width: Int = 512, height: Int = 512): Bitmap? {
    if (text.isBlank()) return null
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            "https://discordapp.com/ra/" + text,
            BarcodeFormat.QR_CODE,
            width,
            height,
            null
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0 else 255)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace() // Log the error
        null
    }
}

@Composable
fun ManualTokenScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var newTokenText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column() {
            Text("Manual token input")


            TextField( // Using OutlinedTextField for better visibility on Wear
                value = newTokenText,
                onValueChange = { newTokenText = it },
                label = { Text("Token") },
                modifier = Modifier.fillMaxWidth(0.75f),

                maxLines = 1 // Allow for multi-line input
            )
            Button(
                onClick = {
                    if (newTokenText.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            if (validateToken(newTokenText)) {
                                withContext(Dispatchers.Main) {
                                    println("success, going home")
                                    userToken = newTokenText
                                    saveToken(context, newTokenText)
                                    navController.clearBackStack("home")
                                    navController.navigate("home")
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(0.25f),
                enabled = newTokenText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Send")
            }
        }
    }

}

fun validateToken(token: String): Boolean {
    return fetchMe(token) != null
}


@Composable
fun LoginScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()

    var newLoginText by remember { mutableStateOf("") }

    var newPasswordText by remember { mutableStateOf("") }

    val context = LocalContext.current


    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // bounding box
        Column(
            modifier = Modifier.fillMaxSize(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { // column for text entries and login button below
            // mail entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = newLoginText,
                    onValueChange = { newLoginText = it },
                    label = { Text("Email") },
                    maxLines = 1 // Allow for multi-line input
                )
            }

            // password entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = newPasswordText,
                    onValueChange = { newPasswordText = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    maxLines = 1 // Allow for multi-line input
                )
            }


            // login button
            Button(
                onClick = {
                    if (newLoginText.isNotBlank() && newPasswordText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            // Simulate sending and then refresh or update based on actual send result
                            // For now, let's just clear and re-fetch as an example
                            var token: Pair<String?, Boolean> = Pair(null, false)
                            withContext(Dispatchers.IO) {
                                token = sendLogin(
                                    newLoginText,
                                    newPasswordText
                                ) // sendLogin automatically sets the user token if successful
                            }
                            if (!token.second) { // mfa not required
                                newLoginText = ""
                                newPasswordText = ""

                                if (token.first != null && token.first?.isNotBlank() ?: false) {
                                    saveToken(context, token.first!!)
                                }

                                // exit this view and go to home logged in
                                navController.clearBackStack("home")
                            } else {
                                navController.navigate("mfa/${token.first}")
                            }
                        }

                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = newLoginText.isNotBlank() && newPasswordText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Log In")
            }
        }
    }
}

@Composable
fun MFAScreen(navController: NavHostController, ticket: String) {
    val scope = rememberCoroutineScope()
    var new2FAText by remember { mutableStateOf("") }


    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { // bounding box
        Column(
            modifier = Modifier.fillMaxSize(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { // column for text entries and login button below
            // code entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.background) // Add a background to overlay content
                    .padding(horizontal = 16.dp, vertical = 3.dp)
            ) {
                TextField( // Using OutlinedTextField for better visibility on Wear
                    value = new2FAText,
                    onValueChange = { new2FAText = it },
                    label = { Text("2FA Code") },
                    maxLines = 1 // Allow for multi-line input
                )
            }

            // login button
            Button(
                onClick = {
                    if (new2FAText.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            var token: String? = null
                            withContext(Dispatchers.IO) {
                                token = send2FA(
                                    ticket,
                                    new2FAText
                                ) // sendLogin automatically sets the user token if successful
                            }
                            if (token != null && token.isNotBlank()) {
                                saveToken(context, token)
                                navController.clearBackStack("home")
                                new2FAText = ""
                            }

                        }

                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = new2FAText.isNotBlank() // Disable button if text is empty
            ) {
                Text("Confirm")
            }
        }
    }
}

fun saveToken(context: Context, token: String) {
    userToken = token

    val sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit { putString("user_token", userToken) }
}

suspend fun sendMessage(channelId: String, content: String): Boolean {
    // Implement your network call to send the message here
    // Return true if successful, false otherwise
    println("API call to send '$content' to channel '$channelId'")

    val json = """
        {
            "content": "$content"
        }
    """.trimIndent()

    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

    val client = OkHttpClient()
    val url = "https://discord.com/api/v9/channels/$channelId/messages"
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .headers(
            Headers.headersOf(
                "Authorization",
                "$userToken",
                "Content-Type",
                "application/json"
            )
        )

        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return false
        }

        println("Request succeeded")

        val body = response.body.string()
        println(body)
        if (body.isBlank()) return false

        return true
    }
}

suspend fun sendLogin(email: String, password: String): Pair<String?, Boolean> {
    // Implement your network call to send the message here
    // Return true if successful, false otherwise

    val json = """
        {
            "login": "$email",
            "password": "$password",
            "undelete": false
        }
    """.trimIndent()

    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

    val client = OkHttpClient()
    val url = "https://discord.com/api/v9/auth/login"
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .headers(
            Headers.headersOf(
                "Content-Type",
                "application/json"
            )
        )

        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return Pair(null, false)
        }

        println("Request succeeded")

        val body = response.body.string()

        if (body.isBlank()) return Pair(null, false)

        val json = Json { ignoreUnknownKeys = true }

        var parsedBody: LoginReply? = null
        try {
            parsedBody = json.decodeFromString<LoginReply>(body)
        } catch (e: Exception) {
            // the incoming body is probably 2fa stuff like authenticator, not gut, but do nothing rn
            // TODO: make 2fa flow
        }

        if (parsedBody == null) return Pair(null, false)

        println("Parsed body: $parsedBody")
        if (parsedBody.mfa) {
            return Pair(parsedBody.ticket, true)
        } else {
            return Pair(parsedBody.ticket, false)
        }
    }
}

suspend fun send2FA(ticket: String, code: String): String? {
    // Implement your network call to send the message here
    // Return true if successful, false otherwise

    val json = """
        {
            "code": "$code",
            "ticket": "$ticket",
            "login_source": null,
            "gift_code_sku_id": null
        }
    """.trimIndent()

    println(json)

    val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

    val client = OkHttpClient()
    val url = "https://discord.com/api/v9/auth/mfa/totp"
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .headers(
            Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorisation",
                "None"
            )
        )

        .build()

    println("request built")
    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            println("Request failed with code ${response.code}")
            println(response.body.string())
            println(response.headers)
            return null
        }

        println("Request succeeded")

        val body = response.body.string()

        if (body.isBlank()) return null

        val json = Json { ignoreUnknownKeys = true }

        var parsedBody: MFAReply? = null
        try {
            parsedBody = json.decodeFromString<MFAReply>(body)
        } catch (e: Exception) {
            // the incoming body is probably 2fa stuff like authenticator, not gut, but do nothing rn
            // TODO: make 2fa flow
        }

        if (parsedBody == null) return null

        println("Parsed body: $parsedBody")
        return parsedBody.token
    }
}


@Composable
fun Greeting(navController: NavHostController, greetingName: String, isOpen: Boolean) {
    var welcomeText by remember { mutableStateOf("") }

    welcomeText = stringResource(R.string.hello_world, "")

    SwipeToDismissBox(
        onDismissed = {},
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = welcomeText
            )
            Text(
                text = "Welcome to WearCord",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
            )

        }
    }

    LaunchedEffect(Unit) {
        GlobalScope.launch {
            if (me != null) {
                welcomeText += me!!.global_name ?: ""
                return@launch
            }
            me = fetchMe()
            if (me == null) {
                // if me not fetched, go to login screen
                withContext(Dispatchers.Main) {
                    navController.navigate("qr")
                }
            } else {
                welcomeText += me!!.global_name ?: ""
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}