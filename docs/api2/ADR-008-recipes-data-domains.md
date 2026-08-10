# ADR-008: Machine recipes and low-risk data-domain services

Status: accepted for API 2 preview

## Decision

API 2 owns an authoritative `MachineRecipeService` for Integration Table,
Distiller and Heat Exchanger definitions. Programmatic definitions form the
code baseline; reload-owned definitions are resolved and published atomically
through the same last-known-good reload machinery used by fuels/coolants.

Recipe selection is deterministic: definition priority descending, then
namespaced recipe id. Duplicate/ambiguous definitions are rejected by the
reload resolver instead of depending on hash-map iteration order.

The initial recipe model consists of:

- `IntegrationRecipeDefinition` for programmatic integration behavior;
- `DistillationRecipeDefinition` for fluid distillation;
- `HeatExchangeRecipeDefinition` for heating and cooling;
- `CountedIngredient` as the typed replacement for legacy `IngredientStack`;
- `FluidIngredient` as a loader-neutral fluid matcher plus consumed amount.

Only stable data forms are serializable. A programmatic fluid matcher cannot be
encoded as data and fails encoding rather than being silently degraded.

Legacy `IntegrationRecipeRegistry` and `RefineryRecipeRegistry` remain public
compatibility facades, but API 2 owns the runtime recipe snapshot. Legacy
programmatic recipes are represented as code-owned definitions with synthetic
or existing namespaced ids. Legacy refinery replacement-by-input semantics are
preserved by replacing the synthetic code definition for that input.

`CropService`, `TemplateService`, `FacadeRuleService` and
`WorldPropertyService` replace additional mutable/global data-domain storage.
Registrations have explicit ids and deterministic ordering. Existing public
legacy entry points delegate to these services so current addons can migrate
incrementally.

## Filler exception

Filler patterns are deliberately not exposed as a new API 2 domain yet. The
legacy pattern type inherits the old statement/client contracts, so promoting
it now would leak legacy statement types into API 2. The legacy registry is
hardened (stable insertion order and immutable views while preserving its historical
last-write-wins replacement behavior) and built-in consumers no longer depend on
the writable global field. A typed
filler extension belongs after the API 2 statements/gates redesign.

## Reload ownership

`MachineRecipeService.reloadData` is the publication boundary for decoded
resource-pack/datapack definitions. Decoding/resource discovery is platform or
Minecraft-version integration code; it must prepare a complete candidate set
and publish it in one call. Failed candidates never partially modify the live
snapshot.

This separation is intentional: loader/event APIs must not become part of the
stable common API.

## Compatibility consequences

- BuildCraft machines, JEI compatibility and legacy addons observe one
  authoritative recipe state.
- Code registrations survive data reloads unless explicitly overridden by a
  higher-priority definition.
- Removing or breaking a datapack does not leave a half-published recipe set.
- Loader-native fluid stacks remain confined to compatibility/platform bridges.
