package top.anorak01.wearcord.presentation.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A successful response or a recoverable request failure. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Failure(
        val reason: Reason,
        val message: String,
        val statusCode: Int? = null,
    ) : ApiResult<Nothing>

    enum class Reason {
        NETWORK,
        UNAUTHORIZED,
        RATE_LIMITED,
        SERVER,
        HTTP,
        INVALID_RESPONSE,
        INVALID_REQUEST,
    }
}

/** Shared Discord HTTP transport. Network work is always confined to [Dispatchers.IO]. */
object DiscordApi {
    @PublishedApi
    internal val baseUrl = "https://discord.com/api/v9/".toHttpUrl()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend inline fun <reified T> get(
        path: String,
        token: String?,
        queryParameters: Map<String, String> = emptyMap(),
    ): ApiResult<T> {
        if (token.isNullOrBlank()) return missingToken()

        val url = baseUrl.newBuilder().apply {
            addPathSegments(path.trim('/'))
            queryParameters.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token)
            .build()
        return executeAndDecode(request)
    }

    suspend inline fun <reified T> post(
        path: String,
        token: String?,
        body: JsonObject,
    ): ApiResult<T> {
        if (token.isNullOrBlank()) return missingToken()
        val request = jsonRequest(path, body)
            .header("Authorization", token)
            .build()
        return executeAndDecode(request)
    }

    suspend inline fun <reified T> postWithoutAuth(
        path: String,
        body: JsonObject,
    ): ApiResult<T> = executeAndDecode(jsonRequest(path, body).build())

    @PublishedApi
    internal fun jsonRequest(path: String, body: JsonObject): Request.Builder =
        Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments(path.trim('/')).build())
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonMediaType))

    @PublishedApi
    internal fun missingToken(): ApiResult.Failure = ApiResult.Failure(
        ApiResult.Reason.UNAUTHORIZED,
        "Sign in to Discord to continue.",
    )

    @PublishedApi
    internal suspend inline fun <reified T> executeAndDecode(request: Request): ApiResult<T> =
        when (val response = execute(request)) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> decode(response.data)
        }

    @PublishedApi
    internal inline fun <reified T> decode(body: String): ApiResult<T> = try {
        ApiResult.Success(json.decodeFromString<T>(body))
    } catch (_: SerializationException) {
        invalidResponse()
    } catch (_: IllegalArgumentException) {
        invalidResponse()
    }

    @PublishedApi
    internal suspend fun execute(request: Request): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use httpFailure(response.code)
                }

                val body = response.body.string()
                if (body.isBlank()) invalidResponse() else ApiResult.Success(body)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            networkFailure()
        } catch (_: SecurityException) {
            networkFailure()
        }
    }

    @PublishedApi
    internal fun networkFailure() = ApiResult.Failure(
        ApiResult.Reason.NETWORK,
        "No connection to Discord. Check your internet connection and retry.",
    )

    @PublishedApi
    internal fun invalidResponse() = ApiResult.Failure(
        ApiResult.Reason.INVALID_RESPONSE,
        "Discord returned an unexpected response. Please retry.",
    )

    @PublishedApi
    internal fun httpFailure(statusCode: Int): ApiResult.Failure {
        val (reason, message) = when (statusCode) {
            401 -> ApiResult.Reason.UNAUTHORIZED to "Your Discord session has expired. Sign in again."
            429 -> ApiResult.Reason.RATE_LIMITED to "Discord is rate limiting requests. Wait a moment and retry."
            in 500..599 -> ApiResult.Reason.SERVER to "Discord is unavailable right now. Retry later."
            else -> ApiResult.Reason.HTTP to "Discord request failed (HTTP $statusCode)."
        }
        return ApiResult.Failure(reason, message, statusCode)
    }
}
