package top.anorak01.wearcord.presentation.fetchers

import top.anorak01.wearcord.presentation.Guild
import java.util.Base64

internal fun sortGuilds(guilds: List<Guild>, guildPositions: List<String>): List<Guild> {
    if (guildPositions.isEmpty()) return guilds
    val positions = guildPositions.mapIndexed { index, id -> id to index }.toMap()
    return guilds.withIndex()
        .sortedWith(compareBy({ positions[it.value.id] ?: Int.MAX_VALUE }, { it.index }))
        .map { it.value }
}

/** Reads only GuildFolders.guild_positions from Discord's PreloadedUserSettings protobuf. */
internal object GuildPositionsDecoder {
    fun decode(base64Settings: String): List<String> = runCatching {
        val data = Base64.getDecoder().decode(base64Settings)
        val reader = ProtoReader(data)
        while (true) {
            val tag = reader.nextTag() ?: break
            val field = tag ushr 3
            val wireType = tag and 7
            if (field == PRELOADED_GUILD_FOLDERS && wireType == LENGTH_DELIMITED) {
                return@runCatching decodeGuildFolders(reader.readBytes())
            }
            reader.skip(wireType)
        }
        emptyList()
    }.getOrDefault(emptyList())

    private fun decodeGuildFolders(data: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val reader = ProtoReader(data)
        while (true) {
            val tag = reader.nextTag() ?: break
            val field = tag ushr 3
            val wireType = tag and 7
            if (field == GUILD_POSITIONS && wireType == FIXED64) {
                result += reader.readFixed64().toString()
            } else if (field == GUILD_POSITIONS && wireType == LENGTH_DELIMITED) {
                val packed = reader.readBytes()
                require(packed.size % Long.SIZE_BYTES == 0)
                for (offset in packed.indices step Long.SIZE_BYTES) {
                    result += readLittleEndianULong(packed, offset).toString()
                }
            } else {
                reader.skip(wireType)
            }
        }
        return result
    }

    private class ProtoReader(private val data: ByteArray) {
        private var offset = 0

        fun nextTag(): Int? = if (offset == data.size) null else readVarint().toInt()

        fun readVarint(): Long {
            var value = 0L
            var shift = 0
            repeat(10) {
                val byte = data[offset++].toInt() and 0xff
                value = value or ((byte and 0x7f).toLong() shl shift)
                if (byte and 0x80 == 0) return value
                shift += 7
            }
            throw IllegalArgumentException("Invalid protobuf varint")
        }

        fun readBytes(): ByteArray {
            val length = readVarint().toInt()
            require(length >= 0 && offset + length <= data.size)
            return data.copyOfRange(offset, offset + length).also { offset += length }
        }

        fun readFixed64(): ULong = readLittleEndianULong(data, offset).also { offset += 8 }

        fun skip(wireType: Int) {
            when (wireType) {
                VARINT -> readVarint()
                FIXED64 -> offset += 8
                LENGTH_DELIMITED -> {
                    val length = readVarint().toInt()
                    require(length >= 0)
                    offset += length
                }
                FIXED32 -> offset += 4
                else -> throw IllegalArgumentException("Unsupported protobuf wire type: $wireType")
            }
            require(offset <= data.size)
        }
    }

    private fun readLittleEndianULong(data: ByteArray, offset: Int): ULong {
        require(offset >= 0 && offset + Long.SIZE_BYTES <= data.size)
        var value = 0UL
        repeat(Long.SIZE_BYTES) { index ->
            value = value or ((data[offset + index].toULong() and 0xffUL) shl (index * 8))
        }
        return value
    }

    private const val PRELOADED_GUILD_FOLDERS = 14
    private const val GUILD_POSITIONS = 2
    private const val VARINT = 0
    private const val FIXED64 = 1
    private const val LENGTH_DELIMITED = 2
    private const val FIXED32 = 5
}
