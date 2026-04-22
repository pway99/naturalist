# ADR-017: Observability, Monitoring, and Validation

**Status:** Accepted
**Full rationale:** [rationale/ADR-017-observability-monitoring-and-validation.md](rationale/ADR-017-observability-monitoring-and-validation.md)

## Decision

### Observability is a first-order architectural concern
Every domain type implements `Observable` and declares `invariants()`. The `Observer`
walks the constraint graph at method boundaries, repository insertion, query results,
event processing. Not cross-cutting/bolt-on — structural, integrated via the type system.

### Evans-aligned terminology
- **Invariant** — domain predicate declared via `Observable.invariants()` returning
  `Consumer<? extends Constraints>`
- **Constraint** — reified rule (`Constraint<V>` interface) — named, evaluable, carries
  a value, knows whether it satisfies its rule
- **InvariantObservation** — result of one graph walk; exposes `violations()`
- **InvariantViolationException** — carries all violations from one pass (no iterative
  error discovery)

### Three roles, one graph walk
1. **Argument validation** — `observer.arguments("method", i -> i.entityName(name, "name")).throwWhenInvalid()`.
   Replaces ad-hoc null checks; single-pass structural validation.
2. **State observation** — `observer.entity(e, "label")` or `observer.observable(o, "label")`.
   Terminal operation chosen by the **consumer**.
3. **Realtime monitoring** — `MonitoringMode.ALWAYS` emits metric per constraint inspected;
   `MonitoringMode.ON_FAILURE` (default) emits per violation.

### Producer vs consumer — who throws
- **Arguments throw.** Producer refuses invalid input (boundary contract) —
  `throwWhenInvalid()` always.
- **Output observes.** Producer emits metrics via `observe()` and returns the value.
  Consumer decides whether to reject/degrade/log.
- **Received state is consumer's choice.** Method observing a received `Observable`
  *is* the consumer; choose terminal op based on data-flow requirement.

### Scoped observation points
Fully-qualified dotted-path scope: `CompoundQuery.getByName.c.compoundInfo.formula`.
Used as metric tag and exception message path.

### Constraint graph walking
Single pass. Tree rooted at observed object. `ObservableConstraint` wraps child
`Entity`/`Aggregate`/`ValueObject` and the walker descends. Result is a
`Set<Constraint<?>>` with deterministic `LinkedHashSet` order, partitioned by
`InvariantObservation` into valid/invalid.

### Logging is discouraged
Traditional `logger.info`/`logger.debug` is unstructured and becomes noise. Use
structured metric emission at observation points. Log for exception diagnostics only —
the `InvariantViolationException` message already serialises everything needed.

### Metrics API
`Metric` fluent builder in `com.naturalist.observability`. Domain never touches
Micrometer types. `Metric.counter(name).tag(k,v).incrementCounter()` delegates to
`io.micrometer.core.instrument.Metrics` internally.

### Two meters, two concerns
- **`naturalist.observation`** — emitted in `ALWAYS` mode per constraint. Tags:
  `constraint`, `class`, `method`, `valid`. High cardinality, opt-in.
- **`naturalist.invariant.violation`** — emitted regardless of mode per failing
  constraint. Tags: `constraint`, `class`, `method`. Low cardinality, always on.

### Micrometer as first-class dependency
`io.micrometer:micrometer-core` is a compile dependency of `kernels/framework` (lighter
than Jackson; minimal transitive footprint). Delegates to `Metrics.globalRegistry` — Spring
Boot actuator wires it in production; no-op `CompositeMeterRegistry` in tests unless a
`SimpleMeterRegistry` is registered.

### Naming conventions
Lowercase, dot-separated. `Metric` silently normalises invalid input (spaces/underscores →
dots, consecutive dots collapse, null/blank → `"unknown"`). Tag values preserved verbatim.

## Type reference

| Type | Package | Role |
|---|---|---|
| `Observable` | `com.naturalist.observability` | Declares `invariants()` on every domain type |
| `Constraint<V>` | `com.naturalist.observability` | Reified invariant rule |
| `Constraints` | `com.naturalist.observability` | Fluent builder |
| `ConstraintCollection` | `com.naturalist.observability` | Composite node for graph descent |
| `Observer` | `com.naturalist.observability` | Entry point — static final, class-scoped |
| `MethodObserver` | `com.naturalist.observability` | Method-scoped with `Class.method.label` paths |
| `InvariantObservation` | `com.naturalist.observability` | One graph walk — emits metrics, exposes `violations()` |
| `InvariantViolationException` | `com.naturalist.exception` | All violations from one pass |
| `Metric` / `Metric.Tag` | `com.naturalist.observability` | Fluent counter |
| `MonitoringMode` | `com.naturalist.observability` | `ON_FAILURE` (default) / `ALWAYS` |
| `ObservableConstraint` | `com.naturalist.observability.constraints` | Composite for Entity/Aggregate/VO |
| `NotNullConstraint`, `NotBlankConstraint` | constraints | Primitive checks |
| `PersistenceIdConstraints`, `EntityNameConstraints`, `NamedValueConstraints` | constraints | Typed validation |

## Consequences

- Single source of truth — `invariants()` drives validation, errors, metrics
- Structural validation replaces ad-hoc checks
- Full-graph visibility — single observation walks the entire constraint tree
- Monitoring toggle without code changes (`ON_FAILURE` ↔ `ALWAYS`)
- Evans-aligned terminology maps to DDD Ch.9
- **Negative:** deep aggregate graphs produce large flattened sets per observation —
  prefer `ON_FAILURE` and shallow graphs for hot paths
- **Negative:** `ALWAYS` mode in high-throughput paths produces high tag cardinality —
  configure Micrometer backend with cardinality limits
