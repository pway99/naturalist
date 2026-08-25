# Organism feature kernel extraction — design memo

**Date:** 2026-08-24
**Status:** design / not scheduled
**Tier:** slice-notes design doc (see [planning tiers](../../CLAUDE.md)). Owns its own scope; the work-tracker points here.

## Motivation

Insects and plants each carry a fully parallel "feature" stack — a diagnostic
trait attached to a taxonomic rank, grouped up the lineage into a read-side
view. The two stacks are near byte-for-byte identical, differing only in the
concrete `EntityId`/`RankName` type parameters. This is the same
per-domain-mirror that motivated the [OrganismObservation](../../MEMORY.md)
and [OrganismImage](../../MEMORY.md) kernel extractions (ADR-020), and it is the
**higher-value** of the two feature-extraction candidates evaluated on
2026-08-24 (the bare `Feature` record on its own is trivial and was *not*
worth extracting alone; the assignment + view + query stack is).

A second, related driver: **feature-vocabulary drift**. Because a feature is
free-text (`@UniqueValue String value`) and the vision-ID workflow mints fresh
descriptive phrases per session, the catalog accumulates semantic
near-duplicates that `@UniqueValue` cannot catch (it only dedupes exact
post-normalize collisions). The shared kernel home is the natural place for a
**similar-feature suggestion** capability that prevents this at ingest. Evidence
of the problem is already sitting in `insect-features.json` (see §5).

## What is actually identical (evidence)

All shapes below are identical **modulo `{ID, FID, RANK, FEATURE}`** — same
components, same `invariants()`, same algorithm.

| Type | Insect | Plant | Divergence |
|---|---|---|---|
| `Feature` record | `InsectFeature(InsectFeatureId, @UniqueValue String)` | `PlantFeature(PlantFeatureId, @UniqueValue String)` | blank-guard idiom only (cosmetic) |
| `FeatureAssignment` record | `(…Id id, InsectFeatureId featureId, InsectRankName rankName, int ordinal)` | `(…Id id, PlantFeatureId featureId, PlantRankName rankName, int ordinal)` | **Jackson permit list** (5 vs 4 subtypes) — see §3 |
| `FeatureView` + nested `RankGroup` | `(InsectRankName subject, List<RankGroup>)` / `RankGroup(InsectRankName, List<InsectFeature>)` | identical shape | **none** (clean fit) |
| `FeatureQueryImpl.findByAncestry` | 2-phase batch → group-by-rank → ordinal-sort | identical body | resolver type; `@DomainService` on plants; insects-only `findByFeature` |
| `FeatureAssignmentRepository` port | `getByRankName / getByRankNames / getByFeatureId` | identical | none |

The query assembly is already N+1-safe on both sides: one
`getByRankNames(ancestry)` batch, one `getByEntityNameSet(featureIds)` batch,
then an in-memory group/sort. **Preserve this batching exactly** in the generic
assembler — do not regress it.

DAG gate passes today: both `insects-api` and `plants-api` already depend on and
import `kernels/observation` (`OrganismImage`/`OrganismObservation`), so a generic
type in that kernel is reachable with no new edge.

## Proposed shared types

Following ADR-020 (bare generic in nested contexts, domain-prefixed standalone
adapters), mirroring the `OrganismImage` precedent:

- `OrganismFeature<ID extends EntityId>` — the trivial record (comes along for
  free; `FeatureView` holds `List<FEATURE>`).
- `OrganismFeatureAssignment<ID extends EntityId, FID extends EntityId, RANK extends RankName>`
- `OrganismFeatureView<RANK extends RankName, FEATURE>` + nested `RankGroup<RANK, FEATURE>`
- A shared assembly helper — either an abstract `FeatureViewAssembler<…>` base or
  a static assemble helper — hosting the `findByAncestry` batching/grouping/sort.
  The per-domain `*FeatureQueryImpl` shrinks to wiring (resolver + accessor).
- `FeatureSimilarity` — the new dedup capability (§5), pure string logic on
  `(id, value)`.

The domains parameterize the generics bare (like `OrganismImage<InsectImageId,
FieldObservationId, InsectRankName>`); no per-domain record subclass required.

## §3 — The one real blocker: the Jackson permit list

`FeatureAssignment.rankName` currently serializes via
`@JsonTypeInfo(use=NAME, include=EXTERNAL_PROPERTY, property="rank")` +
`@JsonSubTypes({…})` — a **compile-time literal permit list** (5 subtypes for
insects, 4 for plants) that cannot be expressed on a generic component.

**Resolution — adopt the kernel's existing RankName serializer.** `OrganismObservation`
and `OrganismImage` already solved this exact problem: their `R subject`/`R parentName`
components use `@JsonSerialize(RankNameSerializer)` / `@JsonDeserialize(RankNameDeserializer)`,
emitting a self-describing `{rank, value}` object that works for **any** `RankName`
with no permit list. `OrganismFeatureAssignment` should use the same pair. This:

- removes the only structural divergence between the two records,
- makes the generic record uniform with its `Organism*` siblings,
- costs a **one-time JSON reformat** of the two `*-feature-assignments.json`
  catalogs (EXTERNAL_PROPERTY sibling `"rank"` → embedded `{rank,value}`) — a
  mechanical, test-guarded migration.

## §5 — Similar-feature suggestion (the new shared logic)

### The problem, concretely

`insect-features.json` (29 features) already contains near-duplicate clusters
that survived `@UniqueValue` because they differ by punctuation or wording:

- `dark (black) pronotum contrasting with red elytra` ⟷ `dark/black pronotum contrasting with red elytra`
- `small head largely concealed beneath pronotum` ⟷ `small, rounded head partially concealed under pronotum`
- `darker midline stripe or suture visible on elytra` ⟷ `faint dark sutural line along midline of elytra`
- `strongly domed, broadly oval body outline` ⟷ `bright reddish-orange, highly domed and oval elytra`

A feature is meant to be a **reusable diagnostic trait shared across taxa**;
vocabulary drift defeats that.

### Where it runs

At **feature ingest** — before the identify-insect workflow (and a future
identify-plant) mints a new feature, it asks: *"do existing features look like
this same trait?"* Surface the top-N candidates with scores; the operator/skill
reuses an existing feature id or deliberately creates a new one. This is a
read-side query/service, not an insert-time hard constraint (a hard constraint
would wrongly block legitimately-distinct-but-lexically-close traits).

### Recommended algorithm — deterministic normalized token-set similarity

Strip punctuation → lowercase → tokenize → (optional light stemming for
plurals/tenses) → **Jaccard / token-overlap score**, return candidates above a
threshold, ranked. Rationale, in line with the "push back on RDBMS-shaped /
heavy designs" principle:

- **Catches the real duplicates.** The punctuation-only pairs collapse to
  identical token sets; the head-concealed and midline pairs score high overlap.
- **Deterministic + dependency-free.** No model, no vector store — important for
  reproducible tests and for staying gate-safe.
- **N+1-safe.** One fetch of candidate features (domain feature sets are small —
  29 today) then an **in-memory scoring pass**. It must NOT fan out a select per
  candidate; score in memory. Take `PageRequest` only if a domain's feature set
  later grows unbounded.

**Explicitly out of scope:** embedding / semantic similarity (synonym matching
like `suture` ⟷ `sutural line`). That is the vision/ML layer's concern, not the
catalog's, and would be over-engineering for a dev-tool dedup. If synonym-level
matching is ever wanted, revisit as a separate slice.

### Reconciling the *existing* duplicates

Once the capability surfaces them, the current insect duplicates should be
reconciled by **merge-and-repoint** (move `FeatureAssignment`s onto the
surviving feature id, then retire the redundant feature) — a deliberate data
migration, **not** a deletion of signal ([pressure tests are signals](../../MEMORY.md)).
Do this as a distinct, reviewed step, not silently inside the extraction.

## Resolved decisions (2026-08-24)

- **Kernel home = `kernels/taxonomy`.** The assignment + view + query are entirely
  rank-keyed (RankName, AncestryResolver, rank-grouped view) and
  `RankAncestry`/`RankName` already live there. `observation` is reserved for
  genuine field records. Package: `com.naturalist.taxonomy`.
- **Assignment keys on `RankName`, NOT `Clade`.** A feature binds to a rank, not a
  clade node. Evidence: (1) `Clade` is a slug-backed `sealed interface`/`record`
  with value-equality — it has **no** `CladeName`/`EntityName`/`EntityId` to serve
  as a persisted FK; (2) the clade-trait axis already exists as a *different*
  mechanism — a typed, code-declared, inherited `Trait` resolved by
  `Function<Clade, Set<Trait>>` via `CladeTraversal.findTrait` (e.g.
  `MetabolyTrait(Metaboly)` on `Holometabola`) — categorically unlike a free-text,
  per-taxon, ordinal, persisted `FeatureAssignment`; (3) no shared supertype unites
  `RankName` and `Clade`. `complete metamorphosis` appearing as a free-text insect
  *feature* is data drift (Slice B territory), not a reason to re-key on clades. If
  a feature ever needs supra-ordinal placement, the established pattern is
  `placedIn` + walk-up via `CladeRanks` — the key stays `RankName`.

## Open decisions (need a call before implementation)
2. **`@DomainService` discrepancy.** `PlantFeatureQueryImpl` carries
   `@DomainService`; `InsectFeatureQueryImpl` does not. There is a known landmine
   here — [factory-backed queries must not carry `@DomainService`](../../MEMORY.md)
   (breaks app-context startup). Reconcile to one convention during extraction and
   verify the console `@SpringBootTest`s still start.
3. **Reverse lookup in the generic port.** Insects has
   `findByFeature(featureId) → Set<RankName>`; plants does not (though its repo
   already has `getByFeatureId`). **Recommendation:** include it in the generic
   contract — plants gains it for free and it is a natural read.
4. **Artifact-set normalization.** Today the two stacks are *not* symmetric:
   insects has a core `FeatureAssignmentCommandImpl` (plants none); plants has an
   api-level record test + three repo-test classes (insects none). Normalize as
   part of the extraction so both domains end symmetric.
5. **`getByEntityNameSet` naming.** Both query impls call
   `featureRepository.getByEntityNameSet(...)` with a `Set<EntityId>` despite the
   "Name" in the method name. Minor; fix opportunistically, not required.

## Proposed slices

Kept small and independently reviewable ([PR size](../../domains/CLAUDE.md)):

- **Slice 0 — cleanup (trivial, unblocks nothing but is free).** Delete the dead
  `FeatureCollection` in both `*EntityCollections` (zero references repo-wide).
  *Confirm the plants one first — memory flags it was "kept at user request."*
- **Slice A — type + query extraction.** Detailed plan:
  [2026-08-24-organism-feature-slice-a-plan.md](2026-08-24-organism-feature-slice-a-plan.md).
  Introduce `OrganismFeatureAssignment` (with the kernel RankName serializer, §3),
  `OrganismFeatureView`/`RankGroup`, and a pure `FeatureViewAssembler` in
  `kernels/taxonomy`. Reparameterize both domains bare (as they already consume
  `OrganismImage<…>`); migrate the two assignment JSON catalogs to the `{rank,value}`
  form. **The bare `OrganismFeature` record is NOT extracted here** — the view carries
  the domain feature via a free `FEATURE` type param, so the trivial (low-value)
  feature-record unification stays deferred. Normalize the artifact asymmetries
  (open decision 4). Land with `mvn verify` + the `rewrite:dryRun` gate green.
- **Slice B — similar-feature capability.** Add `FeatureSimilarity` + a
  read-side query, wire the identify-* workflow(s) to consult it at ingest.
- **Slice C — reconcile existing insect duplicates** via merge-and-repoint,
  as a reviewed data migration.

Slice A is the mechanical unification; B is the new capability the shared home
enables; C is catalog hygiene that B surfaces. A must precede B (shared home
first); C follows B.
