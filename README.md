# BuildCraft Community Edition

## About the project

BuildCraft Community Edition combines the newer foundation of **BuildCraft 8.0.0** with mechanics that were present in **BuildCraft 7.1.27** but were not completed or carried over into BC8, including robotics and building-related features.

The goal of the project is to preserve the classic BuildCraft experience while making it available on newer Minecraft versions.

## Issue reports

When reporting a problem, always include:

- the Minecraft version;
- the mod loader and it's version;
- the exact BuildCraft Community Edition build;
- the relevant log or crash report.

## Roadmap

*   Port robotics (ready)
*   Port builders (ready)
*   Move from alpha to beta (ready)
*   Global project checkup (ready)
*   Forge release for 1.19.2 Forge (ready)
*   Port to 1.20.1 Forge (ready)
*   Backport to 1.12.2 (current subtask)
*   Port to 1.21.1 Forge (ready)
*   Port to 1.21.1 NeoForge (ready)
*   New api system (current task)
*   Port to Fabric
*   Port to 1.21.11+ (maybe)

## Current Status

Now in mod available 100% of content from BC8 and BC7

- 1.19.2 Forge - maintain
- 1.20.1 Forge - maintain
- 1.21.1 Forge - maintain
- 1.21.1 NeoForge - maintain
- 1.12.2 Forge - in dev, (low priority)

## Multi-version source architecture

BCCE is split into two generation-based Stonecutter source families:

- **legacy** — Minecraft 1.19.2 Forge and 1.20.1 Forge;
- **modern** — Minecraft 1.21.1+ targets, currently Forge and NeoForge.

Files identical everywhere live in `source-shared`; generation-specific common code lives in `source-families/legacy` or `source-families/modern`; only real Minecraft/loader differences remain in `version-src/<target>`. The 1.19.2 implementation is the gameplay reference, but source code is allowed to differ when newer Minecraft APIs require a different implementation. The compatibility target is player-visible behaviour: **different implementation, indistinguishable BuildCraft**.

See [`SOURCE_FAMILIES.md`](SOURCE_FAMILIES.md) for layout rules, parity policy and build commands.

## Credits

### Original BuildCraft

- GitHub: [BuildCraft/BuildCraft](https://github.com/BuildCraft/BuildCraft)
- CurseForge: [BuildCraft](https://www.curseforge.com/minecraft/mc-mods/buildcraft)
- Modrinth: [BuildCraft](https://modrinth.com/mod/buildcraft)

Special thanks to the original BuildCraft team and all contributors who made BuildCraft one of the most iconic technical Minecraft mods.

BuildCraft Community Edition is an unofficial community port based on BuildCraft 8.0.0 and BuildCraft 7.1.27. All original BuildCraft work belongs to its respective authors and contributors.

### Community Edition port

Developed and ported by:

- CurativeTree
- ShipovskijKorp

Additional development help:

- nightovl
- pietruszka
- Jimmy
