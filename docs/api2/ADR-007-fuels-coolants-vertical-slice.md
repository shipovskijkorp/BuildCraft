# ADR-007: Fuels and coolants as the first API 2 production domain

## Decision

Combustion fuels, fluid coolants, and solid-coolant conversions are resolved
from one atomically published `EnergyFluidService` snapshot.

Programmatic registrations form a persistent baseline. Reload-owned definitions
are replaced as one candidate set. The candidate is published only after the
generic reload resolver accepts it; otherwise the previous snapshot remains
active.

Matching precedence is definition priority first and stable definition id
second. Overrides of the same id use the generic reload provenance rules.

Legacy `BuildcraftFuelRegistry` managers remain for compatibility, but their
runtime storage is now an adapter over API 2. Existing engine and JEI consumers
therefore read the same authoritative definitions even before their source code
is migrated to direct API 2 calls.

Built-in data codecs intentionally serialize only stable `FluidSelector`
variants (fluid id, tag, exact variant) and constant coolant rates. Arbitrary
programmatic matchers/cooling curves remain valid runtime extensions but refuse
lossy data encoding.

## Compatibility note

The legacy loader-native fuel/coolant bridge resolves fluid identity and fluid
tag/equivalence groups. General component-payload conversion remains owned by
the broader platform `FluidService` bridge rather than leaking loader stack
types into this domain API.
