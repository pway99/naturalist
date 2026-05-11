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
**Status.** OPEN — resolution path is **path 1** (organisms live at their
actual rank; promote on identification). Formal confirmation pending in
identification roadmap Phase 3.
**Resolution path.** Three steps:
1. **Catalog data side** — identification roadmap **Phase 0** moves the
   10 under-identified `InsectSpecies` records and 5 under-identified
   `Plant` records to their actual rank in the existing genus/family
   JSONs and re-anchors life-stage observations
   ([`docs/plans/identification.md`](../../plans/identification.md)
   Phase 0 data side).
2. **Promotion mechanism** — identification roadmap **Phase 2** builds
   the application service that, on session conclusion, creates a
   species record from a parent genus-rank entry and re-anchors
   observations.
3. **Formal confirmation** — identification roadmap **Phase 3**
   documents the path-1 choice in this file and in
   [`structural-commitments.md`](structural-commitments.md) §5;
   tightens the non-null `genusName` invariant on
   `InsectSpecies` / `Plant`.

**Why path 1** (reframed 2026-05-10). Pat's principle: the application
exists to support a naturalist's journey of discovery. An organism
identified to genus level is *the use case*, not waste to be cleared.
The catalog already has genus and family aggregates (PR-2a–e) that can
host these records; the data simply needs to live at the rank it actually
represents. Paths 2 and 3 are rejected:

- Path 2 (separate `UnidentifiedSpecimen` aggregate) duplicates surface
  that `InsectGenus` and `InsectFamily` already provide. Promotion would
  require schema migration rather than a new record at the next rank.
- Path 3 (provisional epithets like `aristolochia-sp`) puts non-Linnaean
  slugs in `LinnaeanSpecies`, violating Commitment 4 (slug derived from
  taxonomic components). The slug-derivation rule is load-bearing for
  cross-domain reference resolution.

**Severity if surfaced as finding.** STRAIN — workflow concern, not a
swallowtail-blocking issue.

**Summary.** The slug-derivation rule (Commitment 4) requires non-null
genus and species on any entity implementing `LinnaeanSpecies`. A
naturalist engaged in field identification commonly has organisms with
genus-level or family-level confidence but no species identification yet.
Such organisms now live as `InsectGenus` / `InsectFamily` / `PlantGenus` /
`PlantFamily` records, not as `LinnaeanSpecies` records. When a
naturalist's identification firms to species, a new `LinnaeanSpecies`
record is created with the typed `genusName` reference pointing at the
existing genus record.

**Why not resolved sooner.** The pressure test surfaced this in Phase 1a
review and the original FU-1 plan deferred resolution to "after the
identification workflow exists." That was the right shape; the
identification roadmap (2026-05-10) is that workflow.

---

## FU-2 — Bibliography / provenance kernel

**Source.** Identity discussion during Phase 1a review; Pat raised the
need for naturalist citations of published authority.
**Resolution path.** `kernels/bibliography` (`LiteratureReference` value
object) lands as part of **identification roadmap Phase 2**
([`docs/plans/identification.md`](../../plans/identification.md))
— moved earlier than the original Phase 4 placement on 2026-05-10. ADR-009
requires every curated `TaxonCharacteristic` to carry a
`LiteratureReference`, and Phase 2 is where curated characteristics start
being written. The EOL adapter in Phase 4 populates these citations from
a real source; the kernel itself is older. Concrete shape (DOI / ISBN /
URL / author / year / title / journal) is decided in the Phase 2 plan.
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
**Status.** OPEN — resolution path settled.
**Resolution path.** Per-domain start in `insects` ships in
**identification roadmap Phase 2**
([`docs/plans/identification.md`](../../plans/identification.md)) as the
`InsectIdentification` aggregate — couplet/choice/step records, JSON-fed
local key adapter, deep-link surfacing when local key exhausts. Kernel
extraction (`kernels/identification`) stays deferred until a second
domain (likely `plants`) demands the same shape, per the original
followup's `@Incubating` discipline.
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
