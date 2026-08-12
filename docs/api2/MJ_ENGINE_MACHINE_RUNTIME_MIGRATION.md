# API 2 Runtime Migration: MJ, Engines, Machines and Lasers

This migration retires the old public `buildcraft.api.mj` package and makes the
API 2 energy/machine contracts the supported extension boundary.

## Public addon boundary

Addon common code should use:

- `BuildCraftServices.ENERGY` for MJ port discovery, connection policy and MJ storage construction;
- `MjPort` / `MjStorage` with `OperationMode.SIMULATE` and `OperationMode.EXECUTE`;
- `EngineType` / `EngineView` for engine metadata and runtime state;
- `BuildCraftServices.MACHINES` / `MachineView` for live machine discovery;
- `BuildCraftServices.LASER_TARGETS` / `LaserTarget` for laser-table energy targets;
- `ExternalEnergyPort` for loader-neutral external-energy integration.

No addon should import native BuildCraft MJ capability interfaces, engine tile
classes, laser tile classes, or loader capability classes.

## Internal compatibility layer

Forge/NeoForge native MJ capability objects remain under
`buildcraft.lib.internal.mj`. They exist only so existing BCCE implementation
and loader capability lookup keep working while runtime systems migrate. They
are not a supported addon API and may be rewritten or removed without an API
major-version change.

`MjApi2PlatformBridge` adapts those native endpoints (and optional FE endpoints)
to API 2 `MjPort` objects. Public connection rules are evaluated before the
platform compatibility check.

## Engines

The BC8 engine base implements `EngineView` and `MjPortProvider`. Engine output
is sent through `EnergyService` using a simulate/execute transaction rather than
directly locating a public `IMjReceiver`.

The FE Engine additionally exposes an `ExternalEnergyPort` input. The MJ Dynamo
is intentionally asymmetric: it exposes MJ input ports and an external-energy
output port; it does not pretend that its FE output is an MJ output port.

Built-in engine archetypes are registered as `EngineType` entries for Redstone,
Stone, Iron, Creative, FE Engine and MJ Dynamo.

## Machines

Quarry, Distiller, Mining Well and Pump expose `MachineRuntimeView`, making their
live block entities discoverable through `MachineService`. Their existing
loader-native MJ endpoints are adapted to `MjPort` behind the internal platform
bridge.

This does not require addon code to subclass those block entities. Machine
archetypes remain the public mechanism for BuildCraft-style variants.

## Lasers

Laser tables implement the public `LaserTarget` contract. Lasers discover them
through `LaserTargetService` and transfer MJ through the target `MjPort` using
simulate/execute semantics. The old `ILaserTarget` / `ILaserTargetBlock` API is
removed.

## Compatibility rule

Java compatibility for `buildcraft.api.mj` is intentionally not preserved.
World/save compatibility is separate: registry IDs, NBT keys and aliases remain
unchanged unless a dedicated data migration exists.
