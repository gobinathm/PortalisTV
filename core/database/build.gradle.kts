plugins {
    alias(libs.plugins.portalis.android.library)
    alias(libs.plugins.portalis.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.gobinathm.portalistv.core.database"
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

room {
    // Exported schemas are committed; migration tests read them.
    schemaDirectory("$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        variant.androidTest?.sources?.assets?.addStaticSourceDirectory("$projectDir/schemas")
    }
}

dependencies {
    api(projects.core.model)
    api(libs.room.runtime)
    implementation(projects.core.common)
    implementation(projects.core.security)
    implementation(libs.sqlcipher.android)
    ksp(libs.room.compiler)

    testImplementation(testFixtures(projects.core.model))
    testImplementation(libs.tink.android)

    androidTestImplementation(testFixtures(projects.core.model))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
