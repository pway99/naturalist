# authority

The seam for consulting outside naming authorities — the Encyclopedia of Life,
iNaturalist, and their kind. It lets a domain fetch deep-links and source
material for a subject without knowing which authority answered or how it was
reached. The kernel names no concrete authority; each provider defines its own.

---

## What it provides

**`ExternalAuthority` — the port.** A consumer picks one authority and asks it
two things: `lookup(EntityName subject)` returns the deep-link references that
authority knows for a subject (an empty set means "nothing known", not an
error), and `fetchContent(AuthorityReference)` retrieves the page's textual
content. There is no fan-out across providers — the consumer chooses which
authority to call. Network-backed implementations are resilience-wrapped
(bulkhead, timeout, retry); an in-memory implementation is exempt because it
does no I/O.

**The reference value objects.** `AuthorityReference` is a self-describing,
fully resolvable deep-link into an authority's catalogue, carrying the
`AuthoritySource` it came from. `AuthoritySource` is open provider metadata — an
id and display name — so each provider names its own authority rather than the
kernel enumerating them. `AuthorityContent` carries the retrieved page text,
used as grounded source material for description generation rather than model
training data.

**`Citation` — a cited external source.** A sealed `NamedEntity` (keyed by
`CitationName`) whose one permit, `OnlineSource`, records a title, an
`AuthorityReference`, and optional author, year, and last-modified metadata. It
is the persisted, citable form of an authority reference.

---

## Learn more

- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) — §9, the authority kernel in detail.
- [ADR-026 — Resilience as a first-order concern](../../docs/adr/ADR-026-resilience-first-order-concern.md) — why network-backed authorities are resilience-wrapped.
