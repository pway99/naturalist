# ADR-013: Value Object Contract

> [rationale](rationale/ADR-013-value-object-contract.md)

A type may implement `ValueObject` only if all four hold. Failing any → reclassify.

1. **No Entity or Aggregate members.** Type owning child entities is an `Aggregate`.
   Compiler won't catch — enforce by review.
2. **Must not uniquely identify an entity.** Identity belongs to `EntityName` and
   `PersistenceId<Long>`. If a type can serve as a natural key, reclassify as an
   `EntityName` subclass.
3. **Cohesive domain concept in the ubiquitous language.** Ubiquitous-language test: ask
   a domain expert what X is. Answer naming a specific entity → projection. Standalone
   definition → legitimate VO.
4. **Collective meaning, not a projection.** Members meaningful as a whole. A subset of
   an entity's fields (even reordered/renamed) is a projection/DTO.

| Symptom                                  | Correct classification |
|------------------------------------------|------------------------|
| Contains `Entity`/`Aggregate` members    | `Aggregate`            |
| Can serve as natural key                 | `EntityName` subclass  |
| Only makes sense in one entity's context | projection/DTO         |
| Subset of an entity's fields             | projection/DTO         |
