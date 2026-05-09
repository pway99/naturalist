# Work Tracker

Single index of every work effort currently in flight or recently completed.
This file does not own scope — every row links to its source-of-truth doc.
Update a row when its status changes; promote completed rows to the
"recently completed" section so the active table stays focused.

Last updated: 2026-05-09. Ordering: **Option B** chosen — open the write surface (command framework) before the heavy editorial slice (FU-1 PR-2f). Runtime data persistence sketch added as a follow-up to the command-framework pilot, ahead of any console write route.

---

## At a glance

| #   | Effort                                           | Type          | Status                       | Source                                                                                | Notes                                                                                              |
|-----|--------------------------------------------------|---------------|------------------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1   | Command framework                                | Plan          | **next** — pilot pending     | [`plans/command-framework.md`](plans/command-framework.md)                            | Kernel + insects pilot; no controller wiring, no persistence change. Selected as the next effort.  |
| 2   | Runtime data persistence                         | Plan (sketch) | gated on #1                  | [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md)              | Optional `JsonRuntimeStore` write-back hook on `TestEntitySource` so console-driven writes survive restart. Defers RDBMS. |
| 3   | FU-1 — Family/Genus catalog tiers                | Plan (notes)  | active — PR-2f after #1, #2  | [`notes/fu-1-plan.md`](notes/fu-1-plan.md)                                            | Unblocked 2026-05-09 by paged-queries. PR-2f, PR-2g, PR-3 remain. Closes pressure-test A1-F1.      |
| 4   | Catalog kernel — finish line                     | Plan          | active — M9b/M10/M11/M12     | [`plans/catalog-kernel.md`](plans/catalog-kernel.md)                                  | Typed observation types + coverage assertion + ArchUnit guard + ADR. Independent of FU-1.          |
| 5   | Admin console                                    | Plan          | active — view 4 (future)     | [`plans/admin-console.md`](plans/admin-console.md)                                    | Views 1–3 shipped. View 4 (`/admin/schedules`) is gated on the future scheduled-task runner.       |
| 6   | Pressure test — *Battus philenor*                | Pressure test | Phase 1b active              | [`pressure-test/battus-philenor/01-findings.md`](pressure-test/battus-philenor/01-findings.md) | Node-by-node evaluation continues. A1-F1 CONTINGENT on FU-1. Next node: `Compound` or `PhytochemicalConstituent`. |
| 7   | Backlog — Soil/Sensor services                   | Notes         | unprioritized backlog        | [`notes/pending-implementation.md`](notes/pending-implementation.md)                  | Stale in spots (BehavioralCollection now exists). Triage pass needed before picking from this list. |
| 8   | Open design questions                            | Notes         | Q0–Q7 in flight              | [`notes/open-questions.md`](notes/open-questions.md)                                  | Several deferred-but-not-resolved. Q0 (Aggregate × Entity ADR) is the only one with structural impact. |
| 9   | Vision-assisted identification                   | Plan (sketch) | **deferred** — long horizon  | [`plans/vision-assisted-identification.md`](plans/vision-assisted-identification.md)  | Claude Vision–driven console identification + draft-review-confirm workflow. Revisit after #1–#3 land and ≥4 weeks of manual entry surface real friction. |

---

## Active efforts — what's left

### 1. Command framework pilot — **next**

**Source:** [`plans/command-framework.md`](plans/command-framework.md). Single PR; insects only; no controller wiring; no persistence change.

- New: `EntityCommand`, `AbstractEntityCommand` in `kernels/framework`.
- New: `EntityCommandContractTest` in `kernels/framework-test`.
- New: public `InsectCommand` namespace + `*CommandImpl` adapters in insects-api / insects-core.
- Test context gains an `insectCommand()` accessor.
- **Out of scope of the pilot:** controller wiring, second-domain rollout, aggregate-level commands, durable persistence (covered separately by #2).

### 2. Runtime data persistence

**Source:** [`plans/runtime-data-persistence.md`](plans/runtime-data-persistence.md). Sketch only — promote to numbered milestones in the implementing PR. **Gated on #1.**

The minimum-complexity stop-gap that lets console-driven inserts/updates survive restart without standing up MyBatis. Optional `JsonRuntimeStore` collaborator on `TestEntitySource`; null in tests (in-memory only), wired in the management-console composition root with a configured runtime directory outside the classpath.

- New: `JsonRuntimeStore<ENTITY>` interface + `FilesystemJsonRuntimeStore` in `kernels/framework-test`.
- `TestEntitySource` gains an opt-in two-arg constructor and a `flushRuntime()` hook on insert/update.
- Composition root binds `naturalist.data.dir` and supplies stores to subclasses that opt in.
- **Out of scope:** RDBMS adapter, multi-process coordination, console write routes (separate follow-up under `command-framework.md`), catalog index re-assembly trigger (own follow-up under `catalog-kernel.md`).

### 3. FU-1 — Family/Genus catalog tiers

**Source:** [`notes/fu-1-plan.md`](notes/fu-1-plan.md). **Unblocked** 2026-05-09 by paged-queries landing. Sequenced after #1 and #2 so the species-narrowing editorial cycle can optionally use the console for data entry.

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
| PR-3   | ⏳ pending  | Console — family + genus list/detail views; species pages link up the chain.                                                                                                                                                              |

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
- **FU-1** — partially in motion (the family/genus plan is the closure path; pending-organism mechanism is the missing piece).
- **FU-2** — bibliography/provenance kernel. OPEN. Pat-driven, independent.
- **FU-3** — identification-as-first-class. OPEN. Likely co-evolves with FU-1.
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

---

## Recently completed

| Effort                    | Completed  | Source                                                    | Final commit                                                   |
|---------------------------|------------|-----------------------------------------------------------|----------------------------------------------------------------|
| Paged queries (steps 1–7) | 2026-05-09 | [`plans/paged-queries-plan.md`](plans/paged-queries-plan.md) | `3789319` (delete unbounded) → `3a5ea41` (mark plan implemented) |

---

## Dependency graph

```
paged-queries  ✅ done
      │
      ▼
command-framework pilot ──► runtime-data-persistence ──► (first console write route, follow-up)
                                                            │
                                                            └─► search-index re-assembly trigger
                                                                (new milestone in catalog-kernel.md)

(parallel — interleavable with the above once command-framework lands)

fu-1 PR-2f ─► fu-1 PR-2g ─► pressure-test A1-F1 closes
                  │
                  └─► fu-1 PR-3 (console)

catalog-kernel M9b ─► M10
                  │
                  └─► M11, M12 (independent)

admin-console view 4              (gated on scheduled-task runner; not actionable yet)

pressure-test Phase 1b node walks (independent; surfaces new FUs as it proceeds)

FU-2 / FU-3 (pressure-test follow-ups; independent kernels, not yet planned)

vision-assisted-identification          (deferred — long horizon; revisit after manual entry has been daily workflow for weeks)
```

---

## Decided ordering (Option B)

**Rationale.** The read side shipped end-to-end through paged-queries; the
write side is still internal-only. Open the symmetric write port (#1)
before the heavy editorial slice (FU-1 PR-2f). Add durable persistence
(#2) right after so any console write route entered during PR-2f's
editorial cycle survives restart. Defers the RDBMS adapter without
losing data.

1. **Command framework pilot** — `EntityCommand` + insects-only adapters. Small, focused, no persistence change. ([source](plans/command-framework.md))
2. **Runtime data persistence** — `JsonRuntimeStore` write-back hook on `TestEntitySource`. Console-driven inserts/updates persist to a configured runtime directory; canonical JSON stays read-only. ([source](plans/runtime-data-persistence.md))
3. **First console write route** *(follow-up under `command-framework.md`)* — exercises #1 + #2 end-to-end with one concrete `@PostMapping`. CSRF / form rendering / route-test review live here.
4. **FU-1 PR-2f** — Species narrowing (heaviest editorial slice). PR-2f's family/genus prose can optionally be entered through the new console write route rather than hand-edited JSON.
5. **FU-1 PR-2g** — A1-F1 closure. Pressure-test finding moves CONTINGENT → CLOSED.
6. **FU-1 PR-3** — Console for family/genus tiers.
7. **Catalog kernel M9b → M10 → M11 → M12** — finish-line work; small per-milestone. Can interleave at any point after #1.
8. **Pressure-test Phase 1b — next node** (`Compound` or `PhytochemicalConstituent`). Interruptible; slot in between coding sessions.

### Notes on the ordering

- **Editorial cost remains concentrated in FU-1 PR-2f** (~29 new descriptions × 4 Durrell levels). Largest single effort remaining; schedule around availability for prose work.
- **#1 → #2 → #3 are all small.** None is a "heavy" PR; they're three focused changes that compound into "console can persist data."
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
