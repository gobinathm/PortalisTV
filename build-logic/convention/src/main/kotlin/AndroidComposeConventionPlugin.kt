import com.android.build.api.dsl.CommonExtension
import io.github.gobinathm.portalistv.buildlogic.libs
import io.github.gobinathm.portalistv.buildlogic.library
import io.github.gobinathm.portalistv.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Enables Compose on an Android module. Apply after the application or library convention. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-compose"))
        extensions.getByType<CommonExtension>().buildFeatures.compose = true
        // Model classes come from a plain Kotlin module; without this, Compose can't skip rows whose data is equal.
        extensions.getByType<ComposeCompilerGradlePluginExtension>().stabilityConfigurationFiles
            .add(rootProject.layout.projectDirectory.file("config/compose-stability.conf"))
        dependencies {
            val bom = libs.library("androidx-compose-bom")
            add("implementation", platform(bom))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        }
    }
}
