plugins {
    alias(libs.plugins.portalis.jvm.library)
    alias(libs.plugins.kotlin.serialization)
    application
}

/**
 * Makes channel logos for your own build of the app: see the README section "Channel logos". The
 * output, local-logos/, is git-ignored; logos are their owners' trademarks and never committed.
 */
application {
    mainClass = "io.github.gobinathm.portalistv.tools.logopack.MainKt"
}

dependencies {
    implementation(projects.tools.portalCheck)
    implementation(projects.core.stalker)
    implementation(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

// Relative paths in the arguments, and the output folder, resolve against the repository root.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
