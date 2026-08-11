# ADR-009 — Complete API 2 migration surface

## Status

Accepted for the BCCE 8.0.14 development line.

## Decision

Before deleting the legacy Java API, API 2 must be able to represent every
public concept that the current BCCE implementation itself consumes. API 2 is
therefore expanded in one contract-first pass rather than inventing a new API
fragment during every migration PR.

The dependency target is:

```text
addon -> buildcraft.api.v2 <- buildcraft.lib.internal.api.v2 <- BCCE modules
```

`buildcraft.api.v2` is contract-only. Registry/reload/persistence engines and
concrete service backends belong to Lib, even when Java visibility must be
`public` for cross-package use inside BCCE. See `API_LIB_BOUNDARY.md`.

and never:

```text
buildcraft.api.v2 -> legacy buildcraft.api -> implementation
```

The old Java API may be deleted domain-by-domain once the BCCE implementation
has moved to the corresponding v2 contracts. Save IDs, aliases and raw unknown
payload preservation remain independently supported.

## Stable design rules

1. Registered extension identities are `ResourceLocation` IDs.
2. Persistent extension state is ID + schema version + codec, never Java class name.
3. Loader capabilities are adapted behind item/fluid/MJ/external-energy ports.
4. Runtime implementation objects are represented by scoped read-only handles/views.
5. Mutation is granted only through narrow control/context interfaces.
6. Pipes are composition-based; addons do not subclass `PipeFlow` or `PipeBehaviour`.
7. Statements use typed parameter schemas and immutable slots.
8. Wires are typed signal channels; internal graph topology is not mutable public API.
9. Robots use task/resource/dock type registries and handles, not entity/AI inheritance.
10. Schematics use adapters and snapshot element types; missing addon payloads are preservable.
11. Client presentation is metadata in the common API; direct client renderer hooks stay out of common code.
12. Network extension payloads declare type ID, codec, direction, phase and maximum size.

## Registry surface

`BuildCraftRegistries` now reserves authoritative registries for:

- machine, engine, chipset and laser-table types;
- pipe types/components/attachments/connection rules/events/sync channels;
- statement parameters/actions/triggers/contributors;
- filler patterns and signal channels;
- automation actions and Stripes handlers;
- robot tasks/resources/dock ports/boards;
- schematic adapters/snapshot elements/inventory-copy policies;
- facade adapters, list adapters, rotation/paint handlers;
- item-label/map-location adapters, fluid-drop providers and debug contributors;
- MJ connection rules and robot lifecycle listeners;
- network payload types and client presentation metadata.

The runtime creates these registries at bootstrap. They support provenance,
aliases and a freeze boundary.

## Service surface

`BuildCraftServices` reserves runtime services for operations that require the
world or implementation state: actors, permissions, world rules, block
interactions, lists, facades, filler, statements/gates, signals, automation,
pipes, machines/laser targets, robots, schematics, networking, platform bridges,
MJ formatting and power-loss effects.

API 2 also has narrow compatibility-shaped ports for legacy semantics that the
implementation still needs during migration, such as slotted inventories,
travelling-item injection, machine areas/control modes/heat profiles, map
locations and robot lifecycle events. These contracts describe behavior, not
legacy implementation classes.

A service key existing does not imply that a domain has already been migrated.
The implementation is installed during the corresponding migration pass.

## Migration consequence

The remaining project work is no longer API invention. It is implementation
migration:

1. pipes/pluggables;
2. statements/gates/filler;
3. wires/signals/Stripes;
4. machines/engines/laser targets;
5. robots;
6. schematics/builders;
7. remaining core/list/facade/block hooks;
8. network/client integration;
9. remove unused legacy Java API classes;
10. keep save/data aliases and migration readers.

`LEGACY_IMPORT_MIGRATION_MAP.csv` plus `validate-api-v2-migration-surface.py`
make this decision enforceable: every old API import still used by BCCE must be
classified as either `MIGRATE` to a named v2 contract or `INTERNALIZE` into the
implementation. Unclassified regressions are a CI failure.
