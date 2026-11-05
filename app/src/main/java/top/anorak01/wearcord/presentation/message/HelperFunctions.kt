package top.anorak01.wearcord.presentation.message

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.fetchers.fetchMessages
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.network.DiscordApi
import top.anorak01.wearcord.presentation.userToken

suspend fun sendMessage(
    channelId: String,
    content: String,
    replyToMessageId: String? = null,
): ApiResult<Message> {
    if (content.isBlank()) {
        return ApiResult.Failure(ApiResult.Reason.INVALID_REQUEST, "A message cannot be empty.")
    }
    return DiscordApi.post(
        "channels/$channelId/messages",
        userToken,
        buildMessagePayload(channelId, content, replyToMessageId),
    )
}

internal fun buildMessagePayload(
    channelId: String,
    content: String,
    replyToMessageId: String?,
): JsonObject = buildJsonObject {
    put("content", content)
    if (replyToMessageId != null) {
        putJsonObject("message_reference") {
            put("message_id", replyToMessageId)
            put("channel_id", channelId)
            put("fail_if_not_exists", false)
        }
    }
}

suspend fun deleteMessage(channelId: String, messageId: String): ApiResult<Unit> =
    DiscordApi.delete("channels/$channelId/messages/$messageId", userToken)

suspend fun jumpToMessage(
    channelID: String,
    messageID: String,
    listState: ScalingLazyListState,
    messageList: SnapshotStateList<Message>,
    listItemOffset: Int = 0,
): Boolean {
    var message = messageList.isMessageInList(messageID)
    var remainingRequests = 50

    while (message == null && remainingRequests > 0) {
        val oldestMessageId = messageList.lastOrNull()?.id ?: return false
        val result = fetchMessages(
            channelId = channelID,
            messagesBeforeId = oldestMessageId,
            messageLimit = 100,
        )
        if (result !is ApiResult.Success || result.data.isEmpty()) return false

        val knownIds = messageList.mapTo(HashSet()) { it.id }
        val newMessages = result.data.filter { it.id !in knownIds }
        if (newMessages.isEmpty()) return false
        messageList.addAll(newMessages)

        message = messageList.isMessageInList(messageID)
        remainingRequests--
    }

    if (message == null) return false
    listState.animateScrollToItem(messageList.getMessageIndex(message) + listItemOffset)
    return true
}

fun SnapshotStateList<Message>.isMessageInList(messageID: String): Message? =
    find { it.id == messageID }

fun SnapshotStateList<Message>.getMessageIndex(message: Message): Int = indexOf(message)
