# `Plant` Read Model — Full Taxon Object Graph (Design)

**Status:** design of record. Each chunk below becomes its own implementation plan.

**Goal.** Give plants a composed `Plant` **ReadModel** — the whole taxon object graph
presented by the query — so every rank-page handler resolves *one* `Plant` and reads the
graph off it (`plant.features()`, `plant.children()`, `plant.role()`, …), instead of the
controller hand-collating a dozen separate query results into loose model attributes. This
is the plants parallel of the insects rank-page read model (`Insect` composed by
`InsectFactory`, exposed via `InsectQuery.getByName`).

**Why.** The plants console currently collates the object graph *in the controller*: each
rank handler fires the rank query, the child-rank query, the ecological-role lookup, the
breadcrumb/clade walks, and (on species) cultivars/programs/constituents, then adds each as
a discrete `model.addAttribute(...)`. That controller-collation is the anti-pattern this
effort removes. Insects already solved it: the DAG is assembled once, in a factory, behind
a single read port. Plants moves to match (`domains/plants/CLAUDE.md` rule 1: insects is the
reference, plants moves).

**Reference implementation (mirror these):**
`domains/insects/insects-api/.../Insect.java`, `InsectTaxonView.java` (+ `InsectOrderView`/
`InsectFamilyView`/`InsectGenusView`/`InsectSpeciesView`), `InsectFeatureView.java`;
`domains/insects/insects-core/.../InsectFactory.java`, `InsectAncestryResolver.java`,
`InsectFeatureQueryImpl.java`, `InsectQueryImpl.java`. The insects rank-page read model was
itself built in stages (features → role+children → retire dead cruft); see
`docs/plans/2026-08-20-insect-rank-page-read-model-design.md`.

**Tech stack.** Java 21 records + sealed types, Jackson 2.19, JUnit 5 + AssertJ, the
Observer framework, `NaturalistDatabase` + repository mocks seeded from JSON, JTE templates.

---

## Target shape

A `Plant` `ReadModel` (identity-less, immutable, `with*` per component, `empty()`
starting point — the `Insect` mould) composed by a package-private `PlantFactory` in
`plants-core`, exposed via:

```java
// PlantQuery
Optional<Plant> getByName(PlantRankName rankName);
```

`getByName` resolves the subject rank, walks its ancestry to populate the rank-chain spine,
and composes every attached axis. Each rank handler in `PlantsController` calls it once.

### `Plant` components

| Component | Type | Source query | Notes |
|---|---|---|---|
| `order` | `@Nullable PlantOrderView` | `orders().getByName` | ancestry spine |
| `family` | `@Nullable PlantFamilyView` | `families().getByName` | ancestry spine |
| `genus` | `@Nullable PlantGenusView` | `genera().getByName` | ancestry spine |
| `species` | `@Nullable PlantSpeciesView` | `species().getByName` | ancestry spine |
| `children` | `List<PlantTaxonView>` (non-null) | `families().forOrderName` / `genera().forFamilyName` / `species().forGenusName` | direct sub-taxa as views carrying their galleries; empty on species pages |
| `features` | `@Nullable PlantFeatureView` | new ancestry query | ancestry-inherited field marks |
| `role` | `@Nullable PlantEcologicalRole` | `ecologicalRoles().forPlantName` | plant analog of the insect functional role |
| `images` | `ImageCollection` | `images().forParentName` | the taxon's own gallery |
| `cultivars` | `CultivarCollection` | `CultivarQuery.forPlantName` | **species-only** (`Cultivar.plantName → PlantSpecies`); empty above species |
| `programs` | `PlantProgramCollection` | `PlantProgramQuery.forPlantName` | rank-attached (`plantName` is `PlantRankName`); composed at the subject rank |
| `constituents` | `PhytochemicalConstituentCollection` | `PhytochemicalConstituentQuery.forPlantName` | rank-attached; composed at the subject rank |

The rank chain is four nullable view slots (not one "subject" slot), exactly as `Insect`
carries `order`/`family`/`genus`/`species`. On an order page only `order` is set; on a
species page all four are set (the full ancestry). The subject is "the deepest non-null
rung."

**Out of `Plant`:** seed lineages (the species page never shows them; they belong to the
cultivar detail page — a future `Cultivar` read model, if ever, is a separate effort).
Breadcrumb / clade-trail presentation stays in the controller (it is viewer concern, not
graph data — same call the insects controller makes); the controller can derive it from
the resolved `Plant`'s rank chain rather than re-walking parents.

### New api types required (the "start at the api" layer)

1. **`PlantTaxonView`** — sealed interface, permits `PlantOrderView`, `PlantFamilyView`,
   `PlantGenusView`, `PlantSpeciesView`. Each permit: a record over `(entity,
   ImageCollection images)` with `name()` returning the entity's typed rank name, static
   `of(entity, ImageCollection)` and `of(entity)` (empty images). Mirror `InsectTaxonView`
   **as it stands after insects Stage 3** — i.e. **no `features()` slot** on the permit
   (that vestigial slot was removed on the insects side; do not reintroduce it).
2. **`PlantFeatureView`** — record `(PlantRankName subject, List<RankGroup> groups)` with
   nested `RankGroup(PlantRankName rank, List<PlantFeature> features)` (a `ValueObject`),
   ancestor-first, ordinal-ordered within a group. Mirror `InsectFeatureView`.
3. **`Plant`** — the `ReadModel` above.
4. **`PlantQuery.getByName(PlantRankName) → Optional<Plant>`**.
5. **`PlantQuery.FeatureQuery.findByRankName(PlantRankName) → PlantFeatureView`** — the
   ancestry-walking view producer, *replacing* the S3 direct-rank
   `forRankName(...) → FeatureCollection` (which has zero consumers).
6. **`PlantQuery.EcologicalRoleQuery.getByPlantNames(Set<PlantRankName>) → EcologicalRoleCollection`**
   — the batched sibling of `forPlantName`; enables role badges on child cards without an
   N+1 (the species-list page already does the equivalent by streaming all roles into a
   map).

### Composition & batching discipline

- `PlantFactory` composes a *single* subject `Plant`, so its per-axis attachments
  (`role`, `images`, `cultivars`, `programs`, `constituents`) are one call each — not a
  fan-out.
- The **ancestry feature query** batches: resolve the ancestor-first ancestry set once,
  one `assignmentRepository.getByRankNames(ancestry)`, one
  `featureRepository.getByEntityNameSet(allIds)`, group by rank (the `InsectFeatureQueryImpl`
  shape). Never a per-rank fetch.
- **`children` fan-out must batch** (`domains/CLAUDE.md` "fan-out must batch"): building
  child views needs each child's gallery, and — once role badges land — each child's role.
  Galleries use the existing `images().forParentName` per child *unless* that proves an
  N+1 at catalog scale, in which case add a batched `forParentNames(Set)`; role badges use
  the new `getByPlantNames(Set)`. Resolve the child set's attachments in batched calls, not
  a loop of single lookups. (Confirm the insects child-helper batching in `InsectFactory`
  and match it.)

### `PlantAncestryResolver` (new, `plants-core`)

Near-mechanical mirror of `InsectAncestryResolver`, wrapping the shipped `RankAncestry`
kernel (`kernels/taxonomy`). Its only real content is `parentOf`, a switch over the **four**
`PlantRankName` permits (no subspecies):

```java
private Optional<PlantRankName> parentOf(PlantRankName rankName) {
    return switch (rankName) {
        case PlantSpeciesName s -> speciesQuery.getByName(s).map(PlantSpecies::genusName);
        case PlantGenusName g   -> genusQuery.getByName(g).map(PlantGenus::familyName);
        case PlantFamilyName f  -> familyQuery.getByName(f).map(PlantFamily::orderName);
        case PlantOrderName o   -> Optional.empty();
    };
}
```

Shared by the feature query now, and reusable by any later cross-rank inheritance (a
citation analog, say), exactly as the insects resolver is shared by citations + features.

---

## Chunk sequence

Each chunk lands green and reviewable; api-first within each. A chunk that must land red
per `domains/plants/CLAUDE.md` rule 4 names the red in its commit.

### Chunk 1 — skeleton (`Plant` + rank-chain views + `getByName`)  ·  *plan: [chunk1-skeleton-plan.md](2026-08-20-plant-read-model-chunk1-skeleton-plan.md)*
- **api:** `PlantTaxonView` sealed + 4 permits; `Plant` ReadModel carrying **only** the
  rank chain (`order`/`family`/`genus`/`species`) + `empty()`/`with*`/`invariants()`;
  `PlantQuery.getByName`.
- **core:** `PlantFactory` composing the ancestry spine (subject + walk up the FK chain);
  wire `getByName` in `PlantQueryImpl` (factory built internally — `PlantsTestContext`
  unchanged).
- **No console changes.** The controller switch is deferred to Chunk 2 (the first chunk
  that actually needs to read off `Plant`). This keeps Chunk 1 a pure api/core seam,
  fully tested via view/read-model/factory unit tests with no user-visible risk.
- **No features.** This chunk is the seam every later chunk folds into.

### Chunk 2 — features (the original request) + controller switch
- `PlantFeatureView` (+ `RankGroup`); `PlantAncestryResolver`; `PlantFeatureQueryImpl`
  upgraded to `findByRankName(PlantRankName) → PlantFeatureView` (ancestry walk, batched),
  replacing `forRankName`; `Plant.withFeatures(...)` composed in `PlantFactory`.
- **Controller switches** the four rank handlers to `plant = plantQuery.getByName(rankName)`
  and reads the rank record + `plant.features()` off it (other attributes still collated
  the old way until their chunk).
- `plants/features.jte` fragment (mirror `insects/features.jte`), included on all four rank
  detail templates. Feature values render as **plain text** (no glossary linker — plants has
  none; a linker is a separate future feature).

### Chunk 3 — children
- Fold the child-rank lists into `plant.children()` as `PlantTaxonView` permits carrying
  their galleries (batched); rewrite the child-card sections of `orders/detail.jte`,
  `families/detail.jte`, `genera/detail.jte` to iterate the permits; delete the child-entity
  queries + `families`/`genera`/`species` model attributes from the handlers.

### Chunk 4 — role + images
- `EcologicalRoleQuery.getByPlantNames(Set)` (batched); `Plant.withRole(...)` +
  `Plant.withImages(...)` composed; handlers read `plant.role()`; child cards can show role
  badges via the batched method.

### Chunk 5 — species extras
- `PlantFactory` composes `cultivars`/`programs`/`constituents` onto the species-rank
  `Plant` (`PlantFactory` gains `CultivarQuery`, `PlantProgramQuery`,
  `PhytochemicalConstituentQuery` deps); species `detail.jte` reads them off `Plant`;
  drop the corresponding controller lookups.

### Chunk 6 — cleanup
- Retire any dead direct-rank query surface and leftover controller collation; grep-confirm
  no dangling references. Behavior-preserving.

---

## Cross-cutting constraints

- **Record-arity ripple.** `Plant` grows one chunk at a time; every `new Plant(...)` /
  `Plant.empty()` / `with*` site updates on each growth. Grep the whole repo per change
  (`grep -rn "new Plant(" --include=*.java`) — do not trust a plan's file list
  (`feedback_record_arity_ripple`).
- **Insects holds still.** This is a plants-only alignment; ideas that would improve
  insects are noted, not acted on (`domains/plants/CLAUDE.md` rule 1).
- **Views are 2-arg from birth.** `PlantTaxonView` permits carry `(entity, images)` only —
  the insects `features()` permit slot was already retired; plants never grows it.
- **PR size.** One chunk per PR, ≤ ~400 lines meaningful diff where feasible (ADR-019); the
  skeleton chunk may run larger because it stands up the view types + read model at once.

## Open items / confirm during implementation
- Confirm the exact `Insect` `invariants()` descent methods for a `ReadModel` (the
  `.readModel(...)` / `.whenNotNull(...)` calls) and mirror them on `Plant`.
- Confirm whether child galleries need a batched `images().forParentNames(Set)` at current
  catalog scale, or whether per-child `forParentName` is acceptable for the seed data while
  a batched sibling is filed as follow-up (must not silently ship an N+1 — name it if
  deferred).
- Confirm `PlantSpecies::genusName`, `PlantGenus::familyName`, `PlantFamily::orderName`
  accessor names for `parentOf` (the controller breadcrumb walk already uses them).
