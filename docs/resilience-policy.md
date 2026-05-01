# Resilience Policy

Every PR that introduces or modifies a cross-boundary call must declare its
resilience strategy. Reviewers reject PRs that do not.

## What "cross-boundary" means

A call is cross-boundary when it can fail or hang for a reason outside the
caller's process:

- a repository read or write (any `*-repository-rdms/` adapter);
- a fan-out from `Catalog.findReferencesTo(...)` to a domain's
  `EntityReferences` provider;
- any HTTP, database, message-broker, or filesystem call;
- any future external service: Solr, Postgres, S3, a sync target.

In-process calls between domain `*-core` classes that take only `Observable`
arguments and return only `Observable` values are *not* cross-boundary. The
JVM's own contracts cover them.

## What "declare a strategy" means

Three acceptable forms, in order of preference:

1. **`@Resilient(name = "...")`** on the class or method making the call. The
   composition root registers `ResilienceConfig` records under that name —
   typically a `RetryConfig`, a `TimeoutConfig`, and a `CircuitBreakerConfig`
   sharing the name.
2. **Programmatic `Resilience` facade use.** A class that takes a
   `Resilience` constructor parameter and wraps its calls explicitly:
   ```java
   resilience.timeout("catalog.fanout").execute(() -> provider.referencesTo(target))
   ```
   This is the right shape when the call site needs finer control (e.g.,
   wrapping only a sub-step of a larger method).
3. **`@ResilienceExempt(reason = "...")`** on the class or method. Only valid
   when the call is in-process, side-effect-free, and not subject to timeout.
   The reason is required and surfaces in source review and in M9's
   compliance test.

## What gets reviewed

Reviewers should ask of every PR that touches a cross-boundary call site:

- Is the strategy named, and is the name registered in the composition root?
- Are the chosen primitives appropriate? Catalog fan-out wants timeout +
  circuit breaker; a flaky external service wants retry + timeout; a
  resource-constrained call wants bulkhead.
- If exempt, does the reason hold up under scrutiny?

PRs without a declared strategy are blocked until one is added.

## Unconfigured-name contract

Production adapters refuse to serve an unprotected fall-back. Asking for
`resilience.retry("foo")` when no `RetryConfig` named `"foo"` was registered
throws `UnconfiguredResilienceException` at the lookup site. A misconfigured
strategy is a deployment defect, not a runtime condition to absorb silently —
the failure surfaces during integration testing, not at 3am after a config
typo merged.

The framework default `Resilience.noOp()` is the one exception: it is the
intentional "no resilience wired" path for unit tests and bring-up. Its
existence at the call site is itself the signal — there is no metric to
watch.

## Implementation home

The facade lives in `kernels/framework` under
`com.naturalist.resilience`; the exception lives in
`com.naturalist.exception`. The production adapter lives in
`kernels/resilience-resilience4j/` (planned move to
`adapters/resilience-resilience4j/` in M5 of the runtime architecture
refactor — see `docs/plans/runtime-architecture-refactor.md`).

Domain `*-core` code references only the facade — never Resilience4j directly.
