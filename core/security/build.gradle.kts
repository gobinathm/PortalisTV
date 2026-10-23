plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.hilt)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.security"
}

dependencies {
    api(libs.tink.android)
    implementation(projects.core.common)
}
