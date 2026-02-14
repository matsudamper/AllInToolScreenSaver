plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    id("com.google.protobuf") version "0.9.6"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
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
        artifact = "com.google.protobuf:protoc:4.33.5"
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
    implementation(project(":ui"))
    implementation(platform(libs.androidxComposeBom))
    implementation(libs.androidxUi)
    implementation(libs.androidxUiGraphics)
    implementation(libs.androidxUiToolingPreview)
    implementation(libs.androidxMaterial3)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxDocumentfile)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.androidxDatastore)
    implementation(libs.haze)

    implementation(libs.protobufKotlinLite)
    implementation(libs.koinAndroid)
    implementation(libs.koinAndroidxCompose)

    implementation(libs.androidxNavigation3Ui)
    implementation(libs.androidxNavigation3Runtime)
    implementation(libs.androidxLifecycleViewmodelNavigation3)
    implementation(libs.kotlinxSerializationCore)

    implementation(libs.androidxMaterial3AdaptiveNavigation3)

    implementation(libs.mlkitFaceDetection)

    androidTestImplementation(platform(libs.androidxComposeBom))
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
    androidTestImplementation(libs.androidxUiTestJunit4)
    androidTestImplementation(libs.coroutinesTest)
    androidTestImplementation(libs.koinTest)

    debugImplementation(libs.androidxUiTooling)
    debugImplementation(libs.androidxUiTestManifest)
}
