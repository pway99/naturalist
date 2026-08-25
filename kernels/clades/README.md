# clades

The evolutionary tree of life as a curated, controlled vocabulary. It holds the
branch structure and each clade's descriptive metadata — who is whose parent, and
how a UI talks about each node. It declares no traits of its own: what is true of
a clade is decided by the domain that cares.

---

## What it provides

**`Clade` — a sealed node in the tree.** A `sealed interface` with one stateless
`record` permit per recognised clade (`Eukaryota` at the root, through `Animalia`,
`Arthropoda`, `Insecta`, `Holometabola`, `Lepidoptera`, the plant lineage under
`Plantae`, and others). Each permit carries its `slug()`, `displayName()`,
four-level Durrell `description()`, and an `Optional<Clade> parent()` — empty at
the root. Adding a clade is a deliberate kernel change, reviewable in code, not
free-text data entry. It serializes as its slug string and rebuilds via a
`@JsonCreator` `of(slug)`.

**`CladeTraversal` — walking the parent chain.** `ancestry(start)` returns the
chain from a clade to the root. `findTrait(start, traitType, traitsFor)` walks
upward and returns the nearest declared trait of a given type — where `traitsFor`
is a `Function<Clade, Set<Trait>>` the caller supplies. `CladeCatalog` provides
further static lookups over the permits.

**`Trait` — an open marker for domain-owned facts.** A marker interface for typed
declarations attached to a clade ("members of this clade undergo complete
metamorphosis"). It is intentionally *not* sealed: trait types are open across the
codebase. The kernel stores and interprets none of them.

---

## Why it looks the way it does

The clade catalog is small and curated, and it needs single-tree identity —
value-equal references to the same logical node across every domain, via record
`equals`/`hashCode` — so trait inheritance by traversal works. Sealed records give
that plus compile-time discoverability (open `Holometabola.java` to see what it
is) without entity machinery: no repository, no write path, no JSON seed.

Traits stay domain-owned because a fact about a clade is a domain judgment:
insects-api decides `Holometabola` is holometabolous; a plant domain would decide
separately what its own clades do. The traversal helper takes the trait function
from the caller, so there is no shared registry and no startup wiring — the two
domains coexist because the clade *values* are value-equal across both.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the sealed-vocabulary rationale in context.
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) —
  a type-by-type tour of the clade permits and helpers.
- [`docs/plans/clades-kernel.md`](../../docs/plans/clades-kernel.md) — the
  multi-phase plan for the kernel.
