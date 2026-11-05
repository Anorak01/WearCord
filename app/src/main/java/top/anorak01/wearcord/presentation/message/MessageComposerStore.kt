package top.anorak01.wearcord.presentation.message

import androidx.compose.runtime.mutableStateMapOf
import top.anorak01.wearcord.presentation.Message

data class ReplyTarget(
    val messageId: String,
    val authorName: String,
    val preview: String,
)

class MessageComposerStore {
    private val replyTargets = mutableStateMapOf<String, ReplyTarget>()
    private val sentRevisions = mutableStateMapOf<String, Int>()

    fun replyFor(channelId: String): ReplyTarget? = replyTargets[channelId]

    fun sentRevision(channelId: String): Int = sentRevisions[channelId] ?: 0

    fun startReply(channelId: String, message: Message) {
        replyTargets[channelId] = ReplyTarget(
            messageId = message.id,
            authorName = message.author.global_name ?: message.author.username,
            preview = message.content.ifBlank { "Attachment or unsupported content" },
        )
    }

    fun cancelReply(channelId: String) {
        replyTargets.remove(channelId)
    }

    fun markMessageSent(channelId: String) {
        replyTargets.remove(channelId)
        sentRevisions[channelId] = sentRevision(channelId) + 1
    }
}

val messageComposerStore = MessageComposerStore()
