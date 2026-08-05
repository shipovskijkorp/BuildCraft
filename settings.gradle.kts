import dev.kikugie.stonecutter.StonecutterSettings

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.4"
}

extensions.configure<StonecutterSettings> {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        vers("1.19.2-forge", "1.19.2")
        vers("1.20.1-forge", "1.20.1")
        vers("1.21.1-forge", "1.21.1")
        vers("1.21.1-neoforge", "1.21.1")
        vcsVersion = "1.19.2-forge"
    }
}

rootProject.name = "buildcraft-community-edition-localizations"
