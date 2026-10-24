plugins {
    alias(libs.plugins.portalis.android.feature)
}

android {
    namespace = "io.github.gobinathm.portalistv.feature.settings"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.data)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
}
