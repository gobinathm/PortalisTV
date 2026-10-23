import com.android.build.api.dsl.CommonExtension
import io.github.gobinathm.portalistv.buildlogic.libs
import io.github.gobinathm.portalistv.buildlogic.library
import io.github.gobinathm.portalistv.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Enables Compose on an Android module. Apply after the application or library convention. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-compose"))
        extensions.getByType<CommonExtension>().buildFeatures.compose = true
        dependencies {
            val bom = libs.library("androidx-compose-bom")
            add("implementation", platform(bom))
            add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
        }
    }
}
