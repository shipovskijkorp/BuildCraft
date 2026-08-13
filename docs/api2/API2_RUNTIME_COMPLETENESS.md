# API 2 runtime completeness

API 2 is not considered complete merely because an addon-facing type exists. A public registry, service or lifecycle phase must have a production runtime consumer on every maintained target before it is advertised as a supported extension point.

## Lifecycle

The production runtime now follows the complete monotonic lifecycle:

1. `DISCOVERY`
2. `TYPE_REGISTRATION`
3. `CONTENT_REGISTRATION`
4. `FROZEN`
5. `RUNNING`

`BCLibRegistries.fmlPreInit()` enters content registration before built-in content is registered. `BCLibRegistries.fmlPostInit()` is queued from the loader's load-complete event and freezes every API registry before entering `RUNNING`.

`BuildCraftApiRuntime.bootstrap()` is deliberately idempotent after discovery because BuildCraft is distributed as one jar but its historical modules are still constructed independently by the loader. A later module may therefore call the bootstrap hook after Lib has already entered content registration; that call must never move the lifecycle backwards.

After `FROZEN`, registry mutation and replacement of runtime services are rejected.

## Live services

The production runtime installs consumers for the public service keys that remain advertised, including:

- world rules and permission-aware world operations;
- diagnostics;
- client/pipe/statement/parameter presentation lookup;
- loader-neutral item, fluid and external-energy transfer bridges;
- MJ energy services;
- robots, requests and automation;
- statements, filler patterns, machines, laser targets, schematics, content-extension domains and the other migrated API2 services.

Forge and NeoForge capabilities remain behind the platform service boundary. Common addon-facing API contracts do not need to import loader capability types.

## Robot extension points

The robot registries are now runtime-backed rather than descriptive-only metadata:

- `ROBOT_RESOURCE_TYPES` dispatches acquisition through each registered `RobotResourceAcquirer`;
- the built-in `BlockRobotResource` is registered through the same path used by addon resources;
- `ROBOT_TASK_TYPES` is consulted before an API task may be assigned to a robot and verifies the runtime task class;
- `ROBOT_DOCK_PORT_TYPES` resolves custom dock ports through `DockPortResolver` and `RobotDockContext`;
- built-in item, fluid, MJ and external-energy dock ports are registered in that registry.

This keeps the registry itself authoritative instead of maintaining a second hard-coded list in the runtime.

## Retired pre-release surface

Several API2 names existed during development before a real backend had been wired. Because API2 has not yet been released as a stable public contract, those names were removed instead of preserving non-functional extension points:

- payload/network service types;
- chipset types;
- generic pipe-event types.

They may be introduced again only together with a production consumer, persistence/network semantics where applicable, and fixture/runtime coverage. Reusing an old development shape is not a compatibility requirement.

## Verification

`scripts/validate-api2-runtime-completeness.py` guards the runtime-specific invariants above. It complements the structural API boundary checks rather than replacing them.

The cross-target integrity validator additionally rejects unresolved Java build-metadata placeholders, production `.jsonx` files, missing production facade-swap recipes, disabled snapshot rendering, accidental NeoForge common-to-client class references, and loss of the client/GameTest/compatibility CI coverage.
