plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.android.compose)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.player"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.androidx.compose.ui)
}
