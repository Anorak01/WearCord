package top.anorak01.wearcord.presentation.fetchers

import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.ChannelGroup
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.LegacyUserSettings
import top.anorak01.wearcord.presentation.Me
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.UserSettingsProto
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

/** Returns empty when settings are unavailable; the REST guild order is then preserved. */
suspend fun fetchGuildPositions(): List<String> {
    when (val proto = DiscordApi.get<UserSettingsProto>("users/@me/settings-proto/1", userToken)) {
        is ApiResult.Success -> return GuildPositionsDecoder.decode(proto.data.settings)
        is ApiResult.Failure -> Unit
    }
    return when (val legacy = DiscordApi.get<LegacyUserSettings>("users/@me/settings", userToken)) {
        is ApiResult.Success -> legacy.data.guild_positions.orEmpty()
        is ApiResult.Failure -> emptyList()
    }
}

suspend fun fetchGuildChannels(guildId: String): ApiResult<List<ChannelGroup>> =
    when (val result = DiscordApi.get<List<Channel>>("guilds/$guildId/channels", userToken)) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> ApiResult.Success(groupChannels(result.data))
    }

internal fun groupChannels(channels: List<Channel>): List<ChannelGroup> {
    val sortedChannels = channels.sortedBy { it.position ?: Int.MAX_VALUE }
    val categories = sortedChannels.filter { it.type == CATEGORY_CHANNEL }
    val uncategorized = sortedChannels.filter { it.parent_id == null && it.type != CATEGORY_CHANNEL }
    val groups = mutableListOf<ChannelGroup>()

    if (uncategorized.isNotEmpty()) {
        groups += ChannelGroup(id = null, name = null, channels = uncategorized)
    }
    categories.forEach { category ->
        groups += ChannelGroup(
            id = category.id,
            name = category.name,
            channels = sortedChannels.filter { it.parent_id == category.id },
        )
    }

    val knownIds = buildSet {
        groups.forEach { group ->
            group.id?.let(::add)
            group.channels.forEach { add(it.id) }
        }
    }
    val orphaned = sortedChannels.filter { it.id !in knownIds && it.type != CATEGORY_CHANNEL }
    if (orphaned.isNotEmpty()) {
        groups += ChannelGroup(id = null, name = "Other channels", channels = orphaned)
    }
    return groups
}

private const val CATEGORY_CHANNEL = 4

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
