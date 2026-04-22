# ADR-007: First Principles Problem Solving
> [rationale](rationale/ADR-007-first-principles-problem-solving.md)

Model from the domain (what the thing *is*), not from infrastructure (how it will be
stored/queried/displayed). Trace to root cause before correcting.

**Diagnostic questions**
1. Ask what it is, not where it lives. "Will be a row in a table" is not a reason to be
   an entity.
2. Ask whether it has independent identity. Referenced from outside its owner? Named? If
   not, it's a value object.
3. Ownership vs relationship. A foreign key naming the parent = ownership — the owned
   thing is a value object or child entity, not a peer entity.
4. Suspect any `EntityName` that duplicates a parent's identity.
5. Suspect any entity whose repository is never queried independently.

**Root cause**
6. When the visible fix is easy, suspect a deeper problem. What made the error possible?
7. Trace the symptom to the structural gap — usually a missing abstraction or a false
   dichotomy in the framework.
8. Validate root-cause fixes against ≥ 1 future scenario — must prevent a class, not an instance.

**When the model is wrong, fix immediately.** Infrastructure accumulates around wrong
models; correction cost grows with time. Wrong identity produces wrong constraints, wrong
uniqueness, wrong queries, wrong data.
