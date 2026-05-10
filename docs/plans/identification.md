# Identification — Roadmap (Sketch)

A multi-phase roadmap for building the per-domain identification
workflow, starting in `insects` and folding in the resolution paths
for FU-1 (under-identified organisms), FU-2 (bibliography), and FU-3
(identification as first-class).

This is a **sketch**, not a binding plan. Each phase is promoted to
its own implementation plan when its predecessor lands.

---

## Why this exists

The identification feature lets a young naturalist walk through
morphological characteristics — like a dichotomous key — and arrive
at a taxon conclusion. The app scaffolds the reasoning; the
naturalist does the thinking. Each concluded identification promotes
into a permanent collection entry in the naturalist's own words.

**Design principles:**

1. The domain never knows how data is fetched or stored.
2. The naturalist's own language is preserved alongside curated
   scientific statements.
3. Authority references are stored locally as IDs and URLs — full
   data is referenced out.
4. The local catalog grows one node at a time as naturalists use
   the key.
5. When the local key runs out of depth, deep links carry the
   naturalist further.

### ADR-009 is the structural reason

[ADR-009 — Accuracy, Precision, and Domain
Authority](../adr/rationale/ADR-009-accuracy-precision-and-domain-authority.md)
forbids the *oracle* shape: a domain model that asserts scientific
fact without exposing the reasoning. Claude explains; the reference
library authorises; the naturalist evaluates the synthesis against
the referenced material.

An AI-only identification flow — vision-assist with no curated
authority backing — *is* the oracle ADR-009 rejects. The EOL adapter
(Phase 4) is what closes the loop for the entomology slice of the
principle: every curated `TaxonCharacteristic` carries a
`LiteratureReference` (FU-2's value object). The naturalist sees the
chain back to published authority.

This is why the EOL adapter is structural completion, not
enrichment: without it, identification is an oracle.

---

## Why per-domain

Per [FU-3 in the *Battus philenor* pressure test
followups](../pressure-test/battus-philenor/99-followups.md):

> Per-domain identification surface — `InsectIdentification`,
> `PlantIdentification` — independently designed initially. Kernel
> extraction (`kernels/identification`) deferred until two domains
> prove they need the same shape — `@Incubating` discipline applied
> to a kernel candidate.

This roadmap adopts that stance unchanged. The work lands inside
`domains/insects/`. `InsectSpecies.IdentificationFeatures` is the
existing seed; the workflow evolves it rather than competing with
it. A second domain — likely `plants` — follows later, on its own
gating, and only then does kernel extraction become a real question.

A parallel `com.naturalist.entomology` module is **not** introduced
— it would duplicate `InsectSpecies` and `InsectImage`, contradict
the FU-3 stance, and ignore the pressure test's explicit "resolve
FU-1 and FU-3 together" guidance.

---

## Why Family is the top catalog tier in `insects`

The taxonomy kernel models `LinnaeanFamily`, `LinnaeanGenus`,
`LinnaeanSpecies`, `LinnaeanSubspecies` as first-class shapes;
`TaxonomicOrder` exists as a typed value but `TaxonomicClass` and
`TaxonomicPhylum` do not. Per the FU-1 plan: ranks above family
remain typed fields on `TaxonomicClassification`. That is correct
for `insects` because phylum and class do not vary within the
domain — every entity in `insects` is class Insecta, phylum
Arthropoda. Modelling them as aggregates would add zero
discriminating power.

For other domains the answer differs. `vertebrates` has class as a
real discriminator (Mammalia / Aves / Reptilia / Amphibia /
Actinopterygii); `worms` is polyphyletic and even *phylum* varies.
When those domains demand identification, `LinnaeanClass` (and
possibly `LinnaeanPhylum`) may graduate from typed field to
aggregate at that point. **Out of scope for this roadmap.** Naming
it here so the roadmap doesn't quietly assume Family is forever the
ceiling.

---

## `DomainId` as the broadest identification scope

The catalog kernel already exposes `DomainId` (ADR-023, open
interface) — a stable, low-cardinality identifier per participating
domain. `InsectsDomain` is the `DomainId` for `insects`; analogous
for plants, chemistry, and so on. Each `*-api` module ships its own
subtype.

The identification scope value object becomes a closed shape with
two members:

- A `DomainId` — broadest possible scope ("any insect"; equivalent
  to "Class Insecta starting from scratch").
- A typed taxonomic rank — `TaxonomicOrder` / `LinnaeanFamilyName`
  / `LinnaeanGenusName` — narrower scope.

A session begun at `InsectsDomain` scope produces a first couplet
that narrows to order; a session begun at `Coleoptera` skips the
order-level couplets and begins at family-level discriminators; a
session begun at `Carabidae` begins at genus. The scope and the
identification descend together.

This dovetails with the catalog: scope's `DomainId` is the same
`DomainId` the catalog already understands, so identification
sessions appear naturally as catalog-aware artifacts — they belong
to one domain by construction.

The exact shape of the scope value object (sealed interface vs.
record-with-Optional vs. visitor) is a Phase 2 decision; calling out
the substrate here so Phase 2 doesn't re-derive it.

---

## Phase summary

| Phase | Name                                          | Status   | Gates on                                  |
| ----- | --------------------------------------------- | -------- | ----------------------------------------- |
| 0     | Taxonomic navigation — api/console review     | next     | FU-1 PR-2f / PR-2g (in flight)            |
| 1     | External-source seam in console               | sketched | independent of Phase 0; can parallelize   |
| 2     | `InsectIdentification` workflow (FU-3)        | sketched | Phases 0 + 1                              |
| 3     | FU-1 closure — under-identified organisms     | sketched | Phase 2; FU-1 PR-2g                       |
| 4     | EOL trait fetch — closes ADR-009 loop         | sketched | Phase 2 (Phase 3 helpful, not strict)     |

---

## Phase 0 — Taxonomic navigation review

**Why first.** The taxonomy kernel has grown to a thorough lineal
model (Order through Subspecies). The `insects-api` reflects this
— `InsectFamily`, `InsectGenus`, `InsectSpecies` aggregates exist;
`InsectQuery` already namespaces `families()` / `genera()` /
`species()`. The console lags: `/insects` lists species directly;
there are no family or genus pages. This is also the existing FU-1
PR-3 ("Console — family + genus list/detail views"), pending.

The identification module needs to *render* every taxonomic level
to display couplet results, scope, and pending-organism records
("at family level, no further yet"). Without this, Phase 2 invents
its own family/genus presentation under time pressure.

**Delivers.**

- An audit pass of `InsectQuery` (and `PlantQuery` for symmetry)
  against the lineal kernel: are family / genus / species
  accessors symmetric, paged, identification-ready? Surface api
  gaps as targeted follow-ups inside this phase.
- Console pages: `/insects/families`, `/insects/genera`,
  family-detail (lists member genera + species), genus-detail
  (lists member species). Species detail links up the chain
  (genus → family → order).
- A reusable **taxonomic-scope rendering primitive** —
  breadcrumb-style ("Animalia › Arthropoda › Insecta › Coleoptera
  › **Carabidae** › *Carabus nemoralis*"). Order-and-above are
  non-clickable labels (no catalog page); family-and-below are
  links. Phase 2 reuses this primitive for session-scope rendering
  and pending-organism display.
- Verification that the scope value object can express
  "`InsectsDomain` as starting scope" cleanly without requiring a
  Family or Genus.

**Subsumes** FU-1 PR-3. The FU-1 work-tracker pointer redirects to
"see roadmap Phase 0" once this lands.

**Touches plants lightly.** The audit looks at `PlantQuery` for
symmetry only; actual plant family/genus console views land when
plant identification (a future, post-roadmap effort) demands them.

---

## Phase 1 — External-source seam in console

**Why.** "Console-ready for EOL" — the gating state Pat named for
the identification work. Establishes the seam now so the EOL real-
call work in Phase 4 doesn't reshape the console.

**Delivers.**

- `kernels/external-source` (or similar; naming is a Phase 1
  decision) — a vendor-neutral facade. `Resilience`-wrapped per
  ADR-026; bulkheaded, timed-out, retried per the kernel's
  facade. `NoOp` default for tests and unwired apps. Mirrors the
  shape `kernels/vision/` was sketched as in
  [`vision-assisted-identification.md`](vision-assisted-identification.md).
- API-key management — env-var read at adapter construction;
  refuse to start if absent and the feature is wired. No
  defaults, no fallback. (`ANTHROPIC_API_KEY`,
  `EOL_API_TOKEN`, etc.)
- First wired adapter: **GBIF name validation**. No auth
  required — the easiest real outbound call. Proves the
  resilience / caching / error-handling shape against a real
  upstream before the auth-required EOL work in Phase 4.
- Console: entity detail pages render external authority links
  on `InsectSpecies`. Forms accept EOL page IDs, BugGuide node
  IDs, iNat taxon IDs, GBIF usage keys as identifier-shaped
  fields, with deep-link rendering. Storage: existing record
  fields on `InsectSpecies` (or a small additive ValueObject
  collection — TBD in the Phase 1 plan).

**Independent of Phase 0.** Different parts of the console;
different api work. Can parallelise; if Phase 0 finds api gaps
that take real work, Phase 1 keeps moving.

**Image upload destination story** lands here too, since EOL's
representative-image use case (and vision-assist's image input)
both want the same plumbing. Reuse the runtime data directory
from [`runtime-data-persistence.md`](runtime-data-persistence.md)
unless the Phase 1 plan finds a reason not to.

---

## Phase 2 — `InsectIdentification` workflow (FU-3)

**Why.** The actual identification module. Per-domain, per the
FU-3 stance.

**Delivers.**

- `InsectIdentification` aggregate — the running session.
  Append-only step list; state machine
  (IN_PROGRESS / CONCLUDED / ABANDONED); promotion to a durable
  observation record on conclusion.
- Couplet / choice / step / scope value objects — naturalist
  conventions throughout (`NamedEntity` / `Entity` /
  `Aggregate` / `ValueObject` from `kernels/framework`;
  cross-domain by `EntityName`; UUIDv7 for surrogate ids).
  **No Spring annotations in api modules** — apis depend only
  on `framework`, `identifiers`, `field-notes`, `taxonomy`.
- JSON-fed local key adapter (`*-repository-test` pattern;
  `JsonFileKeyNavigationAdapter`-equivalent reshaped as a
  `*RepositoryMock` per existing repository conventions, *not*
  as a Spring `@Profile("dev")` adapter). Seed with ~30 couplets
  covering the common North American orders.
- Scope rendering reuses Phase 0's primitive.
- Deep-link surfacing: when the local key returns no further
  couplets, the application service surfaces stored authority
  URLs from Phase 1's link plumbing — the naturalist continues
  outside the app. **No EOL API call in this phase.**
- A new durable observation entity — provisional name
  `InsectObservation`, decided here. Naturalist's own language
  preserved per principle 2.

**Identification scope shape.** Closed value type, two members:
`DomainId` (whole-domain scope) or a typed rank
(`TaxonomicOrder` / `LinnaeanFamilyName` / `LinnaeanGenusName`).
Substrate decision per the `DomainId` section above; concrete
encoding (sealed interface vs. record vs. visitor) is the Phase 2
plan's call.

**Out of scope for this phase.** EOL fetch (Phase 4),
under-identified pending records (Phase 3), vision draft input
(separate plan).

---

## Phase 3 — FU-1 closure — under-identified organisms

**Why.** [FU-1 in the pressure test
followups](../pressure-test/battus-philenor/99-followups.md): a
naturalist with genus-level or family-level confidence has nowhere
to put the observation under the current `InsectSpecies` slug
contract. FU-1 names three resolution paths and explicitly says
"resolving requires understanding the identification workflow."
Phase 2 is that understanding.

**Delivers.** A decision among the three FU-1 paths:

1. Defer entry until species-level — cleanest model, awkward for
   active field workflow.
2. Separate aggregate (`PendingInsectObservation` or similar) at
   family/genus scope, promoted to `InsectSpecies` when
   identification firms.
3. Provisional epithets (`Aristolochia sp.`, `Lepidoptera sp.`)
   with derived slug like `aristolochia-sp` — `InsectSpecies`
   admits genus-level entries.

The decision is informed by what Phase 2 actually built: how
sessions terminate, what the durable observation shape looks like,
how scope-at-family or scope-at-genus renders. **Don't pre-commit
the path** — Phase 2 surfaces the right answer.

**Closes** FU-1 (the family/genus catalog tier work in
[`fu-1-plan.md`](../notes/fu-1-plan.md) closes the *catalog* side;
this phase closes the *identification* side, which FU-1 explicitly
flags as missing).

---

## Phase 4 — EOL trait fetch — closes the ADR-009 loop

**Why.** Structural completion of ADR-009 for the entomology
slice. Without this, identification is an AI-or-author oracle.
With this, every curated claim is traceable.

**Delivers.**

- `kernels/bibliography` — `LiteratureReference` value object
  (DOI, ISBN, URL, author, year, title, journal). Carried as
  `Set<LiteratureReference>` on entities that need citation
  support. **Closes FU-2.**
- An EOL trait API adapter implementing a
  `CharacteristicLookupPort`-equivalent — populates the curated
  knowledge layer (`TaxonCharacteristic`-equivalent — likely an
  evolution of `IdentificationFeatures`) on demand. Each
  `CharacteristicStatement` carries a `LiteratureReference`.
  Per principle 4: catalog grows one node at a time as
  naturalists reach those nodes.
- EOL trait API access registration (account, token) is a
  prerequisite milestone, not work this phase plans.
- Resilience-wrapped per Phase 1's seam.

**Folds in** FU-2 entirely. The standalone FU-2 followup
redirects to "see roadmap Phase 4" once this lands.

---

## Relationship to `vision-assisted-identification.md`

The vision-assist
[sketch](vision-assisted-identification.md) stays a separate
effort, still gated on its own conditions ("≥4 weeks of manual
entry"). But:

- The kernel facade in **Phase 1** of this roadmap *is* the seam
  the vision sketch was going to introduce as `kernels/vision/`.
  The vision sketch becomes a *second adapter* on the same facade,
  not its own kernel. Naming and shape get worked out in the
  Phase 1 plan; the vision sketch updates to reference it.
- ADR-009's "AI explains, references authorise" becomes the
  explicit rule for how vision plugs in: vision produces a
  *draft*, EOL grounds it, naturalist confirms. Vision adapter
  without EOL adapter is the oracle this roadmap rejects.
- Image upload destination (the open question in the vision
  sketch) is solved by Phase 1's image-handling story.

When vision-assist is unfrozen later, it is a smaller plan: tool
definition + draft mapper + console review UI, on top of an
already-shipped facade.

---

## Resolution mapping for the existing followups

| Followup                                    | Status before  | Resolution path                                                                    |
| ------------------------------------------- | -------------- | ---------------------------------------------------------------------------------- |
| FU-1 — under-identified organisms           | OPEN, STRAIN   | Catalog side closes via FU-1 PR-2f / PR-2g; identification side closes in **Phase 3** |
| FU-2 — bibliography / provenance kernel     | OPEN, NOTE     | **Phase 4** lands `kernels/bibliography`                                           |
| FU-3 — identification as first-class        | OPEN, STRAIN   | **Phase 2** ships `InsectIdentification`; kernel extraction stays deferred         |

The followup entries get a "→ rolled into roadmap" pointer once
the roadmap row is live in the work-tracker; they no longer track
separately.

---

## Slot in the work-tracker

One new row in `docs/work-tracker.md`:

> **Identification roadmap** — Plan (sketch) — *active — Phase 0
> next* — [`plans/identification.md`](plans/identification.md) —
> *Multi-week. Per-domain start in `insects`. Phase 0 audits api
> + lands family/genus console views (subsumes FU-1 PR-3); Phase 1
> establishes the external-source kernel seam; Phase 2 ships the
> identification workflow (FU-3); Phase 3 closes FU-1's
> identification-side; Phase 4 lands the EOL adapter and
> `kernels/bibliography` (FU-2), closing ADR-009's authority loop.*

As each phase is promoted to its own implementation plan, that
plan gets its own row; the roadmap row remains as the index. The
FU-1 PR-3 line on the FU-1 row updates to point at Phase 0.

---

## Out of scope (named explicitly)

- BugGuide adapter beyond deep-link surfacing — no public API; the
  deep link is the whole story. Stored as an `AuthorityReference`
  with no machine-readable fetch.
- Borror / Triplehorn & Johnson and other print-only authorities
  as anything more than `LiteratureReference` citations.
- Cross-domain identification ("is this an insect or a spider?")
  before two domains demand it. Per FU-3.
- Web app / mobile capture. Console-only era.
- Bulk EOL trait import for the entire local key. Per principle 4
  — one node at a time.
- Vision-assisted identification itself. Separate plan, separate
  gating; this roadmap establishes the substrate it will plug
  into.
- Promotion of `LinnaeanClass` or `LinnaeanPhylum` to first-class
  aggregates. Future per-domain effort when `vertebrates` /
  `molluscs` / `worms` demands it.
- Reclassification events. FU-4 is CONSIDERED-REJECTED; this
  roadmap does not reopen it.

---

## Open questions (deferred to per-phase plans)

- **Phase 0** — Does the api audit surface gaps that justify their
  own follow-up, or do they all land in this phase? Decided when
  the audit runs.
- **Phase 1** — Naming of the kernel: `external-source`,
  `external-authority`, `domain-source`, something else. Naming
  decision waits for the abstraction to be drafted.
- **Phase 1** — `Set<AuthorityReference>` vs. typed columns
  (`eolPageId`, `bugGuideNodeId`, `inatTaxonId`) on `InsectSpecies`.
  Set is more uniform; typed columns are easier to query. Phase 1
  plan decides.
- **Phase 2** — Concrete encoding of the identification scope
  value object (sealed interface vs. record-with-Optional vs.
  visitor).
- **Phase 2** — Couplet ordering / discrimination strategy: is
  there a single "next couplet" or a partial order with multiple
  candidate discriminators? Real dichotomous keys are often
  partial-order; first-pass simplicity may favour single-next.
  Plan decides.
- **Phase 2** — Naturalist note carry-forward shape: are
  step-level notes denormalised into the durable observation, or
  is the session itself preserved alongside the observation? The
  latter is simpler structurally; the former preserves the
  reasoning chain inline. Plan decides.
- **Phase 3** — Which of the three FU-1 paths. Decided after Phase 2.
- **Phase 4** — Catalog re-assembly trigger when EOL adds a
  characteristic to a previously-unfetched node. Coordinate with
  [`catalog-kernel.md`](catalog-kernel.md)'s pending milestone.
- **Phase 4** — Cost/rate-limit policy for EOL fetches. Per-day
  budget at the kernel facade is cheap insurance.

---

## How this slots into the work order

The current near-term queue (per
[`docs/work-tracker.md`](../work-tracker.md)) is:

> command-framework follow-ups → FU-1 PR-2f / 2g / 3 →
> catalog-kernel M9b–M12

This roadmap sits **after FU-1 PR-2f / PR-2g**, with **Phase 0
absorbing PR-3**. Phase 1 can interleave with catalog-kernel work
since they touch different surfaces. Phase 2 onwards is the
multi-week stretch this roadmap exists to make legible in advance.

If friction appears earlier or differently than expected (Phase 0's
audit reveals deeper api work, Phase 1's GBIF call surfaces a
resilience gap, Phase 2 finds the dichotomous-key abstraction
doesn't fit), this sketch adjusts; the phase structure is the
design space, not a commitment.
