plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.catalog)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.security)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    api(projects.core.stalker)
    implementation(libs.kotlinx.serialization.json)
    api(libs.coil.core)
    implementation(libs.coil.network.okhttp)

    testImplementation(testFixtures(projects.core.model))
    testImplementation(libs.kotlinx.coroutines.test)
}
