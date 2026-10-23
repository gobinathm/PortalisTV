package io.github.gobinathm.portalistv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import io.github.gobinathm.portalistv.core.designsystem.theme.PortalisTheme
import io.github.gobinathm.portalistv.feature.home.HomeScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PortalisTheme {
                HomeScreen()
            }
        }
    }
}
