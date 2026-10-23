import io.github.gobinathm.portalistv.buildlogic.libs
import io.github.gobinathm.portalistv.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A screen-level module: Android library + Compose + Hilt + the design system. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("portalis.android.library")
        pluginManager.apply("portalis.android.compose")
        pluginManager.apply("portalis.hilt")
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.library("androidx-tv-material"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
        }
    }
}
