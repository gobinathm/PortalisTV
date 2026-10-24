package io.github.gobinathm.portalistv.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.data.Clock
import io.github.gobinathm.portalistv.core.data.FavoritesRepository
import io.github.gobinathm.portalistv.core.data.LocalFavoritesRepository
import io.github.gobinathm.portalistv.core.data.LocalProfileRepository
import io.github.gobinathm.portalistv.core.data.ProfileDefaults
import io.github.gobinathm.portalistv.core.data.ProfileRepository
import io.github.gobinathm.portalistv.core.data.source.PortalConfig
import io.github.gobinathm.portalistv.core.network.HttpClients
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindProfileRepository(repository: LocalProfileRepository): ProfileRepository

    @Binds
    abstract fun bindFavoritesRepository(repository: LocalFavoritesRepository): FavoritesRepository

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        fun provideClock(): Clock = Clock(System::currentTimeMillis)

        @Provides
        @Singleton
        fun provideDeviceCatalog(): DeviceCatalog = DeviceCatalog.load()

        @Provides
        fun provideProfileDefaults(catalog: DeviceCatalog): ProfileDefaults = ProfileDefaults(catalog)

        /** One client stack for the app, so all portals share a connection pool and dispatcher. */
        @Provides
        @Singleton
        fun provideHttpClients(): HttpClients = HttpClients()

        @Provides
        fun providePortalConfig(catalog: DeviceCatalog): PortalConfig = PortalConfig(catalog)
    }
}
