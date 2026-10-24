package io.github.gobinathm.portalistv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.BuildConfig
import io.github.gobinathm.portalistv.player.vlc.di.VlcModule
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** The player engine logs its own (unredacted) messages only in debug builds. */
    @Provides
    @Named(VlcModule.VERBOSE_PLAYER_LOGS)
    fun provideVerbosePlayerLogs(): Boolean = BuildConfig.DEBUG
}
