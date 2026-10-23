package io.github.gobinathm.portalistv.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal object Sdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 26
}

private val JAVA_VERSION = JavaVersion.VERSION_17

internal fun Project.configureKotlinAndroid(android: CommonExtension) {
    android.apply {
        compileSdk = Sdk.COMPILE
        defaultConfig.minSdk = Sdk.MIN
        compileOptions.sourceCompatibility = JAVA_VERSION
        compileOptions.targetCompatibility = JAVA_VERSION
        lint.apply {
            warningsAsErrors = true
            abortOnError = true
            checkReleaseBuilds = true
            // Dependency freshness is Dependabot's job; failing builds on new upstream releases is not useful.
            disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        }
    }
    configureKotlin()
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
    }
    configureKotlin()
}

private fun Project.configureKotlin() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
}
