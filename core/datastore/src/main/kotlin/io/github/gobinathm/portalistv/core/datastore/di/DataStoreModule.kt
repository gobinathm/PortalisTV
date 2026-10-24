package io.github.gobinathm.portalistv.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.common.log.Log
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.DataStoreSettingsRepository
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.datastore.SettingsSerializer
import io.github.gobinathm.portalistv.core.security.KeyManager
import io.github.gobinathm.portalistv.core.security.KeyPurpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {
    @Binds
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    companion object {
        private const val TAG = "Settings"
        private const val FILE_NAME = "app_settings.bin"

        @Provides
        @Singleton
        fun provideSettingsStore(
            @ApplicationContext context: Context,
            keyManager: KeyManager,
        ): DataStore<AppSettings> = DataStoreFactory.create(
            serializer = SettingsSerializer { keyManager.cipher(KeyPurpose.SETTINGS) },
            corruptionHandler = ReplaceFileCorruptionHandler { e ->
                // Lost keys (e.g. after a restore) or a damaged file: start over with defaults.
                Log.w(TAG, e) { "Settings unreadable; using defaults" }
                AppSettings()
            },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.dataStoreFile(FILE_NAME) },
        )
    }
}
