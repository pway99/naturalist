# Pressure Test Charter — *Battus philenor* as Cross-Domain Probe

**Version.** 2 (current). Version 1 is preserved in git history; the reframe
between v1 and v2 is significant enough that v2 supersedes rather than amends.

**Status.** Phase 0 — test rig established. Ready for Phase 1a (DAG orientation).

**Authors.** Pat (naturalist project owner) + Claude (chat collaborator).

**Sibling documents.**

- `01-findings.md` — the running record of the experiment: per-finding strain
  observed, language proposed, structural shape committed, JSON instance
  fragment, closure status.
- `02-stress-tests.md` — how the model, as evolved through Phase 1, receives
  three other organisms (green lacewing, braconid wasp, tomato).
- `99-followups.md` — single rolling registry of deferred items, missing
  vocabulary, and research needs across all axes and all sessions.

---

## 1. Purpose and reframe

The experiment is to determine whether **Domain-Driven Design** principles
plus **directed acyclic graph** discipline — applied across the
`insects-api`, `plants-api`, and `chemistry-api` bounded contexts of the
`naturalist` modular monolith — can faithfully model the trilateral
relationship between insects, plants, and chemistry, using *Battus philenor*
(Pipevine Swallowtail) and its obligate host *Aristolochia californica* as
the worked example.

The deliverable is a **Ubiquitous Language** for the trilateral domain,
captured in this document set as findings, materialized in Java by Pat's
application framework, and validated by the swallowtail bundle JSON
expressing each commitment cleanly.

This is reframed from v1, which described an audit of the existing shape.
v2 acknowledges that the modeling process itself is the object of study —
the swallowtail is the medium, not the test. Success is an ecological object
model that can express *Battus philenor* faithfully today and accommodate
unrelated organisms (green lacewing, braconid wasp, tomato; eventually any
catalogued species) tomorrow without restructuring.

### Why *Battus philenor*

*Battus philenor* is the first **trilateral** story in the catalog —
simultaneously requiring `insects-api`, `plants-api`, and `chemistry-api` to
agree on the same biological fact (aristolochic acid is produced by the
plant, sequestered by the larva, retained through pupation, expressed in the
adult, transferred maternally to eggs). Most existing catalog stories are
bilateral.

The case is also high-leverage on three orthogonal dimensions:

- **Directionality.** It demands cross-domain edges that may not yet exist
  (insects → chemistry, possibly), and stresses edges that do (insects →
  plants by `PlantName`, plants → chemistry by `CompoundName`).
- **Vocabulary.** Three role / category vocabularies overlap on this case
  (`FunctionalRole`, `PhytochemicalRole`, `StageChemistryRole.Role`). The
  bundle data already shows at least one category error
  (`BIOLOGICAL_CATALYST` on aristolochic acid I and II) — symptomatic of a
  vocabulary gap rather than a tagging mistake.
- **Coherence.** Multiple assertions across domains should be mutually
  consistent (e.g., `ChemicalDefense.protectedStages` ↔ per-stage
  `chemistryRole`; `Plant.isKeystoneHost()` ↔ `InsectSpecies.isKeystone()`).
  The swallowtail makes those overlaps visible.

---

## 2. Scope

### In scope

- The cross-domain shape — types, references, sealed hierarchies,
  vocabularies — at the API boundaries of `insects-api`, `plants-api`, and
  `chemistry-api`, as exercised by the *Battus philenor* / *A. californica*
  / aristolochic acid story.
- The bundle file `pipevine-aristolochia-bundle.json` as canonical
  worked-example data and as the validation case for every UBL commitment.
- The four current domain briefings (`framework-briefing.md`,
  `insects-domain.md`, `plants-domain.md`, `chemistry-api.md`) as
  authoritative for current API shape.
- The DDD building blocks (Aggregate, Entity, ValueObject, Identifier) as
  the granularity at which we model. Sealed-hierarchy permits and enum
  values are vocabulary belonging to a parent node, not nodes themselves.

### Out of scope

- Persistence adapters, repository implementations, JSON catalog mechanics
  beyond what the bundle directly exposes.
- The BER chemistry/soil interface (mentioned in plants-domain.md, separate
  story).
- The Nick's Italian Pear heritage tomato lineage (also mentioned, separate
  story).
- The mimicry-complex modeling (typed insect ↔ insect Müllerian / Batesian
  relationships — real but separable; deferred).
- Read-side query ergonomics that aren't directly about cross-domain shape.
- Soil-domain consideration. The swallowtail story does not touch soil.
- Performance, caching, transaction boundaries — none are API-shape
  concerns.

### Resolution rule for disagreements

Where the **bundle JSON** and the **domain briefings** disagree, the
briefings win — they describe the API as it is intended to be; the bundle
is data that may itself be wrong. Such disagreements are recorded as
findings, not silently resolved.

Where a briefing claims a type / method / package exists that cannot be
verified from the briefings themselves, the verification gap is captured as
a followup rather than treated as fact. Pat's running application is the
authoritative source for what currently exists; when in doubt, the model in
the running app overrides any document.

### Generalization horizon

Three additional organisms serve as **stress tests** for the DAG that the
swallowtail produces. They are not modeled in this experiment — they are
used as counterfactual probes against the proposed UBL:

- **`green-lacewing`** — predatory holometabolous insect with no chemical
  defense story. Tests whether the model gracefully expresses "no
  chemistry" without holes.
- **`braconid-wasp`** — parasitoid; larva develops inside another insect.
  Tests insect ↔ insect typed relationships, which the swallowtail does not
  exercise.
- **`tomato`** with `amish-paste` cultivar — a plant whose eventual BER
  modeling needs the chemistry edge for *non-defensive* purposes (calcium
  delivery). Tests whether the chemistry edge generalizes beyond herbivore
  deterrence.

Stress tests live in `02-stress-tests.md` and run once Phase 1b reaches
sufficient stability.

---

## 3. Method

### Three phases

**Phase 1a — DAG orientation.** A single short pass to confirm shared
understanding of the model as it currently stands. No findings logged.
Output is a brief orienting section appended to this charter (or to a small
appendix), naming the candidate nodes the swallowtail story exercises and
flagging any nodes Claude expects to find that aren't yet present in Pat's
running app. Pat's app is the primary DAG visualization tool — this pass
does not produce a redundant document-shaped DAG.

**Phase 1b — Node-by-node evaluation.** The substantive work. One node (or
one tightly-coupled cluster) per session. Each session applies the five
evaluation axes (§4) to the node, logs findings as we go, and — when
confidence is sufficient — drives a real-time UBL change that Pat
materializes in the Java model during the session. Bundle JSON is updated
in lockstep so the swallowtail story continues to express cleanly under the
evolving language.

**Phase 1c — Stress tests.** Once the swallowtail-driven UBL is stable, we
walk the three counterfactual organisms through the model and record where
the proposed shape would receive them gracefully and where it would strain.

### Confidence-gated fixing

Findings drive code change in real time during a session **when confidence
is high**. Otherwise findings stay diagnostic and are revisited later.

| Confidence | Meaning                                                                                                                   | Action                                                                                      |
|------------|---------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| HIGH       | Node shape, vocabulary, and edges are clear from briefings and bundle; swallowtail data fits the proposal without strain. | Pat refactors the Java model in-session. Bundle JSON updated. Finding records what changed. |
| MEDIUM     | Node is partially clear but depends on a parent / child node not yet evaluated, or on missing vocabulary.                 | Diagnose, propose candidate language, mark *contingent*. Revisit when dependency clears.    |
| LOW        | Node is not clear enough to evaluate yet.                                                                                 | Diagnose what we can; defer fixing entirely. Capture as followup.                           |

A finding is **closed** only when (a) language is proposed, (b) the
structural commitment is made in Java by Pat, and (c) the bundle JSON
expresses the new language faithfully. Code change alone is not closure.
Language alone is not closure. The data has to demonstrate the shape works.

---

## 4. Evaluation axes

Each node is evaluated against five axes. Findings are tagged with the
axis (or axes) that surfaced them. The interesting findings are often where
multiple axes converge on the same node — that's a signal the node itself
may be miscut.

### Axis 1 — Identity and slug flow

Can the node's identity be expressed cleanly with the framework's two-branch
discipline (`NamedEntity` slug vs. `Entity` UUIDv7)? Where the node is
relationship-shaped, does the slug compound key fall out naturally *and*
read reasonably as a name?

**The two-branch principle, restated by Pat:**

> `EntityName` is for known facts by a name that is stable. `EntityId` is
> for a relationship to a named entity that is difficult to name; it is
> defined by its relationship more than what it is in isolation.

**The slug-naturalness test.** For any candidate `NamedEntity`, ask:

1. *Does the slug fall out naturally?* Either as an inherent name the world
   already uses (`aristolochic-acid-i`, `pipevine-swallowtail`) or as a
   reasonable deterministic compound key
   (`california-pipevine-aristolochic-acid-i`)?
2. *Does it read reasonably as a name?* Could a human encounter the slug
   in isolation and understand what entity it identifies?

If both yes → `NamedEntity` is correct. If either fails → `Entity`
(UUIDv7) is the honest branch.

Axis 1 also catches referential-integrity strains. The bundle's `_meta`
self-reports a slug mismatch (`aristolochia-californica` on the insect side,
`california-pipevine` on the plant side) — that's an Axis 1 finding ready
to formalize when we touch the involved nodes.

### Axis 2 — Vocabulary alignment

For every role / category / type vocabulary that appears in more than one
domain — or that *should* exist but does not — can the vocabulary be
stated precisely in domain language, and does the swallowtail data fit it
without category errors?

This axis covers existing vocabularies (`FunctionalRole`,
`PhytochemicalRole`, `StageChemistryRole.Role`, `StructuralType`,
`PhytochemicalCategory`, `CompoundCategory`, and any other classifying
sealed family or enum the swallowtail exercises) and missing ones. **A
named-but-unmodeled vocabulary is itself a finding.** When such a gap is
identified, Phase 1b captures at minimum the proposed name; ideally also a
short description and the node it would attach to. Concrete examples
include: an insects-side defensive-mechanism vocabulary that does not yet
exist (sequestration vs. de-novo synthesis vs. symbiont-derived); a possible
`SecondaryMetabolite` permit on chemistry's `FunctionalRole` for compounds
whose entire behavioral story is told plant-side; a possible
`NitrophenanthreneAlkaloid` permit on `StructuralType`.

Missing vocabulary is rendered in documents using chevron tokens
(`«SecondaryMetabolite»`) so they are unambiguously distinguished from
extant types and remain grep-able across documents.

### Axis 3 — Story coherence

Are there assertions in one node — or one domain — that should be
derivable from, or constrained by, assertions in another? Where overlapping
assertions exist, do they actually agree on the swallowtail data? Are they
enforced anywhere?

Examples surfaced by the briefings: `ChemicalDefense.protectedStages` ↔
the set of stages with non-null `chemistryRole`; `Plant.isKeystoneHost()`
↔ `InsectSpecies.isKeystone()` for an obligate-host pair. Findings under
this axis include both the existence of the overlap and its enforcement
status (or absence).

### Axis 4 — Direction of coupling

For every cross-domain edge the swallowtail story uses or *would* need:
does it exist in the codebase's module-level DAG? Should it exist? If not,
what carries the information?

A second concern lives here: **module-DAG and concept-DAG must be
consistent**. The framework briefing describes module-level edges
(`insects-api → plants-api`, etc.). The concept-level edges we sketch are
finer-grained (`PhytochemicalConstituent.compoundName : CompoundName`
crossing `plants-api → chemistry-api`). Concept edges must not violate
module edges; if a concept edge would require a new module edge, that's a
material finding for Phase 1b.

Axis 4 comes after the others because directional choices have to honor
identity (Axis 1), use the right vocabularies (Axis 2), and not duplicate
or contradict existing assertions (Axis 3).

### Axis 5 — Generalization fitness

Does this node express a property *Battus philenor* happens to have, or a
property an *organism in this domain* should be expected to express? Are
the swallowtail-specific details (sequestration, `MATERNAL_TRANSFER`,
indeterminate voltinism, aposematic coloration) the *case* or the *shape*?

If a future organism — green lacewing, braconid wasp, tomato, or any not
yet imagined — would require restructuring this node to attach, the node
is overfit to *Battus philenor*. Axis 5 is the counterfactual probe;
Phase 1c stress-tests the same property against three concrete candidates.

### Sixth axis — provisionally open

If a sixth axis emerges during Phase 1b — temporality, lifecycle, scale,
or something we haven't anticipated — it is added to this charter rather
than absorbed silently into one of the existing five. Adding an axis is a
charter revision, recorded explicitly. The DAG-as-a-whole may yet express
something more elaborate than the sum of its parts; we leave the door open.

---

## 5. Severity and finding template

### Severity tiers

| Tier   | Meaning                                                                                                   |
|--------|-----------------------------------------------------------------------------------------------------------|
| BLOCK  | Story cannot be told correctly with the current shape. Data must be either wrong or shoehorned.           |
| STRAIN | Story can be told, but the shape is fighting it — data lands in the wrong field or duplicates elsewhere.  |
| SMELL  | Looks suspicious; may be fine on closer inspection, may not be.                                           |
| NOTE   | Observation that doesn't affect the swallowtail directly but is worth remembering for cross-cutting work. |

### Finding template

Each finding in `01-findings.md` carries:

1. **ID** — stable, of the form `A{axis}-F{number}`. Axes 1–5 are the
   evaluation axes; axis 0 is reserved for cross-axis or charter-level
   findings. IDs do not get renumbered once assigned.
2. **Severity** — BLOCK / STRAIN / SMELL / NOTE.
3. **Context** — one or two sentences on what node we were evaluating
   and what made the strain visible. This is the cheap-now /
   expensive-later element that protects the finding from losing meaning
   across sessions.
4. **Strain observed** — what was wrong with the language as it stood,
   with concrete evidence from the bundle.
5. **Language proposed** — what we are naming it now and why, in domain
   terms first.
6. **Structural shape** — DDD building block (Aggregate / Entity /
   ValueObject / Identifier), home module, identity branch (slug or
   surrogate), and edges to other nodes. Expressed in DDD vocabulary, not
   Java code.
7. **JSON instance fragment** — the swallowtail data expressed under the
   new language, demonstrating that the bundle continues to tell the
   story.
8. **Closure status** — `OPEN`, `CONTINGENT (depends on …)`, or `CLOSED`
   with a brief note on the resulting code change. A finding is CLOSED
   only when language, structural shape, and bundle JSON all align.

---

## 6. Workflow rhythm

### In-session coevolution

Sessions are real-time loops, not document-first batches. The pattern:

1. We pick a node (or cluster) from Phase 1a's orientation.
2. We discuss in domain language; one or more axes surface findings.
3. When confidence is high, Pat refactors the Java model during the
   session. Pat's framework is built for low-friction change and absorbs
   refactoring without ceremony.
4. The bundle JSON is updated in lockstep to express the new shape.
5. The finding is closed in `01-findings.md`.
6. Lower-confidence findings are logged as OPEN or CONTINGENT and left
   for later.

### End-of-session discipline

Every session ends with a **session summary entry** appended to a session
log section in `01-findings.md` (or to a separate `SESSION-LOG.md` if the
log gets long). Five sentences is usually sufficient:

- What node(s) we worked on.
- What was decided.
- What is in flight (CONTINGENT findings).
- What changed in the Java model and the bundle.
- What the next session should pick up.

The session summary is the connective tissue across chats. Without it,
resuming after a break — especially in a new chat instance — costs more
than it saved.

### When to start a new chat

Claude proactively flags when the current chat's context window is
approaching exhaustion (signs: slower responses, dropped details,
self-repetition). Pat may also call a clean break at any time.
Resumption (§7) is designed so a new chat can pick up without loss.

---

## 7. Resumption — designing for multiple chats

This experiment will not fit in a single chat. The artifacts in this
directory are designed as a **self-contained package** sufficient to resume
the work in a fresh chat instance, with no carry-over of conversational
state.

### Resumption inputs

A new chat begins with:

1. **This charter** (`00-charter.md`) — the orienting document, read first.
2. **The findings document** (`01-findings.md`) — what's been decided,
   what's still in flight, what closed and how.
3. **The followups registry** (`99-followups.md`) — pending items,
   missing vocabulary, research needs.
4. **The stress-tests document** (`02-stress-tests.md`) — once it exists,
   relevant for understanding the generalization commitments made.
5. **Pat's running application and current Java model** — authoritative
   for what currently exists. Documents capture *what was decided and
   why*; the running model captures *what currently is*. When they
   disagree, the running model is the truth and the documents need
   updating.
6. **The session log** — what the previous session ended on and what
   should happen next.

### Resumption discipline

- A new chat reads the charter completely before doing anything else.
- A new chat does not re-derive findings that are already CLOSED. It
  takes them as given.
- A new chat may question OPEN or CONTINGENT findings if doing so adds
  rigor; raising a question is encouraged, silently overriding is not.
- A new chat may notice inconsistencies of style or judgment with prior
  sessions. That is expected. Pat is the through-line that keeps the
  experiment coherent across chats; chat instances are not.

### What the documents cannot replace

Pat's living memory of the modeling experiment — the unwritten reasoning,
the half-formed intuitions, the path not taken — is part of how this work
holds together. Documents are the spine; Pat is the muscle holding the
spine together. Resumption tries to minimize what Pat has to reconstruct,
but it does not pretend Pat is dispensable.

---

## 8. Discipline rules

These commitments keep the experiment honest and its conversation
manageable. Pat is invited to hold Claude to them.

- **Domain language first, structural commitment second, Java never.**
  Findings describe domain language and DDD structural shape. Java code is
  Pat's to write — Pat's framework, Pat's micro-conventions. Claude's
  output stops at the language and the shape.
- **JSON instance is proof.** A finding is not closed until the bundle
  JSON expresses the new language faithfully.
- **One node per session.** If evaluating Node A surfaces something about
  Node B, log it (as a future finding or a followup) and continue with A.
  Cluster nodes only when their coupling makes single-node evaluation
  artificial.
- **"I don't know" beats "probably."** When a question depends on code or
  config not in briefings, bundle, or the running app, the gap is captured
  as a verification followup, not guessed.
- **No new axes mid-evaluation.** If the five axes prove insufficient, that
  itself is a charter-level finding (`A0-F{n}`), and a new axis is added
  by charter revision, not by ad-hoc inclusion in a session.
- **Each session ends explicitly with a summary.** Claude does not start a
  next session without an explicit go-ahead, and does not stop a session
  without writing the summary entry.

---

## 9. Glossary local to this pressure test

- **UBL.** Ubiquitous Language. The shared vocabulary the modeling
  experiment produces, materialized by Pat's framework into Java records.
- **Trilateral story.** A domain story requiring three or more bounded
  contexts to agree. *Battus philenor* is the first one in the catalog.
- **Probe.** A worked example chosen specifically to stress a property of
  the shape under test — not because it's typical, but because it's
  revealing.
- **Node.** A DDD building block (Aggregate, Entity, ValueObject, or
  Identifier) placed in a specific bounded-context module. Sub-types of
  sealed hierarchies and enum values are vocabulary belonging to a parent
  node, not nodes themselves.
- **Edge.** A typed cross-node reference, normally an `EntityName` slug.
  Cross-domain edges are the focus of Axis 4.
- **Strain.** A model strains when it can express the case but only by
  putting data in fields the data wasn't designed for, by duplicating
  assertions, or by relying on conventions outside the type system.
- **Axis (in this document).** One of the five orthogonal lenses we
  evaluate through. Not to be confused with the four classification axes on
  `CompoundInfo` (`ChemicalNature`, `PhysicalForm`, `StructuralType`,
  `FunctionalRole`).
- **DAG.** Directed Acyclic Graph. Used in two senses in this experiment:
  (a) the module-level DAG (`insects-api → plants-api`, etc.), enforced
  by the build; (b) the concept-level DAG of nodes and typed edges, finer
  grained, the object of study. They must be consistent.
- **Missing-vocabulary token.** A name in chevrons (e.g.,
  `«SecondaryMetabolite»`) marking proposed-but-unmodeled vocabulary so it
  is grep-able and unambiguous.

---

## 10. Sign-off

Charter v2 is signed off by Pat in chat on the working session that
produced it. Claude is bound by it from that point.

Phase 1a (DAG orientation) is the next working session. It is a
single-session pass and does not produce findings.
