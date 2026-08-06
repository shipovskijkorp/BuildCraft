import java.util.Properties

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.19.2-forge" /* [SC] DO NOT EDIT */

stonecutter {
    parameters {
        val loader = node.metadata.project.substringAfterLast('-')

        // Keeps future shared-source loader conditions available on the
        // Gradle-8-compatible Stonecutter API.
        constants += listOf(
            "forge" to (loader == "forge"),
            "neoforge" to (loader == "neoforge"),
            "fabric" to (loader == "fabric"),
        )
    }
}

// Stonecutter 0.7.x does not expose the registerChiseled/chiseled API used by
// Stonecutter 0.8/0.9. Keep the aggregate task version-independent by wiring
// it to each generated node with normal Gradle task paths.
val targetConfiguration = Properties().apply {
    rootProject.file("stonecutter-targets.properties").inputStream().use { load(it) }
}
val registeredTargets = targetConfiguration.getProperty("targets")
    ?.split(',')
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: error("Missing non-empty 'targets' in stonecutter-targets.properties")

tasks.register("buildAndCollect") {
    group = "build"
    description = "Build every registered Stonecutter target and collect release jars"
    dependsOn(registeredTargets.map { target -> ":$target:buildAndCollect" })
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

if (activeProject.substringAfterLast('-') == "forge") {
    tasks.register("runActiveGameTests") {
        group = "verification"
        description = "Run Forge GameTests for the active Stonecutter target"
        dependsOn(activeTask("runGameTestServer"))
    }
}
