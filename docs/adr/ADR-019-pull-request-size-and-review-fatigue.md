# ADR-019: Pull Request Size and Review Fatigue

**Status:** Accepted
**Full rationale:** [rationale/ADR-019-pull-request-size-and-review-fatigue.md](rationale/ADR-019-pull-request-size-and-review-fatigue.md)

## Decision

- **One concern per PR.** Boundary drawn by type of work, not by entity.
- **Sequential, not bundled.** For a new entity's infrastructure, merge in dependency order:
  1. Entity record + identifiers
  2. `TestEntitySource` + JSON catalog + test
  3. Repository interface + in-memory mock + behavioral contract + contract tests
- **~400-line guideline** on meaningful diff (excludes imports, formatting, generated files).
  Guideline, not hard limit — a 500-line cohesive PR beats two artificial 250-line splits.
- Skills `/test-entity-source` and `/entity-repository` are invoked separately on purpose;
  combining them trades review quality for author convenience.

## Consequences

- Each PR independently reviewable — no companion-PR context required
- Sequential merges create natural checkpoints: structural errors caught before dependent
  layers are built on top
- Domain model errors (the highest-cost defects here) get focused review
- 400-line guideline applies to all PRs, not just skill-generated ones
