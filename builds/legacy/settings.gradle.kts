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
    id("dev.kikugie.stonecutter") version "0.7.11"
}

val repositoryRoot = file("../..").canonicalFile
val commonConfiguration = Properties().apply {
    repositoryRoot.resolve("build-config/common.properties").inputStream().use { load(it) }
}
val targetConfiguration = Properties().apply {
    file("targets.properties").inputStream().use { load(it) }
}

fun property(key: String): String =
    targetConfiguration.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: commonConfiguration.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("Missing required property '$key' in legacy target configuration")

val targets = property("targets").split(',').map(String::trim).filter(String::isNotEmpty)

stonecutter {
    kotlinController = true
    create(rootProject) {
        for (targetId in targets) {
            val loader = targetId.substringAfterLast('-')
            version(targetId, property("target.$targetId.deps.minecraft"))
                .buildscript("build.$loader.gradle")
        }
        vcsVersion = property("vcsTarget")
    }
}

rootProject.name = "BuildCraft-Legacy"
