# Plans

Single index for every effort plan in the repo. Each plan is a durable,
section-by-section work tracker — the kind that survives across sessions
and lets a fresh contributor pick up cold from the document alone.

When a plan is complete, move it to [`archive/`](archive/) and update its
row below. When a new effort starts, drop a file in this directory and add
a row.

## Active

| Plan                                         | Status                                 | Scope                                                                                                                                                              |
|----------------------------------------------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [admin-console.md](admin-console.md)                                   | active — view 4+ pending               | Secure `/admin/**` surface in `apps/management-console`. Views 1–3 (`resilience`, `domain-services`, `catalog`) shipped.                                           |
| [catalog-kernel.md](catalog-kernel.md)                                 | active — M9b / M10 / M11 / M12 pending | Cross-domain reference resolution kernel. Search + inverse-routing + URL-linker SPIs. The 2026-04-29 search-and-discovery redirect is folded into this single doc. |
| [command-framework.md](command-framework.md)                           | active — pilot landed; follow-ups      | Pilot shipped 2026-05-09 (`482b48d`): `EntityCommand` kernel + insects-only adapters. Remaining: console controller wiring, second-domain rollout (chemistry), aggregate-level commands. |
| [runtime-data-persistence.md](runtime-data-persistence.md)             | active — implemented; awaiting smoke   | Origin-tracked write-back on `TestEntitySource`: console writes update canonical JSON in source tree; classpath heuristic locates the write target; `static final` flag keeps tests no-op. Implemented 2026-05-09; verified by manual console smoke. |
| [vision-assisted-identification.md](vision-assisted-identification.md) | sketch — deferred (long-horizon)       | Console / web-driven Claude Vision identification: image → preprocess → tool-call → typed `IdentificationDraft<T>` → review → `EntityCommand.insert`. Revisit after manual entry has been daily workflow for weeks. |

## Archived

Completed efforts that no longer drive ongoing work. Kept for context;
not loaded by default.

| Plan                                                                                 | Completed  | Scope                                                                                                                                             |
|--------------------------------------------------------------------------------------|------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| [archive/chemistry-taxonomy-refactor.md](archive/chemistry-taxonomy-refactor.md)     | 2026-04-26 | Split `CompoundType` into `ChemicalNature` × `PhysicalForm` × `Set<FunctionalRole>`. Phase 13 followups also captured.                            |
| [archive/runtime-architecture-refactor.md](archive/runtime-architecture-refactor.md) | 2026-05-03 | Atlas → catalog rename, open `DomainId`, `apps/` and `adapters/` trees, `Resilience` facade, `@DomainService`, ArchUnit gate. M0–M10 all shipped. |

## Where else to look

Decisions and questions that don't fit the milestone shape live next door
in [`../`](..):

- [`../notes/pending-implementation.md`](../notes/pending-implementation.md) — prioritized backlog of work not yet
  planned.
- [`../notes/open-questions.md`](../notes/open-questions.md) — design decisions in flight.
- [`../briefings/`](../briefings/) — chat-prompt context documents (not plans).
- [`../adr/`](../adr/) — Architecture Decision Records.

## Conventions

- One plan per file. Don't split a coherent effort across multiple plans;
  do split a milestone if it grows mid-execution (per the "How to resume
  across sessions" pattern in the existing plans).
- Status in this index is one of: **active**, **paused**, **superseded**, **archived**.
- Each plan opens with a short narrative, then declarative architectural
  decisions, then numbered milestones with a `**Status:**` line per milestone.
- Update the row in this index whenever a plan's status changes.
