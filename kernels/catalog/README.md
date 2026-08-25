# catalog

The cross-domain navigation surface. It resolves references between domains by
slug alone, so one domain can point at another's entity — and the console can
show "found in:" panels — without any domain importing another's internals. The
kernel carries no domain knowledge; it knows the name of no domain.

---

## What it provides

**`Catalog` — the query surface.** A single read interface the console renders
against. It answers two complementary questions:

- **Search** (`search`, `findBySlug`) — "what does the catalog know about this
  term?" Token-based, case-insensitive matching over an assembled index, plus a
  precise slug lookup. An empty result is a first-class state: a non-empty query
  that resolves to nothing fires an observation so the catalog's growth signal
  is recorded.
- **Inverse** (`domainsReferencing`, `findReferencesTo`) — "which entities, in
  which domains, reference this one?" Fanned out to registered providers and
  grouped by domain.

**`DomainId` — an open domain identifier.** A stable, low-cardinality slug for a
participating domain, used as the owning `domain` of every `EntityRef` and as
the grouping key for inverse queries. The interface is deliberately open: each
domain ships its own `DomainId` subtype from its `*-api` module, so adding a
domain is a change in that domain, never a kernel edit.

**The contribution and reference ports.** Domains register forward-direction
`CatalogContribution`s (what they make searchable) and inverse-direction
`EntityReferences` providers (the back-references they hold). `EntityRef` and
`EntityReferences` are the typed reference records the fan-out returns;
`SearchResults`, `SearchHit`, and `MatchKind` carry search output with
deterministic ordering. `EntityRefLinker` turns a reference into a console URL.

**Observability for the empty case.** `UnresolvedSearchObservation` and
`UnresolvedReferenceObservation` surface unresolved lookups as metrics — the
signal that a domain has something the catalog cannot yet resolve.

---

## Why it looks the way it does

Slug uniqueness is a global invariant, so it is validated at assembly time and
fails fast on collision rather than resolving ambiguously at query time. Because
each domain owns its own `DomainId`, the kernel never fans in to the domains —
the wiring that knows every participating domain lives in each app's composition
root, and the kernel stays a pure resolution surface.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the shared-kernel conventions; the
  catalog section covers the reference-resolution model.
- [ADR-023 — Open `DomainId`, slug uniqueness via assembly validation](../../docs/adr/rationale/ADR-023-open-domainid.md)
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) — §7, the catalog kernel in detail.
