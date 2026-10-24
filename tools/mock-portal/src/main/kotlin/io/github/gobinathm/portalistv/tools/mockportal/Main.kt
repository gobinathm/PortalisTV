package io.github.gobinathm.portalistv.tools.mockportal

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder

/** Serves [portal] over HTTP. Returns the started server; call `stop(0)` to end it. */
fun serve(portal: MockPortal, port: Int, host: String = "0.0.0.0"): HttpServer =
    HttpServer.create(InetSocketAddress(host, port), 0).apply {
        createContext("/") { exchange -> exchange.use { respond(it, portal) } }
        start()
    }

private fun respond(exchange: HttpExchange, portal: MockPortal) {
    val query = exchange.requestURI.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.associate { pair ->
        val name = pair.substringBefore('=')
        val value = pair.substringAfter('=', "")
        URLDecoder.decode(name, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
    }
    val headers = exchange.requestHeaders.entries.associate { (name, values) -> name.lowercase() to values.first() }
    val response = portal.handle(PortalRequest(exchange.requestURI.path, query, headers))
    val bytes = response.body.encodeToByteArray()
    exchange.responseHeaders.add("Content-Type", "${response.contentType}; charset=utf-8")
    exchange.sendResponseHeaders(response.code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
    val call = "${query["type"]}/${query["action"]}"
    println("${exchange.requestMethod} ${exchange.requestURI.path} $call -> ${response.code}")
}

/**
 * Usage: `./gradlew :tools:mock-portal:run --args="--port 8088 --mac 00:1A:79:00:00:01 --token-ttl 30"`.
 * `--mac` may repeat; without it every device is allowed. `--token-ttl` is in seconds. `--filler 500`
 * sets how many filler channels there are, for long lists such as the scroll benchmark's.
 */
fun main(args: Array<String>) {
    var port = DEFAULT_PORT
    val macs = mutableSetOf<String>()
    var tokenTtlMs = MockPortal.DEFAULT_TOKEN_LIFETIME_MS
    var catalog = Catalog()
    args.toList().chunked(2).forEach { (flag, value) ->
        when (flag) {
            "--port" -> port = value.toInt()
            "--mac" -> macs += value.uppercase()
            "--token-ttl" -> tokenTtlMs = value.toLong() * MILLIS_PER_SECOND
            "--filler" -> catalog = Catalog(fillerChannels = value.toInt())
            else -> error("Unknown option $flag")
        }
    }
    serve(MockPortal(catalog, allowedMacs = macs, tokenLifetimeMillis = tokenTtlMs), port)
    println("Mock portal on port $port.")
    println("Profile URL: http://<this computer's IP>:$port/c/ (from the emulator: http://10.0.2.2:$port/c/)")
    println("Plain HTTP: turn on \"Allow insecure HTTP\" in the profile. Stop with Ctrl+C.")
}

private const val DEFAULT_PORT = 8088
private const val MILLIS_PER_SECOND = 1000L
