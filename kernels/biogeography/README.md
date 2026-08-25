# biogeography

A small, curated vocabulary of named biogeographical regions — used to record
where an organism is native, where a field observation was made, or where a
heritage selection program is rooted.

---

## What it provides

**`Bioregion`.** A `sealed interface` permitting only the regions the
application explicitly recognises, one stateless `record` per region:
`SacramentoValley`, `SouthernCascades`, `KlamathMountains`, `CoastRanges`,
`SierraNevada`, `ModocPlateau`. Adding a region is a deliberate code change, not
free-text data entry, which keeps the catalog cartographically consistent and
rules out variant spellings of the same place.

Each permit carries a `slug()`, a `displayName()`, and a four-level Durrell
`Description` (from `field-notes`), so the application can speak about a place at
any audience level from preschool to university. Region naming and extent track
the EPA Level III ecoregion boundaries where those coincide with vernacular
usage.

Jackson serialises a `Bioregion` as its slug string and reconstructs it through
`Bioregion.of(slug)`; an unknown slug throws `IllegalArgumentException`. The
hierarchy is intentionally flat — a containing-region parent can be added later
if a concrete need to reason about containment arises.

This shape — sealed interface plus record permits for a controlled vocabulary —
is the same one the `clades` kernel uses for the tree of life.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — shared-kernel conventions.
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) — the biogeography kernel in context.
