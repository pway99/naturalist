# Parking Lot — Resolved

Entries that landed in code or in a strategy doc. Kept here (separate
from the active [`parking-lot.md`](parking-lot.md)) so grep can still find
the resolution context without inflating the live file.

When a new resolution lands, move the entry here from `parking-lot.md`
and keep its original `PL-N` ID. IDs never reuse.

---

## PL-1 — LifeStage modeling: dual-home problem

**Raised:** 2026-05-10 (mid-slice-2 execution); reinforced 2026-05-11 after Pat's overnight research.
**Resolved:** 2026-05-13.
**Where:** Discovered while applying the green-lacewing → chrysoperla data move; slice 1 had just spread the inline-life-stage pattern from `InsectSpecies` to `InsectGenus` and `InsectFamily`.
**The smell:** `LifeStage` is a `NamedEntity` with its own repository and standalone `life-stages.json`, but is *also* held by value as a component on `InsectSpecies` (annotated `@AggregateRoot`) and now on `InsectGenus` / `InsectFamily` (plain `NamedEntity`, not aggregates per the framework rules). Two homes for the same data; framework rule violation (NamedEntity owning NamedEntity by value).
**Resolution:** Closed by Phase 5 of [`plans/clades-kernel.md`](../plans/clades-kernel.md) (slice plan: [`plans/archive/clades-kernel/phase-5.md`](../plans/archive/clades-kernel/phase-5.md)). `InsectLifeStages.stagesOf(species|genus|family)` is now the canonical answer to "which stages exist" — it walks `placedIn` → `CladeTraversal.findTrait` → `MetabolyTrait` → `Metaboly.stages()`. The inline `egg`/`larva`/`pupa`/`adult` fields are still present on the records (Phase 5 was deliberately additive) and will be removed per-organism via PL-2.

---

## PL-11 — Cross-rank functional ecology

**Resolved:** 2026-05-19.
**Resolution:** Landed as `InsectFunctionalRole` (Entity, cross-rank by `InsectRankName`) per [`plans/archive/insect-functional-role.md`](../plans/archive/insect-functional-role.md). Commits `8f6072a` (entity stack), `bcce0e0` (16 seed records + smoke/contract tests), `2b0c38a` (cross-rank `getByGuild`), `327c5db` (strip `guilds`/`beneficial` from `InsectSpecies`, console fanout). `sightingNotes` rehoming explicitly deferred to the future sightings entity — two facts unique to the deleted potato-leafhopper record (dated crimson-clover observation, "first pest documented" sequencing) noted for that slice.

---

## Q4 — Insects vs Apiary

**Resolved:** Pre-PL rename.
**Resolution:** Apiary is its own domain module (Colony aggregate root). Insects module covers Insecta (six legs). SHB control: H. indica only (not S. feltiae).
