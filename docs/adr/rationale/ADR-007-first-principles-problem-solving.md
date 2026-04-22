# ADR-007: First Principles Problem Solving

**Status:** Accepted

## Context

During the ADR-005 refactor, two distinct problems surfaced at the same time. The first
was visible and obvious. The second was structural and hidden. Solving only the visible
problem would have left the root cause intact — and it nearly did.

### The visible problem: profiles modelled as entities

Four domain classes — `SafetyProfile`, `BioavailabilityProfile`, `SolubilityProfile`,
`VolatilizationProfile` — had been extracted from `CompoundInfo` and given their own
`PersistenceId`, `EntityName`, `TestEntitySource`, and JSON catalog files. Each carried a
`compoundName` field as a foreign key reference back to the parent compound.

When examined from first principles — "what is this object in the domain, independent of
how it is stored?" — the answer was clear: these are value objects. They have no identity
of their own. They are attributes of a compound, not independent things. The `compoundName`
field was a database join artifact — a foreign key masquerading as a domain relationship.
The `EntityName` on each profile was the parent compound's slug, which is not a natural key
for the profile itself — it is the identity of the thing that owns it.

The correction was straightforward: remove the entity infrastructure, inline the profiles
as value object components of `CompoundInfo`. The domain model became smaller, simpler,
and more accurate.

### The hidden problem: no globally unique identifier for facts

The profile extraction had been attempted because the original framework offered only two
identity strategies: a human-readable `EntityName` slug (for catalog entities) or no name
at all (for fact entities, which relied on `PersistenceId` alone). When the profiles were
extracted, they were forced into the catalog entity mould because that was the only path
that gave them a name — even though the name was meaningless (it was just the parent's slug
repeated).

The real question was never "should these profiles be entities?" — that was the visible
symptom. The real question was: **why did the framework force a choice between a
human-readable slug and no name at all?**

The answer: because there was no concept of a machine-generated, globally unique identifier
for entities that are not named catalog classifications. The framework's identity model had
a gap. Fact entities — events, observations, measurements — needed a name for
deduplication, uniform infrastructure, and cross-boundary references, but not a
human-readable slug. The `FactName` abstraction (a GUID wrapped in an `EntityName`
subclass) filled this gap, and its absence was the root cause of the profile extraction
mistake.

Without diagnosing the root cause, the profile correction would have been a local fix that
left the framework's identity gap intact — ready to produce the same class of error the
next time a domain concept needed identity but didn't fit the catalog slug model.

## Decision

Domain modelling in this project follows first principles reasoning. Every design decision
starts from the domain — what the thing *is* — not from the infrastructure — how it will
be stored, queried, or displayed. When a visible problem surfaces, the discipline is to
trace it to its root cause before applying a correction.

### The diagnostic discipline

1. **Ask what it is, not where it lives.** "This will be a row in a table" is not a reason
   to make something an entity. Tables are a storage concern. Identity is a domain concern.

2. **Ask whether it has independent identity.** Can this thing be meaningfully referenced
   from outside the object that owns it? Does the domain recognise it by name? If not, it
   is a value object owned by something that does have identity.

3. **Ask whether the foreign key is a relationship or an ownership.** A `compoundName`
   field on a profile is not a relationship between peers — it is a declaration of
   ownership. The profile belongs to the compound. Ownership means the owned thing is a
   value object or child entity of the owner, not a peer entity with its own repository.

4. **Suspect any `EntityName` that duplicates a parent's identity.** If the "natural key"
   of an object is the name of its parent, the object does not have its own identity. It
   is part of the parent.

5. **Suspect any entity whose repository is never queried independently.** If the only
   access path to a thing is through its parent, the thing is not an independent entity.
   It is a component of the parent.

### The root-cause discipline

6. **When the visible fix is easy, suspect a deeper problem.** The profile → value object
   conversion was obvious once examined. The question is: what made the error possible in
   the first place? If the framework's identity model had not forced a binary choice between
   slug and nothing, the profiles would never have been extracted as entities.

7. **Trace the symptom to the structural gap.** A wrong model is a symptom. The cause is
   usually a missing abstraction, a false dichotomy in the framework, or an assumption that
   was never questioned. Fix the cause, not just the symptom.

8. **Validate the root-cause fix against future scenarios.** The `FactName` abstraction was
   validated not just against the profile case (which it resolved retroactively) but against
   the offline observation deduplication scenario (which it resolved prospectively). A
   root-cause fix should prevent a class of errors, not just one instance.

### When the model is wrong

When first principles analysis reveals that the current model is wrong — a value object
was promoted to an entity, an entity was demoted to a value, an aggregate boundary is in
the wrong place — the correct response is to fix the model immediately. The longer a
wrong model lives in the codebase, the more infrastructure accumulates around it
(repositories, test sources, JSON files, identifier classes), and the more expensive the
correction becomes.

This is not refactoring for aesthetics. A wrong identity model produces wrong constraints,
wrong uniqueness rules, wrong join paths, and wrong query patterns. It will eventually
produce wrong data. Fixing it is not optional.

## Consequences

- Domain modelling starts from "what is this thing?" before any persistence, query, or
  infrastructure consideration
- Value objects that were accidentally promoted to entities are corrected immediately,
  not deferred
- The persistence-first anti-pattern — "it will be a table, therefore it is an entity" —
  is explicitly recognised and guarded against
- Foreign keys that represent ownership (not peer relationships) are a signal that the
  owned thing may be a value object, not an entity
- Visible problems are traced to their root cause before a correction is applied — a local
  fix that leaves the structural gap intact is incomplete
- The cost of correction grows with time; the discipline is to correct early
- Root-cause fixes are validated against both the triggering scenario and at least one
  future scenario to confirm they address a class of errors, not a single instance
