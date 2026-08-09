# BuildCraft source families

BuildCraft Community Edition uses **generation-based source families** rather than one full source tree per Minecraft/loader target.

The project has one gameplay goal across every supported target:

> **Different implementation. Indistinguishable BuildCraft.**

Minecraft and loader APIs are implementation details. Unless a deviation is explicitly documented, a player should not be able to identify the BuildCraft target from BuildCraft gameplay, balance, persistence, UI behaviour, machine timing, routing, permissions, or resource handling.

## Families

### `legacy`

Targets:

- `1.19.2-forge`
- `1.20.1-forge`

Java 17 / classic Forge-era implementation family. `1.19.2-forge` remains the **behaviour reference**, not a requirement that newer source code look identical.

### `modern`

Targets:

- `1.21.1-neoforge`
- future `1.21.11+` / `26.x` targets while the modern implementation remains coherent

Java 21+ family using the modern Minecraft serialization, registry, networking and loader APIs where appropriate.

If a future Minecraft generation becomes so different that keeping it in `modern` would require large conditional blocks throughout gameplay code, it should become a new family rather than degrading the existing source tree.

## Repository layout

```text
source-shared/
└─ src/                    # files identical for every supported target

source-families/
├─ legacy/
│  └─ src/                 # files identical for every legacy target only
└─ modern/
   └─ src/                 # files identical for every modern target only

version-src/
├─ 1.19.2-forge/           # target-only files
├─ 1.20.1-forge/           # target-only files
└─ 1.21.1-neoforge/        # target-only files
```

A target is the union of three maintained layers:

```text
global shared + family base + target overlay = effective target source tree
```

`source-shared` is deliberately conservative: a file lives there only while the exact same implementation is valid for every supported target. Moving a file out of it when generations diverge is normal and does not represent a parity failure.

Build-only merged trees are created under `build/effective-source` / `build/effective-sources` for old tests and offline validators that expect one conventional `src/` directory. Those directories are generated and are never authoritative source.

## Where a file belongs

A file belongs in `source-shared` only when its contents are valid for **every supported target**. A file belongs in `source-families/<family>` only when its contents are valid for **every target in that family**.

If a globally shared file diverges between generations, remove it from `source-shared` and place the corresponding complete implementations into `source-families/legacy` and `source-families/modern` (or deeper overlays when necessary).

If a file differs between targets inside one family:

1. remove that relative path from the family layer;
2. place the complete target implementation in each affected `version-src/<target>` overlay;
3. prefer a small platform adapter over large Stonecutter condition blocks when the difference is loader-specific.

Do not keep an identical file in two overlays, or in both family bases when it is globally identical. `scripts/validate-source-families.py` rejects both forms of duplication for the current layout.

The maintained `source-shared`, `source-families` and `version-src` trees are compiled directly as Gradle source layers. Prefer complete target overlays or narrow platform adapters for differences; do not assume Stonecutter comment directives inside these trees will be preprocessed. Stonecutter remains responsible for the target matrix, active target and generated node/task orchestration.

## Behaviour parity

`1.19.2-forge` is the reference implementation for player-visible behaviour.

Parity means equivalent observable results, not source-code identity. Examples of required parity include:

- machine speed, costs, capacity and refunds;
- MJ generation, consumption and routing;
- item/fluid pipe routing and filtering;
- Builder/Filler/Quarry behaviour;
- robots, boards, stations and ownership identity;
- gates, wires, lasers and statements;
- save/load and chunk-unload persistence;
- permission/protection behaviour;
- blueprint contents and resource reservations;
- GUI semantics and user interactions.

A target may use a different NBT/data-component implementation, registry API, network API, event system or capability system as long as the observable BuildCraft behaviour remains equivalent.

### Blind-room criterion

A useful design test is:

> Put players on different supported BCCE targets in separate rooms, hide Minecraft/loader version information, and give them equivalent BuildCraft scenarios. They should not be able to identify the target from BuildCraft behaviour alone.

If they can, the difference is either:

1. an intentional and documented vanilla-induced deviation; or
2. a parity regression that should be fixed.

## Commands

Build everything:

```text
./gradlew buildAndCollect
```

Build one family:

```text
./gradlew buildLegacy
./gradlew buildModern
```

Validate the family layout and duplicate elimination:

```text
python scripts/validate-source-families.py
```

List targets by family:

```text
python scripts/validate-stonecutter.py --list-targets --family legacy
python scripts/validate-stonecutter.py --list-targets --family modern
```

## Future targets

New current-line ports should be added to `modern` first. Do **not** create another complete 1,400-file `version-src` copy. Start with the modern family and add only files that actually differ.

A new family is justified only when the Minecraft/platform architecture changes enough that version overlays and small adapters stop being local and readable.
