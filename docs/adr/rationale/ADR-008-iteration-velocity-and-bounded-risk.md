# ADR-008: Iteration Velocity and Bounded Risk

**Status:** Accepted

## Context

Ideas are perishable. A design insight that is obvious during a modelling session may be
unrecoverable two days later. The connection between a domain observation, a code smell,
and an architectural correction exists in working memory — and working memory clears when
the naturalist rests, shifts context, or runs out of tokens.

The risk of moving fast is breaking things. The risk of moving slow is losing the thought.
This project resolves the tension by limiting the blast radius of any single change, so
that speed and safety are not in opposition.

## Decision

### Move fast, work hard, iterate, rest

The development rhythm is: intense focused work in bounded sessions, followed by rest.
Rest is not optional and not a reward — it is part of the process. The quality of
architectural decisions degrades with fatigue. The ADR-005 amendment (FactName/EntityName
unification) emerged after stepping away and returning with fresh perspective. The
original Catalog/Fact infrastructure split was designed during a long session and
over-engineered the framework. The correction came after rest.

### Capture thoughts before they are lost

When an idea surfaces — a design correction, a new domain relationship, an architectural
smell — it must be captured immediately, even if incompletely. An ADR draft, a TODO
comment, a CLAUDE.md note, or a conversation summary is sufficient. The goal is not to
finish the thought but to preserve enough signal that a future session can reconstruct it.

This ADR itself was dictated in bullet points at the end of a long session, before the
ideas were lost.

### Limit blast radius with strict bounded contexts

Every change operates within a bounded context — a module, a domain, a sub-context
within a module. The modular monolith architecture (ADR-004) and package-private visibility
enforcement exist precisely to contain the impact of any single change. A mistake in
`chemistry-api` cannot corrupt `soil-api` because the compiler enforces the boundary.

When a change crosses module boundaries — like the ADR-005 refactor that touched
`kernels/framework`, `kernels/framework-test`, `domains/identifiers`, and every domain's
`repository-test` module — the blast radius is large by necessity. These changes require
the highest concentration and the most thorough verification. They should not be attempted
when fatigued.

### Work on a branch

Changes that carry risk are developed on a branch and merged only after verification.
The branch is the safety net that allows aggressive iteration without permanently
damaging the mainline. A failed experiment on a branch costs nothing. A failed experiment
on main costs recovery time and trust.

Branches should be short-lived. A branch that lives for days accumulates merge conflicts
and diverges from the team's mental model of the codebase. The ideal branch is created,
worked, verified, and merged within a single focused session.

### Mark incubating work with @Incubating

When a concept is structurally sound but not yet proven through full integration — a new
framework abstraction, a domain model that has not been exercised by real queries, an
API that has no consumer yet — it is annotated `@Incubating`.

```java
@Incubating
public abstract class FactName extends EntityName { ... }
```

`@Incubating` is a visibility annotation, not a quality annotation. It means: "this
exists, it compiles, it has tests, but it has not yet been validated by production usage
or by a consumer outside its immediate module. Its API may change." It signals to future
sessions and collaborators that the concept is intentional but provisional.

`@Incubating` is removed when the concept has been exercised by at least one real consumer
outside its defining module and has survived a review cycle without API changes.

## Consequences

- Ideas are captured immediately in whatever form is available — ADR drafts, TODOs,
  CLAUDE.md notes — rather than deferred to a future session where they may be lost
- Rest is scheduled as part of the work, not after the work. Architectural decisions
  made while fatigued are suspect
- Every change is bounded by module visibility. Cross-module changes require peak
  concentration and thorough verification
- Branches are the default for risky work. Main stays clean
- `@Incubating` provides visibility into which abstractions are provisional, without
  blocking progress on building them
- Short iteration cycles (branch → work → verify → merge → rest) are preferred over
  long sessions that accumulate risk and fatigue
