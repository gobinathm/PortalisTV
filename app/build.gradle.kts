import java.util.Properties

plugins {
    alias(libs.plugins.portalis.android.application)
    alias(libs.plugins.portalis.android.compose)
    alias(libs.plugins.portalis.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release builds are signed with the builder's own key, created by scripts/create-signing-key.sh.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "io.github.gobinathm.portalistv"

    defaultConfig {
        applicationId = "io.github.gobinathm.portalistv"
        versionCode = 1
        versionName = "0.1.0"
    }

    // TVs are ARM; libVLC also ships x86 libraries that would only bloat the universal APK.
    packaging {
        jniLibs.excludes += setOf("lib/x86/**", "lib/x86_64/**")
    }

    // One APK per CPU architecture: libVLC's native code is ~40 MB per architecture, and a TV needs
    // only one. installDebug/installRelease pick the right APK; the universal one is for manual installs.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        // Android TV devices are ARM; ChromeOS is not a target.
        disable += "ChromeOsAbiSupport"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.core.security)
    implementation(projects.core.data)
    implementation(projects.feature.home)
    implementation(projects.feature.profiles)
    implementation(projects.feature.player)
    implementation(projects.feature.settings)
    implementation(projects.player.vlc)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
