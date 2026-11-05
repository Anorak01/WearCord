package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.RichText
import top.anorak01.wearcord.presentation.components.FullScreenImageView
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.fetchers.fetchMessages
import top.anorak01.wearcord.presentation.message.jumpToMessage
import top.anorak01.wearcord.presentation.message.sendMessage
import top.anorak01.wearcord.presentation.network.ApiResult

private const val MESSAGE_PAGE_SIZE = 50

@Composable
fun MessageScreen(navController: NavHostController, channelId: String) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberScalingLazyListState()
    val isAtTop by remember {
        derivedStateOf { messages.isNotEmpty() && listState.centerItemIndex >= messages.lastIndex }
    }

    var newMessageText by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPaginating by remember { mutableStateOf(false) }
    var hasMoreMessages by remember { mutableStateOf(true) }
    var requestFailure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var sendFailure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(channelId, refreshCount) {
        isRefreshing = true
        requestFailure = null
        when (val result = fetchMessages(channelId = channelId, messageLimit = MESSAGE_PAGE_SIZE)) {
            is ApiResult.Success -> {
                messages.clear()
                messages.addAll(result.data)
                hasMoreMessages = result.data.size == MESSAGE_PAGE_SIZE
                if (messages.isNotEmpty()) listState.scrollToItem(0)
            }
            is ApiResult.Failure -> requestFailure = result
        }
        isRefreshing = false
    }

    LaunchedEffect(channelId) {
        snapshotFlow { isAtTop && hasMoreMessages && !isRefreshing && !isPaginating }
            .collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                val before = messages.lastOrNull()?.id ?: return@collect
                isPaginating = true
                val result = fetchMessages(
                    channelId = channelId,
                    messagesBeforeId = before,
                    messageLimit = MESSAGE_PAGE_SIZE,
                )
                when (result) {
                    is ApiResult.Success -> {
                        val knownIds = messages.mapTo(HashSet()) { it.id }
                        val olderMessages = result.data.filter { it.id !in knownIds }
                        messages.addAll(olderMessages)
                        if (olderMessages.isEmpty() || result.data.size < MESSAGE_PAGE_SIZE) {
                            hasMoreMessages = false
                        }
                    }
                    is ApiResult.Failure -> requestFailure = result
                }
                isPaginating = false
            }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier.fillMaxWidth(0.95f).background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                reverseLayout = true,
                scalingParams = ScalingLazyColumnDefaults.scalingParams(
                    edgeScale = 0.5f,
                    edgeAlpha = 0.5f,
                    minTransitionArea = 0.4f,
                ),
                contentPadding = PaddingValues(vertical = 70.dp),
            ) {
                if (isRefreshing && messages.isEmpty()) item { Text("Loading messages…") }
                requestFailure?.let { error ->
                    item(key = "request_error") {
                        RequestError(failure = error, onRetry = { refreshCount++ })
                    }
                }
                if (isPaginating) item(key = "pagination_loading") { Text("Loading older…") }
                if (!isRefreshing && messages.isEmpty() && requestFailure == null) {
                    item { Text("No messages yet") }
                }
                items(messages, key = { it.id }) { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = message.author.username,
                                color = Color.Black,
                                textAlign = TextAlign.Start,
                                fontSize = 14.sp,
                            )
                            RichText(message.content)
                            message.attachments.forEach { attachment ->
                                if (attachment.content_type.startsWith("image/")) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Image(
                                        painter = rememberAsyncImagePainter(model = attachment.url),
                                        contentDescription = "Attachment: ${attachment.filename}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 150.dp)
                                            .clip(RoundedCornerShape(CornerSize(6.dp)))
                                            .clickable { fullScreenImageUrl = attachment.url },
                                        contentScale = ContentScale.Fit,
                                    )
                                    Text(
                                        text = attachment.filename,
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Text(
                                        text = "Attachment: ${attachment.filename} (${attachment.content_type})",
                                        color = Color.Cyan,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            }
                            if (message.type == 19) {
                                Text(
                                    text = "Reply: Jump to message",
                                    color = Color.Black,
                                    modifier = Modifier.clickable {
                                        val referencedMessage = message.message_reference ?: return@clickable
                                        scope.launch {
                                            jumpToMessage(
                                                channelId,
                                                referencedMessage.message_id,
                                                listState,
                                                messages,
                                            )
                                        }
                                    },
                                    textAlign = TextAlign.Start,
                                    fontSize = 8.sp,
                                )
                            } else if (message.type != 0 || message.flags != 0) {
                                Text(
                                    text = "Part of this message is not supported",
                                    color = Color.Red,
                                    textAlign = TextAlign.Start,
                                    fontSize = 8.sp,
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
            Button(
                onClick = { refreshCount++ },
                content = { Text("R") },
                modifier = Modifier.fillMaxSize(0.15f),
                enabled = !isRefreshing,
            )
            Button(
                onClick = { scope.launch { listState.animateScrollToItem(0, 1) } },
                content = { Text("D") },
                modifier = Modifier.fillMaxSize(0.15f),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 16.dp, vertical = 3.dp),
        ) {
            Column {
                sendFailure?.let {
                    Text(it.message, color = Color.Red, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newMessageText,
                        onValueChange = {
                            newMessageText = it
                            sendFailure = null
                        },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(0.75f),
                        maxLines = 2,
                        enabled = !isSending,
                    )
                    Button(
                        onClick = {
                            val content = newMessageText
                            if (content.isBlank() || isSending) return@Button
                            scope.launch {
                                isSending = true
                                sendFailure = null
                                when (val result = sendMessage(channelId, content)) {
                                    is ApiResult.Success -> {
                                        newMessageText = ""
                                        if (messages.none { it.id == result.data.id }) {
                                            messages.add(0, result.data)
                                        }
                                        listState.animateScrollToItem(0)
                                    }
                                    is ApiResult.Failure -> sendFailure = result
                                }
                                isSending = false
                            }
                        },
                        enabled = newMessageText.isNotBlank() && !isSending,
                    ) {
                        Text(if (isSending) "…" else "Send")
                    }
                }
            }
        }
    }

    fullScreenImageUrl?.let {
        FullScreenImageView(imageUrl = it) { fullScreenImageUrl = null }
    }
}
