import java.util.Properties

plugins {
    id("dev.kikugie.stonecutter")
}

val targetConfiguration = Properties().apply {
    rootProject.file("stonecutter-targets.properties").inputStream().use { load(it) }
}

stonecutter active "1.19.2-forge" /* [SC] DO NOT EDIT */

stonecutter {
    parameters {
        val target = node.metadata.project
        val loader = target.substringAfterLast('-')
        val family = targetConfiguration.getProperty("target.$target.source.family")?.trim()
            ?: error("Missing source.family for $target")

        // Keep target metadata available to Stonecutter/controller tooling.
        // Maintained source-family files are compiled as layered source sets;
        // version differences should therefore live in overlays/adapters rather
        // than growing conditional blocks throughout gameplay code.
        constants += listOf(
            "forge" to (loader == "forge"),
            "neoforge" to (loader == "neoforge"),
            "fabric" to (loader == "fabric"),
            "legacy" to (family == "legacy"),
            "modern" to (family == "modern"),
        )
    }
}

// Stonecutter 0.7.x does not expose the registerChiseled/chiseled API used by
// Stonecutter 0.8/0.9. Keep the aggregate task version-independent by wiring
// it to each generated node with normal Gradle task paths.
val registeredTargets = targetConfiguration.getProperty("targets")
    ?.split(',')
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: error("Missing non-empty 'targets' in stonecutter-targets.properties")

val registeredFamilies = targetConfiguration.getProperty("sourceFamilies")
    ?.split(',')
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: error("Missing non-empty 'sourceFamilies' in stonecutter-targets.properties")

val targetsByFamily = registeredFamilies.associateWith { family ->
    registeredTargets.filter { target ->
        targetConfiguration.getProperty("target.$target.source.family")?.trim() == family
    }
}

tasks.register("buildAndCollect") {
    group = "build"
    description = "Build every registered Stonecutter target and collect release jars"
    dependsOn(registeredTargets.map { target -> ":$target:buildAndCollect" })
}

for ((family, targets) in targetsByFamily) {
    tasks.register("build${family.replaceFirstChar { it.uppercase() }}") {
        group = "build"
        description = "Build the $family BuildCraft source family"
        dependsOn(targets.map { target -> ":$target:buildAndCollect" })
    }
}

// Stable convenience tasks that follow whichever target is active.
val activeProject = stonecutter.current!!.project

fun activeTask(name: String) = ":$activeProject:$name"

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

if (activeProject.substringAfterLast('-') in setOf("forge", "neoforge")) {
    tasks.register("runActiveGameTests") {
        group = "verification"
        description = "Run GameTests for the active Stonecutter target"
        dependsOn(activeTask("runGameTestServer"))
    }
}
