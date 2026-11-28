# Observability decisions

History of the decisions behind [`observability-standards.md`](../observability-standards.md).
The standards doc is the rubric; this file records when and why each rule was decided. The
`observability-reviewer` agent does not read it.


- A decision is observed by the decider, not by a caller that translates it (J2). Pat,
  2026-11-15.
- A lookup from a string (enum, sealed value type, `@JsonCreator`) returns null for an
  unrecognised value; no type gets an `UNRECOGNISED` member (C1). Supersedes the earlier
  `UNRECOGNISED` decision. Pat, 2026-11-15.
- Variable names: `observer` for the class's `Observer`, `methodObserver` for a method's
  `MethodObserver` (M13). Pat, 2026-11-15.
- The reviewer is a read-only project agent, `.claude/agents/observability-reviewer.md`. The
  session that wrote a change dispatches it before declaring the task done (CLAUDE.md,
  *Completeness verification*); Pat runs it on demand with `/observability-review [PR# | branch | path]`.
  A Stop hook that forces the review is deferred. Design:
  [`docs/plans/2026-11-15-observability-reviewer-design.md`](../plans/2026-11-15-observability-reviewer-design.md).
  Pat, 2026-11-15.
- An edge is an inbound adapter (`@RestController`, message handler); Jackson errors on inbound
  input are raised and handled there, before core (P3, B2). A reply to our own call is noticed
  by the code that parses it (J3). Pat, 2026-11-15.
- A double count is one failure noticed twice at the same call site. A producer and its consumer
  each noticing the same failure is fine; the metrics are read together (P2). Pat, 2026-11-15.
- Reviewer scope: everything inside a changed method, old lines included; callers whose data or
  control flow the change alters; and, reported separately for awareness, concerns met in
  existing code. Pat, 2026-11-15.
- M8 is checked on every class a change touches. A helper uses its own method name unless it is
  shared to avoid duplication, when it may take the caller's `MethodObserver` (M4). Pat,
  2026-11-15.
- A custom Jackson deserializer we write observes the `JsonNode`'s fields as arguments
  (`notBlank`, `kebabFormat`) before using them (B2). Pat, 2026-11-15.
- Startup assembly code (composition roots, `*Assembly.from`, startup registry validation) is
  exempt from J1. Pat, 2026-11-15.
- A redirect or fallback page in answer to bad input is a refusal, noticed at the edge (B1).
  Pat, 2026-11-15.
- A value a method calculates, even from its parameters alone, is state, not an argument (M2).
  Pat, 2026-11-15.
- A value of a framework kind (ValueObject, EntityName, …) is observed with its kind's
  constraint, not just null-checked (M14). Pat, 2026-11-15.
- An interface's static method may build its Observer inline (M8). Pat, 2026-11-15.
- An edge observes a refusal it answers (rather than throws) with `arguments(...).observe(...)`;
  a request for one thing tells the caller it was invalid, a request for a list returns what is
  possible (B1, M1). Pat, 2026-11-15. The level was refined 2026-11-27, below.
- A command observes the aggregate it builds before the transaction, with cleanup covering the
  refusal (J4); a rule a change adds about an entity's field is declared in that entity's
  `invariants()` (J5). Pat, 2026-11-15.
- The author fixes only what the change added, edited or broke; everything else the reviewer
  sees is reported as existing code for the maintainer, who reviews existing code separately in
  chunks. Pat, 2026-11-15.
- Terminate as early as possible: an edge that can detect an invalid request checks it, observes
  the refusal and answers before calling core; the command still validates (B1). A failed cleanup
  (a delete attached with `addSuppressed`) is noticed: leftover files fill the disk (J3). Pat,
  2026-11-16.
- An edge orders its checks cheapest first: parameters, then reading the body, then decoding
  (B1). Pat, 2026-11-17.
- A constraint that can never fail is still kept when, under `ALWAYS`, it sits inside a restricted
  branch and counts entries to it (the config-admin login): a critical non-error signal for the
  maintainer (J5). Pat, 2026-11-25.
- A constraint that can't fail today only because other code keeps a contract is a guard, kept
  and named after the contract it guards. Only a constraint that can never fail by construction is
  a finding (J5). Removing guards lowers the confidence a refactor needs. Pat, 2026-11-26.
- A method another class can call observes every parameter it uses before using it; only a
  private helper may rely on its caller's check (J7). Pat, 2026-11-26.
- An edge refusal of what the caller sent (an invalid or unknown name, a lookup that finds
  nothing) is observed at `INFO`; a value our own code handed the edge that fails its invariants
  is observed at `WARN` (B1, M1). Taken from the plants console. Pat, 2026-11-27.
- A double count is judged inside one handler only (M15). Observe wherever a value or failure is
  consumed, however often it was observed upstream; the reviewer never traces the call path to
  decide whether a layer may observe (P2, J1, J3). Supersedes the 2026-11-15 nested-handler and
  "inner layer already noticed" wording. Pat, 2026-11-27.
- A caller that translates a decider's refusal may observe it under its own name, never the
  decider's, so name-keyed alerts fire once (J2). Pat, 2026-11-27.
- Parameters and record components use `java.lang` wrapper types, not primitives, so a missing
  value is null and the argument observation counts it (J7, M16). Pat, 2026-11-27.
- The levels (`ERROR` framework-only, `WARN`, `INFO`) are stated in the standards, not only in
  `Level`'s Javadoc, and P8 (production teaches) is a principle. Pat, 2026-11-27.
- Observe and throw only where the invalid state would itself throw. A method observes what it was
  given, not what it built: `*Dbo.from(entity)` checks `entity` as its argument and does not then
  throw on the Dbo it built, because that takes the decision away from the consumer (J4).
  Supersedes the 2026-11-03 "Dbo.from throws on the write path" example. Pat, 2026-11-27.
- A Dbo's mapping to the entity (`toEntity`, child `to*`) is a read and is permissive: its
  arguments are checked only with `notNull`, and it throws on nothing it read; unrecognised columns map to null (C1) and the
  repository read and its consumer observe the result (J4, M1). Settles "bad stored value on read"
  as hand-it-on. Pat, 2026-11-27.
- A repository read checks its arguments and returns what it read without observing it; the
  consumer observes (J4). Supersedes "a read observes what it returns at WARN". The inherited
  `AbstractEntityRepository` reads and most domain reads still observe; a separate sweep removes
  them. Pat, 2026-11-27.
