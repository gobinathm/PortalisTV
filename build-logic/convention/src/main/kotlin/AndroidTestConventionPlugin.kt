import com.android.build.api.dsl.TestExtension
import io.github.gobinathm.portalistv.buildlogic.Sdk
import io.github.gobinathm.portalistv.buildlogic.configureDetekt
import io.github.gobinathm.portalistv.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** A test-only module that drives another module's APK, such as the benchmarks. */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")
        extensions.configure<TestExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = Sdk.TARGET
        }
        configureDetekt()
    }
}
