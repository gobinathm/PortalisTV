import com.android.build.api.dsl.ApplicationExtension
import io.github.gobinathm.portalistv.buildlogic.Sdk
import io.github.gobinathm.portalistv.buildlogic.configureDetekt
import io.github.gobinathm.portalistv.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = Sdk.TARGET
            lint.checkDependencies = true
        }
        configureDetekt()
    }
}
