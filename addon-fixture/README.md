# API v2 consumer fixture

This source tree is compile-only. It sees the isolated `buildcraft.api.v2`
output and Minecraft common classes, but not BuildCraft implementation classes
and not Forge/NeoForge/Fabric APIs.

The fixture now exercises both the low-level contracts and the high-level content
extension surface: permissions, fuels/coolants, machine recipes, pipe types, typed
statements, signal channels, machines/engines, robot content descriptors, client
presentation metadata, Guide Book entries, worldgen rules, Distiller builders and
copy/override machine variants such as a Quarry Mk2.

`EasyContentFixtureAddon` is the acceptance example for the intended addon UX.
It demonstrates all of the following without a single BuildCraft implementation import:

- add a Guide Book section and entry;
- reuse standard BuildCraft oil generation in a custom dimension;
- add a Distiller recipe;
- derive a Quarry Mk2 from the standard Quarry definition and override only speed/energy;
- attach an addon-owned machine component.

If any of those examples starts requiring an implementation import, the
`addonFixture` compile task fails.


The fixture is also the API/Lib boundary canary: imports from `buildcraft.lib.*`, or
BuildCraft gameplay-module packages are forbidden.
If an addon needs one of those packages, the public extension API is missing a
contract and should be expanded instead of exposing the implementation class.
