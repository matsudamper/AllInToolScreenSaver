plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktlintGradle)
}

dependencies {
    implementation(libs.ktlintRuleEngine)
    implementation(libs.ktlintCliRulesetCore)
    implementation(libs.ktlintLogger)
}
