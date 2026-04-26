# Insects Domain

Reference implementation for [ADR-021](../../docs/adr/ADR-021-persistenceid-is-adapter-internal.md)
— persistence-id is adapter-internal; cross-entity references use `EntityName`.

## Domain Vocabulary

**InsectSpecies** — `NamedEntity<InsectSpeciesName>`. Catalog identity of a species as
recognised by the naturalist. Four-level Durrell `Description`. No `id()` at the domain
layer; the RDBMS adapter carries a numeric primary key privately.

**InsectImage** — `Entity<InsectImageId>`. A photograph of an observed individual.
Carries `InsectSpeciesName` (slug) as its parent reference — no `insectSpeciesId`, no
`withId`, no nullable id-shaped FK column.

**InsectAggregate** — Aggregate rooted at `InsectSpecies`, composing the species with
its `InsectImageCollection`. Assembled by name through `InsectAggregateFactory`.
