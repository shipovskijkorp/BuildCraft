# BuildCraft Community Edition 8.0.12 — Forge 1.20.1 port

This tree is the Forge 1.20.1 branch updated from 8.0.11 to the 8.0.12 feature and
fix set. The 1.19.2 8.0.12 source and `8_0_12.patch` were used as the semantic
reference; Minecraft/Forge, JEI, Jade and Forestry calls were adapted to their
1.20.1 APIs instead of copying incompatible signatures verbatim.

## Major carried changes

- native BuildCraft Guide Book and persisted page/navigation state;
- Forestry Community Edition Propolis Item Pipe, GUI, recipes and list matching;
- owner-aware fake players for robots and automated machines;
- low-power robot return behavior and manual station release;
- JEI category/transfer rework and phantom recipe books for the Advanced
  Crafting Table and Auto Workbench;
- 8.0.12 laser recipe energy rebalance;
- biome-temperature combustion heating and cleaned configuration lifecycle;
- network protocol `BC8.0.x-1.20.1-net2`;
- generated-chunk-only Zone Planner validation;
- power/item/fluid pipe stability and persistence fixes;
- transactional fluid movement with rollback;
- unknown pipe/pluggable NBT preservation;
- continuous engine output and engine state persistence;
- builder/filler/quarry/mining-well/pump/flood-gate task and search fixes;
- server-side packet distance, chunk, permission and size validation;
- texture-atlas reload, Oculus and model-cache fixes;
- 8.0.12 JUnit and Forge GameTest coverage.

## Compatibility notes

- Target: Minecraft `1.20.1`, Forge `47.4.10`, Java `17`.
- Forestry CE runtime is pinned to the 1.20.1-compatible build configured in
  `gradle.properties`; Patchouli is loaded with it.
- JEI and Jade are optional runtime profiles but compile-time APIs.
- IC2 Classic compatibility sources remain in the repository but are excluded
  from the 1.20.1 compilation because there is no compatible Forge 1.20.1 IC2
  Classic release configured for this branch.
- 8.0.12 clients and servers must use the same target build.

## Validation

Offline structural validation:

```bash
python scripts/validate-8.0.12-port.py
```

Required real build checks on a networked machine:

```bash
./gradlew clean test
./gradlew runGameTestServer
SERVER_RUNTIME_PROFILE=base bash scripts/ci-server-smoke.sh
SERVER_RUNTIME_PROFILE=forestry bash scripts/ci-server-smoke.sh
./gradlew build
```

The supplied sandbox could not download Gradle from `services.gradle.org`, so a
real ForgeGradle compilation was not completed there. The offline validator,
resource parsers, merge-marker scan, Java lexical scan, patch-coverage check and
shell/Python syntax checks pass.
