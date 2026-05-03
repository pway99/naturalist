# ADR-008: Iteration Velocity and Bounded Risk

> [rationale](rationale/ADR-008-iteration-velocity-and-bounded-risk.md)

- Capture thoughts immediately — ADR draft, TODO, CLAUDE.md note — enough signal for a
  future session to reconstruct.
- Blast radius bounded by module visibility (ADR-004). Cross-module changes require peak concentration.
- Work on a short-lived branch: create → work → verify → merge in one session. Main stays clean.
- `@Incubating` marks structurally-sound concepts not yet proven by a real consumer.
  Visibility annotation, not quality. Removed after one external consumer survives one review
  cycle without API changes.
