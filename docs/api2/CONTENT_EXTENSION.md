# API 2 high-level content extension surface

API 2 is not intended to mirror every BuildCraft implementation class. Its high-level goal is that an addon can add BuildCraft-style content without subclassing block entities or importing `buildcraft.lib`, `buildcraft.factory`, `buildcraft.transport`, etc.

The convenience entry point is:

```java
ContentRegistrar bc = BuildCraftContent.addon("examplemod");
```

`ContentRegistrar` is deliberately thin sugar over the same public registries and services exposed by `BuildCraftApi`. Advanced addons can still use the lower-level contracts directly.

## Guide Book content

```java
ResourceLocation section = bc.id("industry");
bc.guideSection(GuideSection.builder(section, "examplemod.guide.industry")
    .icon(bc.id("quarry_mk2"))
    .order(100)
    .build());

bc.guideEntry(GuideEntry.builder(bc.id("quarry_mk2"), section, "examplemod.guide.quarry_mk2")
    .icon(bc.id("quarry_mk2"))
    .page(GuidePages.textKey("examplemod.guide.quarry_mk2.text"))
    .page(GuidePages.link(bc.id("moon_oil"), "examplemod.guide.moon_oil"))
    .build());
```

Code-owned guide entries are merged into the existing Guide Book at load/open time. Standard pages are data-like so the same model can later be loaded from datapacks/resource packs without exposing BuildCraft GUI implementation.

## Distiller recipes

```java
bc.distillation("moon_crude", recipe -> recipe
    .input(FluidIngredient.exact(FluidVariant.of(id("examplemod:moon_crude")), 100))
    .gas(FluidVariant.of(id("examplemod:light_gas")), 35)
    .liquid(FluidVariant.of(id("examplemod:heavy_residue")), 65)
    .powerMj(20));
```

The high-level builder avoids raw micro-MJ and legacy `FluidStack`/`RefineryRecipeRegistry`. Distiller and Heat Exchanger runtime lookups now use `MachineRecipeService` directly, and built-in distillation/heat-exchange recipes are registered into the same API 2 service. Code-owned addon recipes therefore use the same runtime path as BCCE recipes.

## Reusing BuildCraft oil generation

```java
bc.oilInDimension("moon_oil", id("examplemod:moon"), 0.75);
```

This registers a `ResourceDepositRule` referencing the stable `BuildCraftContentIds.Worldgen.STANDARD_OIL` profile. The rule is loader-neutral and can additionally target biome ids/tags through `WorldTargetSelector`.

The Forge 1.19.2/1.20.1 and NeoForge 1.21.1 runtimes now install the BuildCraft oil placed feature into biomes through a loader-specific biome-modifier bridge. The feature itself performs the authoritative API 2 rule check at placement time, so an addon can opt an exact custom dimension into standard BuildCraft oil generation with only `oilInDimension(...)`; it does not need its own Forge/NeoForge biome-modifier JSON. A future Fabric target will need an equivalent platform bridge.

## Machine variants / Quarry Mk2

A machine definition is composition plus typed properties, not a block-entity superclass.

```java
bc.machineVariant("quarry_mk2", BuildCraftContentIds.Machines.QUARRY, machine -> machine
    .property(BuiltInMachineProperties.WORK_SPEED_MULTIPLIER, 2.0)
    .property(BuiltInMachineProperties.MAX_MJ_INPUT_PER_TICK, MjAmount.ofMj(512))
    .component(bc.id("overdrive_component")));
```

`machineVariant` copies the registered base definition first. Therefore every component/property not explicitly changed remains identical to the standard Quarry definition. Addon-specific behaviour is added as a `MachineComponentType` instead of subclassing `TileQuarry`.

`BuiltInMachineProperties` currently defines the common reusable settings:

- work speed multiplier;
- energy-cost multiplier;
- maximum MJ input per tick;
- MJ capacity;
- inventory slot count;
- range;
- chunk-loading flag.

Addons may register their own typed `MachineProperty<?>` keys for their own reusable components.

BCCE now registers Quarry, Distiller, Mining Well and Pump as built-in `MachineType` definitions. Their existing runtime implementations consume the reusable energy/capacity/speed/cost/chunk-loading properties from those definitions. This makes archetype overrides meaningful to the built-in runtime. A completely new addon-owned placeable machine still requires the later generic machine factory/executor bridge; addons do not need to subclass BCCE block entities for the definition layer.

## Pipe variants

`PipeType` now has the same copy/override model:

```java
bc.pipeVariant("fast_gold_pipe", standardGoldPipeId, pipe -> pipe
    .component(bc.id("extra_filter")));
```

Profiles and existing components are inherited by copy; addons override only the parts that differ.

## Escape hatch

When a high-level helper does not exist, addons can still register any public type without importing implementation code:

```java
bc.register(BuildCraftRegistries.ROBOT_BOARD_TYPES, board.id(), board);
```

If common addon code repeatedly needs an implementation package, that is considered an API gap and should be solved by adding a public contract rather than exposing the implementation class.
