#!/usr/bin/env python3
"""Helpers for BuildCraft's generation-based layered source layout.

Each target is the union of:
  1. source-shared             - files identical across every supported target
  2. source-families/<family>  - files shared by one generation family
  3. version-src/<target>      - files unique to that Minecraft/loader target

The repository intentionally forbids overlapping relative file paths between
all maintained layers. A file belongs to exactly one layer; this keeps Gradle
and resource semantics deterministic and prevents hidden copies from creeping
back in.
"""
from __future__ import annotations

from dataclasses import dataclass
import os
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = ROOT / "stonecutter-targets.properties"


def load_properties(path: Path = PROPERTIES) -> dict[str, str]:
    result: dict[str, str] = {}
    pending = ""
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = pending + raw
        if line.endswith("\\") and not line.endswith("\\\\"):
            pending = line[:-1]
            continue
        pending = ""
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid property line in {path}: {raw!r}")
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    if pending:
        raise ValueError(f"Dangling property continuation in {path}")
    return result


@dataclass(frozen=True)
class TargetLayout:
    target: str
    family: str
    shared_root: Path
    family_root: Path
    overlay_root: Path

    @property
    def layers(self) -> tuple[Path, Path, Path]:
        return self.shared_root, self.family_root, self.overlay_root

    def resolve(self, relative: str | Path) -> Path | None:
        rel = Path(relative)
        # Overlay first for diagnostics. Overlaps are forbidden by validation,
        # so this ordering should never affect production behavior.
        for root in (self.overlay_root, self.family_root, self.shared_root):
            path = root / rel
            if path.is_file():
                return path
        return None

    def effective_files(self, relative: str | Path = ".") -> dict[str, Path]:
        rel = Path(relative)
        result: dict[str, Path] = {}
        for layer in self.layers:
            base = layer / rel
            if not base.exists():
                continue
            for path in base.rglob("*"):
                if not path.is_file():
                    continue
                key = path.relative_to(layer).as_posix()
                previous = result.get(key)
                if previous is not None:
                    raise ValueError(
                        f"{self.target}: source layer overlap for {key}: "
                        f"{previous.relative_to(ROOT)} and {path.relative_to(ROOT)}"
                    )
                result[key] = path
        return result


def target_ids(properties: dict[str, str] | None = None) -> list[str]:
    props = properties or load_properties()
    return [item.strip() for item in props.get("targets", "").split(",") if item.strip()]


def target_layout(target: str, properties: dict[str, str] | None = None) -> TargetLayout:
    props = properties or load_properties()
    prefix = f"target.{target}."
    family = props.get(prefix + "source.family", "").strip()
    shared_root = props.get(prefix + "source.shared_root", props.get("common.source.shared_root", "")).strip()
    family_root = props.get(prefix + "source.root", "").strip()
    overlay_root = props.get(prefix + "source.overlay_root", "").strip()
    if not family or not shared_root or not family_root or not overlay_root:
        raise ValueError(f"{target}: missing source.family/source.shared_root/source.root/source.overlay_root")
    return TargetLayout(
        target=target,
        family=family,
        shared_root=(ROOT / shared_root).resolve(),
        family_root=(ROOT / family_root).resolve(),
        overlay_root=(ROOT / overlay_root).resolve(),
    )


def configured_layer_paths(target: str, properties: dict[str, str] | None = None) -> tuple[Path, Path, Path]:
    layout = target_layout(target, properties)
    return tuple(path.resolve() for path in layout.layers)


def _link_or_copy(source: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    try:
        os.link(source, destination)
    except OSError:
        shutil.copy2(source, destination)


def materialize_target(
    target: str,
    destination: Path | None = None,
    properties: dict[str, str] | None = None,
) -> Path:
    """Create a conventional merged target tree for offline validators/tests.

    Files are hard-linked when possible, so this staging view is cheap and never
    becomes another maintained source copy in the repository.
    """
    props = properties or load_properties()
    layout = target_layout(target, props)
    dest = destination or (ROOT / "build" / "effective-sources" / target)
    dest = dest.resolve()
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)
    for relative, source in sorted(layout.effective_files().items()):
        _link_or_copy(source, dest / relative)
    return dest


def family_targets(properties: dict[str, str] | None = None) -> dict[str, list[str]]:
    props = properties or load_properties()
    result: dict[str, list[str]] = {}
    for target in target_ids(props):
        family = target_layout(target, props).family
        result.setdefault(family, []).append(target)
    return result


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", choices=target_ids())
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    print(materialize_target(args.target, args.output))
