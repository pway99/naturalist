# Organism Feature Extraction — Slice A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the byte-for-byte-parallel insect/plant *feature-assignment* and *feature-view* stacks with three generic types in `kernels/taxonomy`, driven by one shared pure assembler — leaving each domain's behaviour and tests green.

**Architecture:** Introduce `OrganismFeatureAssignment<ID, FID, RANK>`, `OrganismFeatureView<RANK, FEATURE>` (+ nested `RankGroup`), and a pure `FeatureViewAssembler` in `kernels/taxonomy`. Each domain drops its own `*FeatureAssignment` / `*FeatureView` records and re-parameterises the generics **bare** at every use site — exactly how both domains already consume `OrganismImage<…>`. The `rankName` discriminator switches from the per-domain `@JsonSubTypes` permit list to the kernel's existing `RankNameSerializer`/`RankNameDeserializer` (self-describing `{rank,value}`), the same mechanism `OrganismImage` uses.

**Tech Stack:** Java records + sealed `RankName`; Jackson (`RankNameSerializer`/`Deserializer`/`Reconstructor`); JUnit + the `Observer`/`MethodObserver` invariant harness; the in-memory `NaturalistDatabase`/`TestEntitySource` test stack; JTE console templates; Maven reactor with the OpenRewrite `EnforceArchitecture` gate.

**Design memo:** [2026-08-24-organism-feature-kernel-extraction-design.md](2026-08-24-organism-feature-kernel-extraction-design.md). This plan implements **Slice A only** (type + query extraction). Slice B (similar-feature dedup) and Slice C (reconcile duplicates) are separate plans. Slice 0 (delete dead `FeatureCollection`) is folded into Task 1 here.

## Global Constraints

- **Kernel home:** `kernels/taxonomy`, package `com.naturalist.taxonomy`. (Resolved 2026-08-24. `RankNameSerializer`/`Deserializer`/`RankNameReconstructor` and `RankName`/`RankAncestry` already live here; no new dependency edge.)
- **Assignment key is `RANK extends RankName`, never `Clade`.** (Resolved — see memo.)
- **Naming:** `OrganismFeatureAssignment`, `OrganismFeatureView` (with nested `RankGroup`), `FeatureViewAssembler`. Consistent with the `Organism*` evidence family (`OrganismObservation`, `OrganismImage`).
- **The `OrganismFeature` *record* is NOT extracted in Slice A.** `InsectFeature` / `PlantFeature` stay as per-domain records; the view carries them via a free `FEATURE` type param. Extracting the trivial feature record is explicitly deferred (low value; see memo eval).
- **Never weaken a test or gate to make code pass** (CLAUDE.md). The extraction must keep the N+1 select-count gate and `EnforceArchitecture`/`EnforceQueryHygiene` green — preserve the existing two-batch fetch in every query impl.
- **Kernel signature changes need a clean install** before verify: `mvn install -DskipTests` from the repo root, then the scoped/`verify` build. Incremental `-pl -am` leaves stale classes and produces `NoSuchMethodError` far from the cause.
- **Completeness gate (run before declaring done):** `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
- **Insects is the reference domain.** Implement it first (Tasks 1–3); plants (Tasks 4–5) mirrors it.

---

## File Structure

**New (Task 1) — `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/`:**
- `OrganismFeatureAssignment.java` — generic record binding a feature id to a rank at an ordinal.
- `OrganismFeatureView.java` — generic read model + nested `RankGroup`.
- `FeatureViewAssembler.java` — pure static helper: (subject, ordered ancestry, assignments, resolved-feature map) → view.

**Modified per domain (`X` = `Insect` | `Plant`; `x` = `insects` | `plants`):**
- `domains/x/x-api/.../XFeatureAssignment.java` — **deleted**; call sites use `OrganismFeatureAssignment<XFeatureAssignmentId, XFeatureId, XRankName>`.
- `domains/x/x-api/.../XFeatureView.java` — **deleted**; call sites use `OrganismFeatureView<XRankName, XFeature>`.
- `domains/x/x-api/.../XRepository.java` — nested `FeatureAssignmentRepository` re-typed to the generic.
- `domains/x/x-api/.../XQuery.java` — `FeatureQuery.findByRankName` returns the generic view.
- `domains/x/x-api/.../XEntityCollections.java` — **delete** the dead `FeatureCollection` (Slice 0).
- `domains/x/x-core/.../XFeatureQueryImpl.java` — assembly delegated to `FeatureViewAssembler`.
- `domains/x/x-core/.../XFeatureAssignmentCommandImpl.java` — insects only; re-typed.
- `domains/x/x-repository-test/.../XFeatureAssignmentRepositoryMock.java` — re-typed.
- `domains/x/x-repository-test/.../XFeatureAssignmentTestEntitySource.java` — re-typed + `RankNameReconstructor` registration.
- `domains/x/x-repository-test/.../XFeatureAssignment*RepositoryTest.java` — re-typed.
- `domains/x/x-repository-test/src/main/resources/x/x-feature-assignments.json` — reformat `rank` → `{rank,value}`.
- `domains/x/x-repository-rdms/.../XFeatureAssignmentRepositoryRdms.java` — re-typed.
- `domains/x/x-console/.../XsController.java` + `.../x/features.jte` (and rank-page templates that `@param` the view) — re-typed.

---

## Task 1: Generic kernel types + pure assembler (`kernels/taxonomy`)

**Files:**
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/OrganismFeatureAssignment.java`
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/OrganismFeatureView.java`
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/FeatureViewAssembler.java`
- (No kernel test — kernels are tested through their first consumer; Task 2 is that consumer.)

**Interfaces:**
- Consumes: `com.naturalist.ddd.{Entity,EntityId}`, `com.naturalist.observability.Constraints`, `com.naturalist.taxonomy.{RankName,RankNameSerializer,RankNameDeserializer}`.
- Produces:
  - `OrganismFeatureAssignment<ID extends EntityId, FID extends EntityId, RANK extends RankName>(ID id, FID featureId, RANK rankName, int ordinal)` with `static of(ID,FID,RANK,int)`.
  - `OrganismFeatureView<RANK extends RankName, FEATURE>(RANK subject, List<RankGroup<RANK,FEATURE>> groups)` and nested `RankGroup<RANK extends RankName, FEATURE>(RANK rank, List<FEATURE> features)`.
  - `FeatureViewAssembler.assemble(RANK subject, Set<RANK> ancestry, List<OrganismFeatureAssignment<?, FID, RANK>> assignments, Map<FID, FEATURE> resolved) -> OrganismFeatureView<RANK,FEATURE>` (static; `ancestry` iteration order = group order; features ordinal-sorted; empty ranks and unresolved feature ids dropped).

- [ ] **Step 1: Write `OrganismFeatureAssignment.java`**

```java
package com.naturalist.taxonomy;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityId;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Binds a diagnostic {@code feature} to a taxonomic {@code rankName} at an
 * {@code ordinal} position — the shared feature-attachment unit across organism
 * domains. {@code featureId} references the domain's own feature record
 * (e.g. {@code InsectFeature}); the feature text is not stored here.
 *
 * <p>{@code rankName} is a domain permit ({@code InsectFamilyName}, …) widened to
 * {@link RankName}; it serialises as a self-describing {@code {"rank":…,"value":…}}
 * object and rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see each domain's feature-assignment test source).
 */
public record OrganismFeatureAssignment<ID extends EntityId, FID extends EntityId, RANK extends RankName>(
        ID id,
        FID featureId,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        RANK rankName,
        int ordinal
) implements Entity<ID> {

    public static <ID extends EntityId, FID extends EntityId, RANK extends RankName>
    OrganismFeatureAssignment<ID, FID, RANK> of(ID id, FID featureId, RANK rankName, int ordinal) {
        return new OrganismFeatureAssignment<>(id, featureId, rankName, ordinal);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityId(featureId, "featureId")
                .identifier(rankName, "rankName");
    }
}
```

- [ ] **Step 2: Write `OrganismFeatureView.java`**

```java
package com.naturalist.taxonomy;

import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

/**
 * The feature marks that apply to a taxon, grouped by the ancestor rank that
 * contributed them, ancestor-first. {@code FEATURE} is the domain's own feature
 * record; the view holds it structurally without constraining its type.
 */
public record OrganismFeatureView<RANK extends RankName, FEATURE>(
        RANK subject,
        List<RankGroup<RANK, FEATURE>> groups
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(groups, "groups");
    }

    /** The feature marks contributed at one rank in the lineage, ordinal-ordered. */
    public record RankGroup<RANK extends RankName, FEATURE>(
            RANK rank,
            List<FEATURE> features
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

- [ ] **Step 3: Write `FeatureViewAssembler.java`** (the shared assembly logic, pure — no repository dependency; the domain query impl does the two batched fetches and hands the results in)

```java
package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds an {@link OrganismFeatureView} from pre-fetched assignments and features.
 * Pure and repository-free so it can live in the kernel: the caller performs the
 * two batched reads (assignments across the ancestry, features by id) and passes
 * the results. Group order follows {@code ancestry} iteration order (ancestor-first);
 * within a rank, features are ordinal-sorted; empty ranks and unresolved ids drop out.
 */
public final class FeatureViewAssembler {

    private FeatureViewAssembler() {
    }

    public static <RANK extends RankName, FID extends EntityId, FEATURE>
    OrganismFeatureView<RANK, FEATURE> assemble(
            RANK subject,
            Set<RANK> ancestry,
            List<? extends OrganismFeatureAssignment<?, FID, RANK>> assignments,
            Map<FID, FEATURE> resolved) {

        Map<RANK, List<OrganismFeatureAssignment<?, FID, RANK>>> byRank = new java.util.HashMap<>();
        for (OrganismFeatureAssignment<?, FID, RANK> a : assignments) {
            byRank.computeIfAbsent(a.rankName(), k -> new ArrayList<>()).add(a);
        }

        List<OrganismFeatureView.RankGroup<RANK, FEATURE>> groups = new ArrayList<>();
        for (RANK rank : ancestry) {
            List<OrganismFeatureAssignment<?, FID, RANK>> atRank = byRank.getOrDefault(rank, List.of());
            if (atRank.isEmpty()) continue;
            List<FEATURE> features = atRank.stream()
                    .sorted(Comparator.comparingInt(OrganismFeatureAssignment::ordinal))
                    .map(a -> resolved.get(a.featureId()))
                    .filter(Objects::nonNull)
                    .toList();
            if (!features.isEmpty()) {
                groups.add(new OrganismFeatureView.RankGroup<>(rank, features));
            }
        }
        return new OrganismFeatureView<>(subject, List.copyOf(groups));
    }
}
```

- [ ] **Step 4: Delete the dead `FeatureCollection` (Slice 0).** Remove `FeatureCollection` from `domains/insects/insects-api/.../InsectEntityCollections.java` (L80-92) and `domains/plants/plants-api/.../PlantEntityCollections.java` (L106-118). Both have zero references repo-wide. *Confirm the plants one is safe to delete first — the design note flags it was previously "kept at user request."*

- [ ] **Step 5: Clean-install and verify the kernel compiles**

Run: `mvn install -DskipTests -pl kernels/taxonomy -am`
Expected: BUILD SUCCESS. (No kernel tests added — first-consumer convention.)

- [ ] **Step 6: Commit**

```bash
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/OrganismFeatureAssignment.java \
        kernels/taxonomy/src/main/java/com/naturalist/taxonomy/OrganismFeatureView.java \
        kernels/taxonomy/src/main/java/com/naturalist/taxonomy/FeatureViewAssembler.java \
        domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java \
        domains/plants/plants-api/src/main/java/com/naturalist/plants/PlantEntityCollections.java
git commit -m "feat(taxonomy): add generic OrganismFeatureAssignment/View + assembler; drop dead FeatureCollection"
```

---

## Task 2: Migrate insects api / core / repository (not console)

**Files:**
- Delete: `domains/insects/insects-api/.../InsectFeatureAssignment.java`, `.../InsectFeatureView.java`
- Modify: `.../InsectRepository.java` (FeatureAssignmentRepository port), `.../InsectQuery.java` (FeatureQuery), `.../InsectFeatureQueryImpl.java`, `.../InsectFeatureAssignmentCommandImpl.java`
- Modify: `insects-repository-test/.../InsectFeatureAssignmentRepositoryMock.java`, `.../InsectFeatureAssignmentTestEntitySource.java`, `.../InsectFeatureAssignmentEntityRepositoryTest.java`
- Modify: `insects-repository-rdms/.../InsectFeatureAssignmentRepositoryRdms.java`
- Modify: `insects-repository-test/src/main/resources/insects/insect-feature-assignments.json`

**Interfaces:**
- Consumes: Task 1's `OrganismFeatureAssignment`, `OrganismFeatureView`, `FeatureViewAssembler`.
- Produces: `InsectQuery.FeatureQuery.findByRankName(InsectRankName) -> OrganismFeatureView<InsectRankName, InsectFeature>`; the nested port `getByRankNames(Set<InsectRankName>) -> List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>`.

Throughout this task, the concrete alias is:
`OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>` and
`OrganismFeatureView<InsectRankName, InsectFeature>`.

- [ ] **Step 1: Re-type the repository port.** In `InsectRepository.java`, change the nested `FeatureAssignmentRepository`:

```java
protected interface FeatureAssignmentRepository
        extends EntityRepository<InsectFeatureAssignmentId,
                                 OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> {

    List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByRankName(InsectRankName rankName);
    List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByRankNames(Set<InsectRankName> rankNames);
    List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> getByFeatureId(InsectFeatureId featureId);
}
```

Add `import com.naturalist.taxonomy.OrganismFeatureAssignment;`. Delete `InsectFeatureAssignment.java`.

- [ ] **Step 2: Re-type the query port.** In `InsectQuery.java`, `FeatureQuery.findByRankName` returns `OrganismFeatureView<InsectRankName, InsectFeature>` (keep `findByFeature(InsectFeatureId) -> Set<InsectRankName>`). Delete `InsectFeatureView.java`; add `import com.naturalist.taxonomy.OrganismFeatureView;`.

- [ ] **Step 3: Rewrite the query impl to call the assembler.** In `InsectFeatureQueryImpl.java`, replace the body of `findByAncestry` — keep the two batched fetches, delegate grouping to the kernel:

```java
OrganismFeatureView<InsectRankName, InsectFeature> findByAncestry(InsectRankName subject, Set<InsectRankName> ancestry) {
    observer.arguments("findByAncestry", i -> i
                    .identifier(subject, "subject")
                    .observableCollection(ancestry, "ancestry"))
            .throwWhenInvalid();

    List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>> assignments =
            assignmentRepository.getByRankNames(ancestry);   // batch 1

    Set<InsectFeatureId> allIds = assignments.stream()
            .map(OrganismFeatureAssignment::featureId).collect(Collectors.toSet());
    Map<InsectFeatureId, InsectFeature> resolved = new HashMap<>();
    if (!allIds.isEmpty()) {
        for (InsectFeature f : featureRepository.getByEntityNameSet(allIds)) {   // batch 2
            resolved.put(f.id(), f);
        }
    }

    OrganismFeatureView<InsectRankName, InsectFeature> view =
            FeatureViewAssembler.assemble(subject, ancestry, assignments, resolved);
    observer.observable(view, "featureView").observe(Level.WARN);
    return view;
}
```

Return-type-update `findByRankName` accordingly; `findByFeature` maps `OrganismFeatureAssignment::rankName`. Add imports for `OrganismFeatureAssignment`, `OrganismFeatureView`, `FeatureViewAssembler`.

- [ ] **Step 4: Re-type command impl, mock, and RDMS adapter.** In `InsectFeatureAssignmentCommandImpl.java`, `InsectFeatureAssignmentRepositoryMock.java`, and `InsectFeatureAssignmentRepositoryRdms.java`, replace every `InsectFeatureAssignment` type reference with the generic alias. Constructor calls `InsectFeatureAssignment.of(...)` become `OrganismFeatureAssignment.of(...)`. Preserve any `observer().arguments(...).throwWhenInvalid()` validation on domain-specific mock finders.

- [ ] **Step 5: Register the RankName reconstructor on the test source.** In `InsectFeatureAssignmentTestEntitySource.java`, build the mapper exactly like `InsectImageTestEntitySource`:

```java
private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
        .setInjectableValues(new InjectableValues.Std()
                .addValue(RankNameReconstructor.class, (RankNameReconstructor) InsectRankName::of));
```

Re-type the source's element type to the generic alias; add imports (`RankNameReconstructor`, `OrganismFeatureAssignment`).

- [ ] **Step 6: Reformat the JSON catalog.** In `insect-feature-assignments.json`, migrate each element's rank encoding from the external-property form to the embedded self-describing form the serializer now expects. Change every entry of the shape:

```json
{ "id": "…", "featureId": "…", "rank": "FAMILY", "rankName": "carabidae", "ordinal": 0 }
```

to:

```json
{ "id": "…", "featureId": "…", "rankName": { "rank": "FAMILY", "value": "carabidae" }, "ordinal": 0 }
```

(Match the exact `{rank,value}` shape emitted by `RankNameSerializer` — verify against a serialized `InsectImage` entry in `insect-images.json`. Preserve every id/featureId/ordinal value unchanged.)

- [ ] **Step 7: Re-type the contract test.** In `InsectFeatureAssignmentEntityRepositoryTest.java`, update the entity type parameter to the generic alias and any `new InsectFeatureAssignment(...)` / `.of(...)` builders to `OrganismFeatureAssignment`.

- [ ] **Step 8: Clean-install and run the insects module tests**

Run: `mvn install -DskipTests && mvn verify -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-repository-test,domains/insects/insects-repository-rdms`
Expected: BUILD SUCCESS. Confirm the N+1 gate stays green (the two-batch fetch is preserved) and the JSON round-trip (`InsectFeatureAssignmentTestEntitySource` load) deserialises the new `{rank,value}` form.

- [ ] **Step 9: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core \
        domains/insects/insects-repository-test domains/insects/insects-repository-rdms
git commit -m "refactor(insects): re-parameterise feature assignment/view on generic taxonomy types"
```

---

## Task 3: Migrate insects console (controller + templates)

**Files:**
- Modify: `domains/insects/insects-console/.../InsectsController.java`
- Modify: `domains/insects/insects-console/.../insects/features.jte` and any rank-page template (`order/family/genus/detail.jte`) that declares a `@param` of the feature view.

**Interfaces:**
- Consumes: `InsectQuery.FeatureQuery.findByRankName(...) -> OrganismFeatureView<InsectRankName, InsectFeature>` (Task 2).

- [ ] **Step 1: Re-type the controller.** Replace `InsectFeatureView` references in `InsectsController.java` with `OrganismFeatureView<InsectRankName, InsectFeature>`; add `import com.naturalist.taxonomy.OrganismFeatureView;`. Accessor calls (`.subject()`, `.groups()`, `group.rank()`, `group.features()`) are unchanged.

- [ ] **Step 2: Re-type the JTE `@param`s.** In `features.jte` (and any rank template that takes the view), change the parameter type declaration from `com.naturalist.insects.InsectFeatureView` to `com.naturalist.taxonomy.OrganismFeatureView<com.naturalist.identifiers.…InsectRankName, com.naturalist.insects.InsectFeature>` (use fully-qualified types in the `@param` line; the loop body over `.groups()` / `.features()` / `feature.value()` is unchanged). Verify against the shared-layout classpath rule — `features.jte` is domain-local, so this is safe (unlike `page.jte`).

- [ ] **Step 3: Clean-install and run the console module**

Run: `mvn install -DskipTests && mvn verify -pl apps/management-console`
Expected: BUILD SUCCESS (JTE compiles; console `@SpringBootTest`s start). If a JTE "cannot find symbol" surfaces, it is the stale-jar trap — the preceding `mvn install` resolves it.

- [ ] **Step 4: Visually verify a rank page renders features** (preview the console, open an insect family page, confirm the feature list renders grouped by rank). Skip only if no console change is observable.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console
git commit -m "refactor(insects-console): render features off generic OrganismFeatureView"
```

---

## Task 4: Migrate plants api / core / repository (mirror of Task 2)

Plants mirrors Task 2 with the substitution table below. **Differences from insects, apply exactly:**
- **4 rank permits** (no `SUBSPECIES`) — irrelevant now (the `@JsonSubTypes` list is gone; the serializer is permit-agnostic).
- **No command impl** — plants has no `PlantFeatureAssignmentCommandImpl`; skip that step.
- **`@DomainService` on the query impl** — `PlantFeatureQueryImpl` carries `@DomainService`; insects' does not. Keep it as-is for now (do **not** add/remove markers in this task); reconciliation is Task 6, open-decision 2. Verify the console `@SpringBootTest`s still start after the change.
- **No `findByFeature`** — `PlantQuery.FeatureQuery` has only `findByRankName`. Do not add the reverse lookup here (Task 6 decides that).
- **Extra test classes** — plants additionally has `PlantFeatureAssignmentRepositoryMockTest`, `PlantFeatureAssignmentTestEntitySourceTest`, `PlantFeatureAssignmentCatalogDataTest`, and an api-level `PlantFeatureAssignmentTest`. Re-type the generic alias in each; the `CatalogDataTest` asserts against the JSON, so update its expectations to the `{rank,value}` form.

**Substitution table (Task 2 → Task 4):**

| Task 2 (insects) | Task 4 (plants) |
|---|---|
| `InsectFeatureAssignmentId` | `PlantFeatureAssignmentId` |
| `InsectFeatureId` | `PlantFeatureId` |
| `InsectRankName` | `PlantRankName` |
| `InsectFeature` | `PlantFeature` |
| `InsectRepository` | `PlantRepository` |
| `InsectQuery` | `PlantQuery` |
| `InsectFeatureQueryImpl` | `PlantFeatureQueryImpl` |
| `InsectFeatureAssignmentTestEntitySource` | `PlantFeatureAssignmentTestEntitySource` |
| `insect-feature-assignments.json` | `plant-feature-assignments.json` |
| reconstructor `InsectRankName::of` | `PlantRankName::of` |

**Concrete aliases for this task:**
`OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>` and
`OrganismFeatureView<PlantRankName, PlantFeature>`.

- [ ] **Step 1:** Re-type `PlantRepository.FeatureAssignmentRepository` (three finders) to the plant alias; delete `PlantFeatureAssignment.java`.
- [ ] **Step 2:** Re-type `PlantQuery.FeatureQuery.findByRankName` → `OrganismFeatureView<PlantRankName, PlantFeature>`; delete `PlantFeatureView.java`.
- [ ] **Step 3:** Rewrite `PlantFeatureQueryImpl.findByAncestry` to the assembler-delegating body (see Task 2 Step 3, with plant types); **keep `@DomainService`**.
- [ ] **Step 4:** Re-type the mock and RDMS adapter to the plant alias (no command impl).
- [ ] **Step 5:** Register `RankNameReconstructor` (`PlantRankName::of`) on `PlantFeatureAssignmentTestEntitySource` mapper (mirror `PlantImageTestEntitySource`).
- [ ] **Step 6:** Reformat `plant-feature-assignments.json` to the `{rank,value}` form (see Task 2 Step 6).
- [ ] **Step 7:** Re-type all plant feature-assignment tests (contract, mock, test-source, catalog-data, api record test) to the generic alias; update `CatalogDataTest` expectations to the new JSON shape.
- [ ] **Step 8: Clean-install and run the plants modules**

Run: `mvn install -DskipTests && mvn verify -pl domains/plants/plants-api,domains/plants/plants-core,domains/plants/plants-repository-test,domains/plants/plants-repository-rdms`
Expected: BUILD SUCCESS; N+1 gate green; JSON round-trip loads.

- [ ] **Step 9: Commit**

```bash
git add domains/plants/plants-api domains/plants/plants-core \
        domains/plants/plants-repository-test domains/plants/plants-repository-rdms
git commit -m "refactor(plants): re-parameterise feature assignment/view on generic taxonomy types"
```

---

## Task 5: Migrate plants console (mirror of Task 3)

**Files:**
- Modify: `domains/plants/plants-console/.../PlantsController.java`
- Modify: `domains/plants/plants-console/.../plants/features.jte` and any rank-page template (`orders/families/genera/detail.jte`, `detail.jte`) declaring the feature-view `@param`.

- [ ] **Step 1:** Replace `PlantFeatureView` in `PlantsController.java` with `OrganismFeatureView<PlantRankName, PlantFeature>`; add the import.
- [ ] **Step 2:** Re-type the JTE `@param`s to the fully-qualified generic (see Task 3 Step 2, plant types).
- [ ] **Step 3: Clean-install and run the console**

Run: `mvn install -DskipTests && mvn verify -pl apps/management-console`
Expected: BUILD SUCCESS.

- [ ] **Step 4:** Visually verify a plant rank page renders its features grouped by rank.
- [ ] **Step 5: Commit**

```bash
git add domains/plants/plants-console
git commit -m "refactor(plants-console): render features off generic OrganismFeatureView"
```

---

## Task 6: Reconcile asymmetries + full-gate verification

**Files:** (decision-dependent — see below) potentially `InsectQuery.java`/`InsectFeatureQueryImpl.java` (drop or keep `findByFeature`), `PlantQuery.java`/`PlantFeatureQueryImpl.java` (`@DomainService` marker; optional reverse lookup), and any artifact-symmetry additions.

**Interfaces:** none new — this task only normalises and gates.

- [ ] **Step 1: Decide the reverse-lookup contract.** Design memo open-decision 3: insects has `findByFeature(featureId) -> Set<RankName>`, plants does not. **Recommended:** add the symmetric method to `PlantQuery.FeatureQuery` + `PlantFeatureQueryImpl` (plants' repo already has `getByFeatureId`), so both domains match. If the reviewer prefers minimal surface, instead drop insects' `findByFeature` only if it has no caller — grep first:

Run: `grep -rn "findByFeature" domains --include="*.java"`
Act on the result: keep + mirror to plants (recommended) or remove if unused.

- [ ] **Step 2: Resolve the `@DomainService` discrepancy.** `PlantFeatureQueryImpl` has `@DomainService`, `InsectFeatureQueryImpl` does not. Pick one convention. **Caution:** a factory-backed query carrying `@DomainService` breaks app-context startup and reds every console `@SpringBootTest` (known landmine). Determine whether these query impls are Spring-instantiated or factory-backed, align to the safe convention, and prove it with the console `@SpringBootTest`s.

- [ ] **Step 3: Normalise artifact sets (optional, reviewer's call).** Insects lacks the three repo-test test classes plants has (`RepositoryMockTest`, `TestEntitySourceTest`, `CatalogDataTest`) and the api record test; plants lacks the core command impl. Add the missing coverage on the insects side if the reviewer wants symmetry; otherwise log the asymmetry and move on (do not add production code without a consumer).

- [ ] **Step 4: Run the full build + completeness gate**

Run: `mvn install -DskipTests && mvn verify`
Then: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS on both; `EnforceArchitecture` (incl. `EnforceQueryHygiene`/`NoSelectInIteration`) reports no pending fix or marker.

- [ ] **Step 5: Grep for orphans.** Confirm no dangling references to the deleted types:

Run: `grep -rn "InsectFeatureView\|PlantFeatureView\|InsectFeatureAssignment\b\|PlantFeatureAssignment\b\|FeatureCollection" domains kernels apps --include="*.java" --include="*.jte"`
Expected: only `OrganismFeatureAssignment`/`OrganismFeatureView` and the `*Id`/`*QueryImpl`/`*Mock`/`*TestEntitySource`/`*RepositoryRdms` names remain; zero hits for the deleted record types and `FeatureCollection`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(features): reconcile feature-query asymmetries; pass full architecture gate"
```

---

## Self-Review

**Spec coverage (design memo Slice A):**
- Unify `FeatureAssignment` → Task 1 (`OrganismFeatureAssignment`) + Tasks 2/4 (migrate).
- Unify `FeatureView`/`RankGroup` → Task 1 (`OrganismFeatureView`) + Tasks 2/4.
- Unify query assembly → Task 1 (`FeatureViewAssembler`) + Tasks 3-step-3 / 4-step-3 (delegate).
- Jackson permit-list resolution → Task 1 (serializer annotations) + Tasks 2/4 Steps 5-6 (reconstructor + JSON reformat).
- Nested repository ports → Tasks 2/4 Step 1.
- Slice 0 dead-`FeatureCollection` cleanup → Task 1 Step 4.
- Artifact/`@DomainService`/reverse-lookup asymmetries (open decisions 2-4) → Task 6.
- `getByEntityNameSet` naming (open decision 5) → left as-is (opportunistic; not required).

**Placeholder scan:** all code steps carry concrete bodies; the plants mirror uses an explicit substitution table + named differences rather than "same as Task 2".

**Type consistency:** the two aliases `OrganismFeatureAssignment<…Id, …FeatureId, …RankName>` and `OrganismFeatureView<…RankName, …Feature>` are used identically in every task; `FeatureViewAssembler.assemble(subject, ancestry, assignments, resolved)` signature matches its call sites in Tasks 2/4 Step 3; accessor names (`.rankName()`, `.featureId()`, `.ordinal()`, `.subject()`, `.groups()`, `.rank()`, `.features()`) are unchanged from the deleted per-domain records, so console/JTE bodies need no logic change.
