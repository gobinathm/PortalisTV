plugins {
    alias(libs.plugins.portalis.android.test)
}

/**
 * Macrobenchmarks for the performance budget in docs/ARCHITECTURE.md. They run on a real TV
 * against the mock portal; see benchmark/README.md.
 */
android {
    // Not ".benchmark": that is the package of the app build under test.
    namespace = "io.github.gobinathm.portalistv.macrobenchmark"
    targetProjectPath = ":app"
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // Matches the app's benchmark build type: release code with a debug signature.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
}

// Only the benchmark variant makes sense; it targets the app's benchmark build.
androidComponents {
    beforeVariants { it.enable = it.buildType == "benchmark" }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.ext.junit)
}
