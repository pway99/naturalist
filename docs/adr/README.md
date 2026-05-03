# Architecture Decision Records

Decisions that shape the structure and constraints of The amateur Naturalist.
Each ADR records the context, the decision, and the consequences.
Closed decisions are not revisited without a new ADR.

| ADR                                                           | Title                                                                                   | Status                |
|---------------------------------------------------------------|-----------------------------------------------------------------------------------------|-----------------------|
| [ADR-001](ADR-001-repository-architecture.md)                 | Repository Architecture                                                                 | Accepted              |
| [ADR-002](ADR-002-repository-behavioral-contract.md)          | Repository Behavioral Contract via Test Interface                                       | Accepted              |
| [ADR-003](ADR-003-java-records-no-lombok.md)                  | Java Records, No Lombok                                                                 | Accepted              |
| [ADR-004](ADR-004-modular-monolith.md)                        | Modular Monolith, No Microservices                                                      | Accepted              |
| [ADR-005](ADR-005-entity-identity-model.md)                   | Entity Identity Model — CatalogEntity and FactEntity                                    | Superseded by ADR-022 |
| [ADR-006](ADR-006-command-query-separation.md)                | Command Query Separation                                                                | Accepted              |
| [ADR-007](ADR-007-first-principles-problem-solving.md)        | First Principles Problem Solving                                                        | Accepted              |
| [ADR-008](ADR-008-iteration-velocity-and-bounded-risk.md)     | Iteration Velocity and Bounded Risk                                                     | Accepted              |
| [ADR-010](ADR-010-query-design-contract.md)                   | Query Design Contract                                                                   | Draft                 |
| [ADR-011](ADR-011-behavioral-collections.md)                  | Behavioral Collections                                                                  | Draft                 |
| [ADR-012](ADR-012-static-factory-construction.md)             | Static Factory Construction                                                             | Draft                 |
| [ADR-013](ADR-013-value-object-contract.md)                   | Value Object Contract                                                                   | Draft                 |
| [ADR-014](ADR-014-named-values.md)                            | Named Values                                                                            | Draft                 |
| [ADR-015](ADR-015-bigdecimal-numeric-precision.md)            | BigDecimal for Decimal Domain Values                                                    | Draft                 |
| [ADR-017](ADR-017-observability-monitoring-and-validation.md) | Observability, Monitoring, and Validation                                               | Draft                 |
| [ADR-019](ADR-019-pull-request-size-and-review-fatigue.md)    | Pull Request Size and Review Fatigue                                                    | Accepted              |
| [ADR-020](ADR-020-namespace-interface-pattern.md)             | Namespace Pattern for API Surface                                                       | Accepted              |
| [ADR-021](ADR-021-persistenceid-is-adapter-internal.md)       | PersistenceId is Adapter-Internal; Cross-Entity References Use EntityName               | Superseded by ADR-022 |
| [ADR-022](rationale/ADR-022-entity-identity-unified.md)       | Entity Identity — `NamedEntity` (slug) and `Entity` (UUIDv7), Unified `Named<KEY>` Port | Accepted              |
| [ADR-025](ADR-025-di-via-marker-annotations.md)               | DI via Marker Annotations                                                               | Accepted              |
| [ADR-026](ADR-026-resilience-first-order-concern.md)          | Resilience as a First-Order Concern                                                     | Accepted              |

## Archived (rationale only, not in active read path)

- ADR-009 — Accuracy, Precision, and Domain
  Authority · [rationale](rationale/ADR-009-accuracy-precision-and-domain-authority.md)
- ADR-016 — Date and Time Types · [rationale](rationale/ADR-016-date-time-types.md)
- ADR-018 — Third-Party Dependency Policy · [rationale](rationale/ADR-018-third-party-dependency-policy.md)
- ADR-023 — Open `DomainId` — Slug Uniqueness via Assembly Validation · [rationale](rationale/ADR-023-open-domainid.md)
- ADR-024 — `apps/` and `adapters/` Trees — Placement Rule · [rationale](rationale/ADR-024-apps-and-adapters-trees.md)
