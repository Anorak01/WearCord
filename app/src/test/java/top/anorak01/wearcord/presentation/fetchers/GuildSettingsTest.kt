package top.anorak01.wearcord.presentation.fetchers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.anorak01.wearcord.presentation.Guild
import java.util.Base64

class GuildSettingsTest {
    @Test
    fun sortsByGuildPositionsWithoutReorderingUnlistedGuilds() {
        val guilds = listOf(guild("1"), guild("2"), guild("3"), guild("4"))

        val result = sortGuilds(guilds, listOf("3", "1"))

        assertEquals(listOf("3", "1", "2", "4"), result.map { it.id })
    }

    @Test
    fun preservesRestOrderWithoutSettings() {
        val guilds = listOf(guild("2"), guild("1"))
        assertEquals(guilds, sortGuilds(guilds, emptyList()))
    }

    @Test
    fun decodesPackedGuildPositionsFromPreloadedSettings() {
        val nested = byteArrayOf(0x12, 0x10) + littleEndian(123) + littleEndian(456)
        val root = byteArrayOf(0x72, nested.size.toByte()) + nested
        val encoded = Base64.getEncoder().encodeToString(root)

        assertEquals(listOf("123", "456"), GuildPositionsDecoder.decode(encoded))
    }

    @Test
    fun decodesUnpackedGuildPositions() {
        val nested = byteArrayOf(0x11) + littleEndian(123) + byteArrayOf(0x11) + littleEndian(456)
        val root = byteArrayOf(0x72, nested.size.toByte()) + nested
        val encoded = Base64.getEncoder().encodeToString(root)

        assertEquals(listOf("123", "456"), GuildPositionsDecoder.decode(encoded))
    }

    @Test
    fun malformedSettingsFallBackToRestOrder() {
        assertTrue(GuildPositionsDecoder.decode("not base64").isEmpty())
    }

    private fun guild(id: String) = Guild(id = id, name = id, icon = null)

    private fun littleEndian(value: Long): ByteArray =
        ByteArray(Long.SIZE_BYTES) { index -> ((value ushr (index * 8)) and 0xff).toByte() }
}
