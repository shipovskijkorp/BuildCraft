# BuildCraft Community Edition 8.0.12 — Forge 1.21.1 beta port

This tree is the low-usage Forge 1.21.1 beta branch updated from 8.0.11 beta1 to the full 8.0.12 feature and fix set. The 1.20.1 8.0.11 branch, its completed 8.0.12 port and `8_0_12.patch` were used in a three-way merge so existing 1.21.1 API adaptations were retained instead of overwritten.

## Carried 8.0.12 changes

- native Guide Book with persisted navigation state and modern Data Components storage;
- owner-aware robot and automated-machine actions;
- low-power robot station return/release behavior;
- JEI categories and recipe-transfer fixes;
- laser recipe energy rebalance;
- biome-temperature combustion behavior and configuration cleanup;
- generated-chunk-only Zone Planner behavior;
- item, fluid and power pipe stability/persistence fixes;
- transactional fluid movement and rollback;
- unknown pipe/pluggable NBT preservation;
- continuous engine output and engine-state persistence;
- builder, filler, quarry, mining-well, pump and flood-gate fixes;
- bounded and permission-checked server packets;
- texture-atlas, model-cache and renderer reload fixes;
- JUnit and Forge GameTest coverage adapted to 1.21.1.

## Target-specific decisions

- Target: Minecraft `1.21.1`, Forge `52.1.16`, Java `21`.
- Network protocol: `BC8.0.x-1.21.1-net2`.
- Recipe and advancement resources use the singular 1.21.1 directories (`recipe`, `advancement`).
- Item state uses Data Components/`CustomData`; old direct ItemStack tag access is not used.
- JEI is optional and uses its Forge 1.21.1 API.
- Forestry CE, Patchouli-for-Forestry, IC2 Classic and Jade integrations are disabled for this target because the beta branch has no verified compatible dependency set. Reflection hooks remain harmless when those mods are absent.
- This remains a beta target: compile/test/GameTest/server smoke checks are required before publishing.

## Beta hardening performed during integration

- resolved all three-way-merge syntax damage and scanned every main/test/GameTest Java file for structural parse errors;
- replaced the obsolete two-list debug-overlay adapter with Forge 52's per-side `getText()` / `getSide()` API;
- removed null-context block clone calls used by snapshots and machine loot;
- fixed the west-facing template layer in the Builder multipart model;
- verified internal BuildCraft imports, package paths, JSON/TOML syntax, singular 1.21.1 data directories and custom recipe schemas;
- disabled optional integrations without a verified 1.21.1 Forge dependency rather than shipping untested hard links.

These checks do not replace runtime validation. In particular, world migration, every machine GUI, all pipe/pluggable render paths, robot task recovery, chunk-loading boundaries and dedicated-server startup still need local smoke testing before the beta is published.

## Validation

Run the target-specific structural validator and then the real build suite:

```bash
python scripts/validate-1.21.1-port.py --source-root version-src/1.21.1-forge
./gradlew :1.21.1-forge:clean :1.21.1-forge:test :1.21.1-forge:build
./gradlew :1.21.1-forge:runGameTestServer
STONECUTTER_TARGET=1.21.1-forge SERVER_RUNTIME_PROFILE=base bash scripts/ci-server-smoke.sh
```
