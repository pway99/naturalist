# Identification — Roadmap (Sketch)

A multi-phase roadmap for building the per-domain identification
workflow, starting in `insects` and folding in the resolution paths
for FU-1 (under-identified organisms), FU-2 (bibliography), and FU-3
(identification as first-class).

This is a **sketch**, not a binding plan. Each phase is promoted to
its own implementation plan when its predecessor lands.

**Rebalanced 2026-05-10** per Pat's reframing — pressure tests guide
development; they are not deadlines. The original PR-2f mass migration
would have deleted 10 of 16 `InsectSpecies` records and 5 of 22 `Plant`
records to satisfy a clean invariant. Instead, this roadmap is now the
primary trajectory: Phase 0 reorganizes under-identified records to
their actual rank, Phase 2 lands `kernels/bibliography` alongside the
identification workflow (ADR-009 requires citations from day one),
Phase 4 wires the real EOL adapter and populates citations as the
naturalist reaches each node. A1-F1 closes as a side-effect of
*Battus philenor* completing that workflow.

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

| Phase | Name                                                          | Status   | Gates on                                                              |
| ----- | ------------------------------------------------------------- | -------- | --------------------------------------------------------------------- |
| 0     | Taxonomic reorganization + navigation console                 | **next** | Nothing — fully unblocked                                             |
| 1     | External-source seam (mock first)                             | sketched | Independent of Phase 0; can parallelize                               |
| 2     | `InsectIdentification` workflow (FU-3) + `kernels/bibliography` (FU-2) | sketched | Phases 0 + 1 (mock seam)                                              |
| 3     | FU-1 path-1 confirmation                                      | sketched | Phase 0's reorganization surfaces the actual shape                    |
| 4     | Real EOL REST adapter + citation population                   | sketched | Phase 2 (kernel + workflow); EOL API key obtained                     |

---

## Phase 0 — Taxonomic reorganization + navigation console

**Why first.** Two halves. **Data side** — 10 of 16 records in
`insect-species.json` and 5 of 22 records in `plants.json` carry
partial taxonomy (genus-only, family-only, or fully unresolved) yet
sit in the species-rank file. They are catalogued at a rank above
their file's rank. Before the console renders the family/genus
catalog and before Phase 2 walks identification through them, those
records need to move to their actual rank — `insect-genera.json` /
`insect-families.json` for the under-identified insects, and
`plant-genera.json` / `plant-families.json` for the under-identified
plants. Life-stage observations re-anchor to the new home record.

**UI side** — the taxonomy kernel has grown to a thorough lineal
model (Order through Subspecies). `InsectFamily`, `InsectGenus`,
`InsectSpecies` aggregates exist; `InsectQuery` already namespaces
`families()` / `genera()` / `species()`. The console lags: `/insects`
lists species directly; there are no family or genus pages. This is
also the existing FU-1 PR-3 scope ("Console — family + genus
list/detail views").

The identification module needs to *render* every taxonomic level
to display couplet results, scope, and pending-organism records
("at family level, no further yet"). Without this, Phase 2 invents
its own family/genus presentation under time pressure.

**Delivers — data side.**

- **Reorganization migration.** For each under-identified insect record
  in `insect-species.json` (`green-lacewing`, `tachinid-fly`,
  `braconid-wasp`, `hoverfly`, `ground-beetle`, `crane-fly`,
  `skipper-butterfly`, `native-sweat-bee`, `grey-mining-bee`,
  `potato-leafhopper`): create or update the corresponding entry in
  `insect-genera.json` (when genus is known) or `insect-families.json`
  (when only family is known); migrate the species record's
  description, common names, and any other relevant fields up; remove
  the species-rank record; re-anchor its life-stage observations to
  the new home.
- Same for the five under-identified plants in `plants.json`
  (`creeping-thyme`, `ornamental-passiflora`, `dianthus`, `sage`,
  `citrus`) — promote into `plant-genera.json` or `plant-families.json`.
- The migration is **review-as-you-go**, not algorithmic — each move
  is a small naturalist judgment call about where the existing
  description and observations rightfully live.

**Delivers — UI side.**

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

**Subsumes** FU-1 PR-3 and absorbs the data-migration piece that the
original PR-2f attempted to do (correctly: at the *correct rank*, not
by deletion).

**Touches plants lightly.** The audit looks at `PlantQuery` for
symmetry only; actual plant family/genus console views land when
plant identification (a future, post-roadmap effort) demands them.
The plant data reorganization still happens in this phase because
the under-identified plant records are the same kind of catalog
shape problem the insect records are.

---

## Phase 1 — External-source seam (mock first)

**Why.** Stand up the kernel facade so Phase 2's workflow can
exercise the seam without an API key. The real EOL REST adapter
wires in Phase 4 once Phase 2 has proven the integration shape.

**Delivers.**

- `kernels/external-source` (or similar; naming is a Phase 1
  decision) — a vendor-neutral facade. `Resilience`-wrapped per
  ADR-026; bulkheaded, timed-out, retried per the kernel's
  facade. Mirrors the shape `kernels/vision/` was sketched as in
  [`vision-assisted-identification.md`](vision-assisted-identification.md).
- **Mock adapter** as the default in this phase — returns fixture
  data scoped to a single demo species (likely *Battus philenor*
  so Phase 2 can walk through the swallowtail). The mock is a
  real `*Mock` per repository conventions, not a Spring
  `@Profile("dev")` adapter.
- API-key management *skeleton* — env-var read at adapter
  construction; documented but not enforced at this stage (no
  real adapter requires a key yet).
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

**GBIF as first real adapter — deferred to Phase 4 prerequisite.**
The original sketch placed a GBIF name-validation adapter here as
"easy unauthenticated outbound." Moved to Phase 4 prerequisite work
so Phase 1 stays mock-only and the real-call shape is proven
end-to-end against EOL itself, not a sidecar.

---

## Phase 2 — `InsectIdentification` workflow (FU-3) + `kernels/bibliography` (FU-2)

**Why.** The actual identification module. Per-domain, per the
FU-3 stance. **And** the bibliography kernel — ADR-009 requires
every curated `TaxonCharacteristic` to carry a `LiteratureReference`,
and Phase 2 is where curated characteristics start being written.
The kernel lands at the start of the workflow that produces those
statements, not at the end of the roadmap.

**Delivers — workflow side.**

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
  outside the app. **No real EOL API call in this phase** (mock
  adapter from Phase 1 stands in).
- A new durable observation entity — provisional name
  `InsectObservation`, decided here. Naturalist's own language
  preserved per principle 2.

**Delivers — bibliography kernel.**

- `kernels/bibliography` — a new shared kernel.
- `LiteratureReference` value object carrying citation metadata
  (DOI, ISBN, URL, author, year, title, journal). VO-form
  initially; promote to `NamedEntity` only if duplication becomes
  a real maintenance burden.
- Carried as `Set<LiteratureReference>` on entities that need
  citation support — initially `TaxonCharacteristic` (or whatever
  the curated-statement shape is called), `InsectIdentification`
  step entries, and any other Phase 2 surface that makes
  scientific claims.
- Closes **FU-2** ([`99-followups.md`](../pressure-test/battus-philenor/99-followups.md)).

**Identification scope shape.** Closed value type, two members:
`DomainId` (whole-domain scope) or a typed rank
(`TaxonomicOrder` / `LinnaeanFamilyName` / `LinnaeanGenusName`).
Substrate decision per the `DomainId` section above; concrete
encoding (sealed interface vs. record vs. visitor) is the Phase 2
plan's call.

**Promotion mechanism — preview.** When a naturalist's session
narrows an organism from genus-rank to species-rank (e.g.,
`Chrysoperla` → `Chrysoperla rufilabris`), Phase 2's application
service creates the species record, attaches the citation, and
re-anchors observations from the genus record as appropriate.
The genus record stays — other naturalist may have organisms at
that rank. The formal design of this promotion ceremony is
Phase 3's job; Phase 2 builds the supporting machinery.

**Out of scope for this phase.** Real EOL REST call (Phase 4),
formal FU-1 path-1 documentation (Phase 3), vision draft input
(separate plan).

---

## Phase 3 — FU-1 path-1 confirmation

**Why.** Phase 0's data reorganization is the operational expression
of FU-1 **path 1** — organisms live at their actual identification
rank in the catalog (genus or family), with a species record created
only when species-level identification is firm. Phase 3 makes that
choice explicit in the design record and formalises the promotion
ceremony that Phase 2 began to build.

**Delivers.**

- Formal confirmation of FU-1 path 1 in
  [`99-followups.md`](../pressure-test/battus-philenor/99-followups.md)
  and [`structural-commitments.md`](../pressure-test/battus-philenor/structural-commitments.md)
  §5. Paths 2 (separate `UnidentifiedSpecimen` aggregate) and 3
  (provisional epithets like `aristolochia-sp`) are explicitly
  considered and rejected here, with reasoning preserved.
- Design documentation for the **promotion ceremony**: when a
  naturalist's identification narrows from genus to species, the
  species record is created with `genusName` pointing at the
  existing genus record; the genus record stays (other naturalists
  may still observe at that rank); observations re-anchor as
  appropriate. The mechanics ship in Phase 2; Phase 3 documents
  the contract.
- Tightening review of the non-null `genusName` invariant on
  `InsectSpecies` / `Plant` — invariant activates only after every
  species-rank record satisfies it. Phase 0 removed records that
  couldn't satisfy it (they moved to genus/family). Phase 2's
  workflow only ever creates species records that do. So by
  Phase 3 the invariant is safe to enable.

**Why not earlier in the roadmap.** Phase 0's reorganization is
informed by what's in the data, not by an upfront design choice.
Phase 2's workflow surfaces what the promotion experience actually
looks like. By Phase 3, both the data and the workflow exist to
inform the formal documentation.

**Why not later.** The non-null invariant tightening — small but
load-bearing — should land before Phase 4 wires the real EOL
adapter, so citation-attaching code can assume the invariant
holds.

**Closes** FU-1 fully — both the catalog-side (kernel work shipped
in PR-1 / PR-2a–e, data reorganization shipped in Phase 0) and the
workflow-side (Phase 2's promotion mechanism + Phase 3's formal
confirmation).

---

## Phase 4 — Real EOL REST adapter + citation population

**Why.** Structural completion of ADR-009 for the entomology
slice. Without this, identification is an AI-or-author oracle.
With this, every curated claim is traceable. `kernels/bibliography`
already exists from Phase 2; Phase 4 wires the data source.

**Prerequisite — not work this phase plans.**

- EOL trait API access registration (account, token).
- API key obtained and stored per the env-var convention from
  Phase 1's seam.

**Delivers.**

- **Real EOL REST adapter** implementing the
  `CharacteristicLookupPort`-equivalent from Phase 2 — replaces
  the mock from Phase 1 for production use. The mock stays
  available for tests and offline development.
- **Citation population walk** — per organism, as the naturalist
  reaches a node in the identification workflow, the EOL adapter
  fetches curated trait data and populates `TaxonCharacteristic`
  statements with `LiteratureReference` citations. Per principle 4:
  catalog grows one node at a time, not as a bulk import.
- **A1-F1 closure** happens here. When *Battus philenor*'s walk
  through the identification workflow produces citations from
  EOL, the swallowtail bundle is fully binomial, fully cited, and
  A1-F1 moves CONTINGENT → CLOSED in
  [`01-findings.md`](../pressure-test/battus-philenor/01-findings.md).
- Resilience-wrapped per Phase 1's seam.

**Closes the ADR-009 loop** for the entomology slice — Claude
explains, EOL authorises, the naturalist evaluates the synthesis
against the cited material.

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

| Followup                                    | Status before  | Resolution path                                                                                                       |
| ------------------------------------------- | -------------- | --------------------------------------------------------------------------------------------------------------------- |
| FU-1 — under-identified organisms           | OPEN, STRAIN   | Catalog data side closes in **Phase 0** (reorganization); promotion mechanism ships in **Phase 2**; formal path-1 confirmation in **Phase 3** |
| FU-2 — bibliography / provenance kernel     | OPEN, NOTE     | **Phase 2** lands `kernels/bibliography` (moved earlier than the original sketch; ADR-009 needs citations from day one) |
| FU-3 — identification as first-class        | OPEN, STRAIN   | **Phase 2** ships `InsectIdentification`; kernel extraction stays deferred                                            |

The followup entries get a "→ rolled into roadmap" pointer once
the roadmap row is live in the work-tracker; they no longer track
separately.

---

## Slot in the work-tracker

Row #10 in [`docs/work-tracker.md`](../work-tracker.md):

> **Identification roadmap** — Plan (sketch) — *active — Phase 0
> next* — [`plans/identification.md`](plans/identification.md) —
> *Multi-week. Per-domain start in `insects`. Phase 0 reorganizes
> under-identified records to their actual rank AND ships family/genus
> console views; Phase 1 establishes the external-source kernel seam
> as a mock; Phase 2 ships the identification workflow (FU-3) and
> `kernels/bibliography` (FU-2); Phase 3 confirms FU-1 path-1; Phase 4
> wires the real EOL REST adapter and populates citations, closing
> ADR-009's authority loop.*

As each phase is promoted to its own implementation plan, that
plan gets its own row; the roadmap row remains as the index.

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

**Reframed 2026-05-10.** This roadmap is now the primary near-term
trajectory; PR-2f / PR-2g / PR-3 fold into it:

- **Phase 0** absorbs PR-3 (console views) *and* the data
  reorganization the original PR-2f tried to do via deletion.
- **Phase 2** carries the per-organism species narrowing that
  the original PR-2f tried to do via mass migration. As each
  identification firms, a species record is created (or the
  existing one gains `genusName`).
- **Phase 4** carries the A1-F1 closure the original PR-2g
  tried to do via bundle re-emit. *Battus philenor*'s walk
  through the workflow with EOL citation is what closes it.

Phase 1 can interleave with catalog-kernel work since they touch
different surfaces. Phase 2 onwards is the multi-week stretch
this roadmap exists to make legible in advance.

If friction appears earlier or differently than expected (Phase 0's
audit reveals deeper api work, Phase 1's mock seam surfaces a
resilience gap, Phase 2 finds the dichotomous-key abstraction
doesn't fit), this sketch adjusts; the phase structure is the
design space, not a commitment.
