package io.github.gobinathm.portalistv.core.stalker

import io.github.gobinathm.portalistv.core.common.log.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The payload of a portal reply, or a sign that the session token is no longer accepted. */
internal sealed interface Reply {
    data class Ok(val js: JsonElement) : Reply

    data object AuthFailed : Reply
}

/**
 * Sends one portal request with the headers and cookies a set-top box sends, and unwraps the
 * `{"js": ...}` envelope. Bodies are capped so a hostile or broken portal cannot exhaust memory.
 */
internal class StalkerTransport(
    private val client: OkHttpClient,
    private val config: StalkerConfig,
    private val referer: HttpUrl,
    private val maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES,
    private val recorder: ExchangeRecorder = ExchangeRecorder.NONE,
) {
    suspend fun get(endpoint: HttpUrl, params: List<Pair<String, String>>, token: String?): Reply {
        val url = endpoint.newBuilder()
            .apply { params.forEach { (name, value) -> addQueryParameter(name, value) } }
            .addQueryParameter("JsHttpRequest", "1-xml")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", config.identity.userAgent)
            .header("X-User-Agent", "Model: ${config.identity.stbModel}; Link: Ethernet")
            .header("Referer", referer.toString())
            .header("Accept", "*/*")
            .header("Cookie", cookies())
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val label = params.filter { it.first == "type" || it.first == "action" }.joinToString("/") { it.second }
        client.newCall(request).await().use { response ->
            val body = response.readCapped()
            Log.d(TAG) { "$label -> HTTP ${response.code}, ${body.length} chars" }
            recorder.record(Exchange(label, params, response.code, body))
            return when {
                response.code == HTTP_UNAUTHORIZED || response.code == HTTP_FORBIDDEN -> Reply.AuthFailed
                body.trimStart().startsWith(AUTH_FAILED_TEXT, ignoreCase = true) -> Reply.AuthFailed
                !response.isSuccessful -> throw ProtocolException("HTTP ${response.code} for $label")
                else -> Reply.Ok(parseEnvelope(body, label))
            }
        }
    }

    private fun cookies(): String = listOf(
        "mac" to config.mac,
        "stb_lang" to config.language,
        "timezone" to config.timezone,
    ).joinToString("; ") { (name, value) -> "$name=${URLEncoder.encode(value, Charsets.UTF_8.name())}" }

    private fun Response.readCapped(): String {
        val source = body.source()
        if (source.request(maxBodyBytes + 1)) throw ProtocolException("Response larger than $maxBodyBytes bytes")
        return source.buffer.readUtf8()
    }

    private fun parseEnvelope(body: String, label: String): JsonElement {
        val root = try {
            json.parseToJsonElement(body)
        } catch (e: SerializationException) {
            throw ProtocolException("Not JSON: $label", e)
        }
        return (root as? JsonObject)?.get("js") ?: throw ProtocolException("No js payload: $label")
    }

    companion object {
        private const val TAG = "Stalker"
        private const val AUTH_FAILED_TEXT = "Authorization failed"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403

        /** Large channel lists reach a few megabytes; anything far beyond that is not a portal reply. */
        const val DEFAULT_MAX_BODY_BYTES = 32L * 1024 * 1024

        private val json = Json { isLenient = true }
    }
}

/** Runs the call on OkHttp's dispatcher and cancels it when the coroutine is cancelled. */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onResponse(call: Call, response: Response) = continuation.resume(response) { _, _, _ ->
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
        },
    )
}
