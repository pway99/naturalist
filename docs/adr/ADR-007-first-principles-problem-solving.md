# ADR-007: First Principles Problem Solving

**Status:** Accepted
**Full rationale:** [rationale/ADR-007-first-principles-problem-solving.md](rationale/ADR-007-first-principles-problem-solving.md)

## Decision

Domain modelling starts from the domain (what the thing *is*), not from infrastructure
(how it will be stored/queried/displayed). When a problem surfaces, trace to root cause
before applying a correction.

### Diagnostic discipline
1. **Ask what it is, not where it lives.** "Will be a row in a table" is not a reason to
   be an entity. Tables are storage; identity is domain.
2. **Ask whether it has independent identity.** Meaningfully referenced from outside its
   owner? Recognised by name? If not, it's a value object.
3. **Ownership vs relationship.** A foreign key naming the parent is ownership — the
   owned thing is a value object or child entity, not a peer entity.
4. **Suspect any `EntityName` that duplicates a parent's identity.** Not its own identity.
5. **Suspect any entity whose repository is never queried independently.** Not an entity.

### Root-cause discipline
6. **When the visible fix is easy, suspect a deeper problem.** What made the error
   possible in the first place?
7. **Trace the symptom to the structural gap.** Wrong model = symptom. Cause is usually a
   missing abstraction or a false dichotomy in the framework.
8. **Validate root-cause fixes against future scenarios.** Must prevent a class of errors,
   not one instance.

### When the model is wrong
- Fix immediately. Infrastructure accumulates around wrong models (repositories, JSON,
  identifiers) and correction cost grows with time.
- Not aesthetic refactoring — wrong identity models produce wrong constraints, wrong
  uniqueness rules, wrong queries, eventually wrong data.

## Consequences

- Value objects accidentally promoted to entities are corrected immediately
- Persistence-first anti-pattern ("will be a table → is an entity") explicitly guarded
- Ownership foreign keys signal possible value-object status
- Local fixes leaving structural gaps are incomplete
- Root-cause fixes validated against triggering scenario + ≥ 1 future scenario
