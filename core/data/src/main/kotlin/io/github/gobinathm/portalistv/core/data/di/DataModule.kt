package io.github.gobinathm.portalistv.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.catalog.DeviceCatalog
import io.github.gobinathm.portalistv.core.data.LocalProfileRepository
import io.github.gobinathm.portalistv.core.data.ProfileDefaults
import io.github.gobinathm.portalistv.core.data.ProfileRepository
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

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        fun provideDeviceCatalog(): DeviceCatalog = DeviceCatalog.load()

        @Provides
        fun provideProfileDefaults(catalog: DeviceCatalog): ProfileDefaults = ProfileDefaults(catalog)
    }
}
