# Present the Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface the identification evidence the app already collects — diagnostic field marks, authority citations, per-photo confidence and alternatives — on the family, genus, and order pages where rank-polymorphic identifications actually land.

**Architecture:** Three independent surfaces, each already backed by working domain code. (1) A `TestEntitySource` serialization seam lets `CitationAssociation` — whose `EntityRef` component Jackson cannot round-trip — participate in origin-tracked flush, so console-written citation links persist and the existing `citations.jte` renders. (2) A console-side `FeatureGroup` value type reshapes `InsectFeatureView` into rank-grouped display rows for a new `features.jte` component, wired into all four rank handlers. (3) The photo gallery + identification-evidence block is extracted out of `detail.jte` into a shared `observationGallery.jte` and added to family/genus/order, which currently render only child-rank thumbnails.

**Tech Stack:** Java 21 records and sealed interfaces, JTE templates, Jackson 2.19, Spring MVC (`insects-console`), JUnit 5 + AssertJ, existing `naturalist.css`.

## Global Constraints

- No `mvn` invocations — the user runs builds locally. Where a step says "run tests", report the command and hand off; do not execute Maven.
- Plans go in `docs/plans/`, not `docs/superpowers/plans/`.
- Print git commands; commit only on explicit request from the user.
- `insects` is the reference domain for patterns.
- **No test infra for test infra** — do not add unit tests to `kernels/framework-test`. Task 1's framework change is verified through its library-module consumer.
- Record arity changes ripple repo-wide — grep the whole repo when changing a record's constructor.
- `EntityId` uses UUIDv7 only; `UUID.randomUUID()` is forbidden in domain and adapter code.
- Domain-specific repository methods need `observer().arguments(...).throwWhenInvalid()`; this plan adds none.
- Python catalog rewrites need `json.dump(..., ensure_ascii=False)`. This plan does not rewrite catalogs by script.
- New console helper types follow the existing `AncestorIntro` / `BreadcrumbSegment` / `CladeTrail` pattern: a record in `com.naturalist.insects.console` with a static factory and its own unit test.

## Background — what already works and what does not

Verified against the working tree on 2026-08-08:

| Capability | State |
|---|---|
| `InsectFeature` + `InsectFeatureAssignment` persistence | Working — 29 features, 33 assignments in the catalog |
| `InsectQuery.features().findByRankName(...)` → `InsectFeatureView` | Working, lineage-composited, ancestor-first ordering |
| Feature rendering in `insects-console` | **Absent** — zero references to `InsectFeatureView` |
| `Citation` writes from identification | Working — `ref-chrysomelidae`, `ref-cleridae`, `ref-carabidae`, `ref-orsodacne` are in `citations.json` |
| `CitationAssociation` writes | **Silently dropped on flush** — `CitationAssociationTestEntitySource` never sets `defaultInsertFile` |
| `citations.jte` on family/genus/order | Already wired — renders as soon as associations persist |
| Photos attached at a family/genus/order rank | Persisted (`parentName: chrysomelidae`) but **not rendered** on that rank's detail page |
| Confidence / evidence / alternatives | Persisted on `FieldObservation.identification()`, rendered **only** on `detail.jte` (species) |
| Gallery + evidence CSS (`.image-gallery`, `.obs-identification`, `.id-evidence`) | Already in `naturalist.css` — reuse, do not restyle |

## File Structure

**Task 1 — citation associations persist**

| File | Responsibility |
|---|---|
| `kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java` (modify) | Adds a parser-supplying `loadFile` overload and a `writable`/`writableClass` serialization seam |
| `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationJson.java` (create) | The on-disk DTO shape, both directions, extracted from the source class |
| `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java` (modify) | Uses the new seams; loses its private DTO and hand-rolled loader |
| `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationJsonTest.java` (create) | Round-trip test for the DTO mapping |

**Task 2 — field marks on rank pages**

| File | Responsibility |
|---|---|
| `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/FeatureGroup.java` (create) | Reshapes `InsectFeatureView` into rank-labelled display groups |
| `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/FeatureGroupTest.java` (create) | Unit test for grouping, ordering, and the null/empty view |
| `domains/insects/insects-console/src/main/jte/insects/features.jte` (create) | Renders the groups |
| `insects/detail.jte`, `family.jte`, `genus.jte`, `order.jte` (modify) | Add the `featureGroups` param and the component call |
| `InsectsController.java` (modify) | Populates `featureGroups` in four handlers |
| `apps/management-console/src/main/resources/static/css/naturalist.css` (modify) | `.field-marks` styling |

**Task 3 — rank observation gallery + evidence**

| File | Responsibility |
|---|---|
| `domains/insects/insects-console/src/main/jte/insects/observationGallery.jte` (create) | The photo + confidence + evidence + alternatives + notes-form block, extracted from `detail.jte` |
| `insects/detail.jte` (modify) | Delegates to the component |
| `insects/family.jte`, `genus.jte`, `order.jte` (modify) | Gain the component |
| `InsectsController.java` (modify) | `observationLookup` helper extracted from `detail`; three rank handlers take `HttpServletRequest` and populate `images`/`observations`; `updateNotes` honours `returnPath` |
| `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java` (modify) | Covers the new family params |

**Task 4 — docs**

| File | Responsibility |
|---|---|
| `docs/work-tracker.md` (modify) | New completed row; refreshed current-slice section |
| `domains/insects/CLAUDE.md` (modify) | Documents the console evidence surfaces |

---

### Task 1: Citation Associations Survive the Flush

The one true bug. `CitationAssociationTestEntitySource` loads its catalog through a private `loadFromDto(...)` rather than `loadFile(...)`, so `defaultInsertFile` is never set. Every association the identification flow inserts gets a `null` origin and is filtered out of `flushIfWritable()`. The DTO exists for a real reason — `CitationAssociation.subject` is an `EntityRef(DomainId, EntityName)`, and Jackson can round-trip neither an open interface nor an abstract `EntityName` — so the fix is a serialization seam, not a `loadFile` swap.

**Files:**
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java:204-214` (`loadFile`) and `:251-257` (`writeJsonAtomic`)
- Create: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationJson.java`
- Modify: `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java`
- Test: `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationJsonTest.java`

**Interfaces:**
- Consumes: `TestEntitySource<CitationAssociationId, CitationAssociation>`, `CitationAssociation`, `EntityRef`, `InsectsDomain`, `InsectRankName.of(String, LinealRank)`
- Produces:
  - `protected void TestEntitySource.loadFile(String relativePath, Function<String, List<ENTITY>> parser)`
  - `protected Object TestEntitySource.writable(ENTITY entity)` — defaults to the entity
  - `protected Class<?> TestEntitySource.writableClass()` — defaults to `entityClass()`
  - `static CitationAssociationJson CitationAssociationJson.fromEntity(CitationAssociation)`
  - `CitationAssociation CitationAssociationJson.toEntity()`
  - `static List<CitationAssociation> CitationAssociationJson.parseAll(String json)`

- [ ] **Step 1: Write the failing round-trip test**

Create `domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationJsonTest.java`:

```java
package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectsDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAssociationJsonTest {

    @Test
    void fromEntityFlattensTheSubjectRef() {
        var association = new CitationAssociation(
                CitationAssociationId.of(UUID.fromString("019f0001-a001-7001-8001-a00000000009")),
                CitationName.of("ref-chrysomelidae"),
                new EntityRef(new InsectsDomain(), InsectFamilyName.of("chrysomelidae")),
                "Identified via vision");

        var json = CitationAssociationJson.fromEntity(association);

        assertThat(json.citationName()).isEqualTo("ref-chrysomelidae");
        assertThat(json.subjectDomain()).isEqualTo("insects");
        assertThat(json.subjectRank()).isEqualTo("FAMILY");
        assertThat(json.subjectName()).isEqualTo("chrysomelidae");
        assertThat(json.note()).isEqualTo("Identified via vision");
    }

    @Test
    void toEntityRebuildsTheTypedSubjectRef() {
        var original = new CitationAssociation(
                CitationAssociationId.of(UUID.fromString("019f0001-a001-7001-8001-a00000000009")),
                CitationName.of("ref-chrysomelidae"),
                new EntityRef(new InsectsDomain(), InsectFamilyName.of("chrysomelidae")),
                "Identified via vision");

        var roundTripped = CitationAssociationJson.fromEntity(original).toEntity();

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void parseAllReadsTheCatalogFileShape() {
        var json = """
                [ {
                  "id" : "019f0001-a001-7001-8001-a00000000001",
                  "citationName" : "eol-lepidoptera-747",
                  "subjectDomain" : "insects",
                  "subjectRank" : "ORDER",
                  "subjectName" : "lepidoptera",
                  "note" : "EOL order page"
                } ]
                """;

        var entities = CitationAssociationJson.parseAll(json);

        assertThat(entities).hasSize(1);
        assertThat(entities.getFirst().citationName().value()).isEqualTo("eol-lepidoptera-747");
        assertThat(entities.getFirst().subject().name().value()).isEqualTo("lepidoptera");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl domains/library/library-repository-test -Dtest=CitationAssociationJsonTest`
Expected: FAIL to compile — `CitationAssociationJson` does not exist.

- [ ] **Step 3: Create `CitationAssociationJson`**

Create `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationJson.java`:

```java
package com.naturalist.library;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectRankName;
import com.naturalist.insects.InsectsDomain;
import com.naturalist.taxonomy.LinealRank;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * On-disk shape of {@link CitationAssociation} in
 * {@code library/citation-associations.json}.
 *
 * <p>The entity's {@link EntityRef} subject pairs a {@link DomainId} — an open
 * interface — with an abstract {@link EntityName}. Jackson can round-trip
 * neither on its own, which is why this catalog needs a hand-written DTO where
 * every other catalog serializes its entity directly. The DTO flattens the pair
 * into {@code subjectDomain} / {@code subjectRank} / {@code subjectName} and
 * reconstructs the typed values on read.
 *
 * <p>Both directions are declared here so
 * {@link CitationAssociationTestEntitySource} can register this shape as its
 * flush form via {@code TestEntitySource.writable(...)} — without that, console-
 * driven inserts round-trip to a shape the loader cannot read back.
 */
record CitationAssociationJson(
        String id,
        String citationName,
        String subjectDomain,
        String subjectRank,
        String subjectName,
        @Nullable String note
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final InsectsDomain INSECTS = new InsectsDomain();

    static List<CitationAssociation> parseAll(String json) {
        try {
            List<CitationAssociationJson> dtos =
                    MAPPER.readValue(json, new TypeReference<>() {});
            List<CitationAssociation> entities = new ArrayList<>(dtos.size());
            for (CitationAssociationJson dto : dtos) {
                entities.add(dto.toEntity());
            }
            return entities;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse citation associations", e);
        }
    }

    static CitationAssociationJson fromEntity(CitationAssociation association) {
        EntityRef subject = association.subject();
        EntityName name = subject.name();
        return new CitationAssociationJson(
                association.id().value().toString(),
                association.citationName().value(),
                subject.domain().value(),
                rankOf(name),
                name.value(),
                association.note());
    }

    CitationAssociation toEntity() {
        return new CitationAssociation(
                CitationAssociationId.of(UUID.fromString(id)),
                CitationName.of(citationName),
                new EntityRef(resolveDomain(subjectDomain),
                        resolveEntityName(subjectDomain, subjectRank, subjectName)),
                note);
    }

    private static String rankOf(EntityName name) {
        if (name instanceof InsectRankName rankName) {
            return rankName.rank().name();
        }
        throw new IllegalArgumentException(
                "Unsupported citation subject name type: " + name.getClass().getName());
    }

    private static DomainId resolveDomain(String domain) {
        return switch (domain) {
            case "insects" -> INSECTS;
            default -> throw new IllegalArgumentException("Unknown domain: " + domain);
        };
    }

    private static EntityName resolveEntityName(String domain, String rank, String slug) {
        if ("insects".equals(domain)) {
            return InsectRankName.of(slug, LinealRank.valueOf(rank));
        }
        throw new IllegalArgumentException("Unknown domain: " + domain);
    }
}
```

Note: `InsectRankName.of(slug, LinealRank)` replaces the old five-arm `switch`. It throws `IllegalArgumentException` for ranks above ORDER, which is the same rejection the switch's `default` gave.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl domains/library/library-repository-test -Dtest=CitationAssociationJsonTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Add the parser-supplying `loadFile` overload to `TestEntitySource`**

In `kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java`, add to the import block:

```java
import java.util.function.Function;
```

(`java.util.*` is already imported, but `Function` lives in `java.util.function`.)

Replace the existing `loadFile(String)` method at lines 204-214:

```java
    public void loadFile(String relativePath) {
        loadFile(relativePath,
                json -> TestDataHelper.readObjectsFromString(() -> json, entityClass()));
    }

    /**
     * Loads a catalog file whose on-disk shape is not the entity's own Jackson
     * shape — the parser turns the file's text into entities. Origin tracking and
     * {@code defaultInsertFile} registration behave exactly as for
     * {@link #loadFile(String)}, so entities inserted later (e.g. by a console
     * write) flush back to this file rather than being silently dropped.
     *
     * <p>A source using this overload MUST also override {@link #writable} and
     * {@link #writableClass()} so the flush writes the same shape the parser
     * reads. Overriding one without the other produces a file the loader cannot
     * read back on the next boot.
     */
    protected void loadFile(String relativePath, Function<String, List<ENTITY>> parser) {
        if (defaultInsertFile == null) {
            defaultInsertFile = relativePath;
        }
        String json = TestDataHelper.readFileToString(relativePath);
        for (ENTITY entity : parser.apply(json)) {
            insertCommon(entity);
            originFile.put(entity.key(), relativePath);
        }
    }
```

- [ ] **Step 6: Add the `writable` serialization seam**

Still in `TestEntitySource.java`, add these two methods directly above `flushIfWritable()` (before the comment block at line 216):

```java
    /**
     * The on-disk form of a single entity. Defaults to the entity itself.
     * Override together with {@link #writableClass()} when the catalog file's
     * shape differs from the entity's Jackson shape — for instance when a
     * component is an open interface or abstract type Jackson cannot
     * round-trip.
     */
    protected Object writable(ENTITY entity) {
        return entity;
    }

    /** The declared type {@link #writable} returns. */
    protected Class<?> writableClass() {
        return entityClass();
    }
```

- [ ] **Step 7: Route the flush through the seam**

Replace the first three statements of `writeJsonAtomic` (lines 252-257) so it serializes the writable form. Keep the `writerFor(listType)` construction — it forces serialization by the *declared* type, which is what makes `@JsonTypeInfo` discriminators like `Citation`'s `"kind": "ONLINE_SOURCE"` get written. Serializing the runtime type would drop the discriminator and break the next load.

```java
    private void writeJsonAtomic(Path target, List<ENTITY> entities) {
        try {
            var listType = TestDataHelper.mapper.getTypeFactory()
                    .constructCollectionType(List.class, writableClass());
            List<Object> writables = entities.stream()
                    .map(this::writable)
                    .collect(Collectors.toList());
            byte[] bytes = TestDataHelper.mapper.writerFor(listType)
                    .withDefaultPrettyPrinter()
                    .writeValueAsBytes(writables);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, bytes);
```

Leave the remainder of the method (atomic move, catch block) untouched.

- [ ] **Step 8: Rewire `CitationAssociationTestEntitySource`**

Replace the whole of `domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java`:

```java
package com.naturalist.library;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class CitationAssociationTestEntitySource
        extends TestEntitySource<CitationAssociationId, CitationAssociation> {

    public CitationAssociationTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("library/citation-associations.json", CitationAssociationJson::parseAll);
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

    @Override
    protected Object writable(CitationAssociation entity) {
        return CitationAssociationJson.fromEntity(entity);
    }

    @Override
    protected Class<?> writableClass() {
        return CitationAssociationJson.class;
    }
}
```

The `Function` import is retained for `valueFunction()`. Every other import from the old file is gone with the DTO.

- [ ] **Step 9: Run the library module's tests**

Run: `mvn test -pl domains/library/library-repository-test`
Expected: PASS — `CitationAssociationJsonTest` (3 new), `CitationAssociationTestEntitySourceTest`, `CitationAssociationRepositoryMockTest`, and the concept/citation suites all green. The existing source test exercises the new `loadFile` overload transitively.

- [ ] **Step 10: Run the full build**

Run: `mvn verify`
Expected: PASS. `TestEntitySource` is a framework-test change — every `*-repository-test` module recompiles against it. Failures concentrate in console / Spring Boot / JTE modules; check those first if anything breaks.

- [ ] **Step 11: Smoke-test the real fix**

Start the management console, sign in, and identify any insect photo via `/insects/identify`. Then check:

```bash
git -C /Users/pat/dev/naturalist diff --stat domains/library/library-repository-test/src/main/resources/library/citation-associations.json
```

Expected: the file gained one or more rows, in the flattened `subjectDomain`/`subjectRank`/`subjectName` shape, and the identified rank's page now shows a **Citations** section. Before this task the file was frozen at its 4 seed rows.

- [ ] **Step 12: Commit**

```bash
git add kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java \
       domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationJson.java \
       domains/library/library-repository-test/src/main/java/com/naturalist/library/CitationAssociationTestEntitySource.java \
       domains/library/library-repository-test/src/test/java/com/naturalist/library/CitationAssociationJsonTest.java \
       domains/library/library-repository-test/src/main/resources/library/citation-associations.json
git commit -m "fix(library): persist citation associations written by identification

CitationAssociationTestEntitySource loaded its catalog through a private
loadFromDto helper rather than loadFile, so defaultInsertFile was never set
and every console-written association got a null origin and was filtered out
of the flush. Adds a parser-supplying loadFile overload plus a
writable/writableClass serialization seam to TestEntitySource, and moves the
DTO into CitationAssociationJson with both directions declared.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: Field Marks on Every Rank Page

`InsectFeatureView` composites a rank's own field marks with those inherited from its ancestors, ancestor-first and ordinal-ordered within each rank. Nothing in `insects-console` reads it. This task adds the display type, the component, and the four handler wirings.

The grouping happens in a tested console record rather than inside the template — `family.jte` and friends use `!{var ...}` for one-line derivations, but a `computeIfAbsent` accumulation loop belongs in Java, and `AncestorIntro` / `BreadcrumbSegment` / `CladeTrail` establish the pattern for console-side display types.

**Files:**
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/FeatureGroup.java`
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/FeatureGroupTest.java`
- Create: `domains/insects/insects-console/src/main/jte/insects/features.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`, `family.jte`, `genus.jte`, `order.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — `detail`, `familyDetail`, `genusDetail`, `orderDetail`
- Modify: `apps/management-console/src/main/resources/static/css/naturalist.css`

**Interfaces:**
- Consumes: `InsectFeatureView`, `InsectFeatureView.RankedFeature`, `InsectFeature`, `InsectRankName`, `LinealRank`, `InsectQuery.FeatureQuery.findByRankName(InsectRankName)` (returns `Optional<InsectFeatureView>`)
- Produces: `record FeatureGroup(String rankLabel, String rankSlug, List<String> marks)` with `static List<FeatureGroup> of(@Nullable InsectFeatureView view)` — consumed by `features.jte` and the four handlers

- [ ] **Step 1: Write the failing test**

Create `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/FeatureGroupTest.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectFeature;
import com.naturalist.insects.InsectFeatureId;
import com.naturalist.insects.InsectFeatureView;
import com.naturalist.insects.InsectOrderName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureGroupTest {

    private static InsectFeatureView.RankedFeature mark(
            String value, com.naturalist.insects.InsectRankName at, int ordinal) {
        return new InsectFeatureView.RankedFeature(
                InsectFeature.of(InsectFeatureId.create(), value), at, ordinal);
    }

    @Test
    void ofGroupsByAssignedRankPreservingViewOrder() {
        var order = InsectOrderName.of("coleoptera");
        var family = InsectFamilyName.of("chrysomelidae");
        var view = new InsectFeatureView(family, List.of(
                mark("hardened forewings", order, 0),
                mark("chewing mouthparts", order, 1),
                mark("strongly domed body", family, 0)));

        var groups = FeatureGroup.of(view);

        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().rankLabel()).isEqualTo("Order");
        assertThat(groups.getFirst().rankSlug()).isEqualTo("coleoptera");
        assertThat(groups.getFirst().marks())
                .containsExactly("hardened forewings", "chewing mouthparts");
        assertThat(groups.getLast().rankLabel()).isEqualTo("Family");
        assertThat(groups.getLast().marks()).containsExactly("strongly domed body");
    }

    @Test
    void ofSortsWithinARankByOrdinal() {
        var family = InsectFamilyName.of("chrysomelidae");
        var view = new InsectFeatureView(family, List.of(
                mark("second", family, 1),
                mark("third", family, 2),
                mark("first", family, 0)));

        var groups = FeatureGroup.of(view);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().marks()).containsExactly("first", "second", "third");
    }

    @Test
    void ofReturnsEmptyForNullView() {
        assertThat(FeatureGroup.of(null)).isEmpty();
    }

    @Test
    void ofReturnsEmptyForViewWithNoFeatures() {
        var view = new InsectFeatureView(InsectFamilyName.of("chrysomelidae"), List.of());

        assertThat(FeatureGroup.of(view)).isEmpty();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl domains/insects/insects-console -Dtest=FeatureGroupTest`
Expected: FAIL to compile — `FeatureGroup` does not exist.

- [ ] **Step 3: Create `FeatureGroup`**

Create `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/FeatureGroup.java`:

```java
package com.naturalist.insects.console;

import com.naturalist.insects.InsectFeatureView;
import com.naturalist.insects.InsectRankName;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Display grouping of an {@link InsectFeatureView} — the lineage-composite
 * field marks split into one group per contributing rank, so the page can
 * render "Order-level marks: … / Family-level marks: …".
 *
 * <p>Group order follows the view's ancestor-first contract; within a group,
 * marks are ordered conspicuous-to-diagnostic by
 * {@link InsectFeatureView.RankedFeature#ordinal()}.
 *
 * @param rankLabel display label for the contributing rank ("Order", "Family")
 * @param rankSlug  the contributing rank's slug, for a link back to its page
 * @param marks     the feature values, conspicuous to diagnostic
 */
public record FeatureGroup(String rankLabel, String rankSlug, List<String> marks) {

    /**
     * Groups a feature view for display. Returns an empty list for a null or
     * feature-less view so the template can test one condition.
     */
    public static List<FeatureGroup> of(@Nullable InsectFeatureView view) {
        if (view == null || view.features().isEmpty()) {
            return List.of();
        }
        Map<InsectRankName, List<InsectFeatureView.RankedFeature>> byRank =
                new LinkedHashMap<>();
        for (var ranked : view.features()) {
            byRank.computeIfAbsent(ranked.assignedAt(), k -> new ArrayList<>()).add(ranked);
        }
        List<FeatureGroup> groups = new ArrayList<>(byRank.size());
        byRank.forEach((rankName, ranked) -> {
            List<String> marks = ranked.stream()
                    .sorted(Comparator.comparingInt(InsectFeatureView.RankedFeature::ordinal))
                    .map(r -> r.feature().value())
                    .toList();
            groups.add(new FeatureGroup(label(rankName), rankName.value(), marks));
        });
        return List.copyOf(groups);
    }

    private static String label(InsectRankName rankName) {
        String name = rankName.rank().name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn test -pl domains/insects/insects-console -Dtest=FeatureGroupTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Create the `features.jte` component**

Create `domains/insects/insects-console/src/main/jte/insects/features.jte`:

```
@import com.naturalist.insects.console.FeatureGroup
@import java.util.List

@param List<FeatureGroup> featureGroups = List.of()

@if(!featureGroups.isEmpty())
    <section class="field-marks">
        <h2>Field Marks</h2>
        <p class="field-marks-lead">What the identification was based on, from
            most conspicuous to most diagnostic. Check these against your photo.</p>
        @for(var group : featureGroups)
            <div class="field-mark-group">
                <h3>${group.rankLabel()} — ${group.rankSlug()}</h3>
                <ul>
                    @for(var mark : group.marks())
                        <li>${mark}</li>
                    @endfor
                </ul>
            </div>
        @endfor
    </section>
@endif
```

- [ ] **Step 6: Add the component to the four rank templates**

In each of `detail.jte`, `family.jte`, `genus.jte`, and `order.jte`, add the import alongside the existing `com.naturalist.insects.console.*` imports:

```
@import com.naturalist.insects.console.FeatureGroup
```

Add the param at the end of each template's `@param` block:

```
@param List<FeatureGroup> featureGroups = List.of()
```

Then insert the component call **immediately after** the existing `@template.insects.citations(citations = citations)` line in `family.jte`, `genus.jte`, and `order.jte`:

```
    @template.insects.features(featureGroups = featureGroups)
```

In `detail.jte`, the citations call is the last statement in the body (line 255). Insert the same line immediately after it.

All four templates already `@import java.util.List`, so no additional import is needed for the param type.

- [ ] **Step 7: Wire the four controller handlers**

In `InsectsController.java`, add to the imports:

```java
import com.naturalist.insects.InsectFeatureView;
```

Add this private helper next to the other model-building helpers (near `introsForFamily`):

```java
    private List<FeatureGroup> featureGroups(InsectRankName rankName) {
        return FeatureGroup.of(insectQuery.features().findByRankName(rankName).orElse(null));
    }
```

Then add one line to each handler, immediately after its existing `model.addAttribute("citations", ...)` call:

- `familyDetail` (after line 435): `model.addAttribute("featureGroups", featureGroups(familyName));`
- `orderDetail` (after line 485): `model.addAttribute("featureGroups", featureGroups(orderName));`
- `genusDetail` (after line 547): `model.addAttribute("featureGroups", featureGroups(genusName));`
- `detail` (after `model.addAttribute("citations", i.citations());`): `model.addAttribute("featureGroups", featureGroups(speciesName));`

Each handler already declares its typed name as a local at the top of the method — `var familyName = InsectFamilyName.of(name);`, `var orderName = InsectOrderName.of(name);`, `var genusName = InsectGenusName.of(name);`. Reference those; do not re-derive.

- [ ] **Step 8: Add the stylesheet block**

Append to `apps/management-console/src/main/resources/static/css/naturalist.css`:

```css
/* --- Field marks -------------------------------------------------------- */

.field-marks-lead {
    font-style: italic;
    opacity: 0.75;
    margin-bottom: 1rem;
}

.field-mark-group {
    margin-bottom: 1rem;
}

.field-mark-group h3 {
    font-size: 0.9rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    opacity: 0.7;
    margin-bottom: 0.35rem;
}

.field-mark-group ul {
    margin: 0;
    padding-left: 1.25rem;
}

.field-mark-group li {
    margin-bottom: 0.2rem;
}
```

- [ ] **Step 9: Run the console module's tests**

Run: `mvn test -pl domains/insects/insects-console`
Expected: PASS. The existing template tests render `family.jte` and friends without the new param — it defaults to `List.of()`, so the section is omitted and the tests still assert non-blank output.

- [ ] **Step 10: Run the full build and smoke-test**

Run: `mvn verify`
Expected: PASS.

Then start the console and visit `/insects/families/chrysomelidae`. Expected: a **Field Marks** section listing the eight persisted marks for that family, grouped under `Family — chrysomelidae`, starting with "strongly domed, broadly oval body outline". Visit `/insects/families/cleridae` for the seven-mark comparison — the two lists side by side are the point of the feature.

- [ ] **Step 11: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/FeatureGroup.java \
       domains/insects/insects-console/src/test/java/com/naturalist/insects/console/FeatureGroupTest.java \
       domains/insects/insects-console/src/main/jte/insects/features.jte \
       domains/insects/insects-console/src/main/jte/insects/detail.jte \
       domains/insects/insects-console/src/main/jte/insects/family.jte \
       domains/insects/insects-console/src/main/jte/insects/genus.jte \
       domains/insects/insects-console/src/main/jte/insects/order.jte \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
       apps/management-console/src/main/resources/static/css/naturalist.css
git commit -m "feat(insects): render lineage-composite field marks on every rank page

InsectFeatureView has been persisted and queryable since the feature-entity
migration but was never displayed. Adds FeatureGroup to reshape the view into
rank-labelled display groups, a features.jte component, and wiring in the
order/family/genus/species handlers.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Rank Observation Gallery with Identification Evidence

`familyDetail` builds its gallery from `imagesByGenus` — child-genus images only. A family with no genera renders no photos at all, even when photos are attached directly at the family rank (`parentName: chrysomelidae`). The families *list* page uses `forRankHierarchy(...)` and does show the thumbnail, which is why the same beetle appears on `/insects/families` but vanishes on `/insects/families/chrysomelidae`.

The same three pages also drop the confidence, evidence, and alternatives that `detail.jte` renders — precisely the ranks where rank-polymorphic identification now lands most often. Both gaps close with one extracted component.

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/observationGallery.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte:90-147` (replace with the component call), `family.jte`, `genus.jte`, `order.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — `detail`, `familyDetail`, `genusDetail`, `orderDetail`, `updateNotes`
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java`

**Interfaces:**
- Consumes: `InsectQuery.ImageQuery.forParentName(InsectRankName)`, `InsectQuery.FieldObservationQuery.forNaturalistAndSubjects(NaturalistName, Set<InsectRankName>)`, `FieldObservation.identification()`, `FeatureGroup` (Task 2)
- Produces:
  - `private Map<InsectImageId, FieldObservation> InsectsController.observationLookup(List<InsectImage>, Optional<NaturalistName>, InsectRankName)`
  - `observationGallery.jte` with params `images`, `observations`, `alt`, `notesAction`, `returnPath`, `_csrf`

- [ ] **Step 1: Extract the gallery into a component**

Create `domains/insects/insects-console/src/main/jte/insects/observationGallery.jte` with the block currently inline in `detail.jte:90-147`, parameterised on the alt text and the notes-form target:

```
@import com.naturalist.insects.FieldObservation
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectImageId
@import java.util.List
@import java.util.Map
@import org.springframework.security.web.csrf.CsrfToken

@param List<InsectImage> images = List.of()
@param Map<InsectImageId, FieldObservation> observations = Map.of()
@param String alt = ""
@param String notesAction = ""
@param String returnPath = ""
@param CsrfToken _csrf = null

@if(!images.isEmpty())
    <section>
        <h2>Photo Gallery</h2>
        <div class="image-gallery">
            @for(var img : images)
                <figure class="observation-card">
                    <img src="/insects/uploads/${img.resourceName().value()}"
                         alt="${alt}"
                         loading="lazy"
                         onerror="this.onerror=null; this.src='/insects/images/${img.resourceName().value()}'">
                    <%-- Fall back to classpath images for pre-upload catalog images --%>
                    @if(observations.containsKey(img.id()))
                        !{var obs = observations.get(img.id());}
                        <figcaption>
                            @if(obs.location() != null)
                                <p class="obs-location">${obs.location()}</p>
                            @endif
                            @if(obs.identification() != null)
                                !{var id = obs.identification();}
                                <div class="obs-identification">
                                    <p class="obs-confidence">
                                        <span class="obs-confidence-value">${String.format("%.0f%%", id.confidence() * 100)}</span> confidence
                                    </p>
                                    <details class="id-evidence">
                                        <summary>Why this ID?</summary>
                                        <p>${id.evidence()}</p>
                                    </details>
                                    @if(!id.alternatives().isEmpty())
                                        <p class="id-alternatives-label">Also considered</p>
                                        <ul class="id-alternatives">
                                            @for(var alt2 : id.alternatives())
                                                <li>
                                                    <em>${alt2.scientificName()}</em>@if(alt2.commonName() != null) (${alt2.commonName()})@endif
                                                    <span class="id-alternative-confidence">${String.format("%.0f%%", alt2.confidence() * 100)}</span>
                                                </li>
                                            @endfor
                                        </ul>
                                    @endif
                                </div>
                            @endif
                            @if(!notesAction.isEmpty())
                                <form method="post" action="${notesAction}" class="obs-notes-form">
                                    @if(_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}"
                                               value="${_csrf.getToken()}">
                                    @endif
                                    <input type="hidden" name="observationId" value="${obs.id().value().toString()}">
                                    <input type="hidden" name="returnPath" value="${returnPath}">
                                    <label class="obs-notes-label">Your notes</label>
                                    <textarea name="notes" rows="2"
                                              placeholder="Your field notes...">${obs.notes() != null ? obs.notes() : ""}</textarea>
                                    <button type="submit">Save notes</button>
                                </form>
                            @endif
                        </figcaption>
                    @endif
                </figure>
            @endfor
        </div>
    </section>
@endif
```

The loop variable is `alt2` because `alt` is now a template param — a `@for(var alt : ...)` would shadow it.

- [ ] **Step 2: Point `detail.jte` at the component**

In `detail.jte`, delete lines 90-147 (the whole `@if(!images.isEmpty())` gallery section) and replace with:

```
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = species.name().value(),
        notesAction = "/insects/" + species.name().value() + "/notes",
        returnPath = "/insects/" + species.name().value(),
        _csrf = _csrf)
```

This is behaviour-preserving: same markup, same CSS classes, plus one new hidden `returnPath` field whose value equals the redirect `updateNotes` already performs.

- [ ] **Step 3: Run the console tests to confirm the extraction is inert**

Run: `mvn test -pl domains/insects/insects-console`
Expected: PASS — the extraction changes no rendered output on the species page.

- [ ] **Step 4: Extract the observation-lookup helper in the controller**

In `InsectsController.java`, add this private method next to the other model helpers:

```java
    /**
     * Maps each image to the signed-in naturalist's own {@link FieldObservation}
     * for it, when one exists. Only the viewer's observations are exposed — the
     * gallery's notes form edits the viewer's own field notes, never someone
     * else's.
     */
    private Map<InsectImageId, FieldObservation> observationLookup(
            List<InsectImage> images,
            java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer,
            InsectRankName subject) {
        var lookup = new java.util.HashMap<InsectImageId, FieldObservation>();
        if (viewer.isEmpty()) {
            return lookup;
        }
        var mine = insectQuery.fieldObservations()
                .forNaturalistAndSubjects(viewer.get(), java.util.Set.of(subject));
        for (var img : images) {
            if (img.observationId() != null) {
                mine.stream()
                        .filter(o -> o.id().equals(img.observationId()))
                        .findFirst()
                        .ifPresent(o -> lookup.put(img.id(), o));
            }
        }
        return lookup;
    }
```

- [ ] **Step 5: Use the helper in `detail`, replacing the inline block**

In `detail`, replace the inline lookup block (the `var observations = new java.util.HashMap<...>()` through `model.addAttribute("observations", observations);` span) with:

```java
        var myObservations = viewer.isPresent()
                ? insectQuery.fieldObservations()
                        .forNaturalistAndSubjects(viewer.get(), java.util.Set.<InsectRankName>of(speciesName))
                : null;
        model.addAttribute("observations",
                observationLookup(galleryImages, viewer, speciesName));
```

`myObservations` stays — the `collected` flag below still reads it.

- [ ] **Step 6: Add images and observations to `familyDetail`**

Change the signature to take the request:

```java
    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, HttpServletRequest request, Model model) {
```

Then, immediately before `return "insects/family";`, add:

```java
        List<InsectImage> rankImages = insectQuery.images()
                .forParentName(familyName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, familyName));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
```

`forParentName` — not `forRankHierarchy` — is deliberate: the child-rank cards below already show descendant photos via the `gallery` attribute, and repeating them in the rank's own gallery would double every image.

- [ ] **Step 7: Repeat for `orderDetail` and `genusDetail`**

`orderDetail` — change the signature to `String orderDetail(@PathVariable String name, HttpServletRequest request, Model model)` and add before `return "insects/order";`:

```java
        List<InsectImage> rankImages = insectQuery.images()
                .forParentName(orderName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, orderName));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
```

`genusDetail` — change the signature to `String genusDetail(@PathVariable String name, HttpServletRequest request, Model model)` and add before `return "insects/genus";`:

```java
        List<InsectImage> rankImages = insectQuery.images()
                .forParentName(genusName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, genusName));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
```

Each handler already declares `familyName` / `orderName` / `genusName` as a local at the top of the method; reference those rather than re-deriving. `CSRF_REQUEST_ATTRIBUTE` is an existing private constant on the controller (line 56) and `currentNaturalist(HttpServletRequest)` an existing private static helper (line 61) — no new imports beyond `HttpServletRequest`, which the file already imports for `detail`.

- [ ] **Step 8: Add the component to the three rank templates**

In `family.jte`, add these imports:

```
@import com.naturalist.insects.FieldObservation
@import com.naturalist.insects.InsectImage
@import com.naturalist.insects.InsectImageId
@import java.util.Map
@import org.springframework.security.web.csrf.CsrfToken
```

and these params at the end of the `@param` block:

```
@param List<InsectImage> images = List.of()
@param Map<InsectImageId, FieldObservation> observations = Map.of()
@param CsrfToken _csrf = null
```

Then insert the component call immediately after the `@template.insects.features(...)` line added in Task 2:

```
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = commonName,
        notesAction = "/insects/" + family.name().value() + "/notes",
        returnPath = "/insects/families/" + family.name().value(),
        _csrf = _csrf)
```

In `genus.jte`, add the same imports and params, and the same call with the genus substituted:

```
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = commonName,
        notesAction = "/insects/" + genus.name().value() + "/notes",
        returnPath = "/insects/genera/" + genus.name().value(),
        _csrf = _csrf)
```

In `order.jte`, add the same imports and params, and:

```
    @template.insects.observationGallery(
        images = images,
        observations = observations,
        alt = commonName,
        notesAction = "/insects/" + order.name().value() + "/notes",
        returnPath = "/insects/orders/" + order.name().value(),
        _csrf = _csrf)
```

`commonName` is already declared as a `!{var ...}` at the top of all three templates.

- [ ] **Step 9: Make `updateNotes` honour `returnPath`**

`POST /insects/{name}/notes` is already rank-agnostic — it works off the `observationId` — but it redirects to `/insects/{name}`, which for a family slug falls through `detail()`'s species lookup and bounces the user to `/insects`. Replace the handler at line 693:

```java
    @PostMapping("/{name}/notes")
    String updateNotes(@PathVariable String name,
                       @RequestParam("observationId") String observationId,
                       @RequestParam("notes") String notes,
                       @RequestParam(value = "returnPath", required = false) String returnPath,
                       HttpServletRequest request) {
        var destination = safeReturnPath(returnPath, "/insects/" + name);
        var obsId = FieldObservationId.of(java.util.UUID.fromString(observationId));
        var existing = insectQuery.fieldObservations().getByName(obsId);
        if (existing.isEmpty()) {
            return "redirect:" + destination;
        }
        var obs = existing.get();
        var updated = obs.withNotes((notes == null || notes.isBlank()) ? null : notes);
        insectCommand.fieldObservations().update(updated);
        return "redirect:" + destination;
    }

    /**
     * Constrains a caller-supplied redirect target to this console's own insect
     * pages. A form field is untrusted input; without the prefix check it is an
     * open redirect.
     */
    private static String safeReturnPath(@Nullable String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        if (!candidate.startsWith("/insects/") || candidate.contains("//")) {
            return fallback;
        }
        return candidate;
    }
```

If `org.jspecify.annotations.Nullable` is not yet imported in this file, add it.

- [ ] **Step 10: Extend the family template test to cover the new params**

In `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java`, add a test alongside `family_rendersWithoutError`:

```java
    @Test
    void family_rendersRankImagesWithEvidence() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = orderSource.getByName(anyFamily.orderName()).orElseThrow();
        var image = new com.naturalist.insects.InsectImage(
                com.naturalist.insects.InsectImageId.create(),
                anyFamily.name(),
                java.time.Instant.parse("2026-07-16T01:54:24Z"),
                com.naturalist.data.FileName.of("beetle.jpg"),
                null);
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
                        "order", order,
                        "genera", List.of(),
                        "images", List.of(image),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).contains("Photo Gallery");
        assertThat(output.toString()).contains("beetle.jpg");
    }
```

An image with a null `observationId` has no `figcaption`, so this test covers the photo path; the evidence path is covered by the species-page tests that already render the same component.

- [ ] **Step 11: Run the console tests**

Run: `mvn test -pl domains/insects/insects-console`
Expected: PASS, including the new `family_rendersRankImagesWithEvidence`.

- [ ] **Step 12: Run the full build and smoke-test**

Run: `mvn verify`
Expected: PASS.

Then start the console and visit `/insects/families/chrysomelidae`. Expected: the beetle photo now appears under **Photo Gallery**, with a `72% confidence` badge, a "Why this ID?" disclosure, and an "Also considered" list naming Coccinellidae at 20%. Compare against `/insects/families/carabidae` (88%, Pterostichus / Poecilus) and `/insects/families/cleridae` (72%, Thanasimus / Opilo) — three readings of the same beetle, each with its photo, its confidence, its field marks, and its citation.

- [ ] **Step 13: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/observationGallery.jte \
       domains/insects/insects-console/src/main/jte/insects/detail.jte \
       domains/insects/insects-console/src/main/jte/insects/family.jte \
       domains/insects/insects-console/src/main/jte/insects/genus.jte \
       domains/insects/insects-console/src/main/jte/insects/order.jte \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
       domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java
git commit -m "feat(insects): show rank-level photos and identification evidence on family/genus/order

Rank pages built their gallery from child-rank images only, so a photo
attached at the family rank rendered on /insects/families but vanished on the
family's own page. Extracts detail.jte's gallery into observationGallery.jte
-- photo, confidence, evidence, alternatives, notes form -- and adds it to the
three rank pages, which is where rank-polymorphic identifications now land.
updateNotes gains a prefix-checked returnPath so the notes form returns to the
rank page it was submitted from.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Update the Tracker and Domain Notes

**Files:**
- Modify: `docs/work-tracker.md`
- Modify: `domains/insects/CLAUDE.md`

**Interfaces:**
- Consumes: nothing
- Produces: nothing — documentation only

- [ ] **Step 1: Add the completed row to the work tracker**

In `docs/work-tracker.md`, update the `Last updated:` line at line 10:

```markdown
Last updated: 2026-08-08 (Present-the-evidence shipped — citation associations persist, field marks and rank-level photo/evidence galleries on every rank page.)
```

Add this row at the top of the **Recently completed** table body (immediately below the header separator, above the 2026-07-15 row):

```markdown
| Present the evidence — `CitationAssociation` flush fix (`TestEntitySource.writable`/`writableClass` seam + parser-supplying `loadFile`), `FeatureGroup` + `features.jte` field marks on all four rank pages, `observationGallery.jte` extracted from `detail.jte` and added to family/genus/order | 2026-08-08 | [`plans/2026-08-08-present-the-evidence-plan.md`](plans/2026-08-08-present-the-evidence-plan.md) | _fill in final commit_ |
```

Replace `_fill in final commit_` with the short SHA of Task 3's commit.

- [ ] **Step 2: Add a row to the active-efforts index**

Append to the **Active efforts** table:

```markdown
| 14 | Present the evidence                  | Plan           | [`plans/2026-08-08-present-the-evidence-plan.md`](plans/2026-08-08-present-the-evidence-plan.md) — **shipped** 2026-08-08 (citation-association flush fix, field marks, rank observation gallery) |
```

- [ ] **Step 3: Refresh the current-slice section**

In the **Current phase** section, add this paragraph after the "Identification enrichment" paragraph:

```markdown
**Present the evidence** shipped 2026-08-08. The enrichment effort persisted features and
citations but never displayed them; three identifications of the same beetle produced
`carabidae`, `chrysomelidae`, and `cleridae` with no surfaced grounds for a human to
adjudicate between them. Citation associations now survive the flush (they were being
dropped for a null origin file), field marks render on every rank page, and family /
genus / order pages carry the photo gallery with per-observation confidence, evidence,
and alternatives that previously existed only on the species page.
```

In the **Candidate next slices** list, leave the existing entries — none of them were consumed by this work.

- [ ] **Step 4: Document the console surfaces in the insects domain notes**

In `domains/insects/CLAUDE.md`, append a section after **The Naturalist's Collection**:

```markdown
## Identification Evidence in the Console

Rank-polymorphic identification means most vision identifications land at ORDER or
FAMILY, not SPECIES. The four rank pages therefore carry the same evidence surfaces:

**`features.jte`** — renders `List<FeatureGroup>`, the console-side reshaping of
`InsectFeatureView` (lineage-composite, ancestor-first, ordinal-ordered within a rank).
Built by `FeatureGroup.of(view)`; handlers populate it via the controller's private
`featureGroups(InsectRankName)` helper.

**`observationGallery.jte`** — photo, `%` confidence, "Why this ID?" evidence
disclosure, "Also considered" alternatives, and the field-notes form. Sourced from
`FieldObservation.identification()`. The rank pages pass
`insectQuery.images().forParentName(rankName)` — **not** `forRankHierarchy` — because
the child-rank cards on the same page already show descendant photos via the `gallery`
attribute; using the hierarchy query would render every descendant image twice.

**`citations.jte`** — renders `Insect.citations()`, which resolves through
`CitationAssociation` records in the library domain. An identification that writes a
`Citation` without a matching association produces a silently citation-less page; see
`CitationAssociationJson` in `library-repository-test` for why that catalog needs a
hand-written DTO and how the flush seam keeps it round-trippable.

The notes form posts to `/insects/{name}/notes` from every rank and carries a
`returnPath` hidden field; `InsectsController.safeReturnPath` constrains it to
`/insects/` prefixes so the field cannot become an open redirect.
```

- [ ] **Step 5: Commit**

```bash
git add docs/work-tracker.md domains/insects/CLAUDE.md
git commit -m "docs: record present-the-evidence slice and console evidence surfaces

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Notes and Deliberate Omissions

**Not in scope — deduplicating the three beetle identifications.** The `carabidae` /
`chrysomelidae` / `cleridae` triple is real data the naturalist should adjudicate, not a
defect to clean up. Deleting domain data to make a finding go away is explicitly against
project practice. The `re-identify` endpoint already exists for the naturalist to
converge them once they decide, though its `TODO` about re-pointing linked images'
`parentName` remains open.

**Not in scope — the `re-identify` image-repointing TODO.** `InsectsController.reIdentify`
updates the observation's subject but leaves linked `InsectImage.parentName` stale,
because no query finds images by `observationId`. That needs a new read port and belongs
in its own slice. Worth noting that Task 3 makes the gap more visible: after a
re-identify, the photo stays in the old rank's gallery.

**Not in scope — an "Add Photo" form on the rank pages.** `detail.jte` has one; the rank
pages do not. `InsectAddPhotoCommand` already accepts an `InsectRankName`, so the wiring
is small, but it is a separate capability from presenting evidence and would widen this
diff without serving the goal.

**Feature dedup by value is still open.** `resolveFeatures` in
`InsectIdentificationCommand` creates a fresh `InsectFeature` per value per
identification rather than reusing an existing one — there is no `findByValue(String)`
on the feature repository. The 29 persisted features include near-duplicates across the
three beetle runs ("dark (black) pronotum contrasting with red elytra" vs. "dark/black
pronotum contrasting with red elytra"). Task 2 renders them faithfully; collapsing them
needs a query port and a normalisation decision, which is its own slice.
