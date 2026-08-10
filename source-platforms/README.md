# Platform source layers

Loader-specific code lives here and is shared across Minecraft versions of the
same loader. Small Minecraft-version differences inside a platform file may use
the repository's Stonecutter-style `//? if ...` directives. Structural loader
differences belong in separate `forge`, `neoforge`, or `fabric` trees.
