# Citation Association Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable any `NamedEntity` in any domain to be cited by any `Citation`, with citations attaching at the highest appropriate rank and children discovering inherited citations through hierarchy walks.

**Architecture:** `CitationAssociation` (`Entity<CitationAssociationId>`) lives in the library domain, using the catalog kernel's `EntityRef` for domain-agnostic subject references. The insects domain adds a discovery query that walks the Linnaean hierarchy to collect inherited citations into an `InsectCitationView` read model.

**Tech Stack:** Java records, `kernels/framework` DDD types, `kernels/catalog` `EntityRef`/`DomainId`, Jackson `@JsonTypeInfo` external-property dispatch.

**Spec:** [`docs/plans/2026-06-14-citation-association-design.md`](2026-06-14-citation-association-design.md)

---

## PR 1: CitationAssociation entity + identifiers + test identifiers

### Task 1: CitationAssociationId in domains/identifiers

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/library/CitationAssociationId.java`

- [ ] **Step 1: Create the EntityId subclass**

```java
package com.naturalist.library;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class CitationAssociationId extends EntityId {
    private CitationAssociationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static CitationAssociationId of(UUID value) {
        return new CitationAssociationId(value);
    }

    public static CitationAssociationId create() {
        return new CitationAssociationId(EntityId.newUUID());
    }
}
```

Pattern: identical to `InsectImageId` and `InsectFunctionalRoleId`. Package is `com.naturalist.library` because it belongs to the library domain's identifier namespace.

- [ ] **Step 2: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/library/CitationAssociationId.java
git commit -m "feat(library): add CitationAssociationId EntityId subclass"
```

### Task 2: Test identifier constants in TestLibraryIdentifiers

**Files:**
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java`

The association test data will reference existing citations (`EolSwallowtail`, `EolGreenLacewing`) paired with insect entities at different ranks. We need at least two known IDs and one not-found.

- [ ] **Step 1: Add Associations inner class inside Citations**

Add this inside the existing `Citations` class, after the existing `CitationName` constants and before the `NotFound` class:

```java
        public static class Associations {

            private Associations() {
            }

            public static final CitationAssociationId EolSwallowtailOnLepidoptera =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a001-7001-8001-a00000000001"));

            public static final CitationAssociationId EolSwallowtailOnPapilionidae =
                    CitationAssociationId.of(
                            UUID.fromString("019f0001-a002-7002-8002-a00000000002"));

            public static class NotFound {
                public static final CitationAssociationId name =
                        CitationAssociationId.of(
                                UUID.fromString("019f0001-ffff-7fff-bfff-ffffffffffff"));
            }
        }
```

Add the `import java.util.UUID;` if not already present.

- [ ] **Step 2: Commit**

```bash
git add domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java
git commit -m "feat(library): add CitationAssociation test identifier constants"
```

### Task 3: Add catalog dependency to library-api

**Files:**
- Modify: `domains/library/library-api/pom.xml`

- [ ] **Step 1: Add catalog dependency**

Add after the `authority` dependency (alphabetical):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>catalog</artifactId>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-api/pom.xml
git commit -m "build(library): add catalog kernel dependency to library-api"
```

### Task 4: CitationAssociation entity record

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociation.java`

- [ ] **Step 1: Create the entity record**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Records the fact that a {@link com.naturalist.authority.Citation} covers a named
 * entity in any domain. The association attaches at the highest appropriate rank —
 * a citation about order Lepidoptera attaches to that order, not to every species
 * within it. Children discover inherited citations by walking their domain's
 * hierarchy.
 * <p>
 * {@code subject} is an {@link EntityRef} from the catalog kernel — a
 * {@code (DomainId, EntityName)} pair that fully qualifies the cited entity across
 * domain boundaries. Jackson polymorphic dispatch for the open {@code DomainId}
 * interface is not declared on this record — it would create import cycles to
 * concrete domain types. Instead, the {@code TestEntitySource} uses a DTO
 * intermediary, and the composition root configures a Jackson module.
 */
public record CitationAssociation(
        CitationAssociationId name,
        CitationName citationName,
        EntityRef subject,
        @Nullable String note
) implements Entity<CitationAssociationId> {

    public CitationAssociation withNote(@Nullable String value) {
        return new CitationAssociation(name, citationName, subject, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(name, "name")
                .entityName(citationName, "citationName")
                .valueObject(subject, "subject");
    }
}
```

Notes:
- No `@JsonSubTypes` on the record — listing concrete `DomainId` subtypes (e.g. `InsectsDomain`) would create `library-api → insects-api` dependency cycles. Polymorphic dispatch is handled by the DTO in the test entity source.
- `withNote` is the only mutator — `citationName` and `subject` form the identity pair.

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociation.java
git commit -m "feat(library): add CitationAssociation entity record"
```

### Task 5: CitationAssociation unit test (invariants)

**Files:**
- Create: `domains/library/library-api/src/test/java/com/naturalist/library/CitationAssociationTest.java`

- [ ] **Step 1: Write the invariants test**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAssociationTest {

    static final Observer observer = Observer.forClass(CitationAssociationTest.class);

    @Test
    void validCitationAssociation_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validCitationAssociation_hasNoInvariantViolations");

        CitationAssociation association = new CitationAssociation(
                CitationAssociationId.create(),
                CitationName.of("eol-battus-philenor-130502"),
                new EntityRef(new TestDomainId(), CitationName.of("some-entity")),
                null);

        InvariantObservation observation = mo.entity(association, "association");
        assertThat(observation.invalidInvariants()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        CitationAssociation association = new CitationAssociation(null, null, null, null);

        InvariantObservation observation = mo.entity(association, "association");
        assertThat(observation.invalidInvariantNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder("name", "citationName", "subject");
    }

    /** Minimal DomainId for test isolation — no dependency on any real domain. */
    private record TestDomainId() implements DomainId {
        @Override
        public String value() {
            return "test";
        }
    }
}
```

Note: uses a local `TestDomainId` to avoid importing any real domain's `DomainId` subtype.

- [ ] **Step 2: Verify the test compiles and passes**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-api/src/test/java/com/naturalist/library/CitationAssociationTest.java
git commit -m "test(library): add CitationAssociation invariant tests"
```

### Task 6: CitationAssociationCollection

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationCollection.java`

- [ ] **Step 1: Create the behavioral collection**

```java
package com.naturalist.library;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class CitationAssociationCollection extends BehavioralCollection<CitationAssociation> {

    CitationAssociationCollection(Collection<CitationAssociation> associations) {
        super(associations);
    }

    public static CitationAssociationCollection of(Collection<CitationAssociation> associations) {
        return new CitationAssociationCollection(associations);
    }

    public static CitationAssociationCollection empty() {
        return new CitationAssociationCollection(List.of());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationCollection.java
git commit -m "feat(library): add CitationAssociationCollection"
```

### Task 7: CitationAssociationRepository interface

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationRepository.java`

- [ ] **Step 1: Create the package-private repository interface**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.EntityRepository;

import java.util.List;

interface CitationAssociationRepository
        extends EntityRepository<CitationAssociationId, CitationAssociation> {

    List<CitationAssociation> getByCitationName(CitationName citationName);

    List<CitationAssociation> getBySubject(EntityRef subject);
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationRepository.java
git commit -m "feat(library): add CitationAssociationRepository interface"
```

### Task 8: CitationAssociationQuery interface

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationQuery.java`

- [ ] **Step 1: Create the public query interface**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;

public interface CitationAssociationQuery {

    CitationAssociationCollection findByCitationName(CitationName citationName);

    CitationAssociationCollection findBySubject(EntityRef subject);
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/CitationAssociationQuery.java
git commit -m "feat(library): add CitationAssociationQuery interface"
```

---

## PR 2: TestEntitySource + JSON catalog + repository mock + contract test

### Task 9: Add insects-api dependency to library-repository-test

**Files:**
- Modify: `domains/library/library-repository-test/pom.xml`

The JSON catalog fixtures reference insect entities, so the test entity source needs `InsectsDomain` and insect `EntityName` types.

- [ ] **Step 1: Add insects-api as a dependency**

Add after the existing `identifiers-test` dependency:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>insects-api</artifactId>
        </dependency>
```

Note: this is compile-scope in `library-repository-test`, which is itself only ever consumed as test infrastructure by other modules.

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-repository-test/pom.xml
git commit -m "build(library): add insects-api dependency to library-repository-test"
```

### Task 10: JSON catalog file for citation associations

**Files:**
- Create: `domains/library/library-repository-test/src/main/resources/library/citation-associations.json`

Two association records: the Battus philenor EOL citation attached at order Lepidoptera, and at family Papilionidae. UUIDs match `TestLibraryIdentifiers.Citations.Associations`.

The `EntityRef` subject is stored in flat form (`subjectDomain`, `subjectRank`, `subjectName`) because the open `DomainId` interface cannot be auto-dispatched by Jackson. The `TestEntitySource` DTO maps these back to typed domain objects.

- [ ] **Step 1: Create the JSON catalog**

```json
[
  {
    "name": "019f0001-a001-7001-8001-a00000000001",
    "citationName": "eol-battus-philenor-130502",
    "subjectDomain": "insects",
    "subjectRank": "ORDER",
    "subjectName": "lepidoptera",
    "note": "EOL page covers the entire order with family-level diagnostic keys"
  },
  {
    "name": "019f0001-a002-7002-8002-a00000000002",
    "citationName": "eol-battus-philenor-130502",
    "subjectDomain": "insects",
    "subjectRank": "FAMILY",
    "subjectName": "papilionidae",
    "note": "Species-level photographs and life history for Papilionidae swallowtails"
  }
]
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-repository-test/src/main/resources/library/citation-associations.json
git commit -m "feat(library): add citation-associations.json test catalog"
```

### Task 11: CitationAssociationTestEntitySource

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java`

Because `EntityRef` contains the open `DomainId` interface and abstract `EntityName`, standard Jackson record deserialization can't work. The source reads via a DTO record and maps to domain entities.

- [ ] **Step 1: Create the test entity source with DTO mapping**

```java
package com.naturalist.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.InsectsDomain;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class CitationAssociationTestEntitySource
        extends TestEntitySource<CitationAssociationId, CitationAssociation> {

    private static final InsectsDomain INSECTS = new InsectsDomain();

    public CitationAssociationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFromDto("library/citation-associations.json");
    }

    @Override
    protected List<UniqueConstraint<CitationAssociation>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "citationName+subject";
                    }

                    @Override
                    public Function<CitationAssociation, ?> valueFunction() {
                        return a -> a.citationName().value() + ":"
                                + a.subject().domain().value() + ":"
                                + a.subject().name().value();
                    }
                });
    }

    private void loadFromDto(String resourcePath) {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            List<CitationAssociationDto> dtos = mapper.readValue(is,
                    new TypeReference<>() {});
            for (CitationAssociationDto dto : dtos) {
                insert(dto.toEntity());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourcePath, e);
        }
    }

    private record CitationAssociationDto(
            String name,
            String citationName,
            String subjectDomain,
            String subjectRank,
            String subjectName,
            @Nullable String note
    ) {
        CitationAssociation toEntity() {
            return new CitationAssociation(
                    CitationAssociationId.of(UUID.fromString(name)),
                    CitationName.of(citationName),
                    new EntityRef(resolveDomain(subjectDomain),
                            resolveEntityName(subjectDomain, subjectRank, subjectName)),
                    note);
        }

        private static DomainId resolveDomain(String domain) {
            return switch (domain) {
                case "insects" -> INSECTS;
                default -> throw new IllegalArgumentException("Unknown domain: " + domain);
            };
        }

        private static EntityName resolveEntityName(String domain, String rank, String slug) {
            if ("insects".equals(domain)) {
                return switch (rank) {
                    case "ORDER" -> InsectOrderName.of(slug);
                    case "FAMILY" -> InsectFamilyName.of(slug);
                    case "GENUS" -> InsectGenusName.of(slug);
                    case "SPECIES" -> InsectSpeciesName.of(slug);
                    default -> throw new IllegalArgumentException(
                            "Unknown insect rank: " + rank);
                };
            }
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
    }
}
```

As more domains gain citations, extend `resolveDomain` and `resolveEntityName` with new cases.

- [ ] **Step 2: Verify compilation**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java
git commit -m "feat(library): add CitationAssociationTestEntitySource with DTO mapping"
```

### Task 12: CitationAssociationRepositoryMock

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationRepositoryMock.java`

- [ ] **Step 1: Create the in-memory mock**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class CitationAssociationRepositoryMock
        extends AbstractTestEntityRepository<CitationAssociationId, CitationAssociation, CitationAssociationTestEntitySource>
        implements CitationAssociationRepository {

    CitationAssociationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<CitationAssociation> getByCitationName(CitationName citationName) {
        observer().arguments("getByCitationName",
                        i -> i.entityName(citationName, "citationName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.citationName().equals(citationName))
                .toList();
    }

    @Override
    public List<CitationAssociation> getBySubject(EntityRef subject) {
        observer().arguments("getBySubject",
                        i -> i.valueObject(subject, "subject"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(a -> a.subject().equals(subject))
                .toList();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationRepositoryMock.java
git commit -m "feat(library): add CitationAssociationRepositoryMock"
```

### Task 13: CitationAssociationEntityRepositoryTest (behavioral contract)

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationEntityRepositoryTest.java`

- [ ] **Step 1: Write the contract test interface**

```java
package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

interface CitationAssociationEntityRepositoryTest
        extends EntityRepositoryTest<CitationAssociationId, CitationAssociation> {

    @Override
    CitationAssociationRepository repository();

    @Override
    default TestEntitySource<CitationAssociationId, CitationAssociation> source() {
        return db.getNamed(CitationAssociationTestEntitySource.class);
    }

    @Override
    default CitationAssociationId notFoundName() {
        return TestLibraryIdentifiers.Citations.Associations.NotFound.name;
    }

    @Override
    default List<CitationAssociationId> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.Citations.Associations.EolSwallowtailOnLepidoptera,
                TestLibraryIdentifiers.Citations.Associations.EolSwallowtailOnPapilionidae);
    }

    @Override
    default CitationAssociation newEntity() {
        return new CitationAssociation(
                CitationAssociationId.create(),
                TestLibraryIdentifiers.Citations.EolGreenLacewing,
                new EntityRef(new InsectsDomain(), InsectOrderName.of("hymenoptera")),
                RandomValue.string());
    }

    @Override
    default CitationAssociation ghostEntity() {
        return new CitationAssociation(
                CitationAssociationId.create(),
                TestLibraryIdentifiers.Citations.EolGreenLacewing,
                new EntityRef(new InsectsDomain(), InsectOrderName.of("hymenoptera")),
                null);
    }

    @Override
    default CitationAssociation modifiedEntity(CitationAssociation original) {
        return original.withNote(RandomValue.string());
    }

    // =========================================================================
    // getByCitationName
    // =========================================================================

    @Test
    default void getByCitationName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByCitationName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("citationName");
    }

    @Test
    default void getByCitationName_unknownName_returnsEmpty() {
        assertThat(repository().getByCitationName(
                TestLibraryIdentifiers.Citations.NotFound.name))
                .isEmpty();
    }

    @Test
    default void getByCitationName_knownName_returnsAssociations() {
        List<CitationAssociation> result = repository().getByCitationName(
                CitationName.of("eol-battus-philenor-130502"));

        assertThat(result).hasSize(2);
    }

    // =========================================================================
    // getBySubject
    // =========================================================================

    @Test
    default void getBySubject_rejectsNull() {
        assertThatThrownBy(() -> repository().getBySubject(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    default void getBySubject_unknownSubject_returnsEmpty() {
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));
        assertThat(repository().getBySubject(unknown)).isEmpty();
    }

    @Test
    default void getBySubject_knownSubject_returnsAssociations() {
        EntityRef lepidoptera = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("lepidoptera"));

        List<CitationAssociation> result = repository().getBySubject(lepidoptera);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().citationName().value())
                .isEqualTo("eol-battus-philenor-130502");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationEntityRepositoryTest.java
git commit -m "test(library): add CitationAssociationEntityRepositoryTest contract"
```

### Task 14: CitationAssociationRepositoryMock test class

**Files:**
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationRepositoryMockTest.java`

- [ ] **Step 1: Create the test class that wires the contract**

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class CitationAssociationRepositoryMockTest implements CitationAssociationEntityRepositoryTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    CitationAssociationRepositoryMock repository = new CitationAssociationRepositoryMock(db);

    @Override
    public CitationAssociationRepository repository() {
        return repository;
    }
}
```

- [ ] **Step 2: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationRepositoryMockTest.java
git commit -m "test(library): add CitationAssociationRepositoryMockTest"
```

### Task 15: CitationAssociationTestEntitySourceTest

**Files:**
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationTestEntitySourceTest.java`

- [ ] **Step 1: Create the test entity source test**

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.TestEntitySourceTest;
import org.junit.jupiter.api.extension.RegisterExtension;

class CitationAssociationTestEntitySourceTest
        extends TestEntitySourceTest<CitationAssociationId, CitationAssociation> {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    @Override
    protected CitationAssociationTestEntitySource source() {
        return db.getNamed(CitationAssociationTestEntitySource.class);
    }
}
```

- [ ] **Step 2: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationTestEntitySourceTest.java
git commit -m "test(library): add CitationAssociationTestEntitySourceTest"
```

---

## PR 3: Query adapter in library-core

### Task 16: CitationAssociationQueryImpl

**Files:**
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CitationAssociationQueryImpl.java`

- [ ] **Step 1: Create the query adapter**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

@DomainService
class CitationAssociationQueryImpl implements CitationAssociationQuery {

    private static final Observer observer = Observer.forClass(CitationAssociationQueryImpl.class);

    private final CitationAssociationRepository repository;

    CitationAssociationQueryImpl(CitationAssociationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CitationAssociationCollection findByCitationName(CitationName citationName) {
        observer.arguments("findByCitationName", i -> i
                        .entityName(citationName, "citationName"))
                .throwWhenInvalid();
        return CitationAssociationCollection.of(repository.getByCitationName(citationName));
    }

    @Override
    public CitationAssociationCollection findBySubject(EntityRef subject) {
        observer.arguments("findBySubject", i -> i
                        .valueObject(subject, "subject"))
                .throwWhenInvalid();
        return CitationAssociationCollection.of(repository.getBySubject(subject));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add domains/library/library-core/src/main/java/com/naturalist/library/CitationAssociationQueryImpl.java
git commit -m "feat(library): add CitationAssociationQueryImpl"
```

### Task 17: CitationAssociationQueryImpl test

**Files:**
- Create: `domains/library/library-core/src/test/java/com/naturalist/library/CitationAssociationQueryImplTest.java`

- [ ] **Step 1: Write the query adapter test**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CitationAssociationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    CitationAssociationRepositoryMock repository = new CitationAssociationRepositoryMock(db);
    CitationAssociationQueryImpl query = new CitationAssociationQueryImpl(repository);

    @Test
    void findByCitationName_rejectsNull() {
        assertThatThrownBy(() -> query.findByCitationName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("citationName");
    }

    @Test
    void findByCitationName_knownName_returnsCollection() {
        CitationAssociationCollection result = query.findByCitationName(
                CitationName.of("eol-battus-philenor-130502"));

        assertThat(result.elements()).hasSize(2);
    }

    @Test
    void findBySubject_rejectsNull() {
        assertThatThrownBy(() -> query.findBySubject(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void findBySubject_knownSubject_returnsCollection() {
        EntityRef lepidoptera = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("lepidoptera"));

        CitationAssociationCollection result = query.findBySubject(lepidoptera);

        assertThat(result.elements()).hasSize(1);
    }

    @Test
    void findBySubject_unknownSubject_returnsEmpty() {
        EntityRef unknown = new EntityRef(
                new InsectsDomain(), InsectOrderName.of("zygentoma"));

        CitationAssociationCollection result = query.findBySubject(unknown);

        assertThat(result.elements()).isEmpty();
    }
}
```

- [ ] **Step 2: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/library/library-core/src/test/java/com/naturalist/library/CitationAssociationQueryImplTest.java
git commit -m "test(library): add CitationAssociationQueryImplTest"
```

---

## PR 4: Insect citation discovery — types + query adapter

### Task 18: InsectCitationView read model in insects-api

**Files:**
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCitationView.java`
- Modify: `domains/insects/insects-api/pom.xml` (add `authority` dependency if not present)

- [ ] **Step 1: Check insects-api/pom.xml for authority dependency**

Read `domains/insects/insects-api/pom.xml`. `InsectCitationView` imports `com.naturalist.authority.CitationName`, so `authority` must be on the classpath. If it is not listed, add:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>authority</artifactId>
        </dependency>
```

- [ ] **Step 2: Create the read model with nested RankedCitation**

```java
package com.naturalist.insects;

import com.naturalist.authority.CitationName;
import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Citations applicable to an insect rank, including those inherited from ancestor
 * ranks. Each {@link RankedCitation} preserves which rank the citation was attached
 * at — a species-level query returns direct citations plus those inherited from its
 * genus, family, and order.
 */
public record InsectCitationView(
        InsectRankName subject,
        List<RankedCitation> citations
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(citations, "citations");
    }

    /**
     * A single citation with provenance — which rank in the hierarchy the citation
     * was originally attached at.
     */
    public record RankedCitation(
            CitationName citationName,
            InsectRankName attachedAt,
            @Nullable String note
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .entityName(citationName, "citationName")
                    .identifier(attachedAt, "attachedAt");
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-api/pom.xml domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCitationView.java
git commit -m "feat(insects): add InsectCitationView read model"
```

### Task 19: InsectCitationView unit test

**Files:**
- Create: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectCitationViewTest.java`

- [ ] **Step 1: Write the invariants test**

```java
package com.naturalist.insects;

import com.naturalist.authority.CitationName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCitationViewTest {

    static final Observer observer = Observer.forClass(InsectCitationViewTest.class);

    @Test
    void validView_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validView_hasNoInvariantViolations");

        InsectCitationView view = new InsectCitationView(
                InsectSpeciesName.of("battus-philenor"),
                List.of(new InsectCitationView.RankedCitation(
                        CitationName.of("eol-battus-philenor-130502"),
                        InsectOrderName.of("lepidoptera"),
                        "EOL page")));

        InvariantObservation observation = mo.observable(view, "view");
        assertThat(observation.invalidInvariants()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        InsectCitationView view = new InsectCitationView(null, null);

        InvariantObservation observation = mo.observable(view, "view");
        assertThat(observation.invalidInvariantNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder("subject", "citations");
    }

    @Test
    void validRankedCitation_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validRankedCitation_hasNoInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
                CitationName.of("eol-battus-philenor-130502"),
                InsectFamilyName.of("papilionidae"),
                null);

        InvariantObservation observation = mo.observable(rc, "rankedCitation");
        assertThat(observation.invalidInvariants()).isEmpty();
    }

    @Test
    void nullRankedCitationComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullRankedCitationComponents_reportInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
                null, null, null);

        InvariantObservation observation = mo.observable(rc, "rankedCitation");
        assertThat(observation.invalidInvariantNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder("citationName", "attachedAt");
    }
}
```

- [ ] **Step 2: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectCitationViewTest.java
git commit -m "test(insects): add InsectCitationView invariant tests"
```

### Task 20: Add CitationQuery to InsectQuery namespace

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`

- [ ] **Step 1: Add the citation query accessor and nested interface**

Add the accessor method alongside the existing query accessors (after `orders()`):

```java
    CitationQuery citations();
```

Add the nested interface alongside the existing nested interfaces (after `OrderQuery`):

```java
    interface CitationQuery {
        InsectCitationView findByRankName(InsectRankName rankName);
    }
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java
git commit -m "feat(insects): add CitationQuery to InsectQuery namespace"
```

### Task 21: Add library dependencies to insects-core

**Files:**
- Modify: `domains/insects/insects-core/pom.xml`

- [ ] **Step 1: Add library-api compile dependency and library-repository-test test dependency**

Add to the compile dependencies section (alphabetical):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
```

Add to the test dependencies section:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-repository-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-core/pom.xml
git commit -m "build(insects): add library dependencies for citation discovery"
```

### Task 22: InsectCitationQueryImpl in insects-core

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCitationQueryImpl.java`

The query adapter takes the existing sub-queries (species, genus, family) for the hierarchy walk, plus the external `CitationAssociationQuery` from the library domain. It is constructed inside `InsectQueryImpl` — the same pattern as `TaxonViewQueryImpl`.

- [ ] **Step 1: Create the citation discovery query adapter**

```java
package com.naturalist.insects;

import com.naturalist.catalog.EntityRef;
import com.naturalist.library.CitationAssociation;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.List;

class InsectCitationQueryImpl implements InsectQuery.CitationQuery {

    private static final InsectsDomain INSECTS = new InsectsDomain();
    private final Observer observer = Observer.forClass(getClass());
    private final CitationAssociationQuery citationAssociationQuery;
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectCitationQueryImpl(CitationAssociationQuery citationAssociationQuery,
                            InsectQuery.SpeciesQuery speciesQuery,
                            InsectQuery.GenusQuery genusQuery,
                            InsectQuery.FamilyQuery familyQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery"))
                .throwWhenInvalid();
        this.citationAssociationQuery = citationAssociationQuery;
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    @Override
    public InsectCitationView findByRankName(InsectRankName rankName) {
        observer.arguments("findByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();

        List<InsectRankName> ancestry = resolveAncestry(rankName);
        List<InsectCitationView.RankedCitation> citations = new ArrayList<>();

        for (InsectRankName rank : ancestry) {
            EntityRef ref = new EntityRef(INSECTS, rank);
            for (CitationAssociation a : citationAssociationQuery.findBySubject(ref).elements()) {
                citations.add(new InsectCitationView.RankedCitation(
                        a.citationName(), rank, a.note()));
            }
        }

        InsectCitationView view = new InsectCitationView(rankName, List.copyOf(citations));
        observer.observable(view, "citationView").observe(Level.WARN);
        return view;
    }

    /**
     * Returns the full ancestry chain from the given rank up to order, inclusive.
     * Species -> genus -> family -> order. If a parent cannot be resolved (data
     * gap), the chain stops at the last resolvable rank.
     */
    private List<InsectRankName> resolveAncestry(InsectRankName rankName) {
        List<InsectRankName> ancestry = new ArrayList<>();
        ancestry.add(rankName);

        return switch (rankName) {
            case InsectSpeciesName speciesName -> {
                speciesQuery.getByName(speciesName).ifPresent(species -> {
                    ancestry.add(species.genusName());
                    genusQuery.getByName(species.genusName()).ifPresent(genus -> {
                        ancestry.add(genus.familyName());
                        familyQuery.getByName(genus.familyName()).ifPresent(family ->
                                ancestry.add(family.orderName()));
                    });
                });
                yield ancestry;
            }
            case InsectGenusName genusName -> {
                genusQuery.getByName(genusName).ifPresent(genus -> {
                    ancestry.add(genus.familyName());
                    familyQuery.getByName(genus.familyName()).ifPresent(family ->
                            ancestry.add(family.orderName()));
                });
                yield ancestry;
            }
            case InsectFamilyName familyName -> {
                familyQuery.getByName(familyName).ifPresent(family ->
                        ancestry.add(family.orderName()));
                yield ancestry;
            }
            case InsectOrderName _ -> ancestry;
            case InsectSubspeciesName _ -> ancestry;
        };
    }
}
```

Note: uses the public sub-query interfaces (species, genus, family) instead of the package-private `InsectRepository`. This means the hierarchy walk goes through the query layer — cleaner dependency graph, and the sub-queries already exist as constructor arguments in `InsectQueryImpl`.

- [ ] **Step 2: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCitationQueryImpl.java
git commit -m "feat(insects): add InsectCitationQueryImpl with hierarchy walk"
```

### Task 23: Wire CitationQuery into InsectQueryImpl

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java`

`InsectCitationQueryImpl` is constructed inside `InsectQueryImpl` — the same pattern as `TaxonViewQueryImpl` (which is built from an `InsectTaxonViewFactory` using the existing sub-queries). `InsectQueryImpl` gets one new external dependency: `CitationAssociationQuery`.

- [ ] **Step 1: Add the citationQuery field**

After the existing `taxonViewQuery` field:

```java
    private final CitationQuery citationQuery;
```

- [ ] **Step 2: Update the constructor**

Add `com.naturalist.library.CitationAssociationQuery citationAssociationQuery` as the last constructor parameter.

Add validation in the observer arguments block:

```java
                        .notNull(citationAssociationQuery, "citationAssociationQuery"))
```

After the existing `this.taxonViewQuery = new TaxonViewQueryImpl(factory);` line, add:

```java
        this.citationQuery = new InsectCitationQueryImpl(
                citationAssociationQuery, speciesQuery, genusQuery, familyQuery);
```

- [ ] **Step 3: Add the accessor**

```java
    @Override
    public CitationQuery citations() {
        return citationQuery;
    }
```

The full constructor after changes:

```java
    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery,
                    com.naturalist.library.CitationAssociationQuery citationAssociationQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(functionalRoleQuery, "functionalRoleQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(citationAssociationQuery, "citationAssociationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.familyQuery = familyQuery;
        this.genusQuery = genusQuery;
        this.functionalRoleQuery = functionalRoleQuery;
        this.orderQuery = orderQuery;
        InsectTaxonViewFactory factory =
                new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery);
        this.taxonViewQuery = new TaxonViewQueryImpl(factory);
        this.citationQuery = new InsectCitationQueryImpl(
                citationAssociationQuery, speciesQuery, genusQuery, familyQuery);
    }
```

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java
git commit -m "feat(insects): wire CitationQuery into InsectQueryImpl"
```

### Task 24: Update InsectQueryImplTest for new constructor parameter

**Files:**
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java`

The constructor now takes 7 parameters instead of 6. All construction sites in this test need the new `CitationAssociationQuery` argument.

- [ ] **Step 1: Add library mock fields**

After the existing `InsectQuery.OrderQuery orderQuery` field:

```java
    com.naturalist.library.CitationAssociationRepositoryMock citationAssociationRepository =
            new com.naturalist.library.CitationAssociationRepositoryMock(db);
    com.naturalist.library.CitationAssociationQuery citationAssociationQuery =
            new com.naturalist.library.CitationAssociationQueryImpl(citationAssociationRepository);
```

Note: `CitationAssociationQueryImpl` and `CitationAssociationRepositoryMock` are package-private in the `library` package. Since this test is in `com.naturalist.insects`, it cannot access them directly. Use the public `CitationAssociationQuery` interface. The concrete implementations need to be accessible — check if they are `public` or package-private.

`CitationAssociationQueryImpl` is package-private (`class CitationAssociationQueryImpl`) and `CitationAssociationRepositoryMock` is also package-private (`class CitationAssociationRepositoryMock`). They cannot be constructed from outside the `com.naturalist.library` package.

**Revised approach:** Create a helper in the library-repository-test module that exposes a factory for the mock query, or use a simple stub implementation in the test. The simplest: define a local stub that returns empty collections:

```java
    com.naturalist.library.CitationAssociationQuery citationAssociationQuery =
            new com.naturalist.library.CitationAssociationQuery() {
                @Override
                public com.naturalist.library.CitationAssociationCollection findByCitationName(
                        com.naturalist.authority.CitationName citationName) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }

                @Override
                public com.naturalist.library.CitationAssociationCollection findBySubject(
                        com.naturalist.catalog.EntityRef subject) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }
            };
```

This is sufficient for `InsectQueryImplTest` which only tests constructor validation and accessor delegation — it does not test citation query behavior.

- [ ] **Step 2: Update the InsectQuery construction**

Change line 28-29 from:

```java
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery);
```

To:

```java
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
            orderQuery, citationAssociationQuery);
```

- [ ] **Step 3: Add accessor test**

In `accessors_returnNonNullDelegates`, add:

```java
        assertThat(insectQuery.citations()).isNotNull();
```

In `accessors_idempotent`, add:

```java
        assertThat(insectQuery.citations()).isSameAs(insectQuery.citations());
```

- [ ] **Step 4: Add null-rejection test for citationAssociationQuery**

```java
    @Test
    void constructor_rejectsNullCitationAssociationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("citationAssociationQuery");
    }
```

- [ ] **Step 5: Update the all-nulls test**

Change the `constructor_collectsAllViolationsInSinglePass` test from:

```java
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null))
```

To:

```java
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null, null))
```

And add `"citationAssociationQuery"` to the expected message list:

```java
                .hasMessageContainingAll("speciesQuery", "imageQuery", "familyQuery",
                        "genusQuery", "functionalRoleQuery", "orderQuery",
                        "citationAssociationQuery");
```

- [ ] **Step 6: Update each existing null-rejection test**

Each of the 6 existing `constructor_rejectsNull*` tests constructs `InsectQueryImpl` with 6 args. Add `citationAssociationQuery` as the 7th argument to each. For example:

```java
    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                null, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }
```

Repeat for all 6 existing tests.

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java
git commit -m "test(insects): update InsectQueryImplTest for citation query parameter"
```

### Task 25: Update InsectsTestContext for new constructor parameter

**Files:**
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`
- Modify: `domains/insects/insects-test-context/pom.xml`

`InsectsTestContext` constructs `InsectQueryImpl` at line 51. It needs the new `CitationAssociationQuery` parameter.

- [ ] **Step 1: Add library dependencies to insects-test-context/pom.xml**

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-repository-test</artifactId>
        </dependency>
```

- [ ] **Step 2: Update InsectsTestContext to wire CitationAssociationQuery**

Add imports:

```java
import com.naturalist.library.CitationAssociationQueryImpl;
import com.naturalist.library.CitationAssociationRepositoryMock;
import com.naturalist.library.CitationAssociationQuery;
```

In the constructor, after the existing query construction and before the `insectQuery` construction, add:

```java
        CitationAssociationQuery citationAssociationQuery =
                new CitationAssociationQueryImpl(new CitationAssociationRepositoryMock(db));
```

Wait — `CitationAssociationQueryImpl` and `CitationAssociationRepositoryMock` are package-private in `com.naturalist.library`. `InsectsTestContext` is in `com.naturalist.insects`. This won't compile.

**Resolution:** The library domain needs a test context factory (similar to `InsectsTestContext`) or the mock/query-impl classes need to be promoted to public. The simplest fix: make `CitationAssociationRepositoryMock` and `CitationAssociationQueryImpl` public (they're test infrastructure, not production API leakage — `library-repository-test` is a test module, and `CitationAssociationQueryImpl` is the only adapter).

Alternatively, add a factory method to the library test infrastructure:

Create `LibraryTestContext` or add a static factory to `CitationAssociationRepositoryMock`:

```java
// In CitationAssociationRepositoryMock — make the class public
public class CitationAssociationRepositoryMock ...
```

And in `CitationAssociationQueryImpl` — also make the class public (it's in `library-core`, though, which is production code). Actually, `CitationAssociationQueryImpl` is in `library-core` and IS production code. We should NOT make it public.

**Better approach:** Use the `@DomainService` auto-discovery. `CitationAssociationQueryImpl` is `@DomainService` — Spring will discover it. But `InsectsTestContext` is manual wiring, not Spring.

**Simplest correct approach:** Add a static factory method to the `library-repository-test` module that constructs the mock + query-impl pair and returns the public `CitationAssociationQuery` interface. This is the same concern as `InsectsTestContext.create()`:

Create a small `LibraryCitationTestSupport` class in `library-repository-test`:

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;

public class LibraryCitationTestSupport {

    public static CitationAssociationQuery createQuery(NaturalistDatabase db) {
        return new CitationAssociationQueryImpl(new CitationAssociationRepositoryMock(db));
    }
}
```

This is public, lives in the library-repository-test module (test infrastructure), and returns the public interface.

- [ ] **Step 2 (revised): Create LibraryCitationTestSupport**

Create `domains/library/library-repository-test/src/main/java/com/naturalist/library/LibraryCitationTestSupport.java`:

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test wiring helper — constructs a fully wired {@link CitationAssociationQuery}
 * backed by the in-memory mock. Used by cross-domain test contexts that need
 * citation association behavior without accessing package-private internals.
 */
public class LibraryCitationTestSupport {

    private LibraryCitationTestSupport() {
    }

    public static CitationAssociationQuery createQuery(NaturalistDatabase db) {
        return new CitationAssociationQueryImpl(new CitationAssociationRepositoryMock(db));
    }
}
```

Note: this requires `CitationAssociationQueryImpl` to be accessible from within the `com.naturalist.library` package — it is, since both live there (the mock in `library-repository-test`, the impl in `library-core`, both in `com.naturalist.library`). However, `library-repository-test` depends on `library-api` but NOT `library-core`. `CitationAssociationQueryImpl` is in `library-core`.

**This won't work as written.** The mock is in `library-repository-test`, the query impl is in `library-core`, and `library-repository-test` does not depend on `library-core`.

**Final correct approach:** The `LibraryCitationTestSupport` must live in a module that has both `library-core` and `library-repository-test` on its classpath. The natural home is `insects-test-context` itself (it already depends on both insect modules). Or we can inline the wiring directly in `InsectsTestContext`.

Actually, the `InsectsTestContext` file already constructs `SpeciesQueryImpl`, `ImageQueryImpl` etc. directly — these are package-private classes in `com.naturalist.insects`. `InsectsTestContext` is ALSO in `com.naturalist.insects`, so it can access them.

For the citation query, we need to construct `CitationAssociationQueryImpl` (package-private in `com.naturalist.library`) from `InsectsTestContext` (in `com.naturalist.insects`). These are different packages — won't compile.

**The simplest fix that follows existing patterns:** promote `CitationAssociationQueryImpl` visibility from package-private to public. Looking at the existing code, `CitationQueryImpl` in `library-core` is also package-private. But the library domain doesn't have a test context (it's simpler). For cross-domain test wiring, the query impl needs to be accessible.

Actually — the cleanest approach is the same one used for life stages. Looking at `InsectsTestContext`:

```java
this.insectLifeStageQuery = InsectLifeStageTestContext.createQuery(db);
```

There's an `InsectLifeStageTestContext` that provides a static factory. Let me check where it lives:

- [ ] **Step 2 (actual): Check InsectLifeStageTestContext pattern and follow it**

The `InsectLifeStageTestContext` is imported at `InsectsTestContext:5`. Find where it lives and follow the same pattern for library citations.

Key insight: `InsectLifeStageTestContext.createQuery(db)` is a static factory that returns the public query interface, constructed from package-private internals. It works because `InsectLifeStageTestContext` is in the SAME package as the package-private classes it wraps.

For citations: create `LibraryCitationTestContext` in `com.naturalist.library` within a module that has both `library-core` and `library-repository-test` on its classpath. The natural home is a new source directory in `library-core` test scope, or add the `library-core` dependency to `library-repository-test`.

**Simplest: add `library-core` as a dependency of `library-repository-test`.** Then create `LibraryCitationTestSupport` in `library-repository-test` (package `com.naturalist.library` — same as `CitationAssociationQueryImpl`):

Add to `domains/library/library-repository-test/pom.xml`:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-core</artifactId>
        </dependency>
```

Then create `LibraryCitationTestSupport.java` as shown earlier. It compiles because all classes are in `com.naturalist.library`.

Then in `InsectsTestContext`, add `library-repository-test` as a dependency and use:

```java
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.LibraryCitationTestSupport;

// in constructor:
CitationAssociationQuery citationAssociationQuery = LibraryCitationTestSupport.createQuery(db);
```

- [ ] **Step 3: Add library-core dependency to library-repository-test/pom.xml**

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-core</artifactId>
        </dependency>
```

- [ ] **Step 4: Create LibraryCitationTestSupport**

Create `domains/library/library-repository-test/src/main/java/com/naturalist/library/LibraryCitationTestSupport.java`:

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test wiring helper — constructs a fully wired {@link CitationAssociationQuery}
 * backed by the in-memory mock. Used by cross-domain test contexts that need
 * citation association behavior without accessing package-private internals.
 */
public class LibraryCitationTestSupport {

    private LibraryCitationTestSupport() {
    }

    public static CitationAssociationQuery createQuery(NaturalistDatabase db) {
        return new CitationAssociationQueryImpl(new CitationAssociationRepositoryMock(db));
    }
}
```

- [ ] **Step 5: Add library-repository-test dependency to insects-test-context/pom.xml**

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-repository-test</artifactId>
        </dependency>
```

- [ ] **Step 6: Update InsectsTestContext constructor**

Add import:

```java
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.LibraryCitationTestSupport;
```

In the constructor, before the `insectQuery` construction (line 51), add:

```java
        CitationAssociationQuery citationAssociationQuery =
                LibraryCitationTestSupport.createQuery(db);
```

Update the `InsectQueryImpl` construction to pass the new parameter:

```java
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationAssociationQuery);
```

- [ ] **Step 7: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 8: Commit**

```bash
git add domains/library/library-repository-test/pom.xml \
      domains/library/library-repository-test/src/main/java/com/naturalist/library/LibraryCitationTestSupport.java \
      domains/insects/insects-test-context/pom.xml \
      domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
git commit -m "feat(insects): wire citation query into InsectsTestContext via LibraryCitationTestSupport"
```

### Task 26: InsectCitationQueryImpl test

**Files:**
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCitationQueryImplTest.java`

- [ ] **Step 1: Write the citation discovery test**

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.LibraryCitationTestSupport;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectCitationQueryImplTest {

    static final Observer observer = Observer.forClass(InsectCitationQueryImplTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    // Insect sub-queries
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);

    // Citation association query via library test support
    CitationAssociationQuery citationAssociationQuery =
            LibraryCitationTestSupport.createQuery(db);

    InsectCitationQueryImpl citationQuery = new InsectCitationQueryImpl(
            citationAssociationQuery, speciesQuery, genusQuery, familyQuery);

    @Test
    void findByRankName_rejectsNull() {
        assertThatThrownBy(() -> citationQuery.findByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    void findByRankName_orderWithCitation_returnsCitation() {
        // Fixture: citation on lepidoptera (order)
        InsectCitationView view = citationQuery.findByRankName(
                InsectOrderName.of("lepidoptera"));

        assertThat(view.subject().value()).isEqualTo("lepidoptera");
        assertThat(view.citations()).hasSize(1);
        assertThat(view.citations().getFirst().citationName().value())
                .isEqualTo("eol-battus-philenor-130502");
        assertThat(view.citations().getFirst().attachedAt().value())
                .isEqualTo("lepidoptera");
    }

    @Test
    void findByRankName_familyInheritsFromOrder() {
        // papilionidae has a direct citation AND inherits from lepidoptera
        InsectCitationView view = citationQuery.findByRankName(
                InsectFamilyName.of("papilionidae"));

        assertThat(view.subject().value()).isEqualTo("papilionidae");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_speciesInheritsFullChain() {
        // battus-philenor -> battus (genus) -> papilionidae -> lepidoptera
        InsectCitationView view = citationQuery.findByRankName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(view.subject().value()).isEqualTo("battus-philenor");
        // Inherits from papilionidae and lepidoptera (no citation on battus or battus-philenor)
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_orderWithNoCitations_returnsEmpty() {
        InsectCitationView view = citationQuery.findByRankName(
                TestInsectsIdentifiers.InsectOrder.Diptera.name);

        assertThat(view.subject().value()).isEqualTo("diptera");
        assertThat(view.citations()).isEmpty();
    }

    @Test
    void findByRankName_observesView() {
        InsectCitationView view = citationQuery.findByRankName(
                InsectOrderName.of("lepidoptera"));

        assertThat(observer.observable(view, "citationView").violations()).isEmpty();
    }
}
```

- [ ] **Step 2: Verify tests pass**

The user runs `mvn verify` from the repo root.

- [ ] **Step 3: Commit**

```bash
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCitationQueryImplTest.java
git commit -m "test(insects): add InsectCitationQueryImpl tests with hierarchy walk"
```
