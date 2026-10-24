package io.github.gobinathm.portalistv.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.github.gobinathm.portalistv.core.model.LiveChannel

/**
 * A live channel the user marked as a favorite, per profile. The whole channel is kept so the
 * favorites list opens and zaps without asking the portal for every category.
 */
@Entity(
    tableName = "favorite_channels",
    primaryKeys = ["profile_id", "channel_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profile_id", "added_at"])],
)
data class FavoriteChannelEntity(
    @ColumnInfo(name = "profile_id") val profileId: Long,
    @ColumnInfo(name = "channel_id") val channelId: String,
    val name: String,
    val number: Int?,
    @ColumnInfo(name = "logo_url") val logoUrl: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    val adult: Boolean,
    @ColumnInfo(name = "playback_ref") val playbackRef: String,
    @ColumnInfo(name = "added_at") val addedAtMillis: Long,
)

fun FavoriteChannelEntity.toModel() = LiveChannel(
    id = channelId,
    name = name,
    number = number,
    logoUrl = logoUrl,
    categoryId = categoryId,
    adult = adult,
    playbackRef = playbackRef,
)

fun LiveChannel.toFavoriteEntity(profileId: Long, addedAtMillis: Long) = FavoriteChannelEntity(
    profileId = profileId,
    channelId = id,
    name = name,
    number = number,
    logoUrl = logoUrl,
    categoryId = categoryId,
    adult = adult,
    playbackRef = playbackRef,
    addedAtMillis = addedAtMillis,
)
