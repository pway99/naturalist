# Insect Hierarchical Image Query

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the recursive image-aggregation logic (`imagesForGenus`, `imagesForFamily`, `imagesForOrder`) from the console controller into a core query, so the gallery images for any rank are available to both the desktop and mobile consoles through the existing `InsectQuery.ImageQuery` port.

**Architecture:** Add a `forRankHierarchy(InsectRankName)` method to the `InsectQuery.ImageQuery` interface. The core implementation uses the existing `InsectAncestryResolver` pattern to walk the rank tree and collect images from the target rank and all descendant ranks. This replaces the three recursive helper methods in the controller with a single polymorphic query call.

**Tech Stack:** Java 21 sealed types, JUnit 5, AssertJ, `NaturalistDatabaseExtension`

## Global Constraints

- `InsectQuery.ImageQuery` is a public nested interface in `insects-api`.
- `ImageQueryImpl` in `insects-core` extends `AbstractEntityQuery`.
- The existing `InsectAncestryResolver` walks *up* the rank tree (subject → ancestors). This query needs to walk *down* (rank → descendants). The resolver is not directly reusable here, but the species/genus/family queries provide the downward traversal.
- Return type must be `ImageCollection` (the domain's `BehavioralCollection` subclass), not raw `List<InsectImage>`.
- Tests run via `mvn verify` from repo root (user runs builds locally — do not invoke `mvn`).

## No dependencies on other refactoring plans

This plan is independent of the record-helpers and catalog-service plans.

---

### Task 1: Add `forRankHierarchy` to `InsectQuery.ImageQuery` interface

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`

**Interfaces:**
- Produces: `ImageQuery.forRankHierarchy(InsectRankName)` → `ImageCollection`

- [ ] **Step 1: Read the current `ImageQuery` interface**

Check the current methods on `InsectQuery.ImageQuery` so the new method is placed consistently.

- [ ] **Step 2: Add the method declaration**

Add to the `ImageQuery` nested interface in `InsectQuery.java`:

```java
/**
 * Returns all images for the given rank and all descendant ranks in the
 * Linnaean hierarchy. For a species, this is just the species' own images.
 * For a genus, it includes the genus' images plus all member species' images.
 * For a family, it walks genera and their species. For an order, it walks
 * families, genera, and species.
 */
ImageCollection forRankHierarchy(InsectRankName rankName);
```

- [ ] **Step 3: Verify compilation fails for implementors**

Run: `mvn compile -pl domains/insects/insects-core`
Expected: compilation failure — `ImageQueryImpl` does not implement `forRankHierarchy`.

- [ ] **Step 4: Commit the interface change**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java
git commit -m "feat(insects-api): add forRankHierarchy to ImageQuery interface"
```

---

### Task 2: Implement `forRankHierarchy` in `ImageQueryImpl`

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/ImageQueryImpl.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/ImageQueryImplHierarchyTest.java`

**Interfaces:**
- Consumes: `InsectQuery.SpeciesQuery.forGenusName(InsectGenusName)`, `InsectQuery.GenusQuery.forFamilyName(InsectFamilyName)`, `InsectQuery.FamilyQuery.forOrderName(InsectOrderName)`, `ImageRepository.forParentName(InsectRankName)`, `ImageCollection.of(List<InsectImage>)`
- Produces: `ImageQueryImpl.forRankHierarchy(InsectRankName)` → `ImageCollection`

- [ ] **Step 1: Read `ImageQueryImpl` to understand current structure**

Read `domains/insects/insects-core/src/main/java/com/naturalist/insects/ImageQueryImpl.java` to see the constructor signature and existing dependencies. The implementation needs access to species, genus, and family queries for downward traversal.

- [ ] **Step 2: Write the failing test**

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ImageQueryImplHierarchyTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectQuery query = context.insectQuery();

    @Test
    void forRankHierarchy_species_returnsSpeciesImages() {
        var speciesName = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var directImages = query.images().forParentName(speciesName);
        var hierarchyImages = query.images().forRankHierarchy(speciesName);

        // For a species, hierarchy == direct
        assertThat(hierarchyImages.stream().toList())
                .hasSameSizeAs(directImages.stream().toList());
    }

    @Test
    void forRankHierarchy_genus_includesSpeciesImages() {
        // Battus genus — contains at least BattusPhilenor species
        var genusName = TestInsectsIdentifiers.InsectGenus.Battus.name;
        var hierarchyImages = query.images().forRankHierarchy(genusName);

        // Should include genus-level images + all member species images
        var directGenusImages = query.images().forParentName(genusName);
        var speciesInGenus = query.species().forGenusName(genusName);
        int expectedMinimum = directGenusImages.stream().toList().size();
        for (var species : speciesInGenus.stream().toList()) {
            expectedMinimum += query.images().forParentName(species.name()).stream().toList().size();
        }

        assertThat(hierarchyImages.stream().toList()).hasSize(expectedMinimum);
    }

    @Test
    void forRankHierarchy_order_includesAllDescendantImages() {
        // Use Hymenoptera or Diptera — orders that exist in the test data
        var orderName = TestInsectsIdentifiers.InsectOrder.Hymenoptera.name;
        var hierarchyImages = query.images().forRankHierarchy(orderName);

        // Should include images from the order, its families, genera, and species
        // Just verify it returns at least as many as the direct order images
        var directOrderImages = query.images().forParentName(orderName);
        assertThat(hierarchyImages.stream().toList().size())
                .isGreaterThanOrEqualTo(directOrderImages.stream().toList().size());
    }

    @Test
    void forRankHierarchy_subspecies_returnsEmpty() {
        var subspeciesName = InsectSubspeciesName.of("battus-philenor-hirsuta");
        var result = query.images().forRankHierarchy(subspeciesName);
        assertThat(result.stream().toList()).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-core -Dtest=ImageQueryImplHierarchyTest`
Expected: compilation failure — `forRankHierarchy` not implemented in `ImageQueryImpl`.

- [ ] **Step 4: Modify `ImageQueryImpl` — add species/genus/family query dependencies**

Read `ImageQueryImpl`'s current constructor. It likely takes only an `ImageRepository`. It needs the species, genus, and family queries for downward traversal.

Modify the constructor to accept the additional queries:

```java
class ImageQueryImpl extends AbstractEntityQuery<InsectImageId, InsectImage,
        InsectEntityCollections.ImageCollection, InsectRepository.ImageRepository>
        implements InsectQuery.ImageQuery {

    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    ImageQueryImpl(InsectRepository.ImageRepository repository,
                   InsectQuery.SpeciesQuery speciesQuery,
                   InsectQuery.GenusQuery genusQuery,
                   InsectQuery.FamilyQuery familyQuery) {
        super(repository);
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }
    // ... existing methods unchanged
```

- [ ] **Step 5: Implement `forRankHierarchy`**

Add the method to `ImageQueryImpl`:

```java
@Override
public InsectEntityCollections.ImageCollection forRankHierarchy(InsectRankName rankName) {
    observer().arguments("forRankHierarchy", i -> i.identifier(rankName, "rankName"))
            .throwWhenInvalid();
    var images = new java.util.ArrayList<>(forParentName(rankName).stream().toList());
    switch (rankName) {
        case InsectSpeciesName _ -> { /* species is the leaf — no descendants */ }
        case InsectGenusName genusName -> collectForGenus(images, genusName);
        case InsectFamilyName familyName -> collectForFamily(images, familyName);
        case InsectOrderName orderName -> collectForOrder(images, orderName);
        case InsectSubspeciesName _ -> { /* no entity yet */ }
    }
    return InsectEntityCollections.ImageCollection.of(images);
}

private void collectForGenus(java.util.List<InsectImage> images,
                              InsectGenusName genusName) {
    for (var species : speciesQuery.forGenusName(genusName).stream().toList()) {
        images.addAll(forParentName(species.name()).stream().toList());
    }
}

private void collectForFamily(java.util.List<InsectImage> images,
                               InsectFamilyName familyName) {
    for (var genus : genusQuery.forFamilyName(familyName).stream().toList()) {
        images.addAll(forParentName(genus.name()).stream().toList());
        collectForGenus(images, genus.name());
    }
}

private void collectForOrder(java.util.List<InsectImage> images,
                              InsectOrderName orderName) {
    for (var family : familyQuery.forOrderName(orderName).stream().toList()) {
        images.addAll(forParentName(family.name()).stream().toList());
        collectForFamily(images, family.name());
    }
}
```

- [ ] **Step 6: Update all `ImageQueryImpl` construction sites**

The constructor signature changed — every call site needs the new parameters.

**In `InsectQueryImpl` constructor** (the main wiring point):
Find where `ImageQueryImpl` is created (it's created externally and passed in — check whether `InsectQueryImpl` constructs it or receives it). If received, the construction is in `InsectsTestContext`:

Update `InsectsTestContext.java` — change:
```java
InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(repository.imageRepository);
```
to:
```java
InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(
        repository.imageRepository, speciesQuery, genusQuery, familyQuery);
```

Note: `speciesQuery`, `genusQuery`, and `familyQuery` are already constructed before `imageQuery` in `InsectsTestContext`. Verify the declaration order; if `imageQuery` is constructed before `speciesQuery`, reorder so `speciesQuery`, `genusQuery`, and `familyQuery` are created first.

**In `InsectsTestContextInternal`** (`insects-core/src/test/java/`) — apply the same constructor change. This is the core-local test context that mirrors `InsectsTestContext` for tests that can't depend on the test-context module.

**In `ImageQueryImplTest`** — update the field initialization:
```java
// Was:
InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);

// Now:
InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(
        imageRepository, speciesQuery, genusQuery, familyQuery);
```

Add the missing query fields if not already present — look at `InsectFactoryTest` for the pattern of creating `SpeciesQueryImpl`, `GenusQueryImpl`, `FamilyQueryImpl` from mocked repositories.

**In `InsectFactoryTest`** and any other test that directly constructs `ImageQueryImpl` — apply the same change.

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn test -pl domains/insects/insects-core`
Expected: all tests PASS, including the new `ImageQueryImplHierarchyTest`.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/ImageQueryImpl.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/ImageQueryImplHierarchyTest.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/ImageQueryImplTest.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java
git add domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
git commit -m "feat(insects-core): implement forRankHierarchy on ImageQueryImpl"
```

---

### Task 3: Simplify controller image aggregation

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Consumes: `InsectQuery.ImageQuery.forRankHierarchy(InsectRankName)` → `ImageCollection`

- [ ] **Step 1: Replace `imagesForGenus`, `imagesForFamily`, `imagesForOrder`**

Delete the three private methods from the controller:

```java
// DELETE these three methods entirely:
private List<InsectImage> imagesForGenus(InsectGenusName genusName) { ... }
private List<InsectImage> imagesForFamily(InsectFamilyName familyName) { ... }
private List<InsectImage> imagesForOrder(InsectOrderName orderName) { ... }
```

- [ ] **Step 2: Replace call sites**

In `families()` — replace:
```java
imagesByFamily.put(family.name(), imagesForFamily(family.name()));
```
with:
```java
imagesByFamily.put(family.name(),
        insectQuery.images().forRankHierarchy(family.name()).stream().toList());
```

In `familyDetail()` — replace:
```java
imagesByGenus.put(g.name(), imagesForGenus(g.name()));
```
with:
```java
imagesByGenus.put(g.name(),
        insectQuery.images().forRankHierarchy(g.name()).stream().toList());
```

In `orders()` — replace:
```java
imagesByOrder.put(order.name(), imagesForOrder(order.name()));
```
with:
```java
imagesByOrder.put(order.name(),
        insectQuery.images().forRankHierarchy(order.name()).stream().toList());
```

In `orderDetail()` — replace:
```java
imagesByFamily.put(f.name(), imagesForFamily(f.name()));
```
with:
```java
imagesByFamily.put(f.name(),
        insectQuery.images().forRankHierarchy(f.name()).stream().toList());
```

In `genera()` — replace:
```java
imagesByGenus.put(genus.name(), imagesForGenus(genus.name()));
```
with:
```java
imagesByGenus.put(genus.name(),
        insectQuery.images().forRankHierarchy(genus.name()).stream().toList());
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl domains/insects/insects-console`
Expected: compilation succeeds.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "refactor(insects-console): use forRankHierarchy query for gallery aggregation"
```

---

## Future work: Collection lens query

The collection lens filtering (controller lines 423–435 for species list, 658–666 for detail page) fetches all of a naturalist's observations and filters client-side. This could become a core query like `insectQuery.species().forNaturalist(name, pageRequest)` or a read model that includes "collected by naturalist X." This is lower priority — it works correctly today and doesn't duplicate logic. The value appears when the RDBMS layer makes a database-level `WHERE observedBy = ?` filter practical.
