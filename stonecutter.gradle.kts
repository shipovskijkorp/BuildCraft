@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
    id("dev.kikugie.stonecutter")

    // Access Transformers must be present before ForgeGradle is loaded.
    id("net.minecraftforge.accesstransformers") version "5.0.3" apply false
    id("net.minecraftforge.gradle") version "7.0.32" apply false
    id("net.minecraftforge.renamer") version "1.1.7" apply false
}

stonecutter active "1.19.2-forge" /* [SC] DO NOT EDIT */

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Applies shared, version-specific and loader/version-specific tables from
    // stonecutter.properties.toml to generated projects.
    properties {
        tags(version, loader)
    }

    // Enables conditions such as `//? if forge {` in shared source files.
    constants {
        match(loader, "forge", "neoforge", "fabric")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
    swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
    swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}

// Runs buildAndCollect in every registered version/loader node.
stonecutter registerChiseled tasks.register("buildAndCollect", stonecutter.chiseled) {
    group = "build"
    description = "Build every registered Stonecutter target and collect release jars"
    ofTask("buildAndCollect")
}

// Stable convenience tasks that follow whichever target is active.
val activeProject = stonecutter.current!!.project

fun activeTask(name: String) = "$activeProject:$name"

tasks.register("runActiveClient") {
    group = "stonecutter"
    description = "Run the client for the active Stonecutter target"
    dependsOn(activeTask("runClient"))
}

tasks.register("runActiveServer") {
    group = "stonecutter"
    description = "Run the dedicated server for the active Stonecutter target"
    dependsOn(activeTask("runServer"))
}

if (activeProject.substringAfterLast('-') == "forge") {
    tasks.register("runActiveGameTests") {
        group = "verification"
        description = "Run Forge GameTests for the active Stonecutter target"
        dependsOn(activeTask("runGameTestServer"))
    }
}
