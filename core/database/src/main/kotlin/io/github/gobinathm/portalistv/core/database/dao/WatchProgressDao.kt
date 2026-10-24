package io.github.gobinathm.portalistv.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.gobinathm.portalistv.core.database.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    /** Everything a profile has watched, most recent first. */
    @Query("SELECT * FROM watch_progress WHERE profile_id = :profileId ORDER BY updated_at DESC")
    fun observe(profileId: Long): Flow<List<WatchProgressEntity>>

    /** One movie's entry, or every episode of a series. */
    @Query(
        "SELECT * FROM watch_progress WHERE profile_id = :profileId AND item_id = :itemId ORDER BY updated_at DESC",
    )
    fun observeItem(profileId: Long, itemId: String): Flow<List<WatchProgressEntity>>

    @Query(
        "SELECT * FROM watch_progress " +
            "WHERE profile_id = :profileId AND item_id = :itemId AND episode_key = :episodeKey",
    )
    suspend fun get(profileId: Long, itemId: String, episodeKey: String): WatchProgressEntity?

    @Upsert
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE profile_id = :profileId AND item_id = :itemId")
    suspend fun deleteItem(profileId: Long, itemId: String)

    @Query("DELETE FROM watch_progress WHERE profile_id = :profileId")
    suspend fun deleteAll(profileId: Long)
}
