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

**Four roles, one walk**

1. **Argument validation** — `observer.arguments("method", i -> i.entityName(name, "name")).throwWhenInvalid()`.
   Values the *caller passed*.
2. **State validation** — `observer.state("method", s -> s.notNull(zone, "zone").atLeast(count, 1, "count"))`.
   Values the method *acquired or computed during its own run* (a fetched aggregate, a count, a
   looked-up mapping, a decision). Same constraint lambda and scope as arguments; terminal op
   chosen by the **consumer**.
3. **Observable observation** — `observer.namedEntity(e, "label")` or `observer.observable(o, "label")`.
   Walks the object's own `invariants()`. Terminal op chosen by the **consumer**.
4. **Realtime monitoring** — `MonitoringMode.ALWAYS` emits per constraint inspected;
   `MonitoringMode.ON_FAILURE` (default) emits per violation.

**Producer vs consumer (control flow)**

- **Arguments throw.** Producer refuses invalid input — `throwWhenInvalid()` always.
- **Output observes.** Producer emits metrics via `observe()` and returns the value; consumer decides.
- **Received state is the consumer's call.** Method observing a received `Observable` *is*
  the consumer; choose terminal op accordingly.

**Scoped paths.** Fully-qualified dotted path: `CompoundQuery.getByName.c.compoundInfo.formula`.
Used as metric tag and exception message path.

**Decisions, not just arguments.** A business rule an operation decides (an email already
registered, a token already consumed, a login against a suspended account) is observed as a
predicate-named constraint — `isTrue(existing.isEmpty(), "emailUnregistered")`,
`isFalse(token.isConsumed(), "isConsumed")` — via `observer.forMethod(m).state(...).observe(WARN)`
*before* the exception the operation already throws; the thrown message never changes. The
`name` is the predicate method a reviewer can open, so the metric tag reads as the reason.
Security-critical classes (accounts, authentication, registration) run `MonitoringMode.ALWAYS`
so the same names with `valid=true` give the baseline the violations stand out against;
`observe(level)` tags those valid rows `INFO` regardless of the level chosen for violations.

**Graph walk.** Single pass. Tree rooted at observed object. `ObservableConstraint` wraps
child `Entity`/`Aggregate`/`ValueObject` and the walker descends. Result is a
`Set<Constraint<?>>` in deterministic `LinkedHashSet` order, partitioned by
`InvariantObservation` into valid/invalid.

**Logging discouraged.** Use structured metric emission at observation points. Log only
for exception diagnostics — `InvariantViolationException` serialises everything needed.

**Metrics.** `Metric` fluent builder in `com.naturalist.observability`. Domain never
touches Micrometer types. `Metric.counter(name).tag(k,v).incrementCounter()` delegates
to `io.micrometer.core.instrument.Metrics`.

**One meter**

- `naturalist.invariant.check` — one increment per inspected constraint. Tags: `constraint`,
  `class`, `method`, `name`, `outcome` (`held` | `violated`), `level`, `kind` (`argument` |
  `state` | `observable` — what was observed; split on it to tell a bad caller from a bad
  dependency). The monitoring mode
  selects which outcomes are emitted: `ON_FAILURE` emits `outcome=violated` only (low
  cardinality, always on); `ALWAYS` emits `outcome=held` too (high cardinality, opt-in).
  A violation is counted once, and an alert on `outcome=violated` reads the same in either
  mode. Split on `outcome`, never `level` — `observe(Level.INFO)` tags both outcomes `info`.

*Amended 2026-11-02:* replaces the earlier `naturalist.observation` (ALWAYS, `valid` tag) +
`naturalist.invariant.violation` pair, which counted every violation twice under `ALWAYS`
and borrowed the domain's "observation" vocabulary. Renamed before any deployment.

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
| `IsTrueConstraint`, `IsFalseConstraint`                                      | constraints                                | Predicate-named decisions (`isTrue`/`isFalse`)   |
| `PersistenceIdConstraints`, `EntityNameConstraints`, `NamedValueConstraints` | constraints                                | Typed validation                                 |
