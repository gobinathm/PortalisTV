import com.android.build.api.dsl.LibraryExtension
import io.github.gobinathm.portalistv.buildlogic.configureDetekt
import io.github.gobinathm.portalistv.buildlogic.configureKotlinAndroid
import io.github.gobinathm.portalistv.buildlogic.libs
import io.github.gobinathm.portalistv.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }
        configureDetekt()
        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("truth"))
        }
    }
}
