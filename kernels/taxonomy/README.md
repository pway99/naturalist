# taxonomy

The Linnaean vocabulary organism domains share: the rank ladder, the typed rank
names, classification placement, and the rank-agnostic ancestry walk. It knows
nothing of insects or plants — each domain supplies its own entities and rung
choices over these shared types. Non-organism domains (chemistry, soil, climate)
have no use for it.

---

## What it provides

**`LinealRank` — the closed rank ladder.** An enum from `KINGDOM` down to
`SUBSPECIES`, ordered by increasing specificity. It is the position on the ladder,
independent of the typed name that carries a slug — so a rank-transition invariant
("an identification may move down the ladder, not up") uses the natural enum
comparison. Which rungs a given domain gives entities is a domain decision; the
ladder itself is shared.

**`RankName` — an organism-agnostic rank name.** An `Observable` interface exposing
a `value()` slug and its `rank()`. Each domain's sealed rank-name type (e.g.
`InsectRankName`, `PlantRankName`) extends it, so shared types — such as the
observation kernel's `OrganismObservation.subject` — can reference any domain's
rank name without depending on that domain. It serializes as a self-describing
`{"rank":…,"value":…}` object and rebuilds the concrete permit through a
domain-registered reconstructor (`RankNameSerializer` / `RankNameDeserializer` /
`RankNameReconstructor`).

**`TaxonomicClassification` — Linnaean placement.** A `ValueObject` holding order,
family, and nullable genus and species, with `binomialName()` and
`isSpeciesLevel()`. Nullable genus and species accommodate family- or genus-level
field identifications where the species cannot be confirmed.

**Rank value types and role interfaces.** `TaxonomicOrder`, `TaxonomicFamily`,
`TaxonomicGenus`, `TaxonomicSpecies`, and `TaxonomicSubspecies` are
`NamedValue<String>` records, each enforcing its own Linnaean capitalisation rule.
The `LinnaeanOrder` / `LinnaeanFamily` / `LinnaeanGenus` / `LinnaeanSpecies`
interfaces are rank-level contracts a per-domain entity implements to expose its
epithet and derived slug (`TaxonomicSlugs`).

**`RankAncestry` — the cross-rank walk.** Rank-agnostic ancestry resolution driven
by a domain-supplied `parentOf` function: `ancestry(...)` returns the subject-first
chain upward (cycle-guarded, stopping at a gap or the top), and `inherited(...)`
collects attributes attached across that chain, each tagged with the `AtRank`
it came from. It names no rung and does no ladder arithmetic, so each domain's
ladder shape is expressed entirely by its own `parentOf`.

**`OrganismFeatureView` — lineage-composite feature marks.** A `ReadModel` holding
the feature marks that apply to a taxon, grouped by the ancestor rank that
contributed them, ancestor-first. The feature record type is the domain's own —
the view holds it structurally without constraining its type.
`OrganismFeatureAssignment` and `FeatureViewAssembler` support building it.

---

## Learn more

- [`kernels/CLAUDE.md`](../CLAUDE.md) — the shared-kernel conventions.
- [`docs/briefings/shared-kernels.md`](../../docs/briefings/shared-kernels.md) —
  a type-by-type tour, including the rank value types and role interfaces.
- [`docs/plans/organism-domain-blueprint.md`](../../docs/plans/organism-domain-blueprint.md) —
  the shared rank / identification / clade design these types support.
