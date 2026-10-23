package io.github.gobinathm.portalistv.capture

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder

/** Release builds never record portal traffic. */
@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {
    @Provides
    fun provideRecorder(): ExchangeRecorder = ExchangeRecorder.NONE
}
