# Insect Features Into the Read Model — Stage 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make insect features flow through the `Insect` read model exactly like citations already do — the console renders `insect.features()`, and the group-by-rank display shaping lives in the api/core read model, not the console.

**Architecture:** Two atomic, build-green slices. **Task 1** reshapes `InsectFeatureView` into a display-ready, grouped-by-contributing-rank structure assembled in `InsectFeatureQueryImpl`, renders it directly in the console, and deletes the console-side `FeatureGroup` collator. **Task 2** composes that view into the `Insect` read model via `InsectFactory` (symmetric with `citations`) so each handler reads `insect.features()`. This is stage 1 of the design in [2026-08-20-insect-rank-page-read-model-design.md](2026-08-20-insect-rank-page-read-model-design.md); stages 2 (role + children) and 3 (retire dead `taxonView()`) are separate plans.

**Tech Stack:** Java 21 records + sealed types, JTE templates, JUnit 5 + AssertJ, the project Observer framework, `NaturalistDatabaseExtension` + repository mocks seeded from JSON test data.

## Global Constraints

- **Identity model:** `InsectFeatureView` and its nested group type are `ReadModel` / `ValueObject` (own nothing, assembled from persisted parts). `invariants()` assert structural well-formedness only.
- **No `new Foo(...)` at call sites** outside a type's own class where a static `of(...)` exists — but records are constructed directly inside the factory/query that assembles them (existing pattern; `InsectFactory` uses `new InsectSpeciesView(...)`-style `of` and `Insect.empty().with*`).
- **Symmetry target:** `InsectQuery.CitationQuery.findByRankName` returns `InsectCitationView` (non-`Optional`); `FeatureQuery.findByRankName` must match — return `InsectFeatureView`, not `Optional<InsectFeatureView>`.
- **Record-arity ripple (Task 2):** adding a component to the `Insect` record changes every `with*`, `empty()`, and `new Insect(...)`. Grep the whole repo for construction sites; do not trust this file list.
- **Builds:** scoped `mvn -pl <module> -am test` during a task; the user runs full `mvn verify` at the gate. Kernel/signature changes are not involved here, so no clean-install needed.
- **Presentation vs read model:** the rank *label* string ("Order") is presentation and stays in the console; the *grouping structure* is read-model and moves into the view.

---

### Task 1: Display-ready `InsectFeatureView`; render it in the console; delete `FeatureGroup`

Reshape the feature view from a flat provenance list to groups-by-contributing-rank, move the grouping out of the console `FeatureGroup` class into `InsectFeatureQueryImpl`, render the grouped view directly, and delete `FeatureGroup`. Features are still fetched via `insectQuery.features().findByRankName(rank)` in this task (Task 2 moves them onto `Insect`).

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFeatureView.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java` (the `FeatureQuery` nested interface, ~line 156)
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFeatureQueryImpl.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFeatureQueryImplTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFeatureViewTest.java`
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/RankLabel.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/features.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/{detail,genus,family,order}.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (the `featureGroups(...)` helper ~line 191 and its four callers)
- Delete: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/FeatureGroup.java`
- Delete: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/FeatureGroupTest.java`

**Interfaces:**
- Produces: `InsectFeatureView(InsectRankName subject, List<InsectFeatureView.RankGroup> groups)`; `InsectFeatureView.RankGroup(InsectRankName rank, List<InsectFeature> features)`; `InsectQuery.FeatureQuery.findByRankName(InsectRankName) -> InsectFeatureView` (non-null, groups possibly empty, ancestor-first, ordinal-ordered within a group); `RankLabel.of(InsectRankName) -> String`.
- Consumes: `InsectAncestryResolver.resolveAncestry(InsectRankName) -> List<InsectRankName>` (subject-first), the feature + assignment repositories (unchanged).

- [ ] **Step 1: Write the failing query-impl test**

Create `InsectFeatureQueryImplTest.java`. Wire the feature query against seeded mocks (mirror `InsectFactoryTest`'s wiring), insert a family-rank feature into an otherwise order-seeded lineage, and assert the grouped, ancestor-first shape:

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFeatureQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectFamilyRepositoryMock familyRepository = new InsectFamilyRepositoryMock(db);
    InsectGenusRepositoryMock genusRepository = new InsectGenusRepositoryMock(db);
    InsectSpeciesRepositoryMock speciesRepository = new InsectSpeciesRepositoryMock(db);
    InsectFeatureRepositoryMock featureRepository = new InsectFeatureRepositoryMock(db);
    InsectFeatureAssignmentRepositoryMock assignmentRepository =
            new InsectFeatureAssignmentRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new InsectFamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new InsectGenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new InsectSpeciesQueryImpl(speciesRepository, genusQuery);
    InsectAncestryResolver ancestryResolver =
            new InsectAncestryResolver(speciesQuery, genusQuery, familyQuery);

    InsectFeatureQueryImpl featureQuery = new InsectFeatureQueryImpl(
            featureRepository, assignmentRepository, ancestryResolver);

    @Test
    void findByRankName_groupsAncestorFirst_familyGroupLast() {
        // battus-philenor: species → ... → papilionidae (family) → lepidoptera (order).
        // JSON seeds order-rank (lepidoptera) assignments; add a family-rank one.
        InsectFeature tailed = InsectFeature.of(InsectFeatureId.create(), "tailed hindwings");
        featureRepository.insert(tailed);
        assignmentRepository.insert(new InsectFeatureAssignment(
                InsectFeatureAssignmentId.create(), tailed.id(),
                InsectFamilyName.of("papilionidae"), LinealRank.FAMILY, 0));

        InsectFeatureView view = featureQuery.findByRankName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        // Ancestor-first: the ORDER group precedes the FAMILY group.
        assertThat(view.groups()).isNotEmpty();
        assertThat(view.groups().getFirst().rank().rank()).isEqualTo(LinealRank.ORDER);
        InsectFeatureView.RankGroup last = view.groups().getLast();
        assertThat(last.rank()).isEqualTo(InsectFamilyName.of("papilionidae"));
        assertThat(last.features()).extracting(InsectFeature::value)
                .containsExactly("tailed hindwings");
    }

    @Test
    void findByRankName_ordersFeaturesWithinRankByOrdinal() {
        InsectFeatureView view = featureQuery.findByRankName(InsectOrderName.of("lepidoptera"));
        // Single ORDER group; its features are ordinal-ordered (seeded ordinals 1 then 2).
        assertThat(view.groups()).hasSize(1);
        assertThat(view.groups().getFirst().rank()).isEqualTo(InsectOrderName.of("lepidoptera"));
        assertThat(view.groups().getFirst().features().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void findByRankName_rejectsNull() {
        assertThat(catchThrowable(() -> featureQuery.findByRankName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
```

(Confirm the exact `InsectFeatureAssignment` component order against `InsectFeatureAssignment.java` — JSON order is `id, featureId, rankName, rank, ordinal`. Add `import static org.assertj.core.api.Assertions.catchThrowable;`.)

- [ ] **Step 2: Run it to verify it fails**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFeatureQueryImplTest`
Expected: FAIL to compile — `RankGroup` / `groups()` do not exist yet, and `findByRankName` still returns `Optional`.

- [ ] **Step 3: Reshape `InsectFeatureView`**

Replace `RankedFeature` + `features` with grouped shape:

```java
public record InsectFeatureView(
        InsectRankName subject,
        List<RankGroup> groups
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(groups, "groups");
    }

    /** The field marks contributed at one rank in the lineage, ordinal-ordered. */
    public record RankGroup(
            InsectRankName rank,
            List<InsectFeature> features
    ) implements ValueObject {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .identifier(rank, "rank")
                    .notNull(features, "features");
        }
    }
}
```

- [ ] **Step 4: Change the port and assemble grouped in `InsectFeatureQueryImpl`**

In `InsectQuery.java`, change the `FeatureQuery` method to `InsectFeatureView findByRankName(InsectRankName subject);` (drop `Optional`).

In `InsectFeatureQueryImpl.java`, keep the ancestry walk and per-rank ordinal sort, but emit one `RankGroup` per contributing rank (ancestor-first) instead of a flat list, and return the view directly:

```java
@Override
public InsectFeatureView findByRankName(InsectRankName subject) {
    observer.arguments("findByRankName", i -> i.identifier(subject, "subject"))
            .throwWhenInvalid();

    List<InsectRankName> ancestry = ancestryResolver.resolveAncestry(subject); // subject-first
    List<InsectFeatureView.RankGroup> groups = new ArrayList<>();

    // Ancestor-first: walk the ancestry in reverse (order → … → subject).
    for (int a = ancestry.size() - 1; a >= 0; a--) {
        InsectRankName rank = ancestry.get(a);
        List<InsectFeatureAssignment> atRank = assignmentRepository.getByRankName(rank).stream()
                .sorted(Comparator.comparingInt(InsectFeatureAssignment::ordinal))
                .toList();
        if (atRank.isEmpty()) {
            continue;
        }
        Set<InsectFeatureId> ids = atRank.stream()
                .map(InsectFeatureAssignment::featureId).collect(Collectors.toSet());
        Map<InsectFeatureId, InsectFeature> resolved = new LinkedHashMap<>();
        for (InsectFeature f : featureRepository.getByEntityNameSet(ids)) {
            resolved.put(f.id(), f);
        }
        List<InsectFeature> features = atRank.stream()
                .map(x -> resolved.get(x.featureId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (!features.isEmpty()) {
            groups.add(new InsectFeatureView.RankGroup(rank, features));
        }
    }

    InsectFeatureView view = new InsectFeatureView(subject, List.copyOf(groups));
    observer.observable(view, "featureView").observe(Level.WARN);
    return view;
}
```

Leave `findByFeature(InsectFeatureId)` unchanged.

- [ ] **Step 5: Run the query-impl test to verify it passes**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFeatureQueryImplTest`
Expected: PASS.

- [ ] **Step 6: Update `InsectFeatureViewTest` to the new shape**

Replace the `RankedFeature` constructions with `RankGroup`, and the null-violation names (`.view.features` → `.view.groups`):

```java
@Test
void validView_hasNoInvariantViolations() {
    MethodObserver mo = observer.forMethod("validView_hasNoInvariantViolations");
    InsectFeatureView view = new InsectFeatureView(
            InsectSpeciesName.of("battus-philenor"),
            List.of(new InsectFeatureView.RankGroup(
                    InsectOrderName.of("lepidoptera"),
                    List.of(InsectFeature.of(InsectFeatureId.create(), "scaled wings")))));
    assertThat(mo.observable(view, "view").violations()).isEmpty();
}

@Test
void nullComponents_reportInvariantViolations() {
    MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");
    InsectFeatureView view = new InsectFeatureView(null, null);
    assertThat(mo.observable(view, "view").violationNamesRemovingPrefix(mo.observationPoint()))
            .containsExactlyInAnyOrder(".view.subject", ".view.groups");
}

@Test
void validRankGroup_hasNoInvariantViolations() {
    MethodObserver mo = observer.forMethod("validRankGroup_hasNoInvariantViolations");
    InsectFeatureView.RankGroup group = new InsectFeatureView.RankGroup(
            InsectFamilyName.of("papilionidae"),
            List.of(InsectFeature.of(InsectFeatureId.create(), "tailed hindwings")));
    assertThat(mo.observable(group, "rankGroup").violations()).isEmpty();
}

@Test
void nullRankGroupComponents_reportInvariantViolations() {
    MethodObserver mo = observer.forMethod("nullRankGroupComponents_reportInvariantViolations");
    InsectFeatureView.RankGroup group = new InsectFeatureView.RankGroup(null, null);
    assertThat(mo.observable(group, "rankGroup").violationNamesRemovingPrefix(mo.observationPoint()))
            .containsExactlyInAnyOrder(".rankGroup.rank", ".rankGroup.features");
}
```

- [ ] **Step 7: Add the console `RankLabel` presentation helper**

```java
package com.naturalist.insects.console;

import com.naturalist.insects.InsectRankName;
import java.util.Locale;

/** Presentation helper: the human label for a rank ("Order", "Family"). */
final class RankLabel {
    private RankLabel() {}

    static String of(InsectRankName rankName) {
        String name = rankName.rank().name();               // e.g. "ORDER"
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 8: Render the grouped view in `features.jte`; delete `FeatureGroup`**

Rewrite `features.jte` to take the view and iterate `groups()` (mirrors `citations.jte`'s null/empty guard):

```jte
@import com.naturalist.insects.InsectFeatureView
@import com.naturalist.insects.console.RankLabel
@import com.naturalist.library.console.GlossaryLinker

@param InsectFeatureView features = null
@param GlossaryLinker glossaryLinker = GlossaryLinker.none()

@if(features != null && !features.groups().isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">$unsafe{glossaryLinker.linkHtml("What the identification was based on, from most conspicuous to most diagnostic. Check these against your photo.", "field-marks-lead")}</p>
        @for(var group : features.groups())
            <div class="field-mark-group">
                <h3>${RankLabel.of(group.rank())} — ${group.rank().value()}</h3>
                <ul>
                    @for(int i = 0; i < group.features().size(); i++)
                        <li>$unsafe{glossaryLinker.linkHtml(group.features().get(i).value(), group.rank().value() + "-" + i)}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
```

Delete `FeatureGroup.java` and `FeatureGroupTest.java`.

- [ ] **Step 9: Update the four caller templates**

In each of `detail.jte`, `genus.jte`, `family.jte`, `order.jte`, change the param and the include:
- `@param List<FeatureGroup> featureGroups = List.of()` → `@param com.naturalist.insects.InsectFeatureView features = null`
- `@template.insects.features(featureGroups = featureGroups, glossaryLinker = glossaryLinker)` → `@template.insects.features(features = features, glossaryLinker = glossaryLinker)`
- Remove the now-unused `@import ...FeatureGroup` / `import java.util.List` if they become unused.

- [ ] **Step 10: Point the controller at the view**

In `InsectsController.java`, replace the `featureGroups` helper with one returning the view, and update the four `model.addAttribute` calls:

```java
private InsectFeatureView featureView(InsectRankName rankName) {
    return insectQuery.features().findByRankName(rankName);
}
```
Each handler: `model.addAttribute("featureGroups", featureGroups(x));` → `model.addAttribute("features", featureView(x));`. Remove the `FeatureGroup` import.

- [ ] **Step 11: Build the console and run its template tests**

Run: `mvn -pl domains/insects/insects-console -am test`
Expected: PASS — `FeatureGroupTest` is gone; `detail`/`genera`/`families` template tests still render (they don't set the feature param, so the section is simply absent). Fix any residual `FeatureGroup` references the compiler flags.

- [ ] **Step 12: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core domains/insects/insects-console
git commit -m "refactor(insects): display-ready InsectFeatureView; render from read model, drop FeatureGroup"
```

---

### Task 2: Compose `features` into the `Insect` read model

Add `features` to `Insect` symmetric with `citations`, have `InsectFactory` populate it, and switch the handlers to `insect.features()` — removing the separate feature fetch.

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java` (~line 65, the `new InsectFactory(...)` call)
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Consumes: `InsectQuery.FeatureQuery.findByRankName(InsectRankName) -> InsectFeatureView` (from Task 1).
- Produces: `Insect.features() -> @Nullable InsectFeatureView`; `Insect.withFeatures(@Nullable InsectFeatureView) -> Insect`; `InsectFactory(SpeciesQuery, ImageQuery, GenusQuery, FamilyQuery, OrderQuery, InsectLifeStageQuery, CitationQuery, FeatureQuery)`.

- [ ] **Step 1: Write the failing factory test**

Add to `InsectFactoryTest.java` (wire a `FeatureQuery` into the existing setup, then assert composition). Add fields next to the existing repos/queries:

```java
InsectFeatureRepositoryMock featureRepository = new InsectFeatureRepositoryMock(db);
InsectFeatureAssignmentRepositoryMock assignmentRepository =
        new InsectFeatureAssignmentRepositoryMock(db);
InsectQuery.FeatureQuery featureQuery = new InsectFeatureQueryImpl(
        featureRepository, assignmentRepository, ancestryResolver);
```

Add `featureQuery` as the final argument to the existing `new InsectFactory(...)`. Then:

```java
@Test
void buildByName_composesFeatureView() {
    InsectSpeciesName name = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
    Insect insect = factory.buildByName(name).orElseThrow();
    assertThat(insect.features()).isNotNull();
    assertThat(insect.features().subject()).isEqualTo(name);
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFactoryTest#buildByName_composesFeatureView`
Expected: FAIL to compile — `Insect.features()` and the 8-arg `InsectFactory` do not exist yet.

- [ ] **Step 3: Add the `features` component to `Insect`**

Add `@Nullable InsectFeatureView features` as the final component (after `citations`). Update: the canonical constructor param list, `empty()` (pass `null`), every `with*` copy-constructor to carry `features` through, add `withFeatures`, and add an `invariants()` descent mirroring `citations`:

```java
public Insect withFeatures(@Nullable InsectFeatureView features) {
    return new Insect(observations, order, family, genus, species, lifeStages, citations, features);
}
```
```java
.whenNotNull(features, f -> f.readModel(features, "features"))
```

⚠️ Every existing `with*` body and `empty()` gains the trailing `features` argument. Grep and update all `new Insect(` sites (Step 6 covers tests).

- [ ] **Step 4: Compose it in `InsectFactory`**

Add `InsectQuery.FeatureQuery featureQuery` as the final constructor param (with the null-check in the `observer.arguments("constructor", …)` block and the field assignment). Add `.withFeatures(featureQuery.findByRankName(name))` to each rank branch, right after `.withCitations(...)` — for species/genus/family use the branch's rank name; for the ancestor-resolving branches the *entry* name is correct (features resolve up the lineage from it). Example (species branch):

```java
Insect insect = Insect.empty()
        .withObservations(imageQuery.forParentName(speciesName))
        .withSpecies(InsectSpeciesView.of(species))
        .withLifeStages(lifeStageQuery.lifeStages().forParentName(speciesName))
        .withCitations(citationQuery.findByRankName(speciesName))
        .withFeatures(featureQuery.findByRankName(speciesName));
```

- [ ] **Step 5: Thread `featureQuery` through `InsectQueryImpl`**

At the `new InsectFactory(...)` call (~line 65), append `this.featureQuery` (already a field, constructed just above at ~line 63):

```java
this.insectFactory = new InsectFactory(
        speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
        insectLifeStageQuery, this.citationQuery, this.featureQuery);
```

- [ ] **Step 6: Run factory test + fix `InsectTest` arity**

Run: `mvn -pl domains/insects/insects-core -am test -Dtest=InsectFactoryTest`
Expected: PASS. Then update `InsectTest.java` — every `new Insect(...)` / `Insect.empty()`-based construction gains the trailing `features` arg; add one invariants assertion:

```java
@Test
void featuresDescent_reportsNestedViolations() {
    // A structurally invalid features view (null subject) surfaces under ".features".
    InsectFeatureView badFeatures = new InsectFeatureView(null, List.of());
    Insect insect = Insect.empty().withFeatures(badFeatures);
    MethodObserver mo = observer.forMethod("featuresDescent_reportsNestedViolations");
    assertThat(mo.observable(insect, "insect").violationNamesRemovingPrefix(mo.observationPoint()))
            .contains(".insect.features.subject");
}
```

Run: `mvn -pl domains/insects/insects-api -am test -Dtest=InsectTest`
Expected: PASS.

- [ ] **Step 7: Switch handlers to `insect.features()`; delete the transitional helper**

In `InsectsController.java`, each of the four handlers already resolves `insect` via `insectQuery.getByName(...)`. Change `model.addAttribute("features", featureView(x));` → `model.addAttribute("features", insect.get().features());` (use the local `Insect` variable each handler already has — `i` in `detail`, `insect.get()` in the others). Delete the `featureView(...)` helper added in Task 1.

- [ ] **Step 8: Build the module tree and run tests**

Run: `mvn -pl domains/insects/insects-console -am test`
Expected: PASS. Grep to confirm no remaining `featureView(` / separate feature fetch in the controller and no stray `new Insect(` with the old arity:

Run: `grep -rn "new Insect(" domains/insects --include=*.java`
Expected: every hit passes the new arity (8 args) or is inside `Insect` itself.

- [ ] **Step 9: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core domains/insects/insects-console
git commit -m "feat(insects): compose features into the Insect read model, symmetric with citations"
```

---

## Self-Review

**Spec coverage (design §3 stage 1):**
- "Add `features` to `Insect` symmetric with `citations`" → Task 2 Steps 3–5. ✓
- "Make `InsectFeatureView` display-ready (grouping moves out of the console)" → Task 1 Steps 3–4. ✓
- "`features.jte` renders `insect.features()`" → Task 1 Step 8 renders the view; Task 2 Step 7 sources it from `insect`. ✓
- "Delete `FeatureGroup` + `featureGroups()`" → Task 1 Step 8 (class) + Task 2 Step 7 (helper). ✓
- Out-of-scope (test data, citations-stay-flat, role/children, retire `taxonView()`) → correctly absent. ✓

**Placeholder scan:** No TBD/TODO. Two "confirm the exact signature against <file>" notes (`InsectFeatureAssignment` component order; the per-handler local `Insect` variable name) are verification instructions with concrete fallbacks, not placeholders.

**Type consistency:** `InsectFeatureView(subject, groups)` and `RankGroup(rank, features)` are used identically in the view, the query impl, both tests, and the JTE. `FeatureQuery.findByRankName -> InsectFeatureView` (non-`Optional`) is consistent across the port, the impl, the console helper (Task 1), and the factory (Task 2). `InsectFactory`'s new 8th param `FeatureQuery` matches the `InsectQueryImpl` call site and `InsectFactoryTest` wiring.

**Note for the executor:** `InsectFeatureQueryImplTest` and the `InsectFactoryTest` additions both need a `FeatureQuery` wired from `InsectFeatureRepositoryMock` + `InsectFeatureAssignmentRepositoryMock` + the existing `ancestryResolver`; confirm those mock class names exist (they do — see `InsectsTestContextInternal`).
