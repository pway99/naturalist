# Plans

Single index for every effort plan in the repo. Each plan is a durable,
section-by-section work tracker — the kind that survives across sessions
and lets a fresh contributor pick up cold from the document alone.

When a plan is complete, move it to [`archive/`](archive/) and update its
row below. When a new effort starts, drop a file in this directory and add
a row.

## Active

| Plan                                                     | Status                                 | Scope                                                                                                                                                                                  |
|----------------------------------------------------------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [admin-console.md](admin-console.md)                     | active — view 4+ pending               | Secure `/admin/**` surface in `apps/management-console`. Views 1–3 (`resilience`, `domain-services`, `catalog`) shipped.                                                               |
| [catalog-kernel.md](catalog-kernel.md)                   | active — superseded sections shipped   | Cross-domain reference resolution kernel. Routing-era milestones M0–M7 shipped; later milestones rewritten by the redirect.                                                            |
| [catalog-kernel-redirect.md](catalog-kernel-redirect.md) | active — M9b / M10 / M11 / M12 pending | 2026-04-29 pivot of the catalog plan from exact-match routing to search-and-discovery. M1.5 / M2′ / M4′ / M7′ / M-Search-UI-A / M-Insects-Catalog / M-Chemistry-Catalog / M9a shipped. |

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

- [`../pending-implementation.md`](../pending-implementation.md) — prioritized backlog of work not yet planned.
- [`../open-questions.md`](../open-questions.md) — design decisions in flight.
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
