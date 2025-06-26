plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "net.matsudamper.allintoolscreensaver"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.matsudamper.allintoolscreensaver"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    testOptions {
        @Suppress("UnstableApiUsage")
        managedDevices {
            allDevices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel9api35").apply {
                    device = "Pixel 9"
                    apiLevel = 35
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.8"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxUi)
    implementation(libs.androidxUiGraphics)
    implementation(libs.androidxUiToolingPreview)
    implementation(libs.androidxMaterial3)
    implementation(libs.androidxFoundationPager)
    implementation(libs.androidxDocumentfile)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.androidxDatastore)

    implementation(libs.coilCompose)
    implementation(libs.protobufKotlinLite)
    implementation(libs.koinAndroid)
    implementation(libs.koinAndroidxCompose)
    implementation(libs.haze)

    // Navigation 3 libraries
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.core)

    androidTestImplementation(libs.coroutinesTest)

    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation(libs.androidxUiTestJunit4)
    androidTestImplementation(libs.coroutinesTest)
    androidTestImplementation(libs.koinTest)

    debugImplementation(libs.androidxUiTooling)
    debugImplementation(libs.androidxUiTestManifest)
}
