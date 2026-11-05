package top.anorak01.wearcord.presentation.cache

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.anorak01.wearcord.presentation.Guild
import top.anorak01.wearcord.presentation.network.ApiResult
import java.util.concurrent.atomic.AtomicInteger

class DiscordListCacheTest {
    @Test
    fun guildRefreshUsesDiscordSidebarOrder() = runBlocking {
        val cache = DiscordListCache(
            guildFetcher = { ApiResult.Success(testGuilds) },
            guildPositionsFetcher = { listOf("3", "1") },
        )

        cache.refreshGuilds()

        assertEquals(listOf("3", "1", "2"), cache.guilds.map { it.id })
    }

    @Test
    fun failedRefreshKeepsCachedGuilds() = runBlocking {
        var request = 0
        val cache = DiscordListCache(
            guildFetcher = {
                request++
                if (request == 1) {
                    ApiResult.Success(testGuilds)
                } else {
                    ApiResult.Failure(ApiResult.Reason.NETWORK, "offline")
                }
            },
            guildPositionsFetcher = { emptyList() },
        )

        cache.refreshGuilds()
        cache.refreshGuilds()

        assertEquals(testGuilds, cache.guilds)
    }

    @Test
    fun concurrentGuildRefreshesOnlyFetchOnce() = runBlocking {
        val requests = AtomicInteger()
        val cache = DiscordListCache(
            guildFetcher = {
                requests.incrementAndGet()
                delay(40)
                ApiResult.Success(testGuilds)
            },
            guildPositionsFetcher = { emptyList() },
        )

        val first = async { cache.refreshGuilds() }
        delay(10)
        val second = async { cache.refreshGuilds() }
        first.await()
        second.await()

        assertEquals(1, requests.get())
        assertEquals(testGuilds, cache.guilds)
    }

    @Test
    fun clearingCacheInvalidatesInFlightResult() = runBlocking {
        val cache = DiscordListCache(
            guildFetcher = {
                delay(40)
                ApiResult.Success(testGuilds)
            },
            guildPositionsFetcher = { emptyList() },
        )

        val refresh = async { cache.refreshGuilds() }
        delay(10)
        cache.clear()
        refresh.await()

        assertTrue(cache.guilds.isEmpty())
    }

    private companion object {
        val testGuilds = listOf(
            Guild(id = "1", name = "One", icon = null),
            Guild(id = "2", name = "Two", icon = null),
            Guild(id = "3", name = "Three", icon = null),
        )
    }
}
