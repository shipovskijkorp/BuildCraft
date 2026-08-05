import groovy.json.JsonSlurper
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
}

val target = project.projectDir.name
val minecraftVersion = target.substringBeforeLast('-')
val loader = target.substringAfterLast('-')
val modId = providers.gradleProperty("mod_id").get()
val modName = providers.gradleProperty("mod_name").get()
val modVersion = providers.gradleProperty("mod_version").get()

version = modVersion
group = providers.gradleProperty("mod_group").get()

base {
    archivesName.set("buildcraft-community-edition-localizations")
}

sourceSets {
    main {
        resources {
            setSrcDirs(listOf(
                rootProject.file("src/main/resources"),
                rootProject.file("versions/$target/src/main/resources")
            ))
        }
    }
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    val replacements = mapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "minecraft_version" to minecraftVersion,
        "loader" to loader
    )
    inputs.properties(replacements)
    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(replacements)
    }
}

tasks.withType<Jar>().configureEach {
    archiveFileName.set("$modId-$modVersion+$target.jar")
    manifest {
        attributes(
            "Specification-Title" to modName,
            "Specification-Vendor" to "CurativeTree, ShipovskijKorp",
            "Specification-Version" to modVersion,
            "Implementation-Title" to modName,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to "CurativeTree, ShipovskijKorp"
        )
    }
}

val verifyLocalizations by tasks.registering {
    group = "verification"
    description = "Validates localization JSON and ensures English remains in the main BuildCraft mod."

    val commonResources = rootProject.file("src/main/resources")
    inputs.dir(commonResources)

    doLast {
        val langDir = commonResources.resolve("assets/buildcraft/lang")
        val guideTextDir = commonResources.resolve("assets/buildcraft/guide/text")
        val ordinary = langDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()
        val guide = guideTextDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()

        require(ordinary.size == 30) { "Expected 30 ordinary BuildCraft localization files, found ${ordinary.size}" }
        require(ordinary.none { it.name == "en_us.json" }) {
            "The localization addon must not package assets/buildcraft/lang/en_us.json"
        }
        require(guide.size == 1 && guide.single().name == "ru_ru.json") { "Expected the Russian Guide Book text pack" }
        require(guide.none { it.name == "en_us.json" }) {
            "The localization addon must not package the English Guide Book text pack"
        }

        fun validateStringObject(file: File) {
            val parsed = JsonSlurper().parse(file)
            require(parsed is Map<*, *>) { "${file.relativeTo(rootProject.projectDir)} must contain a JSON object" }
            parsed.forEach { (key, value) ->
                require(key is String && value is String) {
                    "${file.relativeTo(rootProject.projectDir)} must contain only string-to-string translations"
                }
            }
        }

        ordinary.forEach(::validateStringObject)

        guide.forEach { file ->
            val parsed = JsonSlurper().parse(file)
            require(parsed is Map<*, *>) { "${file.relativeTo(rootProject.projectDir)} must contain a JSON object" }
            require(parsed["format"] == 1) { "${file.name} has an unsupported Guide Book text-pack format" }
            val pages = parsed["pages"]
            require(pages is Map<*, *> && pages.size == 207) { "${file.name} must contain all 207 Guide Book pages" }
            pages.forEach { (page, segments) ->
                require(page is String && segments is List<*>) { "Invalid Guide Book page entry in ${file.name}" }
                require(segments.isNotEmpty() && segments.all { it is String && it.isNotBlank() }) {
                    "Guide Book page $page in ${file.name} contains blank or non-string segments"
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyLocalizations)
}
