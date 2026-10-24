plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.android.compose)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.player"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    // Compose UI is part of this module's API (VideoSurface), so consumers need the BOM's version too.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
}
