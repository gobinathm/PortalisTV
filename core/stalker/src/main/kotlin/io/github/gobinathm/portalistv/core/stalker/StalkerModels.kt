package io.github.gobinathm.portalistv.core.stalker

/** What `get_profile` said about this device. */
data class PortalAccount(
    /** The middleware's own id for this device, if reported. */
    val id: String?,
    val name: String?,
    /** 0 means active; other values mean the portal refuses or limits service. */
    val status: Int,
    val message: String?,
)

data class Genre(
    val id: String,
    val title: String,
    /** Adult content, hidden behind the parental lock by default. */
    val censored: Boolean,
)

data class Channel(
    val id: String,
    val name: String,
    val number: Int?,
    /** Opaque command passed back to `create_link` to get a playable URL. */
    val cmd: String,
    val logoUrl: String?,
    val genreId: String?,
    val censored: Boolean,
)

data class Page<T>(
    val items: List<T>,
    val totalItems: Int,
    val pageSize: Int,
) {
    val pageCount: Int get() = if (pageSize <= 0) 1 else (totalItems + pageSize - 1) / pageSize
}

/** A guide entry. Times are Unix seconds. */
data class Programme(
    val title: String,
    val description: String?,
    val startSeconds: Long,
    val stopSeconds: Long,
)
