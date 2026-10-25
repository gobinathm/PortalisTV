package io.github.gobinathm.portalistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gobinathm.portalistv.core.database.entity.FavoriteChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteChannelDao {
    /** A profile's favorites, oldest first. */
    @Query("SELECT * FROM favorite_channels WHERE profile_id = :profileId ORDER BY added_at, channel_id")
    fun observe(profileId: Long): Flow<List<FavoriteChannelEntity>>

    @Query("SELECT * FROM favorite_channels WHERE profile_id = :profileId ORDER BY added_at, channel_id")
    suspend fun getAll(profileId: Long): List<FavoriteChannelEntity>

    @Query("UPDATE favorite_channels SET on_home = :onHome WHERE profile_id = :profileId AND channel_id = :channelId")
    suspend fun setOnHome(profileId: Long, channelId: String, onHome: Boolean)

    /** Keeps the original position when a favorite is added again. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE profile_id = :profileId AND channel_id = :channelId")
    suspend fun delete(profileId: Long, channelId: String)

    @Query("DELETE FROM favorite_channels WHERE profile_id = :profileId")
    suspend fun deleteAll(profileId: Long)
}
