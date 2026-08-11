# BuildCraft Extension API 2

API 2 now contains the complete public contract surface required to migrate the
current BCCE codebase away from the legacy `buildcraft.api` tree.


## API / Lib boundary

Only `buildcraft.api.v2` is supported addon surface. Concrete runtime machinery
lives under `buildcraft.lib.internal.api.v2`; other `buildcraft.lib.*` and gameplay
module packages are implementation details and may change without an API-major
bump. API2 must never import Lib, gameplay modules, loader APIs or Minecraft
client classes. See `docs/api2/API_LIB_BOUNDARY.md`.

Runtime attachment uses Java `ServiceLoader` with the read-only `ApiRuntime`
contract as the service type. The concrete provider lives in Lib; there is no
implementation-only SPI package in the supported addon API.

## Implemented foundation

- write-once runtime facade, API version/features and monotonic bootstrap lifecycle;
- immutable/frozen registries with provenance and namespaced aliases;
- versioned persistence, schema migrations and UnknownPayload preservation;
- atomic last-known-good reload snapshots;
- loader-neutral item/fluid/MJ/external-energy transfer contracts;
- actor/permission model for automated world operations;
- authoritative fuels/coolants, recipes, crops, templates, facade rules and world properties.

## Complete migration surface

The v2 contracts now also cover:

- module discovery and typed extension contexts;
- areas/zones/paths, lists, block rotation/painting and wrench discovery;
- named-item labels, map-location items, request providers, debug views and fluid-drop hooks;
- facade material adapters and filler pattern types;
- typed statements, parameters, contributors and gate programs;
- generic typed signal channels replacing direct wire graph exposure;
- common automation requests used by robots, Stripes, builders and machines;
- composition-based pipe types, transport profiles, components, attachments,
  connection rules, typed routing hooks, injection ports, sync channels and typed events;
- machine/engine/chipset/laser-table descriptors, control modes, heat profiles and runtime views;
- robot tasks/resources/docks/boards/events without implementation inheritance or class-name persistence;
- schematic snapshot elements, snapshot kinds, adapters and typed inventory-copy paths;
- bounded typed network payloads;
- loader-neutral client presentation metadata;
- addon testkit helpers and an isolated compile-only consumer fixture.

The new contracts intentionally do **not** expose Forge, NeoForge or Fabric
classes, BuildCraft implementation classes, raw internal block entities,
`PipeFlow`, `PipeBehaviour`, `EntityRobotBase`, reflection-based event buses or
writable global maps.

At this stage the public surface is migration-ready. Some runtime services
(`PIPES`, `STATEMENTS`, `ROBOTS`, `SCHEMATICS`, etc.) are contracts whose BCCE
backends are installed during the domain migration pass; static extension types
can already be registered through `BuildCraftRegistries`.

`docs/api2/LEGACY_IMPORT_MIGRATION_MAP.csv` is the executable migration ledger:
CI rejects every implementation import from the old Java API that does not have
an API 2 replacement or an explicit `INTERNALIZE` disposition. This lets the
legacy Java tree be deleted progressively without losing save/data aliases.
