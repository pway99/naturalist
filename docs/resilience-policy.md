# Resilience Policy

Every PR that introduces or modifies a cross-boundary call must declare its
resilience strategy. Reviewers reject PRs that do not. Every PR description
should answer the question *"What resilience strategy does this introduce
or rely on?"* — or check `@ResilienceExempt` with a reason.

Two automated gates back the reviewer up. `ResilienceComplianceTest` (ArchUnit,
in `apps/management-console`) fails the build when a class on a known
cross-boundary path declares neither `@Resilient` nor `@ResilienceExempt`, and
when a `@Resilient` declaration never reaches the facade. `ResilienceNameValidator`
fails the boot when a declared strategy name resolves to no registered config.
Reviewer discipline catches the rest.

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

## Build-time gate

`apps/management-console/src/test/java/com/naturalist/console/architecture/ResilienceComplianceTest`
runs as part of the standard `mvn verify` cycle and asserts the policy
across the assembled classpath. Its rules cover:

- concrete `Catalog` implementations;
- concrete `EntityReferences<?>` implementations;
- classes touching a known HTTP transport package (`okhttp3`,
  `java.net.http`, Apache HttpClient, Spring's `RestClient`/`WebClient`, and
  vendor SDK transport packages such as `com.anthropic.client.okhttp`);
- classes depending on `java.lang.ProcessBuilder` (subprocess spawners);
- classes residing in any package whose name includes an `rdms` segment
  (the future RDMS adapter convention).

The first four rules cover every cross-boundary site shipping today; the
fifth arms the gate for the first soil RDMS adapter (and any successor).
A new class matching one of these patterns must declare resilience or the
build fails — pointing the author back at this file.

The HTTP rule carries a curated prefix list rather than a structural
predicate, because there is no common supertype across HTTP clients. It is a
floor, not a proof: a client nobody listed still slips through. **A PR that
introduces a new HTTP client adds its transport package to
`HTTP_CLIENT_PREFIXES` in the same change** — otherwise the rule weakens
silently, which is the failure mode it exists to prevent.

A fifth rule runs the other direction: **every `@Resilient` declaration must
reach the `Resilience` facade.** Nothing reads the annotation at runtime, so a
class that declares a strategy and never calls the facade is protected by
nothing while reading to the next developer as protected — strictly worse than
an honest omission. `EntityReferences` implementations are excluded, because
they are wrapped by their caller: `InMemoryCatalog.findReferencesTo` applies
`catalog.fanout` around each provider invocation individually, so that one
wedged domain degrades only its own slice of the response. Their annotation
declares coverage, not application.

## Startup gate

`ResilienceNameValidator` (an `ApplicationRunner` in
`com.naturalist.console.resilience`) fails the boot when a `@Resilient(name =
...)` on any registered bean resolves to no registered `ResilienceConfig`. It
collects class-level and method-level declarations across every bean
definition and checks each name against the union of the four
`Resilience.*Names()` sets.

This closes the gap the build gate cannot see: a name can be spelled
correctly, pass every ArchUnit rule, and still have had no config registered
for it in the composition root. `UnconfiguredResilienceException` already
catches the same defect, but only at the moment of a facade lookup — which for
a rarely-exercised path means production rather than integration testing. The
runner moves the failure to boot, and because Spring Boot invokes runners
under `@SpringBootTest`, to `mvn verify` as well.

The runner does not check that the registered *primitives* match the call
site's intent — a name registered with only a `TimeoutConfig` satisfies it
even where a circuit breaker was wanted. That judgement stays with the
reviewer, informed by `/admin/resilience`.

## Why no AOP weaver

Making `@Resilient` load-bearing via a proxy was considered and rejected. It
cannot express the per-provider wrapping inside the catalog fan-out (a
method-level interceptor would bound the whole fan-out, so one slow provider
would take every other domain's results with it); the timeout adapter runs its
supplier on a worker thread, so weaving a controller method would move request
handling off the thread its `SecurityContext` and `RequestContext` are bound
to; and proxying never intercepts self-invocation or non-bean instances, so
the annotation would stay inert exactly where it looks wired. `InMemoryCatalog`
is `final`, which CGLIB cannot subclass at all.

The two gates above give what the weaver was wanted for — a declaration that
cannot silently mean nothing — without changing where or how the strategy is
applied.

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
`adapters/resilience-resilience4j/`. The composition root that registers
named strategies and exposes the assembled `Resilience` bean lives in
`apps/management-console` under `com.naturalist.console.resilience`.

Domain `*-core` code references only the facade — never Resilience4j directly.
