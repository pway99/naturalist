# Insect Add-Photo Command

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the controller's `addImage()` coordination logic (two linked inserts: optional `FieldObservation` + `InsectImage`) into a reusable command in `insects-core`, so both desktop and mobile consoles can add photos without duplicating the entity-construction and FK-linking logic.

**Architecture:** A single `InsectAddPhotoCommand` class in `insects-core` takes `InsectCommand` and provides an `addPhoto(...)` method that creates the optional `FieldObservation` (when a naturalist is signed in) and the `InsectImage` with a linked `observationId`. No Aggregate or Transaction — the two inserts are simple enough that entity-level Observer validation is sufficient. The controller simplifies to parsing request parameters and delegating to the command.

**Tech Stack:** Java 21, JUnit 5, AssertJ, `NaturalistDatabaseExtension`

## Global Constraints

- Tests run via `mvn verify` from repo root (user runs builds locally — do not invoke `mvn`).
- `InsectAddPhotoCommand` is `public class` in `com.naturalist.insects` (used by the console controller in `com.naturalist.insects.console`).
- Constructor takes `InsectCommand` only — no query dependencies needed.
- Not exposed from `InsectsTestContext` or `InsectsTestContextInternal` — tests and controller construct it directly from `insectCommand`, same pattern as `InsectIdentificationCommand`.

## No dependencies on other plans

This plan is independent of the hierarchical image query plan.

---

### Task 1: Create `InsectAddPhotoCommand` with tests

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAddPhotoCommand.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectAddPhotoCommandTest.java`

**Interfaces:**
- Consumes: `InsectCommand.images().insert(InsectImage)`, `InsectCommand.fieldObservations().insert(FieldObservation)`
- Produces: `InsectAddPhotoCommand.addPhoto(InsectSpeciesName, FileName, @Nullable NaturalistName, @Nullable String notes, @Nullable String location)` → `void`

- [ ] **Step 1: Read the controller's `addImage()` method**

Read `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` lines 663–690 to confirm the exact entity construction and insertion logic being extracted.

- [ ] **Step 2: Write the failing test**

```java
package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class InsectAddPhotoCommandTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    InsectsTestContextInternal context = InsectsTestContextInternal.create(db);
    InsectAddPhotoCommand command = new InsectAddPhotoCommand(context.insectCommand());
    InsectQuery query = context.insectQuery();

    @Test
    void addPhoto_withNaturalist_insertsImageAndObservation() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-001.jpg");
        var naturalist = NaturalistName.of("test-naturalist");

        command.addPhoto(species, fileName, naturalist, "Found on pipevine", "Garden");

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().observationId()).isNotNull();

        var observation = query.fieldObservations()
                .getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().observedBy()).isEqualTo(naturalist);
        assertThat(observation.get().subject()).isEqualTo(species);
        assertThat(observation.get().notes()).isEqualTo("Found on pipevine");
        assertThat(observation.get().location()).isEqualTo("Garden");
        assertThat(observation.get().identification()).isNull();
    }

    @Test
    void addPhoto_withoutNaturalist_insertsImageOnly() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-002.jpg");

        command.addPhoto(species, fileName, null, null, null);

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().observationId()).isNull();
    }

    @Test
    void addPhoto_withNaturalist_blankNotesAndLocation_persistsAsNull() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var fileName = FileName.of("test-photo-003.jpg");
        var naturalist = NaturalistName.of("test-naturalist");

        command.addPhoto(species, fileName, naturalist, "  ", "");

        var images = query.images().forParentName(species).stream().toList();
        var photo = images.stream()
                .filter(img -> img.resourceName().equals(fileName))
                .findFirst();
        assertThat(photo).isPresent();

        var observation = query.fieldObservations()
                .getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().notes()).isNull();
        assertThat(observation.get().location()).isNull();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectAddPhotoCommandTest`
Expected: compilation failure — `InsectAddPhotoCommand` class does not exist.

- [ ] **Step 4: Create `InsectAddPhotoCommand`**

```java
package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Adds a photograph to an existing catalog species, optionally recording
 * a {@link FieldObservation} when a naturalist is signed in. The image's
 * {@code observationId} links to the observation when present; it is
 * {@code null} for shared catalog images with no owning naturalist.
 */
public class InsectAddPhotoCommand {

    private final Observer observer = Observer.forClass(InsectAddPhotoCommand.class);
    private final InsectCommand insectCommand;

    public InsectAddPhotoCommand(InsectCommand insectCommand) {
        this.insectCommand = insectCommand;
    }

    public void addPhoto(InsectSpeciesName species, FileName storedFileName,
                         @Nullable NaturalistName naturalist,
                         @Nullable String notes, @Nullable String location) {
        observer.arguments("addPhoto", i -> i
                        .identifier(species, "species")
                        .namedValue(storedFileName, "storedFileName"))
                .throwWhenInvalid();

        FieldObservationId observationId = null;
        if (naturalist != null) {
            observationId = FieldObservationId.create();
            var observation = new FieldObservation(
                    observationId, naturalist, species, Instant.now(),
                    (notes == null || notes.isBlank()) ? null : notes,
                    (location == null || location.isBlank()) ? null : location,
                    null);
            insectCommand.fieldObservations().insert(observation);
        }

        var image = new InsectImage(
                InsectImageId.create(), species, Instant.now(),
                storedFileName, observationId);
        insectCommand.images().insert(image);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectAddPhotoCommandTest`
Expected: all three tests PASS.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAddPhotoCommand.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectAddPhotoCommandTest.java
git commit -m "feat(insects-core): extract addPhoto coordination into InsectAddPhotoCommand"
```

---

### Task 2: Simplify controller `addImage()` method

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Consumes: `InsectAddPhotoCommand.addPhoto(InsectSpeciesName, FileName, @Nullable NaturalistName, @Nullable String notes, @Nullable String location)` → `void`

- [ ] **Step 1: Read the controller constructor and `addImage()` method**

Read `InsectsController.java` lines 82–105 (fields + constructor) and lines 663–690 (`addImage`).

- [ ] **Step 2: Add field and constructor wiring**

Add the field alongside the existing `identificationCommand` field (line 89):

```java
private final InsectAddPhotoCommand addPhotoCommand;
```

In the constructor, after line 104 (`this.identificationCommand = ...`), add:

```java
this.addPhotoCommand = new InsectAddPhotoCommand(this.insectCommand);
```

- [ ] **Step 3: Replace `addImage()` body**

Replace the current `addImage()` method (lines 663–690) with:

```java
@PostMapping("/{name}/images")
String addImage(@PathVariable String name,
                @RequestParam("image") MultipartFile imageFile,
                @RequestParam(name = "location", required = false) String location,
                @RequestParam(name = "notes", required = false) String notes,
                HttpServletRequest request) throws IOException {
    var species = InsectSpeciesName.of(name);
    var me = currentNaturalist(request);
    var storedFileName = imageStorageService.store(imageFile.getBytes());
    addPhotoCommand.addPhoto(species, storedFileName,
            me.orElse(null), notes, location);
    return "redirect:/insects/" + name;
}
```

Note: `currentNaturalist(request)` returns `Optional<NaturalistName>` — use `orElse(null)` to pass the nullable value the command expects.

- [ ] **Step 4: Verify compilation**

Run: `mvn compile -pl domains/insects/insects-console`
Expected: compilation succeeds.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "refactor(insects-console): use InsectAddPhotoCommand for addImage endpoint"
```

---

## Verification

- `mvn verify` from repo root — all existing tests pass
- `InsectAddPhotoCommandTest` — three cases: with naturalist (image + observation linked), without naturalist (image only, null observationId), blank notes/location persisted as null
- Controller compilation — new field and simplified method compile cleanly
