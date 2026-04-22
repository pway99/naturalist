# ADR-008: Iteration Velocity and Bounded Risk

**Status:** Accepted
**Full rationale:** [rationale/ADR-008-iteration-velocity-and-bounded-risk.md](rationale/ADR-008-iteration-velocity-and-bounded-risk.md)

## Decision

- **Move fast, work hard, iterate, rest.** Rest is part of the process. Architectural
  decisions made while fatigued are suspect.
- **Capture thoughts immediately.** ADR draft, TODO, CLAUDE.md note — enough signal for a
  future session to reconstruct. Don't wait to finish the thought.
- **Limit blast radius with strict bounded contexts.** Module visibility (ADR-004) contains
  impact. Cross-module changes require peak concentration.
- **Work on a branch.** Short-lived: create → work → verify → merge in one session. Main stays clean.
- **`@Incubating`** marks structurally-sound concepts not yet proven by a real consumer.
  Visibility annotation, not quality. Removed after survival of one consumer outside the
  defining module + one review cycle without API changes.

## Consequences

- Ideas captured in whatever form is available rather than deferred and lost
- Rest scheduled as part of the work
- Every change bounded by module visibility
- Branches default for risky work; main stays clean
- `@Incubating` signals provisional abstractions without blocking progress
- Short iteration cycles preferred over long sessions that accumulate risk and fatigue
