# ADR-005: Loader-neutral resource and fluid transfer model

Status: accepted for API 2 preview

## Decision

API 2 common code does not expose Forge, NeoForge or Fabric fluid stack/storage types.

Fluid identity is represented by `FluidVariant`:

- a Minecraft fluid registry `ResourceLocation`;
- an optional opaque canonical component payload;
- equality compares both fields;
- `sameFluid` intentionally ignores component data.

The opaque payload carries a format identifier and immutable canonical bytes. Common addons can preserve, compare, hash and serialize it without understanding the platform-native component representation.

Fluid quantity is represented by `FluidAmount` as a non-negative signed 64-bit number of milliBuckets. One bucket is 1000 mB. Arithmetic is checked; overflow and negative values are rejected.

`FluidVolume` combines variant and amount. Zero amount is canonicalized to an explicit empty value and therefore does not retain a meaningless variant identity.

`FluidPort` is the stable transfer contract. Every operation explicitly receives `OperationMode.SIMULATE` or `OperationMode.EXECUTE`. SIMULATE must not mutate the port or world. Results report what actually moved and preserve partial-transfer/remainder information.

`FluidMatcher` distinguishes exact variant matching from same-fluid-ID matching and supports tags through a loader-neutral `FluidMatchContext`. Addons can implement custom rules without depending on loader APIs.

Vanilla `ItemStack` remains an acceptable Minecraft-level value for `ItemPort`; Forge/NeoForge capabilities and Fabric transfer/storage objects are not part of the public common contract.

## Platform conversion

Platform bridges own native stack conversion. A bridge must preserve the complete variant identity, including component/NBT data. A conversion that cannot preserve data must fail rather than silently drop it.

Unit conversion is explicit. `FluidUnitConverter` returns a rational remainder whenever a platform unit scale cannot represent an mB amount exactly. Callers decide whether to reject, buffer or otherwise account for that remainder; silent rounding is not allowed.

## Consequences

- Forge, NeoForge and Fabric may use unrelated native transfer systems while exposing the same API 2 contract.
- Fuels/coolants and fluid pipes can be migrated without making common API signatures loader-specific.
- Equality of component-bearing fluid variants remains deterministic.
- Persisted addon data can retain a fluid variant even when the original addon/platform bridge is temporarily unavailable.

## Stability note

This contract is still API 2 preview. Native platform adapters and the fuels/coolants vertical slice must validate the model before it is declared stable.
