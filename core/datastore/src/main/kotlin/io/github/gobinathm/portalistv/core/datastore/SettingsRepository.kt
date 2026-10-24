package io.github.gobinathm.portalistv.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

@Singleton
internal class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<AppSettings>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = store.data

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.updateData(transform)
    }
}
