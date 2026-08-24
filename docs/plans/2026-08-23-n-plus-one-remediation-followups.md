# N+1 Remediation Follow-ups

**Status: RESOLVED 2026-08-23.** All 7 sites below are fixed (commits `57b95bc1`,
`b90f7295`) and the gate is **armed** (`26364168`) — `EnforceQueryHygiene` was folded into the
`EnforceArchitecture` composite (the root pom's active recipe), so repo-wide
`rewrite:dryRun -Drewrite.failOnDryRunResults=true` now runs it and is clean. See "Resolution"
at the foot. The findings are preserved below as the record.

The `EnforceQueryHygiene` static N+1 gate (recipe `com.naturalist.rewrite.NoSelectInIteration`,
`tooling/naturalist-rewrite`) is built, tested, and defined in `naturalist.yml`. A repo-wide
`rewrite:dryRun` reckoning against it surfaced 7 real N+1s — a repository/query select
invoked once per element inside a loop or per-element stream operation, instead of once via
a batched sibling. (An 8th finding, a pagination-cursor `while`-loop in
`EntityRepositoryTest.getPage_streamingThroughAllPagesYieldsEverySourceEntity`, was a false
positive and has been fixed at the recipe level — `getPage`/`findPage` are now excluded from
select-site detection.) Each of the 7 sites below must be fixed — collapse the loop/stream
fan-out into a single call to the batched sibling — before the gate can be armed. Arming is
the final step: adding `<recipe>com.naturalist.EnforceQueryHygiene</recipe>` to the root
`pom.xml` `<activeRecipes>`.

## The 7 sites

### insects-core

| File | Method | Looped select | Iteration kind | Suggested batched sibling |
|---|---|---|---|---|
| `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java` | `familyChildren` | `forRankHierarchy` | stream:map | `findByNameSet` |
| `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java` | `genusChildren` | `forRankHierarchy` | stream:map | `findByNameSet` |
| `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java` | `speciesChildren` | `forParentName` | stream:map | `findByNameSet` |

### insects-console

| File | Method | Looped select | Iteration kind | Suggested batched sibling |
|---|---|---|---|---|
| `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` | `families` | `forRankHierarchy` | for-each-loop | `findByNameSet` |
| `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` | `orders` | `forRankHierarchy` | for-each-loop | `findByNameSet` |
| `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` | `genera` | `forRankHierarchy` | for-each-loop | `findByNameSet` |

### plants-core

| File | Method | Looped select | Iteration kind | Suggested batched sibling |
|---|---|---|---|---|
| `domains/plants/plants-core/src/main/java/com/naturalist/plants/PlantSpeciesQueryImpl.java` | `forFamilyName` | `forGenusName` | stream:flatMap | `findByNameSet` |

## Note on the insects-console rows

The 3 `InsectsController` rows likely fold into the existing controller de-fork work at
`docs/plans/2026-08-22-controller-defork-archunit-plan.md`. Coordinate with that effort
rather than double-fixing the same call sites.

## Arming step

Once all 7 sites are green under a fresh `mvn rewrite:dryRun`, add
`<recipe>com.naturalist.EnforceQueryHygiene</recipe>` to the root `pom.xml`
`<activeRecipes>` — that arms the gate.

## Resolution (2026-08-23)

Done — the gate is armed. Note the arming was ultimately done by adding
`com.naturalist.EnforceQueryHygiene` to the **`EnforceArchitecture` composite** in
`naturalist.yml` (which the root pom already lists in `<activeRecipes>` and the completeness
gate runs), rather than a second `<activeRecipes>` entry — same effect, one active recipe.

The 7 sites (3 files):

- **plants-core `PlantSpeciesQueryImpl.forFamilyName`** (`57b95bc1`) — batched
  `forGenusNames(Set<PlantGenusName>)` sibling on `PlantQuery.SpeciesQuery` /
  `PlantRepository.SpeciesRepository` (+ mock + contract test); mirrors
  `InsectSpeciesQueryImpl.forFamilyName`.
- **insects-core `InsectFactory`** (familyChildren/genusChildren/speciesChildren) **+
  insects-console `InsectsController`** (families/orders/genera) (`b90f7295`) — a new batched
  `InsectQuery.ImageQuery.forRankHierarchies(Set<InsectRankName>)` returns an `ImageGallery`
  keyed by root: it attributes every rank in the forest to its root via one batched taxonomy
  query per level, then one `getByParentNames`, then groups (roots must be mutually
  non-ancestral — the sibling/page shape both callers already produce). Added the supporting
  `FamilyQuery.forOrderNames(Set)` (+ repo `getByOrderNames`, mock, contract test). Both
  callers reduced to a single `forRankHierarchies` call, pulling each child's gallery via
  `ImageGallery.forEntity(name)`.

The `InsectsController` rows were expected to possibly fold into the controller de-fork effort
(`2026-08-22-controller-defork-archunit-plan.md`); in the end they batched cleanly in place,
so no coordination was needed.
