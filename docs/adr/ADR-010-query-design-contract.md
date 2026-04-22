# ADR-010: Query Design Contract

**Status:** Draft
**Full rationale:** [rationale/ADR-010-query-design-contract.md](rationale/ADR-010-query-design-contract.md)

## Decision

- **Port declaration.** Query is a public interface in `<domain>-api` — a hexagonal port.
- **Single adapter** per query, in `<domain>-core`. No multi-adapter strategy (queries are
  read models over repositories, not persistence boundaries).
- **Idempotent.** Same args → same result, always. Structural guarantee, not advisory.
- **Argument validation** via `observer().arguments(...)` before any repository access.
  Null/invalid throws `InvalidVariantException`, all violations collected in a single pass.
- **Empty-result default:** a logically valid argument yielding no result returns empty,
  not throws. Throwing requires explicit method-contract documentation.
- **Aggregate construction delegates to a package-private factory in `<domain>-core`.**
  Single concrete class. **No factory interface.** **No factory type in `<domain>-api`.**
  A factory surfacing in api is a review blocker — it leaks assembly concern and becomes
  Spring-injectable across module boundaries.
- **Multiple results return a `BehavioralCollection` subclass, not raw `List<T>`.** See
  ADR-011.

## Consequences

- Query ports public; adapters module-private
- Consumers depend only on port — adapter changes don't touch callers
- Uniform argument validation across all queries
- Aggregate construction complexity isolated in factories
- Raw `List<T>` at query boundary is a review flag
- DAG unchanged: `<domain>-core → <domain>-api`; api carries collection types alongside port
