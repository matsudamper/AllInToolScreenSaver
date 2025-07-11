// Top-level build file where you can add configuration options common to all sub-projects/modules.
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.ktlintGradle) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    val detektPlugins by configurations
    val ktlintRuleset by configurations
    configure<DetektExtension> {
        parallel = true
        config.setFrom(
            "$rootDir/.detekt/default.yml",
            "$rootDir/.detekt/compose.yml",
            "$rootDir/.detekt/custom.yml",
        )
    }
    dependencies {
        detektPlugins(rootProject.libs.kotlinCompilerWrapper)
        detektPlugins(rootProject.libs.detekt)
        detektPlugins(rootProject.projects.costomDetektRules)

        ktlintRuleset(rootProject.projects.ktlintCustomRules)
    }

    configure<KtlintExtension> {
        verbose.set(true)
        version.set(
            provider {
                libs.versions.ktlint.get()
            },
        )
        filter {
            val excludePathList = listOf(
                "generated",
                "build",
            ).map { "${File.separator}$it${File.separator}" }
            exclude {
                excludePathList.any { path -> it.file.path.contains(path) }
            }
        }
    }
}
