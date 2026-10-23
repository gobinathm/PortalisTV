package io.github.gobinathm.portalistv.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.gobinathm.portalistv.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY sort_order, id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun get(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): ProfileEntity?

    /** @return the new row id. Fails if a profile with the same uuid exists. */
    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    /** @return the number of rows updated: 0 if the profile no longer exists. */
    @Update
    suspend fun update(profile: ProfileEntity): Int

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: Long)
}
