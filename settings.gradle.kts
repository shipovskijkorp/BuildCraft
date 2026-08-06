import java.util.Properties

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // Stonecutter 0.8/0.9 requires Gradle 9. ForgeGradle 6 uses Gradle 8,
    // so the workspace intentionally stays on the Gradle-8-compatible line.
    id("dev.kikugie.stonecutter") version "0.7.11"
}

val targetConfiguration = Properties().apply {
    file("stonecutter-targets.properties").inputStream().use { load(it) }
}

fun requiredTargetProperty(key: String): String =
    targetConfiguration.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("Missing required property '$key' in stonecutter-targets.properties")

val registeredTargets = requiredTargetProperty("targets")
    .split(',')
    .map { it.trim() }
    .filter { it.isNotEmpty() }

stonecutter {
    // Required by the 0.7.x controller API for stonecutter.gradle.kts.
    kotlinController = true

    create(rootProject) {
        for (targetId in registeredTargets) {
            val separator = targetId.lastIndexOf('-')
            require(separator > 0 && separator < targetId.lastIndex) {
                "Target '$targetId' must end with a loader suffix, for example 1.20.1-forge"
            }
            val loader = targetId.substring(separator + 1)

            val minecraftVersion = requiredTargetProperty("target.$targetId.deps.minecraft")
            version(targetId, minecraftVersion).buildscript("build.$loader.gradle")
        }

        vcsVersion = requiredTargetProperty("vcsTarget")
    }
}

rootProject.name = "BuildCraft"
