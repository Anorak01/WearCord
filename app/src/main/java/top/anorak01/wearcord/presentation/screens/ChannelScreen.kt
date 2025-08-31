package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.fetchers.fetchGuildChannels

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