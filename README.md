# BuildCraft Community Edition

## About the project

BuildCraft Community Edition combines the newer foundation of **BuildCraft 8.0.0** with mechanics that were present in **BuildCraft 7.1.27** but were not completed or carried over into BC8, including robotics and building-related features.

The goal of the project is to preserve and continue the classic BuildCraft experience on newer Minecraft versions, restoring missing content and completing unfinished ideas from the original mod without changing its core identity.

## Issue reports

When reporting a problem, always include:

- the Minecraft version;
- the mod loader and its version;
- the exact BuildCraft Community Edition build;
- the relevant log or crash report.

## Roadmap 2.0

- Drop support for 1.21.1 Forge due to low usage and high maintenance overhead
- New API system - in progress
- FE/RF compat - in progress
- Port to 1.20.1 Fabric
- Port to 1.21.11 Fabric/NeoForge
- Port to 26.X Fabric/NeoForge

## Supported versions

- Minecraft 1.19.2 — Forge
- Minecraft 1.20.1 — Forge
- Minecraft 1.21.1 — NeoForge
- Minecraft 1.21.1 — Forge (support will be discontinued)

## Multi-version source architecture

BCCE is split into two generation-based Stonecutter source families:

- **legacy** — Minecraft 1.19.2 and 1.20.1;
- **modern** — Minecraft 1.21.1+ targets.

Files identical everywhere live in `source-shared`; generation-specific common code lives in `source-families/legacy` or `source-families/modern`; only real Minecraft/loader differences remain in `version-src/<target>`. The 1.19.2 implementation is the gameplay reference, but source code is allowed to differ when newer Minecraft APIs require a different implementation. The compatibility target is player-visible behaviour: **different implementation, indistinguishable BuildCraft**.

See `SOURCE_FAMILIES.md` for layout rules, parity policy and build commands.

## Addons developed by BCCE team:

- **BuildCraft Community Edition Localizations**
  - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/buildcraft-community-edition-localizations) [Modrinth](https://modrinth.com/mod/buildcraft-community-edition-localizations) [GitHub](https://github.com/CurativeTree/BuildCraft/tree/Localizations)

## Credits

### Original BuildCraft

- GitHub: https://github.com/BuildCraft/BuildCraft
- CurseForge: [BuildCraft](https://www.curseforge.com/minecraft/mc-mods/buildcraft)
- Modrinth: [BuildCraft](https://modrinth.com/mod/buildcraft)

Special thanks to the original BuildCraft team and all contributors who made BuildCraft one of the most iconic technical Minecraft mods.

BuildCraft Community Edition is an unofficial community port based on BuildCraft 8.0.0 and BuildCraft 7.1.27. All original BuildCraft work belongs to its respective authors and contributors.

### Community Edition port

Developed and ported by:

- CurativeTree
- ShipovskijKorp

Thanks for helping with development:

- nightovl
- pietruszka
- Jimmy