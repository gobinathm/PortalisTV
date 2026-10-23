package io.github.gobinathm.portalistv.core.network

import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.net.Proxy as JavaProxy

/** An HTTP proxy for one profile. Credentials, if any, are sent only to the proxy. */
data class ProxyConfig(
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
)

/**
 * Creates OkHttp clients. All clients share one connection pool and dispatcher, and differ only
 * in per-profile settings such as the proxy. TLS uses the platform defaults: no custom trust
 * managers and no "trust all" options, by design.
 */
class HttpClients(base: OkHttpClient = defaultClient()) {
    private val shared = base

    fun forProfile(proxy: ProxyConfig?): OkHttpClient {
        if (proxy == null) return shared
        return shared.newBuilder()
            .proxy(JavaProxy(JavaProxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy.host, proxy.port)))
            .apply {
                if (proxy.username != null) {
                    val credential = Credentials.basic(proxy.username, proxy.password.orEmpty())
                    proxyAuthenticator { _, response ->
                        // Give up after one attempt instead of looping on wrong credentials.
                        if (response.request.header(PROXY_AUTHORIZATION) != null) {
                            null
                        } else {
                            response.request.newBuilder().header(PROXY_AUTHORIZATION, credential).build()
                        }
                    }
                }
            }
            .build()
    }

    companion object {
        private const val PROXY_AUTHORIZATION = "Proxy-Authorization"
        private const val CONNECT_TIMEOUT_S = 10L
        private const val READ_TIMEOUT_S = 20L
        private const val CALL_TIMEOUT_S = 60L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
            // Redirects to another scheme could silently downgrade HTTPS to HTTP.
            .followSslRedirects(false)
            .build()
    }
}
