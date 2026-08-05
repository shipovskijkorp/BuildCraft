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

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        version("1.19.2-forge", "1.19.2")
        version("1.20.1-forge", "1.20.1")
        version("1.21.1-forge", "1.21.1")
        version("1.21.1-neoforge", "1.21.1")
    }

    create(rootProject)
}

rootProject.name = "buildcraft-community-edition-localizations"