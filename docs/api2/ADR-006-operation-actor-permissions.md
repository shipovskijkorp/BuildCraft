# ADR-006: Automation actor and permission decisions

## Decision

API 2 world mutations carry a loader-neutral `AutomationActor` and
`WorldOperationContext`. Protection integrations return `PermissionDecision`
with `ALLOW`, `DENY`, or `PASS` plus an authority id and optional reason.

`PermissionServiceRegistry` is the addon registration point. Providers have a
namespaced id and explicit priority and are evaluated deterministically by
descending priority, then provider id. Duplicate ids are rejected.

`DENY` is terminal when permission services are composed. `ALLOW` is not
terminal: a later protection provider can still deny an operation. A chain of
only `PASS` decisions remains `PASS` so the caller can apply its normal vanilla
or platform fallback policy.

`WorldOperationContext` contains the actual common-side `Level` as well as a
dimension accessor, so protection integrations do not need global server
lookups. `OperationMode.SIMULATE` is part of the context. Permission providers are
observational and must not mutate the world for either mode.

## Consequences

Legacy FakePlayer creation stays in platform/compat implementation. API common
code never exposes Forge/NeoForge fake-player classes. Machines and robots can
retain owner UUIDs without pretending that every actor is a live player.
