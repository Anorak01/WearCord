package top.anorak01.wearcord.presentation.fetchers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.Me
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.userToken

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