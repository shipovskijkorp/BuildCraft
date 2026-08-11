# API 2 migration coverage

This document maps the legacy BCCE API concepts to the contract that should be
used while migrating the implementation.

Current implementation audit: **224** unique legacy API imports are still used
by BCCE implementation code. **211** are classified `MIGRATE` to API 2 and
**13** are classified `INTERNALIZE`; **0** imports are left without a migration
destination.

| Legacy/current concept | API 2 replacement |
|---|---|
| `BuildCraftAPI.worldProperties`, soft blocks | `WorldPropertyService`, `WorldRuleService` |
| fake-player provider / player-owned automation | `ActorService`, `AutomationActor`, `OwnedView`, permissions |
| `BCModules` | `ModuleService`, `BuildCraftModules` |
| custom rotation / paint / wrench | `RotationHandler`, `PaintHandler`, `BlockInteractionService`, `WrenchService` |
| `IBox`, `IZone`, `IPathProvider`, area providers | `BlockBox`, `Zone`, `Path`, `AreaProvider` |
| `IStackFilter`, list handlers | `ItemMatcher`, `ItemList`, `ListMatchAdapter`, `ItemListService` |
| filtered/slotted inventories | `SlottedItemPort`, `ItemTransferPolicy` |
| `INamedItem` | `ItemLabelAdapter`, `ItemLabelService` |
| `IMapLocation` and map-location variants | `MapLocationView`, `MapLocationAdapter`, `MapLocationService` |
| `IRequestProvider` and index-based robot requests | `ItemRequest`, `RequestProvider`, `RequestService` |
| `IDebuggable` | `DebugContributor`, `DebugService` |
| `FluidItemDrops` / fluid shard hooks | `FluidDropProvider`, `FluidDropService` |
| old MJ capability family / `MjBattery` | `MjPort`, `MjBuffer`, `MjPortDescriptor`, `MjConnectionRule`, `EnergyService` |
| MJ display/effects | `MjFormatter`, `PowerLossEffectService` |
| Forge/NeoForge item/fluid/energy capabilities | platform bridges to `ItemPort`, `FluidPort`, `ExternalEnergyPort` |
| fuels/coolants | `EnergyFluidService` and immutable profiles |
| integration/refinery recipes | `MachineRecipeService` |
| facade globals/interfaces | `FacadeRuleService`, `FacadeMaterialAdapter`, `FacadeService` |
| crops/templates | existing API 2 crop/template services |
| filler registry and statement-backed patterns | `FillerPatternType`, `FillerPatternService` |
| `StatementManager`, `IAction*`, `ITrigger*`, raw parameter arrays | typed parameter/action/trigger registries and `StatementService` |
| `IGate` | `GateProgram`, `GateRule`, `GateView`, `GateControl` |
| `IWireEmitter`, `IWireManager`, internal `WireNode` | typed `SignalChannelType`, `SignalPort`, `SignalNetworkView` |
| Stripes handlers | `AutomationRequest`/`AutomationService`, `StripesHandler` |
| `PipeDefinition`, `PipeFlow`, `PipeBehaviour` | `PipeType`, transport profiles, component types and narrow component hooks |
| `IInjectable` / direct travelling-item insertion | `ItemPipePort`, `ItemInjectionRequest`, `PipeService.injectItem` |
| `PipePluggable` | `PipeAttachmentType`, `PipeAttachment` |
| `PipeConnectionAPI` | `PipeConnectionRule` registry |
| reflected pipe event bus | `PipeEventType` + typed listeners |
| pipe client hooks | data-only client presentations plus implementation renderer adapters |
| extensible engine enum | `EngineType` registry |
| chipset enum | `ChipsetType` registry |
| tile controllable/work/heatable interfaces | `MachineView`, `MachineControl`, `MachineControlMode`, `WorkStatus`, `HeatPort`, `HeatProfile` |
| laser targets/table enums | `LaserTarget`, `LaserTargetService`, `LaserTableType` |
| robot AI/entity inheritance API | `RobotTaskType`, `RobotTask`, `RobotHandle`, `RobotControl` |
| robot resource reflection persistence | `RobotResourceType` + persistent codec |
| docking station monolith | `RobotDock` + typed `DockPortType` |
| robot boards | `RobotBoardType` |
| reflected/global robot events | `RobotEventContext`, `RobotEventListener`, `RobotService.evaluateEvent` |
| schematic factory registries | `SchematicAdapter`, `SnapshotElementType`, `SnapshotKind`, `SchematicService` |
| Builder inventory-copy string paths | `DataPath`, `InventoryCopyPolicy` |
| raw addon packet registration | `PayloadType`, `NetworkService` |
| common API client renderer references | `buildcraft.api.v2.client` presentation metadata |

The exact implementation-level inventory is tracked in
`LEGACY_IMPORT_MIGRATION_MAP.csv`. The CI migration-surface validator requires
every remaining non-v2 `buildcraft.api.*` import in BCCE implementation source
to have either a concrete API 2 replacement (`MIGRATE`) or an explicit
implementation-only destination (`INTERNALIZE`). New unmapped legacy
dependencies fail validation.

Once the implementation contains no imports from the corresponding legacy
package, that legacy Java surface can be deleted immediately. Persistence
aliases are tracked separately and must not be removed with the Java types.
