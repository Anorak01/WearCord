package top.anorak01.wearcord.presentation.network

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.anorak01.wearcord.presentation.Me
import java.net.ServerSocket

class DiscordApiTest {
    @Test
    fun refusedConnectionReturnsNetworkFailureInsteadOfThrowing() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val request = Request.Builder().url("http://127.0.0.1:$port/").build()

        val result = DiscordApi.execute(request)

        val failure = result as ApiResult.Failure
        assertEquals(ApiResult.Reason.NETWORK, failure.reason)
    }

    @Test
    fun unauthorizedResponseIsDistinctFromNetworkFailure() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response(401, "{}"))
            server.start()

            val result = DiscordApi.execute(Request.Builder().url(server.url("/")).build())

            val failure = result as ApiResult.Failure
            assertEquals(ApiResult.Reason.UNAUTHORIZED, failure.reason)
            assertEquals(401, failure.statusCode)
        }
    }

    @Test
    fun serverErrorIsRetryableFailure() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response(503, "{}"))
            server.start()

            val result = DiscordApi.execute(Request.Builder().url(server.url("/")).build())

            val failure = result as ApiResult.Failure
            assertEquals(ApiResult.Reason.SERVER, failure.reason)
            assertEquals(503, failure.statusCode)
        }
    }

    @Test
    fun blankSuccessfulResponseIsInvalidResponse() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response(200, ""))
            server.start()

            val result = DiscordApi.execute(Request.Builder().url(server.url("/")).build())

            val failure = result as ApiResult.Failure
            assertEquals(ApiResult.Reason.INVALID_RESPONSE, failure.reason)
        }
    }

    @Test
    fun emptyResponseCanBeAcceptedForDeleteRequests() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(response(204, ""))
            server.start()

            val result = DiscordApi.execute(
                Request.Builder().url(server.url("/")).build(),
                allowEmptyResponse = true,
            )

            val success = result as ApiResult.Success
            assertEquals("", success.data)
        }
    }

    @Test
    fun validJsonIsDecodedAndUnknownFieldsAreIgnored() {
        val result = DiscordApi.decode<Me>(
            """{"id":"1","username":"watch","avatar":null,"global_name":"Watch User","extra":true}""",
        )

        val success = result as ApiResult.Success
        assertEquals("watch", success.data.username)
        assertEquals("Watch User", success.data.global_name)
    }

    @Test
    fun malformedJsonIsFailureInsteadOfException() {
        val result = DiscordApi.decode<Me>("not json")

        val failure = result as ApiResult.Failure
        assertEquals(ApiResult.Reason.INVALID_RESPONSE, failure.reason)
        assertTrue(failure.message.isNotBlank())
    }

    private fun response(status: Int, body: String): MockResponse =
        MockResponse.Builder().code(status).body(body).build()
}
