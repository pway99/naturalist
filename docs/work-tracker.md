# Work Tracker

Single index of every work effort currently in flight or recently completed.
This file does not own scope — every row links to its source-of-truth doc.
Update a row when its status changes; promote completed rows to the
"recently completed" section so the active table stays focused.

Last updated: 2026-05-10. Ordering: **Option B** chosen — open the write surface (command framework) before the heavy editorial slice (FU-1 PR-2f). Command framework pilot landed 2026-05-09 (commit `482b48d`); runtime data persistence implemented same day; first console write route — `POST /insects/{name}/images` — landed 2026-05-09 (commit `b6c4957`), surfacing and fixing two latent persistence bugs (`@JsonValue` missing on `EntityId`/`EntityName`; `defaultInsertFile` only set by `loadFiles` plural). Manual QA verified end-to-end. **The identification roadmap** ([`plans/identification.md`](plans/identification.md), 2026-05-10) sets the multi-week trajectory: PR-2f → PR-2g → roadmap Phase 0 (taxonomic navigation, subsumes PR-3) → Phase 1 (external-source seam) → Phase 2 (`InsectIdentification`, FU-3) → Phase 3 (FU-1 closure) → Phase 4 (EOL adapter + `kernels/bibliography`, closes ADR-009 loop and FU-2). **FU-1 PR-2f is the immediate next step**; it is also the first prereq of roadmap Phase 0.

---

## At a glance

| #   | Effort                                           | Type          | Status                       | Source                                                                                | Notes                                                                                              |
|-----|--------------------------------------------------|---------------|------------------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1   | Runtime data persistence                         | Plan          | **landed** 2026-05-09        | [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md)              | Origin-tracked write-back on `TestEntitySource`: console edits flush to canonical source-tree JSON via `target/classes` → `src/main/resources` heuristic; `static final` flag keeps tests no-op. Two latent bugs fixed when first write route exercised it (commit `b6c4957`): `@JsonValue` on `EntityId`/`EntityName.value()` and `defaultInsertFile` set in `loadFile` singular. Defers RDBMS. |
| 2   | Command framework — follow-ups                   | Plan          | active — 1 of 3 FUs landed   | [`plans/command-framework.md`](plans/command-framework.md)                            | Pilot shipped 2026-05-09 (commit `482b48d`); first console write route landed 2026-05-09 (commit `b6c4957` — `POST /insects/{name}/images`). Remaining: second-domain rollout (chemistry), aggregate-level commands (when forced by a use case). |
| 3   | FU-1 — Family/Genus catalog tiers                | Plan (notes)  | **active — PR-2f next**      | [`notes/fu-1-plan.md`](notes/fu-1-plan.md)                                            | Unblocked 2026-05-09 by paged-queries; first console write route landed, persistence verified. PR-2f, PR-2g remain; **PR-3 is now subsumed by identification roadmap Phase 0** (#10). Closes pressure-test A1-F1; FU-1 closes structurally via roadmap Phase 3. |
| 4   | Catalog kernel — finish line                     | Plan          | active — M9b/M10/M11/M12     | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                                  | Typed observation types + coverage assertion + ArchUnit guard + ADR. Independent of FU-1.          |
| 5   | Admin console                                    | Plan          | active — view 4 (future)     | [`plans/admin-console.md`](plans/admin-console.md)                                    | Views 1–3 shipped. View 4 (`/admin/schedules`) is gated on the future scheduled-task runner.       |
| 6   | Pressure test — *Battus philenor*                | Pressure test | Phase 1b active              | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) | Node-by-node evaluation continues. A1-F1 CONTINGENT on FU-1. Next node: `Compound` or `PhytochemicalConstituent`. |
| 7   | Backlog — Soil/Sensor services                   | Notes         | unprioritized backlog        | [`notes/pending-implementation.md`](notes/pending-implementation.md)                  | Stale in spots (BehavioralCollection now exists). Triage pass needed before picking from this list. |
| 8   | Open design questions                            | Notes         | Q0–Q7 in flight              | [`notes/open-questions.md`](notes/open-questions.md)                                  | Several deferred-but-not-resolved. Q0 (Aggregate × Entity ADR) is the only one with structural impact. |
| 9   | Vision-assisted identification                   | Plan (sketch) | **deferred** — long horizon  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md)  | Claude Vision–driven console identification + draft-review-confirm workflow. **Kernel facade subsumed by identification roadmap Phase 1** (#10) — vision becomes a second adapter on the shared seam, not its own kernel. Revisit when ≥4 weeks of manual entry surface real friction. |
| 10  | **Identification roadmap**                       | Plan (sketch) | **active — Phase 0 after FU-1 PR-2g** | [`plans/identification.md`](plans/identification.md)                              | Multi-week per-domain identification workflow, starting in `insects`. Five phases. Resolves FU-1 (identification side), FU-2 (`kernels/bibliography`), FU-3 (`InsectIdentification`); structurally closes ADR-009's authority loop for entomology. Phase 0 absorbs FU-1 PR-3. |

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

**Source:** [`notes/fu-1-plan.md`](notes/fu-1-plan.md). **Unblocked** 2026-05-09 by paged-queries landing. Sequenced after #1 so the species-narrowing editorial cycle can optionally use the console for data entry once write routes land.

| Slice  | Status     | Description                                                                                                                                                                                                                               |
|--------|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| PR-1   | ✅ landed   | Kernel `LinnaeanFamily` / `LinnaeanGenus` / `LinnaeanSpecies` interfaces.                                                                                                                                                                  |
| PR-2a  | ✅ landed   | `InsectFamily` aggregate.                                                                                                                                                                                                                  |
| PR-2b  | ✅ landed   | `PlantFamily` aggregate.                                                                                                                                                                                                                   |
| PR-2c  | ✅ landed   | `InsectGenus` aggregate.                                                                                                                                                                                                                   |
| PR-2d  | ✅ landed   | `PlantGenus` aggregate.                                                                                                                                                                                                                    |
| PR-2e  | ✅ landed   | Catalog wiring + queries for the four new aggregates.                                                                                                                                                                                      |
| PR-2f  | ⏳ pending  | **Species narrowing.** Heaviest editorial slice — `Plant` and `InsectSpecies` gain typed `genusName`; backfill ~6 insect families + ~6 insect genera + ~17 plant genera × 4 Durrell description levels. JSON migration of every species record. |
| PR-2g  | ⏳ pending  | A1-F1 closure — bundle JSON re-emit under fully-binomial catalog; identifier cleanups; pressure-test finding moves CONTINGENT → CLOSED.                                                                                                    |
| PR-3   | ↪ rolled    | **Subsumed by identification roadmap Phase 0** (#10). Console family + genus views land there with an identification-readiness lens — adds a reusable taxonomic-scope rendering primitive that Phase 2 reuses for session scope and pending-organism display. |

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

Phase 1b is the running record; further findings (A1-F2+, A2-F\*, A3-F\*, A4-F\*) will be added as Pat continues node-by-node evaluation. Next node per the session log: `Compound` or `PhytochemicalConstituent`. Pat chooses at session start (charter §6).

Related deferred items (in [`99-followups.md`](pressure-test/battus-philenor/99-followups.md)):
- **FU-1** — under-identified organisms. Catalog side closes via PR-2f / PR-2g. **Identification side closes in identification roadmap Phase 3** (#10).
- **FU-2** — bibliography/provenance kernel. **`kernels/bibliography` lands in identification roadmap Phase 4** (#10) as part of the EOL adapter work.
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

**Source:** [`plans/identification.md`](plans/identification.md). Sketch, 2026-05-10.

Multi-week, multi-phase roadmap for the per-domain identification workflow, starting in `insects`. Naturalist conventions throughout (`NamedEntity` / `Entity` / `Aggregate` / `ValueObject`; cross-domain by `EntityName`; UUIDv7; no Spring in api modules). Per the FU-3 stance, kernel extraction (`kernels/identification`) is deferred until a second domain proves the same shape.

| Phase | Name                                          | Status   | Gates on                                  |
|-------|-----------------------------------------------|----------|-------------------------------------------|
| 0     | Taxonomic navigation — api/console review     | next     | FU-1 PR-2f / PR-2g (in flight)            |
| 1     | External-source seam in console               | sketched | independent of Phase 0; can parallelise   |
| 2     | `InsectIdentification` workflow (FU-3)        | sketched | Phases 0 + 1                              |
| 3     | FU-1 closure — under-identified organisms     | sketched | Phase 2; FU-1 PR-2g                       |
| 4     | EOL trait fetch — closes ADR-009 loop         | sketched | Phase 2 (Phase 3 helpful, not strict)     |

**Phase 0 absorbs FU-1 PR-3** — the existing "console family + genus views" slice gains an identification-readiness lens (taxonomic-scope rendering primitive, scope-at-domain via `DomainId`). Phase 1 establishes a shared external-source kernel facade — the same seam vision-assist (#9) was sketched against; vision becomes a second adapter on the same kernel. Phase 4 lands `kernels/bibliography` (closes FU-2) and the EOL trait API adapter; together they close the ADR-009 authority loop for the entomology slice ("Claude explains, references authorise").

**Resolution mapping:** FU-1 (identification side) → Phase 3; FU-2 → Phase 4; FU-3 → Phase 2. Their followup entries in [`99-followups.md`](pressure-test/battus-philenor/99-followups.md) carry "rolled into roadmap" pointers.

**Phase 0 plan is not yet promoted.** Promote when FU-1 PR-2f / PR-2g land and the audit subject (the `InsectQuery` shape and console state at that point) is stable to plan against.

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
fu-1 PR-2f  ←  next on the path
      │
      └─► search-index re-assembly trigger
          (new milestone in catalog-kernel.md)

fu-1 PR-2f ─► fu-1 PR-2g ─► pressure-test A1-F1 closes
                  │
                  ▼
            ┌──────────────────────────────────────────────────┐
            │  identification roadmap  (plans/identification.md) │
            │                                                    │
            │  Phase 0 — taxonomic nav review (subsumes PR-3)    │
            │  Phase 1 — external-source seam  ←  parallelisable │
            │      │                                             │
            │      ▼                                             │
            │  Phase 2 — InsectIdentification workflow (FU-3)    │
            │      │                                             │
            │      ├─► Phase 3 — FU-1 closure (under-identified) │
            │      └─► Phase 4 — EOL adapter + kernels/biblio    │
            │                    (closes FU-2 + ADR-009 loop)    │
            └──────────────────────────────────────────────────┘

(parallel — interleavable)

command-framework second-domain rollout (chemistry) — independent
command-framework aggregate commands     — deferred until forced by a use case

catalog-kernel M9b ─► M10
                  │
                  └─► M11, M12 (independent)

admin-console view 4              (gated on scheduled-task runner; not actionable yet)

pressure-test Phase 1b node walks (independent; surfaces new FUs as it proceeds)

vision-assisted-identification          (deferred — long horizon; plugs into roadmap Phase 1's facade when unfrozen)
```

---

## Decided ordering (Option B)

**Rationale.** The read side shipped end-to-end through paged-queries; the
write side is now opened by the command-framework pilot (landed 2026-05-09).
Add durable persistence next so any console write route entered during
PR-2f's editorial cycle survives restart. Defers the RDBMS adapter without
losing data.

0. ✅ **Command framework pilot** — landed `482b48d`. ([source](plans/command-framework.md))
1. ✅ **Runtime data persistence** — origin-tracked source-tree write-back on `TestEntitySource`; classpath heuristic locates the target; static-final flag keeps tests no-op. Console writes update canonical JSON in place. Initial commit `2a4dea4`; `@JsonValue` + `defaultInsertFile` follow-ups in `b6c4957`. ([source](plans/runtime-data-persistence.md))
2. ✅ **First console write route** — landed `b6c4957`. `POST /insects/{name}/images` exercises the command port + persistence end-to-end. Surfaced the two persistence bugs noted in #1. CSRF / form rendering / web-mvc tests covered.
3. **FU-1 PR-2f** ← **next.** Species narrowing (heaviest editorial slice). PR-2f's family/genus prose can optionally be entered through the new console write route rather than hand-edited JSON. *Also the first prereq of identification roadmap Phase 0.*
4. **FU-1 PR-2g** — A1-F1 closure. Pressure-test finding moves CONTINGENT → CLOSED. *Final prereq of roadmap Phase 0.*
5. **Identification roadmap Phase 0** — taxonomic navigation review. Audits `InsectQuery` for identification-readiness, lands the family/genus console pages (subsumes the old FU-1 PR-3), introduces the reusable taxonomic-scope rendering primitive that Phase 2 reuses. Promote [`plans/identification.md`](plans/identification.md) Phase 0 to its own implementation plan when this slot opens.
6. **Identification roadmap Phase 1** — external-source kernel seam. Independent of Phase 0; can parallelise. GBIF as first wired adapter (no auth). Console renders external authority links; forms accept EOL/BugGuide/iNat/GBIF identifiers.
7. **Identification roadmap Phase 2** — `InsectIdentification` workflow (FU-3). Per-domain dichotomous-key. Reuses Phase 0's scope primitive; reuses Phase 1's link rendering for deep-link surfacing.
8. **Identification roadmap Phase 3** — FU-1 closure (under-identified organisms). Decides among the three FU-1 paths after Phase 2 surfaces the right answer.
9. **Identification roadmap Phase 4** — EOL adapter + `kernels/bibliography` (FU-2). Closes ADR-009's authority loop for entomology.
10. **Catalog kernel M9b → M10 → M11 → M12** — finish-line work; small per-milestone. Can interleave at any point.
11. **Pressure-test Phase 1b — next node** (`Compound` or `PhytochemicalConstituent`). Interruptible; slot in between coding sessions.

### Notes on the ordering

- **Editorial cost remains concentrated in FU-1 PR-2f** (~29 new descriptions × 4 Durrell levels). Largest single effort remaining; schedule around availability for prose work.
- **#1 → #2 both shipped 2026-05-09.** Two focused changes that compound into "console can persist data," verified end-to-end via manual QA on insect images.
- **Catalog kernel M9b–M12 is interleavable** — pick up between bigger efforts. M10's observation pipeline is its only sequencing constraint (M9b first).
- **Pressure-test Phase 1b is interruptible.** Produces findings, not code. Slot in when the engineering queue is light.
- **Backlog triage** ([`notes/pending-implementation.md`](notes/pending-implementation.md)) and **Q0 ADR** ([`notes/open-questions.md`](notes/open-questions.md)) are not on the path. Worth a 30-minute independent pass to retire stale items or schedule the live ones.
- **Vision-assisted identification** ([`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md)) is deliberately **off the near-term path**. The sketch is captured so the design space is organized when Pat does decide to pick it up; until then it does not compete for attention.

---

## Conventions

- Every row points to its source-of-truth doc; this file is an index, not a duplicate.
- One row per effort. Sub-milestones live in the source doc, not here.
- Update a row's **Status** when it changes. Move completed rows to **Recently completed** with a final commit reference.
- New effort? Add a row at the bottom of **At a glance**, write a brief "what's left" entry in the section below, and link the source doc.
