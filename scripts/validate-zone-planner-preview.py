#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *needles: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [needle for needle in needles if needle not in text]
    if missing:
        raise SystemExit(f"{path}: missing " + ", ".join(repr(item) for item in missing))


for family in ("legacy", "modern"):
    renderer = f"source-families/{family}/src/main/java/buildcraft/robotics/client/render/RenderZonePlanner.java"
    require(
        renderer,
        "TEXTURE_WIDTH = 10",
        "TEXTURE_HEIGHT = 8",
        "BLOCKS_PER_PIXEL = 4",
        "new ZonePlannerMapChunk(",
        "level.dimension().location().hashCode()",
        "LightTexture.FULL_BRIGHT",
        "RenderType.entityCutoutNoCull(preview.location)",
        "expireAfterAccess(30, TimeUnit.SECONDS)",
    )
    text = (ROOT / renderer).read_text(encoding="utf-8")
    if "ZonePlannerMapDataClient" in text or "MessageZoneMapRequest" in text:
        raise SystemExit(f"{renderer}: block preview must not use remote GUI-map requests")

for path in (
    "version-src/1.19.2-forge/src/main/java/buildcraft/robotics/BCRobotics.java",
    "version-src/1.20.1-forge/src/main/java/buildcraft/robotics/BCRobotics.java",
    "source-platforms/neoforge/src/main/java/buildcraft/robotics/BCRobotics.java",
):
    require(
        path,
        "import buildcraft.robotics.client.render.RenderZonePlanner;",
        "event.registerBlockEntityRenderer(BCRoboticsBlocks.ZONE_PLANNER_TILE.get(), RenderZonePlanner::new);",
    )

print("Zone Planner block preview parity OK: 10x8 BC8-style local terrain preview registered on all maintained targets")
