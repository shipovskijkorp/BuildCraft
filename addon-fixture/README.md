# API v2 consumer fixture

This source tree is compile-only. It sees the isolated `buildcraft.api.v2`
output and Minecraft common classes, but not BuildCraft implementation classes
and not Forge/NeoForge/Fabric APIs.

The fixture now exercises both the production API and the complete migration
surface: permissions, fuels/coolants, machine recipes, pipe types, typed
statements, signal channels, machines/engines, robot content descriptors and
client presentation metadata.

If any of those examples starts requiring an implementation import, the
`addonFixture` compile task fails.
