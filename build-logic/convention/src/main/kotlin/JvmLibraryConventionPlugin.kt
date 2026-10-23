import io.github.gobinathm.portalistv.buildlogic.configureDetekt
import io.github.gobinathm.portalistv.buildlogic.configureKotlinJvm
import io.github.gobinathm.portalistv.buildlogic.libs
import io.github.gobinathm.portalistv.buildlogic.library
import io.github.gobinathm.portalistv.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A pure-Kotlin module with no Android dependencies. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-jvm"))
        // Lets Android Lint analyze pure-Kotlin modules too.
        pluginManager.apply("com.android.lint")
        configureKotlinJvm()
        configureDetekt()
        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("truth"))
        }
    }
}
