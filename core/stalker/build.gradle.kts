plugins {
    alias(libs.plugins.portalis.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.network)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.common)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
