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
    description = "Validates ordinary and Guide Book localization packs bundled by the add-on."

    val commonResources = rootProject.file("src/main/resources")
    inputs.dir(commonResources)

    doLast {
        val langDir = commonResources.resolve("assets/buildcraft/lang")
        val guideTextDir = commonResources.resolve("assets/buildcraft/guide/text")
        val ordinary = langDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()
        val guide = guideTextDir.listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()

        require(ordinary.isNotEmpty()) { "No ordinary BuildCraft localization files were found" }
        require(guide.isNotEmpty()) { "No Guide Book text packs were found" }
        require(ordinary.none { it.name == "en_us.json" }) {
            "The localization add-on must not package assets/buildcraft/lang/en_us.json"
        }
        require(guide.none { it.name == "en_us.json" }) {
            "The localization add-on must not package assets/buildcraft/guide/text/en_us.json"
        }

        val localeName = Regex("[a-z]{2}_[a-z]{2}\\.json")
        (ordinary + guide).forEach { file ->
            require(localeName.matches(file.name)) {
                "${file.relativeTo(rootProject.projectDir)} has an invalid locale filename"
            }
        }

        val ordinaryLocales = ordinary.mapTo(sortedSetOf()) { it.nameWithoutExtension }
        val guideLocales = guide.mapTo(sortedSetOf()) { it.nameWithoutExtension }
        require(ordinaryLocales == guideLocales) {
            val missingGuide = ordinaryLocales - guideLocales
            val missingOrdinary = guideLocales - ordinaryLocales
            buildString {
                append("Ordinary and Guide Book locale sets do not match.")
                if (missingGuide.isNotEmpty()) append(" Missing Guide Book packs: ${missingGuide.joinToString() }.")
                if (missingOrdinary.isNotEmpty()) append(" Missing ordinary lang files: ${missingOrdinary.joinToString() }.")
            }
        }

        fun validateStringObject(file: File) {
            val parsed = JsonSlurper().parse(file)
            require(parsed is Map<*, *>) {
                "${file.relativeTo(rootProject.projectDir)} must contain a JSON object"
            }
            parsed.forEach { (key, value) ->
                require(key is String && value is String) {
                    "${file.relativeTo(rootProject.projectDir)} must contain only string-to-string translations"
                }
            }
        }

        val allOrdinaryLang = commonResources.resolve("assets").walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.parentFile.name == "lang" }
            .sortedBy { it.relativeTo(commonResources).path }
            .toList()
        require(allOrdinaryLang.none { it.name == "en_us.json" }) {
            "The localization add-on must not package en_us in any asset namespace"
        }
        allOrdinaryLang.forEach(::validateStringObject)

        val ironTanksLocales = commonResources.resolve("assets/irontanks/lang")
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.mapTo(sortedSetOf<String>()) { it.nameWithoutExtension } ?: sortedSetOf<String>()
        require(ironTanksLocales.containsAll(setOf("ru_ru", "zh_cn"))) {
            "Iron Tanks Community Edition translations were not migrated into the localization add-on"
        }

        val expectedPageSegmentsByPageCount = mutableMapOf<Int, Map<String, Int>>()
        guide.forEach { file ->
            val parsed = JsonSlurper().parse(file)
            require(parsed is Map<*, *>) {
                "${file.relativeTo(rootProject.projectDir)} must contain a JSON object"
            }
            require(parsed["format"] == 1) {
                "${file.name} has an unsupported Guide Book text-pack format"
            }

            val pages = parsed["pages"]
            require(pages is Map<*, *> && pages.size in setOf(207, 218)) {
                "${file.name} must contain either the legacy 207-page or current 218-page Guide Book layout"
            }

            val pageSegments = linkedMapOf<String, Int>()
            pages.forEach { (page, segments) ->
                require(page is String && segments is List<*>) {
                    "Invalid Guide Book page entry in ${file.name}"
                }
                require(segments.isNotEmpty() && segments.all { it is String && it.isNotBlank() }) {
                    "Guide Book page $page in ${file.name} contains blank or non-string segments"
                }
                pageSegments[page] = segments.size
            }

            val pageCount = pages.size
            val expected = expectedPageSegmentsByPageCount[pageCount]
            if (expected == null) {
                expectedPageSegmentsByPageCount[pageCount] = pageSegments
            } else {
                require(pageSegments == expected) {
                    "${file.name} does not match the $pageCount-page Guide Book layout used by the other locales"
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyLocalizations)
}
