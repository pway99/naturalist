# Library

The domain that binds claims to sources. When the catalog asserts something about
a species, a compound, or a clade, the library is where that assertion is tied to
an external authority — and where the reference material that explains the
catalog's own vocabulary lives.

It is a service domain: it attaches citations to entities in *other* domains
without importing any of their apis, reaching them through a domain-agnostic
reference instead.

---

## Core concepts

**A citation is an external-authority reference.** `Citation` is a sealed
`NamedEntity` (its one permit today is `OnlineSource`) carrying an
`AuthorityReference` — a deep link into an external catalogue such as the
Encyclopedia of Life. The type itself lives in `kernels/authority`; the library
domain hosts its query, collection, and repository.

**A citation association is the binding, and it points anywhere.**
`CitationAssociation` is an `Entity<CitationAssociationId>` linking one citation
to one cataloged entity through an `EntityRef` — a `(domain, slug)` pointer from
`kernels/catalog` that can name an insect species, a compound, a plant, or
anything else. Because the subject is a generic reference, the library never
depends on the domain it cites. It is looked up by its FK axes (`citationName`,
`subject`), not by its own surrogate id.

**Inheritance is the consumer's job, not the library's.** The library exposes the
raw associations; a consuming domain owns any aggregation over them. The insects
domain, for example, walks the Linnaean hierarchy to collect citations inherited
from ancestor ranks — the library just answers "what is attached to this
subject."

**Reference entries explain the vocabulary itself.** `Concept` is a
`NamedEntity<ConceptName>` — a human-titled, four-level Durrell `Description` of
something the catalog needs to explain (what a clade is, how ranks relate to
taxonomy). These are a parallel sub-context: a citation never references a
concept.

**Clade navigation is a read-side projection over the tree of life.** `CladeView`
(a `ReadModel`) and `CladeStep` (a `ValueObject`), served by `CladeQuery`, derive
a clade's ancestry and children directly from the sealed permits in
`kernels/clades`. There is no repository or persisted data behind them — the tree
is the kernel's vocabulary read at query time.

---

## Module layout

Follows the standard domain split described in the
[top-level README](../../README.md#architecture-at-a-glance). The api depends only
on kernels — `framework`, `identifiers`, `field-notes`, `authority`, `catalog`,
`clades`, `taxonomy` — and on no other domain. Domain-specific notes:

- The module is organized as parallel, non-interacting sub-contexts (reference
  entries, citation, citation-association, clade-navigation) that share one
  package. The first three apply ADR-020's N=1 collapse; clade-navigation has no
  repository at all.
- **`library-core`** — `CitationAssociationQueryImpl` plus the package-private
  `CladeViewFactory` and the curated clade-to-rank mapping.
- **`library-repository-rdms`** — production persistence adapter (currently
  delegating to the in-memory mock; see the top-level README).

---

## Learn more

- [`docs/briefings/library-domain.md`](../../docs/briefings/library-domain.md) —
  a full type-by-type tour of the four sub-contexts, the JSON catalogs, and the
  cross-domain consumer pattern.
- [ADR-020 — Namespace interface pattern](../../docs/adr/ADR-020-namespace-interface-pattern.md) —
  the N=1 collapse and standalone-query shapes this domain uses.
