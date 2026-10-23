package io.github.gobinathm.portalistv.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureDetekt() {
    pluginManager.apply(libs.pluginId("detekt"))
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        parallel = true
    }
    // Detekt embeds its own Kotlin compiler; keep it from being aligned to the project's Kotlin version.
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(getSupportedKotlinVersion())
            }
        }
    }
    dependencies {
        add("detektPlugins", libs.library("detekt-formatting"))
    }
}
