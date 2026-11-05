package top.anorak01.wearcord.presentation.fetchers

import org.junit.Assert.assertEquals
import org.junit.Test
import top.anorak01.wearcord.presentation.Channel

class ChannelGroupingTest {
    @Test
    fun childrenAppearUnderTheirCategoryRegardlessOfResponseOrder() {
        val result = groupChannels(
            listOf(
                channel(id = "general", name = "general", parentId = "community", position = 2),
                channel(id = "community", name = "Community", type = 4, position = 1),
            ),
        )

        assertEquals(1, result.size)
        assertEquals("Community", result.single().name)
        assertEquals(listOf("general"), result.single().channels.map { it.id })
    }

    @Test
    fun uncategorizedChannelsAreNotDropped() {
        val result = groupChannels(
            listOf(channel(id = "announcements", name = "announcements", parentId = null)),
        )

        assertEquals(1, result.size)
        assertEquals(null, result.single().name)
        assertEquals(listOf("announcements"), result.single().channels.map { it.id })
    }

    @Test
    fun orphanedChannelsRemainAvailable() {
        val result = groupChannels(
            listOf(channel(id = "archive", name = "archive", parentId = "deleted-category")),
        )

        assertEquals("Other channels", result.single().name)
        assertEquals(listOf("archive"), result.single().channels.map { it.id })
    }

    private fun channel(
        id: String,
        name: String,
        parentId: String? = null,
        position: Int = 0,
        type: Int = 0,
    ) = Channel(
        id = id,
        name = name,
        type = type,
        parent_id = parentId,
        position = position,
    )
}
