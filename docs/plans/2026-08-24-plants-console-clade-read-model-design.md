# Plants console: source the clade trail from the library read model

**Date:** 2026-08-24
**Status:** Design — approved, pending spec review
**Scope:** `domains/plants/plants-console` only. Insects holds still.

## Goal

Wire `plants-console` onto the organism-agnostic clade read model that already
lives in the `library` domain (`CladeQuery` / `CladeView` / `CladeStep`),
retiring its direct `kernels/clades` `CladeTraversal` calls for the phylogenetic
**Tree-of-Life trail**. Insects already sources its trail this way; plants is the
last console re-deriving the lineage in its own controller.

This is a **re-sourcing refactor**: the rendered trail is unchanged (same
root→subject lineage, same dropdowns, same routing). Only its *source* moves from
the kernel to the shared read model. No new user-facing behavior.

### Why

The broader objective is to prepare the modulith for the remaining organism
modules (arachnids, fungi, molluscs, vertebrates, …). Every new console will
carry a clade trail. If insects and plants disagree on how the trail is sourced,
each new module inherits a coin-flip. Putting plants on the same read model insects
uses makes "query `library.CladeQuery`" the one blessed path a future module copies,
and it settles the surface ahead of any eventual shared-console extraction.

Aligned with `plants/CLAUDE.md` rule 3 ("align first, abstract second"): this
narrows the insects/plants gap **without** introducing a shared abstraction yet.

## Non-goals (explicitly out of scope)

- **No new UX.** The "not yet placed" gap affordance and the ancestor-intro panels
  that insects has and plants lacks are **not** added here.
- **No shared-console module / no kernel extraction.** Plants gains a dependency on
  the existing `library-api`; nothing new is created.
- **Insects is untouched.** It is the reference and holds still.
- **No anchor/override machinery.** Plants does not get insects'
  `InsectCladeAnchors` (curated override map + floor). Plants clades are
  supra-ordinal, so the trail slug comes straight off the order's `placedIn`.

## Justified per-domain differences — document, do not "fix"

These are correct consequences of plants' order-only clade placement, not drift.
They are recorded in `plants/CLAUDE.md` as intentional so a future module (and any
later extraction) reads them as design:

1. **Clade placement is order-only** (supra-ordinal), so every rank resolves its
   clade by walking up to its order. Insects places at every rank.
2. **Kingdom anchoring is a single hand-anchored `Plantae` "Kingdom" segment** —
   plants has no Class rank; insects derives Kingdom/Phylum/Class from clade ancestry.
3. **The clade-detail page groups Orders only** — nothing is placed below Order —
   whereas insects groups Orders/Families/Genera/Species.

## The read model (already organism-agnostic)

`domains/library/library-api`:

```java
public interface CladeQuery {
    Optional<CladeView> getBySlug(String slug);
    CladeTreeNode tree();
}
public record CladeView(CladeStep subject, List<CladeStep> ancestry, List<CladeStep> children) …
public record CladeStep(String cladeSlug, String displayName, Optional<LinealRank> rank) …
```

Keyed by clade slug (`String`), carrying `LinealRank`. Nothing insect-specific.
Insects builds its trail as `ancestry ++ subject` (root→subject). Plants does the same.

## Design

### Controller (`PlantsController`)

Add the dependency and one private helper; rewrite the three trail producers to use it.

```java
private final CladeQuery cladeQuery;   // new constructor field

/** root→subject clade steps for a clade, off the shared read model. */
private List<CladeStep> stepsFor(Clade clade) {
    CladeView view = cladeQuery.getBySlug(clade.slug()).orElseThrow(
        () -> new IllegalStateException("no clade view for slug " + clade.slug()));
    var steps = new ArrayList<>(view.ancestry());
    steps.add(view.subject());
    return steps;
}
```

Rewrites (each keeps its existing guard logic; only the ancestry tail changes):

- **`cladeTrailFor(Plant)`** → returns `List<CladeStep>`. Keep the
  `plant.order()==null` and `placedIn==null` guards returning `List.of()`; replace
  `CladeTraversal.ancestry(placedIn).reversed()` with `stepsFor(placedIn)`.
- **`catalogCladeRoot()`** → returns `List<CladeStep>`. Keep the
  `lowestCommonAncestor(...)` computation (pure kernel — stays). Replace the two
  return sites: `null` shared-ancestor → `stepsFor(new Plantae())`; otherwise
  `stepsFor(sharedAncestor)`.
- **`cladeDetail` handler** → set `cladeTrail` to `stepsFor(clade)` in place of
  `CladeTraversal.ancestry(clade).reversed()`. The `Clade.of(slug)` guard, the
  `isAnimal` redirect, `ordersPlacedAt`, and `PlantCladeTree.narrower` for child
  cards are all unchanged.

`lowestCommonAncestor(List<Clade>)` and `ordersPlacedAt(Clade)` keep their kernel
use — they select *which* clade to show, they are not the trail lineage.

### Templates

- **`nav.jte`** — `@param List<Clade> cladeTrail` → `@param List<CladeStep> cladeTrail`.
  `current` stays `Clade` (the `clade` model attribute the detail page passes through).
- **`cladeTrail.jte`** — `@param List<Clade> cladeTrail` → `@param List<CladeStep> cladeTrail`;
  add `@import com.naturalist.library.CladeStep`. Per node:
  - `node.slug()` → `node.cladeSlug()`; `node.displayName()` unchanged.
  - dropdown children and links need a `Clade`: `PlantCladeTree.narrower(Clade.of(node.cladeSlug()))`
    and `PlantCladeTree.pageUrl(Clade.of(node.cladeSlug()))` — the same inline
    `Clade.of(...)` reach insects' template already makes.
  - current-node bolding: `current != null && node.cladeSlug().equals(current.slug())`.

`PlantCladeTree` (`pageUrl`, `isAnimal`, `narrower`) is unchanged.

### Data flow

Before: `PlantOrder.placedIn` (`Clade`) → `CladeTraversal.ancestry().reversed()` → `List<Clade>` → template.

After: `PlantOrder.placedIn` → `.slug()` → `cladeQuery.getBySlug(slug)` →
`CladeView` → `ancestry ++ subject` → `List<CladeStep>` → template.

### Wiring ripple

- **`plants-console/pom.xml`** adds a `library-api` dependency (no version — root
  `dependencyManagement` already carries it; insects-console depends on it).
  `library-console` is **not** added (no type from it is referenced in this scope).
- **`PlantsController` construction sites** must all supply a `CladeQuery`:
  - the app (`apps/management-console`) auto-wires it — the `CladeQuery` bean already
    exists in that context because insects consumes it;
  - `PlantsTestContext` and any plants-console controller/template test that
    constructs `PlantsController` — audit during implementation and supply the same
    `CladeQuery` test double insects' console tests use (trace how insects-console
    tests obtain `CladeQuery` and reuse that path).

### Error handling

`stepsFor` uses `getBySlug(...).orElseThrow(...)`, mirroring insects
(`cladeTrailAt`). The slug derives from a `placedIn` / LCA `Clade` already
validated at fixture load (`Clade.of` throws on unknown slugs), so a missing view
is a real configuration bug worth failing loudly on. User-supplied slugs in
`cladeDetail` are still guarded by the existing `Clade.of` try/catch **before** any
query, so a bad URL still redirects to `/plants/orders`.

## Testing

- Existing plants-console template/controller tests keep passing after the wiring
  and param-type updates.
- Add/extend a test asserting the trail for a known plant — e.g. a species whose
  order carries a known `placedIn` — renders the expected ordered clade slugs
  (root→subject), now produced via `CladeQuery`.
- Assert no `CladeTraversal.ancestry` call remains in the clade-*trail* path
  (`cladeTrailFor`, `catalogCladeRoot` return, `cladeDetail`); it survives only
  inside `lowestCommonAncestor`.

### Completeness gate

```bash
mvn verify -pl domains/plants/plants-console -am
mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
```

## Documentation

Update `domains/plants/CLAUDE.md`:

- Note that `plants-console` sources the Tree-of-Life trail from
  `library.CladeQuery` / `CladeView` (matching insects), via the `stepsFor` seam.
- Record the three justified per-domain differences above as **intentional**, so
  the next organism module and any future shared-console extraction read them as
  design rather than drift.

## Risks

- **Construction-site ripple** (record-arity trap): adding the `CladeQuery` param
  breaks every `new PlantsController(...)` site. Mitigation: grep all construction
  sites before compiling; mirror insects' test wiring.
- **Stale-jar runtime 500s**: after touching module boundaries, a clean
  `mvn install` is required before `spring-boot:run` (see the console-run memory).
```
