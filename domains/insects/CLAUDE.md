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

**InsectTaxonView** — Sealed `ReadModel` over the four Linnaean ranks that carry
catalog entities: `InsectSpeciesView`, `InsectGenusView`, `InsectFamilyView`,
`InsectOrderView`. Each permit composes its rank entity with the `ImageCollection`
of photographs attached at that rank — a read-side projection, not a consistency
boundary. Identity is the root's typed `InsectRankName`, returned polymorphically
by `name()`. Assembled by name through `InsectTaxonViewFactory` and read via
`insectQuery.taxonView().getByName(rankName)`; `InsectSubspeciesName` is permitted
on `InsectRankName` but yields `Optional.empty()` (no subspecies entity exists yet).
