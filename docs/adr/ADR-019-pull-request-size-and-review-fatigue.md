# ADR-019: Pull Request Size and Review Fatigue

**Status:** Accepted

## Context

This project uses Claude Code skills to scaffold domain infrastructure — entity
repositories, test entity sources, behavioral contracts, in-memory mocks, and their
associated test classes. Each skill produces a coherent, self-contained unit of work:
`/entity-repository` builds the repository stack, `/test-entity-source` builds the test
data stack.

The temptation is to combine related skills into a single operation. A new entity needs
both a `TestEntitySource` and a repository — why not scaffold both at once? The answer is
that a single PR containing the full vertical slice (entity source + JSON catalog +
repository interface + in-memory mock + behavioral contract + contract tests + identifier
updates) produces a diff that is too large to review effectively.

### The research on review fatigue

The empirical evidence is unambiguous. Cisco's study of SmartBear's CodeCollaborator data
(Ciolkowski, Cohen, et al.) found that review effectiveness drops sharply after 200–400
lines of diff. Beyond that threshold, reviewers shift from deliberate analysis to pattern
scanning — they stop reading the code and start skimming it. Defects that would be caught
in a 150-line PR pass unnoticed in an 800-line PR.

Microsoft Research (Rigby & Bird, 2013) reached similar conclusions studying open-source
and industrial code review: smaller, focused changesets receive more thorough review and
produce higher-quality feedback. Google's internal engineering practices codify this as a
cultural norm — small CLs (changelists) are preferred not for aesthetic reasons but because
large CLs receive superficial review.

The mechanism is cognitive, not motivational. A reviewer holding 6–8 concepts in working
memory can reason about invariant relationships, spot missing edge cases, and verify
behavioral contracts. A reviewer holding 20+ concepts is performing recognition, not
reasoning. They will catch syntax errors and obvious bugs but miss domain model errors —
exactly the class of defects that matter most in this project.

### The domain model amplifier

Domain modelling errors are the highest-cost defects in this codebase. A wrong identity
model (ADR-007), a misclassified value object (ADR-013), or a violated aggregate boundary
produces cascading infrastructure — repositories, test sources, JSON catalogs, identifier
classes — that must all be unwound when the error is discovered. These errors are caught
by careful review of the domain semantics, not by scanning for compilation errors.

A PR that combines entity source scaffolding with repository scaffolding forces the
reviewer to hold both the data model and the behavioral contract in working memory
simultaneously. These are related but distinct concerns. Reviewing them separately allows
the reviewer to focus fully on each: first verify that the entity's structure, invariants,
and test data are correct, then verify that the repository's behavioral contract and
in-memory implementation are correct.

## Decision

### One concern per pull request

Each PR addresses a single cohesive concern. The boundary is drawn by the type of work,
not by the entity being worked on. Scaffolding a `TestEntitySource` (entity structure,
JSON catalog, test data verification) is one PR. Scaffolding an `EntityRepository`
(repository interface, in-memory mock, behavioral contract, contract tests) is a separate
PR, submitted after the first is merged.

### Sequential, not bundled

When multiple skills contribute to the same entity's infrastructure, they are executed in
separate PRs, merged in dependency order:

1. Entity record + identifiers (if new)
2. `TestEntitySource` + JSON catalog + test entity source test
3. Repository interface + in-memory mock + behavioral contract + contract tests

Each PR is self-contained and independently reviewable. The reviewer can verify one layer
of correctness before the next layer is built on top of it.

### The 400-line guideline

A PR should not exceed approximately 400 lines of meaningful diff (excluding generated
files, import reordering, and trivial formatting). This is a guideline, not a hard limit.
A 500-line PR that touches a single cohesive concern is preferable to splitting it
artificially into two 250-line PRs that cannot be understood independently.

The goal is not small PRs — it is reviewable PRs. A PR is reviewable when a competent
reviewer can hold the entire change in working memory, reason about its correctness, and
identify domain model errors within a single focused review session.

### Skill composition is a workflow, not a feature

The skills `/test-entity-source` and `/entity-repository` are designed to be invoked
separately precisely because their outputs are reviewed separately. Combining them into a
single skill would optimize for author convenience at the expense of review quality. The
cost of two PRs (two review cycles, two merge operations) is small. The cost of a domain
model error that survives a superficial review of a large PR is large — potentially an
entire entity's infrastructure built on a wrong foundation (ADR-007).

## Consequences

- Skills that scaffold infrastructure for the same entity are invoked in separate
  sessions, producing separate PRs
- Each PR is independently reviewable — the reviewer does not need context from an
  unmerged companion PR to evaluate correctness
- Domain model errors are more likely to be caught because each review cycle focuses on a
  single type of correctness (structural, behavioral, contractual)
- The sequential merge order creates a natural checkpoint: if the entity's structure or
  test data is wrong, it is caught before the repository is built on top of it
- Authors pay a small overhead in PR management (multiple branches, sequential merges) in
  exchange for significantly higher review quality
- The 400-line guideline applies to all PRs, not just skill-generated ones — manual
  changes follow the same discipline
