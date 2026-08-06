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
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    create(rootProject) {
        /**
         * Registers one Minecraft version for one or more loaders.
         *
         * Each node is generated under versions/{minecraftVersion}-{loader}
         * and uses build.{loader}.gradle as its platform build script.
         */
        fun target(minecraftVersion: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$minecraftVersion-$loader", minecraftVersion)
                    .buildscript("build.$loader.gradle")
            }
        }

        target("1.19.2", "forge")
        target("1.20.1", "forge")

        // Future examples:
        // target("1.21.1", "forge", "neoforge")
        // target("1.20.1", "fabric")

        vcsVersion = "1.19.2-forge"
    }
}

rootProject.name = "BuildCraft"
