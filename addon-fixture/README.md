# API v2 consumer fixture

This source tree is compile-only. It deliberately sees the isolated API v2
output and Minecraft dependencies, but not BuildCraft implementation classes.

The fixture currently proves that an addon can register a permission provider,
fuel, and coolant with only `buildcraft.api.v2` imports. It is compiled without
`sourceSets.main.output`, so implementation imports fail at compile time. Future
API domains extend this same fixture.
