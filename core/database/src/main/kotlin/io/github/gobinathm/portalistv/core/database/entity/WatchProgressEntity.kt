package io.github.gobinathm.portalistv.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.gobinathm.portalistv.core.model.VodPlayback
import io.github.gobinathm.portalistv.core.model.WatchProgress

/**
 * How far a movie or one episode has been watched, per profile. [episodeKey] is the episode id,
 * or "" for a movie, so each episode keeps its own position.
 */
@Entity(
    tableName = "watch_progress",
    primaryKeys = ["profile_id", "item_id", "episode_key"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profile_id", "updated_at"])],
)
data class WatchProgressEntity(
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "episode_key") val episodeKey: String,
    @ColumnInfo(name = "season_id") val seasonId: String?,
    @ColumnInfo(name = "playback_ref") val playbackRef: String?,
    val title: String,
    val subtitle: String?,
    @ColumnInfo(name = "poster_url") val posterUrl: String?,
    @ColumnInfo(name = "is_series") val isSeries: Boolean,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAtMillis: Long,
)

fun WatchProgressEntity.toModel() = WatchProgress(
    playback = VodPlayback(itemId, seasonId, episodeKey.ifEmpty { null }, playbackRef),
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    isSeries = isSeries,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAtMillis = updatedAtMillis,
)

fun WatchProgress.toEntity(profileId: Long) = WatchProgressEntity(
    profileId = profileId,
    itemId = playback.itemId,
    episodeKey = playback.episodeId.orEmpty(),
    seasonId = playback.seasonId,
    playbackRef = playback.playbackRef,
    title = title,
    subtitle = subtitle,
    posterUrl = posterUrl,
    isSeries = isSeries,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAtMillis = updatedAtMillis,
)
