# Clades & Taxonomy Console Teaching Surface — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Describe clades and taxonomy to the naturalist in the management console — two concept pages, a browsable tree of life, and cross-links from insect pages into both.

**Architecture:** A new `Concept` NamedEntity sub-context in the existing `library` domain (mirroring `Citation`) holds the two meta-concept descriptions as data behind a `ConceptQuery`; the console talks only to the query. A new `kernels/clades` `CladeCatalog` enumerates the sealed permits (via `getPermittedSubclasses()`) so a tree can be built. App-level `ConceptsController` and `CladesController` in `management-console` render both, reusing the existing `components/description.jte` four-level Durrell renderer.

**Tech Stack:** Java 21 records & sealed types, Jackson slug serialization, JUnit 5, AssertJ, Spring Boot MVC, JTE templates, Maven multi-module.

**Design source:** `docs/plans/2026-06-13-clades-taxonomy-console-design.md`
**Content source:** `docs/notes/clade-assignment-investigation/clades-taxonomy-durrell.md`

**Coordination:** The concurrent paraphyly-fixtures plan (`docs/plans/2026-06-13-paraphyly-fixtures.md`) owns all edits to `Clade.java`, `CladeTest.java`, the clade permit files, `InsectClades`, and the insect catalog JSON. This plan **adds one new file** to `kernels/clades` (`CladeCatalog.java`) and edits **none** of those. The clade permits it depends on already exist in the kernel.

> **Build/commit note:** The user runs Maven (`mvn verify` from repo root) and `git push` themselves; do not invoke `mvn` or push. Commit steps stage and commit only as the execution skill directs.

---

## File Structure

**New domain sub-context (`library`):**
- `domains/identifiers/src/main/java/com/naturalist/library/ConceptName.java` — typed slug identity.
- `domains/library/library-api/src/main/java/com/naturalist/library/Concept.java` — the NamedEntity record.
- `domains/library/library-api/src/main/java/com/naturalist/library/ConceptRepository.java` — package-private repository port.
- `domains/library/library-api/src/main/java/com/naturalist/library/ConceptQuery.java` — public query port.
- `domains/library/library-api/src/main/java/com/naturalist/library/ConceptCollection.java` — behavioral collection.
- `domains/library/library-core/src/main/java/com/naturalist/library/ConceptQueryImpl.java` — query adapter.
- `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptTestEntitySource.java` — test data source.
- `domains/library/library-repository-test/src/main/resources/library/concepts.json` — the two concept records (single source of truth for the console).
- `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptRepositoryMock.java` — in-memory mock.
- `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptEntityRepositoryTest.java` — behavioral contract.
- `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java` — add `Concepts` constants.
- `domains/library/library-test-context/**` — new module assembling `ConceptQuery` for the console.

**Kernel:**
- `kernels/clades/src/main/java/com/naturalist/clades/CladeCatalog.java` — enumeration + children.

**App (`management-console`):**
- `apps/management-console/src/main/java/com/naturalist/console/concept/ConceptsController.java`
- `apps/management-console/src/main/java/com/naturalist/console/clade/CladesController.java`
- `apps/management-console/src/main/jte/concepts/list.jte`, `concepts/detail.jte`
- `apps/management-console/src/main/jte/clades/tree.jte`, `clades/node.jte`, `clades/detail.jte`

**Edits:** `library-api/pom.xml` (add `identifiers`, `field-notes`), `library/pom.xml` (add module), root `pom.xml` (dep-mgmt entry), `management-console/pom.xml` (library deps), `page.jte` ("More" nav), `InsectsController.java` (`CLADE_URL`).

---

## Task 1: `Concept` entity + `ConceptName` identifier

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/library/ConceptName.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/Concept.java`
- Test: `domains/library/library-api/src/test/java/com/naturalist/library/ConceptTest.java`
- Modify: `domains/library/library-api/pom.xml`

- [ ] **Step 1: Create `ConceptName`**

```java
package com.naturalist.library;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Natural key for {@code Concept} — a human-readable slug (e.g. {@code "clade"},
 * {@code "clade-taxonomy-relation"}) stable across deployments. Values match the
 * {@code "name"} field in {@code concepts.json}.
 */
public final class ConceptName extends EntityName {

    private ConceptName(String value) {
        super(value);
    }

    @JsonCreator
    public static ConceptName of(String value) {
        return new ConceptName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
```

- [ ] **Step 2: Add `identifiers` + `field-notes` deps to `library-api/pom.xml`**

In `domains/library/library-api/pom.xml`, replace the `framework` dependency block (lines 19–22) so the compile dependencies read:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>identifiers</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>field-notes</artifactId>
        </dependency>
```

(Leave the `authority` dependency and the test-scoped `framework-test` dependency unchanged.)

- [ ] **Step 3: Write the failing test `ConceptTest`**

```java
package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConceptTest {

    private static final Observer observer = Observer.forClass(ConceptTest.class);

    @Test
    void validConceptPassesAllInvariants() {
        var mo = observer.forMethod("validConceptPassesAllInvariants");
        Concept concept = new Concept(
                ConceptName.of("clade"),
                "What is a clade?",
                description());

        InvariantObservation result = mo.namedEntity(concept, "concept");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedInvariantViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedInvariantViolations");
        Concept concept = new Concept(null, null, null);

        InvariantObservation result = mo.namedEntity(concept, "concept");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".concept.name",
                        ".concept.title",
                        ".concept.description");
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
```

- [ ] **Step 4: Run the test — expect FAIL (Concept does not compile/exist)**

Run: `mvn -q -pl domains/library/library-api test`
Expected: compilation failure — `Concept` not found.

- [ ] **Step 5: Create `Concept`**

```java
package com.naturalist.library;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A teaching/reference entry — a human-titled, four-level Durrell
 * {@link Description} of a concept the catalog needs to explain (e.g. what a
 * clade is, how clades relate to taxonomy). A parallel sub-context to
 * {@code Citation} in the library domain; the two never interact.
 */
public record Concept(
        ConceptName name,
        String title,
        Description description
) implements NamedEntity<ConceptName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(title, "title")
                .valueObject(description, "description");
    }
}
```

- [ ] **Step 6: Run the test — expect PASS**

Run: `mvn -q -pl domains/library/library-api test`
Expected: `ConceptTest` passes (2 tests green).

- [ ] **Step 7: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/library/ConceptName.java \
        domains/library/library-api/src/main/java/com/naturalist/library/Concept.java \
        domains/library/library-api/src/test/java/com/naturalist/library/ConceptTest.java \
        domains/library/library-api/pom.xml
git commit -m "feat(library): add Concept NamedEntity + ConceptName

Teaching/reference entry carrying a human title and a four-level Durrell
Description. Parallel sub-context to Citation; adds identifiers + field-notes
deps to library-api.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: `Concept` query stack (api + core)

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/ConceptRepository.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/ConceptQuery.java`
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/ConceptCollection.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/ConceptQueryImpl.java`
- Test: `domains/library/library-core/src/test/java/com/naturalist/library/ConceptQueryImplTest.java`

This task's test depends on the repository mock + test identifiers from Task 3. To keep the order TDD-clean, write the production code here and the `ConceptQueryImplTest` in Task 3 Step 7 (after the mock and `concepts.json` exist). The production interfaces below compile on their own.

- [ ] **Step 1: Create `ConceptRepository` (package-private)**

```java
package com.naturalist.library;

import com.naturalist.data.EntityRepository;

interface ConceptRepository extends EntityRepository<ConceptName, Concept> {
}
```

- [ ] **Step 2: Create `ConceptCollection`**

```java
package com.naturalist.library;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class ConceptCollection extends BehavioralCollection<Concept> {

    ConceptCollection(Collection<Concept> concepts) {
        super(concepts);
    }

    public static ConceptCollection of(Collection<Concept> concepts) {
        return new ConceptCollection(concepts);
    }

    public static ConceptCollection empty() {
        return new ConceptCollection(List.of());
    }
}
```

- [ ] **Step 3: Create `ConceptQuery` (public)**

```java
package com.naturalist.library;

import com.naturalist.data.EntityQuery;

public interface ConceptQuery extends EntityQuery<ConceptName, Concept, ConceptCollection> {
}
```

- [ ] **Step 4: Create `ConceptQueryImpl`**

```java
package com.naturalist.library;

import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;
import java.util.Set;

@DomainService
class ConceptQueryImpl implements ConceptQuery {

    private static final Observer observer = Observer.forClass(ConceptQueryImpl.class);

    private final ConceptRepository repository;

    ConceptQueryImpl(ConceptRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Concept> getByName(ConceptName name) {
        observer.arguments("getByName", i -> i.notNull(name, "name")).throwWhenInvalid();
        return repository.getByName(name);
    }

    @Override
    public ConceptCollection findByNameSet(Set<ConceptName> names) {
        observer.arguments("findByNameSet", i -> i.notNull(names, "names")).throwWhenInvalid();
        return ConceptCollection.of(repository.getByEntityNameSet(names));
    }

    @Override
    public Page<Concept> findPage(PageRequest pageRequest) {
        observer.arguments("findPage", i -> i.notNull(pageRequest, "pageRequest")).throwWhenInvalid();
        return repository.getPage(pageRequest);
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q -pl domains/library/library-api,domains/library/library-core -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/ConceptRepository.java \
        domains/library/library-api/src/main/java/com/naturalist/library/ConceptQuery.java \
        domains/library/library-api/src/main/java/com/naturalist/library/ConceptCollection.java \
        domains/library/library-core/src/main/java/com/naturalist/library/ConceptQueryImpl.java
git commit -m "feat(library): add Concept repository port, query, collection + impl

Mirrors the Citation stack (N=1 collapse: flat top-level repository + query).

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Test data — source, JSON, mock, contract, identifiers

**Files:**
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptTestEntitySource.java`
- Create: `domains/library/library-repository-test/src/main/resources/library/concepts.json`
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptRepositoryMock.java`
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/ConceptEntityRepositoryTest.java`
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/ConceptEntityRepositoryMockTest.java`
- Create: `domains/library/library-repository-test/src/test/java/com/naturalist/library/ConceptTestEntitySourceTest.java`
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java`
- Create: `domains/library/library-core/src/test/java/com/naturalist/library/ConceptQueryImplTest.java`

- [ ] **Step 1: Create `concepts.json`** (prose copied verbatim from the content source; `title` added)

```json
[
  {
    "name": "clade",
    "title": "What is a clade?",
    "description": {
      "preschool": "A clade is one whole branch of the tree of life — a parent and all its babies, and their babies, with nobody left out. If you snap a branch off a tree, everything still attached to it is a clade.",
      "elementary": "Every living thing has ancestors, the way a family has grandparents and great-grandparents. A clade is one complete family on the giant tree of life: a single ancestor and all of its descendants — the whole branch and nothing but the branch. If you leave even one descendant out, it stops being a clade, because you have broken the branch.",
      "secondary": "A clade is a monophyletic group — one common ancestor together with every species descended from it. Clades are the natural units of evolutionary classification because they are real branches of the tree of life, defined by shared ancestry rather than by how similar things look. A grouping that looks tidy but leaves some descendants out is a \"grade,\" not a clade: birds plus their dinosaur ancestors form a clade, but \"reptiles\" drawn so as to exclude birds does not.",
      "university": "A clade is a monophyletic taxon: an ancestor and the complete set of its descendants — the subtree produced by a single cut on the phylogeny. Clades are recovered, not stipulated — hypotheses inferred from shared derived characters (synapomorphies) or molecular signal — and they nest strictly, so any two clades are either disjoint or one wholly contains the other. Groupings that omit some descendants of their common ancestor are paraphyletic (\"fish,\" \"reptiles,\" cockroaches-excluding-termites); groupings stitched together from unrelated lineages by convergence are polyphyletic. Only monophyletic groups carry the inferential payload of common descent — predict a member's biology from the clade — which is why cladistics treats the clade, not the rank, as the meaningful unit. Clade membership is independent of Linnaean rank: a clade can be vast and supra-ordinal (Holometabola), tiny (a tribe), or correspond to no rank at all (Anthophila, the bees)."
    }
  },
  {
    "name": "clade-taxonomy-relation",
    "title": "How do clades relate to taxonomy?",
    "description": {
      "preschool": "Scientists sort living things into nested boxes — small boxes inside bigger boxes — and name each box, like \"genus\" or \"family.\" A clade is a real branch of the tree of life. Most of the time the boxes match the branches, but sometimes a branch does not fit neatly into a box.",
      "elementary": "Taxonomy is the naming-and-sorting system: each animal goes in a species, inside a genus, inside a family, inside an order — boxes inside boxes. Clades come from the actual family tree of life. Usually a family or genus is a clade, a real branch. But people drew many of the boxes long ago, by how creatures looked, before they knew the tree — so a few boxes accidentally leave out a relative. A termite looks nothing like a cockroach, so termites got their own box; but on the tree, termites are just one twig growing straight out of the cockroach branch.",
      "secondary": "Linnaean taxonomy is a ranked hierarchy of nested categories (order, family, genus, species), each a named container at a fixed level. Cladistics describes the same organisms by their branching ancestry. The two usually agree — most families and genera are clades — but they are different kinds of structure: ranks are stipulated and rank-bound; clades are inferred and rank-free. They part ways when a traditional rank, drawn on overall resemblance, turns out to be paraphyletic — excluding some descendants of its own ancestor. \"Reptiles\" (minus birds) and \"cockroaches\" (minus termites) are such ranks. Modern classification keeps redrawing ranks to match clades — Blattodea now formally includes termites — but the rank system can never perfectly mirror the tree, because the tree branches at far more points than there are ranks to name.",
      "university": "Taxonomy and phylogeny are two distinct mappings over the same organisms. Linnaean taxonomy imposes a small, fixed set of nested ranks — a stipulated containment hierarchy in which each taxon sits at exactly one rank; cladistics recovers the branching tree, a rank-free hierarchy of nested monophyletic groups. They coincide only where a rank's circumscription happens to be monophyletic, and three mismatches recur. First, clades fall between or across ranks — Anthophila is a clade with no rank, Holometabola is supra-ordinal — so the rank system cannot host every clade. Second, traditional ranks are frequently paraphyletic or polyphyletic, having been circumscribed phenetically before phylogenetics: Isoptera nested within Blattodea, the apoid wasps paraphyletic with respect to bees, the genus Drosophila paraphyletic with other genera nested inside it. Third, because ranks are mutually exclusive boxes while clades nest at arbitrarily many depths, no rank assignment can capture all the nesting — there are always more branch points than rank labels. Phylogenetic nomenclature (the PhyloCode) responds by naming clades directly and dropping ranks; rank-based codes (ICZN) respond by recircumscribing taxa toward monophyly while keeping ranks — which is why familiar names migrate (Reticulitermes' family; Philanthus moving from Crabronidae to Philanthidae) and why a genus split can threaten a famous binomial (Drosophila melanogaster). The working rule for a naturalist: read the rank as a name and a filing convenience, and the clade as the claim about ancestry — when they disagree, the clade carries the biology."
    }
  }
]
```

- [ ] **Step 2: Create `ConceptTestEntitySource`**

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class ConceptTestEntitySource extends TestEntitySource<ConceptName, Concept> {

    public ConceptTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/concepts.json");
    }
}
```

- [ ] **Step 3: Create `ConceptRepositoryMock`**

```java
package com.naturalist.library;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class ConceptRepositoryMock
        extends AbstractTestEntityRepository<ConceptName, Concept, ConceptTestEntitySource>
        implements ConceptRepository {

    protected ConceptRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 4: Add `Concepts` to `TestLibraryIdentifiers`**

In `domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java`, add a new import and a `Concepts` static class inside the existing `TestLibraryIdentifiers` class (alongside `Citations`):

Add import near the top:
```java
import com.naturalist.library.ConceptName;
```

Add inside the class body:
```java
    public static class Concepts {

        public static final ConceptName Clade = ConceptName.of("clade");

        public static final ConceptName CladeTaxonomyRelation =
                ConceptName.of("clade-taxonomy-relation");

        public static class NotFound {
            public static final ConceptName name = ConceptName.of("unobtainium-concept");
        }
    }
```

- [ ] **Step 5: Create `ConceptEntityRepositoryTest` (behavioral contract)**

```java
package com.naturalist.library;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.Description;

import java.util.List;

interface ConceptEntityRepositoryTest
        extends EntityRepositoryTest<ConceptName, Concept> {

    @Override
    ConceptRepository repository();

    @Override
    default TestEntitySource<ConceptName, Concept> source() {
        return db.getNamed(ConceptTestEntitySource.class);
    }

    @Override
    default ConceptName notFoundName() {
        return TestLibraryIdentifiers.Concepts.NotFound.name;
    }

    @Override
    default List<ConceptName> knownEntityNames() {
        return List.of(
                TestLibraryIdentifiers.Concepts.Clade,
                TestLibraryIdentifiers.Concepts.CladeTaxonomyRelation
        );
    }

    @Override
    default Concept newEntity() {
        return new Concept(
                ConceptName.of(RandomValue.string()),
                RandomValue.string(),
                description());
    }

    @Override
    default Concept ghostEntity() {
        return new Concept(
                ConceptName.of(RandomValue.string()),
                RandomValue.string(),
                description());
    }

    @Override
    default Concept modifiedEntity(Concept original) {
        return new Concept(
                original.name(),
                RandomValue.string(),
                description());
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
```

- [ ] **Step 6: Create `ConceptEntityRepositoryMockTest` and `ConceptTestEntitySourceTest`**

`ConceptEntityRepositoryMockTest.java`:
```java
package com.naturalist.library;

class ConceptEntityRepositoryMockTest implements ConceptEntityRepositoryTest {
    @Override
    public ConceptRepository repository() {
        return new ConceptRepositoryMock(db);
    }
}
```

`ConceptTestEntitySourceTest.java`:
```java
package com.naturalist.library;

import com.naturalist.data.TestEntitySourceTest;

class ConceptTestEntitySourceTest
        extends TestEntitySourceTest<ConceptName, Concept, ConceptTestEntitySource> {
}
```

- [ ] **Step 7: Create `ConceptQueryImplTest`** (deferred from Task 2)

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConceptQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    ConceptRepository repository = new ConceptRepositoryMock(db);
    ConceptQuery conceptQuery = new ConceptQueryImpl(repository);

    @Test
    void getByNameReturnsTheConcept() {
        var result = conceptQuery.getByName(TestLibraryIdentifiers.Concepts.Clade);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("What is a clade?");
    }

    @Test
    void findByNameSetReturnsBothConcepts() {
        Set<ConceptName> names = Set.of(
                TestLibraryIdentifiers.Concepts.Clade,
                TestLibraryIdentifiers.Concepts.CladeTaxonomyRelation);

        ConceptCollection collection = conceptQuery.findByNameSet(names);

        assertThat(collection.size()).isEqualTo(2);
        assertThat(collection.stream().map(c -> c.name()))
                .containsExactlyInAnyOrderElementsOf(names);
    }
}
```

- [ ] **Step 8: Run the tests — expect PASS**

Run: `mvn -q -pl domains/library/library-repository-test,domains/library/library-core -am test`
Expected: `ConceptEntityRepositoryMockTest`, `ConceptTestEntitySourceTest`, `ConceptQueryImplTest` all green (the contract test exercises validation + not-found + happy path across the inherited methods).

- [ ] **Step 9: Commit**

```bash
git add domains/library/library-repository-test/ \
        domains/identifiers-test/src/main/java/com/naturalist/library/TestLibraryIdentifiers.java \
        domains/library/library-core/src/test/java/com/naturalist/library/ConceptQueryImplTest.java
git commit -m "test(library): Concept test data, mock, behavioral contract + query test

concepts.json holds the two meta-concepts (clade, clade-taxonomy-relation);
TestLibraryIdentifiers gains a Concepts scope.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: `library-test-context` module

**Files:**
- Create: `domains/library/library-test-context/pom.xml`
- Create: `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`
- Modify: `domains/library/pom.xml` (add module)
- Modify: `pom.xml` (root dep-management entry)

- [ ] **Step 1: Create the module pom**

`domains/library/library-test-context/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>library</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>library-test-context</artifactId>
    <name>domains :: Library :: Test Context</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-repository-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register the module in `domains/library/pom.xml`**

Change the `<modules>` block to add `library-test-context`:
```xml
    <modules>
        <module>library-api</module>
        <module>library-core</module>
        <module>library-repository-test</module>
        <module>library-test-context</module>
    </modules>
```

- [ ] **Step 3: Add the root dep-management entry**

In root `pom.xml`, immediately after the `library-repository-test` entry (the `</dependency>` at line 302), insert:
```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>library-test-context</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Create `LibraryTestContext`**

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;

/**
 * Pre-wired, in-memory read surface for the library bounded context. Mirrors
 * {@code ChemistryTestContext} — colocated in {@code com.naturalist.library}
 * so it can assemble the package-private {@link ConceptRepositoryMock} and the
 * package-private {@code ConceptQueryImpl} without promoting either to public.
 *
 * <p>Read seam only. Not a JUnit extension — consumers needing per-method reset
 * wrap a {@code NaturalistDatabaseExtension} alongside this context.
 */
public class LibraryTestContext {

    private final ConceptQuery conceptQuery;

    private LibraryTestContext(NaturalistDatabase db) {
        ConceptRepository repository = new ConceptRepositoryMock(db);
        this.conceptQuery = new ConceptQueryImpl(repository);
    }

    public static LibraryTestContext create(NaturalistDatabase db) {
        return new LibraryTestContext(db);
    }

    public ConceptQuery conceptQuery() {
        return conceptQuery;
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q -pl domains/library/library-test-context -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add domains/library/library-test-context/ domains/library/pom.xml pom.xml
git commit -m "feat(library): add library-test-context assembling ConceptQuery

Console read seam — mirrors chemistry-test-context. Wires the package-private
mock + query impl behind a public LibraryTestContext.create(db).conceptQuery().

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: `CladeCatalog` (kernel)

**Files:**
- Create: `kernels/clades/src/main/java/com/naturalist/clades/CladeCatalog.java`
- Test: `kernels/clades/src/test/java/com/naturalist/clades/CladeCatalogTest.java`

A direct kernel test is justified here (per the kernel testing convention: subtle reflection-based semantics not exercised by an existing consumer test).

- [ ] **Step 1: Write the failing test `CladeCatalogTest`**

```java
package com.naturalist.clades;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CladeCatalogTest {

    @Test
    void allCoversEveryPermit() {
        assertThat(CladeCatalog.all())
                .hasSize(Clade.class.getPermittedSubclasses().length);
    }

    @Test
    void onlyEukaryotaHasNoParent() {
        assertThat(CladeCatalog.all().stream().filter(c -> c.parent().isEmpty()).toList())
                .containsExactly(new Eukaryota());
    }

    @Test
    void childrenOfInsectaIncludesItsDirectDescendants() {
        assertThat(CladeCatalog.childrenOf(new Insecta()))
                .contains(new Holometabola(), new Hemiptera(), new Blattodea());
    }

    @Test
    void childrenOfALeafIsEmpty() {
        assertThat(CladeCatalog.childrenOf(new Troidini())).isEmpty();
    }

    @Test
    void childrenAreSortedByDisplayName() {
        var children = CladeCatalog.childrenOf(new Insecta());
        var sorted = children.stream().map(Clade::displayName).sorted().toList();
        assertThat(children.stream().map(Clade::displayName).toList()).isEqualTo(sorted);
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL (CladeCatalog does not exist)**

Run: `mvn -q -pl kernels/clades test`
Expected: compilation failure — `CladeCatalog` not found.

- [ ] **Step 3: Create `CladeCatalog`**

```java
package com.naturalist.clades;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Read-side enumeration of the sealed {@link Clade} permit set, for building a
 * browsable tree of life. {@link CladeTraversal} walks <em>up</em> the parent
 * chain; this catalog provides the whole permit set and the <em>down</em>
 * (children) direction the kernel otherwise lacks.
 *
 * <p>{@link #all()} reflects over {@code Clade.class.getPermittedSubclasses()}
 * — every permit is a stateless no-arg record — so the catalog tracks the
 * sealed permit list automatically with no second list to maintain. New permits
 * (including those added by other in-flight work) appear here at runtime with
 * no change to this file.
 */
public final class CladeCatalog {

    private CladeCatalog() {
    }

    /** Every clade permit, in no guaranteed order. */
    public static List<Clade> all() {
        return Arrays.stream(Clade.class.getPermittedSubclasses())
                .map(CladeCatalog::instantiate)
                .toList();
    }

    /** Direct descendants of {@code parent}, sorted by display name. */
    public static List<Clade> childrenOf(Clade parent) {
        return all().stream()
                .filter(c -> c.parent().map(p -> p.equals(parent)).orElse(false))
                .sorted(Comparator.comparing(Clade::displayName))
                .toList();
    }

    private static Clade instantiate(Class<?> permit) {
        try {
            return (Clade) permit.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate clade permit: " + permit, e);
        }
    }
}
```

- [ ] **Step 4: Run the test — expect PASS**

Run: `mvn -q -pl kernels/clades test`
Expected: all `CladeCatalogTest` cases green.

- [ ] **Step 5: Commit**

```bash
git add kernels/clades/src/main/java/com/naturalist/clades/CladeCatalog.java \
        kernels/clades/src/test/java/com/naturalist/clades/CladeCatalogTest.java
git commit -m "feat(clades): add CladeCatalog for tree enumeration + children

all() reflects over the sealed permit set; childrenOf() gives the down
direction CladeTraversal lacks. Auto-tracks new permits at runtime.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Wire `library` into `management-console`

**Files:**
- Modify: `apps/management-console/pom.xml`

- [ ] **Step 1: Add the library dependencies**

In `apps/management-console/pom.xml`, after the `<!-- DOMAIN CONSOLE MODULES -->` block (after the `plants-console` dependency, line 74), add a new block:

```xml
        <!-- DOMAIN MODULES (no console module) -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>library-test-context</artifactId>
        </dependency>
```

(`library-repository-test` and `framework-test`/`NaturalistDatabase` arrive transitively through `library-test-context`, the same way `chemistry-console` depends only on `chemistry-test-context`.)

- [ ] **Step 2: Verify the app still compiles**

Run: `mvn -q -pl apps/management-console -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add apps/management-console/pom.xml
git commit -m "build(console): depend on library api/core/test-context

Brings the Concept read surface onto the console classpath.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: `ConceptsController` + concept templates

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/concept/ConceptsController.java`
- Create: `apps/management-console/src/main/jte/concepts/list.jte`
- Create: `apps/management-console/src/main/jte/concepts/detail.jte`

- [ ] **Step 1: Create `ConceptsController`**

```java
package com.naturalist.console.concept;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.Concept;
import com.naturalist.library.ConceptName;
import com.naturalist.library.ConceptQuery;
import com.naturalist.library.LibraryTestContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Comparator;
import java.util.List;

@Controller
class ConceptsController {

    private final ConceptQuery conceptQuery;
    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    ConceptsController() {
        // TODO:: This will eventually be a spring managed bean
        this.conceptQuery = LibraryTestContext.create(NaturalistDatabase.create()).conceptQuery();
    }

    @GetMapping("/concepts")
    String list(Model model) {
        List<Concept> concepts = conceptQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE))
                .content().stream()
                .sorted(Comparator.comparing(Concept::title))
                .toList();
        model.addAttribute("concepts", concepts);
        return "concepts/list";
    }

    @GetMapping("/concepts/{slug}")
    String detail(@PathVariable String slug, Model model) {
        var concept = conceptQuery.getByName(ConceptName.of(slug));
        if (concept.isEmpty()) {
            return "redirect:/concepts";
        }
        Concept c = concept.get();
        var description = c.description();
        model.addAttribute("concept", c);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "concepts/detail";
    }
}
```

- [ ] **Step 2: Create `concepts/list.jte`**

```jte
@import com.naturalist.library.Concept
@import java.util.List
@param List<Concept> concepts

@template.layout.page(title = "Concepts", content = @`
    <h1>Concepts</h1>
    <p>Teaching notes on how the catalog of life is organised.</p>
    <ul class="concept-list">
        @for(var concept : concepts)
            <li><a href="/concepts/${concept.name().value()}">${concept.title()}</a></li>
        @endfor
    </ul>
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
```

- [ ] **Step 3: Create `concepts/detail.jte`**

```jte
@import com.naturalist.library.Concept

@param Concept concept
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = concept.title(), content = @`
    <nav class="breadcrumb" aria-label="Breadcrumb"><a href="/concepts">Concepts</a></nav>
    <h1>${concept.title()}</h1>
    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)
    <p><a href="/clades">Browse the tree of life →</a></p>
`)
```

- [ ] **Step 4: Verify it compiles**

Run: `mvn -q -pl apps/management-console -am compile`
Expected: BUILD SUCCESS (JTE templates are compiled by the build).

- [ ] **Step 5: Commit**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/concept/ \
        apps/management-console/src/main/jte/concepts/
git commit -m "feat(console): concept pages (/concepts, /concepts/{slug})

Lists and renders the two meta-concepts via the four-level Durrell component,
backed only by ConceptQuery.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: `CladesController` + clade-tree templates

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/clade/CladesController.java`
- Create: `apps/management-console/src/main/jte/clades/tree.jte`
- Create: `apps/management-console/src/main/jte/clades/node.jte`
- Create: `apps/management-console/src/main/jte/clades/detail.jte`

`CladesController` is `public` because its nested view records are referenced from templates (same pattern as `SearchController`).

- [ ] **Step 1: Create `CladesController`**

```java
package com.naturalist.console.clade;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class CladesController {

    private final DescriptionRenderer descriptionRenderer = new DescriptionRenderer(List.of());

    @GetMapping("/clades")
    String tree(Model model) {
        model.addAttribute("root", toNode(new Eukaryota()));
        return "clades/tree";
    }

    @GetMapping("/clades/{slug}")
    String detail(@PathVariable String slug, Model model) {
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException e) {
            return "redirect:/clades";
        }
        var description = clade.description();
        model.addAttribute("displayName", clade.displayName());
        model.addAttribute("slug", clade.slug());
        model.addAttribute("ancestry", ancestryLinks(clade));
        model.addAttribute("children", childLinks(clade));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "clades/detail";
    }

    private CladeNode toNode(Clade clade) {
        List<CladeNode> children = CladeCatalog.childrenOf(clade).stream()
                .map(this::toNode)
                .toList();
        return new CladeNode(clade.slug(), clade.displayName(), children);
    }

    /** Ancestors from root down to the clade's parent (the clade itself excluded). */
    private List<CladeLink> ancestryLinks(Clade clade) {
        List<Clade> chain = new ArrayList<>(CladeTraversal.ancestry(clade));
        chain.removeFirst();              // drop the clade itself
        Collections.reverse(chain);       // root → parent order
        return chain.stream().map(c -> new CladeLink(c.slug(), c.displayName())).toList();
    }

    private List<CladeLink> childLinks(Clade clade) {
        return CladeCatalog.childrenOf(clade).stream()
                .map(c -> new CladeLink(c.slug(), c.displayName()))
                .toList();
    }

    public record CladeNode(String slug, String displayName, List<CladeNode> children) {
    }

    public record CladeLink(String slug, String displayName) {
    }
}
```

- [ ] **Step 2: Create `clades/node.jte`** (recursive)

```jte
@import com.naturalist.console.clade.CladesController.CladeNode
@param CladeNode node

<li>
    <a href="/clades/${node.slug()}">${node.displayName()}</a>
    @if(!node.children().isEmpty())
        <ul>
            @for(var child : node.children())
                @template.clades.node(node = child)
            @endfor
        </ul>
    @endif
</li>
```

- [ ] **Step 3: Create `clades/tree.jte`**

```jte
@import com.naturalist.console.clade.CladesController.CladeNode
@param CladeNode root

@template.layout.page(title = "Tree of Life", content = @`
    <h1>Tree of Life</h1>
    <p>Every clade in the catalog, from Eukaryota down.
       <a href="/concepts/clade">What is a clade?</a></p>
    <ul class="clade-tree">
        @template.clades.node(node = root)
    </ul>
`)
```

- [ ] **Step 4: Create `clades/detail.jte`**

```jte
@import com.naturalist.console.clade.CladesController.CladeLink
@import java.util.List

@param String displayName
@param String slug
@param List<CladeLink> ancestry = List.of()
@param List<CladeLink> children = List.of()
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = displayName, content = @`
    <nav class="breadcrumb" aria-label="Clade ancestry">
        <a href="/clades">Tree of Life</a>
        @for(var ancestor : ancestry)
            &rsaquo; <a href="/clades/${ancestor.slug()}">${ancestor.displayName()}</a>
        @endfor
        &rsaquo; <span aria-current="page">${displayName}</span>
    </nav>

    <h1>${displayName}</h1>

    <p class="clade-concept-links">
        <a href="/concepts/clade">What is a clade?</a> ·
        <a href="/concepts/clade-taxonomy-relation">Clades vs. taxonomy</a>
    </p>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    @if(!children.isEmpty())
        <section>
            <h2>Direct descendants</h2>
            <ul class="clade-children">
                @for(var child : children)
                    <li><a href="/clades/${child.slug()}">${child.displayName()}</a></li>
                @endfor
            </ul>
        </section>
    @endif
`)
```

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q -pl apps/management-console -am compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/clade/ \
        apps/management-console/src/main/jte/clades/
git commit -m "feat(console): browsable tree of life (/clades, /clades/{slug})

Renders the clade tree from CladeCatalog; each clade page shows its own
four-level description, ancestry, children, and links to the concept pages.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Cross-linking — insect breadcrumbs + "More" nav

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java:146-160`
- Modify: `apps/management-console/src/main/jte/layout/page.jte:38-42`

- [ ] **Step 1: Link every breadcrumb clade to its clade page**

In `InsectsController.java`, the `CLADE_URL` map (lines 146–147) currently only maps `insecta`. Keep that override but make `cladePrefix()` fall back to `/clades/{slug}` for all other clades. Replace the `cladePrefix()` body (lines 149–160) with:

```java
    private List<BreadcrumbSegment> cladePrefix() {
        return CladeTraversal.ancestry(new Insecta()).reversed().stream()
                .filter(c -> !(c instanceof Eukaryota))
                .map(c -> {
                    String rank = CLADE_RANK_LABEL.get(c.slug());
                    String url = CLADE_URL.getOrDefault(c.slug(), "/clades/" + c.slug());
                    return BreadcrumbSegment.link(c.displayName(), url, rank);
                })
                .toList();
    }
```

(Every ancestry clade now links: `Insecta` to its existing `/insects/orders` landing, all others to `/clades/{slug}`. The `BreadcrumbSegment.text(...)` branch is no longer needed here.)

Also update the `CLADE_URL` javadoc (lines 140–145) to note the fallback:
```java
    /**
     * URL overrides for clades that own a landing page elsewhere in this
     * console. Insecta points at the catalog root (the orders listing) so its
     * breadcrumb segment is a click-back-to-the-top affordance. Every other
     * clade falls back to its tree-of-life page at {@code /clades/{slug}}.
     */
```

- [ ] **Step 2: Add "Tree of Life" + "Concepts" to the "More" overflow**

In `apps/management-console/src/main/jte/layout/page.jte`, replace the empty overflow comment (lines 39–41) inside `<ul class="primary-nav-overflow">` with:

```jte
                            <li><a href="/clades">Tree of Life</a></li>
                            <li><a href="/concepts">Concepts</a></li>
```

- [ ] **Step 3: Verify the full build**

Run: `mvn verify` (from repo root)
Expected: BUILD SUCCESS — all module tests pass, including the new `ConceptTest`, `ConceptEntityRepositoryMockTest`, `ConceptTestEntitySourceTest`, `ConceptQueryImplTest`, and `CladeCatalogTest`. The management-console JTE templates compile.

- [ ] **Step 4: Manual smoke (optional but recommended)**

Run the console (per the project's run procedure) and check:
- `/clades` renders the nested tree from Eukaryota; clade links work.
- `/clades/blattodea` shows Blattodea's description, ancestry breadcrumb, children (e.g. Termitoidae), and the two concept links.
- `/concepts` lists both concepts; `/concepts/clade` and `/concepts/clade-taxonomy-relation` render the four-level tabs.
- On any insect species page, the breadcrumb clade names (e.g. Arthropoda) link to `/clades/{slug}`.
- The top-nav "More" menu shows Tree of Life + Concepts.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
        apps/management-console/src/main/jte/layout/page.jte
git commit -m "feat(console): cross-link clades — insect breadcrumbs + More nav

Breadcrumb clade names link to /clades/{slug}; the More menu surfaces Tree of
Life and Concepts.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review (against the design spec)

**Spec coverage:**
- A. Concept sub-context in `library` (api/core/repository-test/identifiers/test-context + concepts.json) → Tasks 1–4. ✓
- B. `CladeCatalog` (`all()` + `childrenOf()`, reflection-based) → Task 5. ✓
- C. App-level `ConceptsController` + `CladesController` + templates → Tasks 6–8. ✓
- D. Cross-linking (`CLADE_URL` fallback) + "More" nav → Task 9. ✓
- Non-goals (trait display on clade pages; concept search) → not implemented, as specified. ✓
- Coordination: only new file in `kernels/clades` is `CladeCatalog.java`; no edits to `Clade.java`/`CladeTest.java`/permits/`InsectClades`/insect catalog. ✓

**Type consistency:** `ConceptName.of` / `Concept(name,title,description)` / `ConceptQuery.conceptQuery()` / `CladeCatalog.all()`,`childrenOf()` / `CladeNode(slug,displayName,children)` / `CladeLink(slug,displayName)` used consistently across tasks. Description model attributes (`descriptionPreschool…University`) match the `components.description` params (`preschool/elementary/secondary/university`).

**Placeholder scan:** No TBD/“similar to”; all code blocks are concrete. `concepts.json` prose is the verbatim content from the source note.

**Assumptions to verify during execution (low risk):**
- `EntityRepositoryTest` exposes a `db` field and the inherited contract methods (`getByName`/`getByEntityNameSet`/`getPage`) — confirmed against `CitationEntityRepositoryTest`/`CitationQueryImpl`.
- `mo.violationNamesRemovingPrefix(...)` is the exact method name on `InvariantObservation` — confirmed against `InsectOrderTest`. If the local method is named `violationNamesRemovingPrefix` vs `invalidInvariantNamesRemovingPrefix`, match `InsectOrderTest` verbatim.
- JTE import of a nested record (`CladesController.CladeNode`) requires the enclosing controller to be `public` — handled (CladesController is public, mirroring SearchController).
```
