# Insect Domain Record Helpers

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract domain-level factory and mutation helpers from the console controller so that record construction and field-observation mutation logic lives on the domain types, reusable by any future console (desktop, mobile, API).

**Architecture:** Add a static factory `InsectRankName.of(String, LinealRank)` for slug + rank → typed rank name dispatch (currently a manual switch in the controller). Add `withNotes` and `withSubject` mutation methods to `FieldObservation` so callers don't reconstruct the full record by hand. Simplify the controller methods that currently inline this logic.

**Tech Stack:** Java 21 records, JUnit 5, AssertJ

## Global Constraints

- `InsectRankName` is a sealed interface in `domains/identifiers/`; static methods on sealed interfaces are legal in Java 17+.
- `FieldObservation` is a record in `insects-api` implementing `Entity<FieldObservationId>`.
- Records use `with*` method naming per domain conventions.
- Tests run via `mvn verify` from repo root (user runs builds locally — do not invoke `mvn`).
- `SUBSPECIES` is a valid rank on `InsectRankName` but has no catalog entity yet — the factory should support it, the controller switch should too.
- `LinealRank` has values beyond the 5 insect ranks (KINGDOM, PHYLUM, CLASS) — the factory must reject those with `IllegalArgumentException`.

---

### Task 1: `InsectRankName.of(String, LinealRank)` static factory

**Files:**
- Modify: `domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java`
- Create: `domains/identifiers/src/test/java/com/naturalist/insects/InsectRankNameTest.java`

**Interfaces:**
- Consumes: `InsectOrderName.of(String)`, `InsectFamilyName.of(String)`, `InsectGenusName.of(String)`, `InsectSpeciesName.of(String)`, `InsectSubspeciesName.of(String)`, `LinealRank` enum
- Produces: `static InsectRankName of(String slug, LinealRank rank)` — dispatches to the correct permit's `of` factory; throws `IllegalArgumentException` for non-insect ranks (KINGDOM, PHYLUM, CLASS)

- [ ] **Step 1: Write the failing tests**

```java
package com.naturalist.insects;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectRankNameTest {

    @Test
    void species() {
        InsectRankName result = InsectRankName.of("battus-philenor", LinealRank.SPECIES);
        assertThat(result).isInstanceOf(InsectSpeciesName.class);
        assertThat(result.value()).isEqualTo("battus-philenor");
    }

    @Test
    void genus() {
        InsectRankName result = InsectRankName.of("battus", LinealRank.GENUS);
        assertThat(result).isInstanceOf(InsectGenusName.class);
        assertThat(result.value()).isEqualTo("battus");
    }

    @Test
    void family() {
        InsectRankName result = InsectRankName.of("papilionidae", LinealRank.FAMILY);
        assertThat(result).isInstanceOf(InsectFamilyName.class);
        assertThat(result.value()).isEqualTo("papilionidae");
    }

    @Test
    void order() {
        InsectRankName result = InsectRankName.of("lepidoptera", LinealRank.ORDER);
        assertThat(result).isInstanceOf(InsectOrderName.class);
        assertThat(result.value()).isEqualTo("lepidoptera");
    }

    @Test
    void subspecies() {
        InsectRankName result = InsectRankName.of("battus-philenor-hirsuta", LinealRank.SUBSPECIES);
        assertThat(result).isInstanceOf(InsectSubspeciesName.class);
        assertThat(result.value()).isEqualTo("battus-philenor-hirsuta");
    }

    @ParameterizedTest
    @EnumSource(value = LinealRank.class, names = {"KINGDOM", "PHYLUM", "CLASS"})
    void rejectsNonInsectRanks(LinealRank rank) {
        assertThatThrownBy(() -> InsectRankName.of("test", rank))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl domains/identifiers -Dtest=InsectRankNameTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation failure — `of(String, LinealRank)` does not exist yet.

- [ ] **Step 3: Implement the factory method**

Add to `InsectRankName.java`, inside the sealed interface body, after the `rank()` method:

```java
/**
 * Creates the appropriate {@code InsectRankName} permit for the given slug
 * and Linnaean rank. Only the five insect-side ranks are supported; higher
 * ranks (Kingdom, Phylum, Class) throw {@link IllegalArgumentException}.
 */
static InsectRankName of(String slug, LinealRank rank) {
    return switch (rank) {
        case ORDER -> InsectOrderName.of(slug);
        case FAMILY -> InsectFamilyName.of(slug);
        case GENUS -> InsectGenusName.of(slug);
        case SPECIES -> InsectSpeciesName.of(slug);
        case SUBSPECIES -> InsectSubspeciesName.of(slug);
        default -> throw new IllegalArgumentException("Unsupported insect rank: " + rank);
    };
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl domains/identifiers -Dtest=InsectRankNameTest`
Expected: all 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java
git add domains/identifiers/src/test/java/com/naturalist/insects/InsectRankNameTest.java
git commit -m "feat(identifiers): add InsectRankName.of(slug, rank) static factory"
```

---

### Task 2: `FieldObservation.withNotes()` and `.withSubject()`

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java`
- Modify: `domains/insects/insects-repository-test/src/test/java/com/naturalist/insects/FieldObservationRepositoryContractTest.java` (or create a unit test alongside the entity)
- Create: `domains/insects/insects-api/src/test/java/com/naturalist/insects/FieldObservationTest.java`

**Interfaces:**
- Consumes: `FieldObservation` record components
- Produces: `FieldObservation withNotes(@Nullable String notes)`, `FieldObservation withSubject(InsectRankName subject)`

- [ ] **Step 1: Write the failing tests**

Check whether `insects-api` already has a `src/test` directory:

```bash
ls domains/insects/insects-api/src/test/java/com/naturalist/insects/ 2>/dev/null || echo "no test dir"
```

If no test directory exists, create `domains/insects/insects-api/src/test/java/com/naturalist/insects/FieldObservationTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FieldObservationTest {

    private final FieldObservation base = new FieldObservation(
            FieldObservationId.create(),
            NaturalistName.of("pat"),
            InsectSpeciesName.of("battus-philenor"),
            Instant.parse("2026-07-01T12:00:00Z"),
            "original notes",
            "oak vista",
            null);

    @Test
    void withNotes_replacesNotes() {
        var updated = base.withNotes("new notes");
        assertThat(updated.notes()).isEqualTo("new notes");
        assertThat(updated.id()).isEqualTo(base.id());
        assertThat(updated.observedBy()).isEqualTo(base.observedBy());
        assertThat(updated.subject()).isEqualTo(base.subject());
        assertThat(updated.observedOn()).isEqualTo(base.observedOn());
        assertThat(updated.location()).isEqualTo(base.location());
        assertThat(updated.identification()).isEqualTo(base.identification());
    }

    @Test
    void withNotes_acceptsNull() {
        var updated = base.withNotes(null);
        assertThat(updated.notes()).isNull();
    }

    @Test
    void withSubject_replacesSubject() {
        var newSubject = InsectGenusName.of("battus");
        var updated = base.withSubject(newSubject);
        assertThat(updated.subject()).isEqualTo(newSubject);
        assertThat(updated.id()).isEqualTo(base.id());
        assertThat(updated.observedBy()).isEqualTo(base.observedBy());
        assertThat(updated.notes()).isEqualTo(base.notes());
        assertThat(updated.observedOn()).isEqualTo(base.observedOn());
        assertThat(updated.location()).isEqualTo(base.location());
        assertThat(updated.identification()).isEqualTo(base.identification());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-api -Dtest=FieldObservationTest`
Expected: compilation failure — `withNotes` and `withSubject` do not exist.

- [ ] **Step 3: Implement the mutation methods**

Add to `FieldObservation.java`, inside the record body after the `invariants()` method:

```java
public FieldObservation withNotes(@Nullable String notes) {
    return new FieldObservation(id, observedBy, subject, observedOn, notes, location, identification);
}

public FieldObservation withSubject(InsectRankName subject) {
    return new FieldObservation(id, observedBy, subject, observedOn, notes, location, identification);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl domains/insects/insects-api -Dtest=FieldObservationTest`
Expected: all 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/FieldObservationTest.java
git commit -m "feat(insects-api): add withNotes and withSubject helpers to FieldObservation"
```

---

### Task 3: Simplify controller `reIdentify()` and `updateNotes()`

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Consumes: `InsectRankName.of(String, LinealRank)` (from Task 1), `FieldObservation.withNotes(String)`, `FieldObservation.withSubject(InsectRankName)` (from Task 2)

- [ ] **Step 1: Simplify `reIdentify()`**

Replace the manual switch in `InsectsController.reIdentify()` (lines ~795–801):

```java
// Before:
InsectRankName newRankName = switch (newSubjectRank) {
    case "SPECIES" -> InsectSpeciesName.of(newSubject);
    case "GENUS" -> InsectGenusName.of(newSubject);
    case "FAMILY" -> InsectFamilyName.of(newSubject);
    case "ORDER" -> InsectOrderName.of(newSubject);
    default -> throw new IllegalArgumentException("Unknown rank: " + newSubjectRank);
};
```

```java
// After:
InsectRankName newRankName = InsectRankName.of(
        newSubject, com.naturalist.taxonomy.LinealRank.valueOf(newSubjectRank));
```

And replace the manual record reconstruction (lines ~804–806):

```java
// Before:
var updatedObs = new FieldObservation(
        obs.id(), obs.observedBy(), newRankName, obs.observedOn(),
        obs.notes(), obs.location(), obs.identification());

// After:
var updatedObs = obs.withSubject(newRankName);
```

- [ ] **Step 2: Simplify `updateNotes()`**

Replace the manual record reconstruction in `InsectsController.updateNotes()` (lines ~776–779):

```java
// Before:
var updated = new FieldObservation(
        obs.id(), obs.observedBy(), obs.subject(), obs.observedOn(),
        (notes == null || notes.isBlank()) ? null : notes,
        obs.location(), obs.identification());

// After:
var updated = obs.withNotes((notes == null || notes.isBlank()) ? null : notes);
```

- [ ] **Step 3: Verify the console still compiles**

Run: `mvn compile -pl domains/insects/insects-console`
Expected: compilation succeeds.

- [ ] **Step 4: Add the `LinealRank` import if needed**

Ensure the controller has `import com.naturalist.taxonomy.LinealRank;` — it likely does not yet.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "refactor(insects-console): use domain helpers for observation mutations"
```