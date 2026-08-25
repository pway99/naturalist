# catalog-inmem

The in-process adapter for the [`catalog`](../catalog/) port. It assembles a
`Catalog` from the contributions and providers an app wires together, and
answers inverse queries by fanning out across those providers in memory. Its
dependencies are light — only the framework's existing third-party set — which
is why it lives under `kernels/` rather than `adapters/`.

---

## What it provides

**`CatalogAssembly` — the composition-root factory.** A set of static `from(...)`
overloads that take a domain's forward `CatalogContribution`s and inverse
`EntityReferences` providers (and, at runtime, the registered `DomainId`s) and
return an assembled `Catalog`. Each app collects the pieces for the domains it
includes and makes a single declarative call; no module under `domains/` or
`kernels/` fans in to every domain to build a global catalog.

**Startup slug-uniqueness validation.** Assembly checks that no two registered
`DomainId` instances share a slug and fails fast with an `IllegalArgumentException`
on collision. Passing the registered `DomainId`s explicitly lets the check cover
a domain even before its contributions are wired, so a misnamed domain is
rejected at startup regardless of registration order.

**`InMemoryCatalog` — the adapter itself.** Immutable after assembly and safe for
concurrent reads. Its inverse-direction fan-out is resilience-wrapped: each
provider invocation runs under a `catalog.fanout` circuit breaker over a timeout
resolved from the supplied `Resilience` facade, so a wedged or failing provider
degrades only its own domain's slice of the response. The facade defaults to a
no-op for tests and unwired composition roots.

---

## Why it looks the way it does

Choosing the in-memory adapter is an import-site decision: a future
heavier-weight backend (for example a Lucene-backed registry) would ship as its
own sibling module with its own assembly factory, so swapping it in is visible at
the composition root and invisible to contributing domains and consumers. Backends
that carry heavy infrastructure dependencies belong under `adapters/`, not here.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the shared-kernel conventions; the
  catalog-inmem section covers the fan-out and resilience wrapping.
- [ADR-024 — `apps/` and `adapters/` trees, placement rule](../../docs/adr/rationale/ADR-024-apps-and-adapters-trees.md)
- [ADR-026 — Resilience as a first-order concern](../../docs/adr/ADR-026-resilience-first-order-concern.md)
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) — §8, the catalog-inmem adapter in detail.
