plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.datastore"
}

dependencies {
    api(projects.core.player)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.common)
    implementation(projects.core.security)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.tink.android)
}
