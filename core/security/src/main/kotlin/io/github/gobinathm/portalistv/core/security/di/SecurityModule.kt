package io.github.gobinathm.portalistv.core.security.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.security.KeyManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    @Provides
    @Singleton
    fun provideKeyManager(@ApplicationContext context: Context): KeyManager = KeyManager(context)
}
