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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.fetchers.fetchDms
import top.anorak01.wearcord.presentation.fetchers.getUserUrl
import top.anorak01.wearcord.presentation.network.ApiResult

@Composable
fun DMScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val dms = remember { mutableStateListOf<Channel>() }
    val listState = rememberScalingLazyListState()
    var isLoading by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshCount) {
        isLoading = true
        failure = null
        when (val result = fetchDms()) {
            is ApiResult.Success -> {
                dms.clear()
                dms.addAll(result.data)
            }
            is ApiResult.Failure -> failure = result
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxWidth(0.8f).background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = false,
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minTransitionArea = 0.4f,
                ),
                contentPadding = PaddingValues(vertical = 40.dp),
            ) {
                if (isLoading && dms.isEmpty()) item { Text("Loading DMs…") }
                failure?.let { error ->
                    item(key = "request_error") {
                        RequestError(failure = error, onRetry = { refreshCount++ })
                    }
                }
                if (!isLoading && dms.isEmpty() && failure == null) item { Text("No DMs found") }
                items(dms, key = { it.id }) { channel ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .padding(horizontal = 4.dp)
                            .clickable { navController.navigate("messages/${channel.id}") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column {
                            Text(
                                text = channel.name ?: "Direct message",
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                            )
                            val recipient = channel.recipients.firstOrNull()
                            if (!recipient?.avatar.isNullOrEmpty()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = getUserUrl(recipient!!.id, recipient.avatar!!),
                                        placeholder = null,
                                    ),
                                    contentDescription = "${channel.name} avatar",
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .heightIn(80.dp)
                                        .clip(RoundedCornerShape(CornerSize(6.dp))),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        Button(
            onClick = { refreshCount++ },
            content = { Text("R") },
            modifier = Modifier.fillMaxSize(0.15f),
            enabled = !isLoading,
        )
    }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        Button(
            onClick = { scope.launch { listState.animateScrollToItem(0, 1) } },
            content = { Text("D") },
            modifier = Modifier.fillMaxSize(0.15f),
        )
    }
}
