package io.github.gobinathm.portalistv.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.gobinathm.portalistv.core.database.dao.ProfileDao
import io.github.gobinathm.portalistv.core.database.entity.ProfileEntity

/**
 * The app's only database, encrypted with SQLCipher.
 *
 * Every version bump needs a migration in [Migrations] and a test in `MigrationTest`;
 * destructive fallback is never enabled, because the database holds the user's profiles.
 */
@Database(entities = [ProfileEntity::class], version = 1, exportSchema = true)
abstract class PortalisDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        const val FILE_NAME = "portalis.db"
    }
}
