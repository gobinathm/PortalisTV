package io.github.gobinathm.portalistv.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.database.DatabaseKey
import io.github.gobinathm.portalistv.core.database.Migrations
import io.github.gobinathm.portalistv.core.database.PortalisDatabase
import io.github.gobinathm.portalistv.core.database.dao.ProfileDao
import io.github.gobinathm.portalistv.core.security.KeyManager
import io.github.gobinathm.portalistv.core.security.KeyPurpose
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    @Suppress("SpreadOperator") // One small array copy, once per process.
    fun provideDatabase(@ApplicationContext context: Context, keyManager: KeyManager): PortalisDatabase {
        System.loadLibrary("sqlcipher")
        // SQLCipher's own logging goes straight to logcat, bypassing the redacting logger.
        Logger.setTarget(NoopTarget())
        val databaseFile = context.getDatabasePath(PortalisDatabase.FILE_NAME)
        val key = DatabaseKey(
            // noBackupFilesDir: the key is device-bound and must never be restored elsewhere.
            keyFile = File(context.noBackupFilesDir, "database.key"),
            databaseFile = databaseFile,
            cipher = keyManager.cipher(KeyPurpose.DATABASE),
        )
        // The factory keeps the key for the database's lifetime, since it may need to reopen it.
        return Room.databaseBuilder(context, PortalisDatabase::class.java, databaseFile.absolutePath)
            .openHelperFactory(SupportOpenHelperFactory(key.rawKeySpec()))
            .addMigrations(*Migrations.ALL)
            .build()
    }

    @Provides
    fun provideProfileDao(database: PortalisDatabase): ProfileDao = database.profileDao()
}
