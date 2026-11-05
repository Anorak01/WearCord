package top.anorak01.wearcord.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.RichText
import top.anorak01.wearcord.presentation.components.ActionChip
import top.anorak01.wearcord.presentation.components.EmptyState
import top.anorak01.wearcord.presentation.components.FullScreenImageView
import top.anorak01.wearcord.presentation.components.LoadingState
import top.anorak01.wearcord.presentation.components.RequestError
import top.anorak01.wearcord.presentation.components.ScreenHeader
import top.anorak01.wearcord.presentation.components.SurfaceCard
import top.anorak01.wearcord.presentation.components.WearListScaffold
import top.anorak01.wearcord.presentation.fetchers.fetchMessages
import top.anorak01.wearcord.presentation.message.jumpToMessage
import top.anorak01.wearcord.presentation.network.ApiResult

private const val MESSAGE_PAGE_SIZE = 50
private const val FIRST_MESSAGE_ITEM_INDEX = 2

@Composable
fun MessageScreen(navController: NavHostController, channelId: String) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<Message>() }
    val listState = rememberScalingLazyListState()
    val isNearOlderMessages by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.isNotEmpty() && lastVisible >= layout.totalItemsCount - 2
        }
    }

    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var refreshCount by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isPaginating by remember { mutableStateOf(false) }
    var hasMoreMessages by remember { mutableStateOf(true) }
    var requestFailure by remember { mutableStateOf<ApiResult.Failure?>(null) }
    var replyFailure by remember { mutableStateOf(false) }

    LaunchedEffect(channelId, refreshCount) {
        isRefreshing = true
        requestFailure = null
        replyFailure = false
        when (val result = fetchMessages(channelId = channelId, messageLimit = MESSAGE_PAGE_SIZE)) {
            is ApiResult.Success -> {
                messages.clear()
                messages.addAll(result.data)
                hasMoreMessages = result.data.size == MESSAGE_PAGE_SIZE
                listState.scrollToItem(0)
            }
            is ApiResult.Failure -> requestFailure = result
        }
        isRefreshing = false
    }

    LaunchedEffect(channelId) {
        snapshotFlow { isNearOlderMessages && hasMoreMessages && !isRefreshing && !isPaginating }
            .collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                val before = messages.lastOrNull()?.id ?: return@collect
                isPaginating = true
                when (
                    val result = fetchMessages(
                        channelId = channelId,
                        messagesBeforeId = before,
                        messageLimit = MESSAGE_PAGE_SIZE,
                    )
                ) {
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

    WearListScaffold(listState = listState) {
        item { ScreenHeader("Messages") }
        item {
            ActionChip(
                label = "Write a message",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = { navController.navigate("compose/$channelId") },
            )
        }
        if (isRefreshing && messages.isEmpty()) item { LoadingState("Loading messages…") }
        if (!isRefreshing && messages.isEmpty() && requestFailure == null) {
            item { EmptyState("No messages yet", "Start the conversation from this channel.") }
        }
        items(messages, key = { it.id }) { message ->
            MessageCard(
                message = message,
                onImageClick = { fullScreenImageUrl = it },
                onReplyClick = replyClick@{
                    val reference = message.message_reference ?: return@replyClick
                    scope.launch {
                        replyFailure = false
                        val found = jumpToMessage(
                            channelID = channelId,
                            messageID = reference.message_id,
                            listState = listState,
                            messageList = messages,
                            listItemOffset = FIRST_MESSAGE_ITEM_INDEX,
                        )
                        replyFailure = !found
                    }
                },
            )
        }
        if (isPaginating) item { LoadingState("Loading older messages…") }
        requestFailure?.let { error ->
            item(key = "request_error") {
                RequestError(failure = error, onRetry = { refreshCount++ })
            }
        }
        if (replyFailure) {
            item {
                Text(
                    "Couldn’t load the replied-to message. Try again when you’re online.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                )
            }
        }
        item {
            ActionChip(
                label = if (isRefreshing) "Refreshing…" else "Refresh messages",
                icon = Icons.Default.Refresh,
                enabled = !isRefreshing,
                onClick = { refreshCount++ },
            )
        }
    }

    fullScreenImageUrl?.let {
        FullScreenImageView(imageUrl = it) { fullScreenImageUrl = null }
    }
}

@Composable
private fun MessageCard(
    message: Message,
    onImageClick: (String) -> Unit,
    onReplyClick: () -> Unit,
) {
    SurfaceCard {
        Text(
            text = message.author.global_name ?: message.author.username,
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.primary,
        )
        if (message.content.isNotBlank()) RichText(message.content)
        message.attachments.forEach { attachment ->
            if (attachment.content_type.startsWith("image/")) {
                Image(
                    painter = rememberAsyncImagePainter(model = attachment.url),
                    contentDescription = "Open attachment: ${attachment.filename}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp, max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(attachment.url) },
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = attachment.filename,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Attachment: ${attachment.filename} (${attachment.content_type})",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
        if (message.type == 19 && message.message_reference != null) {
            CompactChip(onClick = onReplyClick, label = { Text("View replied message") })
        } else if (message.type != 0 || message.flags != 0) {
            Text(
                text = "Some content in this message isn’t supported.",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
        }
    }
}
