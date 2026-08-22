# Insect Rank-Page Read Model — Stage 3 (retire dead read-model cruft) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the two now-dead pieces of read-model machinery: the never-consumed standalone `taxonView()` query stack, and the vestigial `features()` slot on the `InsectTaxonView` permits (features live on `Insect` as `InsectFeatureView` since stage 1; the permits are used only for the rank chain and child cards, neither of which reads their `features()`).

**Architecture:** One cleanup task, two concerns. **(A)** delete `InsectTaxonViewFactory`, `InsectTaxonViewQueryImpl`, and their tests; strip `taxonView()` from `InsectQuery`/`InsectQueryImpl`/`InsectQueryImplTest`. **(B)** drop the `FeatureCollection features` component from the sealed `InsectTaxonView` and its four permits, collapse the 3-arg `of(entity, images, features)` to `of(entity, images)`, and update the two caller kinds (the `InsectFactory` child helpers and the stage-2 template tests). Behavior-preserving — pure dead-code removal. Design: [2026-08-20-insect-rank-page-read-model-design.md](2026-08-20-insect-rank-page-read-model-design.md) §2.5/§3.

**Tech Stack:** Java 21 records + sealed types, JUnit 5 + AssertJ, Maven.

## Global Constraints

- **Behavior-preserving.** No page output changes. The only observable effect is a smaller api surface. Keep the permits' 1-arg `of(entity)` (empty images) and `name()`/`belongsTo*` behavior intact.
- **Do NOT touch** `InsectFeatureView` / `RankGroup.features()` / `Insect.features()` — those are the live feature path (a different `features()`); only the permit/`InsectTaxonView` `features()` is being removed.
- The sealed `InsectTaxonView` interface and its four permits STAY (they are the rank-chain and child-card building blocks) — only their `features()` member is removed.
- Builds: `mvn -pl domains/insects/insects-api -am test` and `mvn -pl domains/insects/insects-core -am test` and `mvn -pl domains/insects/insects-console -am test` must all pass; grep to confirm no dangling references.
- Out of scope: the N+1 in `InsectFeatureQueryImpl`; `InsectsController:114` image path.

---

### Task 1: Retire dead `taxonView()` + drop the permits' vestigial `features()` slot

**Files — (A) delete the dead standalone query:**
- Delete: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectTaxonViewFactory.java`
- Delete: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectTaxonViewQueryImpl.java`
- Delete: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectTaxonViewFactoryTest.java`
- Delete: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectTaxonViewQueryImplTest.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` (remove the `TaxonViewQuery taxonView();` method, the nested `TaxonViewQuery` interface if present, and the `taxonView()` javadoc example lines)
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java` (remove the `taxonViewQuery` field, its construction of `new InsectTaxonViewFactory(...)` + `new InsectTaxonViewQueryImpl(...)`, and the `taxonView()` accessor)
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java` (remove any `taxonView()` assertion)

**Files — (B) drop the permit `features()` slot:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectTaxonView.java` (remove `FeatureCollection features();` and the now-unused `FeatureCollection` import + the `features()` javadoc)
- Modify: `InsectOrderView.java`, `InsectFamilyView.java`, `InsectGenusView.java`, `InsectSpeciesView.java` (each: remove the `FeatureCollection features` record component; collapse `of(entity, images, features)` → `of(entity, images)`; remove `.behavioralCollection(features, "features")` from `invariants()`; keep the 1-arg `of(entity)` — it now calls `of(entity, ImageCollection.empty())`; drop the unused `FeatureCollection` import)
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java` (the three child helpers: `InsectFamilyView.of(f, imageQuery.forRankHierarchy(f.name()), FeatureCollection.empty())` → `InsectFamilyView.of(f, imageQuery.forRankHierarchy(f.name()))`; same for genus/species; drop the now-unused `FeatureCollection` import if nothing else uses it)
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTaxonViewTest.java` (update permit constructions to 2-arg `of`; remove any `features()` assertions)
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsOrdersTemplateTest.java`, `InsectsFamiliesTemplateTest.java`, `InsectsGeneraTemplateTest.java` (the stage-2 populated-child-card tests build `InsectFamilyView.of(entity, ImageCollection.empty(), FeatureCollection.empty())` — collapse to `of(entity, ImageCollection.empty())`; drop unused `FeatureCollection` import)

**Interfaces:**
- Produces: `InsectTaxonView` with methods `name()` + `images()` only (no `features()`); permit factory `of(entity, ImageCollection)` (2-arg) and `of(entity)` (1-arg). `InsectQuery` no longer declares `taxonView()`.

- [ ] **Step 1: (A) Delete the dead `taxonView()` stack**

Delete the four files listed under (A). In `InsectQuery.java` remove the `taxonView()` method, the nested `TaxonViewQuery` interface (if the method returned one), and the `taxonView()` javadoc examples. In `InsectQueryImpl.java` remove the `taxonViewQuery` field, the `new InsectTaxonViewFactory(...)` + `new InsectTaxonViewQueryImpl(...)` lines, and the `public TaxonViewQuery taxonView()` accessor. In `InsectQueryImplTest.java` remove any assertion referencing `taxonView()`.

- [ ] **Step 2: Verify (A) builds green**

Run: `mvn -pl domains/insects/insects-core -am test`
Expected: PASS. Then confirm no dangling references:
`grep -rn "taxonView\|InsectTaxonViewFactory\|InsectTaxonViewQueryImpl\|TaxonViewQuery" domains/insects --include=*.java` → no hits.

- [ ] **Step 3: (B) Drop `features()` from the sealed interface + four permits**

In `InsectTaxonView.java` remove `FeatureCollection features();`, the `FeatureCollection` import, and the `features()` javadoc. In each of the four permit records remove the `FeatureCollection features` component, change `of(entity, ImageCollection images, FeatureCollection features)` to `of(entity, ImageCollection images)`, keep the 1-arg `of(entity)` (now `return of(entity, ImageCollection.empty());`), remove `.behavioralCollection(features, "features")` from `invariants()`, and drop the unused `FeatureCollection` import. Example — `InsectFamilyView` after:

```java
public record InsectFamilyView(
        InsectFamily family,
        ImageCollection images
) implements InsectTaxonView {

    public static InsectFamilyView of(InsectFamily family, ImageCollection images) {
        return new InsectFamilyView(family, images);
    }

    public static InsectFamilyView of(InsectFamily family) {
        return new InsectFamilyView(family, ImageCollection.empty());
    }

    @Override public InsectFamilyName name() { return family.name(); }
    // belongsToOrder(...) / orderName() unchanged

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(family, "family")
                .behavioralCollection(images, "images");
    }
}
```

- [ ] **Step 4: (B) Update the callers to 2-arg `of`**

In `InsectFactory.java` change the three child helpers' `ChildView.of(entity, images, FeatureCollection.empty())` → `ChildView.of(entity, images)` and drop the `FeatureCollection` import if now unused. In `InsectTaxonViewTest.java` and the three stage-2 template tests, change every `of(entity, images, FeatureCollection.empty())` → `of(entity, images)` and remove `features()` assertions + unused `FeatureCollection` imports.

- [ ] **Step 5: Verify (B) — full build green + no dangling references**

Run: `mvn -pl domains/insects/insects-api -am test` then `mvn -pl domains/insects/insects-console -am test`
Expected: PASS. Then:
`grep -rn "FeatureCollection features\|\.features()" domains/insects/insects-api domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java` → the only `features` hits should be `InsectFeatureView`/`RankGroup`-related, none on a `TaxonView` permit. Confirm `InsectFeatureView` / `Insect.features()` are untouched.

- [ ] **Step 6: Stage (do NOT commit)**

`git add` all deletions and modifications; leave staged on `main`. (Controller presents for user review + commit.)

---

## Self-Review

**Spec coverage (design §2.5/§3 stage 3):**
- Retire dead `taxonView()` / `InsectTaxonViewFactory` / `TaxonViewQueryImpl` → Steps 1–2. ✓
- Drop permits' `features()` slot → Steps 3–4. ✓
- Sealed `InsectTaxonView` + permits kept (only `features()` removed) → Step 3. ✓
- `InsectFeatureView`/`Insect.features()` untouched → Step 5 guard. ✓

**Placeholder scan:** none — the `InsectFamilyView` after-state is shown in full; the other three permits are the identical transformation on their own entity type.

**Type consistency:** `of(entity, ImageCollection)` (2-arg) is the new factory shape used by the `InsectFactory` child helpers and the template tests; `of(entity)` (1-arg) stays for the rank chain in `Insect`/`InsectFactory`. `InsectTaxonView` exposes `name()` + `images()` only.

**Note for the executor:** the removal is behavior-preserving; if any test asserts on a permit's `features()`, that assertion is testing the vestigial slot — delete the assertion, do not re-add the slot. If the 1-arg `of(entity)` was previously implemented as `new Permit(entity, ImageCollection.empty(), FeatureCollection.empty())`, it becomes `of(entity, ImageCollection.empty())` or `new Permit(entity, ImageCollection.empty())`.
