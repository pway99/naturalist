# FU-1 — Family/Genus catalog tiers (execution plan)

Resolution path for FU-1, settled in chat: **accretion model with peer aggregates per
rank.** `InsectFamily`, `InsectGenus`, `InsectSpecies` are three first-class catalog
aggregates; `PlantFamily`, `PlantGenus`, `Plant` are the three plant peers. Identification
adds entities — never mutates them. Every catalogued species implies its genus and family
are also catalogued.

This is too large for one PR. Sliced into per-entity PRs so each is reviewable in one
sitting; main stays buildable at every PR boundary.

---

## Reframed 2026-05-10 — remaining slices fold into identification roadmap

The kernel + aggregate work (PR-1 / PR-2a–e) is complete and remains the foundation. The
remaining slices have been reshaped per Pat's 2026-05-10 reframing — pressure tests guide
development; build the capability A1-F1 surfaced rather than shorten the path to closure:

- **PR-2f** (species narrowing) is **no longer a mass migration**. Under the original
  plan, the non-null `genusName` invariant on `InsectSpecies` / `Plant` would have
  orphaned 10 of 16 insect species and 5 of 22 plants that carry partial taxonomy.
  Deleting those records (or downgrading them to placeholder slugs) destroys domain
  content the app exists to support. Species narrowing now happens **opportunistically
  per organism** through identification roadmap Phase 2's workflow — as a naturalist
  firms an identification, the genus-rank record becomes (or spawns) a species-rank
  record. The invariant tightens only after every record satisfies it.
- **PR-2g** (A1-F1 closure) is no longer a single bundle re-emit. The closure happens
  when *Battus philenor* completes the identification workflow with an EOL-grounded
  citation in roadmap Phase 4.
- **PR-3** (console family/genus views) is subsumed by roadmap Phase 0, which adds a
  prerequisite step: **data reorganization** — move the under-identified records to
  their actual rank in the existing family/genus JSONs and re-anchor life-stage
  observations. The console then renders the reorganized data.

The original PR-2f / PR-2g / PR-3 specifications below remain as reference for the kernel
shape and JSON migration semantics, but their execution shape is now per-organism inside
the identification roadmap. See [`docs/plans/identification.md`](../identification.md)
and [`docs/work-tracker.md`](../../work-tracker.md) for the current trajectory.

---

## RESUME HERE — current state (snapshot for next-chat handoff, 2026-05-06)

**Landed / pushed (`git log --oneline -10`):**

- **`9f6aed6`** PR-2e — read-side queries (`InsectQuery.FamilyQuery/GenusQuery`,
  `PlantQuery.PlantFamilyEntityQuery/PlantGenusEntityQuery`) + adapters in
  `*-core`, catalog contributions extended to emit family + genus, test contexts
  rewired.
- **`ba00948`** `ForeignKeyConstraint` kernel — sibling of `UniqueConstraint`,
  resolves intra-domain FKs via `NaturalistDatabase.getNamed`. Wired:
  `InsectGenus.familyName → InsectFamily`, `PlantGenus.familyName → PlantFamily`.
- **`194e3f2`** `NaturalistDatabase.getNamed(Class<NTS>)` generics tightened —
  return type now bound to the requested class; FK resolver compiles without
  unchecked cast.
- **`d9873ae`** `TestEntitySource` constructor migration — every subclass now
  receives `NaturalistDatabase` via constructor; primer for FK resolution.
- **`26ac0db`** PR-2d (PlantGenus) + earlier PR-2a/b/c (families and insect genus).
- **`841ab92`** PR-1 kernel object model (`LinnaeanFamily`,
  `LinnaeanGenus<FAMILY_NAME>`, narrowed `LinnaeanSpecies<GENUS_NAME>`).

**Catalog records present today:**

| Domain  | Family | Genus | Species                                                            |
|---------|--------|-------|--------------------------------------------------------------------|
| Insects | 10 ✓   | 4 ✓   | 16 (existing, no `genusName`, doesn't implement `LinnaeanSpecies`) |
| Plants  | 14 ✓   | 5 ✓   | 19 (existing, no `genusName`, doesn't implement `LinnaeanSpecies`) |

`mvn verify` is green across the modulith.

**Up next — paged-queries gate (NOT PR-2f).** Per Pat's call on 2026-05-06,
unbounded queries (`getAllSpeciesNames`, `getAllFamilyNames`, `entityStream`,
`forSpeciesName`, etc.) are a production-availability hazard. Before PR-2f or
PR-3 grows the read surface further, the kernel needs a `Page<T>` /
cursor-based contract and the existing query/repository ports need to migrate.
See `docs/plans/archive/paged-queries-plan.md` for the design sketch and rollout
strategy.

After paged-queries lands, the FU-1 series resumes at PR-2f (Species
narrowing). PR-2f is unchanged in shape but will use the paged read surface
for any bulk migration helpers it needs.

**Suggested resume command:** open `docs/plans/archive/paged-queries-plan.md`, agree on
the kernel `Page<T>` shape and port migration order, then start with the kernel
contract change.

---

**Out of scope (deferred):**

- FU-2 — bibliography / provenance kernel. Pat is driving this independently.
- FU-3 — identification as a per-domain first-class concern (the guided-key workflow:
  exoskeleton? body segments? legs? — the use case that motivates the tiered catalog
  but does not require it to ship first).
- FU-4 — reclassification events.
- Subspecies (`LinnaeanSubspecies` + per-domain subspecies aggregates) — the kernel
  already has the interface stub from the original Phase-1b work; new aggregates land
  when the first subspecies entry needs cataloguing.
- Higher ranks than family (order, class) — stay as fields on `TaxonomicClassification`.
- Cross-domain reference shape *additions* at non-species rank (e.g.
  `Larva.hostPlantGenera`) — additive, not blocking, defer until first caller needs it.

---

## PR-1 — Object model (kernel/taxonomy interfaces)  — **LANDED**

**Ships.** The Linnaean type graph at the kernel level. Pure-kernel PR — no
identifiers, no domain entities, no JSON, no console. After PR-1 a reader can open
`kernels/taxonomy` and read the full Family ← Genus ← Species (← Subspecies) type
graph end-to-end.

**Kernel (`kernels/taxonomy`).**

- New `LinnaeanFamily` interface, no type parameter. Exposes `family() :
  TaxonomicFamily` (non-null). Default `familySlug() = lowerKebab(family)`. The
  implementing aggregate's typed name is exposed via its `NamedEntity` binding —
  the rank interface itself does not redundantly re-expose it.
- New `LinnaeanGenus<FAMILY_NAME extends EntityName>` interface. Exposes the upward
  typed reference `familyName() : FAMILY_NAME` (non-null), the redundant
  `family() : TaxonomicFamily` epithet (for catalog-assembly chain consistency),
  and `genus() : TaxonomicGenus` (non-null). Default `genusSlug() =
  lowerKebab(genus)`. The implementing aggregate's own typed name comes from its
  `NamedEntity<*GenusName>` binding.
- **`LinnaeanSpecies` narrows** — gains a `<GENUS_NAME extends EntityName>` type
  parameter and a `genusName() : GENUS_NAME` member (non-null), the upward typed
  reference to the parent genus aggregate. Existing `genus() : TaxonomicGenus`,
  `species() : TaxonomicSpecies`, and `binomialSlug()` are unchanged. The narrowing
  is purely the added upward reference.
- `LinnaeanSubspecies` reviewed for consistency with the new graph. Expected
  unchanged (already references parent species via typed name); this PR documents
  the contract relative to the rest of the graph.
- Kernel-only tests — test-doubles implementing each interface, exercising graph
  composition, slug derivation per rank, and the contract that a species' `genus()`
  epithet matches its (eventual) resolved genus's `genus()` epithet. Resolution
  itself is a catalog-assembly concern; the kernel verifies the contract the
  catalog will rely on.

**Out of PR-1.**

- No identifier types (`InsectFamilyName` etc.) — those are domain-side.
- No domain entity changes.

**Hard narrow.** `LinnaeanSpecies` narrows in PR-1 with no soft-default. `Plant` and
`InsectSpecies` no longer satisfy the narrowed contract — their compilation breaks
until PR-2 supplies the typed `genusName` reference. This means **PR-2 lands
tightly behind PR-1**; main is briefly in an intermediate state where the kernel is
narrowed and the entities are not yet refactored. Acceptable tradeoff — the
alternative (a temporary `default genusName() { return null }` on the kernel
interface) leaves the kernel expressive of a state we explicitly don't want.

**A1-F1.** Untouched in PR-1.

---

## PR-2 series — Refactor the entities (per-entity slices)

The original "single PR-2" is too much to review in one pass. Slice by entity:
each PR introduces one new aggregate end-to-end (record + identifier +
TestEntitySource + repository stack + JSON catalog), small enough that a
reviewer holds the whole thing in working memory. Catalog wiring and queries
follow as separate slices once the foundation is in place. Species record
narrowing and the A1-F1 closure happen in the final slice once all four new
aggregates exist.

**Slice order:**

- **PR-2a — LANDED.** `InsectFamily` aggregate + `InsectFamilyName` +
  `InsectFamilyTestEntitySource` + repository stack + `insect-families.json`
  (6 entries: tachinidae, braconidae, syrphidae, carabidae, tipulidae,
  hesperiidae).
- **PR-2b — LANDED.** `PlantFamily` aggregate + `PlantFamilyName` +
  `PlantFamilyTestEntitySource` + repository stack + `plant-families.json`
  (14 entries — every distinct family epithet from `plants.json`, with full
  Durrell descriptions).
- **PR-2c — LANDED.** `InsectGenus` aggregate + `InsectGenusName` +
  `InsectGenusTestEntitySource` + repository stack + `insect-genera.json`
  (4 entries: halictus, andrena, chrysoperla, empoasca). Extended
  `insect-families.json` by 4 (halictidae, andrenidae, chrysopidae,
  cicadellidae) to keep upward `familyName` references resolvable.
- **PR-2d — NEXT.** `PlantGenus` aggregate, same shape as PR-2c.
- **PR-2e** — Catalog wiring for the four new aggregates (DomainIds,
  CatalogContributions, EntityReferences providers) + queries
  (`InsectFamilyQuery`, `PlantFamilyQuery`, `InsectGenusQuery`, `PlantGenusQuery`)
  + their adapters in `*-core`. Cross-rank validation activates at catalog
    assembly.
- **PR-2f** — ↪ **reshaped 2026-05-10**. Original spec retained below for kernel-shape
  reference. Execution now happens per-organism through identification roadmap Phase 2:
  as a naturalist firms an identification, the affected record narrows from
  genus-rank (or family-rank) to species-rank and gains the typed `genusName`. No mass
  migration; no up-front editorial backfill. The non-null `genusName` invariant
  tightens only after every record satisfies it. Roadmap Phase 0's data reorganization
  step is the prerequisite — it moves the 10 under-identified insect species and 5
  under-identified plants *out* of their species JSONs and into the existing
  genus/family JSONs.
- **PR-2g** — ↪ **reshaped 2026-05-10**. A1-F1 closes when *Battus philenor* completes
  the identification workflow with an EOL-grounded citation (roadmap Phase 4). Bundle
  re-emit happens organically at that point; not a discrete PR.

The original "PR-2 — Refactor the entities" specification below remains the
reference for what eventually lands across the slices.

---

### Original PR-2 specification (reference for the slice series)

**Ships.** Domain side, end-to-end. After the slice series every catalog record
exists at the right rank, the cross-rank reference chain is mandatory and
resolves at startup, the swallowtail bundle re-emits under fully-binomial
catalog, and A1-F1 closes.

**Identifiers (`domains/identifiers`).**

- New: `InsectFamilyName`, `InsectGenusName`, `PlantFamilyName`, `PlantGenusName` —
  kebab-slug `EntityName` subtypes.

**Insects domain.**

- New `InsectFamily` aggregate (`NamedEntity<InsectFamilyName>`), implements
  `LinnaeanFamily`. Carries `Description`, `Set<CommonName>`, `TaxonomicFamily`,
  `TaxonomicOrder` (parent order — nullable).
- New `InsectGenus` aggregate (`NamedEntity<InsectGenusName>`), implements
  `LinnaeanGenus`. Carries `InsectFamilyName` reference (non-null), `Description`,
  `Set<CommonName>`, `TaxonomicGenus`.
- `InsectSpecies` record gains `InsectGenusName genusName` component (non-null
  invariant). Now implements the narrowed `LinnaeanSpecies`.
- Repository stacks for both new aggregates: package-private repository interface in
  api, in-memory mock, behavioral contract test, repository-test JSON loader,
  `TestInsectFamiliesSource` / `TestInsectGeneraSource`,
  `TestInsectFamiliesIdentifiers` / `TestInsectGeneraIdentifiers`.
- Public queries + adapters in `insects-core`: `InsectFamilyQuery`, `InsectGenusQuery`.
- Catalog wiring: two new `DomainId` subtypes, two new `CatalogContribution`s,
  two new `EntityReferences` providers. Common names emit as search surface tokens.

**Plants domain.**

- Mirror: `PlantFamily`, `PlantGenus` aggregates with the same stack. `Plant` record
  gains `PlantGenusName genusName` component.

**JSON data.**

- `insect-families.json` — every distinct family epithet from species + the 6
  currently-pending family-only insect entries (`tachinid-fly` → `tachinidae`, etc.).
- `insect-genera.json` — every distinct genus epithet from species + the 4 currently-
  pending insect-genus entries.
- `plant-families.json`, `plant-genera.json` — analogous; 5 pending plants are all
  genus-level so they land in `plant-genera.json`.
- `insect-species.json`, `plants.json` — every record gains `genusName` field
  derived from its existing genus epithet. Pending records (genus + family-only)
  removed from these files since they're now in the new files.
- **Editorial fill-in** for every backfilled family/genus description — full Durrell
  four-level Description for each, written before PR-2 ships. No TODO stubs.

**Catalog cross-rank validation.**

- Activates: assembly validates `InsectGenus.familyName → InsectFamily` and
  `InsectSpecies.genusName → InsectGenus` resolution. Same for plants. Fail-fast on
  unresolved or collided slugs.
- Record-time invariant on species: `genusName.slug()` matches `lowerKebab(genus())`
  (cheap local check, doesn't require resolution).

**A1-F1 closure.**

- Bundle JSON for the swallowtail story re-emitted under fully-binomial catalog.
- The three FU-1-flagged `TestInsectsIdentifiers` comments resolve — entries point
  at `InsectFamily` / `InsectGenus` records, comments removed.
- `TestPlantsIdentifiers.CreepingThyme` redirects to `PlantGenus(thymus)`.
- A1-F1 moves CONTINGENT → CLOSED. FU-1 retires from `99-followups.md`.

**Out of PR-2.**

- No console changes. Existing console pages keep working against the refactored
  data model; new family/genus pages land in PR-3.

---

## PR-3 — Console — *rolled into identification roadmap Phase 0 (2026-05-10)*

**Status.** No longer a standalone slice of FU-1. The console family + genus
views land as part of **identification roadmap Phase 0** —
[`docs/plans/identification.md`](../identification.md) — with an
identification-readiness lens layered on top of the original PR-3 scope.

**Why rolled.** The identification module needs to *render* every taxonomic
level (couplet results, session scope, pending-organism display). Building the
catalog console pages without that lens, then retrofitting them in Phase 2 of
the roadmap, would invent the same primitive twice. Phase 0 absorbs PR-3's
scope unchanged and adds:

- An audit of `InsectQuery` (and `PlantQuery` for symmetry) against the lineal
  taxonomy kernel — surfaces api gaps before identification depends on them.
- A reusable **taxonomic-scope rendering primitive** —
  breadcrumb-style ("Animalia › Arthropoda › Insecta › Coleoptera ›
  **Carabidae** › *Carabus nemoralis*"), with order-and-above as
  non-clickable labels and family-and-below as links. Phase 2 of the
  roadmap reuses this for session-scope and pending-organism display.
- Verification that the scope value object can express
  `InsectsDomain` (a `DomainId`) as the broadest starting scope, without
  requiring a Family or Genus.

**Original PR-3 scope** — preserved for traceability, lands in Phase 0:

- New view: family catalog list + detail per organism domain. Detail page shows
  description, common names, child genera (with links), child species count.
- New view: genus catalog list + detail per organism domain. Detail page shows
  description, common names, parent family link, child species (with links).
- Existing species detail pages: link up to genus (and transitively family) via the
  `genusName` reference now present on every species record.
- Family and genus pages designed to host future identification-key data (FU-3)
  without a structural rewrite — leave room for characteristic blocks even if
  empty in PR-3.

**Out of Phase 0.**

- Identification-key data — lands in roadmap Phase 2 (`InsectIdentification`).
- Any further entity changes — Phase 0 is read-only against the model from PR-2.

**Promote** [`docs/plans/identification.md`](../identification.md) Phase 0
to its own implementation plan next — Phase 0 is now fully unblocked (PR-2f /
PR-2g were reshaped 2026-05-10 and no longer ship as discrete slices).

---

## Sequencing notes

- PR-1 / PR-2a–e all landed; the kernel and the four new aggregates exist and the
  catalog wires them.
- PR-2f / PR-2g were reshaped 2026-05-10 — they no longer ship as discrete slices.
  Species narrowing happens opportunistically per organism through identification
  roadmap Phase 2; A1-F1 closes when *Battus philenor* completes that workflow
  with EOL citation in Phase 4.
- The console slice that was originally PR-3 now lands as part of identification
  roadmap Phase 0 ([`docs/plans/identification.md`](../identification.md)),
  paired with the Phase 0 **data reorganization** that moves under-identified
  records to their actual rank before the console renders them.

## Memory notes for next-chat handoff

- User runs `mvn` locally — don't invoke; print the build commands.
- User runs `git mv` / `git rm` / `git add` — print, don't run.
- Scratch files go to `/Users/pat/dev/naturalist/temp/`, not `/tmp/`.

## Documents to read at start of each PR

1. This plan (`docs/plans/archive/fu-1-plan.md`).
2. `docs/pressure-test/battus-philenor/structural-commitments.md` §6 (the resolution
   path is no longer "TBD" — it is the accretion model).
3. `docs/pressure-test/battus-philenor/01-findings.md` §A1-F1 (closes in PR-2g).
4. `kernels/CLAUDE.md`, `domains/CLAUDE.md`, target domain's `CLAUDE.md`.
