# OrganismObservation Kernel Implementation Plan

> **REVISED to Design B (2026-08-19, mid-execution).** The entity is GENERIC over
> its id — `OrganismObservation<ID extends EntityId>` — with per-domain
> `InsectObservationId` / `PlantObservationId` (today's `FieldObservationId`
> renamed, kept in `identifiers`), NOT a single shared concrete
> `OrganismObservationId`. Tasks 1, 2, 4, 6 are unaffected and shipped as written.
> Task 3's kernel `OrganismObservationId` is **removed**; Task 5's record is
> **generic**; Tasks 7–9 migrate the domains to `OrganismObservation<…Id>` and
> **rename** (not delete) the per-domain ids. See the design doc's Design-B banner
> and the SDD ledger's "REVISED remaining tasks" for the authoritative steps.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the two duplicated per-domain `FieldObservation` records (insects, plants) with one shared `OrganismObservation` entity in a new `kernels/observation` module, preserving the typed taxonomic `subject`.

**Architecture:** Promote a non-sealed `RankName` interface into the `taxonomy` kernel; the domain sealed interfaces extend it. A new `kernels/observation` module holds `OrganismObservation`, `OrganismObservationId`, and `Identification`/`Candidate`. Because the shared record can no longer name domain permits at the `subject` field, `subject` serializes as a self-describing `{rank,value}` object via a kernel serializer, and deserializes through a per-domain `RankNameReconstructor` injected into that domain's persistence mapper. Insects then plants migrate to the shared type atomically.

**Tech Stack:** Java 21 (records, sealed interfaces, pattern matching), Maven multi-module, Jackson (databind + jsr310), JUnit 5 + AssertJ, JTE templates.

## Global Constraints

- **Typed identifiers only.** Never raw `String`/`Long`/`UUID` as an entity reference across a boundary. `Entity` records carry a concrete `EntityId` subtype.
- **UUIDv7 only.** Generate ids via `EntityId.newUUID()`; `UUID.randomUUID()` is forbidden.
- **Records for Entity/ValueObject.** Immutable, equality by value, `invariants()` declared.
- **Entity ids never cross a domain boundary by value.** A single kernel `OrganismObservationId` is legal — it is the observation's own id, not a cross-domain reference.
- **Repository interfaces stay package-private in api;** cross-domain interaction goes through public query/service classes.
- **Kernel signature changes need a clean install.** After editing `taxonomy` or `framework-test`, run `mvn clean install` (not incremental `-pl -am`) or stale classes cause `NoSuchMethodError` far from the cause.
- **The user runs Maven.** Do not invoke `mvn` yourself; hand the user the exact command and wait. Builds are `mvn verify` (or `mvn clean install`) from the repo root.
- **JSON round-trips use `ensure_ascii=False`** when rewriting catalog files with Python (em-dashes/UTF-8 in slugs must not be escaped).
- **DAG:** `observation → framework, taxonomy, identifiers`. `taxonomy → framework`. Nothing new may introduce a cycle.

---

## Phase A — taxonomy: the RankName seam + codec

### Task 1: Promote `RankName` to the taxonomy kernel

**Files:**
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankName.java`
- Modify: `domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java`
- Modify: `domains/identifiers/src/main/java/com/naturalist/plants/PlantRankName.java`
- Test: `kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankNameTest.java`

**Interfaces:**
- Produces: `interface com.naturalist.taxonomy.RankName { String value(); LinealRank rank(); }`. `InsectRankName`/`PlantRankName` now `extends RankName`.

- [ ] **Step 1: Write the failing test**

```java
// kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankNameTest.java
package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RankNameTest {
    @Test
    void exposesSlugAndRank() {
        RankName name = new RankName() {
            @Override public String value() { return "salvia"; }
            @Override public LinealRank rank() { return LinealRank.GENUS; }
        };
        assertThat(name.value()).isEqualTo("salvia");
        assertThat(name.rank()).isEqualTo(LinealRank.GENUS);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Hand the user: `mvn -q -pl kernels/taxonomy test` — Expected: FAIL, `RankName` does not exist.

- [ ] **Step 3: Create `RankName`**

```java
// kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankName.java
package com.naturalist.taxonomy;

/**
 * Organism-agnostic contract for a typed taxonomic-rank name: the slug it carries and the
 * {@link LinealRank} rung it occupies. Each organism domain's sealed rank-name interface
 * (e.g. {@code InsectRankName}, {@code PlantRankName}) extends this so shared types — the
 * observation kernel's {@code OrganismObservation.subject} — can reference any domain's
 * rank name without depending on that domain. Non-sealed by necessity: permits live in
 * domain packages a sealed type here could not enumerate.
 */
public interface RankName {
    String value();
    LinealRank rank();
}
```

- [ ] **Step 4: Make the domain interfaces extend it**

In `InsectRankName.java`: change the declaration to
`public sealed interface InsectRankName extends RankName permits …` and add
`import com.naturalist.taxonomy.RankName;`. Delete the now-inherited `String value();`
and `LinealRank rank();` method declarations (keep their javadoc on `RankName` only if
useful; the domain interface no longer redeclares them). Do the identical change in
`PlantRankName.java`. Leave the `static … of(String, LinealRank)` factories untouched.

- [ ] **Step 5: Run tests to verify green**

Hand the user: `mvn -q -pl kernels/taxonomy,domains/identifiers test` — Expected: PASS (taxonomy + identifiers compile; leaf permits already implement `value()`/`rank()`).

- [ ] **Step 6: Commit**

```bash
git add kernels/taxonomy domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java domains/identifiers/src/main/java/com/naturalist/plants/PlantRankName.java
git commit -m "feat(taxonomy): promote RankName interface; domain rank names extend it"
```

---

### Task 2: RankName JSON codec (the spike — prove the mechanism)

**Files:**
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankNameReconstructor.java`
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankNameSerializer.java`
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/RankNameDeserializer.java`
- Test: `kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankNameCodecTest.java`

**Interfaces:**
- Produces:
  - `@FunctionalInterface RankNameReconstructor { RankName reconstruct(String slug, LinealRank rank); }`
  - `RankNameSerializer extends JsonSerializer<RankName>` → writes `{"rank":<LinealRank name>,"value":<slug>}`.
  - `RankNameDeserializer extends JsonDeserializer<RankName>` → reads that object, resolves a `RankNameReconstructor` via `ctxt.findInjectableValue(RankNameReconstructor.class.getName(), null, null)`, returns `reconstructor.reconstruct(slug, LinealRank.valueOf(rank))`.
- Consumers register the reconstructor on their mapper with
  `new InjectableValues.Std().addValue(RankNameReconstructor.class, <impl>)`.

- [ ] **Step 1: Write the failing round-trip test**

```java
// kernels/taxonomy/src/test/java/com/naturalist/taxonomy/RankNameCodecTest.java
package com.naturalist.taxonomy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RankNameCodecTest {

    /** A stand-in typed permit — models a domain rank name for the codec test. */
    record FakeSpeciesName(String value) implements RankName {
        @JsonCreator static FakeSpeciesName of(String v) { return new FakeSpeciesName(v); }
        @Override public LinealRank rank() { return LinealRank.SPECIES; }
    }

    record Holder(
            @JsonSerialize(using = RankNameSerializer.class)
            @JsonDeserialize(using = RankNameDeserializer.class)
            RankName subject) {}

    private static final RankNameReconstructor RECONSTRUCTOR =
            (slug, rank) -> new FakeSpeciesName(slug);

    private static ObjectMapper mapperWithReconstructor() {
        return new ObjectMapper().setInjectableValues(
                new InjectableValues.Std().addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void subjectRoundtripsAsSelfDescribingObjectAndReconstructsTypedPermit() throws Exception {
        ObjectMapper mapper = mapperWithReconstructor();
        Holder original = new Holder(new FakeSpeciesName("battus-philenor"));

        String json = mapper.writeValueAsString(original);
        Holder decoded = mapper.readValue(json, Holder.class);

        assertThat(json).contains("\"rank\":\"SPECIES\"");
        assertThat(json).contains("\"value\":\"battus-philenor\"");
        assertThat(decoded.subject()).isInstanceOf(FakeSpeciesName.class);
        assertThat(decoded.subject().value()).isEqualTo("battus-philenor");
        assertThat(decoded.subject().rank()).isEqualTo(LinealRank.SPECIES);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Hand the user: `mvn -q -pl kernels/taxonomy test` — Expected: FAIL, codec classes missing.

- [ ] **Step 3: Implement the reconstructor SPI**

```java
// RankNameReconstructor.java
package com.naturalist.taxonomy;

/**
 * Domain-supplied bridge that rebuilds the correct typed permit from a slug + rank pair
 * read out of JSON. Each organism domain injects its own (e.g. {@code InsectRankName::of})
 * into the mapper that reads that domain's catalog; the kernel deserializer stays
 * domain-agnostic.
 */
@FunctionalInterface
public interface RankNameReconstructor {
    RankName reconstruct(String slug, LinealRank rank);
}
```

- [ ] **Step 4: Implement the serializer**

```java
// RankNameSerializer.java
package com.naturalist.taxonomy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/** Writes a {@link RankName} as a self-describing {@code {"rank":…,"value":…}} object. */
public class RankNameSerializer extends JsonSerializer<RankName> {
    @Override
    public void serialize(RankName value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("rank", value.rank().name());
        gen.writeStringField("value", value.value());
        gen.writeEndObject();
    }
}
```

- [ ] **Step 5: Implement the deserializer**

```java
// RankNameDeserializer.java
package com.naturalist.taxonomy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Reads a {@code {"rank":…,"value":…}} object and rebuilds the typed permit via the
 * {@link RankNameReconstructor} injected on the mapper. Fails loudly if no reconstructor
 * was registered — a mapper reading observation JSON must supply one.
 */
public class RankNameDeserializer extends JsonDeserializer<RankName> {
    @Override
    public RankName deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        String rankText = node.get("rank").asText();
        String slug = node.get("value").asText();
        Object injected = ctxt.findInjectableValue(
                RankNameReconstructor.class.getName(), null, null);
        if (!(injected instanceof RankNameReconstructor reconstructor)) {
            throw new IllegalStateException(
                    "No RankNameReconstructor registered on this ObjectMapper; "
                            + "register one via InjectableValues before reading RankName subjects.");
        }
        return reconstructor.reconstruct(slug, LinealRank.valueOf(rankText));
    }
}
```

- [ ] **Step 6: Run to verify green**

Hand the user: `mvn -q -pl kernels/taxonomy test` — Expected: PASS. This proves the typed-permit round-trip that every domain migration relies on.

- [ ] **Step 7: Commit**

```bash
git add kernels/taxonomy
git commit -m "feat(taxonomy): RankName JSON codec (self-describing subject + injected reconstructor)"
```

---

## Phase B — the observation kernel

### Task 3: Create `kernels/observation` module + `OrganismObservationId`

**Files:**
- Create: `kernels/observation/pom.xml`
- Create: `kernels/observation/src/main/java/com/naturalist/observation/OrganismObservationId.java`
- Modify: `kernels/pom.xml` (add `<module>observation</module>`)
- Modify: `pom.xml` (add `observation` to `<dependencyManagement>`)
- Test: `kernels/observation/src/test/java/com/naturalist/observation/OrganismObservationIdTest.java`

**Interfaces:**
- Produces: `final class OrganismObservationId extends EntityId` with `static of(UUID)` (`@JsonCreator`) and `static create()`.

- [ ] **Step 1: Create the module pom**

```xml
<!-- kernels/observation/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>kernels</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>observation</artifactId>
    <name>kernels :: observation</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>taxonomy</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>identifiers</artifactId>
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

- [ ] **Step 2: Register the module + dependency management**

In `kernels/pom.xml` add `<module>observation</module>` after `<module>taxonomy</module>`.
In the root `pom.xml` `<dependencyManagement>` block (near the `identifiers` entry ~line 121), add:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>observation</artifactId>
    <version>${project.version}</version>
</dependency>
```

Match the exact `<version>` style used by the neighboring `identifiers` entry (copy it).

- [ ] **Step 3: Write the failing id test**

```java
// OrganismObservationIdTest.java
package com.naturalist.observation;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationIdTest {
    @Test
    void createMintsAValidUuidV7() {
        OrganismObservationId id = OrganismObservationId.create();
        assertThat(id.isValid()).isTrue();
    }

    @Test
    void distinctIdsWithSameUuidAreEqual() {
        var id = OrganismObservationId.create();
        assertThat(OrganismObservationId.of(id.value())).isEqualTo(id);
    }
}
```

- [ ] **Step 4: Run to verify it fails**

Hand the user: `mvn -q -pl kernels/observation test` — Expected: FAIL, class missing (module resolves).

- [ ] **Step 5: Implement `OrganismObservationId`**

```java
// OrganismObservationId.java
package com.naturalist.observation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

/** Surrogate UUIDv7 identity for an {@link OrganismObservation}. */
public final class OrganismObservationId extends EntityId {
    private OrganismObservationId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static OrganismObservationId of(UUID value) {
        return new OrganismObservationId(value);
    }

    public static OrganismObservationId create() {
        return new OrganismObservationId(EntityId.newUUID());
    }
}
```

- [ ] **Step 6: Run to verify green + clean install (new module + kernel)**

Hand the user: `mvn clean install -q -pl kernels/observation -am` — Expected: PASS. (Clean install because a new kernel module joined the reactor.)

- [ ] **Step 7: Commit**

```bash
git add kernels/observation kernels/pom.xml pom.xml
git commit -m "feat(observation): new kernel module + OrganismObservationId"
```

---

### Task 4: Move `Identification`/`Candidate` into the observation kernel

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/Identification.java`
- Test: `kernels/observation/src/test/java/com/naturalist/observation/IdentificationTest.java`

**Interfaces:**
- Produces: `record com.naturalist.observation.Identification(double confidence, String evidence, List<Candidate> alternatives) implements ValueObject` with nested `record Candidate(String scientificName, @Nullable String commonName, double confidence) implements ValueObject`. Both keep their existing `invariants()`.

(The insects copy at `domains/insects/insects-api/.../Identification.java` is **not** deleted here — it is deleted in Task 7 when insects switches to this one. Two identically-shaped types coexisting in different packages is fine meanwhile.)

- [ ] **Step 1: Write the failing invariants test**

```java
// IdentificationTest.java
package com.naturalist.observation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IdentificationTest {
    @Test
    void validWhenConfidenceInRangeAndEvidencePresent() {
        var id = new Identification(0.9, "wing venation",
                List.of(new Identification.Candidate("Danaus plexippus", null, 0.1)));
        assertThat(id.confidence()).isEqualTo(0.9);
        assertThat(id.alternatives()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Hand the user: `mvn -q -pl kernels/observation test` — Expected: FAIL.

- [ ] **Step 3: Copy `Identification`/`Candidate` verbatim into `com.naturalist.observation`**

Copy the body of `domains/insects/insects-api/src/main/java/com/naturalist/insects/Identification.java` into the new file, changing only the `package` line to `com.naturalist.observation`. The type references it uses (`ValueObject` from `com.naturalist.ddd`, `Constraints` from `com.naturalist.observability`, `@Nullable`) are all available on the kernel's `framework` dependency. It has no organism-specific references.

- [ ] **Step 4: Run to verify green**

Hand the user: `mvn -q -pl kernels/observation test` — Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add kernels/observation/src/main/java/com/naturalist/observation/Identification.java kernels/observation/src/test/java/com/naturalist/observation/IdentificationTest.java
git commit -m "feat(observation): move Identification/Candidate into the kernel"
```

---

### Task 5: The `OrganismObservation` record

**Files:**
- Create: `kernels/observation/src/main/java/com/naturalist/observation/OrganismObservation.java`
- Test: `kernels/observation/src/test/java/com/naturalist/observation/OrganismObservationTest.java`

**Interfaces:**
- Produces: `record OrganismObservation(OrganismObservationId id, NaturalistName observedBy, RankName subject, Instant observedOn, @Nullable String notes, @Nullable String location, @Nullable Identification identification) implements Entity<OrganismObservationId>` with `withNotes(@Nullable String)` and `withSubject(RankName)`. `subject` carries `@JsonSerialize(using = RankNameSerializer.class)` + `@JsonDeserialize(using = RankNameDeserializer.class)`.

- [ ] **Step 1: Write the failing test**

```java
// OrganismObservationTest.java
package com.naturalist.observation;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrganismObservationTest {

    record FakeGenusName(String value) implements RankName {
        @Override public LinealRank rank() { return LinealRank.GENUS; }
    }

    private static final RankNameReconstructor RECONSTRUCTOR =
            (slug, rank) -> new FakeGenusName(slug);

    private static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setInjectableValues(new InjectableValues.Std()
                        .addValue(RankNameReconstructor.class, RECONSTRUCTOR));
    }

    @Test
    void withNotesPreservesEverythingElse() {
        var obs = new OrganismObservation(
                OrganismObservationId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null);
        var updated = obs.withNotes("in the herb bed");
        assertThat(updated.notes()).isEqualTo("in the herb bed");
        assertThat(updated.subject()).isEqualTo(obs.subject());
        assertThat(updated.id()).isEqualTo(obs.id());
    }

    @Test
    void serializesSubjectAsSelfDescribingObjectAndRoundtrips() throws Exception {
        var obs = new OrganismObservation(
                OrganismObservationId.create(), NaturalistName.of("ada"),
                new FakeGenusName("salvia"), Instant.parse("2026-08-19T00:00:00Z"),
                null, null, null);
        String json = mapper().writeValueAsString(obs);
        assertThat(json).contains("\"rank\":\"GENUS\"").contains("\"value\":\"salvia\"");
        var decoded = mapper().readValue(json, OrganismObservation.class);
        assertThat(decoded.subject().value()).isEqualTo("salvia");
    }
}
```

(Confirm `NaturalistName.of(String)` is the correct factory by checking
`domains/identifiers/src/main/java/com/naturalist/naturalist/NaturalistName.java`; adjust the call if the factory name differs.)

- [ ] **Step 2: Run to verify it fails**

Hand the user: `mvn -q -pl kernels/observation test` — Expected: FAIL, `OrganismObservation` missing.

- [ ] **Step 3: Implement `OrganismObservation`**

```java
// OrganismObservation.java
package com.naturalist.observation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameDeserializer;
import com.naturalist.taxonomy.RankNameSerializer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A naturalist's field observation of an organism — the shared collection unit across
 * organism domains. Records that a naturalist ({@code observedBy}) encountered an organism
 * at a taxonomic rank ({@code subject}) at a point in time. Photographic evidence is optional
 * and lives on each domain's image entity via an observation link. {@code identification}
 * carries an optional machine (vision) result; a manual sighting leaves it null.
 *
 * <p>{@code subject} is a domain permit ({@code InsectSpeciesName}, {@code PlantGenusName}, …)
 * widened to {@link RankName}. It serializes as a self-describing {@code {"rank":…,"value":…}}
 * object; deserialization rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see the domain's observation test-entity source).
 */
public record OrganismObservation(
        OrganismObservationId id,
        NaturalistName observedBy,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        RankName subject,
        Instant observedOn,
        @Nullable String notes,
        @Nullable String location,
        @Nullable Identification identification
) implements Entity<OrganismObservationId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(observedBy, "observedBy")
                .identifier(subject, "subject")
                .notNull(observedOn, "observedOn")
                .valueObjectOrNull(identification, "identification");
    }

    public OrganismObservation withNotes(@Nullable String notes) {
        return new OrganismObservation(id, observedBy, subject, observedOn, notes, location, identification);
    }

    public OrganismObservation withSubject(RankName subject) {
        return new OrganismObservation(id, observedBy, subject, observedOn, notes, location, identification);
    }
}
```

**Note on `.identifier(subject, …)`:** the insects/plants records call `.identifier(subject, "subject")` today with a `RankName` permit. Confirm the `Constraints.identifier(...)` overload accepts the `RankName` supertype (it takes the `EntityName`/named-value shape the permits already satisfy). If it is typed to `EntityName`, keep `subject` validating through the permit — since every permit is an `EntityName`, pass it as-is; if the compiler rejects the widened `RankName`, add a `Constraints.identifier` overload accepting `RankName` in a preparatory step and note it in the commit.

- [ ] **Step 4: Run to verify green**

Hand the user: `mvn -q -pl kernels/observation test` — Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add kernels/observation/src/main/java/com/naturalist/observation/OrganismObservation.java kernels/observation/src/test/java/com/naturalist/observation/OrganismObservationTest.java
git commit -m "feat(observation): OrganismObservation record with self-describing RankName subject"
```

---

## Phase C — framework-test mapper hook

### Task 6: Per-source `mapper()` override in `TestEntitySource`

**Files:**
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/TestDataHelper.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java`
- Test: `kernels/framework-test/src/test/java/com/naturalist/data/TestEntitySourceMapperTest.java` (or extend an existing framework-test suite)

**Interfaces:**
- Produces: `TestDataHelper.newBaseMapper()` → a freshly-configured `ObjectMapper` (JavaTime + dates-as-ISO). `TestEntitySource.mapper()` → `protected ObjectMapper` defaulting to `TestDataHelper.mapper`; both the load-parse path and `writeJsonAtomic` route through `mapper()`.

- [ ] **Step 1: Add `newBaseMapper()` to `TestDataHelper`, keep the static field**

Replace the static initializer so the shared instance is built from the factory:

```java
public static ObjectMapper newBaseMapper() {
    return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
}

static final ObjectMapper mapper = newBaseMapper();
```

- [ ] **Step 2: Add the overridable `mapper()` and route reads/writes through it**

In `TestEntitySource`, add:

```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
// …

/**
 * The Jackson mapper this source uses to read and write its catalog. Defaults to the
 * shared base mapper. A source whose entity has a component needing per-domain
 * (de)serialization — e.g. an {@code OrganismObservation} subject — overrides this to
 * register the domain's codec.
 */
protected ObjectMapper mapper() {
    return TestDataHelper.mapper;
}
```

Change the default `loadFile(String)` parser to read through `mapper()`:

```java
public void loadFile(String relativePath) {
    loadFile(relativePath, json -> {
        try {
            return mapper().readerForListOf(entityClass()).readValue(json);
        } catch (JsonProcessingException e) {
            throw new java.io.UncheckedIOException(e);
        }
    });
}
```

In `writeJsonAtomic`, replace both `TestDataHelper.mapper` references with `mapper()`.

- [ ] **Step 3: Write a focused test that a custom `mapper()` is honored**

```java
// TestEntitySourceMapperTest.java
package com.naturalist.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TestEntitySourceMapperTest {
    @Test
    void newBaseMapperIsAFreshInstanceEachCall() {
        ObjectMapper a = TestDataHelper.newBaseMapper();
        ObjectMapper b = TestDataHelper.newBaseMapper();
        assertThat(a).isNotSameAs(b);
    }
}
```

(The behavioral proof that `mapper()` is actually consulted on the load path arrives in Tasks 7–8 when the domain sources override it; this unit test guards the factory contract. Do **not** add a mock `TestEntitySource` subclass purely to test the hook — that is "test infra for test infra"; the domain suites exercise it.)

- [ ] **Step 4: Run to verify green + clean install (kernel-test signature change)**

Hand the user: `mvn clean install -q -pl kernels/framework-test -am` — Expected: PASS. Existing TestEntitySource subclasses compile unchanged (they inherit the default `mapper()`).

- [ ] **Step 5: Commit**

```bash
git add kernels/framework-test
git commit -m "feat(framework-test): overridable per-source ObjectMapper hook"
```

---

## Phase D — insects migration (atomic; keeps the build green)

### Task 7: Migrate insects to `OrganismObservation`

A type unification does not compile half-done. Treat this as one reviewable unit: make every edit below, then verify. Use the IDE's *rename/move* refactor where possible, but the type changes are cross-package so the imports must be repointed by hand or by find/replace.

**Type/name mapping (apply everywhere in `domains/insects/**` and the two apps files):**

| Old (insects) | New |
| --- | --- |
| `com.naturalist.insects.FieldObservation` | `com.naturalist.observation.OrganismObservation` |
| `com.naturalist.insects.FieldObservationId` | `com.naturalist.observation.OrganismObservationId` |
| `com.naturalist.insects.Identification` | `com.naturalist.observation.Identification` |
| `Identification.Candidate` | `com.naturalist.observation.Identification.Candidate` |
| plumbing class `FieldObservationQueryImpl` | `OrganismObservationQueryImpl` |
| plumbing class `FieldObservationCommandImpl` | `OrganismObservationCommandImpl` |
| plumbing class `FieldObservationRepositoryMock` | `OrganismObservationRepositoryMock` |
| plumbing class `FieldObservationTestEntitySource` | `OrganismObservationTestEntitySource` |
| plumbing class `FieldObservationEntityRepositoryTest` | `OrganismObservationEntityRepositoryTest` |
| plumbing class `FieldObservationRepositoryMockTest` | `OrganismObservationRepositoryMockTest` |
| test class `FieldObservationTest` | `OrganismObservationTest` (in insects-api test; distinct from the kernel one — keep it in `com.naturalist.insects` or drop if fully covered by the kernel test) |

**Files to edit** (from the inventory — verify with the grep in Step 8; do not trust this list to be exhaustive):
`InsectRepository`, `InsectQuery`, `InsectCommand`, `InsectEntityCollections`, `CatalogIdentification`, `IdentifiedRankEntity`, `PhotoAddition`, `InsectImage` (only if it imports the observation id/type), and their `-core` impls (`InsectQueryImpl`, `InsectCommandImpl`, `InsectAddPhotoCommand`, `InsectAddPhotoTransaction`, `InsectCatalogIdentificationTransaction`, `InsectIdentificationCommand`, `InsectIdentificationResult`, `FieldObservationCommandImpl`, `FieldObservationQueryImpl`), the repository-test plumbing, `InsectsTestContext(Internal)`, the console `InsectsController`, and every test named above.

- [ ] **Step 1: Add the observation dependency to the insects api pom**

In `domains/insects/insects-api/pom.xml`, add (alongside `taxonomy`/`identifiers`):

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>observation</artifactId>
</dependency>
```

`insects-core`, `insects-repository-test`, `insects-console`, `insects-test-context`, and `apps/management-console` inherit the type transitively through `insects-api`; add a direct `observation` dependency only to a module the build reports as missing it.

- [ ] **Step 2: Repoint api-layer types**

Delete `domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java` and `Identification.java`. In the remaining api files (`InsectRepository`, `InsectQuery`, `InsectCommand`, `InsectEntityCollections`, `CatalogIdentification`, `IdentifiedRankEntity`, `PhotoAddition`), replace `FieldObservation` → `OrganismObservation`, `FieldObservationId` → `OrganismObservationId`, and the `Identification` import to the observation package. The `.subject()`/`.parentName()` consumers (`CatalogIdentification`, `PhotoAddition`) keep compiling — they compare `RankName` permits by value.

- [ ] **Step 3: Repoint + rename core-layer plumbing**

Rename `FieldObservationQueryImpl` → `OrganismObservationQueryImpl`, `FieldObservationCommandImpl` → `OrganismObservationCommandImpl` (files + class names), and repoint every `FieldObservation*` type reference in the `-core` module per the mapping table. Method-local variable names may stay; only types/imports change.

- [ ] **Step 4: Rename + rewire the repository-test source, register the reconstructor**

Rename `FieldObservationTestEntitySource` → `OrganismObservationTestEntitySource`, `FieldObservationRepositoryMock` → `OrganismObservationRepositoryMock`, `FieldObservationEntityRepositoryTest` → `OrganismObservationEntityRepositoryTest`. In the renamed test-entity source, add the mapper override:

```java
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.data.TestDataHelper;
import com.naturalist.taxonomy.RankNameReconstructor;

private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
        .setInjectableValues(new InjectableValues.Std()
                .addValue(RankNameReconstructor.class,
                        (RankNameReconstructor) InsectRankName::of));

@Override
protected ObjectMapper mapper() {
    return mapper;
}
```

The `foreignKeyConstraints()` `instanceof InsectFamilyName`/`InsectGenusName`/`InsectSpeciesName` extractors are unchanged — `subject()` still returns a `RankName` whose runtime type is the concrete permit.

- [ ] **Step 5: Migrate the insects catalog JSON to the `{rank,value}` subject shape**

Transform `domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json`. For every entry, replace the flat `"subjectRank": "<RANK>", "subject": "<slug>"` pair with `"subject": { "rank": "<RANK>", "value": "<slug>" }`. Concrete transform:

```python
import json
p = "domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json"
data = json.load(open(p))
for o in data:
    rank = o.pop("subjectRank")
    slug = o.pop("subject")
    o["subject"] = {"rank": rank, "value": slug}
json.dump(data, open(p, "w"), indent=2, ensure_ascii=False)
open(p, "a").write("\n")
```

(If the file also stores `identification`, leave those objects unchanged — `Identification` moved packages but its JSON shape is identical.) Consider renaming the resource to `organism-observations.json` **only** if you also update the `loadFile(...)` path in the source; otherwise keep the filename to minimize churn.

- [ ] **Step 6: Repoint console + templates**

In `InsectsController` and the console tests (`InsectsControllerNotesOwnershipTest`, `InsectsFamiliesTemplateTest`), repoint `FieldObservation`/`FieldObservationId`/`Identification` per the mapping. In the 8 JTE templates (`detail`, `family`, `features`, `genus`, `identify`, `list`, `observationGallery`, `order`), update any `@import com.naturalist.insects.FieldObservation` / `Identification` lines to the observation package and rename referenced types. Grep the templates for `FieldObservation`/`Identification` and fix each `@import` and type usage. (Recall: JTE templates are compiled per module — a stale type reference is a build-time failure, so the verify in Step 9 catches misses.)

- [ ] **Step 7: Repoint the two apps/management-console files**

`apps/management-console/.../ResilienceConfiguration.java` and its test reference the observation types only incidentally (resilience wiring). Repoint imports per the mapping.

- [ ] **Step 8: Sweep for stragglers**

Run and confirm zero hits in insects/app main+test sources:

```bash
grep -rnE '\bFieldObservation\b|com\.naturalist\.insects\.Identification' domains/insects apps --include='*.java' --include='*.jte'
```

Every remaining hit is a missed edit — fix it. (Do not touch `InsectImage`'s own field-level `@JsonSubTypes` for `parentName`; Image is out of scope and its `InsectRankNameJacksonTest` stays valid.)

- [ ] **Step 9: Verify the whole build green**

Hand the user: `mvn clean install` from the repo root — Expected: PASS, including `insects-repository-test` contract tests loading the migrated JSON and reconstructing typed permits. If a repository/query test fails to reconstruct `subject`, the reconstructor registration in Step 4 or the JSON transform in Step 5 is the suspect.

- [ ] **Step 10: Commit**

```bash
git add domains/insects apps/management-console
git commit -m "refactor(insects): adopt shared OrganismObservation kernel type"
```

---

## Phase E — plants migration (atomic)

### Task 8: Migrate plants to `OrganismObservation`

Mirror Task 7 for plants. **Key difference:** plants' `FieldObservation` has **six** components (no `identification`). Every construction site gains a trailing `null` argument for the new 7th component.

**Type/name mapping:** identical to Task 7 but under `com.naturalist.plants`, with reconstructor `PlantRankName::of`.

**Files** (from inventory; verify with Step 7 grep): `PlantRepository`, `PlantQuery`, `PlantEntityCollections`, `PlantImage` (only if it imports the id/type), `plants-core` (`PlantQueryImpl`, `FieldObservationQueryImpl`, `catalog/PlantsCompoundReferences`), repository-test (`FieldObservationRepositoryMock`, `FieldObservationRepositoryTest`, `FieldObservationTestEntitySource`, mock test), `PlantsTestContext`, plants console templates/controller if any reference it, and the plants api `FieldObservationTest`.

- [ ] **Step 1: Add the observation dependency to `plants-api/pom.xml`** (as in Task 7 Step 1).

- [ ] **Step 2: Delete `plants.FieldObservation`; repoint api types** per the mapping. Plants has no `plants.Identification` to delete.

- [ ] **Step 3: Rename + repoint core plumbing** (`FieldObservationQueryImpl` → `OrganismObservationQueryImpl`, etc.).

- [ ] **Step 4: Add the trailing `null` identification argument** at every `new OrganismObservation(...)` site in plants (previously 6-arg `new FieldObservation(...)`). Grep to find them:

```bash
grep -rnE 'new (FieldObservation|OrganismObservation)\(' domains/plants --include='*.java'
```

Each becomes `new OrganismObservation(id, observedBy, subject, observedOn, notes, location, null)`.

- [ ] **Step 5: Rename + rewire the plants test-entity source with the reconstructor**

```java
private final ObjectMapper mapper = TestDataHelper.newBaseMapper()
        .setInjectableValues(new InjectableValues.Std()
                .addValue(RankNameReconstructor.class,
                        (RankNameReconstructor) PlantRankName::of));

@Override
protected ObjectMapper mapper() {
    return mapper;
}
```

- [ ] **Step 6: Migrate `plants/field-observations.json`** to the `{rank,value}` subject shape:

```python
import json
p = "domains/plants/plants-repository-test/src/main/resources/plants/field-observations.json"
data = json.load(open(p))
for o in data:
    rank = o.pop("subjectRank")
    slug = o.pop("subject")
    o["subject"] = {"rank": rank, "value": slug}
json.dump(data, open(p, "w"), indent=2, ensure_ascii=False)
open(p, "a").write("\n")
```

Plants entries have no `identification` field — after migration the record deserializes it as `null`, which is correct.

- [ ] **Step 7: Sweep for stragglers**

```bash
grep -rnE '\bFieldObservation\b' domains/plants --include='*.java' --include='*.jte'
```

Fix every hit. (Leave `PlantImage`'s `parentName` `@JsonSubTypes` and `PlantRankNameDispatchTest` untouched — Image out of scope.)

- [ ] **Step 8: Verify green**

Hand the user: `mvn clean install` from the repo root — Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add domains/plants
git commit -m "refactor(plants): adopt shared OrganismObservation kernel type"
```

---

## Phase F — cleanup

### Task 9: Remove the dead per-domain identifiers

**Files:**
- Delete: `domains/identifiers/src/main/java/com/naturalist/insects/FieldObservationId.java`
- Delete: `domains/identifiers/src/main/java/com/naturalist/plants/FieldObservationId.java`
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java`
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/plants/TestPlantsIdentifiers.java`

- [ ] **Step 1: Confirm nothing references the old ids**

```bash
grep -rn 'com\.naturalist\.insects\.FieldObservationId\|com\.naturalist\.plants\.FieldObservationId' . --include='*.java'
```

Expected after Tasks 7–8: hits only in the two `identifiers` id files and the two `identifiers-test` helpers.

- [ ] **Step 2: Delete the two `FieldObservationId` classes.**

- [ ] **Step 3: Update `TestInsectsIdentifiers` / `TestPlantsIdentifiers`**

Wherever they mint a `FieldObservationId` (e.g. `FieldObservationId.create()` for a test fixture), repoint to `com.naturalist.observation.OrganismObservationId`. If these helpers live in `identifiers-test` and cannot depend on the `observation` kernel, move the observation-id fixture accessor to the domain test-context modules (`InsectsTestContext`/`PlantsTestContext`) instead, and delete the accessor here. Confirm the `identifiers-test` pom does not need an `observation` dependency (prefer relocating the fixture over adding kernel deps to `identifiers-test`).

- [ ] **Step 4: Verify green**

Hand the user: `mvn clean install` from the repo root — Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers domains/identifiers-test domains/insects domains/plants
git commit -m "chore(identifiers): remove per-domain FieldObservationId; use kernel OrganismObservationId"
```

- [ ] **Step 6: Update the work tracker + design doc status**

Add/refresh the row in `docs/work-tracker.md` for this effort, and mark the design doc `docs/plans/2026-08-19-organism-observation-kernel-design.md` status Shipped. Commit:

```bash
git add docs/work-tracker.md docs/plans/2026-08-19-organism-observation-kernel-design.md
git commit -m "docs: mark OrganismObservation kernel shipped; work-tracker row"
```

---

## Follow-up (not in this plan)

- **OrganismImage** — unify `Insect/PlantImage` (`parentName : RankName`) reusing this kernel + codec pattern; migrate `InsectRankNameJacksonTest` at that point.
- **Console breadcrumb / clade-trail** shared module (reconcile the `CladeTrail` vs `PlantCladeTree` divergence first).
- **Non-taxonomic `subject`** (soil/weather observations) — generalize only when such an observation is actually built.
- **Plumbing prefixing** — whether the per-domain `OrganismObservation*` plumbing and other bare `*QueryImpl`/`*RepositoryMock` internals should be domain-prefixed.
