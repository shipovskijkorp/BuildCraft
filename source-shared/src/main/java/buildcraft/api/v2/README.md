# BuildCraft Extension API 2

Current implemented foundation:

- runtime facade, typed service keys and lifecycle contracts;
- immutable/frozen registration primitives and diagnostics;
- versioned persistence, aliases/migrations and UnknownPayload preservation;
- atomic last-known-good reload snapshots;
- loader-neutral item/fluid transfer and fluid identity/value model;
- automation actor / deterministic permission-provider registry;
- first production domain: fuels and coolants through `EnergyFluidService`;
- isolated API-v2/API-v2-sources artifacts, compile-only addon fixture and API boundary validator.

The legacy `buildcraft.api` tree remains a compatibility surface and is migrated
domain by domain. Loader-specific Forge/NeoForge/Fabric types are forbidden in
`buildcraft.api.v2` common code.
