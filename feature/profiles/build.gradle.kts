plugins {
    alias(libs.plugins.portalis.android.feature)
}

android {
    namespace = "io.github.gobinathm.portalistv.feature.profiles"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.security)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(testFixtures(projects.core.model))
    testImplementation(libs.kotlinx.coroutines.test)
}
