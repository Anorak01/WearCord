package top.anorak01.wearcord.presentation.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import top.anorak01.wearcord.presentation.Channel
import top.anorak01.wearcord.presentation.ChannelGroup
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.fetchers.fetchDms
import top.anorak01.wearcord.presentation.fetchers.fetchGuildChannels
import top.anorak01.wearcord.presentation.fetchers.fetchGuildPositions
import top.anorak01.wearcord.presentation.fetchers.fetchGuilds
import top.anorak01.wearcord.presentation.fetchers.sortGuilds
import top.anorak01.wearcord.presentation.network.ApiResult
import java.util.concurrent.ConcurrentHashMap

class DiscordListCache(
    private val guildFetcher: suspend () -> ApiResult<List<Guild>> = ::fetchGuilds,
    private val guildPositionsFetcher: suspend () -> List<String> = ::fetchGuildPositions,
    private val dmFetcher: suspend () -> ApiResult<List<Channel>> = ::fetchDms,
    private val channelFetcher: suspend (String) -> ApiResult<List<ChannelGroup>> = ::fetchGuildChannels,
) {
    var guilds by mutableStateOf<List<Guild>>(emptyList())
        private set
    var dms by mutableStateOf<List<Channel>>(emptyList())
        private set
    val channelsByGuild: SnapshotStateMap<String, List<ChannelGroup>> = mutableStateMapOf()

    private val guildMutex = Mutex()
    private val dmMutex = Mutex()
    private val channelMutexes = ConcurrentHashMap<String, Mutex>()
    private var generation = 0L

    suspend fun refreshGuilds(): ApiResult<List<Guild>> {
        if (!guildMutex.tryLock()) return ApiResult.Success(guilds)
        val requestGeneration = generation
        try {
            val result = coroutineScope {
                val guildRequest = async { guildFetcher() }
                val positionsRequest = async { guildPositionsFetcher() }
                guildRequest.await() to positionsRequest.await()
            }
            val guildResult = result.first
            if (guildResult is ApiResult.Success && generation == requestGeneration) {
                guilds = sortGuilds(guildResult.data, result.second)
                return ApiResult.Success(guilds)
            }
            return if (generation == requestGeneration) guildResult else ApiResult.Success(guilds)
        } finally {
            guildMutex.unlock()
        }
    }

    suspend fun refreshDms(): ApiResult<List<Channel>> {
        if (!dmMutex.tryLock()) return ApiResult.Success(dms)
        val requestGeneration = generation
        try {
            val result = dmFetcher()
            if (result is ApiResult.Success && generation == requestGeneration) {
                dms = result.data
            }
            return if (generation == requestGeneration) result else ApiResult.Success(dms)
        } finally {
            dmMutex.unlock()
        }
    }

    suspend fun refreshChannels(guildId: String): ApiResult<List<ChannelGroup>> {
        val mutex = channelMutexes.getOrPut(guildId) { Mutex() }
        if (!mutex.tryLock()) return ApiResult.Success(channelsByGuild[guildId].orEmpty())
        val requestGeneration = generation
        try {
            val result = channelFetcher(guildId)
            if (result is ApiResult.Success && generation == requestGeneration) {
                channelsByGuild[guildId] = result.data
            }
            return if (generation == requestGeneration) {
                result
            } else {
                ApiResult.Success(channelsByGuild[guildId].orEmpty())
            }
        } finally {
            mutex.unlock()
        }
    }

    fun clear() {
        generation++
        guilds = emptyList()
        dms = emptyList()
        channelsByGuild.clear()
    }
}

val discordListCache = DiscordListCache()
