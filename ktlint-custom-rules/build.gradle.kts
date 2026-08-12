plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktlintGradle)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    implementation(libs.ktlintRuleEngine)
    implementation(libs.ktlintCliRulesetCore)
    implementation(libs.ktlintLogger)
}
