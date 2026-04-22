# ADR-019: Pull Request Size
> [rationale](rationale/ADR-019-pull-request-size-and-review-fatigue.md)

- **One concern per PR.** Boundary drawn by type of work, not by entity.
- **Sequential, not bundled.** For new-entity infrastructure, merge in dependency order:
  1. Entity record + identifiers
  2. `TestEntitySource` + JSON catalog + test
  3. Repository interface + in-memory mock + behavioral contract + contract tests
- **~400-line guideline** on meaningful diff (exclude imports, formatting, generated).
  Guideline, not hard limit — a 500-line cohesive PR beats two artificial 250-line splits.
- `/test-entity-source` and `/entity-repository` are invoked separately on purpose.
