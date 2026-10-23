package io.github.gobinathm.portalistv.capture

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gobinathm.portalistv.core.stalker.ExchangeRecorder
import java.io.File
import javax.inject.Singleton

/** Debug builds capture redacted portal replies, so they can be reviewed and turned into fixtures. */
@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {
    @Provides
    @Singleton
    fun provideRecorder(@ApplicationContext context: Context): ExchangeRecorder {
        val dir = context.getExternalFilesDir("captures") ?: File(context.filesDir, "captures")
        return FileCaptureRecorder(dir)
    }
}
