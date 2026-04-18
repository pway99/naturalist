# ADR-013: Value Object Contract

**Status:** Draft

## Context

`ValueObject` is a first-class interface in the framework. Every domain model uses value
objects as members of entities. Without a strict boundary, entities accumulate rich
value objects that quietly cross into aggregate territory — collecting child entities,
acting as informal natural keys, or becoming projections of the entity they belong to.
The boundary exists to protect the identity model: entities have identity, aggregates
own child entities, and value objects are neither.

## Decision

A type may implement `ValueObject` only if it satisfies all four constraints
simultaneously. Failing any one requires reclassification.

### Constraint 1 — No Entity or Aggregate Members

A `ValueObject` must not contain an `Entity`, an `Aggregate`, or a collection of either.
A type that owns child entities is an `Aggregate`. Putting that ownership inside a
`ValueObject` on an `Entity` smuggles aggregate responsibility into the entity without
declaring it. The compiler will not catch this; the rule must be enforced by review.

```java
// Correct — CompoundInfo owns only primitives, enums, and other value objects
public record CompoundInfo(
        String formula,
        @Nullable Double molecularWeight,
        CompoundType type,
        PhCharacter phCharacter,
        Set<PeriodicElement> constituentElements   // Set of enum — fine
) implements ValueObject { ... }

// Wrong — SoilProfile must not own a collection of entities
public record SoilProfile(
        List<Amendment> appliedAmendments   // Amendment is an Entity — invalid
) implements ValueObject { ... }
```

### Constraint 2 — Must Not Uniquely Identify an Entity

A `ValueObject` must not be designed to serve as a natural key or surrogate identity
for an entity. If a value object can uniquely identify an entity — even coincidentally —
it is doing identity work. Identity work belongs to `EntityName` and `PersistenceId<Long>`.

The structural enforcement is direct: if a concept can serve as a natural key, it is
modelled as an `EntityName` subclass. `EntityName` is not a `ValueObject`. Any type
that starts being used by callers to look up entities has revealed itself as an
identifier and must be reclassified.

```java
// Wrong — if this combination is unique per compound it is an identifier, not a value
public record CompoundClassification(CompoundType type, String formula) implements ValueObject { ... }

// Correct — formula is a unique identifier component; it belongs on CompoundName or
// as an @EntityIdentifier field, not inside a generic value object
```

### Constraint 3 — A Cohesive Domain Concept in the Ubiquitous Language

A `ValueObject` must represent a concept a domain expert can define independently of
any particular entity. The ubiquitous language test: ask a domain expert "what is X?"
If the answer requires naming a specific entity ("it's the solubility data for a
compound"), X is a projection. If the answer is a standalone definition ("it describes
how readily a substance dissolves across temperature ranges"), X is a domain concept
and a legitimate value object candidate.

This is not a soft guideline. A type that fails the ubiquitous language test is a
projection or a DTO and must not be modelled as a `ValueObject`.

### Constraint 4 — Collective Meaning, Not a Projection

The members of a `ValueObject` must have meaning as a whole. A value object is not a
trimmed-down view of an entity's fields assembled for convenience. If reviewing the
members reveals that they are a subset of a specific entity's components — even
reordered or renamed — the type is a projection and does not belong in the domain model.

```java
// Wrong — this is just Compound with fewer fields; it has no independent meaning
public record CompoundSummary(CompoundName name, CompoundType type) implements ValueObject { ... }

// Correct — SolubilityProfile describes a physical phenomenon; its members are
// coherent as a standalone concept regardless of which compound they describe
public record SolubilityProfile(
        double solubility,
        TemperatureCelsius referenceTemperature,
        SolubilityClass classification
) implements ValueObject { ... }
```

## Reclassification Guide

| Symptom | Correct classification |
|---------|----------------------|
| Contains `Entity` or `Aggregate` members | `Aggregate` |
| Can serve as a natural key for an entity | `EntityName` subclass |
| Only makes sense in the context of a specific entity | Projection / DTO (not a domain type) |
| Is a subset of an entity's fields | Projection / DTO (not a domain type) |

## Consequences

- The entity–aggregate boundary is structurally protected: aggregates must be declared
  as `Aggregate`, not discovered by accident
- `EntityName` remains the single mechanism for entity identification; value objects
  cannot quietly absorb that role
- Value objects are domain vocabulary — every `ValueObject` in the codebase should be
  recognisable to a domain expert without explanation
- Projection and read-model concerns are explicitly outside the domain model; if a
  consumer needs a shaped view of an entity, that is a query concern (ADR-010), not
  a value object concern
- Review checklist: any new `ValueObject` implementation must be verified against all
  four constraints before merge
