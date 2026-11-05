package top.anorak01.wearcord.presentation.message

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import top.anorak01.wearcord.presentation.Author
import top.anorak01.wearcord.presentation.Message

class MessageComposerTest {
    @Test
    fun replyPayloadContainsTheReferencedMessage() {
        val payload = buildMessagePayload("channel-1", "Hello \"world\"", "message-2")
        val reference = payload.getValue("message_reference").jsonObject

        assertEquals("Hello \"world\"", payload.getValue("content").jsonPrimitive.content)
        assertEquals("message-2", reference.getValue("message_id").jsonPrimitive.content)
        assertEquals("channel-1", reference.getValue("channel_id").jsonPrimitive.content)
        assertFalse(reference.getValue("fail_if_not_exists").jsonPrimitive.boolean)
    }

    @Test
    fun normalMessageOmitsReplyReference() {
        val payload = buildMessagePayload("channel-1", "Hello", null)
        assertNull(payload["message_reference"])
    }

    @Test
    fun replyTargetIsClearedAndSentRevisionAdvancesAfterSending() {
        val store = MessageComposerStore()
        val message = testMessage()

        store.startReply("channel", message)
        assertEquals("message", store.replyFor("channel")?.messageId)
        store.markMessageSent("channel")

        assertNull(store.replyFor("channel"))
        assertEquals(1, store.sentRevision("channel"))
    }

    private fun testMessage() = Message(
        id = "message",
        content = "Original message",
        author = Author(id = "author", username = "user", global_name = null, avatar = null),
        type = 0,
        flags = 0,
    )
}
