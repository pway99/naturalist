# Plants Evidence Stack S3 — PlantFeature + PlantFeatureAssignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give plants the feature layer insects has — `PlantFeature` (a normalised field-mark) and `PlantFeatureAssignment` (feature↔rank), with repositories and a direct-rank `features()` query — mirroring the current insects feature stack.

**Architecture:** Two domain entities in `plants-api` (`PlantFeature`, `PlantFeatureAssignment`), each with a repository super-interface + in-memory mock + behavioral contract + `TestEntitySource` + JSON catalog. A `FeatureCollection` and a `PlantQuery.FeatureQuery.forRankName(PlantRankName) → FeatureCollection` that composes via an **application-layer join**: one batched `getByRankName` for the rank's assignments, then one batched `getByEntityNameSet` for the features — never per-element. Faithful mirror of `domains/insects/insects-api/.../InsectFeature*` and `InsectFeatureQueryImpl`, adapted to `PlantRankName`'s four permits. The ancestry-walking lineage composite (`PlantFeatureView` + a `PlantAncestryResolver`) and the composed `Plant` read model are **explicitly out of scope** (later slice).

**Tech Stack:** Java 21 records + sealed types, Jackson 2.19 (record deserialization + `@JsonSubTypes` rank dispatch), JUnit 5 + AssertJ, the Observer framework, `NaturalistDatabase` + `NamedTestEntitySource`/`AbstractTestEntityRepository`.

## Global Constraints

- **Server-side (application-layer) joins only.** Any query composing more than one entity invokes each entity query **exactly once, batched**, and joins in memory. `FeatureQuery.forRankName` = `getByRankName(rank)` (1) + `getByEntityNameSet(featureIds)` (1). No repository call inside a loop/stream (the `domains/CLAUDE.md` "fan-out must batch" convention).
- **Mirror the CURRENT insects stack**, not a stale one. Reference files: `InsectFeature.java`, `InsectFeatureAssignment.java`, `InsectRepository.java` (`FeatureRepository`/`FeatureAssignmentRepository`), `InsectFeatureAssignmentRepositoryMock.java`, `InsectFeatureAssignmentEntityRepositoryTest.java`, `InsectFeatureQueryImpl.java`, `InsectEntityCollections.FeatureCollection`, `InsectQuery.FeatureQuery`. Adapt `InsectRankName`→`PlantRankName` (four permits: Order/Family/Genus/Species — **no Subspecies**).
- **Identity:** `PlantFeature`/`PlantFeatureAssignment` are `Entity<…Id>` (UUIDv7 surrogate, generated via the kernel generator — never `UUID.randomUUID()`). Ids live in `domains/identifiers/src/main/java/com/naturalist/plants/`.
- **`@UniqueValue`** on `PlantFeature.value`; declared in `uniqueConstraints()`. Value trimmed + lowercased in the compact constructor.
- **Batched set-arg validation:** `getByRankNames(Set<PlantRankName>)` validates with `observableCollection(rankNames, "rankNames")` (PlantRankName is a `RankName` → `Observable`; do NOT use `entityNameCollection` — a rank name is not statically an `EntityName`). Single-rank args use `.identifier(rankName, "rankName")`.
- **Jackson rank dispatch:** `PlantFeatureAssignment.rankName` carries `@JsonTypeInfo(use=Id.NAME, property="rank", include=As.EXTERNAL_PROPERTY)` + `@JsonSubTypes` for the four `PlantRankName` permits — mirror `InsectFeatureAssignment` exactly.
- **Test data uses real botany.** Feature values are real diagnostic marks; assignments attach them at real ranks in the seeded Asterales→Asteraceae→Helianthus chain. Every assignment→feature FK gets a `ForeignKeyConstraint`; a catalog-data test asserts every referenced feature exists.
- **Scaffolding skills:** use `/entity-repository <Entity>`, `/test-entity-source <Entity>`, `/entity-query <Entity>` to generate the repository/test-source/query stacks rather than hand-writing boilerplate — then fill in the domain-specific method bodies below. Build: scoped `mvn -pl domains/plants/<module> -am test`; the user runs full `mvn verify`.

---

### Task 1: `PlantFeature` entity + id + identifiers + invariant test

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/plants/PlantFeatureId.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFeature.java`
- Modify: `domains/identifiers-test/.../TestPlantsIdentifiers.java` (add a `PlantFeatures` nested class + `NotFound`)
- Test: `domains/plants/plants-api/src/test/java/com/naturalist/plants/PlantFeatureTest.java`

**Interfaces:**
- Produces: `PlantFeatureId` (EntityId, UUIDv7); `PlantFeature(PlantFeatureId id, @UniqueValue String value)` with `of(id, value)`, value trimmed+lowercased; `TestPlantsIdentifiers.PlantFeatures.<Name>.id` constants (≥2) + `TestPlantsIdentifiers.PlantFeatures.NotFound.id`.

- [ ] **Step 1: Create `PlantFeatureId`** — mirror `PlantImageId` (an `EntityId` subclass, `@JsonCreator of(...)`, `create()` via the kernel generator). Look at `domains/identifiers/src/main/java/com/naturalist/plants/PlantImageId.java` and copy its shape.

- [ ] **Step 2: Write the failing invariant test** `PlantFeatureTest`:
```java
@Test
void validFeature_hasNoInvariantViolations() {
    Observer o = Observer.forClass(PlantFeatureTest.class);
    PlantFeature f = PlantFeature.of(PlantFeatureId.create(), "opposite leaves");
    assertThat(o.forMethod("valid").observable(f, "feature").violations()).isEmpty();
}
@Test
void valueIsTrimmedAndLowercased() {
    assertThat(PlantFeature.of(PlantFeatureId.create(), "  Ray Florets ").value())
            .isEqualTo("ray florets");
}
@Test
void nullComponents_reportViolations() {
    Observer o = Observer.forClass(PlantFeatureTest.class);
    var mo = o.forMethod("null");
    assertThat(mo.observable(new PlantFeature(null, null), "feature")
            .violationNamesRemovingPrefix(mo.observationPoint()))
            .contains(".feature.id");
}
```

- [ ] **Step 3: Run to verify failure.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantFeatureTest` → FAIL (PlantFeature undefined).

- [ ] **Step 4: Implement `PlantFeature`** (mirror `InsectFeature`):
```java
package com.naturalist.plants;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.UniqueValue;
import com.naturalist.observability.Constraints;
import java.util.List;
import java.util.function.Consumer;

public record PlantFeature(PlantFeatureId id, @UniqueValue String value)
        implements Entity<PlantFeatureId> {

    public PlantFeature {
        if (value != null) value = value.trim().toLowerCase();
    }

    public static PlantFeature of(PlantFeatureId id, String value) {
        return new PlantFeature(id, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.entityId(id, "id").notBlank(value, "value");
    }

    // uniqueConstraints() declaring `value` — mirror how InsectFeature/its source declares @UniqueValue
}
```
(Confirm the exact `@UniqueValue` declaration + `uniqueConstraints()` mechanism against `InsectFeature` and `domains/CLAUDE.md` Field Annotations — `value` is a `@UniqueValue` plain-value field, declared in `uniqueConstraints()`.)

- [ ] **Step 5: Add `TestPlantsIdentifiers.PlantFeatures`** — a nested class with ≥2 named `PlantFeatureId` constants (e.g. `RayFlorets`, `OppositeLeaves`) and a `NotFound` inner class with a synthetic id. Mirror the existing `PlantGenera`/`Plants` nested structure.

- [ ] **Step 6: Run tests + commit.** `mvn -pl domains/plants/plants-api -am test -Dtest=PlantFeatureTest` → PASS.
```bash
git add domains/identifiers domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFeature.java domains/plants/plants-api/src/test domains/identifiers-test
git commit -m "feat(plants): PlantFeature entity + PlantFeatureId"
```

---

### Task 2: `PlantFeature` TestEntitySource + JSON + repository stack

**Files:**
- Create (via `/test-entity-source PlantFeature in plants module`): `PlantFeatureTestEntitySource.java` + `plants/feature/plant-features.json` (resources) + its test class.
- Create (via `/entity-repository PlantFeature`): `PlantRepository.FeatureRepository` (nested `protected interface`), `PlantFeatureRepositoryMock.java`, `PlantFeatureEntityRepositoryTest.java` + concrete mock test.

**Interfaces:**
- Produces: `PlantRepository.FeatureRepository extends EntityRepository<PlantFeatureId, PlantFeature>` (inherits `getByEntityNameSet(Set<PlantFeatureId>)`, `findByNameSet`, etc. — no domain-specific methods; mirror insects `FeatureRepository` which is empty); `PlantFeatureTestEntitySource`; `plant-features.json`.

- [ ] **Step 1: Scaffold the test-entity-source.** Invoke `/test-entity-source PlantFeature in plants module`. Populate `plant-features.json` with ≥3 real feature values (e.g. `"ray florets"`, `"opposite leaves"`, `"composite inflorescence"`), each `{"id": "<uuidv7>", "value": "<slug>"}` (no separate name — `PlantFeature`'s natural handle is its surrogate id; follow how the insects `insect-features.json` is shaped). Use the `TestPlantsIdentifiers.PlantFeatures` ids for the two known constants so contract tests can reference them.

- [ ] **Step 2: Scaffold the repository.** Invoke `/entity-repository PlantFeature`. The `FeatureRepository` interface stays empty (only inherited methods), mirroring insects. Ensure the mock + `PlantFeatureEntityRepositoryTest` (three cases per inherited select method) build and pass.

- [ ] **Step 3: Run + commit.** `mvn -pl domains/plants/plants-repository-test -am test` (or the modules the scaffolding touched) → green.
```bash
git add domains/plants
git commit -m "feat(plants): PlantFeature repository + test source + catalog"
```

---

### Task 3: `PlantFeatureAssignment` entity + id + identifiers + invariant test

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/plants/PlantFeatureAssignmentId.java`
- Create: `domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantFeatureAssignment.java`
- Modify: `TestPlantsIdentifiers.java` (add `PlantFeatureAssignments` + `NotFound`)
- Test: `PlantFeatureAssignmentTest.java` + rank-dispatch coverage

**Interfaces:**
- Produces: `PlantFeatureAssignment(PlantFeatureAssignmentId id, PlantFeatureId featureId, PlantRankName rankName, int ordinal)` with `of(...)`; ids + `TestPlantsIdentifiers.PlantFeatureAssignments.<Name>.id` (≥2) + `NotFound`.

- [ ] **Step 1: Create `PlantFeatureAssignmentId`** (EntityId, mirror `PlantFeatureId`).

- [ ] **Step 2: Write the failing test** (`PlantFeatureAssignmentTest`) — valid assignment has no violations; null components report `.id`/`.featureId`/`.rankName`; and a `PlantRankName`-dispatch round-trip (serialize with a rank, deserialize, get the right permit) mirroring the plants `PlantRankNameDispatchTest` / how `InsectFeatureAssignment` is round-tripped. Example valid case:
```java
PlantFeatureAssignment a = PlantFeatureAssignment.of(
        PlantFeatureAssignmentId.create(),
        TestPlantsIdentifiers.PlantFeatures.RayFlorets.id,
        PlantFamilyName.of("asteraceae"),
        0);
assertThat(observer.forMethod("valid").observable(a, "assignment").violations()).isEmpty();
```

- [ ] **Step 3: Run to verify failure.**

- [ ] **Step 4: Implement `PlantFeatureAssignment`** (mirror `InsectFeatureAssignment`, four `@JsonSubTypes` for `PlantRankName`):
```java
public record PlantFeatureAssignment(
        PlantFeatureAssignmentId id,
        PlantFeatureId featureId,
        @JsonTypeInfo(use = Id.NAME, property = "rank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantOrderName.class, name = "ORDER"),
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName rankName,
        int ordinal
) implements Entity<PlantFeatureAssignmentId> {
    public static PlantFeatureAssignment of(PlantFeatureAssignmentId id, PlantFeatureId featureId,
                                            PlantRankName rankName, int ordinal) {
        return new PlantFeatureAssignment(id, featureId, rankName, ordinal);
    }
    @Override public Consumer<? extends Constraints> invariants() {
        return i -> i.entityId(id, "id").entityId(featureId, "featureId").identifier(rankName, "rankName");
    }
}
```
(Confirm the `PlantRankName` permit class names: `PlantOrderName`/`PlantFamilyName`/`PlantGenusName`/`PlantSpeciesName` — grep the `PlantRankName` sealed `permits` clause.)

- [ ] **Step 5: Add `TestPlantsIdentifiers.PlantFeatureAssignments`** (≥2 constants + `NotFound`).

- [ ] **Step 6: Run + commit.**
```bash
git commit -m "feat(plants): PlantFeatureAssignment entity + id"
```

---

### Task 4: `PlantFeatureAssignment` TestEntitySource + JSON + FK + repository (getByRankName/getByRankNames/getByFeatureId)

**Files:**
- Create (via `/test-entity-source`): `PlantFeatureAssignmentTestEntitySource.java` + `plants/feature/plant-feature-assignments.json` + FK to `PlantFeature` + a catalog-data test.
- Create (via `/entity-repository`): `PlantRepository.FeatureAssignmentRepository` + `PlantFeatureAssignmentRepositoryMock.java` + `PlantFeatureAssignmentEntityRepositoryTest.java`.

**Interfaces:**
- Produces: `FeatureAssignmentRepository extends EntityRepository<PlantFeatureAssignmentId, PlantFeatureAssignment>` with:
  - `List<PlantFeatureAssignment> getByRankName(PlantRankName rankName)`
  - `List<PlantFeatureAssignment> getByRankNames(Set<PlantRankName> rankNames)`
  - `List<PlantFeatureAssignment> getByFeatureId(PlantFeatureId featureId)`

- [ ] **Step 1: Scaffold test-source + JSON.** `/test-entity-source PlantFeatureAssignment in plants module`. Populate `plant-feature-assignments.json` with real assignments in the seeded Asterales/Asteraceae/Helianthus chain (e.g. `"ray florets"` at FAMILY `asteraceae`, `"composite inflorescence"` at FAMILY `asteraceae`, `"opposite leaves"` at ORDER `asterales`), each `{"id","featureId","rank":"FAMILY","rankName":"asteraceae","ordinal":0}` — the `rank`+`rankName` external-property shape Jackson needs (mirror `insect-feature-assignments.json`). Add a `ForeignKeyConstraint` from `featureId` → `PlantFeatureTestEntitySource`, and a `PlantFeatureAssignmentCatalogDataTest` asserting every `featureId` referenced exists in the feature catalog (mirror `PlantGenusCatalogDataTest`).

- [ ] **Step 2: Scaffold repository + add the three domain methods.** `/entity-repository PlantFeatureAssignment`. Then add the three methods to the `FeatureAssignmentRepository` interface, and implement them in `PlantFeatureAssignmentRepositoryMock` mirroring `InsectFeatureAssignmentRepositoryMock` — **note the validators**:
```java
@Override public List<PlantFeatureAssignment> getByRankName(PlantRankName rankName) {
    observer().arguments("getByRankName", i -> i.identifier(rankName, "rankName")).throwWhenInvalid();
    return testEntitySource().entityStream().filter(a -> a.rankName().equals(rankName)).toList();
}
@Override public List<PlantFeatureAssignment> getByRankNames(Set<PlantRankName> rankNames) {
    observer().arguments("getByRankNames", i -> i.observableCollection(rankNames, "rankNames")).throwWhenInvalid();
    return testEntitySource().entityStream().filter(a -> rankNames.contains(a.rankName())).toList();
}
@Override public List<PlantFeatureAssignment> getByFeatureId(PlantFeatureId featureId) {
    observer().arguments("getByFeatureId", i -> i.entityId(featureId, "featureId")).throwWhenInvalid();
    return testEntitySource().entityStream().filter(a -> a.featureId().equals(featureId)).toList();
}
```

- [ ] **Step 3: Contract tests** — add to `PlantFeatureAssignmentEntityRepositoryTest` (mirror `InsectFeatureAssignmentEntityRepositoryTest`): `getByRankName_rejectsNull`, `getByRankNames_rejectsNull`, `getByFeatureId_rejectsNull` (all `InvariantViolationException` containing the arg name), plus an expected-result case for `getByRankNames` across two seeded ranks (`.contains(...)` + `.doesNotContain(...)` a seeded-but-unrequested rank + `allSatisfy(a -> a.rankName() in the requested set)`).

- [ ] **Step 4: Run + commit.** `mvn -pl domains/plants/plants-repository-test -am test` → green.
```bash
git commit -m "feat(plants): PlantFeatureAssignment repository + assignments catalog + FK"
```

---

### Task 5: `FeatureCollection` + `PlantQuery.FeatureQuery.forRankName` (the application-layer join)

**Files:**
- Modify: `domains/plants/plants-api/.../PlantEntityCollections.java` (add `FeatureCollection`)
- Modify: `domains/plants/plants-api/.../PlantQuery.java` (add `FeatureQuery features()` + nested `FeatureQuery` interface)
- Create: `domains/plants/plants-core/.../PlantFeatureQueryImpl.java`
- Modify: `domains/plants/plants-core/.../PlantQueryImpl.java` (wire `features()`) + `PlantsTestContext` / any `PlantsTestContextInternal`
- Test: `PlantFeatureQueryImplTest.java`

**Interfaces:**
- Consumes: `FeatureRepository.getByEntityNameSet(Set<PlantFeatureId>)`, `FeatureAssignmentRepository.getByRankName(PlantRankName)`.
- Produces: `PlantEntityCollections.FeatureCollection extends BehavioralCollection<PlantFeature>` (`of`, `empty`); `PlantQuery.FeatureQuery` with `FeatureCollection forRankName(PlantRankName rankName)`; `PlantQuery.features()`.

- [ ] **Step 1: Add `FeatureCollection`** to `PlantEntityCollections` — mirror `InsectEntityCollections.FeatureCollection`:
```java
final class FeatureCollection extends BehavioralCollection<PlantFeature> {
    FeatureCollection(Collection<PlantFeature> features) { super(features); }
    public static FeatureCollection of(Collection<PlantFeature> features) { return new FeatureCollection(features); }
    public static FeatureCollection empty() { return new FeatureCollection(List.of()); }
}
```

- [ ] **Step 2: Write the failing query test** `PlantFeatureQueryImplTest` — wire the impl against seeded mocks (mirror how `PlantImageQueryImpl`/insects tests wire), and assert `forRankName(asteraceae family)` returns exactly the features assigned at that rank:
```java
FeatureCollection result = featureQuery.forRankName(PlantFamilyName.of("asteraceae"));
assertThat(result.stream().map(PlantFeature::value))
        .containsExactlyInAnyOrder("ray florets", "composite inflorescence");   // the seeded FAMILY assignments

// null-rejection
assertThat(catchThrowable(() -> featureQuery.forRankName(null)))
        .isInstanceOf(InvariantViolationException.class);
```
(Confirm the seeded FAMILY-rank feature values against `plant-feature-assignments.json` + `plant-features.json`; adjust the `containsExactlyInAnyOrder` to the real seeded set.)

- [ ] **Step 3: Add the port.** In `PlantQuery.java`, add `FeatureQuery features();` and:
```java
interface FeatureQuery {
    /** Field marks assigned DIRECTLY at the given rank (no ancestry walk — that is a later slice). */
    FeatureCollection forRankName(PlantRankName rankName);
}
```

- [ ] **Step 4: Implement `PlantFeatureQueryImpl`** — the application-layer join: **one** assignment fetch, **one** feature fetch, joined in memory. NO per-element repository call.
```java
class PlantFeatureQueryImpl implements PlantQuery.FeatureQuery {
    private final Observer observer = Observer.forClass(getClass());
    private final PlantRepository.FeatureRepository featureRepository;
    private final PlantRepository.FeatureAssignmentRepository assignmentRepository;
    // constructor with observer.arguments(...).notNull(...).throwWhenInvalid()

    @Override
    public FeatureCollection forRankName(PlantRankName rankName) {
        observer.arguments("forRankName", i -> i.identifier(rankName, "rankName")).throwWhenInvalid();
        List<PlantFeatureAssignment> assignments = assignmentRepository.getByRankName(rankName);   // 1 batched call
        if (assignments.isEmpty()) return FeatureCollection.empty();
        Set<PlantFeatureId> ids = assignments.stream()
                .sorted(Comparator.comparingInt(PlantFeatureAssignment::ordinal))
                .map(PlantFeatureAssignment::featureId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<PlantFeature> features = featureRepository.getByEntityNameSet(ids).stream().toList();  // 1 batched call
        return FeatureCollection.of(features);
    }
}
```
(Ordinal ordering: since `getByEntityNameSet` doesn't preserve request order, if the template needs conspicuous→diagnostic ordering, sort `features` by the assignment ordinal via a resolved-map lookup — mirror the ordering discipline in `InsectFeatureQueryImpl`. For S3's flat `FeatureCollection`, confirm whether the consumer needs ordering; if not, keep it simple.)

- [ ] **Step 5: Wire `features()`** in `PlantQueryImpl` (construct `PlantFeatureQueryImpl` with the two repositories, expose via `features()`), and in `PlantsTestContext` (+ `PlantsTestContextInternal` if present).

- [ ] **Step 6: Run + commit.** `mvn -pl domains/plants/plants-core -am test -Dtest=PlantFeatureQueryImplTest` then the plants modules → green.
```bash
git commit -m "feat(plants): FeatureCollection + direct-rank features() query (app-layer join)"
```

---

## Self-Review

**Spec coverage (evidence-stack S3):** `PlantFeature` (Task 1–2), `PlantFeatureAssignment` (Task 3–4), repositories + mocks + contracts (Task 2, 4), `getByRankNames` batched sibling (Task 4, per the user's decision), test sources + JSON + FK + catalog-data test (Task 2, 4), `FeatureCollection` + `features()` query (Task 5). ✓ Lineage composite + `Plant` read model deferred (stated in Architecture). ✓

**Placeholder scan:** The "confirm against `InsectFeature`/`PlantRankName`/the seeded JSON" notes are verification instructions with concrete fallbacks, not placeholders. The two scaffolding-skill invocations (`/test-entity-source`, `/entity-repository`) generate real code the plan then fills in — the domain-specific method bodies and JSON shapes are spelled out.

**Type consistency:** `PlantFeature(PlantFeatureId, String)`, `PlantFeatureAssignment(PlantFeatureAssignmentId, PlantFeatureId, PlantRankName, int)`, `FeatureRepository`/`FeatureAssignmentRepository`, `FeatureCollection`, and `FeatureQuery.forRankName(PlantRankName) → FeatureCollection` are used consistently across tasks. `getByRankNames(Set<PlantRankName>)` validates with `observableCollection`; single-rank with `identifier`.

**Architectural invariant honored:** `forRankName` composes with exactly two single batched invocations (application-layer join), never a per-element fetch — the load-bearing "server-side joins, single query per entity" principle.

**Note for the executor:** confirm the exact `@UniqueValue`/`uniqueConstraints()` mechanism and the `EntityId` scaffolding (`create()`/`@JsonCreator of`) against `InsectFeature`/`PlantImageId` before writing; and confirm `PlantRankName`'s four permit class names via its `permits` clause.
