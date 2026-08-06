# BuildCraft 8.0.12 multi-version workspace

This repository uses Stonecutter as one build and release workspace for several
Minecraft/loader targets. Generated Gradle projects live under `versions/` and
are not committed.

## Registered targets

| Target | Source strategy | Java | Notes |
|---|---|---:|---|
| `1.19.2-forge` | shared root `src/` | 17 | Stonecutter VCS/source-of-truth target |
| `1.20.1-forge` | `version-src/1.20.1-forge/src/` | 17 | Full 8.0.12 API port |

The 1.20.1 API delta is large enough that forcing every changed method through
hundreds of inline preprocessor branches would make critical machine and pipe
logic unsafe to review. Stonecutter therefore supports a target-specific
`source.root`. Build logic, dependencies, metadata, runs, tests, packaging and
CI remain centralized in `build.forge.gradle` and
`stonecutter.properties.toml`.

For nearby versions, prefer the shared `src/` tree with small Stonecutter
conditions or bridge classes. Use a full version source root only across a large
Minecraft/loader API boundary.

## Important files

- `settings.gradle.kts` — target matrix.
- `stonecutter.properties.toml` — target versions, dependencies, pack formats,
  metadata ranges, protocol and optional source root.
- `stonecutter.gradle.kts` — active target and aggregate tasks.
- `build.forge.gradle` — common Forge build logic for all registered versions.
- `src/` — shared source, currently based on Forge 1.19.2.
- `version-src/1.20.1-forge/src/` — Forge 1.20.1 source port.
- `scripts/validate-stonecutter.py` — offline matrix/source/resource validation.

## Gradle compatibility

The workspace wrapper is pinned to **Gradle 8.8**, and every Forge target
explicitly applies **ForgeGradle 6.x**. This is intentional: the original
1.19.2 project used ForgeGradle 5, which rejects Gradle 8, while the original
1.20.1 project already used ForgeGradle 6. Both targets are now on the same
Gradle-compatible ForgeGradle generation.

Do not restore ForgeGradle 5, the standalone AccessTransformers plugin, or the
standalone Renamer plugin. ForgeGradle 6 handles access transformers directly,
uses `fg.deobf` for mod dependencies, and produces the release jar through
`reobfJar`. Use Java 17 or a newer JVM supported by Gradle 8.8 to run the
wrapper; the Minecraft compilation toolchain remains Java 17 for both targets.

## Commands

```bash
# No dependency downloads required
python scripts/validate-stonecutter.py

# Build and test all registered targets; collect production jars
./gradlew buildAndCollect

# Build one target
./gradlew :1.19.2-forge:buildAndCollect
./gradlew :1.20.1-forge:buildAndCollect

# Switch the IDE/Stonecutter active target
./gradlew "Set active project to 1.20.1-forge"

# Run a target directly
./gradlew :1.20.1-forge:runClient
./gradlew :1.20.1-forge:runServer
./gradlew :1.20.1-forge:runGameTestServer
```

`vcsVersion` remains `1.19.2-forge` because the shared root `src/` is expressed
in that API. This does not prevent building or running 1.20.1.

## Adding another Forge version

1. Register the target in `settings.gradle.kts`:

   ```kotlin
   target("1.21.1", "forge")
   ```

2. Add `[forge."1.21.1"]` to `stonecutter.properties.toml`, including Minecraft,
   Forge, Java, dependency coordinates/ranges, pack formats and a unique network
   protocol.

3. Choose one source strategy:

   **Small API delta:** keep the code in shared `src/` and use narrow conditions:

   ```java
   //? if >=1.20.1 {
   newApiCall();
   //?} else {
   /*oldApiCall();*/
   //?}
   ```

   **Large API delta:** create `version-src/1.21.1-forge/src/` and add:

   ```toml
   source.root = "version-src/1.21.1-forge"
   ```

   A version source root contains `src/main`, `src/test` and optionally
   `src/gametest`; it must not contain another Gradle build or nested `.git`.

4. Validate and compile the new node before editing additional versions:

   ```bash
   python scripts/validate-stonecutter.py
   ./gradlew :1.21.1-forge:build
   ```

## Adding another loader

Keep loader Gradle models separate:

1. Add `build.neoforge.gradle` or `build.fabric.gradle`.
2. Preload the loader plugin with `apply false` in `stonecutter.gradle.kts`.
3. Register the loader in `settings.gradle.kts`.
4. Add the corresponding loader/version TOML table.
5. Put platform-only calls behind small bridge classes or loader conditions.

Do not mix Forge and NeoForge Gradle plugins in one target build script.
