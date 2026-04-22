# ADR-013: Value Object Contract

**Status:** Draft
**Full rationale:** [rationale/ADR-013-value-object-contract.md](rationale/ADR-013-value-object-contract.md)

## Decision

A type may implement `ValueObject` only if it satisfies **all four** constraints
simultaneously. Failing any one requires reclassification.

### Constraint 1 — No Entity or Aggregate members
No `Entity`, `Aggregate`, or collection of either. A type owning child entities is an
`Aggregate`. Compiler will not catch this — enforced by review.

### Constraint 2 — Must not uniquely identify an entity
Identity work belongs to `EntityName` and `PersistenceId<Long>`. If a type can serve as
a natural key — even coincidentally — it is doing identity work and must be reclassified
as an `EntityName` subclass.

### Constraint 3 — Cohesive domain concept in the ubiquitous language
Ubiquitous-language test: ask a domain expert "what is X?" If the answer names a specific
entity ("the solubility data for a compound"), X is a projection. If it's a standalone
definition ("how readily a substance dissolves across temperature ranges"), X is a
legitimate value object.

### Constraint 4 — Collective meaning, not a projection
Members must have meaning as a whole. A subset of an entity's fields (even reordered or
renamed) is a projection/DTO, not a value object.

## Reclassification guide

| Symptom | Correct classification |
|---|---|
| Contains `Entity` or `Aggregate` members | `Aggregate` |
| Can serve as natural key | `EntityName` subclass |
| Only makes sense in context of a specific entity | Projection / DTO (not a domain type) |
| Subset of an entity's fields | Projection / DTO (not a domain type) |

## Consequences

- Entity/aggregate boundary structurally protected — aggregates declared, not discovered
- `EntityName` remains the single entity-identification mechanism
- Value objects are domain vocabulary recognisable to a domain expert
- Projection/read-model concerns are query concerns (ADR-010), not value-object concerns
- Review checklist: every new `ValueObject` verified against all four constraints before merge
