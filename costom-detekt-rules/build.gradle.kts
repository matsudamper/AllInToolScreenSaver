plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.detekt)
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")
    compileOnly("io.gitlab.arturbosch.detekt:detekt-psi-utils:1.23.8")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation("junit:junit:4.13.2")
}
