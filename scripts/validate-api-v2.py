#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
API_ROOTS = [
    ROOT / "source-shared/src/main/java/buildcraft/api/v2",
    ROOT / "source-families/legacy/src/main/java/buildcraft/api/v2",
    ROOT / "source-families/modern/src/main/java/buildcraft/api/v2",
]
FIXTURE_ROOT = ROOT / "addon-fixture/src/main/java"
REGISTRY_KEYS_FILE = ROOT / "source-shared/src/main/java/buildcraft/api/v2/BuildCraftRegistries.java"
RUNTIME_FILE = ROOT / "source-shared/src/main/java/buildcraft/lib/api/v2/BuildCraftApiRuntime.java"

FORBIDDEN_API_IMPORT_PREFIXES = (
    "net.minecraftforge.",
    "net.neoforged.",
    "net.fabricmc.",
    "net.minecraft.client.",
)
IMPORT_RE = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)", re.MULTILINE)
PUBLIC_STATIC_FIELD_RE = re.compile(
    r"\bpublic\s+static\s+(?!final\b)(?:[\w<>?,.\[\] ]+\s+)?[A-Za-z_$][\w$]*\s*(?:=|;)",
    re.MULTILINE,
)
REGISTRY_KEY_RE = re.compile(
    r"^\s*public\s+static\s+final\s+RegistryKey<.*>\s+([A-Z0-9_]+)\s*=",
    re.MULTILINE,
)
RUNTIME_REGISTRY_RE = re.compile(r"registerRegistry\(BuildCraftRegistries\.([A-Z0-9_]+)\)\s*;")


def java_files(root: Path):
    if root.is_dir():
        yield from sorted(root.rglob("*.java"))


def main() -> int:
    errors: list[str] = []
    api_files = [path for root in API_ROOTS for path in java_files(root)]
    if not api_files:
        errors.append("No API v2 Java sources found")

    for path in api_files:
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)
        for imported in IMPORT_RE.findall(text):
            if imported.startswith(FORBIDDEN_API_IMPORT_PREFIXES):
                errors.append(f"{rel}: forbidden loader/client import {imported}")
            if imported.startswith("buildcraft.") and not imported.startswith("buildcraft.api.v2."):
                errors.append(f"{rel}: API v2 imports BuildCraft implementation/legacy API: {imported}")
        for match in PUBLIC_STATIC_FIELD_RE.finditer(text):
            line = text.count("\n", 0, match.start()) + 1
            errors.append(f"{rel}:{line}: public writable static field is forbidden")

    fixture_files = list(java_files(FIXTURE_ROOT))
    if not fixture_files:
        errors.append("addon-fixture has no Java sources")
    for path in fixture_files:
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)
        for imported in IMPORT_RE.findall(text):
            if imported.startswith("buildcraft.") and not imported.startswith("buildcraft.api.v2."):
                errors.append(f"{rel}: fixture imports non-v2 BuildCraft class {imported}")
            if imported.startswith(("net.minecraftforge.", "net.neoforged.", "net.fabricmc.")):
                errors.append(f"{rel}: fixture common code imports loader API {imported}")

    if REGISTRY_KEYS_FILE.is_file() and RUNTIME_FILE.is_file():
        registry_keys = set(REGISTRY_KEY_RE.findall(REGISTRY_KEYS_FILE.read_text(encoding="utf-8")))
        runtime_keys = set(RUNTIME_REGISTRY_RE.findall(RUNTIME_FILE.read_text(encoding="utf-8")))
        for name in sorted(registry_keys - runtime_keys):
            errors.append(f"BuildCraftRegistries.{name} is not created by BuildCraftApiRuntime")
        for name in sorted(runtime_keys - registry_keys):
            errors.append(f"BuildCraftApiRuntime registers unknown BuildCraftRegistries.{name}")
    else:
        errors.append("BuildCraftRegistries.java or BuildCraftApiRuntime.java is missing")

    if errors:
        print("API v2 boundary validation FAILED:")
        for error in errors:
            print(f" - {error}")
        return 1

    print(f"API v2 boundary OK: {len(api_files)} API file(s), {len(fixture_files)} fixture file(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
