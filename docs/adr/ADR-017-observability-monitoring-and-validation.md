# ADR-017: Observability, Monitoring, Validation

> [rationale](rationale/ADR-017-observability-monitoring-and-validation.md)

Every domain type implements `Observable` and declares `invariants()`. `Observer` walks
the constraint graph at method boundaries, repository insertion, query results, event
processing. Structural, not cross-cutting.

**Terminology**

- **Invariant** — domain predicate via `Observable.invariants()` returning
  `Consumer<? extends Constraints>`.
- **Constraint** — reified rule (`Constraint<V>`); named, evaluable, carries value.
- **InvariantObservation** — result of one graph walk; exposes `violations()`.
- **InvariantViolationException** — all violations from one pass (no iterative discovery).

**Three roles, one walk**

1. **Argument validation** — `observer.arguments("method", i -> i.entityName(name, "name")).throwWhenInvalid()`.
2. **State observation** — `observer.entity(e, "label")` or `observer.observable(o, "label")`.
   Terminal op chosen by the **consumer**.
3. **Realtime monitoring** — `MonitoringMode.ALWAYS` emits per constraint inspected;
   `MonitoringMode.ON_FAILURE` (default) emits per violation.

**Producer vs consumer (control flow)**

- **Arguments throw.** Producer refuses invalid input — `throwWhenInvalid()` always.
- **Output observes.** Producer emits metrics via `observe()` and returns the value; consumer decides.
- **Received state is the consumer's call.** Method observing a received `Observable` *is*
  the consumer; choose terminal op accordingly.

**Scoped paths.** Fully-qualified dotted path: `CompoundQuery.getByName.c.compoundInfo.formula`.
Used as metric tag and exception message path.

**Graph walk.** Single pass. Tree rooted at observed object. `ObservableConstraint` wraps
child `Entity`/`Aggregate`/`ValueObject` and the walker descends. Result is a
`Set<Constraint<?>>` in deterministic `LinkedHashSet` order, partitioned by
`InvariantObservation` into valid/invalid.

**Logging discouraged.** Use structured metric emission at observation points. Log only
for exception diagnostics — `InvariantViolationException` serialises everything needed.

**Metrics.** `Metric` fluent builder in `com.naturalist.observability`. Domain never
touches Micrometer types. `Metric.counter(name).tag(k,v).incrementCounter()` delegates
to `io.micrometer.core.instrument.Metrics`.

**Two meters**

- `naturalist.observation` — emitted in `ALWAYS` mode per constraint. Tags: `constraint`,
  `class`, `method`, `valid`. High cardinality, opt-in.
- `naturalist.invariant.violation` — emitted per failing constraint regardless of mode.
  Tags: `constraint`, `class`, `method`. Low cardinality, always on.

**Naming.** Lowercase, dot-separated. `Metric` normalises invalid input (spaces/underscores →
dots, consecutive dots collapse, null/blank → `"unknown"`). Tag values preserved verbatim.

## Type reference

| Type                                                                         | Package                                    | Role                                             |
|------------------------------------------------------------------------------|--------------------------------------------|--------------------------------------------------|
| `Observable`                                                                 | `com.naturalist.observability`             | `invariants()` declaration                       |
| `Constraint<V>`                                                              | `com.naturalist.observability`             | Reified rule                                     |
| `Constraints`                                                                | `com.naturalist.observability`             | Fluent builder                                   |
| `ConstraintCollection`                                                       | `com.naturalist.observability`             | Composite node for graph descent                 |
| `Observer`                                                                   | `com.naturalist.observability`             | Entry point — static final, class-scoped         |
| `MethodObserver`                                                             | `com.naturalist.observability`             | Method-scoped; `Class.method.label` paths        |
| `InvariantObservation`                                                       | `com.naturalist.observability`             | One walk — emits metrics, exposes `violations()` |
| `InvariantViolationException`                                                | `com.naturalist.exception`                 | All violations from one pass                     |
| `Metric` / `Metric.Tag`                                                      | `com.naturalist.observability`             | Fluent counter                                   |
| `MonitoringMode`                                                             | `com.naturalist.observability`             | `ON_FAILURE` (default) / `ALWAYS`                |
| `ObservableConstraint`                                                       | `com.naturalist.observability.constraints` | Composite for Entity/Aggregate/VO                |
| `NotNullConstraint`, `NotBlankConstraint`                                    | constraints                                | Primitive checks                                 |
| `PersistenceIdConstraints`, `EntityNameConstraints`, `NamedValueConstraints` | constraints                                | Typed validation                                 |
