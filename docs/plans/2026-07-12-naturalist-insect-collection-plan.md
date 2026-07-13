# Naturalist Insect Collection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let each naturalist build a collection of insects by recording field observations (with optional photo evidence), and browse the full catalog or filter to just the insects they've observed.

**Architecture:** A new `FieldObservation` entity (surrogate-id `Entity`, the collection unit) lives in the insects domain with a read query (`forNaturalist`) and write command, mirroring `InsectImage`. `InsectImage` gains a nullable `observationId` linking a photo to the observation it evidences. The current naturalist reaches `insects-console` via a request attribute published by the app's header interceptor (no app/security dependency in the domain console). The species browse gains an "All / My collection" toggle.

**Tech Stack:** Java 21 records, Spring Boot 3 / Spring Security 6, JTE, the kernel framework (`Entity`, `EntityId`, `EntityQuery`/`AbstractEntityQuery`, `EntityCommand`/`AbstractEntityCommand`, `EntityRepository`, `BehavioralCollection`, `TestEntitySource`), `InsectsTestContext` assembly.

## Global Constraints

- **Design source of truth:** `docs/plans/2026-07-12-naturalist-insect-collection-design.md`.
- **Naming (avoid the legacy clash):** the `Insect` read model already has `observations()` returning **images**. The new concept is always the full name — `FieldObservation`, `FieldObservationId`, `FieldObservationCollection`, `FieldObservationRepository`, `FieldObservationQuery`, `FieldObservationCommand`, and the namespace accessor `fieldObservations()`. Never shorten to `observations()`.
- **Identity:** `FieldObservation` is an `Entity<FieldObservationId>` (UUIDv7 surrogate). `observedBy: NaturalistName` (cross-`NamedEntity` reference by name; `NaturalistName` is on the `insects-api` classpath via `identifiers`). `subject: InsectRankName` (existing sealed type). `FieldObservationId` lives in `domains/identifiers` (package `com.naturalist.insects`), alongside `InsectImageId` and the other insects `EntityId` subclasses — the identifiers module, NOT insects-api (import is unchanged: `com.naturalist.insects.FieldObservationId`). Never `UUID.randomUUID()` — use `EntityId.newUUID()` via the `create()` factory.
- **Immutability:** `FieldObservation` is a record; equality by value; no setters.
- **Seam discipline:** only `NaturalistUserDetailsService`, `CurrentNaturalist`/`SecurityContextCurrentNaturalist`, and `CurrentNaturalistView` may read `SecurityContextHolder`. The interceptor reads identity through `CurrentNaturalistView`; `insects-console` reads a plain request-attribute String, never Spring-Security.
- **Request-attribute key:** `"naturalist.currentNaturalistName"` — the app interceptor writes it; `insects-console` reads it. Both sides hardcode this literal with a cross-referencing comment (no shared code dependency; same convention as the CSRF attribute key).
- **Builds:** you (the executor) are authorized to run Maven for this execution. Scoped commands like `mvn -q -pl domains/insects/insects-repository-test -am test -Dtest=...`; the final task runs `mvn -q verify` from the repo root.
- **Commits:** stage + commit each task with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` footer.
- **Insects patterns:** query/command impls are package-private classes assembled in `InsectsTestContext` (NOT `@DomainService`). Repository mocks mirror `InsectImageRepositoryMock` (match its annotations exactly — read that file). Contract tests: repository via `EntityRepositoryTest`, command via `EntityCommandContractTest`.

---

## File Structure

**identifiers (`domains/identifiers/src/main/java/com/naturalist/insects/`)**
- `FieldObservationId.java` — new `EntityId` subclass (mirror `InsectImageId`, which lives here too).

**insects-api (`domains/insects/insects-api/src/main/java/com/naturalist/insects/`)**
- `FieldObservation.java` — new record.
- `InsectEntityCollections.java` — **modify**: add `FieldObservationCollection`.
- `InsectRepository.java` — **modify**: add nested `FieldObservationRepository`; add field + `create(...)` param (Task 2).
- `InsectQuery.java` — **modify**: add `fieldObservations()` + `FieldObservationQuery` (Task 2).
- `InsectCommand.java` — **modify**: add `fieldObservations()` + `FieldObservationCommand` (Task 2).
- `InsectImage.java` — **modify**: add nullable `observationId` (Task 3).

**insects-core (`domains/insects/insects-core/src/main/java/com/naturalist/insects/`)**
- `FieldObservationQueryImpl.java` — new (Task 2).
- `FieldObservationCommandImpl.java` — new (Task 2).
- `InsectQueryImpl.java` / `InsectCommandImpl.java` — **modify**: accept + expose the new sub-query/command (Task 2).

**insects-repository-test (`domains/insects/insects-repository-test/`)**
- `src/main/java/.../FieldObservationRepositoryMock.java` — new.
- `src/main/java/.../FieldObservationTestEntitySource.java` — new.
- `src/main/java/.../FieldObservationEntityRepositoryTest.java` — new contract interface.
- `src/main/resources/insects/field-observations.json` — new seed (≥4).
- `src/test/java/.../FieldObservationRepositoryMockTest.java` — new binding.

**insects-test-context**
- `InsectsTestContext.java` — **modify**: wire the observation repo/query/command (Task 2).

**insects-core test**
- `FieldObservationCommandImplTest.java` — new command contract test (Task 2).

**identifiers-test**
- `TestInsectsIdentifiers.java` — **modify**: add `FieldObservation` id constants (Task 1).

**insects-console**
- `InsectsController.java` — **modify**: read current naturalist; `observe` endpoint; `?mine=true`; link photo (Tasks 5–7).
- `src/main/jte/insects/list.jte` (+ `detail.jte`) — **modify**: toggle + collected indicator + observe form (Tasks 5–7).

**management-console (app)**
- `auth/CurrentNaturalistView.java` — **modify**: add `currentNaturalistSlug()` (Task 4).
- `auth/NaturalistHeaderInterceptor.java` — **modify**: publish the naturalist-name attribute (Task 4).
- `src/test/java/...` — new `@SpringBootTest` cases (Tasks 4–7).

---

## Task 1: FieldObservation entity + data layer

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/insects/FieldObservationId.java`
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java` (add nested interface only)
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationTestEntitySource.java`
- Create: `domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationRepositoryMock.java`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationEntityRepositoryTest.java`
- Test: `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/FieldObservationRepositoryMockTest.java`

**Interfaces:**
- Consumes: `Entity`, `EntityId`, `EntityRepository`, `AbstractTestEntityRepository`, `TestEntitySource`, `BehavioralCollection`, `NaturalistDatabase`, `EntityRepositoryTest`, `NaturalistName` (`com.naturalist.naturalist`), `InsectRankName`/`InsectSpeciesName`/`InsectGenusName`/`InsectFamilyName`.
- Produces: `FieldObservation(id, observedBy, subject, observedOn, notes)`; `FieldObservationId` (`create()`, `of(UUID)`); `InsectRepository.FieldObservationRepository` with `List<FieldObservation> getByNaturalist(NaturalistName)`; `InsectEntityCollections.FieldObservationCollection`.

- [ ] **Step 1: Write `FieldObservationId`** (mirror `InsectImageId`)

Create `FieldObservationId.java`:

```java
package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class FieldObservationId extends EntityId {
    private FieldObservationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static FieldObservationId of(UUID value) {
        return new FieldObservationId(value);
    }

    public static FieldObservationId create() {
        return new FieldObservationId(EntityId.newUUID());
    }
}
```

- [ ] **Step 2: Write the `FieldObservation` record**

Create `FieldObservation.java`:

```java
package com.naturalist.insects;

import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A naturalist's field observation of an insect — the unit of a naturalist's collection.
 * <p>
 * Records that a naturalist ({@code observedBy}) encountered an insect at a taxonomic rank
 * ({@code subject}) at a point in time. Photographic evidence is optional and lives on
 * {@link InsectImage} via its {@code observationId} link — an observation needs no photo.
 * "Insects I've collected" is the distinct set of {@code subject}s across a naturalist's
 * observations. Multiple observations of the same subject are allowed (distinct sightings).
 */
public record FieldObservation(
        FieldObservationId id,
        NaturalistName observedBy,
        InsectRankName subject,
        Instant observedOn,
        @Nullable String notes
) implements Entity<FieldObservationId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(observedBy, "observedBy")
                .identifier(subject, "subject")
                .notNull(observedOn, "observedOn");
    }
}
```

- [ ] **Step 3: Add `FieldObservationCollection`**

In `InsectEntityCollections.java`, add this nested class alongside `ImageCollection` (match the surrounding style; `BehavioralCollection`, `Collection`, `List` are already imported):

```java
    final class FieldObservationCollection extends BehavioralCollection<FieldObservation> {

        FieldObservationCollection(Collection<FieldObservation> observations) {
            super(observations);
        }

        public static FieldObservationCollection of(Collection<FieldObservation> observations) {
            return new FieldObservationCollection(observations);
        }

        public static FieldObservationCollection empty() {
            return new FieldObservationCollection(List.of());
        }
    }
```

- [ ] **Step 4: Add the `FieldObservationRepository` nested interface**

In `InsectRepository.java`, add inside the class body (alongside the other `protected interface`s; `NaturalistName` import needed):

```java
    protected interface FieldObservationRepository
            extends EntityRepository<FieldObservationId, FieldObservation> {

        java.util.List<FieldObservation> getByNaturalist(
                com.naturalist.naturalist.NaturalistName observedBy);

        java.util.List<FieldObservation> getByNaturalistAndSubjects(
                com.naturalist.naturalist.NaturalistName observedBy,
                java.util.Set<InsectRankName> subjects);
    }
```

(Leave `InsectRepository`'s fields and `create(...)` unchanged in this task — Task 2 wires it in. The mock is constructed directly by the contract test here.)

- [ ] **Step 5: Add test-id constants**

In `TestInsectsIdentifiers.java`, add a top-level nested class (mirror the existing scoped classes; `UUID` is already imported):

```java
    public static class FieldObservation {

        private FieldObservation() {
        }

        public static class NotFound {
            public static final FieldObservationId id = FieldObservationId.of(
                    UUID.fromString("019e9000-0000-7000-8000-0000deadbeef"));
        }

        public static final FieldObservationId PatrickBattus = FieldObservationId.of(
                UUID.fromString("019e9000-0001-7000-8000-000000000001"));
        public static final FieldObservationId PatrickEmpoasca = FieldObservationId.of(
                UUID.fromString("019e9000-0002-7000-8000-000000000002"));
        public static final FieldObservationId DeliaBattus = FieldObservationId.of(
                UUID.fromString("019e9000-0003-7000-8000-000000000003"));
        public static final FieldObservationId DeliaEmpoasca = FieldObservationId.of(
                UUID.fromString("019e9000-0004-7000-8000-000000000004"));
    }
```

- [ ] **Step 6: Write the seed JSON** (≥4 for the paging contract)

Create `field-observations.json`. `subject` serializes via a `parentRank`/`subject` external-property pair? No — `subject` is a bare `InsectRankName`. Because the record has no `@JsonTypeInfo` on `subject`, provide the concrete rank via the same external-property mechanism `InsectImage` uses. **Add the `@JsonTypeInfo`/`@JsonSubTypes` block to the `subject` component in Step 2's record** (copy it verbatim from `InsectImage.parentName`, changing `property = "subjectRank"`), then the JSON is:

```json
[
  { "id": "019e9000-0001-7000-8000-000000000001", "observedBy": "patrick-way",   "subjectRank": "SPECIES", "subject": "battus-philenor", "observedOn": "2026-04-20T09:00:00Z", "notes": "On the pipevine by the gate." },
  { "id": "019e9000-0002-7000-8000-000000000002", "observedBy": "patrick-way",   "subjectRank": "GENUS",   "subject": "empoasca",        "observedOn": "2026-04-22T14:30:00Z", "notes": null },
  { "id": "019e9000-0003-7000-8000-000000000003", "observedBy": "delia-durrell", "subjectRank": "SPECIES", "subject": "battus-philenor", "observedOn": "2026-05-02T11:00:00Z", "notes": "Shown to the school group." },
  { "id": "019e9000-0004-7000-8000-000000000004", "observedBy": "delia-durrell", "subjectRank": "GENUS",   "subject": "empoasca",        "observedOn": "2026-05-03T10:15:00Z", "notes": null }
]
```

> Update Step 2's record so `subject` carries the polymorphic annotations (verbatim from `InsectImage.parentName`, with `property = "subjectRank"`):
> ```java
>         @com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "subjectRank", include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY)
>         @com.fasterxml.jackson.annotation.JsonSubTypes({
>                 @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = InsectOrderName.class, name = "ORDER"),
>                 @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = InsectFamilyName.class, name = "FAMILY"),
>                 @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = InsectGenusName.class, name = "GENUS"),
>                 @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = InsectSpeciesName.class, name = "SPECIES"),
>                 @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
>         })
>         InsectRankName subject,
> ```
> (Prefer clean top-of-file imports over the fully-qualified form if the imports don't already exist.)

- [ ] **Step 7: Write the test entity source** (mirror `InsectImageTestEntitySource`, FK on `subject` only — `observedBy` is a cross-domain soft reference, not FK-checked)

Create `FieldObservationTestEntitySource.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class FieldObservationTestEntitySource
        extends TestEntitySource<FieldObservationId, FieldObservation> {

    public FieldObservationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/field-observations.json");
    }

    @Override
    protected List<ForeignKeyConstraint<FieldObservation, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "subject (family)",
                        o -> o.subject() instanceof InsectFamilyName f ? f : null,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (genus)",
                        o -> o.subject() instanceof InsectGenusName g ? g : null,
                        InsectGenusTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "subject (species)",
                        o -> o.subject() instanceof InsectSpeciesName s ? s : null,
                        InsectSpeciesTestEntitySource.class));
    }
}
```

- [ ] **Step 8: Write the repository mock** (mirror `InsectImageRepositoryMock` exactly — read that file for the class modifiers / `@DomainService` presence and match it)

Create `FieldObservationRepositoryMock.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.naturalist.NaturalistName;

import java.util.List;
import java.util.Set;

class FieldObservationRepositoryMock
        extends AbstractTestEntityRepository<FieldObservationId, FieldObservation, FieldObservationTestEntitySource>
        implements InsectRepository.FieldObservationRepository {

    FieldObservationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<FieldObservation> getByNaturalist(NaturalistName observedBy) {
        observer().arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy))
                .toList();
    }

    @Override
    public List<FieldObservation> getByNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("getByNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(o -> o.observedBy().equals(observedBy) && subjects.contains(o.subject()))
                .toList();
    }
}
```

> If `InsectImageRepositoryMock` carries `@DomainService` and/or `public`, match it here.

- [ ] **Step 9: Write the failing repository contract test** (mirror `LifeStageEntityRepositoryTest`)

Create `FieldObservationEntityRepositoryTest.java` (`src/main/java`):

```java
package com.naturalist.insects;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

interface FieldObservationEntityRepositoryTest
        extends EntityRepositoryTest<FieldObservationId, FieldObservation> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");

    @Override
    InsectRepository.FieldObservationRepository repository();

    @Override
    default TestEntitySource<FieldObservationId, FieldObservation> source() {
        return db.getNamed(FieldObservationTestEntitySource.class);
    }

    @Override
    default FieldObservationId notFoundName() {
        return TestInsectsIdentifiers.FieldObservation.NotFound.id;
    }

    @Override
    default List<FieldObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.FieldObservation.PatrickBattus,
                TestInsectsIdentifiers.FieldObservation.DeliaBattus);
    }

    @Override
    default FieldObservation newEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-01T08:00:00Z"),
                "new observation");
    }

    @Override
    default FieldObservation ghostEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                PATRICK,
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-02T08:00:00Z"),
                null);
    }

    @Override
    default FieldObservation modifiedEntity(FieldObservation original) {
        return new FieldObservation(
                original.id(),
                DELIA,
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-06-03T08:00:00Z"),
                "changed");
    }

    @Test
    default void getByNaturalist_rejectsNull() {
        assertThatThrownBy(() -> repository().getByNaturalist(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("observedBy");
    }

    @Test
    default void getByNaturalist_returnsOnlyThatNaturalistsObservations() {
        var results = repository().getByNaturalist(PATRICK);
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(o -> o.observedBy().equals(PATRICK));
    }

    @Test
    default void getByNaturalistAndSubjects_matchesNaturalistAndSubject() {
        var results = repository().getByNaturalistAndSubjects(
                PATRICK, java.util.Set.of(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(o -> o.observedBy().equals(PATRICK)
                && o.subject().equals(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
    }

    @Test
    default void getByNaturalistAndSubjects_excludesOtherNaturalists() {
        // delia also observed battus-philenor; patrick's query must not return it.
        var results = repository().getByNaturalistAndSubjects(
                PATRICK, java.util.Set.of(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name));
        assertThat(results).noneMatch(o -> o.observedBy().equals(DELIA));
    }

    @Test
    default void getByNaturalistAndSubjects_rejectsNullNaturalist() {
        assertThatThrownBy(() -> repository().getByNaturalistAndSubjects(null, java.util.Set.of()))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("observedBy");
    }
}
```

Create the binding `FieldObservationRepositoryMockTest.java` (`src/test/java`):

```java
package com.naturalist.insects;

class FieldObservationRepositoryMockTest implements FieldObservationEntityRepositoryTest {
    @Override
    public InsectRepository.FieldObservationRepository repository() {
        return new FieldObservationRepositoryMock(db);
    }
}
```

- [ ] **Step 10: Run — expect FAIL then PASS**

Run: `mvn -q -pl domains/insects/insects-repository-test -am test -Dtest=FieldObservationRepositoryMockTest`
Expected: after Steps 1–9 it PASSES (all inherited `EntityRepositoryTest` cases + the two `getByNaturalist` cases). If you ran it before writing the mock/source, it FAILS to compile (missing symbols) — that is the RED.

- [ ] **Step 11: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-repository-test domains/identifiers-test
git commit -m "feat(insects): add FieldObservation entity + repository (collection unit)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Expose FieldObservation read + write via the namespaces

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java` (add field + `create(...)` param + accessor)
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCommand.java`
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/FieldObservationQueryImpl.java`
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/FieldObservationCommandImpl.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCommandImpl.java`
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/FieldObservationCommandImplTest.java`
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/FieldObservationQueryImplTest.java`

**Interfaces:**
- Consumes: Task 1 types; `AbstractEntityQuery`, `AbstractEntityCommand`, `EntityCommandContractTest`, `NaturalistDatabaseExtension`.
- Produces: `InsectQuery.fieldObservations() → FieldObservationQuery` with `FieldObservationCollection forNaturalist(NaturalistName)`; `InsectCommand.fieldObservations() → FieldObservationCommand`.

- [ ] **Step 1: Add the query port** to `InsectQuery.java`

Add the accessor to the namespace method list: `FieldObservationQuery fieldObservations();` and the nested interface:

```java
    interface FieldObservationQuery
            extends EntityQuery<FieldObservationId, FieldObservation,
                    InsectEntityCollections.FieldObservationCollection> {

        /** All of a naturalist's observations — used for the species-list "my collection" filter. */
        InsectEntityCollections.FieldObservationCollection forNaturalist(
                com.naturalist.naturalist.NaturalistName observedBy);

        /**
         * A naturalist's observations restricted to the given ranks — bounded read port for the
         * rank pages (order/family/genus/species detail), which pass the ranks they display to
         * render a per-entity "collected" indicator.
         */
        InsectEntityCollections.FieldObservationCollection forNaturalistAndSubjects(
                com.naturalist.naturalist.NaturalistName observedBy,
                java.util.Set<InsectRankName> subjects);
    }
```

- [ ] **Step 2: Add the command port** to `InsectCommand.java`

Add `FieldObservationCommand fieldObservations();` and:

```java
    interface FieldObservationCommand
            extends EntityCommand<FieldObservationId, FieldObservation> {
    }
```

- [ ] **Step 3: Write the query impl** (mirror `ImageQueryImpl`)

Create `FieldObservationQueryImpl.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.insects.InsectEntityCollections.FieldObservationCollection;
import com.naturalist.naturalist.NaturalistName;

import java.util.Set;

class FieldObservationQueryImpl
        extends AbstractEntityQuery<
        FieldObservationId,
        FieldObservation,
        FieldObservationCollection,
        InsectRepository.FieldObservationRepository>
        implements InsectQuery.FieldObservationQuery {

    FieldObservationQueryImpl(InsectRepository.FieldObservationRepository repository) {
        super(repository);
    }

    @Override
    public FieldObservationCollection findByNameSet(Set<FieldObservationId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public FieldObservationCollection forNaturalist(NaturalistName observedBy) {
        observer().arguments("forNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByNaturalist(observedBy));
    }

    @Override
    public FieldObservationCollection forNaturalistAndSubjects(NaturalistName observedBy, Set<InsectRankName> subjects) {
        observer().arguments("forNaturalistAndSubjects", i -> i
                        .identifier(observedBy, "observedBy")
                        .identifierSet(subjects, "subjects"))
                .throwWhenInvalid();
        return FieldObservationCollection.of(repository().getByNaturalistAndSubjects(observedBy, subjects));
    }
}
```

- [ ] **Step 4: Write the command impl** (mirror `ImageCommandImpl`, without `@DomainService` — assembled in the test context)

Create `FieldObservationCommandImpl.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FieldObservationCommandImpl
        extends AbstractEntityCommand<FieldObservationId, FieldObservation, InsectRepository.FieldObservationRepository>
        implements InsectCommand.FieldObservationCommand {

    FieldObservationCommandImpl(InsectRepository.FieldObservationRepository repository) {
        super(repository);
    }
}
```

- [ ] **Step 5: Wire the repository into the namespace**

In `InsectRepository.java`: add a `final FieldObservationRepository fieldObservationRepository;` field, add it as the last constructor param, assign it, add it as the last `create(...)` param and pass-through, and add an accessor `FieldObservationRepository fieldObservationRepository() { return fieldObservationRepository; }` — matching the existing repositories' shape exactly.

- [ ] **Step 6: Wire query + command into the impls**

In `InsectQueryImpl.java`: add a `FieldObservationQuery` constructor param + field + `fieldObservations()` accessor (match the existing sub-queries). In `InsectCommandImpl.java`: add a `FieldObservationCommand` param + field + `fieldObservations()` accessor, and extend the constructor's `Observer` null-check block to include it.

- [ ] **Step 7: Wire the test context**

In `InsectsTestContext.java`, in the constructor: add `new FieldObservationRepositoryMock(db)` as the last arg to `InsectRepository.create(...)`; construct `InsectQuery.FieldObservationQuery fieldObservationQuery = new FieldObservationQueryImpl(repository.fieldObservationRepository);` and pass it as the new last arg to `new InsectQueryImpl(...)`; construct `InsectCommand.FieldObservationCommand fieldObservationCommand = new FieldObservationCommandImpl(repository.fieldObservationRepository);` and pass it as the new last arg to `new InsectCommandImpl(...)`.

- [ ] **Step 8: Write the failing tests**

Create `FieldObservationCommandImplTest.java` (mirror `ImageCommandImplTest`):

```java
package com.naturalist.insects;

import com.naturalist.data.EntityCommand;
import com.naturalist.data.EntityCommandContractTest;
import com.naturalist.data.EntityQuery;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.TestEntitySource;
import com.naturalist.insects.InsectEntityCollections.FieldObservationCollection;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.List;

class FieldObservationCommandImplTest
        implements EntityCommandContractTest<FieldObservationId, FieldObservation, FieldObservationCollection> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    FieldObservationRepositoryMock repository = new FieldObservationRepositoryMock(db);
    InsectCommand.FieldObservationCommand command = new FieldObservationCommandImpl(repository);
    InsectQuery.FieldObservationQuery query = new FieldObservationQueryImpl(repository);

    @Override
    public EntityCommand<FieldObservationId, FieldObservation> command() {
        return command;
    }

    @Override
    public EntityQuery<FieldObservationId, FieldObservation, FieldObservationCollection> query() {
        return query;
    }

    @Override
    public TestEntitySource<FieldObservationId, FieldObservation> source() {
        return db.getNamed(FieldObservationTestEntitySource.class);
    }

    @Override
    public FieldObservationId notFoundName() {
        return TestInsectsIdentifiers.FieldObservation.NotFound.id;
    }

    @Override
    public List<FieldObservationId> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.FieldObservation.PatrickBattus,
                TestInsectsIdentifiers.FieldObservation.PatrickEmpoasca);
    }

    @Override
    public FieldObservation newEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-10T08:00:00Z"),
                "new");
    }

    @Override
    public FieldObservation ghostEntity() {
        return new FieldObservation(
                FieldObservationId.create(),
                NaturalistName.of("patrick-way"),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                Instant.parse("2026-06-11T08:00:00Z"),
                null);
    }

    @Override
    public FieldObservation modifiedEntity(FieldObservation original) {
        return new FieldObservation(
                original.id(),
                NaturalistName.of("delia-durrell"),
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
                Instant.parse("2026-06-12T08:00:00Z"),
                "changed");
    }
}
```

Create `FieldObservationQueryImplTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class FieldObservationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectQuery.FieldObservationQuery query =
            new FieldObservationQueryImpl(new FieldObservationRepositoryMock(db));

    @Test
    void forNaturalist_returnsOnlyThatNaturalistsObservations() {
        var patrick = query.forNaturalist(NaturalistName.of("patrick-way"));
        assertThat(patrick.isEmpty()).isFalse();
        assertThat(patrick.stream()).allMatch(o ->
                o.observedBy().equals(NaturalistName.of("patrick-way")));
    }

    @Test
    void forNaturalist_unknownNaturalist_isEmpty() {
        assertThat(query.forNaturalist(NaturalistName.of("nobody-here")).isEmpty()).isTrue();
    }

    @Test
    void forNaturalistAndSubjects_restrictsToGivenRanks() {
        var battus = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var result = query.forNaturalistAndSubjects(NaturalistName.of("patrick-way"), java.util.Set.of(battus));
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.stream()).allMatch(o -> o.subject().equals(battus)
                && o.observedBy().equals(NaturalistName.of("patrick-way")));
    }

    @Test
    void forNaturalistAndSubjects_emptyRankSet_isEmpty() {
        assertThat(query.forNaturalistAndSubjects(
                NaturalistName.of("patrick-way"), java.util.Set.<InsectRankName>of()).isEmpty()).isTrue();
    }
}
```

- [ ] **Step 9: Run — expect PASS**

Run: `mvn -q -pl domains/insects/insects-core -am test -Dtest='FieldObservationCommandImplTest,FieldObservationQueryImplTest'`
Expected: PASS. Also confirm the existing insects suite compiles/passes: `mvn -q -pl domains/insects/insects-core -am test` (the `InsectQueryImpl`/`InsectCommandImpl`/`InsectsTestContext` signature changes must not break `ImageCommandImplTest` etc.).

- [ ] **Step 10: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core domains/insects/insects-test-context
git commit -m "feat(insects): expose FieldObservation query (forNaturalist) + command

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Link images to observations (`InsectImage.observationId`)

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/ImageCommandImplTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/ImageGalleryTest.java` (the `image(...)` helper)
- Test: add a round-trip case to `ImageCommandImplTest` (or a small new test)

**Interfaces:**
- Consumes: `FieldObservationId` (Task 1).
- Produces: `InsectImage(id, parentName, dateAdded, resourceName, @Nullable observationId)` with `observationId()` accessor.

- [ ] **Step 1: Add the nullable component**

In `InsectImage.java`: add a trailing record component `@org.jspecify.annotations.Nullable FieldObservationId observationId` (prefer a clean `import org.jspecify.annotations.Nullable;`). Extend `invariants()` to validate it only when present:

```java
    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(parentName, "parentName")
                .notNull(dateAdded, "dateAdded")
                .namedValue(resourceName, "resourceName")
                .whenNotNull(observationId, c -> c.entityId(observationId, "observationId"));
    }
```

(`whenNotNull(@Nullable Object, Consumer<Constraints>)` exists on the `Constraints` DSL.)

- [ ] **Step 2: Update the 4 test construction sites**

In `ImageCommandImplTest.java` add `null` as the trailing arg to `newEntity()`, `ghostEntity()`, `modifiedEntity()`. In `ImageGalleryTest.java`'s `image(...)` helper, add `null` as the trailing arg. (The controller site is updated in Task 7.) The 11 `insect-images.json` entries need **no** change — a missing `observationId` deserializes to `null`.

- [ ] **Step 3: Add a round-trip test** for the new field in `ImageCommandImplTest.java`:

```java
    @org.junit.jupiter.api.Test
    void image_withObservationId_roundTrips() {
        FieldObservationId obs = TestInsectsIdentifiers.FieldObservation.PatrickBattus;
        InsectImage img = new InsectImage(
                InsectImageId.create(),
                TestInsectsIdentifiers.InsectGenus.Empoasca.name,
                java.time.Instant.parse("2026-06-20T08:00:00Z"),
                com.naturalist.data.FileName.of("IMG_OBS.HEIC"),
                obs);
        command.insert(img);
        InsectImage found = query.getByName(img.id()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(found.observationId()).isEqualTo(obs);
    }
```

- [ ] **Step 4: Run — expect PASS**

Run: `mvn -q -pl domains/insects/insects-core -am test -Dtest=ImageCommandImplTest`
then `mvn -q -pl domains/insects/insects-api -am test -Dtest=ImageGalleryTest`
Expected: PASS (existing cases still green + the new round-trip). RED first if run before adding the field (compile error on the 5-arg constructor).

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api domains/insects/insects-core
git commit -m "feat(insects): link InsectImage to a FieldObservation via nullable observationId

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Publish the current naturalist to the request

**Files:**
- Modify: `apps/management-console/src/main/java/com/naturalist/console/auth/CurrentNaturalistView.java`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/auth/NaturalistHeaderInterceptor.java`
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/NaturalistHeaderInterceptorTest.java`

**Interfaces:**
- Consumes: `NaturalistPrincipal.naturalistName()`.
- Produces: `CurrentNaturalistView.currentNaturalistSlug()` (String or null); request attribute `"naturalist.currentNaturalistName"` (the slug or null).

- [ ] **Step 1: Add the slug accessor** to `CurrentNaturalistView.java`:

```java
    /** The current naturalist's slug (NaturalistName value), or {@code null} for admin/anonymous. */
    public static String currentNaturalistSlug() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return p.naturalistName().value();
        }
        return null;
    }
```

(Prefer clean imports if the file doesn't already have them.)

- [ ] **Step 2: Publish the attribute** in `NaturalistHeaderInterceptor.java` — add a constant and a `setAttribute` in `preHandle` (after the existing display-name block):

```java
    /** Request-attribute key read by insects-console (same literal, by convention). */
    static final String CURRENT_NATURALIST_NAME = "naturalist.currentNaturalistName";
```
```java
        request.setAttribute(CURRENT_NATURALIST_NAME, CurrentNaturalistView.currentNaturalistSlug());
```

- [ ] **Step 3: Write the failing test**

Create `NaturalistHeaderInterceptorTest.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalistHeaderInterceptorTest {

    private final NaturalistHeaderInterceptor interceptor = new NaturalistHeaderInterceptor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesNaturalistSlug_forNaturalistPrincipal() {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
        var request = new MockHttpServletRequest();

        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME))
                .isEqualTo("patrick-way");
    }

    @Test
    void publishesNull_forAnonymous() {
        var request = new MockHttpServletRequest();
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(request.getAttribute(NaturalistHeaderInterceptor.CURRENT_NATURALIST_NAME)).isNull();
    }
}
```

- [ ] **Step 4: Run — expect PASS**

Run: `mvn -q -pl apps/management-console test -Dtest=NaturalistHeaderInterceptorTest`
Expected: PASS. Also confirm the existing header/login tests still pass: `-Dtest='HeaderWebMvcTest,CurrentNaturalistTest'`.

- [ ] **Step 5: Commit**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/auth apps/management-console/src/test/java/com/naturalist/console/auth/NaturalistHeaderInterceptorTest.java
git commit -m "feat(console): publish current naturalist slug as a request attribute

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: "My collection" browse toggle

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (the `/species` handler + a current-naturalist helper)
- Modify: `domains/insects/insects-console/src/main/jte/insects/list.jte` (toggle)
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/MyCollectionWebMvcTest.java`

**Interfaces:**
- Consumes: `insectQuery.fieldObservations().forNaturalist(...)`; request attribute `"naturalist.currentNaturalistName"`.
- Produces: `GET /insects/species?mine=true` filtered to the current naturalist's observed species.

- [ ] **Step 1: Add a current-naturalist helper** to `InsectsController.java`:

```java
    // Written by the app's NaturalistHeaderInterceptor (same literal, by convention).
    private static final String CURRENT_NATURALIST_ATTRIBUTE = "naturalist.currentNaturalistName";

    private static java.util.Optional<com.naturalist.naturalist.NaturalistName> currentNaturalist(
            jakarta.servlet.http.HttpServletRequest request) {
        Object slug = request.getAttribute(CURRENT_NATURALIST_ATTRIBUTE);
        return slug instanceof String s && !s.isBlank()
                ? java.util.Optional.of(com.naturalist.naturalist.NaturalistName.of(s))
                : java.util.Optional.empty();
    }
```

- [ ] **Step 2: Write the failing test**

Create `MyCollectionWebMvcTest.java` (uses seeded observations — patrick-way observed `battus-philenor` + `empoasca`):

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MyCollectionWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken as(String slug, String given) {
        var p = new NaturalistPrincipal(NaturalistName.of(slug), given, "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void mine_filtersToObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/species").param("mine", "true").with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("battus-philenor")));
    }

    @Test
    void allByDefault_showsFullCatalog() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 3: Implement the filter** in the `/species` handler

Change the handler signature to `list(@RequestParam(defaultValue = "0") int page, @RequestParam(name = "mine", defaultValue = "false") boolean mine, HttpServletRequest request, Model model)`. When `mine` is true and a current naturalist is present, restrict the page content to species whose `InsectSpeciesName` is in the current naturalist's observed subjects:

```java
        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        java.util.List<InsectSpecies> speciesList;
        if (mine && me.isPresent()) {
            java.util.Set<InsectRankName> mySubjects = insectQuery.fieldObservations()
                    .forNaturalist(me.get()).stream()
                    .map(FieldObservation::subject)
                    .collect(java.util.stream.Collectors.toSet());
            speciesList = speciesPage.content().stream()
                    .filter(s -> mySubjects.contains(s.name()))
                    .toList();
        } else {
            speciesList = speciesPage.content();
        }
        model.addAttribute("mine", mine && me.isPresent());
```

Use `speciesList` (not `speciesPage.content()`) where the template iterates species, and keep passing `speciesPage` for paging metadata. (Filtering the already-paged content is acceptable for the pilot's small catalog; a repository-level filter is a later optimization — note it.)

> **Rank-page "collected" indicator.** The `forNaturalistAndSubjects(me, subjects)` port built in Tasks 1–2 is the read port the rank pages use: each rank handler (order/family/genus/species detail) already assembles the descendant ranks it displays, so it passes that set to `forNaturalistAndSubjects(me, displayedSubjects)` and marks the entities present in the result as "collected." This task wires the indicator into the **species list** (pass the page's `InsectSpeciesName`s); the species **detail** indicator is wired in Task 6. Extending the same indicator to the order/family/genus rank pages is a mechanical follow-up reusing this port (add a `collected` set to each rank handler's model) — tracked, not built here, to keep the slice bounded.

- [ ] **Step 4: Add the toggle** to `list.jte`

At the top of the species list, add an "All / My collection" toggle (only meaningful when logged in as a naturalist; when `mine` is false it links to `?mine=true`, when true it links to `/insects/species`). Add a `@param boolean mine = false` and:

```jte
<div class="collection-toggle">
    @if(mine)
        <a href="/insects/species">All insects</a> · <strong>My collection</strong>
    @else
        <strong>All insects</strong> · <a href="/insects/species?mine=true">My collection</a>
    @endif
</div>
```

- [ ] **Step 5: Run — expect PASS**

Run: `mvn -q -pl apps/management-console test -Dtest=MyCollectionWebMvcTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console apps/management-console/src/test/java/com/naturalist/console/insects/MyCollectionWebMvcTest.java
git commit -m "feat(insects): 'My collection' browse toggle filters to observed species

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: Record an observation (the no-photo entry path)

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (new `observe` handler)
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte` (observe form)
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/ObserveWebMvcTest.java`

**Interfaces:**
- Consumes: `insectCommand.fieldObservations().insert(...)`; `currentNaturalist(request)`; Task 5's `?mine=true` read (to verify).
- Produces: `POST /insects/{name}/observe` → creates a `FieldObservation`.

- [ ] **Step 1: Write the failing test** — observe a species the naturalist has NOT already observed, then confirm `?mine=true` now includes it. Use `flora-mendez` (seeded naturalist with **no** observations) observing `battus-philenor`:

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ObserveWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken flora() {
        var p = new NaturalistPrincipal(NaturalistName.of("flora-mendez"), "Flora", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void observe_thenMine_includesTheSpecies() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").param("notes", "seen today")
                        .with(authentication(flora())).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/insects/species").param("mine", "true").with(authentication(flora())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("battus-philenor")));
    }
}
```

- [ ] **Step 2: Implement the `observe` handler** in `InsectsController.java`:

```java
    @PostMapping("/{name}/observe")
    String observe(@PathVariable String name,
                   @RequestParam(name = "notes", required = false) String notes,
                   HttpServletRequest request) {
        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/" + name;
        }
        var observation = new FieldObservation(
                FieldObservationId.create(),
                me.get(),
                InsectSpeciesName.of(name),
                Instant.now(),
                (notes == null || notes.isBlank()) ? null : notes);
        insectCommand.fieldObservations().insert(observation);
        return "redirect:/insects/" + name;
    }
```

- [ ] **Step 3: Add the observe form** to `detail.jte` (only shown to a logged-in naturalist). Reuse the existing `_csrf` model attribute the `detail` handler already provides. Add near the images/collection section:

```jte
@if(_csrf != null)
    <form method="post" action="/insects/${species.name().value()}/observe" class="observe-form">
        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
        <input type="text" name="notes" placeholder="Notes (optional)">
        <button type="submit">Add to my collection</button>
    </form>
@endif
```

(The observe control is visible whenever a CSRF token is present, i.e. any authenticated session; the handler no-ops for a non-naturalist. Hiding it precisely for admin is a later polish — note it.)

- [ ] **Step 4: Run — expect PASS**

Run: `mvn -q -pl apps/management-console test -Dtest=ObserveWebMvcTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console apps/management-console/src/test/java/com/naturalist/console/insects/ObserveWebMvcTest.java
git commit -m "feat(insects): observe endpoint records a FieldObservation (no photo needed)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Photo capture creates + links an observation

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` (the `addImage` handler)
- Test: `apps/management-console/src/test/java/com/naturalist/console/insects/CaptureLinksObservationWebMvcTest.java`

**Interfaces:**
- Consumes: `currentNaturalist(request)`; `insectCommand.fieldObservations().insert(...)`; the 5-arg `InsectImage` (Task 3).
- Produces: a photo captured by a naturalist creates an observation and links the image to it; captured with no naturalist stays owner-less (`observationId == null`).

- [ ] **Step 1: Write the failing test** — `amir-hassan` (seeded, no observations) captures a photo on `empoasca`, then `?mine=true` includes `empoasca`:

```java
package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CaptureLinksObservationWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken amir() {
        var p = new NaturalistPrincipal(NaturalistName.of("amir-hassan"), "Amir", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void capture_asNaturalist_addsSpeciesToCollection() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/images").param("resourceName", "IMG_NEW.HEIC")
                        .with(authentication(amir())).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/insects/species").param("mine", "true").with(authentication(amir())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("battus-philenor")));
    }
}
```

- [ ] **Step 2: Implement** — change `addImage` to inject `HttpServletRequest`, and when a naturalist is present create + link an observation:

```java
    @PostMapping("/{name}/images")
    String addImage(@PathVariable String name,
                    @RequestParam("resourceName") String resourceName,
                    HttpServletRequest request) {
        var speciesName = InsectSpeciesName.of(name);
        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        FieldObservationId observationId = null;
        if (me.isPresent()) {
            var observation = new FieldObservation(
                    FieldObservationId.create(), me.get(), speciesName, Instant.now(), null);
            insectCommand.fieldObservations().insert(observation);
            observationId = observation.id();
        }
        var image = new InsectImage(
                InsectImageId.create(),
                speciesName,
                Instant.now(),
                FileName.of(resourceName),
                observationId);
        insectCommand.images().insert(image);
        return "redirect:/insects/" + name;
    }
```

- [ ] **Step 3: Run — expect PASS**

Run: `mvn -q -pl apps/management-console test -Dtest=CaptureLinksObservationWebMvcTest`
Expected: PASS.

- [ ] **Step 4: Full verify + domain note**

Create `domains/insects/CLAUDE.md` addition (append a short section if the file exists; otherwise add a note to it) documenting: `FieldObservation` is the collection unit (`observedBy` + `subject`); `InsectImage.observationId` links a photo to its observation (null = shared catalog image); the current naturalist reaches `insects-console` via the `"naturalist.currentNaturalistName"` request attribute set by the app interceptor; "collected" dedups by `subject`. Reference `docs/plans/2026-07-12-naturalist-insect-collection-design.md`.

Run: `mvn -q verify` (repo root)
Expected: BUILD SUCCESS — all modules, all tests, including every domain-console template test.

- [ ] **Step 5: Commit**

```bash
git add domains/insects apps/management-console/src/test/java/com/naturalist/console/insects/CaptureLinksObservationWebMvcTest.java
git commit -m "feat(insects): a naturalist's photo capture creates + links an observation

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage** (against `2026-07-12-naturalist-insect-collection-design.md`):
- FieldObservation entity + read/write stack → Tasks 1, 2. ✔
- Nullable image→observation link → Task 3. ✔
- Current naturalist via request attribute → Task 4 (publish) + Tasks 5–7 (read). ✔
- Observe (no photo) entry path → Task 6. ✔
- Photo capture creates+links observation → Task 7. ✔
- Browse toggle (entire catalog vs mine) → Task 5. ✔
- Testing (repo/query/command contracts, image round-trip, interceptor, console end-to-end) → all tasks. ✔
- Out of scope (cross-organism generalization, edit/delete, multi-photo UI, admin name-collision guard, Spring-managing the controllers) → not implemented. ✔

**Placeholder scan:** No TBD/"add validation"/"similar to". Where the plan says "mirror `InsectImageRepositoryMock`/`ImageQueryImpl`", the exact code is given and the reference is only to match annotations on an existing file. The `InsectQueryImpl`/`InsectCommandImpl`/`InsectRepository` field-and-constructor edits (Task 2 Steps 5–7) are described structurally rather than as a full diff because those files' full bodies are long and additive-by-pattern; the shapes to match are named exactly.

**Type consistency:** `FieldObservation(id, observedBy, subject, observedOn, notes)` and its accessors are used identically across Tasks 1–7. `fieldObservations()` (never `observations()`) is used on both `InsectQuery` and `InsectCommand`. `FieldObservationId`/`FieldObservationCollection`/`FieldObservationRepository`/`FieldObservationQuery`/`FieldObservationCommand` names are consistent. The request-attribute key `"naturalist.currentNaturalistName"` matches between Task 4 (writer) and Tasks 5–7 (reader). The 5-arg `InsectImage` constructor (Task 3) matches every construction site updated in Tasks 3 and 7. Seeded naturalists (`patrick-way`, `delia-durrell` with observations; `flora-mendez`, `amir-hassan` without) match the auth slice's `naturalist-credentials.json`.
