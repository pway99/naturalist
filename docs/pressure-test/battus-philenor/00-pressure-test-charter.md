# Pressure Test Charter — *Battus philenor* as Cross-Domain Probe

**Status.** Draft, Phase 0.
**Authors.** Pat (naturalist project owner) + Claude (chat collaborator).
**Phase.** 0 — establish test rig. No findings recorded yet.
**Sibling documents.** `axes/01-…` through `axes/04-…` (audit findings, one per axis); `99-followups.md` (rolling
registry of deferred items).

---

## 1. Purpose

This is a **pressure test** of the cross-domain shape currently exposed by the
`naturalist` modular monolith — specifically the surface where `insects-api`,
`plants-api`, and `chemistry-api` meet. The test uses a single biological
worked example, *Battus philenor* (Pipevine Swallowtail) and its obligate host
*Aristolochia californica* (California Pipevine), as the probe.

**The goal is diagnostic, not corrective.** Phase 1 produces a clear-eyed map of
where the current shape holds and where it strains under a real trilateral
domain story. Whether to act on the findings is a separate Phase 2 decision and
is explicitly out of scope here.

---

## 2. Why *Battus philenor*

Most cross-domain stories in the codebase today are bilateral: insect ↔ plant
(host plants, nectar sources), plant ↔ chemistry (phytochemical constituents),
plant ↔ soil (calcium availability for BER). *Battus philenor* is the first
**trilateral** story that requires all three of `insects-api`, `plants-api`,
and `chemistry-api` to agree:

- **Chemistry asserts** — aristolochic acid I and II exist as catalogued
  compounds, with structural and behavioral profiles.
- **Plants asserts** — California Pipevine produces those compounds, in named
  tissues, constitutively, playing named ecological roles.
- **Insects asserts** — *Battus philenor* sequesters them from that host,
  retains them through metamorphosis, expresses them in adults, and transfers
  them maternally to eggs.

A second-order property of the case: the same compound (`aristolochic-acid-i`)
must carry **different stories in three places** without those stories
contradicting, duplicating in load-bearing ways, or smearing across the wrong
boundaries. That second-order property is what makes the swallowtail a probe
rather than just a record.

Three additional traits make *Battus philenor* a high-leverage test case:

1. **Directionality probe.** The case demands edges that don't yet exist
   (insects → chemistry, possibly), and stresses edges that do (insects →
   plants by `PlantName`; plants → chemistry by `CompoundName`).
2. **Vocabulary probe.** Three role / category vocabularies overlap on this
   case (`FunctionalRole`, `PhytochemicalRole`, `StageChemistryRole.Role`).
   The bundle data already shows at least one category error
   (`BIOLOGICAL_CATALYST` on AA-I/II) — symptomatic of a vocabulary gap rather
   than a tagging mistake.
3. **Coherence probe.** Multiple assertions across domains should be mutually
   consistent (e.g., `ChemicalDefense.protectedStages` ↔ per-stage
   `chemistryRole`; `Plant.isKeystoneHost()` ↔ `InsectSpecies.isKeystone()`).
   The swallowtail makes those overlaps visible.

---

## 3. Scope

### In scope

- The cross-domain *shape* — types, references, sealed hierarchies, vocabularies
  — at the api boundaries of `insects-api`, `plants-api`, and `chemistry-api`,
  as those boundaries are exercised by the *Battus philenor* / *A. californica*
  / aristolochic acid story.
- The bundle file `pipevine-aristolochia-bundle.json` as canonical worked-example
  data.
- The three current domain briefings (`framework-briefing.md`,
  `insects-domain.md`, `plants-domain.md`, `chemistry-api.md`) as authoritative
  for current api shape.

### Out of scope

- Persistence adapters, repository implementations, JSON catalog mechanics
  beyond what the bundle directly exposes.
- The BER chemistry/soil interface (mentioned in plants-domain.md but a separate
  story).
- The Nick's Italian Pear heritage tomato lineage (also mentioned but a separate
  story).
- The mimicry-complex modeling (typed insect ↔ insect relationships for
  Müllerian / Batesian mimicry — real but separable; deferred).
- Read-side query ergonomics that aren't directly about cross-domain shape.
- Any soil-domain consideration. The swallowtail story does not touch soil.
- Aggregate factory placement, except where it directly affects how a consumer
  must traverse the cross-domain graph.
- Performance, caching, transaction boundaries — none of these are api-shape
  concerns.

### Resolution rule for disagreements

Where the **bundle JSON** and the **domain briefings** disagree, the briefings
win — they describe the api as it is intended to be; the bundle is data that
may itself be wrong. Such disagreements are recorded as findings, not silently
resolved.

Where a briefing claims a type / method / package exists that I cannot verify
from the briefings themselves, the verification gap is captured as a followup
rather than treated as fact.

---

## 4. Method — the four audit axes

Phase 1 walks four axes, one per session, in order. Each axis takes the
previous as given:

### Axis 1 — Identity and slug flow

Can the swallowtail story be told end-to-end with `EntityName` slugs as the
only cross-domain identity? Where does a slug get translated, and is the
translation honest? This axis surfaces referential-integrity issues before
anything else, because a broken slug invalidates downstream analysis on the
same edge.

**Known surface-level finding to formalize:** the bundle's `_meta` flags a slug
mismatch — `aristolochia-californica` on the insect side vs `california-pipevine`
on the plant side. This is acknowledged data, not a discovery; Axis 1 gives it
a stable ID and a documented home.

### Axis 2 — Vocabulary alignment

For every role / category / type vocabulary that appears in more than one
domain, can we state precisely what each one is *for*, and demonstrate that
*Battus philenor* data fits each one without category errors? This axis looks
at `FunctionalRole`, `PhytochemicalRole`, `StageChemistryRole.Role`,
`StructuralType`, `PhytochemicalCategory`, `CompoundCategory`, and any other
classifying enum or sealed family that the swallowtail data exercises.

### Axis 3 — Story coherence

Are there assertions in one domain that should be derivable from — or
constrained by — assertions in another? Where overlapping assertions exist
(e.g., the `protectedStages` ↔ per-stage `chemistryRole` pair, or
`Plant.isKeystoneHost()` ↔ `InsectSpecies.isKeystone()`), do they actually
agree on the swallowtail data? Are they enforced anywhere?

### Axis 4 — Direction of coupling

Walk every cross-domain edge the swallowtail story actually uses or *would*
need. For each edge: does it exist? Should it exist? If not, what carries the
information? Output is a small DAG diagram of the cross-domain dependencies the
worked example demands, comparable against the codebase's current DAG.

This axis comes last because it depends on the previous three: directional
choices have to honor identity rules (Axis 1), use the right vocabularies
(Axis 2), and not duplicate or contradict existing assertions (Axis 3).

---

## 5. Pass / fail criteria

A finding records where the current shape **strains, blocks, or smells** when
asked to express *Battus philenor* faithfully. We use four severity tiers:

| Tier   | Meaning                                                                                                   | Phase 2 implication                                 |
|--------|-----------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| BLOCK  | Story cannot be told correctly with the current shape. Data must be either wrong or shoehorned.           | Demands a redesign answer.                          |
| STRAIN | Story can be told, but the shape is fighting it — data lands in the wrong field or duplicates elsewhere.  | Strong candidate for redesign.                      |
| SMELL  | Looks suspicious; may be fine on closer inspection, may not be.                                           | Investigate before deciding whether to address.     |
| NOTE   | Observation that doesn't affect the swallowtail directly but is worth remembering for cross-cutting work. | May aggregate into a higher-severity finding later. |

A finding does **not** get a fix proposal inline. Where a fix is obvious, the
fix idea goes into `99-followups.md` tagged as a redesign candidate. The audit
documents are diagnosis-only.

The pressure test as a whole **passes** if Phase 1 produces zero `BLOCK`
findings and the `STRAIN` findings have honest workarounds. It **fails** if any
`BLOCK` exists, in which case Phase 2 (redesign) is the natural next step. A
test result of "passes with strain" is a real and acceptable outcome: the model
holds, with documented friction, and the friction is logged for future work.

---

## 6. Finding ID convention

Each finding gets a stable ID of the form **`A{axis}-F{number}`**, e.g.,
`A1-F1`, `A2-F3`. IDs are stable across revisions of the audit documents — once
assigned, they don't get renumbered, even if findings are merged or closed.
Closed findings remain in their document with their resolution, not deleted.

Followups in `99-followups.md` use the form **`FU-{number}`** with a cross-link
to the source axis finding(s) that raised them.

Phase 2 redesign proposals (when and if they happen) will use **`R{number}`**
and reference the finding IDs they address.

---

## 7. Discipline rules (Claude-side)

These are commitments made to keep the audit honest and the conversation
manageable. Pat is invited to hold Claude to them.

- **Diagnosis only in Phase 1.** No fix proposals inline. Obvious fixes get
  parked in `99-followups.md`.
- **One axis per session.** If an audit on Axis N surfaces something that
  belongs to Axis N+M, it gets logged in followups and addressed when its axis
  comes around.
- **"I don't know" beats "probably."** When a question depends on code or
  config not in the briefings or bundle, the gap is captured as a verification
  followup, not guessed.
- **No new axes mid-audit.** If the four axes prove insufficient, that itself
  is a finding (probably a `NOTE` on the charter), and a new axis is added by
  charter revision, not on the fly.
- **Each session ends explicitly.** Claude does not start Axis N+1 without an
  explicit go-ahead.

---

## 8. Working agreement summary

| Item                | Decision                                                                                          |
|---------------------|---------------------------------------------------------------------------------------------------|
| Output format       | Markdown plan documents, one per axis + charter + followups registry.                             |
| Document location   | `pressure-tests/battus-philenor/` under the project's `docs/` directory.                          |
| Authoritative input | The four briefings (framework, insects, plants, chemistry) + `pipevine-aristolochia-bundle.json`. |
| Disagreement rule   | Briefings win over bundle; verification gaps captured as followups.                               |
| Pacing              | One axis per session. Pat confirms before Claude proceeds.                                        |
| Findings            | Stable IDs (`A{n}-F{n}`), severity tiers (BLOCK / STRAIN / SMELL / NOTE), diagnosis-only.         |
| Followups           | Single rolling registry (`99-followups.md`), tagged by source axis and theme.                     |
| Phase 2             | Out of scope of this charter. Decided after Phase 1 completes, not before.                        |

---

## 9. Open questions for charter sign-off

These are questions about *the test rig itself*, not about the model under
test. They are recorded here so that if any of them is answered "no" or
"unclear," the charter is revised before Phase 1 begins.

| #  | Question                                                                                                                                                                                                     | Resolution |
|----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| Q1 | Is "diagnosis without inline fixes" the right Phase 1 discipline, or does Pat want fix proposals interleaved?                                                                                                | Pending.   |
| Q2 | Are the four axes the right cut, or is there a fifth axis (e.g., temporality / lifecycle) that the swallowtail makes visible and that this charter is missing?                                               | Pending.   |
| Q3 | Should Axis 2 (vocabulary alignment) include vocabularies that *don't* appear in multiple domains but *should* (e.g., a hypothetical insect-side defense-mechanism enum that doesn't exist today)?           | Pending.   |
| Q4 | Is the bundle the *only* canonical evidence, or should Claude also inspect the briefings' worked examples (e.g., `LarvaStage.hostPlants` examples in insects-domain.md) when they reference the swallowtail? | Pending.   |

These need not all be answered before Phase 1 begins, but answering them
sharpens the audit. Q1 in particular is load-bearing — its answer determines
the shape of every axis document.

---

## 10. Glossary local to this pressure test

- **Trilateral story.** A domain story that requires three or more bounded
  contexts to agree. *Battus philenor* is trilateral; tomato BER prevention
  will be trilateral once modeled (plants + chemistry + soil); most existing
  stories are bilateral.
- **Probe.** A worked example chosen specifically to stress a property of the
  shape under test — not because it's typical, but because it's revealing.
- **Strain (verb).** A model strains when it can express the case but only by
  putting data in fields the data wasn't designed for, or by duplicating
  assertions, or by relying on conventions outside the type system.
- **Axis (in this document).** One of the four orthogonal lenses we audit
  through. Not to be confused with the four classification axes on
  `CompoundInfo` (`ChemicalNature`, `PhysicalForm`, `StructuralType`,
  `FunctionalRole`).
