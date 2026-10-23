plugins {
    alias(libs.plugins.portalis.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
}
