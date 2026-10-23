plugins {
    alias(libs.plugins.portalis.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass = "io.github.gobinathm.portalistv.tools.mockportal.MainKt"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // End-to-end test: the app's real client against this portal.
    testImplementation(projects.core.stalker)
    testImplementation(libs.kotlinx.coroutines.test)
}
