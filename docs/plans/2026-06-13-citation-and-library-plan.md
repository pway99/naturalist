# Citation + Library — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the `Citation` sealed NamedEntity (+ `OnlineSource` permit) to `kernels/authority`, create the `domains/library` domain stack, and add an EOL citation factory.

**Architecture:** `Citation` is a sealed NamedEntity in `com.naturalist.authority` composing `AuthorityReference` for its locator. `OnlineSource` is the first permit — a record for HTTP-accessible authority resources. The `domains/library` domain module owns persistence (repository, mock, contract tests). The EOL module gains a factory method for constructing citations.

**Tech Stack:** Java 21 records, sealed interfaces, Jackson polymorphic deserialization, framework Observer/Constraints, TestEntitySource JSON fixtures.

**Design doc:** [`2026-06-13-citation-and-library-design.md`](2026-06-13-citation-and-library-design.md)

---

### Task 1: CitationName — EntityName subclass

Kernel-level EntityName stays alongside its entity in `kernels/authority` per convention.

**Files:**
- Create: `kernels/authority/src/main/java/com/naturalist/authority/CitationName.java`

- [ ] **Step 1: Write CitationName**

```java
package com.naturalist.authority;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.naturalist.ddd.EntityName;

public final class CitationName extends EntityName {

    @JsonCreator
    public static CitationName of(@JsonProperty("value") String value) {
        return new CitationName(value);
    }

    private CitationName(String value) {
        super(value);
    }

    @Override
    protected int maxLength() {
        return 200;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/CitationName.java
git commit -m "feat(authority): add CitationName EntityName subclass"
```

---

### Task 2: Citation sealed interface + OnlineSource permit

Follows the `LifeStage` pattern: sealed NamedEntity interface with top-level record permits in the same package. Jackson `@JsonTypeInfo`/`@JsonSubTypes` on the interface for polymorphic deserialization.

**Files:**
- Create: `kernels/authority/src/main/java/com/naturalist/authority/Citation.java`
- Create: `kernels/authority/src/main/java/com/naturalist/authority/OnlineSource.java`

- [ ] **Step 1: Write Citation sealed interface**

```java
package com.naturalist.authority;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.naturalist.ddd.NamedEntity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnlineSource.class, name = "ONLINE_SOURCE")
})
public sealed interface Citation extends NamedEntity<CitationName>
        permits OnlineSource {

    AuthorityReference authorityReference();

    String title();
}
```

- [ ] **Step 2: Write OnlineSource record**

```java
package com.naturalist.authority;

import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

public record OnlineSource(
        CitationName name,
        AuthorityReference authorityReference,
        String title,
        @Nullable String author,
        @Nullable Integer year,
        @Nullable Instant lastModified
) implements Citation {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(authorityReference, "authorityReference")
                .notBlank(title, "title");
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/Citation.java \
       kernels/authority/src/main/java/com/naturalist/authority/OnlineSource.java
git commit -m "feat(authority): add Citation sealed NamedEntity + OnlineSource permit"
```

---

### Task 3: Citation kernel tests

Per the kernel testing convention, `Citation` has no immediate consumer yet (the library domain comes in Task 5+), so direct kernel tests document the semantics. Follows the existing `AuthorityReferenceTest` pattern.

**Files:**
- Create: `kernels/authority/src/test/java/com/naturalist/authority/CitationTest.java`

- [ ] **Step 1: Write the invariant tests**

```java
package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CitationTest {

    private static final Observer observer = Observer.forClass(CitationTest.class);

    private static final AuthoritySource EOL = new AuthoritySource("eol", "Encyclopedia of Life");
    private static final AuthorityReference EOL_SWALLOWTAIL =
            new AuthorityReference(EOL, URI.create("https://eol.org/pages/1188585"));

    @Test
    void wellFormedOnlineSourcePassesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "Battus philenor — Encyclopedia of Life",
                "EOL Curators",
                2024,
                Instant.parse("2024-03-15T00:00:00Z"));

        InvariantObservation result = observer.forMethod("wellFormedOnlineSourcePassesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void onlineSourceWithNullablesOmittedPassesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "Battus philenor — Encyclopedia of Life",
                null, null, null);

        InvariantObservation result = observer.forMethod("onlineSourceWithNullablesOmittedPassesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullNameViolatesInvariants() {
        Citation citation = new OnlineSource(
                null,
                EOL_SWALLOWTAIL,
                "Battus philenor",
                null, null, null);

        InvariantObservation result = observer.forMethod("nullNameViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".name"));
    }

    @Test
    void nullAuthorityReferenceViolatesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                null,
                "Battus philenor",
                null, null, null);

        InvariantObservation result = observer.forMethod("nullAuthorityReferenceViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".authorityReference"));
    }

    @Test
    void blankTitleViolatesInvariants() {
        Citation citation = new OnlineSource(
                CitationName.of("eol-battus-philenor-1188585"),
                EOL_SWALLOWTAIL,
                "   ",
                null, null, null);

        InvariantObservation result = observer.forMethod("blankTitleViolatesInvariants")
                .namedEntity(citation, "citation");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".title"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl kernels/authority -Dtest=CitationTest`
Expected: all 5 tests PASS

- [ ] **Step 3: Commit**

```bash
git add kernels/authority/src/test/java/com/naturalist/authority/CitationTest.java
git commit -m "test(authority): add Citation invariant tests"
```

---

### Task 4: Library domain Maven scaffolding

Create the `domains/library` parent module with three sub-modules. Register in `domains/pom.xml` and root `pom.xml` dependency management.

**Files:**
- Create: `domains/library/pom.xml`
- Create: `domains/library/library-api/pom.xml`
- Create: `domains/library/library-core/pom.xml`
- Create: `domains/library/library-repository-test/pom.xml`
- Modify: `domains/pom.xml` — add `<module>library</module>`
- Modify: `pom.xml` (root) — add dependency management entries for `library-api`, `library-core`, `library-repository-test`

- [ ] **Step 1: Create parent pom**

`domains/library/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>domains</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>library</artifactId>
    <packaging>pom</packaging>
    <name>domains :: Library</name>
    <modules>
        <module>library-api</module>
        <module>library-core</module>
        <module>library-repository-test</module>
    </modules>
</project>
```

- [ ] **Step 2: Create library-api pom**

`domains/library/library-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>library</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>library-api</artifactId>
    <name>domains :: Library :: API</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>authority</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>

        <!-- TEST DEPENDENCIES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create library-core pom**

`domains/library/library-core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>library</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>library-core</artifactId>
    <name>domains :: Library :: Core</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>

        <!-- TEST DEPENDENCIES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>identifiers-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-repository-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create library-repository-test pom**

`domains/library/library-repository-test/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>library</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>library-repository-test</artifactId>
    <name>domains :: Library :: Repository Test</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>identifiers-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: Create source directories**

```bash
mkdir -p domains/library/library-api/src/main/java/com/naturalist/library
mkdir -p domains/library/library-api/src/test/java/com/naturalist/library
mkdir -p domains/library/library-core/src/main/java/com/naturalist/library
mkdir -p domains/library/library-core/src/test/java/com/naturalist/library
mkdir -p domains/library/library-repository-test/src/main/java/com/naturalist/library
mkdir -p domains/library/library-repository-test/src/main/resources/library
mkdir -p domains/library/library-repository-test/src/test/java/com/naturalist/library
```

- [ ] **Step 6: Add module to domains/pom.xml**

Insert `<module>library</module>` in the `<modules>` list, alphabetically after `insects`.

- [ ] **Step 7: Add dependency management entries to root pom.xml**

In the `<dependencyManagement>` section, add a `<!-- LIBRARY -->` block in the DOMAINS section, alphabetically between INSECTS and MICROBES:

```xml
<!-- LIBRARY -->
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-api</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-core</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-repository-test</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 8: Verify build compiles**

Run: `mvn compile -pl domains/library/library-api,domains/library/library-core,domains/library/library-repository-test -am`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add domains/library/ domains/pom.xml pom.xml
git commit -m "feat(library): scaffold domains/library Maven modules"
```

---

### Task 5: CitationRepository + CitationQuery + CitationCollection

N=1 collapse rule — one entity in the library domain, so top-level interfaces (no namespace class).

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationRepository.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationQuery.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/CitationCollection.java`

- [ ] **Step 1: Write CitationRepository**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.EntityRepository;

interface CitationRepository extends EntityRepository<CitationName, Citation> {
}
```

- [ ] **Step 2: Write CitationQuery**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.EntityQuery;

public interface CitationQuery extends EntityQuery<CitationName, Citation, CitationCollection> {
}
```

- [ ] **Step 3: Write CitationCollection**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class CitationCollection extends BehavioralCollection<Citation> {

    CitationCollection(Collection<Citation> citations) {
        super(citations);
    }

    public static CitationCollection of(Collection<Citation> citations) {
        return new CitationCollection(citations);
    }

    public static CitationCollection empty() {
        return new CitationCollection(List.of());
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `mvn compile -pl domains/library/library-api -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/
git commit -m "feat(library): add CitationRepository, CitationQuery, CitationCollection"
```

---

### Task 6: TestLibraryIdentifiers

Test identity constants for the library domain. Follows the `TestChemistryIdentifiers` pattern.

**Files:**
- Create: `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java`
- Modify: `domains/identifiers-test/pom.xml` — add `authority` dependency if not present

- [ ] **Step 1: Check identifiers-test pom.xml**

Read `domains/identifiers-test/pom.xml`. Verify `authority` is listed as a dependency. If not, add it — `CitationName` lives in `kernels/authority`.

- [ ] **Step 2: Write TestLibraryIdentifiers**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;

public class TestLibraryIdentifiers {

    public static class Citations {

        public static final CitationName EolSwallowtail =
                CitationName.of("eol-battus-philenor-1188585");

        public static final CitationName EolGreenLacewing =
                CitationName.of("eol-chrysoperla-rufilabris-2774541");

        public static final CitationName EolMonarchButterfly =
                CitationName.of("eol-danaus-plexippus-151935");

        public static final CitationName EolHoneyBee =
                CitationName.of("eol-apis-mellifera-1045608");

        public static class NotFound {
            public static final CitationName name =
                    CitationName.of("eol-unobtainium-bug-9999999");
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java \
       domains/identifiers-test/pom.xml
git commit -m "feat(library): add TestLibraryIdentifiers"
```

---

### Task 7: CitationTestEntitySource + JSON fixtures

Use the `/test-entity-source` skill for scaffolding guidance, then adapt for the sealed-interface specifics.

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationTestEntitySource.java`
- Create: `domains/library/library-repository-test/src/main/resources/library/citations.json`
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationTestEntitySourceTest.java`

- [ ] **Step 1: Write citations.json**

At least 4 entries for meaningful contract coverage (per `TestEntitySourceTest` minimum). Each entry needs the `"kind"` discriminator for Jackson polymorphic deserialization.

```json
[
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-battus-philenor-1188585",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/1188585"
    },
    "title": "Battus philenor — Encyclopedia of Life",
    "author": "EOL Curators",
    "year": 2024,
    "lastModified": "2024-03-15T00:00:00Z"
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-chrysoperla-rufilabris-2774541",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/2774541"
    },
    "title": "Chrysoperla rufilabris — Encyclopedia of Life",
    "author": "EOL Curators",
    "year": 2023,
    "lastModified": "2023-11-20T00:00:00Z"
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-danaus-plexippus-151935",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/151935"
    },
    "title": "Danaus plexippus — Encyclopedia of Life",
    "author": null,
    "year": 2024,
    "lastModified": null
  },
  {
    "kind": "ONLINE_SOURCE",
    "name": "eol-apis-mellifera-1045608",
    "authorityReference": {
      "source": { "id": "eol", "displayName": "Encyclopedia of Life" },
      "url": "https://eol.org/pages/1045608"
    },
    "title": "Apis mellifera — Encyclopedia of Life",
    "author": "EOL Curators",
    "year": 2024,
    "lastModified": "2024-06-01T00:00:00Z"
  }
]
```

- [ ] **Step 2: Write CitationTestEntitySource**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.NaturalistDatabase;

public class CitationTestEntitySource extends TestEntitySource<CitationName, Citation> {

    public CitationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/citations.json");
    }
}
```

- [ ] **Step 3: Write CitationTestEntitySourceTest**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.TestEntitySourceTest;

class CitationTestEntitySourceTest
        extends TestEntitySourceTest<CitationName, Citation, CitationTestEntitySource> {
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -pl domains/library/library-repository-test -Dtest=CitationTestEntitySourceTest`
Expected: `dataLoads` and `hasAtLeastFourEntities` PASS

- [ ] **Step 5: Commit**

```bash
git add domains/library/library-repository-test/src/
git commit -m "feat(library): add CitationTestEntitySource + JSON fixtures"
```

---

### Task 8: CitationRepositoryMock + contract test

Use the `/entity-repository` skill for scaffolding guidance.

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationRepositoryMock.java`
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationEntityRepositoryTest.java`
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationEntityRepositoryMockTest.java`

- [ ] **Step 1: Write CitationRepositoryMock**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class CitationRepositoryMock
        extends AbstractTestEntityRepository<CitationName, Citation, CitationTestEntitySource>
        implements CitationRepository {

    protected CitationRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 2: Write CitationEntityRepositoryTest contract interface**

```java
package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.net.URI;
import java.util.List;

interface CitationEntityRepositoryTest
        extends EntityRepositoryTest<CitationName, Citation> {

    @Override
    CitationRepository repository();

    @Override
    default TestEntitySource<CitationName, Citation> source() {
        return db.getNamed(CitationTestEntitySource.class);
    }

    @Override
    default CitationName notFoundName() {
        return TestLibraryIdentifiers.Citations.NotFound.name;
    }

    @Override
    default List<CitationName> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.Citations.EolSwallowtail,
                TestLibraryIdentifiers.Citations.EolGreenLacewing
        );
    }

    @Override
    default Citation newEntity() {
        return new OnlineSource(
                CitationName.of(RandomValue.string()),
                new AuthorityReference(
                        new AuthoritySource("test", "Test Authority"),
                        URI.create("https://example.com/" + RandomValue.string())),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.integer(),
                null);
    }

    @Override
    default Citation ghostEntity() {
        return new OnlineSource(
                CitationName.of(RandomValue.string()),
                new AuthorityReference(
                        new AuthoritySource("test", "Test Authority"),
                        URI.create("https://example.com/" + RandomValue.string())),
                RandomValue.string(),
                null, null, null);
    }

    @Override
    default Citation modifiedEntity(Citation original) {
        return new OnlineSource(
                original.name(),
                new AuthorityReference(
                        new AuthoritySource("modified", "Modified Authority"),
                        URI.create("https://modified.com/" + RandomValue.string())),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.integer(),
                null);
    }
}
```

- [ ] **Step 3: Write CitationEntityRepositoryMockTest**

```java
package com.naturalist.library;

class CitationEntityRepositoryMockTest implements CitationEntityRepositoryTest {
    @Override
    public CitationRepository repository() {
        return new CitationRepositoryMock(db);
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -pl domains/library/library-repository-test`
Expected: all inherited contract tests PASS (getByName, getByEntityNameSet, getPage, insert, update — ~15+ tests)

- [ ] **Step 5: Commit**

```bash
git add domains/library/library-repository-test/src/
git commit -m "feat(library): add CitationRepositoryMock + behavioral contract tests"
```

---

### Task 9: CitationQueryImpl + test

The query implementation in `library-core`. Thin adapter per ADR-010 — observe, dispatch, delegate.

**Files:**
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CitationQueryImpl.java`
- Create: `domains/library/library-core/src/test/java/com/naturalist/library/CitationQueryImplTest.java`

- [ ] **Step 1: Write CitationQueryImpl**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;
import java.util.Set;

@DomainService
class CitationQueryImpl implements CitationQuery {

    private static final Observer observer = Observer.forClass(CitationQueryImpl.class);

    private final CitationRepository repository;

    CitationQueryImpl(CitationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Citation> getByName(CitationName name) {
        observer.arguments("getByName", i -> i.notNull(name, "name")).throwWhenInvalid();
        return repository.getByName(name);
    }

    @Override
    public CitationCollection findByNameSet(Set<CitationName> names) {
        observer.arguments("findByNameSet", i -> i.notNull(names, "names")).throwWhenInvalid();
        return CitationCollection.of(repository.getByEntityNameSet(names));
    }

    @Override
    public Page<Citation> findPage(PageRequest pageRequest) {
        observer.arguments("findPage", i -> i.notNull(pageRequest, "pageRequest")).throwWhenInvalid();
        return repository.getPage(pageRequest);
    }
}
```

- [ ] **Step 2: Write CitationQueryImplTest**

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    CitationRepository repository = new CitationRepositoryMock(db);
    CitationQuery citationQuery = new CitationQueryImpl(repository);

    @Test
    void findByNameSet_happyPath() {
        Set<CitationName> names = Set.of(
                TestLibraryIdentifiers.Citations.EolSwallowtail,
                TestLibraryIdentifiers.Citations.EolGreenLacewing
        );

        CitationCollection collection = citationQuery.findByNameSet(names);

        assertThat(collection).isNotNull();
        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(c -> c.name()))
                .containsExactlyInAnyOrderElementsOf(names);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -pl domains/library/library-core`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add domains/library/library-core/src/
git commit -m "feat(library): add CitationQueryImpl"
```

---

### Task 10: Eol.citation() factory method + test

Add the citation factory to the existing `Eol` utility class. The factory wires EOL-specific knowledge (source constant, deep link format) so callers pass domain-meaningful arguments.

**Files:**
- Modify: `external-authorities/eol/eol-api/src/main/java/com/naturalist/authority/eol/Eol.java`
- Create: `external-authorities/eol/eol-api/src/test/java/com/naturalist/authority/eol/EolCitationTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.naturalist.authority.eol;

import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EolCitationTest {

    private static final Observer observer = Observer.forClass(EolCitationTest.class);

    @Test
    void citationFactoryProducesValidOnlineSource() {
        OnlineSource citation = Eol.citation(
                CitationName.of("eol-battus-philenor-1188585"),
                new EolPageId("1188585"),
                "Battus philenor — Encyclopedia of Life",
                "EOL Curators",
                2024,
                Instant.parse("2024-03-15T00:00:00Z"));

        assertThat(citation.name().value()).isEqualTo("eol-battus-philenor-1188585");
        assertThat(citation.authorityReference().source()).isEqualTo(Eol.SOURCE);
        assertThat(citation.authorityReference().url().toString())
                .isEqualTo("https://eol.org/pages/1188585");
        assertThat(citation.title()).isEqualTo("Battus philenor — Encyclopedia of Life");
        assertThat(citation.author()).isEqualTo("EOL Curators");
        assertThat(citation.year()).isEqualTo(2024);

        InvariantObservation result = observer.forMethod("citationFactoryProducesValidOnlineSource")
                .namedEntity(citation, "citation");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void citationFactoryWithNullOptionalsProducesValidOnlineSource() {
        OnlineSource citation = Eol.citation(
                CitationName.of("eol-apis-mellifera-1045608"),
                new EolPageId("1045608"),
                "Apis mellifera — Encyclopedia of Life",
                null, null, null);

        assertThat(citation.author()).isNull();
        assertThat(citation.year()).isNull();
        assertThat(citation.lastModified()).isNull();

        InvariantObservation result = observer.forMethod("citationFactoryWithNullOptionalsProducesValidOnlineSource")
                .namedEntity(citation, "citation");
        assertThat(result.violations()).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl external-authorities/eol/eol-api -Dtest=EolCitationTest`
Expected: FAIL — `Eol.citation` method does not exist

- [ ] **Step 3: Add the factory method to Eol.java**

Add to `Eol.java`:

```java
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.authority.AuthorityReference;
import org.jspecify.annotations.Nullable;
import java.time.Instant;

// inside the Eol class:

public static OnlineSource citation(
        CitationName name,
        EolPageId pageId,
        String title,
        @Nullable String author,
        @Nullable Integer year,
        @Nullable Instant lastModified) {
    return new OnlineSource(
            name,
            new AuthorityReference(SOURCE, deepLink(pageId)),
            title, author, year, lastModified);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl external-authorities/eol/eol-api -Dtest=EolCitationTest`
Expected: both tests PASS

- [ ] **Step 5: Run the full eol-api test suite**

Run: `mvn test -pl external-authorities/eol/eol-api`
Expected: all existing tests still PASS (no regressions)

- [ ] **Step 6: Commit**

```bash
git add external-authorities/eol/eol-api/src/main/java/com/naturalist/authority/eol/Eol.java \
       external-authorities/eol/eol-api/src/test/java/com/naturalist/authority/eol/EolCitationTest.java
git commit -m "feat(eol): add Eol.citation() factory method"
```

---

### Task 11: Update work-tracker + roadmap docs

Update the work-tracker to reflect the completed slice and note the naming changes (bibliography → citation, LiteratureReference → Citation).

**Files:**
- Modify: `docs/work-tracker.md`
- Modify: `docs/plans/identification.md` (add a note at the Phase 2 section referencing the naming decision)

- [ ] **Step 1: Update work-tracker**

Move the `kernels/bibliography` candidate next slice entry to "Recently completed" and update the "Current slice" section. Update naming throughout.

- [ ] **Step 2: Add naming note to identification.md**

At the top of Phase 2, add a brief note:

```markdown
> **Naming decision (2026-06-13).** Brainstorming renamed:
> `kernels/bibliography` → types join `kernels/authority` (no new kernel);
> `LiteratureReference` → `Citation` (sealed NamedEntity, format-polymorphic);
> the domain is `domains/library`. See
> [`2026-06-13-citation-and-library-design.md`](2026-06-13-citation-and-library-design.md).
```

- [ ] **Step 3: Commit**

```bash
git add docs/work-tracker.md docs/plans/identification.md
git commit -m "docs: update work-tracker and roadmap for citation/library slice"
```
