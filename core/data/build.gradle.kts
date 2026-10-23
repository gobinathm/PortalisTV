plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.hilt)
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

    testImplementation(testFixtures(projects.core.model))
}
