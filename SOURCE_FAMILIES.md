# BuildCraft build generations and hybrid source layout

BuildCraft Community Edition uses two independent Gradle/Stonecutter builds and one shared source repository.

The project has one gameplay goal across every supported target:

> **Different implementation. Indistinguishable BuildCraft.**

Minecraft versions, loaders and build toolchains are implementation details. Unless a deviation is explicitly documented, players should not be able to identify the target from BuildCraft gameplay, balance, persistence, UI behaviour, machine timing, routing, permissions or resource handling.

## Build generations

### `legacy`

Current targets:

- `1.19.2-forge`
- `1.20.1-forge`

Build root: `builds/legacy`

The legacy build owns ForgeGradle-era targets and currently uses its own Gradle 8 wrapper. `1.19.2-forge` remains the behaviour reference; newer implementations are not required to look identical in source.

### `modern`

Current target:

- `1.21.1-neoforge`

Planned targets include Minecraft 1.21.11 and 26.x on NeoForge and Fabric.

Build root: `builds/modern`

The modern wrapper is independent from the legacy wrapper. It may move to a newer Gradle, Stonecutter or Java toolchain when future Minecraft/Fabric/NeoForge versions require it, without forcing those requirements onto the legacy build.

If a future generation becomes structurally incompatible with the current modern family, create another independent build generation instead of forcing every version through one wrapper or filling gameplay code with large condition blocks.

## Repository layout

```text
build-config/
├─ common.properties              shared mod metadata
└─ generations.properties         independent build-generation index

builds/
├─ legacy/                        legacy settings, controller and wrapper
└─ modern/                        modern settings, controller and wrapper

source-shared/
└─ src/                           files valid for every target

source-families/
├─ legacy/
│  └─ src/                        generation-wide legacy implementation
└─ modern/
   └─ src/                        generation-wide modern implementation

source-platforms/
├─ forge/
├─ neoforge/
└─ fabric/                        loader-specific implementation

version-src/
├─ 1.19.2-forge/
├─ 1.20.1-forge/
└─ 1.21.1-neoforge/               irreducible target-only files/resources
```

A target is materialized as:

```text
shared + family + platform + target overlay = effective target source tree
```

Layers use that precedence order: a different file in a later layer overrides the same logical path from an earlier layer. Byte-identical overrides are rejected by the source-layout validator.

The generated effective tree is created under the target subproject's `build/effective-source` directory. It is build output, not authoritative source.

## Placement rules

Choose the narrowest layer that represents the real reason for a difference.

### `source-shared`

Use for code and resources that are valid for every supported target.

### `source-families/<generation>`

Use for substantial Minecraft-generation differences shared by every target in one build generation. Examples include serialization models, registry architecture, networking generations or broad rendering/API changes.

### `source-platforms/<loader>`

Use for loader APIs and integration points, including:

- Forge, NeoForge or Fabric registration;
- capabilities or transfer APIs;
- loader lifecycle and events;
- loader networking setup;
- access transformers/access wideners;
- loader metadata and loader-specific compatibility glue.

Loader imports must not escape into `source-shared` or `source-families`.

### `version-src/<target>`

Use only when a complete file or resource is genuinely target-specific and cannot remain readable in a family/platform layer. Target overlays should stay small and must not contain inline version conditions.

## Localized version conditions

Small Minecraft API differences may remain in a family or platform file with Stonecutter-style directives. The effective-source generator evaluates them for each target before compilation.

Example:

```java
//? if <1.20 {
player.level
//?} else {
/*?
player.level()
?*/
//?}
```

Use inline conditions for local differences such as:

- renamed methods, fields, constants or enum values;
- a small signature change;
- an added argument or import;
- a short alternative branch.

Do not use inline conditions for loader selection. Loader differences belong in `source-platforms`.

Current policy enforced by `scripts/validate-source-families.py`:

1. no loader conditions in shared gameplay source;
2. no inline conditions in target overlays;
3. no more than four conditional blocks in one Java file;
4. large or deeply nested alternatives must become family/platform/target implementations;
5. every configured target must successfully preprocess every conditional file.

## Behaviour parity

`1.19.2-forge` is the reference implementation for player-visible behaviour.

Parity means equivalent observable results, not source-code identity. It includes:

- machine speed, costs, capacity and refunds;
- MJ generation, consumption and routing;
- item/fluid pipe routing and filtering;
- Builder, Filler and Quarry behaviour;
- robots, boards, stations and ownership identity;
- gates, wires, lasers and statements;
- save/load and chunk-unload persistence;
- permission/protection behaviour;
- blueprint contents and resource reservations;
- GUI semantics and user interactions.

A target may use different data components, registries, network APIs, events, capabilities or transfer systems as long as the observable BuildCraft behaviour remains equivalent.

### Blind-room criterion

> Put players on different supported BCCE targets in separate rooms, hide Minecraft/loader information, and give them equivalent BuildCraft scenarios. They should not be able to identify the target from BuildCraft behaviour alone.

A visible difference is either an intentional and documented vanilla-induced deviation or a parity regression.

## Build commands

The effective-source generator requires Python 3. Gradle, Java and loader toolchains remain isolated inside their respective build generation.

Build every generation with its own wrapper:

```text
./build-all.sh
```

Windows:

```text
build-all.bat
```

PowerShell:

```text
./build-all.ps1
```

Build only one generation:

```text
cd builds/legacy
./gradlew buildAndCollect
```

```text
cd builds/modern
./gradlew buildAndCollect
```

Run the active target of one generation:

```text
cd builds/legacy
./gradlew runActiveClient
```

```text
cd builds/modern
./gradlew runActiveClient
```

List configured targets:

```text
python scripts/validate-stonecutter.py --list-targets
python scripts/validate-stonecutter.py --list-targets --generation legacy
python scripts/validate-stonecutter.py --list-targets --generation modern
```

Validate the complete architecture:

```text
python scripts/validate-stonecutter.py
python scripts/validate-source-families.py
python scripts/validate-repository-cleanliness.py
python scripts/validate-behavior-parity.py
```

Materialize one target manually:

```text
python scripts/source_layout.py 1.20.1-forge --output build/manual/1.20.1-forge
```

## Adding future targets

1. Add the target to the appropriate `builds/<generation>/targets.properties` file.
2. Add its loader build script to that generation if the loader is new there.
3. Reuse `source-shared`, the generation family and the loader platform layer.
4. Add only irreducible files to `version-src/<target>`.
5. Prefer a localized version condition over copying a large file for a one-line Minecraft API change.
6. Add a new build generation only when toolchain or architecture incompatibility makes the existing wrapper/family unsuitable.

Do not create another full source-tree copy for a new port.
