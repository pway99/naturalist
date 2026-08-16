# ADR-026: Resilience as a First-Order Concern

**Status:** Accepted

**Date:** 2026-05-02

## Context

For most of the codebase's life, "resilience" meant whatever ad-hoc
retry, timeout, or fallback logic the call site happened to need. The
catalog fan-out used a `try/catch (RuntimeException)` swallow on every
provider invocation; the insects controller's image-conversion pipeline
spawned `sips` with no wall-clock cap; the soil RDBMS adapter — when it
ships — would land without a place for retry or circuit-breaker
configuration.

That posture worked while the system was a single in-process
modular monolith with no real external collaborators. It will not work
once the first production adapters land. A wedged Solr query, a
slow-responding sync target, a flaky external service all need an
explicit resilience strategy at the call site, registered by the
composition root, and visible in code review and at runtime.

The runtime architecture refactor (`docs/plans/runtime-architecture-refactor.md`)
makes resilience the priority foundation — M1 ships the kernel facade and
the Resilience4j adapter before any other structural change, on the
grounds that subsequent milestones can then evaluate themselves against
it. This ADR records the decision the plan executes.

## Decision

### Facade lives in the kernel; Resilience4j is an adapter

The kernel ships a small set of facade interfaces in
`kernels/framework`'s `com.naturalist.resilience` package:

```
Resilience              — facade aggregating the four primitives
├─ Retry
├─ Timeout
├─ CircuitBreaker
└─ Bulkhead

ResilienceConfig        — sealed configuration record per primitive
@Resilient(name)        — declarative marker (TYPE or METHOD)
@ResilienceExempt(reason) — explicit opt-out
NoOpResilience          — package-private default for tests
```

Each primitive interface takes a `Supplier<T>` (or `Runnable`) and
returns the protected execution. Domain code calls
`resilience.timeout("catalog.fanout").execute(...)` or annotates with
`@Resilient(name = "catalog.fanout")`. It never imports a vendor type.

The production implementation lives in
`adapters/resilience-resilience4j/`. It bridges the facade to
`io.github.resilience4j.{retry,timelimiter,circuitbreaker,bulkhead}`.
The same boundary discipline that protects core from Spring (ADR-025)
protects it from Resilience4j: only one adapter knows the vendor.

`kernels/framework`'s `pom.xml` declares no dependency on Resilience4j.
Adding a domain-side resilience call adds zero transitives to the
domain's dependency surface.

### Two annotations, one obligation

`@Resilient(name = "...")` declares the call is protected by the named
strategy. The composition root registers `ResilienceConfig` records
under that name (typically a `RetryConfig`, a `TimeoutConfig`, and a
`CircuitBreakerConfig` sharing the name). The adapter applies every
primitive configured under the name when wrapping the call.

`@ResilienceExempt(reason = "...")` declares the call is intentionally
unprotected. The reason is required and surfaces in code review and in
the M9 compliance test. Valid grounds: the call is in-process,
side-effect-free, and not subject to timeout. "Intra-process kernel
helper" is acceptable; "I forgot to add a strategy" is not.

Both annotations carry runtime retention and target both `TYPE` and
`METHOD`, so a class can declare a default and individual methods can
override.

### Programmatic facade use is equally valid

A class that takes a `Resilience` constructor parameter and wraps its
calls explicitly is a first-class declaration shape:

```java
public InsectsController(Resilience resilience) { ... }

byte[] convert(Path source) {
    return resilience.timeout("image.conversion").execute(() -> {
        // sips invocation, Files.copy, Files.readAllBytes
    });
}
```

This is the right shape when the call site needs finer control than the
annotation allows — wrapping a sub-step of a larger method, composing
multiple primitives by hand, or threading the facade through a class
whose method-level granularity does not match the strategy boundary.
The compliance gate (below) accepts both shapes.

### Unconfigured-name contract

Production adapters refuse silent fall-back. Asking for
`resilience.retry("foo")` when no `RetryConfig` named `"foo"` was
registered throws `UnconfiguredResilienceException` at the lookup site.
A misconfigured strategy is a deployment defect, not a runtime condition
to absorb — the failure surfaces during integration testing, not at 3am
after a config typo merged.

`Resilience.noOp()` is the one exception. It is the intentional "no
resilience wired" path for unit tests and composition roots that have
not yet wired a production adapter. Its presence at the call site is
itself the signal — there is no metric to watch.

### PR-level expectation

Every PR that introduces or modifies a cross-boundary call site must
declare its resilience strategy in one of the three forms:
`@Resilient(...)`, programmatic facade use, or
`@ResilienceExempt(reason = "...")`. The PR description must answer the
question *"What resilience strategy does this introduce or rely on?"*.
Reviewers reject PRs that do not.

The full policy and reviewer checklist live in
[`docs/resilience-policy.md`](../resilience-policy.md).

### Build-time compliance gate (M9)

`apps/management-console/.../ResilienceComplianceTest` is an ArchUnit
test that runs as part of `mvn verify`. It scans the assembled
classpath for classes on known cross-boundary paths and fails the build
when any such class declares neither `@Resilient` nor
`@ResilienceExempt`. The four rules cover:

1. concrete `Catalog` implementations;
2. concrete `EntityReferences<?>` implementations;
3. classes depending on `java.lang.ProcessBuilder` (subprocess
   spawners);
4. classes residing in any package whose name includes an `rdms`
   segment (the future RDBMS adapter convention).

The compliance test catches the sites the policy applies to today and
arms the gate for the first soil RDBMS adapter and any successor. The
violation message names the offending class and points the author at
`docs/resilience-policy.md`.

ArchUnit 1.4.2 is required (1.4.0's bundled ASM rejects Java 25 class
files). Surefire's `useManifestOnlyJar=false` is required so the forked
test JVM exposes the real classpath URLs to ArchUnit's classloader.

## Consequences

- Every cross-boundary call site in the codebase carries either
  `@Resilient`, programmatic facade use, or `@ResilienceExempt`. The
  intent is visible in source.
- Domain `*-core` modules gain a dependency on
  `kernels/framework`'s resilience facade — a few interface types and
  two annotations, no new transitives.
- The composition root (`apps/management-console`) gains a
  `ResilienceConfiguration` that registers each named
  `ResilienceConfig` and exposes the assembled `Resilience` as a
  `@Bean`. A new strategy is added there, not in domain code.
- Unconfigured names fail at the lookup site at startup, not silently at
  request time.
- Reviewer discipline is the first line; the M9 compliance test is the
  safety net. New cross-boundary patterns (a future Solr adapter, a
  message-broker adapter) extend the test's rules to catch the new
  shape.
- A future AOP weaver under `adapters/spring-runtime/` would let
  domain authors declare intent with `@Resilient` alone, without
  threading `Resilience` through constructors. That weaver is the
  natural M8.6 follow-up; M8.5 establishes the programmatic shape and
  the synthetic-failure proof, so the AOP work is a pure ergonomic
  upgrade rather than a behaviour change.
- Resilience4j ships Micrometer integration. Wiring its metrics through
  the kernel's `Observable` pipeline (so `naturalist.resilience.*`
  meters land on the same backend as `naturalist.observation` and
  `naturalist.invariant.violation`) is adjacent work — out of scope
  here, recorded as a follow-up.

## Applicability Signals

Flag an ADR-026 violation in review when any of the following appears:

- A new class on a cross-boundary path (catalog implementation,
  inverse-SPI provider, subprocess spawner, RDBMS adapter, future Solr
  adapter, future message-broker adapter) ships with neither
  `@Resilient` nor `@ResilienceExempt`.
- A domain module imports `io.github.resilience4j.*`.
- An `@ResilienceExempt` reason describes laziness rather than the
  in-process / side-effect-free / not-subject-to-timeout grounds the
  policy admits.
- A `Resilience` adapter silently falls back to no-op on an
  unconfigured name (`UnconfiguredResilienceException` is the only
  acceptable behaviour outside `Resilience.noOp()` itself).
- The compliance test gains an `allowEmptyShould` workaround for a
  rule that should match a real class.
- A PR description does not answer *"What resilience strategy does
  this introduce or rely on?"* for a PR touching a cross-boundary call
  site.

## Related ADRs

- ADR-017 — Observability, Monitoring, and Validation (resilience
  observability lands on the same metric backend; the kernel keeps
  the `Observable` pipeline as the canonical exit).
- ADR-018 — Third-Party Dependency Policy (Resilience4j is an
  adapter-only dependency; it never appears in framework, api, or
  core).
- ADR-024 — Apps and Adapters Trees
  (`adapters/resilience-resilience4j/` is the canonical adapter
  shape).
- ADR-025 — DI via Marker Annotations (the same boundary discipline
  governs Spring and Resilience4j; both are adapter-only).

## Reference Implementation

Post-M1, M8, M8.5, and M9 of the runtime architecture refactor:

- `kernels/framework/.../resilience/` — facade, primitives, configs,
  annotations, no-op default.
- `adapters/resilience-resilience4j/` — production adapter; bridges
  the facade to Resilience4j's primitives.
- `apps/management-console/.../resilience/ResilienceConfiguration.java`
  — registers the named `ResilienceConfig` records, exposes the
  assembled `Resilience` `@Bean`.
- `kernels/catalog-inmem/.../InMemoryCatalog.java` — declarative
  `@Resilient(name = "catalog.fanout")` on `findReferencesTo`,
  programmatic wrapping via the constructor-injected facade.
- `domains/plants/plants-core/.../PlantsCompoundReferences.java` —
  class-level `@Resilient(name = "catalog.fanout")`.
- `apps/management-console/.../insects/InsectsController.java` —
  method-level `@Resilient(name = "image.conversion")` plus
  programmatic timeout wrap.
- `apps/management-console/.../architecture/ResilienceComplianceTest.java`
  — four ArchUnit rules covering the cross-boundary paths.
- `docs/resilience-policy.md` — the full policy, reviewer checklist,
  and unconfigured-name contract.
