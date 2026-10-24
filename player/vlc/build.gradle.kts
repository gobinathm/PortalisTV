plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.android.compose)
    alias(libs.plugins.portalis.hilt)
}

android {
    namespace = "io.github.gobinathm.portalistv.player.vlc"
}

dependencies {
    api(projects.core.player)
    implementation(projects.core.common)
    implementation(libs.androidx.core.ktx)
    // libVLC is LGPL-2.1, but this prebuilt AAR also contains GPL components (x264, libpostproc);
    // see THIRD_PARTY_NOTICES.md.
    implementation(libs.libvlc)
}
