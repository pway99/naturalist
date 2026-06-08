# Insect Aggregate — Bounded-Context Review

**Date:** 2026-06-03
**Subject:** Critical pre-identification review of the `Insect` aggregate, the
rank-rooted `InsectAggregate` permits, and the boundary between them.
**Verdict:** The structure is competently built and the invariant machinery is
clever, but the aggregate is **mis-cast**. It is, in substance, a *read model /
loader result* for the species-detail page, dressed as a mutable
identification-workflow aggregate it will not actually be. Several invariants
defend against states the real consumer cannot produce, and the central
load-bearing justification (the deferred clade-conformance invariant) rests on
a typed reference that does not exist on `LifeStage`. Better to correct this now
than to let the identification module inherit it.

This memo is honest critique by request. Where the model is right I say so;
where I think it is wrong I say so plainly, including where that undoes Phase 1.

---

## Ground truth gathered before forming an opinion

- `Insect` has **zero consumers** — no file imports it (Phase 1 shipped it in
  isolation, as planned).
- The sealed `InsectAggregate` and its four rank permits have a factory
  (`InsectAggregateFactory`) and query (`InsectAggregateQueryImpl`) in
  `insects-core`, but **no UI consumer** — `InsectsController` still does raw
  `getByName(...).orElseThrow()` chain-walks (`InsectsController.java:408-443`,
  `:457-478`). The sealed type's own plan admits this: "It has no consumers."
- `aggregateOrNull(...)`, added 3 commits ago (`7e9161f`), has **zero
  consumers** — `Insect.invariants()` uses `whenNotNull(...) + aggregate(...)`
  instead (`Insect.java:174-196`).
- `InsectAggregateFactory.buildByName` fetches images by
  `imageQuery.forParentName(name)` — so an image's `parentName` match is
  **tautological at assembly** (`InsectAggregateFactory.java:60-77`). The same
  will be true of the unbuilt Phase 2 `Insect` factory, which walks *up* the FK
  chain to load ancestors.
- `LifeStage` (and every permit, e.g. `EggStage.java:14-22`) carries **no
  typed `parentName` FK**. Parent linkage is encoded only as a substring of the
  composite `LifeStageName` slug; `LifeStageName.parentSlug()` returns a raw
  `String` and its own javadoc states "the slug carries no intrinsic rank
  discrimination" (`LifeStageName.java`). This directly contradicts
  `Insect.java:28-29` ("each carries its own `parentName`").
- The identification roadmap (`docs/plans/identification.md:281-345`) designs
  its **own** aggregate, `InsectIdentification` (a session: append-only steps,
  IN_PROGRESS/CONCLUDED/ABANDONED, promotion to a durable `InsectObservation`).
  It nowhere references `Insect.with*`.

---

## Q1 — Does `Insect` reach beyond its bounded context?

**Yes. It fuses two aggregates with different identities into one record.**

`Insect` is documented as "everything known about an insect at Oak Vista, at
whatever identification depth has been reached" (`Insect.java:13-15`). That
sentence hides a conflation of two distinct concepts:

1. **A taxon-catalog view** — what we know about *Battus philenor as a
   catalogued taxon*: its rank chain and its life stages. Identity = the rank
   name. Exactly one per taxon. Stable, shared across all naturalists.
2. **A sighting / occurrence** — *I photographed an organism and am working out
   what it is*. Identity = the observation(s) / a session. Many per taxon.
   Per-naturalist, ephemeral until concluded.

These have **different aggregate identities**, which is precisely why `Insect`
ends up with none ("sum-of-parts, not persisted, no aggregate-level
identifier," `Insect.java:32-36`). An aggregate without an identity is a sign
the boundary is wrong, not a clever optimisation.

- **Observations are sightings — a separate concept, not part of the taxon.**
  The `observations` field is the tell. The plan itself says the catalog-read
  case loads "the rank's images" while the workflow case loads "the
  naturalist's gathered photos" (`insect-aggregate.md:139-142`,
  `Insect.java:19-21`) — two semantically different collections sharing one
  Java type. `InsectImage` is already an `Entity` with its own identity and
  repository. A taxon view should *reference / expose a gallery of*
  observations, not *contain* them as a defining constituent. An
  observations-only `Insect` (the `empty()`→add-photos state) is simply a
  sighting with no taxon — reinforcing that this field belongs to a different
  aggregate.

- **Life stages are taxon-scoped catalog data — they belong with the rank
  chain.** `LifeStageName` is `{rank-slug}-{kind}`; stages are keyed to a
  single rank. So composing them into a taxon view is correct *in principle*.
  But the plan's claim that one `Insect`'s `lifeStages` "potentially span ranks"
  (`Insect.java:27-29`) is itself a symptom of the Q1 conflation: in the
  catalog-read case for *Battus philenor* the stages are all species-rank
  (`battus-philenor-egg`, …); a genus-rank stage (`battus-egg`) belongs to the
  *genus's* taxon view, a different aggregate. Mixing them in one record blurs
  "the species taxon" with "the genus taxon."

**Bottom line:** the coherent decomposition is (a) a **taxon catalog view**
(rank chain + life stages, identity = rank name) and (b) **observations as
referenced occurrence data**, not a constituent that defines the aggregate.

---

## Q2 — Are the rank-rooted aggregates real aggregates, or query tuples?

**They are read-model projections mislabeled as `Aggregate`, and they have no
consumer.**

Each permit holds one rank entity + an `ImageCollection`, and its
`invariants()` only descends into those two parts — no cross-entity invariant
(`InsectOrderAggregate.java:31-36`, `InsectSpeciesAggregate.java:61-66`). The
images are not *owned*: `InsectImage` is its own `Entity` in its own repository,
referenced by `parentName`. The sealed type's own javadoc concedes that
referential integrity between `InsectImage.parentName` and the root name is "the
assembly factory's responsibility… tautological at construction time"
(`InsectAggregate.java:39-41`) — i.e. **the aggregate enforces nothing across
its parts.**

A DDD aggregate is a consistency boundary with invariants spanning a cluster of
objects, mutated as a unit. These bundle (rank entity, gallery) for *display*.
That is a projection / read model — a query-result tuple — not an aggregate.

They carry the `Aggregate` marker only because the kernel offers no "read
model" slot (the five interfaces are NamedEntity / Entity / Aggregate /
ValueObject / BehavioralCollection). They were "defined, factored, queried,
tested" ahead of their intended consumer (`Insect`), which itself has no
consumer yet — so today they are **effectively dead code reachable only through
`InsectAggregateQuery`**, which the console does not call.

**What they should be:** read models. Pragmatically, either (i) keep them but
stop calling them consistency boundaries and stop adding cross-rank invariant
machinery that pretends they are, or (ii) delete them if the reworked taxon
view (Q1) makes the (rank, gallery) tuple unnecessary. See "kernel gap" in the
final section.

---

## Q3 — Is `Insect.invariants()` the right amount of work?

**Too much, and partly defending against states the real loader cannot
produce.** Three layers of redundancy:

1. **Redundant ancestor re-descents.** Each present rank re-walks every
   ancestor's full invariant graph: `order` is descended as `"order"`,
   `"family:order"`, `"genus:order"`, and `"species:order"` when the chain is
   full (`Insect.java:174-196`) — up to 1+2+3+4 = 10 descents for a 4-rank
   chain, most of them re-validating the same object. The
   "diagnostic-traceability-per-perspective" rationale (`Insect.java:166-173`)
   produces **duplicate violations under different path prefixes for one
   underlying problem** — that is noise, not signal. Keep one descent per
   present rank.

2. **The 5 cross-rank FK checks are tautological under the actual loader.** The
   Phase 2 factory will load ancestors by *following* the FKs (read
   `species.genusName` → fetch that genus, `genus.familyName` → fetch that
   family). So `family.name == species.familyName` is guaranteed *by
   construction* — exactly the "tautological at construction time" situation the
   sealed type already documents for image `parentName`
   (`InsectAggregate.java:39-41`). In the catalog-read path these five checks
   can never fire. They only have teeth where some *other* path assembles ranks
   independently and the species record's denormalized FK has drifted — i.e.
   they are really **write-time data-integrity checks for denormalization
   drift** (Q5), hoisted onto every read.

3. **Two of the five only exist because of denormalization.**
   `speciesBelongsToFamily` and `genusBelongsToOrder` are skip-level checks that
   are necessary *only* because `InsectSpecies` stores `familyName` and
   `InsectGenus` stores `orderName` (Q5). Remove the denormalization and these
   two checks vanish.

**Keepers:** required-collections (`Insect.java:164-165`), monotonic-fill /
ancestor-presence, and a single descent per present rank. **Reconsider:** the FK
checks — they are the aggregate doubling up on a guarantee the loader (or the
write boundary) already owns.

---

## Q4 — `belongsTo*` on both the entity and the aggregate: right layering?

**The aggregate-level layer is redundant glue with a trap baked in.**

- Entity level (`InsectSpecies.belongsToGenus(InsectGenusName)`,
  `InsectSpecies.java:111-119`): strict typed-FK equality. This is the real,
  reusable predicate.
- Aggregate level
  (`InsectSpeciesAggregate.belongsToGenus(@Nullable InsectGenusAggregate)`,
  `InsectSpeciesAggregate.java:48-59`): unwraps the aggregate to its name and
  is **null-tolerant — `belongsToGenus(null)` returns `true`**.

The null tolerance exists *solely* to let `Insect.invariants()` read as a
one-liner inside `whenNotNull` without firing a redundant violation when the
ancestor is absent. But the `whenNotNull` guard already gates on presence; the
null-as-true semantics double-guard the same condition — and "belongs to
nothing = true" is a genuine footgun for any future caller who reads the method
name at face value.

**Recommendation:** drop the aggregate-level null-tolerant variants. Compose the
entity predicate directly inside the `whenNotNull(child, …)` blocks, where both
child and ancestor are known non-null (e.g.
`isTrue(species.species().belongsToGenus(genus.name()), "speciesBelongsToGenus")`).
That removes a layer and the surprising semantics in one move.

---

## Q5 — Denormalized FKs on `InsectSpecies` (`genusName` AND `familyName`)

**Weakly load-bearing; it buys a lookup-hop the in-memory catalog doesn't need
and pays for it with the drift class that drives 2 of the 5 invariants.**

Each rank denormalizes its *full* ancestor chain, not just its immediate parent:
`InsectSpecies` carries `genusName` + `familyName` (`InsectSpecies.java:83-100`);
`InsectGenus` carries `familyName` + `orderName` (`InsectGenus.java:36-44`).

- **What it buys:** the console can jump straight from a species to its family
  without the genus hop — `detail` does
  `families().getByName(s.familyName())` (`InsectsController.java:417`) and the
  species list groups by `familyName`/`genusName` directly
  (`InsectsController.java:220-223`). One in-memory map lookup saved per render.
- **What it costs:** a normalization violation. `family` is *derivable* from
  `genus.familyName`; storing `species.familyName` independently creates the
  drift opportunity that then requires `speciesBelongsToFamily` /
  `genusBelongsToOrder` to police it. For a 16-species, hand-curated JSON
  catalog the read savings are negligible and the hand-edit drift risk is the
  larger concern.

This is a borderline call, but given the project's stated preference to push
back on RDBMS-shaped designs and prefer the simpler model, I lean: **the
grandparent denormalization (`species.familyName`, `genus.orderName`) is a
premature optimization.** The immediate-parent FKs (`species.genusName`,
`genus.familyName`, `family.orderName`) are the real model; the grandparent FKs
should be derived, not stored. The Q3 unease about "too many FK invariants" is
itself the signal that the denormalization underneath them is the thing to
question.

**Caveat:** reversing it is the most invasive change in this memo (entity
records, JSON catalogs, repository contracts, console reads) and is *not*
strictly required before identification starts. If kept, keep it *deliberately*
— as a documented read-model denormalization whose guard is the FK invariant —
rather than leaving the intent ambiguous. Do not leave it half-justified.

---

## Q6 — Does the design serve both use cases?

**It serves catalog-read (a) and carries speculative, probably-wrong machinery
for workflow (b).**

- **(a) Catalog read:** well served *in substance*. Strip the mutators and the
  elaborate FK invariants and what remains is a loader that returns
  `(order, family, genus, species, images, stages)` and replaces the
  controller's `getByName().orElseThrow()` chain-walks
  (`InsectsController.java:415-422`). That is a legitimately useful read model.
  The aggregate machinery around it is mostly overhead.

- **(b) Identification workflow:** the `with*` mutators and the
  "monotonic refinement, identification refines and never retreats" framing
  (`insect-aggregate.md:156-162`) model identification as *fill in ranks on an
  `Insect`*. **The actual identification roadmap models it completely
  differently:** a separate `InsectIdentification` session aggregate
  (append-only steps, state machine) that, on conclusion, *promotes* to a
  durable `InsectObservation` record (`identification.md:281-345`). The session
  is the mutable thing — its steps, choices, and scope — not a progressively
  filled rank chain. Identification narrowing genus→species **creates a species
  record**, it does not call `Insect.withSpecies(...)`.

So the design is pulled toward (a) while carrying (b) machinery the (b) plan
won't use — and that speculative machinery is the *source* of much of the Q3/Q4
complexity (null tolerance to support progressive fill; FK checks to catch
user-assembled mismatches). This is building scaffolding for a building that
will be poured on a different foundation.

---

## Mistakes to fix before identification work starts

1. **Decide what `Insect` is, and stop being two things.** It is a taxon-catalog
   **read model**, not a sighting and not the identification aggregate. Rename
   in intent, drop the workflow framing. (Drives 2–6.)
2. **Get observations out of the taxon view** (Q1). Observations are occurrence
   data with their own identity; the taxon view references/exposes a gallery.
   Settle this before identification introduces real `InsectObservation`
   entities, or identification inherits the "is `Insect` a taxon or a sighting?"
   ambiguity.
3. **Drop the `with*` / progressive-refinement machinery** unless a concrete
   consumer demands it (Q6) — and `identification.md` says it won't.
4. **`LifeStage` has no typed parent FK** (the single most important discovery).
   The deferred Phase 3 clade-conformance invariant — *the* stated load-bearing
   reason the aggregate exists (`insect-aggregate.md:401-406`) — must verify
   each `LifeStage` belongs to one of the aggregate's ranks. But `LifeStage`
   exposes only a rank-ambiguous `String parentSlug()`
   (`LifeStageName.java`); the aggregate **cannot type-safely confirm its life
   stages belong to it.** Either add a typed `parentName : InsectRankName` to
   `LifeStage`, or accept that the conformance invariant can only ever be a
   stringly-typed best-effort check. Resolve this before betting the aggregate's
   justification on it. Also fix the false claim at `Insect.java:28-29`.
5. **Trim the invariants** to monotonic-fill + one descent per rank; reconsider
   the 5 FK checks alongside the Q5 denormalization decision (Q3/Q4).
6. **Resolve the rank aggregates' fate** (Q2): keep as honest read models or
   delete. Don't let identification build on a "consistency boundary" that
   enforces nothing and currently has no consumer.

---

## Recommended refactors (effort)

| # | Refactor | Effort |
|---|----------|--------|
| R1 | Remove redundant ancestor re-descents in `Insect.invariants()` (one descent per present rank) | **Small** |
| R2 | Drop aggregate-level null-tolerant `belongsTo*`; inline entity predicates inside `whenNotNull` blocks | **Small** |
| R3 | Delete the unused `aggregateOrNull` kernel constraint (zero consumers) | **Small** |
| R4 | Reframe `Insect` as a read model; remove `with*` mutators (and the tests that only exercise them) | **Small–Medium** |
| R5 | Split `observations` out of the taxon view; expose a gallery via query instead of as a defining field | **Medium** |
| R6 | Fix `LifeStage` parent reference — add typed `parentName : InsectRankName` (enables a real conformance invariant) | **Medium** |
| R7 | Decide rank-aggregate fate; if delete, ripple through factory/query/tests | **Small to assess, Medium to delete** |
| R8 | Reverse grandparent denormalization (`species.familyName`, `genus.orderName`) → derive from immediate parent | **Medium–Large** (records, JSON, contracts, console) |

R1–R4 are cheap, high-signal, and safe to do immediately (no consumers to
break). R5–R8 are design decisions that should be made *before* identification
lands but sequenced deliberately. R8 is the only one large enough to defer if
time-boxed — but make the keep/reverse call explicitly rather than by default.

---

## Things the six questions don't cover

- **The kernel has no read-model / projection type, and three things now want
  one.** Both the rank aggregates *and* `Insect` are using the `Aggregate`
  marker to mean "bundle of constituents assembled for reading, owning nothing,
  no identity." That is a recurring shape. Rather than keep stretching
  `Aggregate` (whose kernel definition is "consistency boundary, owns child
  entities"), consider whether a first-class `ReadModel`/`Projection`
  `Observable` is warranted. This is a kernel conversation, not an insects one —
  flagging because it will recur in plants and every other organism domain.
- **`Insect implements Aggregate` is in direct tension with the kernel
  definition.** `domains/CLAUDE.md:13-15` defines Aggregate as "consistency
  boundary, owns child entities and value objects." `Insect` explicitly owns
  nothing and is not persisted. It satisfies the *marker* but not the
  *contract*. Same critique as the rank aggregates; same fix (read-model type).
- **`empty()` + add-photos is a sighting masquerading as a taxon.**
  `Insect.empty()` then `withObservations(...)` with no rank is, conceptually, a
  brand-new sighting. That this state is even constructible is further evidence
  the observation concept doesn't belong inside the taxon aggregate (Q1).
- **Test suite locks in the current shape.** `InsectTest` is thorough but
  ~half its cases (`with*` mutators, the per-FK violation matrix) test machinery
  this memo recommends removing. Expect the test count to *drop*, not grow, when
  R1/R2/R4 land — that's correct, not a regression.
- **Verification scope:** this is a static read of the model and plans. I did
  not run the build (per project convention, the user runs Maven). Claims about
  "zero consumers" and "tautological FK" are from grep + reading the factory,
  not from a failing/passing test.
