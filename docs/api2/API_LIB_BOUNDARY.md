# API 2 / BuildCraft Lib boundary

## Rule

`buildcraft.api.v2` is the **supported external extension contract**.
`buildcraft.lib` and all gameplay module packages are **implementation**.

The intended dependency direction is:

```text
addon ---------------> buildcraft.api.v2
                           ^
                           |
buildcraft.lib.internal.api.v2
                           ^
                           |
        core / transport / factory / builders / energy / robotics / silicon
```

The reverse dependency is forbidden:

```text
buildcraft.api.v2 -X-> buildcraft.lib
buildcraft.api.v2 -X-> buildcraft.transport
buildcraft.api.v2 -X-> buildcraft.factory
buildcraft.api.v2 -X-> Forge / NeoForge / Fabric
buildcraft.api.v2 -X-> net.minecraft.client
```

Vanilla common Minecraft types such as `ResourceLocation`, `BlockPos`,
`ItemStack`, `BlockState`, `Level` and `Direction` are allowed when they are the
natural domain types for the contract.

## What belongs in API 2

A type belongs under `buildcraft.api.v2` when an addon is expected to rely on it
as a stable contract. Typical examples:

- extension type descriptors and IDs (`PipeType`, `MachineType`, `EngineType`);
- runtime views/ports (`PipeView`, `MachineView`, `MjPort`, `FluidPort`);
- service contracts (`PipeService`, `MachineRecipeService`, `Guide`/world/data services);
- immutable request/result/value objects;
- codecs and persistence descriptors that an addon supplies for its own state;
- client **presentation metadata**, not BuildCraft renderer implementations;
- testkit utilities intentionally supplied to addon authors.

Pure, deterministic helpers may remain public when they are useful to addon
code and do not couple callers to BuildCraft implementation state.

## What belongs in Lib

A type belongs in `buildcraft.lib` when it describes how BCCE implements a
contract rather than what an addon is allowed to do. Examples:

- authoritative registry implementations and freeze machinery;
- reload transaction engines and last-known-good publication state;
- persistence registry implementations;
- concrete MJ storage implementation;
- concrete API service backends;
- legacy adapters used while old BCCE code is migrated;
- networking, GUI, rendering, caches, topology solvers, block-entity helpers,
  chunk-loading machinery and other shared implementation utilities.

The API backend lives under the explicit namespace:

```text
buildcraft.lib.internal.api.v2
```

Classes there may need Java `public` visibility because BCCE modules live in
separate packages, but they are **not** supported addon API and may change
without an API-major bump.

## Runtime attachment

`BuildCraftApi` no longer exposes a public `install(ApiRuntime)` mutation hook.
The public facade discovers the BCCE runtime through Java `ServiceLoader`
using the public read-only `ApiRuntime` contract itself as the service type.
The only provider shipped by BCCE is:

```text
buildcraft.lib.internal.api.v2.BuildCraftApiRuntimeProvider
```

This keeps runtime construction/bootstrap inside Lib while allowing the public
API package to compile without any BuildCraft implementation dependency. There
is no implementation-only SPI package under `buildcraft.api.v2`; addons simply
consume `BuildCraftApi` / `ApiRuntime` and never provide the runtime service.

## Internalized API2 implementation classes

The following implementation mechanisms were removed from the supported API2
surface in this split:

- `ImmutableApiFeatureSet` -> `ApiFeatureSet.of(...)` public value factory;
- `MjBuffer` -> internal implementation behind `MjStorage` and
  `EnergyService.createStorage(...)`;
- `SimpleApiRegistry` -> internal registry backend;
- obsolete `RegistryBuilder` / `RegistrySnapshot` / `SimpleRegistryBuilder`;
- `ReloadableDefinitionRegistry`, `ReloadTransaction`, `ReloadPhase`,
  `DefinitionValidator` -> internal reload engine;
- `PersistenceRegistryBuilder`, `PersistenceRegistrySnapshot` -> internal
  persistence engine;
- bulk datapack publication records/results for fuels and machine recipes ->
  internal reload backend. Addons keep the stable `register(...)` and read-only
  `snapshot()` service surface.

Read-only results that addons legitimately inspect, such as
`DefinitionSnapshot`, `ResolvedDefinition`, `ReloadDiagnostic` and
`DefinitionProvenance`, remain public.

## Enforcement

`scripts/validate-api-v2.py` enforces this boundary in CI. It checks that:

1. API2 has no BuildCraft implementation/legacy imports;
2. API2 has no Forge, NeoForge, Fabric or Minecraft client imports;
3. internalized implementation classes do not reappear under the public API;
4. the addon fixture imports only supported API2 packages;
5. the old ambiguous `buildcraft.lib.api.v2` implementation namespace stays empty;
6. every public registry key is actually created by the Lib runtime;
7. public API registries/services do not expose internal lifecycle controls such
   as `freeze()` or bulk `reloadData(...)` publication;
8. `BuildCraftApi` does not regain a public runtime-install mutation hook;
9. the `ApiRuntime` ServiceLoader descriptor points to exactly the internal BCCE provider.

This boundary is independent from save compatibility. Registry IDs, aliases,
NBT keys and schema migrations remain stable even when a Java implementation
class moves or disappears.
