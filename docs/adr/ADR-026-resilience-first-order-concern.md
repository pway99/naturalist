# ADR-026: Resilience as a First-Order Concern
> [rationale](rationale/ADR-026-resilience-first-order-concern.md)

Every cross-boundary call declares its resilience strategy. Reviewer discipline
catches it; an ArchUnit test catches the rest.

### Facade in the kernel; vendor in an adapter
- `kernels/framework`'s `com.naturalist.resilience` ships `Resilience`,
  `Retry`, `Timeout`, `CircuitBreaker`, `Bulkhead`, the sealed
  `ResilienceConfig`, `@Resilient(name)`, `@ResilienceExempt(reason)`, and
  the package-private `NoOpResilience` default.
- `adapters/resilience-resilience4j/` is the production implementation. No
  domain or kernel module imports `io.github.resilience4j.*`.

### Three acceptable declarations (per call site)
1. `@Resilient(name = "...")` — class- or method-level. The composition root
   registers `ResilienceConfig` records under that name.
2. **Programmatic facade use** — constructor-inject `Resilience` and call
   `resilience.timeout("name").execute(() -> ...)`.
3. `@ResilienceExempt(reason = "...")` — only valid when the call is
   in-process, side-effect-free, and not subject to timeout.

### Unconfigured-name contract
Production adapters throw `UnconfiguredResilienceException` on a missing
config name. Silent fall-back is forbidden. `Resilience.noOp()` is the one
exception — it is the explicit "no resilience wired" default.

### Build-time gate (M9)
`apps/management-console/.../ResilienceComplianceTest` (ArchUnit) fails the
build when a class on a known cross-boundary path declares neither
`@Resilient` nor `@ResilienceExempt`. Rules cover concrete `Catalog`
implementations, concrete `EntityReferences<?>`, classes depending on
`java.lang.ProcessBuilder`, and any class in a package whose name contains
an `rdms` segment.

### PR expectation
Every PR that touches a cross-boundary call site answers the question
*"What resilience strategy does this introduce or rely on?"*. Full policy
and reviewer checklist: [`docs/resilience-policy.md`](../resilience-policy.md).

### Review flags
- New cross-boundary class with neither `@Resilient` nor `@ResilienceExempt`.
- Domain module importing `io.github.resilience4j.*`.
- A `Resilience` adapter falling back to no-op on an unconfigured name.
- A `@ResilienceExempt` reason describing laziness rather than
  in-process / side-effect-free / not-subject-to-timeout grounds.
