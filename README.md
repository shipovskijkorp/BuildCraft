# BuildCraft Community Edition: Localizations

A client-side localization addon for BuildCraft Community Edition.

The addon contains every bundled BCCE and ITCE translation except `en_us`. English remains in the main JAR and is used as the fallback language.

## Supported targets

- Minecraft 1.19.2 — Forge
- Minecraft 1.20.1 — Forge
- Minecraft 1.21.1 — NeoForge

All targets live in this single branch and are managed by Stonecutter.

## Build all versions

```bash
./gradlew build
```

The resulting JAR files are written to the individual target directories:

```text
versions/<target>/build/libs/
```

## Switch the active development target

Edit only through Stonecutter tasks or the generated active-version selector in `stonecutter.gradle.kts`. The initial active target is `1.19.2-forge`.

## Resource layout

Shared translations are stored once:

```text
src/main/resources/assets/buildcraft/lang/
src/main/resources/assets/buildcraft/guide/text/
```

Loader- and version-specific metadata is stored under:

```text
versions/<target>/src/main/resources/
```

A target-specific translation override may also be placed in the corresponding version resource directory. Gradle packages the common files and that target's overlay into one JAR.

## Validation

```bash
./gradlew check
```

The verification task enforces the complete locale sets, exact ordinary-translation key sets, current Guide Book page/layout coverage, valid text-pack metadata, and runtime-compatible fallback slots. It also rejects bundled `en_us`, invalid JSON values, blank translated Guide Book segments, and missing BuildCraft/Iron Tanks locale pairs.
