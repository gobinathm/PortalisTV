package io.github.gobinathm.portalistv.player.vlc.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.player.PlayerFactory
import io.github.gobinathm.portalistv.player.vlc.VlcPlayerFactory
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object VlcModule {
    /** The app binds [VERBOSE_PLAYER_LOGS] to its debug flag. */
    @Provides
    fun providePlayerFactory(
        @ApplicationContext context: Context,
        @Named(VERBOSE_PLAYER_LOGS) verbose: Boolean,
    ): PlayerFactory = VlcPlayerFactory(context, verbose)

    const val VERBOSE_PLAYER_LOGS = "verbosePlayerLogs"
}
