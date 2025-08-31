package top.anorak01.wearcord.presentation.message

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import top.anorak01.wearcord.presentation.Message
import top.anorak01.wearcord.presentation.fetchers.fetchMessages
import top.anorak01.wearcord.presentation.userToken

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