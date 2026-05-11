# Work Tracker

Single index of every work effort currently in flight or recently completed.
This file does not own scope — every row links to its source-of-truth doc.
Update a row when its status changes; promote completed rows to the
"recently completed" section so the active table stays focused.

Last updated: 2026-05-10. Ordering reframed by Pat on 2026-05-10: **pressure-test findings guide development; they are not deadlines.** A1-F1 closes as a side-effect of building the identification capability the finding exposed, not by deleting under-identified domain data to satisfy a clean invariant. PR-2f's original "remove 15 pending records" disposition is rejected — 10 of 16 `InsectSpecies` records and 5 of 22 `Plant` records are under-identified, and they are the use case the application exists to support, not waste to be cleared. **The new sequence:** (1) re-classify under-identified records to the rank at which they can confidently be identified (Phase 0 data side); (2) console renders the reorganized data with family/genus navigation (Phase 0 UI side); (3) mock the external-source adapter (Phase 1 with no-op/mock); (4) build the identification aggregates and model (Phase 2 + `kernels/bibliography`); (5) wire the real EOL REST adapter (Phase 1 completion); (6) step through the binomial identification process organism-by-organism, narrowing under-identified records to species as confidence firms and attaching citations as the workflow produces them (Phase 2 execution); (7) resume the *Battus philenor* pressure test. PR-2f's species-narrowing kernel change happens **opportunistically per organism** through step 6, not as a mass migration. A1-F1 closes when *Battus philenor* completes that workflow with an EOL-grounded citation.

---

## At a glance

| #   | Effort                                           | Type          | Status                       | Source                                                                                | Notes                                                                                              |
|-----|--------------------------------------------------|---------------|------------------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1   | Runtime data persistence                         | Plan          | **landed** 2026-05-09        | [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md)              | Origin-tracked write-back on `TestEntitySource`: console edits flush to canonical source-tree JSON via `target/classes` → `src/main/resources` heuristic; `static final` flag keeps tests no-op. Two latent bugs fixed when first write route exercised it (commit `b6c4957`): `@JsonValue` on `EntityId`/`EntityName.value()` and `defaultInsertFile` set in `loadFile` singular. Defers RDBMS. |
| 2   | Command framework — follow-ups                   | Plan          | active — 1 of 3 FUs landed   | [`plans/command-framework.md`](plans/command-framework.md)                            | Pilot shipped 2026-05-09 (commit `482b48d`); first console write route landed 2026-05-09 (commit `b6c4957` — `POST /insects/{name}/images`). Remaining: second-domain rollout (chemistry), aggregate-level commands (when forced by a use case). |
| 3   | FU-1 — Family/Genus catalog tiers                | Plan (notes)  | active — PR-2f/PR-2g reshaped | [`notes/fu-1-plan.md`](notes/fu-1-plan.md)                                            | PR-1 / PR-2a–e ✅. **PR-2f is no longer a mass migration** — species narrowing happens opportunistically per organism through identification roadmap Phase 2's workflow. **PR-2g** (A1-F1 closure) similarly: the bundle re-emits once *Battus philenor* completes the workflow with citation. **PR-3 is subsumed by identification roadmap Phase 0** (#10). |
| 4   | Catalog kernel — finish line                     | Plan          | active — M9b/M10/M11/M12     | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                                  | Typed observation types + coverage assertion + ArchUnit guard + ADR. Independent of FU-1.          |
| 5   | Admin console                                    | Plan          | active — view 4 (future)     | [`plans/admin-console.md`](plans/admin-console.md)                                    | Views 1–3 shipped. View 4 (`/admin/schedules`) is gated on the future scheduled-task runner.       |
| 6   | Pressure test — *Battus philenor*                | Pressure test | Phase 1b active              | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) | Node-by-node evaluation continues. A1-F1 CONTINGENT on FU-1. Next node: `Compound` or `PhytochemicalConstituent`. |
| 7   | Backlog — Soil/Sensor services                   | Notes         | unprioritized backlog        | [`notes/pending-implementation.md`](notes/pending-implementation.md)                  | Stale in spots (BehavioralCollection now exists). Triage pass needed before picking from this list. |
| 8   | Open design questions                            | Notes         | Q0–Q7 in flight              | [`notes/open-questions.md`](notes/open-questions.md)                                  | Several deferred-but-not-resolved. Q0 (Aggregate × Entity ADR) is the only one with structural impact. |
| 9   | Vision-assisted identification                   | Plan (sketch) | **deferred** — long horizon  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md)  | Claude Vision–driven console identification + draft-review-confirm workflow. **Kernel facade subsumed by identification roadmap Phase 1** (#10) — vision becomes a second adapter on the shared seam, not its own kernel. Revisit when ≥4 weeks of manual entry surface real friction. |
| 10  | **Identification roadmap**                       | Plan (sketch) | **active — Phase 0 next**    | [`plans/identification.md`](plans/identification.md)                              | Multi-week per-domain identification workflow, starting in `insects`. Five phases, now the primary trajectory. Phase 0 reorganizes under-identified records to their actual rank AND ships family/genus console views. Phase 1 lands the external-source seam as a mock. Phase 2 ships `InsectIdentification` + `kernels/bibliography` (FU-2 closes here, not Phase 4). Phase 3 confirms FU-1's path-1 (organisms live at their actual rank; promote on identification). Phase 4 wires the real EOL REST adapter and populates citations. ADR-009's authority loop closes for entomology in Phase 4. |

---

## Active efforts — what's left

### 1. Runtime data persistence — **landed 2026-05-09**

**Source:** [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md). Implemented same day as command-framework pilot.

Origin-tracked write-back on `TestEntitySource`. Console edits flush to canonical source-tree JSON; the classpath heuristic (`target/classes` → `src/main/resources`) locates the write target with zero per-source configuration. A `static final boolean PERSISTENCE_ENABLED = Boolean.getBoolean("naturalist.persistence.enabled")` is captured at class load — tests can never enable it (the only setters run before `SpringApplication.run`). Verification is manual via the console; no unit tests for kernel test infrastructure.

- `TestEntitySource` gains `originFile` map (per-entity source file) and `defaultInsertFile` (first file passed to `loadFile` or `loadFiles`); `insert`/`update` flush; flush groups by origin, sorts by `name().toString()` for stable diffs, writes per-file atomically.
- `ConsoleApplication.main` sets `naturalist.persistence.enabled=true` before `SpringApplication.run`.
- **Two follow-up fixes** landed alongside the first console write route (commit `b6c4957`) when manual QA exercised the flush end-to-end:
    - `@JsonValue` on `EntityId.value()` and `EntityName.value()` — without it, Jackson serialised the abstract wrappers via JavaBean discovery and emitted `{"valid":..,"notValid":..}` envelopes instead of the wrapped UUID/slug. Reads worked via `@JsonCreator`; only writes were broken.
    - `loadFile` (singular) now sets `defaultInsertFile`. Previously only `loadFiles` (plural) did, so every existing source — all use the singular form — left the default null, new inserts got null origin, and the flush filter dropped them silently.
- **Out of scope:** RDBMS adapter, multi-process coordination, catalog index re-assembly trigger (own follow-up under `catalog-kernel.md`).

### 2. Command framework — follow-ups

**Source:** [`plans/command-framework.md`](plans/command-framework.md). Pilot shipped 2026-05-09 (commit `482b48d`):
`EntityCommand` + `AbstractEntityCommand` in the kernel, `EntityCommandContractTest` in framework-test, public `InsectCommand` namespace with entity-level `SpeciesCommand` / `ImageCommand`, `*CommandImpl` adapters in insects-core, contract tests, and `insectCommand()` accessor on `InsectsTestContext`.

- ✅ **First console write route** — landed 2026-05-09 (commit `b6c4957`). `POST /insects/{name}/images` in `insects-console` calling `insectCommand.images().insert(...)`. CSRF protection via Spring Security default; form on the species detail page; web-mvc tests cover authenticated POST, anonymous POST, and missing-CSRF rejection. Manual QA verified pipevine-swallow-tail entries flush to source-tree JSON. Surfaced and resolved the two persistence bugs noted under #1.
- **Second-domain rollout.** Replicate the namespace + adapters to chemistry (N=3 — Compound, Element, Product). Proves the abstraction at higher cardinality.
- **Aggregate-level commands.** Defer until a controller route genuinely needs to coordinate multi-entity writes; a real shape will be obvious then.

### 3. FU-1 — Family/Genus catalog tiers

**Source:** [`notes/fu-1-plan.md`](notes/fu-1-plan.md). **Reshaped 2026-05-10** — PR-2f / PR-2g / PR-3 no longer ship as discrete editorial slices. The kernel/aggregate work (PR-1 / PR-2a–e) is complete and remains the foundation. The remaining work folds into the identification roadmap (#10).

| Slice  | Status     | Description                                                                                                                                                                                                                               |
|--------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PR-1   | ✅ landed   | Kernel `LinnaeanFamily` / `LinnaeanGenus` / `LinnaeanSpecies` interfaces.                                                                                                                                                                  |
| PR-2a  | ✅ landed   | `InsectFamily` aggregate.                                                                                                                                                                                                                  |
| PR-2b  | ✅ landed   | `PlantFamily` aggregate.                                                                                                                                                                                                                   |
| PR-2c  | ✅ landed   | `InsectGenus` aggregate.                                                                                                                                                                                                                   |
| PR-2d  | ✅ landed   | `PlantGenus` aggregate.                                                                                                                                                                                                                    |
| PR-2e  | ✅ landed   | Catalog wiring + queries for the four new aggregates.                                                                                                                                                                                      |
| PR-2f  | ↪ reshaped | **Species narrowing is no longer a mass migration.** `Plant` and `InsectSpecies` gain typed `genusName` opportunistically as each organism's identification firms through roadmap Phase 2's workflow. The non-null `genusName` invariant tightens only after every record satisfies it. Under-identified records move *out* of species JSON to their actual rank (genus or family) as part of roadmap Phase 0's data reorganization. Editorial backfill of family/genus descriptions happens per organism in Phase 2, not as an up-front batch. |
| PR-2g  | ↪ reshaped | A1-F1 closure is no longer a single bundle re-emit. It happens when *Battus philenor* completes the identification workflow (Phase 2) with an EOL-grounded citation (Phase 4). At that point the swallowtail's records are fully binomial, fully cited, and the finding moves CONTINGENT → CLOSED. |
| PR-3   | ↪ rolled   | **Subsumed by identification roadmap Phase 0** (#10). Console family + genus views render the *reorganized* data (Phase 0's data-side) and introduce the reusable taxonomic-scope rendering primitive that Phase 2 reuses for session scope and pending-organism display. |

### 4. Catalog kernel — M9b / M10 / M11 / M12

**Source:** [`plans/catalog-kernel.md`](plans/catalog-kernel.md). M0–M9a all shipped. Independent of #1–#3; can interleave.

| Milestone | Status    | Description                                                                                                                                                            |
|-----------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| M9b       | ⏳ pending | Typed observation types (`UnresolvedReferenceObservation` not yet shipped) + four console observers (Micrometer + logging × search-miss + unresolved-reference).         |
| M10       | ⏳ pending | `CatalogCoverageValidator` — startup assertion that every contributed entity is reachable by its slug. Misses fire `IncompleteContributionObservation` (WARN), no boot fail. |
| M11       | ⏳ pending | ArchUnit guard — every concrete `EntityName` subclass must have at least one `CatalogContribution` or `EntityReferences<T>` somewhere; build error if not.            |
| M12       | ⏳ pending | ADR + `kernels/CLAUDE.md` reference for the catalog kernel (3 SPIs, search-not-routing, soft-validation, per-app composition).                                          |

Loose dependency: M10 reads cleanly after M9b's observation pipeline lands. M11 and M12 are independent. A new milestone for **catalog re-assembly trigger** is anticipated as the natural follow-up to #2 (runtime data persistence) — to be scoped in that plan when it's promoted from sketch.

### 5. Admin console — view 4 (deferred)

**Source:** [`plans/admin-console.md`](plans/admin-console.md). Views 1–3 (`/admin/resilience`, `/admin/domain-services`, `/admin/catalog`) shipped. View 4 (`/admin/schedules`) is explicitly gated on the future scheduled-task runner; not actionable until that lands.

### 6. Pressure test — *Battus philenor* Phase 1b

**Source:** [`pressure-test/battus-philenor/`](pressure-test/battus-philenor/). [Charter](pressure-test/battus-philenor/00-pressure-test-charter.md), [findings](pressure-test/battus-philenor/01-findings.md), [follow-ups](pressure-test/battus-philenor/99-followups.md).

| Finding | Severity | Status                                | Notes                                                                                                              |
|---------|----------|---------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| A1-F1   | STRAIN   | CONTINGENT on FU-1                    | Vernacular ↔ binomial slug mismatch. Closes when FU-1 PR-2g lands and the bundle re-emits under binomial slugs.    |

Phase 1b is **paused** while the identification roadmap (#10) builds the capability A1-F1 surfaced. Node-by-node evaluation resumes after Phase 2 has narrowed *Battus philenor* through the identification workflow and Phase 4 has wired EOL citation. Pat's reframing on 2026-05-10: pressure tests guide development; the pressure test resumes when the structural work has caught up.

Related deferred items (in [`99-followups.md`](pressure-test/battus-philenor/99-followups.md)):
- **FU-1** — under-identified organisms. **Path 1 (organisms live at their actual rank; promote on identification)** is favored. Data reorganization happens in identification roadmap Phase 0; the promotion mechanism is part of Phase 2's workflow; Phase 3 confirms the path formally.
- **FU-2** — bibliography/provenance kernel. **`kernels/bibliography` lands at the start of identification roadmap Phase 2** (#10) — the dichotomous-key data carries `LiteratureReference` citations from day one per ADR-009. The EOL adapter in Phase 4 *populates* references; the kernel itself is older.
- **FU-3** — identification-as-first-class. **Resolution path is the identification roadmap** (#10); `InsectIdentification` ships in Phase 2.
- **FU-4** — reclassification events. CONSIDERED-REJECTED. Reopens only with evidence of frequent reclassification.

### 7. Pending-implementation backlog (triage needed)

**Source:** [`notes/pending-implementation.md`](notes/pending-implementation.md).

The list is partly stale: item 1 (`BehavioralCollection`) is shipped (ADR-011 + several concrete subclasses). Items 2–8 cover Soil/Sensor/Event services that haven't been started. **Recommend a triage pass** before this list drives priorities.

### 8. Open design questions

**Source:** [`notes/open-questions.md`](notes/open-questions.md).

Of the seven, only **Q0** (Aggregate × CatalogEntity × Entity composition ADR) has potential structural impact and may want resolving before more aggregate-shaped work lands. Q1–Q7 are infrastructure concerns deferred behind the in-memory adapter.

### 9. Vision-assisted identification (deferred)

**Source:** [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md). Sketch only.

Console- and web-driven workflow that posts an image to Claude Vision, returns a typed `IdentificationDraft<T>` per domain, lets Pat review and edit, and confirms via `EntityCommand.insert`. Architectural shape sketched (kernel facade `kernels/vision/` + `adapters/anthropic-vision/` + per-domain `<domain>-vision/` + console routes); not promoted to numbered milestones.

**Why deferred:**
- Pressure-test framing doesn't justify it (the test stresses cross-domain shape, not data volume).
- Manual console entry through #1–#3 hasn't been tried; vision UX should be designed against the friction manual entry surfaces, not speculatively.
- Prompt + tool-schema design wants iteration; locking now risks the wrong shape.
- Multi-PR architectural shell is non-trivial and competes with five queued near-term efforts.

**Revisit when:** Console-driven manual entry has been Pat's daily workflow for ≥4 weeks AND he can name the specific bottleneck.

**Subordinate to identification roadmap (#10).** ADR-009 frames AI-only identification as the *oracle* shape it forbids; vision-assist without a curated authority backing is exactly that. The identification roadmap establishes the substrate (Phase 1 — `external-source` kernel facade) that vision-assist plugs into when unfrozen, and the curated knowledge layer (Phase 4 — EOL adapter) that grounds vision drafts. When this comes back online it is a smaller plan: tool definition + draft mapper + console review UI on top of an already-shipped facade.

### 10. Identification roadmap

**Source:** [`plans/identification.md`](plans/identification.md). Sketch, 2026-05-10. Rebalanced 2026-05-10 per Pat's reframing — the roadmap is now the primary trajectory; PR-2f / PR-2g / PR-3 fold into it.

Multi-week, multi-phase roadmap for the per-domain identification workflow, starting in `insects`. Naturalist conventions throughout (`NamedEntity` / `Entity` / `Aggregate` / `ValueObject`; cross-domain by `EntityName`; UUIDv7; no Spring in api modules). Per the FU-3 stance, kernel extraction (`kernels/identification`) is deferred until a second domain proves the same shape.

| Phase | Name                                                          | Status   | Gates on                                                              |
|-------|---------------------------------------------------------------|----------|-----------------------------------------------------------------------|
| 0     | Taxonomic reorganization + navigation console                 | **next** | Nothing — fully unblocked                                             |
| 1     | External-source seam (mock first)                             | sketched | Independent of Phase 0; can parallelise                               |
| 2     | `InsectIdentification` workflow (FU-3) + `kernels/bibliography` (FU-2) | sketched | Phases 0 + 1 (mock seam)                                              |
| 3     | FU-1 path-1 confirmation                                      | sketched | Phase 0's reorganization surfaces the actual shape                    |
| 4     | Real EOL REST adapter + citation population                   | sketched | Phase 2 (kernel + workflow); EOL API key obtained                     |

**Phase 0** now opens with **data reorganization** — under-identified records in `insect-species.json` (10 of 16) move to their actual rank in `insect-genera.json` or `insect-families.json`, with life-stage observations re-anchored. Under-identified plant records (5 of 22) move analogously. Then the console family + genus list/detail views render the reorganized data, with the reusable taxonomic-scope rendering primitive Phase 2 reuses.

**Phase 1** stands up the external-source kernel facade with a mock/no-op adapter so Phase 2's identification workflow can exercise the seam without an API key. The real EOL REST adapter lands in **Phase 4** once the workflow has proven the integration points.

**Phase 2** ships `InsectIdentification` aggregates AND `kernels/bibliography` — ADR-009 requires every curated `TaxonCharacteristic` to carry a `LiteratureReference`, so the kernel lands at the start of the workflow that produces those statements, not at the end of the roadmap. The dichotomous-key data is born with citations.

**Phase 3** formally confirms FU-1's **path 1** (organisms live at their actual rank; promote on identification). Phase 0's reorganization is the operational expression of path 1; Phase 3 makes the choice explicit in `99-followups.md` and structural-commitments and designs the promotion ceremony (when an organism narrows from genus to species, the genus record stays, a new species record is created, observations attach as appropriate).

**Phase 4** wires the real EOL REST adapter, obtains the API key, and populates `LiteratureReference` citations on `TaxonCharacteristic` statements per organism as the naturalist reaches them. *Battus philenor*'s walk-through the identification workflow with an EOL-grounded citation is what closes A1-F1.

**Resolution mapping:** FU-1 → Phase 0 (data) + Phase 2 (promotion mechanism) + Phase 3 (formal path choice); FU-2 → Phase 2 (`kernels/bibliography` lands here); FU-3 → Phase 2 (`InsectIdentification`). Their followup entries in [`99-followups.md`](pressure-test/battus-philenor/99-followups.md) carry "rolled into roadmap" pointers.

**Phase 0 plan is not yet promoted.** Promote next — Phase 0 is fully unblocked.

---

## Recently completed

| Effort                            | Completed  | Source                                                                       | Final commit                                                     |
|-----------------------------------|------------|------------------------------------------------------------------------------|------------------------------------------------------------------|
| First console write route         | 2026-05-09 | [`plans/command-framework.md`](plans/command-framework.md)                   | `b6c4957` (POST /insects/{name}/images + 2 persistence bug fixes)|
| Runtime data persistence          | 2026-05-09 | [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md)     | `2a4dea4` (initial); `b6c4957` (`@JsonValue` + `loadFile` default)|
| Command framework pilot           | 2026-05-09 | [`plans/command-framework.md`](plans/command-framework.md)                   | `482b48d` (kernel + insects pilot; follow-ups deferred)          |
| Paged queries (steps 1–7)         | 2026-05-09 | [`plans/paged-queries-plan.md`](plans/paged-queries-plan.md)                 | `3789319` (delete unbounded) → `3a5ea41` (mark plan implemented) |

---

## Dependency graph

```
paged-queries            ✅ done
command-framework pilot  ✅ done
runtime-data-persistence ✅ done
first console write route ✅ done (POST /insects/{name}/images)
      │
      ▼
┌────────────────────────────────────────────────────────────────────┐
│  identification roadmap  (plans/identification.md)                  │
│                                                                     │
│  Phase 0 — taxonomic reorganization + navigation console  ← NEXT    │
│      (data side: move 10 under-identified insects + 5 plants to    │
│       their actual rank, re-anchor life stages; UI side: family +  │
│       genus list/detail views; scope-rendering primitive)          │
│                                                                     │
│  Phase 1 — external-source seam (mock first)  ← parallelisable      │
│      (no API key required; mock/no-op adapter for Phase 2 to use)  │
│      │                                                              │
│      ▼                                                              │
│  Phase 2 — InsectIdentification workflow (FU-3)                    │
│           + kernels/bibliography (FU-2)                             │
│      (dichotomous-key aggregates; citations on TaxonCharacteristic │
│       from day one per ADR-009)                                    │
│      │                                                              │
│      ├─► Phase 3 — FU-1 path-1 confirmation                         │
│      │       (formalize: organisms live at their actual rank;      │
│      │        promote on identification)                           │
│      │                                                              │
│      └─► Phase 4 — Real EOL REST adapter + citation population     │
│              (API key obtained; per-organism citation walk;        │
│               Battus philenor walks through here → A1-F1 CLOSES)   │
└────────────────────────────────────────────────────────────────────┘
      │
      ▼
pressure-test Phase 1b resumes (Compound or PhytochemicalConstituent)

(parallel — interleavable)

command-framework second-domain rollout (chemistry) — independent
command-framework aggregate commands     — deferred until forced by a use case

catalog-kernel M9b ─► M10
                  │
                  └─► M11, M12 (independent)

admin-console view 4              (gated on scheduled-task runner; not actionable yet)

vision-assisted-identification          (deferred — long horizon; plugs into roadmap Phase 1's facade when unfrozen)
```

---

## Decided ordering (reframed 2026-05-10)

**Rationale.** Pat's reframing on 2026-05-10: pressure tests guide development; they are not deadlines. The original PR-2f plan would have deleted 10 of 16 `InsectSpecies` records and 5 of 22 `Plant` records to satisfy a clean non-null `genusName` invariant — destroying domain content the app exists to support. The new sequence builds the identification capability A1-F1 exposed, and lets A1-F1 close as a side-effect of that capability working.

0. ✅ **Command framework pilot** — landed `482b48d`. ([source](plans/command-framework.md))
1. ✅ **Runtime data persistence** — origin-tracked source-tree write-back on `TestEntitySource`. Initial commit `2a4dea4`; `@JsonValue` + `defaultInsertFile` follow-ups in `b6c4957`. ([source](plans/runtime-data-persistence.md))
2. ✅ **First console write route** — landed `b6c4957`. `POST /insects/{name}/images`. CSRF / form rendering / web-mvc tests covered.
3. **Identification roadmap Phase 0** ← **next.** Two halves: (a) **data reorganization** — move the 10 under-identified `InsectSpecies` records (`green-lacewing`, `tachinid-fly`, `braconid-wasp`, `hoverfly`, `ground-beetle`, `crane-fly`, `skipper-butterfly`, `native-sweat-bee`, `grey-mining-bee`, `potato-leafhopper`) to their actual rank in `insect-genera.json` / `insect-families.json`, re-anchor life-stage observations; same for the 5 under-identified plants (`creeping-thyme`, `ornamental-passiflora`, `dianthus`, `sage`, `citrus`); (b) **console family + genus list/detail views** rendering the reorganized data, plus the reusable taxonomic-scope rendering primitive Phase 2 reuses. Promote [`plans/identification.md`](plans/identification.md) Phase 0 to its own implementation plan when starting.
4. **Identification roadmap Phase 1** — external-source kernel seam, mock-only adapter. Independent of Phase 0; can parallelise. No API key required at this stage.
5. **Identification roadmap Phase 2** — `InsectIdentification` workflow (FU-3) + `kernels/bibliography` (FU-2). Dichotomous-key aggregates; `LiteratureReference` value object on `TaxonCharacteristic` from day one per ADR-009.
6. **Identification roadmap Phase 3** — FU-1 path-1 confirmation. Formalize that organisms live at their actual rank and promote on identification. The Phase 0 reorganization is the operational expression of this; Phase 3 makes the choice explicit and designs the promotion ceremony.
7. **Identification roadmap Phase 4** — Real EOL REST adapter wired; API key obtained; citation population walks per organism as the naturalist reaches each node. *Battus philenor*'s walk through this is what closes A1-F1.
8. **Pressure-test Phase 1b resumes** — next node (`Compound` or `PhytochemicalConstituent`). The pressure test runs again once the structural development it surfaced has caught up.
9. **Catalog kernel M9b → M10 → M11 → M12** — finish-line work; small per-milestone. Can interleave at any point.

### Notes on the ordering

- **A1-F1 stays CONTINGENT through Phases 0–3.** It closes in Phase 4 when *Battus philenor* completes the identification workflow with EOL citation. Not a target the schedule bends toward — a side-effect of the capability landing.
- **PR-2f and PR-2g are no longer discrete slices.** Species narrowing (`genusName` non-null) happens opportunistically per organism through Phase 2's workflow; PR-2f's editorial backfill (~29 descriptions × 4 Durrell levels) dissolves into per-organism work in Phase 2.
- **Phase 0's data reorganization is the most concrete change in #3.** It physically moves catalog data. Touches `insect-species.json`, `insect-genera.json`, `insect-families.json`, `life-stages.json` (re-anchor), `plants.json`, `plant-genera.json`, plant cross-references. Algorithmic per record; review-as-you-go.
- **Phases 0 and 1 can parallelise.** Different surfaces. Phase 1's mock seam is independent of Phase 0's data and console work.
- **`kernels/bibliography` lands earlier than the previous plan said** — Phase 2 instead of Phase 4. Reason: ADR-009 requires every curated `TaxonCharacteristic` to carry a `LiteratureReference`, and Phase 2 is where curated characteristics start being written.
- **Catalog kernel M9b–M12 is interleavable.** Pick up between bigger efforts.
- **Backlog triage** ([`notes/pending-implementation.md`](notes/pending-implementation.md)) and **Q0 ADR** ([`notes/open-questions.md`](notes/open-questions.md)) are off the path. Worth a 30-minute independent pass.
- **Vision-assisted identification** ([`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md)) stays deferred. Plugs into Phase 1's seam when unfrozen.

---

## Conventions

- Every row points to its source-of-truth doc; this file is an index, not a duplicate.
- One row per effort. Sub-milestones live in the source doc, not here.
- Update a row's **Status** when it changes. Move completed rows to **Recently completed** with a final commit reference.
- New effort? Add a row at the bottom of **At a glance**, write a brief "what's left" entry in the section below, and link the source doc.
