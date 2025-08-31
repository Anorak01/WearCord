package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.fetchers.fetchGuilds
import top.anorak01.wearcord.presentation.fetchers.getIconUrl

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
