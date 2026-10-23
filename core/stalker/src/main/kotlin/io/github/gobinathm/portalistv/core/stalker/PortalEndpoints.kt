package io.github.gobinathm.portalistv.core.stalker

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Derives the API endpoints to probe from the address a user enters.
 *
 * Users usually copy the portal's web address, such as `http://host/c/` or
 * `http://host/stalker_portal/c/`. The API sits beside the `c/` folder, at `portal.php` or
 * `server/load.php`, depending on the middleware version. Candidates are tried in order and the
 * first that completes a handshake is kept.
 */
object PortalEndpoints {
    private val apiFiles = listOf("server/load.php", "portal.php")

    /** @throws InvalidPortalUrlException for unusable URLs or plain HTTP without the opt-in. */
    fun parse(portalUrl: String, allowInsecureHttp: Boolean): HttpUrl {
        val url = portalUrl.trim().toHttpUrlOrNull() ?: throw InvalidPortalUrlException("Not an http(s) URL")
        if (!url.isHttps && !allowInsecureHttp) throw InvalidPortalUrlException("Plain HTTP is not allowed")
        return url
    }

    fun candidates(portalUrl: HttpUrl): List<HttpUrl> {
        val segments = portalUrl.pathSegments.filter { it.isNotEmpty() }
        // A direct API address is used as is.
        if (segments.lastOrNull()?.endsWith(".php") == true) return listOf(portalUrl.withoutQuery())

        // Folder that holds the web UI's "c/" folder, e.g. "/stalker_portal/" for ".../stalker_portal/c/".
        val base = segments.dropLastWhile { it == "c" || it.endsWith(".html") }
        val prefixes = buildList {
            add(base)
            if ("stalker_portal" !in base) add(base + "stalker_portal")
            add(emptyList())
            add(listOf("stalker_portal"))
        }.distinct()

        return prefixes.flatMap { prefix ->
            // Try the file that usually lives in this folder first.
            val files = if (prefix.lastOrNull() == "stalker_portal") apiFiles else apiFiles.reversed()
            files.map { file -> portalUrl.at(prefix, file) }
        }.distinct()
    }

    private fun HttpUrl.withoutQuery(): HttpUrl = newBuilder().query(null).fragment(null).build()

    private fun HttpUrl.at(prefix: List<String>, file: String): HttpUrl {
        val builder = newBuilder().encodedPath("/").query(null).fragment(null)
        prefix.forEach { builder.addPathSegment(it) }
        file.split('/').forEach { builder.addPathSegment(it) }
        return builder.build()
    }
}
