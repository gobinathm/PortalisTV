package io.github.gobinathm.portalistv.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.gobinathm.portalistv.core.database.dao.FavoriteChannelDao
import io.github.gobinathm.portalistv.core.database.dao.ProfileDao
import io.github.gobinathm.portalistv.core.database.dao.WatchProgressDao
import io.github.gobinathm.portalistv.core.database.entity.FavoriteChannelEntity
import io.github.gobinathm.portalistv.core.database.entity.ProfileEntity
import io.github.gobinathm.portalistv.core.database.entity.WatchProgressEntity

/**
 * The app's only database, encrypted with SQLCipher.
 *
 * Every version bump needs a migration (an [AutoMigration] here, or a manual one in
 * [Migrations]), checked by `MigrationTest`; destructive fallback is never enabled, because the
 * database holds the user's profiles.
 *
 * Versions: 1 profiles; 2 favorite channels; 3 watch progress; 4 favorites shown on Home.
 */
@Database(
    entities = [ProfileEntity::class, FavoriteChannelEntity::class, WatchProgressEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class PortalisDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun favoriteChannelDao(): FavoriteChannelDao

    abstract fun watchProgressDao(): WatchProgressDao

    companion object {
        const val FILE_NAME = "portalis.db"
    }
}
