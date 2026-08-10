import java.util.Properties

plugins {
    id("dev.kikugie.stonecutter")
}

val targetConfiguration = Properties().apply {
    rootProject.file("targets.properties").inputStream().use { load(it) }
}

stonecutter active "1.21.1-neoforge" /* [SC] DO NOT EDIT */

stonecutter {
    parameters {
        val target = node.metadata.project
        val loader = target.substringAfterLast('-')
        constants += listOf(
            "forge" to (loader == "forge"),
            "neoforge" to (loader == "neoforge"),
            "fabric" to (loader == "fabric"),
            "legacy" to false,
            "modern" to true,
        )
    }
}

val targets = targetConfiguration.getProperty("targets")
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
    ?: error("Missing non-empty targets in targets.properties")

tasks.register("buildAndCollect") {
    group = "build"
    description = "Build every modern BuildCraft target and collect release jars"
    dependsOn(targets.map { target -> ":$target:buildAndCollect" })
}

val activeProject = stonecutter.current!!.project
fun activeTask(name: String) = ":$activeProject:$name"

tasks.register("runActiveClient") {
    group = "stonecutter"
    dependsOn(activeTask("runClient"))
}

tasks.register("runActiveServer") {
    group = "stonecutter"
    dependsOn(activeTask("runServer"))
}

tasks.register("runActiveGameTests") {
    group = "verification"
    dependsOn(activeTask("runGameTestServer"))
}
