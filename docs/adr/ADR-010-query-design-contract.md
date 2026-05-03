# ADR-010: Query Design Contract

> [rationale](rationale/ADR-010-query-design-contract.md)

- Query is a public interface (port) in `<domain>-api`.
- Single adapter per query, in `<domain>-core`. No multi-adapter strategy — queries are
  read models over repositories, not persistence boundaries.
- Idempotent: same args → same result.
- Argument validation via `observer().arguments(...)` before any repository access.
  Null/invalid throws `InvariantViolationException`, all violations collected in one pass.
- Empty-result default: valid args yielding nothing return empty. Throwing requires
  explicit method-contract documentation.
- Aggregate construction delegates to a package-private factory in `<domain>-core`.
  Single concrete class. No factory interface. No factory type in `<domain>-api` — a factory
  surfacing in api is a review blocker.
- Multi-result methods return a `BehavioralCollection` subclass, not raw `List<T>` —
  see ADR-011. Raw `List<T>` at a query boundary is a review flag.
