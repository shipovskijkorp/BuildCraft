#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_PARTS = {"build", ".gradle", ".git"}


def text(path: str) -> str:
    p = ROOT / path
    if not p.is_file():
        raise FileNotFoundError(path)
    return p.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    # The old Transport namespace is retired, not a compatibility facade.
    for path in ROOT.rglob("*.java"):
        if any(part in SKIP_PARTS for part in path.parts):
            continue
        src = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)
        if re.search(r"^\s*package\s+buildcraft\.api\.transport(?:\.|;)", src, re.MULTILINE):
            errors.append(f"{rel}: retired buildcraft.api.transport source remains")
        if re.search(r"^\s*import\s+(?:static\s+)?buildcraft\.api\.transport\.", src, re.MULTILINE):
            errors.append(f"{rel}: imports retired buildcraft.api.transport")

    internal_roots = [
        ROOT / "source-shared/src/main/java/buildcraft/transport/internal",
        ROOT / "source-platforms/forge/src/main/java/buildcraft/transport/internal",
        ROOT / "source-platforms/neoforge/src/main/java/buildcraft/transport/internal",
        ROOT / "source-families/modern/src/main/java/buildcraft/transport/internal",
        ROOT / "version-src/1.19.2-forge/src/main/java/buildcraft/transport/internal",
        ROOT / "version-src/1.20.1-forge/src/main/java/buildcraft/transport/internal",
    ]
    internal_count = sum(1 for root in internal_roots if root.is_dir() for _ in root.rglob("*.java"))
    if internal_count == 0:
        errors.append("Transport internals were not internalized under buildcraft.transport.internal")

    for path in [
        "version-src/1.19.2-forge/src/main/java/buildcraft/transport/BCTransport.java",
        "version-src/1.20.1-forge/src/main/java/buildcraft/transport/BCTransport.java",
        "source-platforms/neoforge/src/main/java/buildcraft/transport/BCTransport.java",
    ]:
        try:
            src = text(path)
        except FileNotFoundError:
            errors.append(f"{path}: missing active Transport bootstrap")
            continue
        if "TransportApi2.install();" not in src:
            errors.append(f"{path}: does not install the API2 PipeService")

    pipe_paths = [
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/Pipe.java",
        "source-platforms/neoforge/src/main/java/buildcraft/transport/pipe/Pipe.java",
    ]
    for path in pipe_paths:
        src = text(path)
        for token in [
            "implements IPipe, IDebuggable, PipeMutationContext",
            "applyItemRouting(",
            "applyFluidRouting(",
            "applyMjRouting(",
            "applyExternalEnergyRouting(",
            "routingWeight()",
            "PipeActivationComponent",
            "PipeConnectionComponent",
            "PIPE_CONNECTION_RULES",
            "PIPE_COMPONENT_TYPES",
        ]:
            if token not in src:
                errors.append(f"{path}: missing API2 runtime hook {token}")

    flow_checks = {
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowItems.java": ["applyItemRouting(", "requireItemProfile()", "maxItemsPerCycle()"],
        "source-platforms/neoforge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowItems.java": ["applyItemRouting(", "requireItemProfile()", "maxItemsPerCycle()"],
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowFluids.java": ["requireFluidProfile()", "applyFluidRouting("],
        "source-platforms/neoforge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowFluids.java": ["requireFluidProfile()", "applyFluidRouting("],
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowPower.java": ["mjProfile()", "applyMjRouting("],
        "source-platforms/neoforge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowPower.java": ["mjProfile()", "applyMjRouting("],
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowForgeEnergy.java": ["externalEnergyProfile()", "applyExternalEnergyRouting("],
        "source-platforms/neoforge/src/main/java/buildcraft/transport/pipe/flow/PipeFlowForgeEnergy.java": ["externalEnergyProfile()", "applyExternalEnergyRouting("],
    }
    for path, tokens in flow_checks.items():
        src = text(path)
        for token in tokens:
            if token not in src:
                errors.append(f"{path}: missing authoritative API2 transport hook {token}")

    for path in [
        "version-src/1.19.2-forge/src/main/java/buildcraft/transport/block/BlockPipeHolder.java",
        "version-src/1.20.1-forge/src/main/java/buildcraft/transport/block/BlockPipeHolder.java",
        "source-platforms/neoforge/src/main/java/buildcraft/transport/block/BlockPipeHolder.java",
    ]:
        src = text(path)
        if "activateApiComponents(" not in src:
            errors.append(f"{path}: pipe activation bypasses API2 components")

    for path in [
        "version-src/1.19.2-forge/src/main/java/buildcraft/transport/item/ItemPipeHolder.java",
        "version-src/1.20.1-forge/src/main/java/buildcraft/transport/item/ItemPipeHolder.java",
        "source-families/modern/src/main/java/buildcraft/transport/item/ItemPipeHolder.java",
    ]:
        src = text(path)
        if "getApiType()" not in src:
            errors.append(f"{path}: tooltip does not read API2 transport profiles")
        if any(old in src for old in ("getFluidTransferInfo", "getPowerTransferInfo", "getForgeEnergyTransferInfo")):
            errors.append(f"{path}: tooltip still reads legacy PipeApi transfer metadata")

    service = text("source-shared/src/main/java/buildcraft/transport/api2/PipeServiceImpl.java")
    for token in ["ensureRuntimeDefinition", "createItem(", "itemPipePort(", "placeAttachment(", "removeAttachment("]:
        if token not in service:
            errors.append(f"PipeServiceImpl: missing runtime bridge {token}")

    for path in [
        "source-platforms/forge/src/main/java/buildcraft/transport/pipe/PipeRegistry.java",
        "source-families/modern/src/main/java/buildcraft/transport/pipe/PipeRegistry.java",
    ]:
        src = text(path)
        for token in ["PipeTypeBridge.ensureRegistered", "ensureRuntimeDefinition("]:
            if token not in src:
                errors.append(f"{path}: missing API2 definition bridge {token}")

    fixture = text("addon-fixture/src/main/java/dev/bcce/apifixture/EasyContentFixtureAddon.java")
    if "pipeVariant(" not in fixture or ".createItem(" not in fixture:
        errors.append("EasyContentFixtureAddon does not prove API2 pipe variant + placeable Item usage")

    if errors:
        print("Transport API2 migration validation FAILED:")
        for error in errors:
            print(f" - {error}")
        return 1

    print(f"Transport API2 migration OK: {internal_count} internalized Transport Java source(s), old public Transport namespace absent")
    return 0


if __name__ == "__main__":
    sys.exit(main())
