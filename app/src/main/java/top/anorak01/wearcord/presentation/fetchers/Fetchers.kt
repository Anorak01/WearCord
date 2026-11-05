package top.anorak01.wearcord.presentation.fetchers

import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.Me
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.network.ApiResult
import top.anorak01.wearcord.presentation.network.DiscordApi
import top.anorak01.wearcord.presentation.userToken

suspend fun fetchMessages(
    channelId: String,
    messagesBeforeId: String = "",
    messageLimit: Int = 50,
): ApiResult<List<Message>> {
    if (messageLimit !in 1..100) {
        return ApiResult.Failure(
            ApiResult.Reason.INVALID_REQUEST,
            "Message limit must be between 1 and 100.",
        )
    }

    val query = buildMap {
        put("limit", messageLimit.toString())
        if (messagesBeforeId.isNotBlank()) put("before", messagesBeforeId)
    }
    return DiscordApi.get("channels/$channelId/messages", userToken, query)
}

suspend fun fetchGuilds(): ApiResult<List<Guild>> =
    DiscordApi.get("users/@me/guilds", userToken)

suspend fun fetchGuildChannels(guildId: String): ApiResult<Map<String, List<Channel>>> =
    when (val result = DiscordApi.get<List<Channel>>("guilds/$guildId/channels", userToken)) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> ApiResult.Success(groupChannels(result.data))
    }

private fun groupChannels(channels: List<Channel>): Map<String, List<Channel>> {
    val sortedChannels = channels.sortedBy { it.position ?: Int.MAX_VALUE }
    val grouped = linkedMapOf<String, MutableList<Channel>>()

    // Create categories first so children are retained regardless of response order.
    sortedChannels.filter { it.parent_id == null }.forEach { channel ->
        grouped[channel.id] = mutableListOf()
    }
    sortedChannels.filter { it.parent_id != null }.forEach { channel ->
        grouped.getOrPut(channel.parent_id!!) { mutableListOf() }.add(channel)
    }
    return grouped
}

/** Returns DMs sorted from newest to oldest activity. */
suspend fun fetchDms(): ApiResult<List<Channel>> =
    when (val result = DiscordApi.get<List<Channel>>("users/@me/channels", userToken)) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> {
            val channels = result.data.map { channel ->
                if (channel.recipients.isEmpty()) {
                    channel
                } else {
                    val recipient = channel.recipients.first()
                    channel.copy(name = recipient.global_name ?: recipient.username)
                }
            }.sortedByDescending { toTimestamp(it.last_message_id ?: it.id) }
            ApiResult.Success(channels)
        }
    }

suspend fun fetchMe(token: String? = userToken): ApiResult<Me> =
    DiscordApi.get("users/@me", token)

fun toTimestamp(id: String): Long = (id.toLongOrNull() ?: 0L) shr 22

fun getIconUrl(guildId: String, iconId: String): String =
    "https://cdn.discordapp.com/icons/$guildId/$iconId.png?size=128"

fun getUserUrl(userId: String, iconId: String): String =
    "https://cdn.discordapp.com/avatars/$userId/$iconId.png?size=128"
