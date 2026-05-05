# FU-1 — Family/Genus catalog tiers (execution plan)

Resolution path for FU-1, settled in chat: **accretion model with peer aggregates per
rank.** `InsectFamily`, `InsectGenus`, `InsectSpecies` are three first-class catalog
aggregates; `PlantFamily`, `PlantGenus`, `Plant` are the three plant peers. Identification
adds entities — never mutates them. Every catalogued species implies its genus and family
are also catalogued.

This is too large for one PR. Three sequenced PRs, each independently shippable, each
ending with the console reflecting the new tier.

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

## PR-1 — Object model (kernel/taxonomy interfaces)

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

- **PR-2a** — `InsectFamily` aggregate + `InsectFamilyName` identifier +
  `TestInsectFamilySource` + repository stack + `insect-families.json`.
  No catalog wiring, no query, no species changes.
- **PR-2b** — `PlantFamily` aggregate, same shape.
- **PR-2c** — `InsectGenus` aggregate, same shape (depends on `InsectFamilyName`
  for the upward reference).
- **PR-2d** — `PlantGenus` aggregate, same shape.
- **PR-2e** — Catalog wiring for the four new aggregates (DomainIds,
  CatalogContributions, EntityReferences providers) + queries
  (`InsectFamilyQuery`, `PlantFamilyQuery`, `InsectGenusQuery`, `PlantGenusQuery`)
  + their adapters in `*-core`.
- **PR-2f** — Species narrowing: `Plant` and `InsectSpecies` gain typed
  `genusName` reference, JSON migration adds the field per record, pending
  records move out of the species JSONs into their family/genus homes.
- **PR-2g** — A1-F1 closure: bundle JSON re-emit, `TestInsectsIdentifiers` /
  `TestPlantsIdentifiers` cleanups, `99-followups.md` FU-1 retirement,
  `01-findings.md` A1-F1 CONTINGENT → CLOSED, cross-rank catalog validation
  activation.

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

## PR-3 — Console

**Ships.** Naturalist-facing UI for the family and genus catalog tiers, with the
species pages updated to link up the chain.

**Console.**

- New view: family catalog list + detail per organism domain. Detail page shows
  description, common names, child genera (with links), child species count.
- New view: genus catalog list + detail per organism domain. Detail page shows
  description, common names, parent family link, child species (with links).
- Existing species detail pages: link up to genus (and transitively family) via the
  `genusName` reference now present on every species record.
- Family and genus pages designed to host future identification-key data (FU-3)
  without a structural rewrite — leave room for characteristic blocks even if
  empty in PR-3.

**Out of PR-3.**

- Identification-key data (FU-3) — not in this plan.
- Any further entity changes — PR-3 is read-only against the model from PR-2.

---

## Sequencing notes

- PR-1 lands kernel-only with the hard narrow in place. The kernel module compiles
  and tests pass via test-doubles, but `domains/plants` and `domains/insects` will
  not compile against the new `LinnaeanSpecies` until PR-2 supplies the typed
  `genusName` reference. Land PR-2 tightly behind PR-1 to keep main's intermediate
  state short.
- PR-2 is the heavy PR. It depends on PR-1's interfaces and introduces the four
  new identifier types. Editorial fill-in of family/genus descriptions is the
  pre-flight gate — descriptions written before merge, not as follow-up commits.
- PR-3 depends on PR-2's repositories and queries. Console-only; no further entity
  changes.
- Each PR keeps the build and bundle green. The swallowtail bundle re-emits in
  PR-2 (entity-side closure); PR-3 renders it.

## Memory notes for next-chat handoff

- User runs `mvn` locally — don't invoke; print the build commands.
- User runs `git mv` / `git rm` / `git add` — print, don't run.
- Scratch files go to `/Users/pat/dev/naturalist/temp/`, not `/tmp/`.

## Documents to read at start of each PR

1. This plan (`docs/notes/fu-1-plan.md`).
2. `docs/pressure-test/battus-philenor/structural-commitments.md` §6 (the resolution
   path is no longer "TBD" — it is the accretion model).
3. `docs/pressure-test/battus-philenor/01-findings.md` §A1-F1 (closes in PR-3).
4. `kernels/CLAUDE.md`, `domains/CLAUDE.md`, target domain's `CLAUDE.md`.
