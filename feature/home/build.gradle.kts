plugins {
    alias(libs.plugins.portalis.android.feature)
}

android {
    namespace = "io.github.gobinathm.portalistv.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.datastore)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(testFixtures(projects.core.model))
    testImplementation(libs.kotlinx.coroutines.test)
}
