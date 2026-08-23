# N+1 Remediation Follow-ups

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
