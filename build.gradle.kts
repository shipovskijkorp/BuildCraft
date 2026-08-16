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
        val localeName = Regex("[a-z]{2}_[a-z]{2}\\.json")

        fun jsonObject(file: File): Map<*, *> {
            val parsed = JsonSlurper().parse(file)
            require(parsed is Map<*, *>) {
                "${file.relativeTo(rootProject.projectDir)} must contain a JSON object"
            }
            return parsed
        }

        fun jsonFiles(directory: File): List<File> = directory
            .listFiles { file -> file.isFile && file.extension == "json" }
            ?.sortedBy { it.name } ?: emptyList()

        fun validateOrdinary(namespace: String, expectedKeyCount: Int): Set<String> {
            val directory = commonResources.resolve("assets/$namespace/lang")
            val files = jsonFiles(directory)
            require(files.isNotEmpty()) { "No $namespace ordinary localization files were found" }
            require(files.none { it.name == "en_us.json" }) {
                "The localization add-on must not package assets/$namespace/lang/en_us.json"
            }

            var expectedKeys: Set<String>? = null
            files.forEach { file ->
                require(localeName.matches(file.name)) {
                    "${file.relativeTo(rootProject.projectDir)} has an invalid locale filename"
                }
                val parsed = jsonObject(file)
                parsed.forEach { (key, value) ->
                    require(key is String && value is String) {
                        "${file.relativeTo(rootProject.projectDir)} must contain only string-to-string translations"
                    }
                }
                val keys = parsed.keys.map { it as String }.toSet()
                val reference = expectedKeys
                if (reference == null) {
                    require(keys.size == expectedKeyCount) {
                        "${file.name} has ${keys.size} keys; expected $expectedKeyCount"
                    }
                    expectedKeys = keys
                } else {
                    require(keys == reference) {
                        val missing = reference - keys
                        val extra = keys - reference
                        buildString {
                            append("${file.name} does not match the $namespace localization key set.")
                            if (missing.isNotEmpty()) append(" Missing: ${missing.sorted().joinToString()}.")
                            if (extra.isNotEmpty()) append(" Extra: ${extra.sorted().joinToString()}.")
                        }
                    }
                }
            }
            return files.mapTo(sortedSetOf()) { it.nameWithoutExtension }
        }

        fun validateGuide(
            namespace: String,
            expectedPageCount: Int,
            expectedMaxSlotCount: Int,
            allowFallbackSlots: Boolean
        ): Set<String> {
            val directory = commonResources.resolve("assets/$namespace/guide/text")
            val files = jsonFiles(directory)
            require(files.isNotEmpty()) { "No $namespace Guide Book text packs were found" }
            require(files.none { it.name == "en_us.json" }) {
                "The localization add-on must not package assets/$namespace/guide/text/en_us.json"
            }

            val parsedFiles = files.map { file ->
                require(localeName.matches(file.name)) {
                    "${file.relativeTo(rootProject.projectDir)} has an invalid locale filename"
                }
                val parsed = jsonObject(file)
                require(parsed["format"] == 1) {
                    "${file.name} has an unsupported Guide Book text-pack format"
                }
                require(parsed["language"] == file.nameWithoutExtension) {
                    "${file.name} must declare language=${file.nameWithoutExtension}"
                }
                val pagesValue = parsed["pages"]
                require(pagesValue is Map<*, *> && pagesValue.size == expectedPageCount) {
                    "${file.name} must contain all $expectedPageCount $namespace Guide Book pages"
                }
                val pages = pagesValue as Map<*, *>
                file to pages
            }

            val expectedPages = parsedFiles.first().second.keys.map { key ->
                require(key is String) { "Guide Book page IDs must be strings" }
                key
            }.toSet()

            val maxSlots = linkedMapOf<String, Int>()
            parsedFiles.forEach { (file, pages) ->
                val pageIds = pages.keys.map { key ->
                    require(key is String) { "Invalid Guide Book page ID in ${file.name}" }
                    key
                }.toSet()
                require(pageIds == expectedPages) {
                    val missing = expectedPages - pageIds
                    val extra = pageIds - expectedPages
                    buildString {
                        append("${file.name} does not match the $namespace Guide Book page set.")
                        if (missing.isNotEmpty()) append(" Missing: ${missing.sorted().joinToString()}.")
                        if (extra.isNotEmpty()) append(" Extra: ${extra.sorted().joinToString()}.")
                    }
                }
                pages.forEach { (page, value) ->
                    require(page is String && value is List<*> && value.isNotEmpty()) {
                        "Invalid Guide Book page entry $page in ${file.name}"
                    }
                    maxSlots[page] = maxOf(maxSlots[page] ?: 0, value.size)
                }
            }

            require(maxSlots.values.sum() == expectedMaxSlotCount) {
                "$namespace Guide Book layout exposes ${maxSlots.values.sum()} text slots; expected $expectedMaxSlotCount"
            }

            parsedFiles.forEach { (file, pages) ->
                pages.forEach { (pageValue, segmentsValue) ->
                    val page = pageValue as String
                    val segments = segmentsValue as List<*>
                    val expectedSlots = maxSlots.getValue(page)
                    require(segments.size == expectedSlots) {
                        "Guide Book page $page in ${file.name} has ${segments.size} slots; layout requires $expectedSlots"
                    }
                    require(segments.all { it == null || it is String }) {
                        "Guide Book page $page in ${file.name} contains a non-string fallback value"
                    }
                    require(segments.filterIsInstance<String>().all { it.isNotBlank() }) {
                        "Guide Book page $page in ${file.name} contains a blank translated segment"
                    }
                    require(segments.any { it is String && it.isNotBlank() }) {
                        "Guide Book page $page in ${file.name} contains no translated text"
                    }
                    if (!allowFallbackSlots) {
                        require(segments.size == expectedSlots && segments.none { it == null }) {
                            "Guide Book page $page in ${file.name} must provide all $expectedSlots translated slots"
                        }
                    }
                }
            }

            return files.mapTo(sortedSetOf()) { it.nameWithoutExtension }
        }

        val buildCraftOrdinary = validateOrdinary("buildcraft", 2008)
        val ironTanksOrdinary = validateOrdinary("irontanks", 87)
        val buildCraftGuide = validateGuide("buildcraft", 218, 1342, true)
        val ironTanksGuide = validateGuide("irontanks", 27, 135, false)

        require(buildCraftOrdinary == buildCraftGuide) {
            "BuildCraft ordinary and Guide Book locale sets do not match"
        }
        require(ironTanksOrdinary == ironTanksGuide) {
            "Iron Tanks ordinary and Guide Book locale sets do not match"
        }
        require(buildCraftOrdinary == ironTanksOrdinary) {
            val missingIronTanks = buildCraftOrdinary - ironTanksOrdinary
            val missingBuildCraft = ironTanksOrdinary - buildCraftOrdinary
            buildString {
                append("BuildCraft and Iron Tanks locale sets do not match.")
                if (missingIronTanks.isNotEmpty()) append(" Missing Iron Tanks locales: ${missingIronTanks.joinToString()}.")
                if (missingBuildCraft.isNotEmpty()) append(" Missing BuildCraft locales: ${missingBuildCraft.joinToString()}.")
            }
        }

        val ironTanksLanguageRegistryFile = commonResources.resolve("assets/irontanks/guide/languages.json")
        val ironTanksLanguageRegistry = jsonObject(ironTanksLanguageRegistryFile)
        require(ironTanksLanguageRegistry["format"] == 1 && ironTanksLanguageRegistry["default"] == "en_us") {
            "Iron Tanks Guide Book language registry has invalid metadata"
        }
        val registeredLanguagesValue = ironTanksLanguageRegistry["languages"]
        require(registeredLanguagesValue is Map<*, *>) {
            "Iron Tanks Guide Book language registry has no languages object"
        }
        val registeredLanguages = registeredLanguagesValue as Map<*, *>
        val registeredLocales = registeredLanguages.keys.map { key ->
            require(key is String) { "Iron Tanks Guide Book registry locale IDs must be strings" }
            key
        }.toSet() - "en_us"
        require(registeredLocales == ironTanksGuide) {
            val missing = ironTanksGuide - registeredLocales
            val extra = registeredLocales - ironTanksGuide
            buildString {
                append("Iron Tanks Guide Book language registry does not match its text packs.")
                if (missing.isNotEmpty()) append(" Missing: ${missing.sorted().joinToString()}.")
                if (extra.isNotEmpty()) append(" Extra: ${extra.sorted().joinToString()}.")
            }
        }
        registeredLocales.forEach { locale ->
            val entryValue = registeredLanguages[locale]
            require(entryValue is Map<*, *>) {
                "Iron Tanks Guide Book registry entry $locale must be an object"
            }
            val entry = entryValue as Map<*, *>
            require(entry["name"] is String && (entry["name"] as String).isNotBlank()) {
                "Iron Tanks Guide Book registry entry $locale has no display name"
            }
            val fallback = entry["fallback"]
            require(fallback is List<*> && fallback.contains("en_us")) {
                "Iron Tanks Guide Book registry entry $locale must fall back to en_us"
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyLocalizations)
}
