# ADR-017: Observability, Monitoring, and Validation

**Status:** Accepted

## Context

Domain systems typically treat validation, monitoring, and logging as three separate
concerns wired at different layers with different libraries. Validation is an if-throw
at the method boundary. Monitoring is a metrics library sprinkled at call sites.
Logging is scattered `logger.info` and `logger.debug` calls with no structural
relationship to the state they describe. The result is three parallel descriptions of
the same data flow that drift apart as the code evolves.

The Naturalist application models domain knowledge as structured types — `Entity`,
`Aggregate`, `ValueObject` — each declaring the invariants that must hold for an
instance to be valid. The observability framework exploits this: because every domain
type already declares its invariants as a constraint graph, that same graph can serve
as the single source of truth for validation, state monitoring, and failure signalling.
There is no need for a separate validation library, a separate monitoring integration
point, or a separate logging strategy. One walk of one graph produces all three.

### Evans' Invariant/Constraint distinction

Eric Evans (DDD, Chapter 9) draws a distinction between an *invariant* — an abstract
predicate that must always hold true for a domain object — and a *constraint* — that
same rule made explicit as a first-class object in the model. The codebase aligns with
this terminology:

- **Invariant** is the domain predicate. Entities declare their invariants via
  `Observable.invariants()`, which returns a `Consumer<? extends Constraints>` — a
  declaration of what must hold, not a reified object.
- **Constraint** is the reified rule. `Constraint<V>` is the interface for a named,
  evaluable object that carries a value and knows whether it satisfies its rule.
  Concrete implementations — `NotNullConstraint`, `NotBlankConstraint`,
  `ObservableConstraint`, `PersistenceIdConstraints`, `EntityNameConstraints`,
  `NamedValueConstraints` — live in `com.naturalist.observability.constraints`.
- **InvariantObservation** is the result of observing whether invariants hold. It holds
  the flattened set of constraints evaluated during one pass and exposes `violations()`
  — the constraints that failed. The name is deliberate: you observe *invariants*; you
  evaluate *constraints*; you report *violations*.
- **InvariantViolationException** carries all violations from a single observation pass.
  No iterative error discovery — the caller sees every failure at once.

## Decision

### Observability is a first-order architectural concern

Every domain type implements `Observable` and declares `invariants()`. The `Observer`
walks the constraint graph wherever domain objects flow — method boundaries, repository
insertion, query results, event processing. Observability is not a cross-cutting concern
bolted on; it is structural, integrated into the type system through `Observable` and
composed via the fluent `Constraints` builder.

### Three roles, one graph walk

The `Observer` performs three roles over the same constraint-graph traversal:

1. **Argument validation.** `observer.arguments("methodName", i -> i.entityName(name, "name"))`
   builds an `InvariantObservation` over a method's input. Calling `throwWhenInvalid()`
   throws `InvariantViolationException` carrying every violation. This replaces ad-hoc
   null checks and manual `IllegalArgumentException` throws with a single-pass,
   structural validation that shares the same constraint definitions used everywhere
   else.

2. **State observation.** `observer.entity(e, "label")` or `observer.observable(o, "label")`
   walks an object's full constraint graph. The caller chooses the terminal operation:
   `throwWhenInvalid()` for hard enforcement, or `observe()` for metrics-only inspection.
   `observe()` returns the `InvariantObservation` so the caller can inspect
   `violations()` after metric emission.

3. **Realtime monitoring.** `Observer.forClass(SensorReadingProcessor.class, MonitoringMode.ALWAYS)`
   causes every observation to emit a metric for every constraint inspected — valid or
   invalid. With `MonitoringMode.ON_FAILURE` (the default), metrics fire only per
   violation. The `ALWAYS` mode is designed for high-throughput data flows where
   visibility into every state transition matters — temperature readings, moisture
   content events, sensor telemetry — where the interesting signal is the distribution
   of valid states, not just the exceptions.

### Scoped observation points

Every observation carries a fully-qualified dotted-path scope that locates it in class,
method, and variable:

- `Observer.forClass(CompoundQuery.class)` scopes to the class.
- `observer.forMethod("getByName")` narrows to the method, producing a `MethodObserver`.
- `mo.observable(compound, "c")` narrows to the variable.

The resulting scope — `CompoundQuery.getByName.c.compoundInfo.formula` — is the metric
tag and the exception message path. A violation names its exact location in the object
graph relative to the observation point. No stack trace parsing required.

### Constraint graph walking

The constraint graph is a tree rooted at the observed object. Each `Observable` declares
its direct constraints via `invariants()`. When a constraint is itself a
`ConstraintCollection` — specifically `ObservableConstraint`, which wraps an `Entity`,
`Aggregate`, or `ValueObject` member — the walker descends into the child's own
constraints. Each level prepends one dotted segment, producing fully-qualified paths
like `compound.compoundInfo.constituentElements`.

The walk is a single pass. The flattened result is a `Set<Constraint<?>>` with
deterministic insertion order (`LinkedHashSet`). Every constraint in the set carries
its fully-qualified name. `InvariantObservation` partitions this set into valid and
invalid subsets — no second walk required.

### Logging is discouraged

Traditional logging — `logger.info`, `logger.debug`, scattered format strings — provides
unstructured, high-volume text that quickly becomes noise. The observability framework
replaces it with structured metric emission at well-defined observation points. The only
appropriate use of logging is for exception diagnostics, and even there the
`InvariantViolationException` message already serialises scope and all violation names
into a single readable string.

Structured metrics carry tags (scope, constraint name, valid/invalid) that are
filterable, aggregatable, and alertable. Log lines do not. A metric counter
`naturalist.invariant.violation{scope=CompoundQuery.getByName, constraint=name}`
is machine-readable and dashboardable. A log line `"Invalid compound name: null"` is
not.

Configure logging for exceptions only. Do not add `logger.info` or `logger.debug` calls
to observe state flow — use the Observer framework instead.

### Metrics API

`Metric` (`com.naturalist.observability.Metric`) is a fluent, lightweight class that
the consumer uses directly. Domain code never touches Micrometer's `MeterRegistry` or
`Counter` — those are infrastructure concerns. The consumer builds a metric with tags
and fires it:

```java
Metric.counter("naturalist.invariant.violation")
    .tag("constraint", c.getClass().getSimpleName())
    .tag("class", c.source())
    .tag("method", c.methodName())
    .incrementCounter();
```

`incrementCounter()` is the terminal operation. It delegates to Micrometer's
`Metrics.counter(name, tags)` internally — the consumer never imports Micrometer types.
The `Metric` builder enforces naming conventions (lowercase, dot-separated) and
normalises invalid input, providing a discoverable API without requiring knowledge of
Micrometer best practices.

### Two meters, two concerns

Observations and violations are separate counters with different tag sets and different
cardinality profiles. Combining them would create noise — they serve different audiences.

- **`naturalist.observation`** — emitted in `ALWAYS` mode for every constraint inspected.
  Tags: `constraint` (type), `class` (source), `method`, `valid` (boolean). High
  cardinality, opt-in per Observer. Drives state-distribution dashboards for
  high-throughput data flows (sensor readings, telemetry).
- **`naturalist.invariant.violation`** — emitted regardless of mode for every failing
  constraint. Tags: `constraint` (type), `class` (source), `method`. No `valid` tag —
  violations are definitionally invalid. Low cardinality, always on. Drives alerting
  and canary analysis.

### Constraint diagnostic methods

Each `Constraint<V>` exposes `source()` and `methodName()` as default methods derived
from the fully-qualified dotted name assigned during graph flattening. Given a constraint
named `CompoundQuery.getByName.c.compoundInfo.formula`:

- `source()` → `"CompoundQuery"` — the class declaring the observation point.
- `methodName()` → `"getByName"` — the method at the observation point.

These exist solely for metric tag population. They carry zero cost on the validation
hot path — the dotted name is already computed during flattening; the substring
extraction happens only at emission time.

### Micrometer as a first-class framework dependency

`io.micrometer:micrometer-core` is a compile dependency of `kernels/framework`.
Micrometer is the SLF4J of metrics in the Java ecosystem — it is itself a facade over
backend implementations (Prometheus, Datadog, etc.) and carries minimal transitive
dependencies (`micrometer-commons`, `micrometer-observation` at compile scope;
`HdrHistogram` and `LatencyUtils` at runtime scope only). It is lighter than Jackson.

`Metric.incrementCounter()` delegates directly to `io.micrometer.core.instrument.Metrics`,
which holds a static `CompositeMeterRegistry`. This registry is effectively no-op when
no backends are registered — the default state during tests and before application
startup. Spring Boot's actuator wires the application's `MeterRegistry` into this global
at startup; no custom adapter class is required.

- **Production:** Spring Boot auto-configures `Metrics.globalRegistry` via actuator.
  No application code is needed to wire the metric backend.
- **Test:** Domain tests assert invariant violations via `InvariantObservation.violations()`,
  not via metric counters. Metric emission in tests goes to the empty
  `CompositeMeterRegistry` and is effectively a no-op. Tests that specifically need to
  assert metric emission can register a `SimpleMeterRegistry` on
  `Metrics.globalRegistry` for the duration of the test.
- **Default:** No-op. The observability framework must never throw from metric emission —
  a failure in monitoring must not become a failure in the domain.

The previous design used a hand-rolled port (`MetricRegistry`, `Counter`) with a
volatile static holder and an `InMemoryMetricRegistry` test adapter. This was
structurally isomorphic to Micrometer's own API and added no value beyond what
Micrometer provides natively. The port, adapter, and test registry were removed in
favour of direct Micrometer delegation.

### Metric naming conventions

Metric names and tag keys follow Micrometer convention: lowercase, dot-separated.
`Metric` silently normalises invalid input — spaces and underscores become dots,
consecutive dots collapse, leading/trailing dots are stripped. Null or blank input
becomes `"unknown"`. Tag values are preserved verbatim — observation scopes and
constraint names carry mixed case by design.

## Consequences

### Positive

- **Single source of truth.** The `invariants()` declaration on each domain type is the
  definition of validity, the source of validation errors, and the source of monitoring
  metrics. There is no second place to update when a constraint changes.

- **Structural validation replaces ad-hoc checks.** Argument validation via
  `observer.arguments(...)` eliminates scattered null checks and manual exception
  construction. Every method boundary validates in a single pass using the same
  constraint vocabulary as entity-level observation.

- **Full-graph visibility.** A single observation walks the entire constraint tree of an
  object. A `Compound` observation evaluates the compound's own constraints, descends
  into `CompoundInfo`, `SolubilityProfile`, `BioavailabilityProfile`, and reports every
  violation with its fully-qualified path. No constraint is silently skipped.

- **Monitoring without code changes.** Switching an Observer from `ON_FAILURE` to
  `ALWAYS` turns on full-state monitoring for that observation point. No new code, no
  new metric declarations — the constraint graph already defines what to observe.

- **Evans-aligned terminology.** The naming convention — invariants are declared,
  constraints are evaluated, violations are reported — maps directly to Evans Ch.9 and
  communicates intent to anyone familiar with DDD.

### Negative

- **Constraint graph depth affects performance.** A deeply nested aggregate with many
  child observables produces a large flattened constraint set per observation. For
  hot-path code, `ON_FAILURE` mode and shallow graphs are preferred.

- **Metric cardinality in ALWAYS mode.** Emitting a counter per constraint per
  observation in high-throughput paths (sensor readings, event streams) produces high
  tag cardinality. The production Micrometer backend must be configured with appropriate
  cardinality limits.

## Type Reference

| Type                         | Package                                    | Role                                                                  |
|------------------------------|--------------------------------------------|-----------------------------------------------------------------------|
| `Observable`                 | `com.naturalist.observability`              | Interface declaring `invariants()` on every domain type               |
| `Constraint<V>`              | `com.naturalist.observability`              | Reified invariant rule — named, evaluable, `source()`, `methodName()` |
| `Constraints`                | `com.naturalist.observability`              | Fluent builder accumulating `Constraint<?>` instances                  |
| `ConstraintCollection`       | `com.naturalist.observability`              | Composite node exposing child constraints for graph descent            |
| `Observer`                   | `com.naturalist.observability`              | Entry point — `static final`, class-scoped, zero metric overhead       |
| `MethodObserver`             | `com.naturalist.observability`              | Method-scoped observer with `ClassName.methodName.label` paths         |
| `InvariantObservation`       | `com.naturalist.observability`              | Result of one graph walk — emits metrics, exposes `violations()`       |
| `InvariantViolationException`| `com.naturalist.exception`                  | Carries all violations from a single observation pass                  |
| `Metric`                     | `com.naturalist.observability`              | Fluent counter — `counter(name).tag(k,v).incrementCounter()`           |
| `Metric.Tag`                 | `com.naturalist.observability`              | Immutable key-value pair, keys normalised to Micrometer convention     |
| `MonitoringMode`             | `com.naturalist.observability`              | `ON_FAILURE` (default) or `ALWAYS` — controls metric emission          |
| `ObservableConstraint`       | `com.naturalist.observability.constraints`  | Composite constraint for `Entity`/`Aggregate`/`ValueObject` members    |
| `NotNullConstraint`          | `com.naturalist.observability.constraints`  | Null-check constraint                                                  |
| `NotBlankConstraint`         | `com.naturalist.observability.constraints`  | Blank-string constraint                                                |
| `PersistenceIdConstraints`   | `com.naturalist.observability.constraints`  | `PersistenceId` validation (single and collection)                     |
| `EntityNameConstraints`      | `com.naturalist.observability.constraints`  | `EntityName` validation (single and collection)                        |
| `NamedValueConstraints`      | `com.naturalist.observability.constraints`  | `NamedValue` validation                                                |
