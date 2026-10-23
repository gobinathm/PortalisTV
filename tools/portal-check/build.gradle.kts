plugins {
    alias(libs.plugins.portalis.jvm.library)
    application
}

application {
    mainClass = "io.github.gobinathm.portalistv.tools.portalcheck.MainKt"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.catalog)
    implementation(projects.core.stalker)
    implementation(libs.kotlinx.coroutines.core)
}

// Relative paths in the arguments resolve against the repository root, not this module.
tasks.named<JavaExec>("run") {
    workingDir = rootDir
}
