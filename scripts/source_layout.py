#!/usr/bin/env python3
"""BuildCraft multi-generation source layout and conditional preprocessor.

Every target is materialised from four maintained layers:

  1. source-shared                files valid for every supported target
  2. source-families/<family>     generation-wide implementation
  3. source-platforms/<loader>    Forge/NeoForge/Fabric implementation
  4. version-src/<target>         irreducible target-specific files/resources

Small Minecraft-version differences may stay in family/platform files through
Stonecutter-style ``//? if ...`` directives. Loader differences must stay in
platform layers; large generation differences belong in source families.

The legacy and modern Gradle builds are intentionally independent. Their target
matrices are indexed by build-config/generations.properties and may use different
Gradle/Stonecutter/JDK toolchains while sharing this source repository.
"""
from __future__ import annotations

import ast
from dataclasses import dataclass
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
COMMON_PROPERTIES = ROOT / "build-config" / "common.properties"
GENERATIONS_PROPERTIES = ROOT / "build-config" / "generations.properties"


def read_properties(path: Path) -> dict[str, str]:
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


def generation_config_paths() -> dict[str, Path]:
    index = read_properties(GENERATIONS_PROPERTIES)
    generations = [x.strip() for x in index.get("generations", "").split(",") if x.strip()]
    if not generations:
        raise ValueError(f"No generations configured in {GENERATIONS_PROPERTIES}")
    result: dict[str, Path] = {}
    for generation in generations:
        raw = index.get(f"generation.{generation}.config", "").strip()
        if not raw:
            raise ValueError(f"Missing generation.{generation}.config in {GENERATIONS_PROPERTIES}")
        path = (ROOT / raw).resolve()
        if not path.is_file():
            raise ValueError(f"Missing {generation} target configuration: {path}")
        result[generation] = path
    return result


def load_generation_properties(path: Path) -> dict[str, str]:
    """Load common metadata plus one independent build generation."""
    config_path = path.resolve()
    local = read_properties(config_path)
    generation = local.get("generation", "").strip()
    targets = [x.strip() for x in local.get("targets", "").split(",") if x.strip()]
    if not generation:
        raise ValueError(f"Missing generation in {config_path}")
    if not targets:
        raise ValueError(f"{generation}: empty targets list in {config_path}")

    combined = read_properties(COMMON_PROPERTIES)
    combined["targets"] = ",".join(targets)
    combined["vcsTarget"] = local.get("vcsTarget", "").strip()
    combined[f"generation.{generation}.targets"] = ",".join(targets)
    combined[f"generation.{generation}.vcsTarget"] = combined["vcsTarget"]
    for target in targets:
        combined[f"target.{target}.build.generation"] = generation
    for key, value in local.items():
        if key in {"generation", "targets", "vcsTarget"}:
            continue
        if key in combined and combined[key] != value:
            raise ValueError(f"Conflicting property {key!r} while loading {config_path}")
        combined[key] = value
    return combined


def load_properties(path: Path | None = None) -> dict[str, str]:
    """Load one build generation or the complete repository target matrix."""
    if path is not None:
        return load_generation_properties(path)

    combined = read_properties(COMMON_PROPERTIES)
    all_targets: list[str] = []
    for generation, config_path in generation_config_paths().items():
        local = read_properties(config_path)
        targets = [x.strip() for x in local.get("targets", "").split(",") if x.strip()]
        if not targets:
            raise ValueError(f"{generation}: empty targets list in {config_path}")
        combined[f"generation.{generation}.targets"] = ",".join(targets)
        combined[f"generation.{generation}.vcsTarget"] = local.get("vcsTarget", "").strip()
        for target in targets:
            if target in all_targets:
                raise ValueError(f"Target {target!r} appears in more than one build generation")
            all_targets.append(target)
            combined[f"target.{target}.build.generation"] = generation
        for key, value in local.items():
            if key in {"generation", "targets", "vcsTarget"}:
                continue
            if key in combined and combined[key] != value:
                raise ValueError(f"Conflicting property {key!r} while loading {config_path}")
            combined[key] = value
    combined["targets"] = ",".join(all_targets)
    combined["vcsTarget"] = combined.get("behaviorReference", all_targets[0] if all_targets else "")
    return combined


@dataclass(frozen=True)
class TargetLayout:
    target: str
    generation: str
    family: str
    platform: str
    shared_root: Path
    family_root: Path
    platform_root: Path
    overlay_root: Path

    @property
    def layers(self) -> tuple[Path, Path, Path, Path]:
        return self.shared_root, self.family_root, self.platform_root, self.overlay_root

    def resolve(self, relative: str | Path) -> Path | None:
        rel = Path(relative)
        for root in reversed(self.layers):
            path = root / rel
            if path.is_file():
                return path
        return None

    def effective_files(self, relative: str | Path = ".") -> dict[str, Path]:
        """Return the effective tree using shared < family < platform < target precedence."""
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
                result[key] = path
        return result

    def source_candidates(self, relative: str | Path) -> tuple[Path, ...]:
        """Return every physical candidate for one logical path in precedence order."""
        rel = Path(relative)
        return tuple(path for layer in self.layers if (path := layer / rel).is_file())


def target_ids(properties: dict[str, str] | None = None) -> list[str]:
    props = properties or load_properties()
    return [item.strip() for item in props.get("targets", "").split(",") if item.strip()]


def generation_targets(properties: dict[str, str] | None = None) -> dict[str, list[str]]:
    props = properties or load_properties()
    result: dict[str, list[str]] = {}
    for target in target_ids(props):
        generation = props.get(f"target.{target}.build.generation", "").strip()
        if not generation:
            raise ValueError(f"{target}: missing build generation")
        result.setdefault(generation, []).append(target)
    return result


def target_build_root(target: str, properties: dict[str, str] | None = None) -> Path:
    props = properties or load_properties()
    generation = props.get(f"target.{target}.build.generation", "").strip()
    configs = generation_config_paths()
    if generation not in configs:
        raise ValueError(f"{target}: unknown build generation {generation!r}")
    return configs[generation].parent


def target_layout(target: str, properties: dict[str, str] | None = None) -> TargetLayout:
    props = properties or load_properties()
    prefix = f"target.{target}."
    generation = props.get(prefix + "build.generation", "").strip()
    family = props.get(prefix + "source.family", "").strip()
    platform = props.get(prefix + "source.platform", "").strip()
    shared_root = props.get(prefix + "source.shared_root", props.get("common.source.shared_root", "")).strip()
    family_root = props.get(prefix + "source.root", "").strip()
    platform_root = props.get(prefix + "source.platform_root", "").strip()
    overlay_root = props.get(prefix + "source.overlay_root", "").strip()
    if not all((generation, family, platform, shared_root, family_root, platform_root, overlay_root)):
        raise ValueError(
            f"{target}: missing build.generation/source.family/source.platform/"
            "source.shared_root/source.root/source.platform_root/source.overlay_root"
        )
    return TargetLayout(
        target=target,
        generation=generation,
        family=family,
        platform=platform,
        shared_root=(ROOT / shared_root).resolve(),
        family_root=(ROOT / family_root).resolve(),
        platform_root=(ROOT / platform_root).resolve(),
        overlay_root=(ROOT / overlay_root).resolve(),
    )


def configured_layer_paths(
    target: str, properties: dict[str, str] | None = None
) -> tuple[Path, Path, Path, Path]:
    return tuple(path.resolve() for path in target_layout(target, properties).layers)


def family_targets(properties: dict[str, str] | None = None) -> dict[str, list[str]]:
    props = properties or load_properties()
    result: dict[str, list[str]] = {}
    for target in target_ids(props):
        family = target_layout(target, props).family
        result.setdefault(family, []).append(target)
    return result


def platform_targets(properties: dict[str, str] | None = None) -> dict[str, list[str]]:
    props = properties or load_properties()
    result: dict[str, list[str]] = {}
    for target in target_ids(props):
        platform = target_layout(target, props).platform
        result.setdefault(platform, []).append(target)
    return result


_VERSION_RE = re.compile(r"^\d+(?:\.\d+)*$")
_COMPARISON_RE = re.compile(r"(?<![\w.])(>=|<=|==|!=|>|<)\s*(\d+(?:\.\d+)*)")


def _version_tuple(value: str, width: int = 6) -> tuple[int, ...]:
    if not _VERSION_RE.fullmatch(value):
        raise ValueError(f"Invalid Minecraft version in condition: {value!r}")
    parts = [int(part) for part in value.split(".")]
    return tuple((parts + [0] * width)[:width])


def evaluate_condition(condition: str, *, minecraft: str, family: str, platform: str) -> bool:
    def version_cmp(operator: str, other: str) -> bool:
        left = _version_tuple(minecraft)
        right = _version_tuple(other)
        return {
            ">=": left >= right,
            "<=": left <= right,
            ">": left > right,
            "<": left < right,
            "==": left == right,
            "!=": left != right,
        }[operator]

    expression = condition.strip().replace("&&", " and ").replace("||", " or ")
    expression = re.sub(r"!(?!=)", " not ", expression)
    expression = _COMPARISON_RE.sub(lambda m: f'version_cmp("{m.group(1)}", "{m.group(2)}")', expression)
    names = {
        "forge": platform == "forge",
        "neoforge": platform == "neoforge",
        "fabric": platform == "fabric",
        "legacy": family == "legacy",
        "modern": family == "modern",
        "true": True,
        "false": False,
        "version_cmp": version_cmp,
    }
    tree = ast.parse(expression, mode="eval")
    allowed_nodes = (
        ast.Expression, ast.BoolOp, ast.And, ast.Or, ast.UnaryOp, ast.Not,
        ast.Call, ast.Name, ast.Load, ast.Constant,
    )
    for node in ast.walk(tree):
        if not isinstance(node, allowed_nodes):
            raise ValueError(f"Unsupported condition syntax {condition!r}: {type(node).__name__}")
        if isinstance(node, ast.Name) and node.id not in names:
            raise ValueError(f"Unknown condition name {node.id!r} in {condition!r}")
        if isinstance(node, ast.Call):
            if not isinstance(node.func, ast.Name) or node.func.id != "version_cmp":
                raise ValueError(f"Unsupported function in condition {condition!r}")
    return bool(eval(compile(tree, "<stonecutter-condition>", "eval"), {"__builtins__": {}}, names))


_IF_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*if\s+(.+?)\s*\{\s*(?:\*/)?\s*$")
_ELSE_IF_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*else\s+if\s+(.+?)\s*\{\s*(?:\*/)?\s*$")
_ELSE_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*else\s*\{\s*(?:\*/)?\s*$")
_END_RE = re.compile(r"^\s*(?://\?|/\*\?)\s*}\s*(?:\*/)?\s*$")


def _directive(line: str) -> tuple[str, str | None] | None:
    text = line.rstrip("\r\n")
    if match := _IF_RE.match(text):
        return "if", match.group(1)
    if match := _ELSE_IF_RE.match(text):
        return "else_if", match.group(1)
    if _ELSE_RE.match(text):
        return "else", None
    if _END_RE.match(text):
        return "end", None
    return None


def _activate_branch(lines: list[str]) -> list[str]:
    nonblank = [i for i, line in enumerate(lines) if line.strip()]
    if not nonblank:
        return lines
    first, last = nonblank[0], nonblank[-1]
    first_text = lines[first].strip()
    last_text = lines[last].strip()

    # Preferred BCCE marker for a completely commented alternative branch.
    if first_text == "/*?" and last_text == "?*/":
        return lines[:first] + lines[first + 1:last] + lines[last + 1:]

    # Also accept the style used by existing Stonecutter projects, where the
    # inactive alternative is wrapped in one ordinary outer block comment.
    if first_text.startswith("/*") and not first_text.startswith("/**") and last_text.endswith("*/"):
        result = list(lines)
        start = result[first].find("/*")
        result[first] = result[first][:start] + result[first][start + 2:]
        end = result[last].rfind("*/")
        result[last] = result[last][:end] + result[last][end + 2:]
        return result
    return lines


def preprocess_text(text: str, *, minecraft: str, family: str, platform: str, source: str = "<memory>") -> str:
    lines = text.splitlines(keepends=True)

    def parse_sequence(index: int, stop_at_branch: bool) -> tuple[list[str], int, tuple[str, str | None] | None]:
        output: list[str] = []
        while index < len(lines):
            marker = _directive(lines[index])
            if marker is None:
                output.append(lines[index])
                index += 1
                continue
            kind, value = marker
            if kind == "if":
                selected, index = parse_conditional(index, value or "")
                output.extend(selected)
                continue
            if stop_at_branch and kind in {"else_if", "else", "end"}:
                return output, index, marker
            raise ValueError(f"{source}:{index + 1}: unexpected Stonecutter directive {lines[index].strip()!r}")
        if stop_at_branch:
            raise ValueError(f"{source}: unterminated Stonecutter conditional")
        return output, index, None

    def parse_conditional(index: int, first_condition: str) -> tuple[list[str], int]:
        branches: list[tuple[str | None, list[str]]] = []
        condition: str | None = first_condition
        index += 1
        while True:
            body, marker_index, marker = parse_sequence(index, True)
            branches.append((condition, body))
            if marker is None:
                raise ValueError(f"{source}: unterminated Stonecutter conditional")
            kind, value = marker
            if kind == "end":
                index = marker_index + 1
                break
            if kind == "else_if":
                condition = value or ""
                index = marker_index + 1
                continue
            if kind == "else":
                condition = None
                index = marker_index + 1
                body, marker_index, marker = parse_sequence(index, True)
                branches.append((None, body))
                if marker is None or marker[0] != "end":
                    line_no = marker_index + 1
                    raise ValueError(f"{source}:{line_no}: else branch must end with //?}}")
                index = marker_index + 1
                break
            raise AssertionError(kind)

        for branch_condition, body in branches:
            if branch_condition is None or evaluate_condition(
                branch_condition, minecraft=minecraft, family=family, platform=platform
            ):
                return _activate_branch(body), index
        return [], index

    output, index, marker = parse_sequence(0, False)
    if index != len(lines) or marker is not None:
        raise ValueError(f"{source}: failed to consume conditional source")
    return "".join(output)


_CONDITIONAL_TEXT_SUFFIXES = {
    ".java", ".kt", ".kts", ".gradle", ".json", ".mcmeta", ".toml",
    ".properties", ".yml", ".yaml", ".md", ".txt", ".cfg", ".xml",
    ".sh", ".bat", ".ps1",
}


def _is_conditional_text_path(path: Path) -> bool:
    return path.suffix.lower() in _CONDITIONAL_TEXT_SUFFIXES


def _copy_source(source: Path, destination: Path) -> None:
    # Effective trees are build output and must never share writable inodes with
    # authoritative source files. A compiler/tool modifying its staged input must
    # not mutate the repository through a hard link.
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)


def materialize_target(
    target: str,
    destination: Path | None = None,
    properties: dict[str, str] | None = None,
    *,
    preprocess: bool = True,
) -> Path:
    """Create a conventional merged and optionally preprocessed target tree."""
    props = properties or load_properties()
    layout = target_layout(target, props)
    dest = destination or (ROOT / "build" / "effective-sources" / target)
    dest = dest.resolve()
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True, exist_ok=True)
    minecraft = props.get(f"target.{target}.deps.minecraft", "").strip()
    if not minecraft:
        raise ValueError(f"{target}: missing deps.minecraft")

    for relative, source_path in sorted(layout.effective_files().items()):
        output = dest / relative
        raw = source_path.read_bytes()
        if preprocess and _is_conditional_text_path(source_path) and (b"//?" in raw or b"/*?" in raw):
            try:
                text = raw.decode("utf-8")
            except UnicodeDecodeError as exc:
                raise ValueError(f"{source_path}: conditional file is not UTF-8") from exc
            processed = preprocess_text(
                text,
                minecraft=minecraft,
                family=layout.family,
                platform=layout.platform,
                source=str(source_path.relative_to(ROOT)),
            )
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(processed, encoding="utf-8", newline="")
            shutil.copymode(source_path, output)
        else:
            _copy_source(source_path, output)
    return dest


def validate_all_directives(properties: dict[str, str] | None = None) -> None:
    props = properties or load_properties()
    for target in target_ids(props):
        layout = target_layout(target, props)
        minecraft = props[f"target.{target}.deps.minecraft"]
        for relative, source in layout.effective_files().items():
            raw = source.read_bytes()
            if not _is_conditional_text_path(source) or (b"//?" not in raw and b"/*?" not in raw):
                continue
            preprocess_text(
                raw.decode("utf-8"),
                minecraft=minecraft,
                family=layout.family,
                platform=layout.platform,
                source=f"{target}:{relative}",
            )


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", nargs="?")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--config", type=Path, help="load only one independent build-generation target matrix")
    parser.add_argument("--no-preprocess", action="store_true")
    parser.add_argument("--validate-directives", action="store_true")
    parser.add_argument("--list-targets", action="store_true")
    parser.add_argument("--generation", choices=sorted(generation_config_paths()))
    args = parser.parse_args()

    properties = load_properties(args.config) if args.config else load_properties()
    configured_targets = target_ids(properties)

    if args.target and args.target not in configured_targets:
        parser.error(f"target {args.target!r} is not present in the selected configuration")

    if args.validate_directives:
        validate_all_directives(properties)
        print("Stonecutter-style source directives OK")
    elif args.list_targets:
        targets = generation_targets(properties).get(args.generation, []) if args.generation else configured_targets
        print("\n".join(targets))
    elif args.target:
        print(materialize_target(
            args.target, args.output, properties, preprocess=not args.no_preprocess
        ))
    else:
        parser.error("target, --list-targets, or --validate-directives is required")
