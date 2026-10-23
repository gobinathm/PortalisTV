plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.android.compose)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.designsystem"
}

dependencies {
    api(libs.androidx.tv.material)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
}
