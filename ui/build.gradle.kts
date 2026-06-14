plugins {
    id("com.android.library")
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "net.matsudamper.allintoolscreensaver.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.androidxUi)
    implementation(libs.androidxUiGraphics)
    implementation(libs.androidxUiToolingPreview)
    implementation(libs.androidxMaterial3)
    implementation(libs.androidxFoundationPager)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxNavigation3Ui)
    implementation(libs.androidxNavigation3Runtime)
    implementation(libs.androidxLifecycleViewmodelNavigation3)
    implementation(libs.androidxMaterial3AdaptiveNavigation3)
    implementation(libs.coilCompose)
    implementation(libs.haze)

    debugImplementation(libs.androidxUiTooling)
    debugImplementation(libs.androidxUiTestManifest)
}
