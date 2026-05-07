# Followups — Deferred Items

**Status.** Active rolling registry.

**Purpose.** Single home for items surfaced during the audit but not
resolved within it. Each entry is a self-contained record of what was
raised, when, why it was deferred, and what would resolve it.

**Sibling documents.**

- `00-charter.md` — governance, evaluation axes, severity tiers.
- `phase-1a-orientation.md` — Phase 1a output (shared map).
- `structural-commitments.md` — pre-Phase-1b architectural commitments.
- `01-findings.md` — Phase 1b findings (not yet started).

**Entry conventions.**

- Every entry has a stable ID `FU-{number}`, assigned in order. IDs are
  never reused or renumbered.
- Each entry names its **source** (the conversation or finding that
  raised it) and its **status** (`OPEN`, `CONSIDERED-DEFERRED`,
  `CONSIDERED-REJECTED`, `RESOLVED`).
- Resolution, when it happens, links to the finding ID or document
  section that closes the item. Closed entries remain in this file for
  traceability.
- New entries are appended at the bottom; this file grows over time.

---

## FU-1 — Under-identified organisms in `LinnaeanSpecies` contract

**Source.** Structural commitment §5; Phase 1a review of identity model.
**Status.** OPEN.
**Severity if surfaced as finding.** Likely STRAIN — workflow concern,
not a swallowtail-blocking issue.

**Summary.** The slug-derivation rule (Commitment 4) requires non-null
genus and species on any entity implementing `LinnaeanSpecies`. A
naturalist engaged in field identification commonly has organisms with
genus-level or family-level confidence but no species identification yet.
Such organisms cannot enter the `Plant` or `InsectSpecies` catalog under
the current commitment.

**Three resolution paths considered:**

1. **Defer entry until species-level identification is achieved.**
   Cleanest model; awkward for active field workflow.
2. **Separate aggregate for unidentified specimens** (`UnidentifiedSpecimen`
   or similar), promoted to `Plant` / `InsectSpecies` when identification
   firms.
3. **Accommodate provisional epithets** (`Aristolochia sp.`,
   `Lepidoptera sp.`) in `LinnaeanSpecies`, with derived slug like
   `aristolochia-sp` or `aristolochia-unknown-1`.

**Why deferred.** The swallowtail story does not exercise this — *Battus
philenor* identification is firm, and every catalogued organism in the
current JSON files has species-level taxonomy. Resolving this question
properly requires understanding how the application's identification
workflow is intended to work, which is broader than the audit's scope.

**What would resolve it.** A future modeling session focused on the
field-identification workflow. Possibly tied to `FU-3` (identification
as a first-class concern) — they share design surface.

---

## FU-2 — Bibliography / provenance kernel

**Source.** Identity discussion during Phase 1a review; Pat raised the
need for naturalist citations of published authority.
**Status.** OPEN, structural commitment named but not resolved.
**Severity if surfaced as finding.** NOTE — cross-cutting feature, not a
swallowtail-blocking concern.

**Summary.** Naturalist claims (species identification, ecological
relationship, chemical classification, conservation status) often need to
cite published authority. The catalog currently has no typed mechanism
for this; references appear in narrative description fields when they
appear at all.

**Proposed shape (provisional):**

- **`kernels/bibliography`** as a new shared kernel.
- **`LiteratureReference`** as a `ValueObject` carrying citation
  metadata (DOI, ISBN, URL, author, year, title, journal).
- Carried as `Set<LiteratureReference>` on entities that need citation
  support — possibly `Plant`, `InsectSpecies`, `Compound`,
  `PhytochemicalConstituent`, others as patterns emerge.
- **Value-object form initially**, accepting duplication if the same
  paper is cited from multiple entities. Promote to `NamedEntity` only
  if duplication becomes a real maintenance burden.

**The cross-cutting tension Pat named.** A single literature reference
may apply to claims in multiple domains. The reference itself is
domain-agnostic (the citation metadata is universal); what it references
is per-domain. This matches the precedent of `Description` and
`TaxonomicClassification` — kernel-shape, per-domain content.

**Why deferred.** The swallowtail story does not require bibliography to
tell itself. Adding it now would help (the swallowtail's mimicry
references and AA-I/II's IARC carcinogen classification both have
real published authorities), but it's additive cross-cutting feature
work, not part of resolving cross-domain shape questions.

**What would resolve it.** A scoped kernel-design session. Estimated
size: small kernel addition plus VO field on a handful of aggregates.

---

## FU-3 — Identification as a per-domain first-class concern

**Source.** Identity discussion during Phase 1a review.
**Status.** OPEN, structural commitment named but not resolved.
**Severity if surfaced as finding.** STRAIN — workflow capability that
is currently under-modeled.

**Summary.** `TaxonomicClassification` answers "what is this?"
declaratively. **Identification** answers "is this what I think it is?"
or "what is this thing I'm looking at?" — the inverse query. Different
machinery: dichotomous keys, identification features, comparison against
published references, observational confidence, alternate hypotheses.

`InsectSpecies` carries `IdentificationFeatures` as a seed of this. No
plant-side equivalent exists yet. There is no shared abstraction across
living domains for identification workflow.

**Proposed shape (provisional):**

- Per-domain identification surface — `InsectIdentification`,
  `PlantIdentification` — independently designed initially.
- Each living domain exposes a service (or query) that supports the
  workflow: "given these observed features, candidate matches in
  declining likelihood."
- Kernel extraction (`kernels/identification`) deferred until two domains
  prove they need the same shape — `@Incubating` discipline applied to
  a kernel candidate.

**Why deferred.** The swallowtail story does not exercise identification
workflow — the swallowtail is identified, with description, features,
and taxonomy already populated. Modeling identification properly is its
own substantial design effort.

**What would resolve it.** A focused identification-workflow modeling
session, likely after at least one living domain has accumulated
identification surface in practice.

**Relationship to FU-1.** Identification workflow and under-identified
organisms are related concerns — the workflow produces the data that
under-identified organisms need to firm up. Resolving them together
likely makes sense.

---

## FU-4 — Reclassification events (Path B)

**Source.** Identity discussion during Phase 1a review.
**Status.** CONSIDERED-REJECTED.
**Severity if surfaced as finding.** N/A — explicitly considered and
rejected in favor of Path A.

**Summary.** Originally proposed as a way to handle scientific
reclassification of catalogued species — promoting `Plant` and
`InsectSpecies` from `NamedEntity` to `Entity<XId>` (UUIDv7), with
reclassification events fired for downstream domains to react to.

**Why rejected.** Three reasons, weighed during the identity discussion:

1. **Path B's architectural cost is high.** Surrogate ids never cross
   domain boundaries by value (per the framework's identity discipline).
   Cross-domain references like `LarvaStage.hostPlants : List<PlantName>`
   would need rebuilding via service interfaces or different containment.
2. **Reclassification frequency is low in practice.** Pat estimated the
   problem is rare enough at current scope to handle as occasional manual
   data migration when it arises.
3. **Path A resolves the slug-mismatch concern through convention** (the
   binomial slug standard) without requiring identity-shape changes.

**Why recorded despite rejection.** So future sessions do not silently
reopen the question. If reclassification frequency turns out higher than
estimated, this entry is the place to revisit Path B with concrete
evidence.

**What would reopen it.** Accumulated evidence that reclassification is
common enough in practice (e.g., five or more reclassifications in a
single year of catalog operation) to justify the architectural cost.

---

## Future entries

Entries `FU-5` and beyond will be added as Phase 1b surfaces them.
Expected sources: Axis 2 vocabulary gaps that exceed the swallowtail's
scope, Axis 3 coherence rules whose enforcement is cross-cutting, Axis 4
direction-of-coupling questions that touch the catalog kernel.
