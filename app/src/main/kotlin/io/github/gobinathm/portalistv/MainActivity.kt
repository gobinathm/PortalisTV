package io.github.gobinathm.portalistv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.gobinathm.portalistv.core.datastore.AppSettings
import io.github.gobinathm.portalistv.core.datastore.SettingsRepository
import io.github.gobinathm.portalistv.core.designsystem.theme.Accent
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.core.designsystem.theme.ThemeVariant
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The stored theme replaces the default as soon as it's read; changes apply at once.
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            PortalisTheme(
                variant = ThemeVariant.valueOf(settings.theme.name),
                accent = Accent.valueOf(settings.accent.name),
            ) {
                PortalisNavHost()
            }
        }
    }
}
