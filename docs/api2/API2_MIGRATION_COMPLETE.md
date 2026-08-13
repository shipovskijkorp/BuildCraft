# BuildCraft API 2 migration complete

The Java API migration is complete. The only supported public BuildCraft Java extension namespace is `buildcraft.api.v2`.

## Final boundary

- `buildcraft.api.v2.*` is the public addon-facing Java API.
- `buildcraft.lib.internal.*` and domain-specific `*.internal.*` packages are implementation details and are not addon contracts.
- There is no legacy Java API compatibility facade and no parallel `buildcraft.api.*` surface outside `v2`.
- The isolated `apiV2` source set and `addon-fixture` remain verification boundaries: public consumers must compile without importing BuildCraft implementation classes or loader APIs into common API code.

## Compatibility policy

Removal of the old Java surface does not mean world data is intentionally broken. Save and registry compatibility remains a separate concern: persisted IDs, aliases, schema migrations, NBT/component compatibility and unknown-payload preservation continue to be maintained where required by the runtime migration rules.

Source compatibility for unpublished legacy addon Java contracts is not preserved. New integrations must target API 2.

## CI invariants

The repository enforces the completed state with two independent checks:

1. `validate-api-v2.py` verifies the loader-neutral public/API implementation boundary and the isolated addon fixture.
2. `validate-api-v2-only.py` rejects any non-v2 `buildcraft.api` package/import/source path and verifies that Gradle and CI use the final API2-only gate rather than the temporary migration ledger.

The Stage 7-9 migration ledger and burn-down documents were intentionally removed after completion. They described a temporary transition state and are no longer authoritative; the source tree and the final validators are.

## Runtime-completeness follow-up

The migration boundary is now also guarded for runtime completeness. Public API2 registries and services must be backed by production consumers rather than existing only as compile-time surface area. The lifecycle reaches `FROZEN` and `RUNNING` in normal loader startup, robot resource/task/dock registries dispatch through registered extension types, and loader-native transfer capabilities are exposed only through the loader-neutral platform service.

Development-only API2 names that had no production backend (the provisional payload/network, chipset and generic pipe-event surfaces) were retired before the first stable API2 release instead of being frozen as non-functional contracts.

See [`API2_RUNTIME_COMPLETENESS.md`](API2_RUNTIME_COMPLETENESS.md) for the runtime contract and its validation policy.
