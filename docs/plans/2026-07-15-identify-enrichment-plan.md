# Insect Identification Enrichment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the insect identification flow with rank-polymorphic identification, authority-validated citations, structured feature persistence, and grounded Durrell descriptions for parent ranks.

**Architecture:** The `InsectIdentificationCommand` in `insects-core` orchestrates: vision identification → authority validation (with rank fallback) → parent rank enrichment (description + features via text generation grounded in authority content) → citation creation (via library domain) → atomic transaction. All external calls (vision, authority, text generation) complete before the transaction boundary. The controller stays thin — one call, one redirect.

**Tech Stack:** Java 21 records, sealed interfaces, Anthropic SDK (text generation adapter), existing `kernels/vision` vocabulary (`ToolSchema`, `ToolResult`), `kernels/authority` (`ExternalAuthority`, `Citation`), `library-api` (citation persistence).

**Spec:** [`docs/plans/2026-07-15-identify-enrichment-design.md`](2026-07-15-identify-enrichment-design.md)

## Global Constraints

- No `mvn` invocations — the user runs builds locally.
- `EntityName` subclasses live in `domains/identifiers/` for cross-domain cycle breaking; kernel-level ones stay alongside their entity.
- `EntityId` uses UUIDv7 only — `UUID.randomUUID()` is forbidden.
- All records implement `Observable` with `invariants()`. All `Entity`/`NamedEntity` records use `EntityCommand` (via `AbstractEntityCommand`) for writes.
- Kernel testing convention: no direct kernel tests unless no consumer exists yet.
- Plans go in `docs/plans/`, not `docs/superpowers/plans/`.
- Don't invoke `mvn`. Print git commands; commit only on explicit request.
- `insects` is the reference domain for patterns.
- Record arity changes ripple repo-wide — grep the whole repo when changing a record's constructor.

## Dependency Graph

```
Task 1 (TextGenerationService kernel)  ──┐
Task 2 (Authority kernel expansion)  ────┤
Task 3 (Library command surface)  ───────┤
Task 4 (InsectCommand features)  ────────┤
                                         ├──► Task 5 (Rank-polymorphic aggregate)
                                         │        │
                                         │        ▼
                                         ├──► Task 6 (Vision schema + parsing)
                                         │        │
                                         │        ▼
                                         ├──► Task 7 (Transaction widening)
                                         │        │
                                         │        ▼
                                         └──► Task 8 (Orchestration + controller)
                                                  │
                                                  ▼
                                              Task 9 (Anthropic text-generation adapter)
```

Tasks 1–4 are independent and can be worked in parallel. Tasks 5–8 are sequential. Task 9 depends only on Task 1.

---

### Task 1: TextGenerationService Kernel Port

**Files:**
- Create: `kernels/text-generation/pom.xml`
- Create: `kernels/text-generation/src/main/java/com/naturalist/textgeneration/TextGenerationService.java`
- Create: `kernels/text-generation/src/main/java/com/naturalist/textgeneration/NoOpTextGenerationService.java`
- Modify: `kernels/pom.xml` — add `<module>text-generation</module>`
- Modify: `pom.xml` (root) — add dependency-management entry

**Interfaces:**
- Consumes: `ToolSchema` and `ToolResult` from `kernels/vision` (`com.naturalist.vision`)
- Produces: `TextGenerationService` interface with `ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt)`, consumed by Task 8

- [ ] **Step 1: Create `kernels/text-generation/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>kernels</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>text-generation</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>vision</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create `TextGenerationService.java`**

```java
package com.naturalist.textgeneration;

import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

/**
 * Text-only generation port — symmetric with {@link com.naturalist.vision.VisionService}
 * but without an image. Used for generating structured content (Durrell descriptions,
 * diagnostic features) from textual source material.
 */
public interface TextGenerationService {

    ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt);
}
```

- [ ] **Step 3: Create `NoOpTextGenerationService.java`**

```java
package com.naturalist.textgeneration;

import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

/**
 * Default no-op implementation for unit tests and unwired composition roots.
 */
public class NoOpTextGenerationService implements TextGenerationService {

    @Override
    public ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException(
                "TextGenerationService is not wired — configure an adapter.");
    }
}
```

- [ ] **Step 4: Add module to `kernels/pom.xml`**

Add `<module>text-generation</module>` in alphabetical order within the `<modules>` block (after `taxonomy`, before `vision`).

- [ ] **Step 5: Add dependency-management entry to root `pom.xml`**

In the KERNELS section of `<dependencyManagement>`, add alphabetically (after `taxonomy`, before `vision`):

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>text-generation</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 6: Commit**

```bash
git add kernels/text-generation/ kernels/pom.xml pom.xml
git commit -m "feat(kernel): add TextGenerationService port for text-only AI generation"
```

---

### Task 2: Authority Kernel Expansion — AuthorityContent + fetchContent

**Files:**
- Create: `kernels/authority/src/main/java/com/naturalist/authority/AuthorityContent.java`
- Modify: `kernels/authority/src/main/java/com/naturalist/authority/ExternalAuthority.java`
- Modify: `external-authorities/eol/eol-client-mock/src/main/java/com/naturalist/authority/eol/EolClientMock.java`

**Interfaces:**
- Consumes: `AuthorityReference` (existing), `ValueObject` (existing)
- Produces: `AuthorityContent` value object, `ExternalAuthority.fetchContent(AuthorityReference)` method — consumed by Task 8

- [ ] **Step 1: Create `AuthorityContent.java`**

```java
package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Textual content retrieved from an external authority's page for a given
 * reference. Used as grounded source material for Durrell description
 * generation — the {@link com.naturalist.textgeneration.TextGenerationService}
 * reshapes this content into four levels rather than generating from
 * training data.
 */
public record AuthorityContent(
        AuthorityReference reference,
        String content
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(reference, "reference")
                .notBlank(content, "content");
    }
}
```

- [ ] **Step 2: Add `fetchContent` to `ExternalAuthority.java`**

Add after the existing `lookup` method at line 25 of `kernels/authority/src/main/java/com/naturalist/authority/ExternalAuthority.java`:

```java
    /**
     * Retrieves the textual content from the authority's page at the given
     * reference. Used to ground description generation in authoritative
     * source material rather than AI training data.
     *
     * <p>Returns {@link AuthorityContent} with the page's textual summary.
     * Network-backed implementations MUST be Resilience-wrapped per ADR-026.
     */
    AuthorityContent fetchContent(AuthorityReference ref);
```

- [ ] **Step 3: Update `EolClientMock` to implement `fetchContent`**

In `external-authorities/eol/eol-client-mock/src/main/java/com/naturalist/authority/eol/EolClientMock.java`, add after the `lookup` method:

```java
    @Override
    public AuthorityContent fetchContent(AuthorityReference ref) {
        return new AuthorityContent(ref,
                "Stub authority content for " + ref.url()
                + ". This is placeholder text from the EOL mock client.");
    }
```

- [ ] **Step 4: Verify any other `ExternalAuthority` implementations compile**

Search for other classes implementing `ExternalAuthority`:

```bash
grep -rn "implements ExternalAuthority" --include="*.java" .
```

Each must implement `fetchContent`. If there are other implementations beyond `EolClientMock`, add the same stub pattern.

- [ ] **Step 5: Commit**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/AuthorityContent.java \
       kernels/authority/src/main/java/com/naturalist/authority/ExternalAuthority.java \
       external-authorities/eol/eol-client-mock/src/main/java/com/naturalist/authority/eol/EolClientMock.java
git commit -m "feat(authority): add AuthorityContent and fetchContent to ExternalAuthority"
```

---

### Task 3: Library Domain Command Surface

**Files:**
- Create: `domains/library/library-api/src/main/java/com/naturalist/library/LibraryCommand.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/LibraryCommandImpl.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CitationCommandImpl.java`
- Create: `domains/library/library-core/src/main/java/com/naturalist/library/CitationAssociationCommandImpl.java`
- Modify: `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`

**Interfaces:**
- Consumes: `EntityCommand<CitationName, Citation>`, `EntityCommand<CitationAssociationId, CitationAssociation>` from framework; `CitationRepository`, `CitationAssociationRepository` (package-private, accessed via split-package)
- Produces: `LibraryCommand` with `CitationCommand citations()` and `CitationAssociationCommand citationAssociations()` — consumed by Task 8 via `InsectIdentificationCommand`

- [ ] **Step 1: Create `LibraryCommand.java` in `library-api`**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.EntityCommand;

/**
 * Namespace command for the library bounded context — the single discoverable
 * entry point for mutating library data. Symmetric write-side analogue of
 * {@link CitationQuery} and {@link CitationAssociationQuery}.
 */
public interface LibraryCommand {

    CitationCommand citations();

    CitationAssociationCommand citationAssociations();

    interface CitationCommand extends EntityCommand<CitationName, Citation> {
    }

    interface CitationAssociationCommand
            extends EntityCommand<CitationAssociationId, CitationAssociation> {
    }
}
```

- [ ] **Step 2: Create `CitationCommandImpl.java` in `library-core`**

```java
package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.data.AbstractEntityCommand;

class CitationCommandImpl
        extends AbstractEntityCommand<CitationName, Citation, CitationRepository>
        implements LibraryCommand.CitationCommand {

    CitationCommandImpl(CitationRepository repository) {
        super(repository);
    }
}
```

- [ ] **Step 3: Create `CitationAssociationCommandImpl.java` in `library-core`**

```java
package com.naturalist.library;

import com.naturalist.data.AbstractEntityCommand;

class CitationAssociationCommandImpl
        extends AbstractEntityCommand<CitationAssociationId, CitationAssociation,
                CitationAssociationRepository>
        implements LibraryCommand.CitationAssociationCommand {

    CitationAssociationCommandImpl(CitationAssociationRepository repository) {
        super(repository);
    }
}
```

- [ ] **Step 4: Create `LibraryCommandImpl.java` in `library-core`**

```java
package com.naturalist.library;

import com.naturalist.observability.Observer;

class LibraryCommandImpl implements LibraryCommand {

    private final CitationCommand citationCommand;
    private final CitationAssociationCommand citationAssociationCommand;

    LibraryCommandImpl(CitationCommand citationCommand,
                       CitationAssociationCommand citationAssociationCommand) {
        var observer = Observer.forClass(LibraryCommandImpl.class);
        observer.arguments("constructor", i -> i
                .notNull(citationCommand, "citationCommand")
                .notNull(citationAssociationCommand, "citationAssociationCommand")
        ).throwWhenInvalid();
        this.citationCommand = citationCommand;
        this.citationAssociationCommand = citationAssociationCommand;
    }

    @Override
    public CitationCommand citations() {
        return citationCommand;
    }

    @Override
    public CitationAssociationCommand citationAssociations() {
        return citationAssociationCommand;
    }
}
```

- [ ] **Step 5: Wire into `LibraryTestContext`**

Modify `domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java`:

Add a `libraryCommand` field and wire it in the constructor after the existing repository creation (lines 22-29). Add a public accessor.

In the constructor, after line 29 (`new CitationAssociationRepositoryMock(db)`), add:

```java
        LibraryCommand.CitationCommand citationCommand =
                new CitationCommandImpl(citationRepository);
        LibraryCommand.CitationAssociationCommand citationAssociationCommand =
                new CitationAssociationCommandImpl(citationAssociationRepository);
        this.libraryCommand = new LibraryCommandImpl(citationCommand, citationAssociationCommand);
```

Add the field:

```java
    private final LibraryCommand libraryCommand;
```

Add the accessor:

```java
    public LibraryCommand libraryCommand() {
        return libraryCommand;
    }
```

- [ ] **Step 6: Check `library-core` has `framework` dependency for `AbstractEntityCommand`**

Read `domains/library/library-core/pom.xml` and verify it includes `framework` as a dependency. If not, add:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>framework</artifactId>
</dependency>
```

- [ ] **Step 7: Commit**

```bash
git add domains/library/library-api/src/main/java/com/naturalist/library/LibraryCommand.java \
       domains/library/library-core/src/main/java/com/naturalist/library/LibraryCommandImpl.java \
       domains/library/library-core/src/main/java/com/naturalist/library/CitationCommandImpl.java \
       domains/library/library-core/src/main/java/com/naturalist/library/CitationAssociationCommandImpl.java \
       domains/library/library-test-context/src/main/java/com/naturalist/library/LibraryTestContext.java
git commit -m "feat(library): add LibraryCommand write surface for citations"
```

---

### Task 4: InsectCommand Feature Write Surfaces

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/FeatureCommandImpl.java`
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/FeatureAssignmentCommandImpl.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCommand.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCommandImpl.java`
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java`

**Interfaces:**
- Consumes: `EntityCommand` from framework; `InsectRepository.FeatureRepository`, `InsectRepository.FeatureAssignmentRepository` (package-private, accessed via split-package in core)
- Produces: `InsectCommand.FeatureCommand` and `InsectCommand.FeatureAssignmentCommand` — consumed by Task 7 (transaction)

- [ ] **Step 1: Add interfaces to `InsectCommand.java`**

In `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCommand.java`, add the nested interface definitions after line 77 (`interface GenusCommand`):

```java
    /**
     * Entity-level command surface for {@link InsectFeature}.
     */
    interface FeatureCommand extends EntityCommand<InsectFeatureId, InsectFeature> {
    }

    /**
     * Entity-level command surface for {@link InsectFeatureAssignment}.
     */
    interface FeatureAssignmentCommand
            extends EntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment> {
    }
```

And add the accessor method declarations to the `InsectCommand` interface body (alongside the existing `species()`, `images()`, etc.):

```java
    FeatureCommand features();

    FeatureAssignmentCommand featureAssignments();
```

- [ ] **Step 2: Create `FeatureCommandImpl.java`**

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FeatureCommandImpl
        extends AbstractEntityCommand<InsectFeatureId, InsectFeature,
                InsectRepository.FeatureRepository>
        implements InsectCommand.FeatureCommand {

    FeatureCommandImpl(InsectRepository.FeatureRepository repository) {
        super(repository);
    }
}
```

- [ ] **Step 3: Create `FeatureAssignmentCommandImpl.java`**

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityCommand;

class FeatureAssignmentCommandImpl
        extends AbstractEntityCommand<InsectFeatureAssignmentId, InsectFeatureAssignment,
                InsectRepository.FeatureAssignmentRepository>
        implements InsectCommand.FeatureAssignmentCommand {

    FeatureAssignmentCommandImpl(InsectRepository.FeatureAssignmentRepository repository) {
        super(repository);
    }
}
```

- [ ] **Step 4: Wire into `InsectCommandImpl`**

Modify `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCommandImpl.java`:

Add two new fields:

```java
    private final FeatureCommand featureCommand;
    private final FeatureAssignmentCommand featureAssignmentCommand;
```

Update the constructor to accept and validate them. Add two new accessor methods:

```java
    @Override
    public FeatureCommand features() {
        return featureCommand;
    }

    @Override
    public FeatureAssignmentCommand featureAssignments() {
        return featureAssignmentCommand;
    }
```

The constructor observer validation adds:

```java
                .notNull(featureCommand, "featureCommand")
                .notNull(featureAssignmentCommand, "featureAssignmentCommand")
```

- [ ] **Step 5: Wire into `InsectsTestContext`**

Modify `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`.

After the existing command construction (line 77, `GenusCommand`), add:

```java
        InsectCommand.FeatureCommand featureCommand =
                new FeatureCommandImpl(repository.featureRepository);
        InsectCommand.FeatureAssignmentCommand featureAssignmentCommand =
                new FeatureAssignmentCommandImpl(repository.featureAssignmentRepository);
```

Update the `InsectCommandImpl` constructor call to include the new commands.

- [ ] **Step 6: Wire into `InsectsTestContextInternal`**

Same pattern as Step 5 in `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java`.

- [ ] **Step 7: Grep for all `new InsectCommandImpl(` call sites**

```bash
grep -rn "new InsectCommandImpl(" --include="*.java" .
```

Update every call site to pass the two new command parameters. This will include the test contexts (already handled) and any console bootstrap wiring (e.g., in `apps/management-console/`).

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCommand.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/FeatureCommandImpl.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/FeatureAssignmentCommandImpl.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCommandImpl.java \
       domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java
# Include any other files found by the grep in Step 7
git commit -m "feat(insects): add FeatureCommand and FeatureAssignmentCommand write surfaces"
```

---

### Task 5: Rank-Polymorphic CatalogIdentification Aggregate

**Files:**
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/IdentifiedRankEntity.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/CatalogIdentification.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCatalogIdentificationTransactionTest.java` (fixture updates only — transaction tests come in Task 7)

**Interfaces:**
- Consumes: `InsectSpecies`, `InsectGenus`, `InsectFamily`, `InsectOrder`, `InsectRankName` (existing), `InsectFeature`, `InsectFeatureAssignment` (existing)
- Produces: `IdentifiedRankEntity` sealed interface (4 permits), widened `CatalogIdentification` aggregate, widened `InsectIdentificationResult` — consumed by Tasks 6, 7, 8

- [ ] **Step 1: Create `IdentifiedRankEntity.java`**

```java
package com.naturalist.insects;

/**
 * The rank entity that was identified from a photograph — a sealed sum type
 * carrying whichever Linnaean rank the vision service and authority validation
 * confirmed. The transaction uses pattern matching to dispatch to the
 * appropriate command for persistence.
 *
 * <p>Each permit wraps its rank entity and exposes the polymorphic
 * {@link InsectRankName} for FK validation in the {@link CatalogIdentification}
 * aggregate's invariants.
 */
public sealed interface IdentifiedRankEntity {

    /** The polymorphic rank name for FK validation. */
    InsectRankName rankName();

    record Species(InsectSpecies species) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return species == null ? null : species.name();
        }
    }

    record Genus(InsectGenus genus) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return genus == null ? null : genus.name();
        }
    }

    record Family(InsectFamily family) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return family == null ? null : family.name();
        }
    }

    record Order(InsectOrder order) implements IdentifiedRankEntity {
        @Override
        public InsectRankName rankName() {
            return order == null ? null : order.name();
        }
    }
}
```

- [ ] **Step 2: Widen `CatalogIdentification.java`**

Replace the entire file `domains/insects/insects-api/src/main/java/com/naturalist/insects/CatalogIdentification.java`:

```java
package com.naturalist.insects;

import com.naturalist.ddd.Aggregate;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Write-side consistency boundary for insect catalog identification — the
 * aggregate that a {@link com.naturalist.data.Transaction} persists
 * atomically when a naturalist identifies an insect from a photograph.
 *
 * <p>Carries the identified rank entity (polymorphic — species, genus,
 * family, or order), taxonomy, image, observation, structured features,
 * and pre-resolved parent rank descriptions. Cross-entity invariants
 * enforce FK consistency: the image and observation must reference the
 * identified rank.
 */
public record CatalogIdentification(
        IdentifiedRankEntity identifiedEntity,
        TaxonomicClassification taxonomy,
        InsectImage image,
        FieldObservation observation,
        List<InsectFeature> newFeatures,
        List<InsectFeatureAssignment> featureAssignments,
        Map<InsectRankName, Description> parentDescriptions
) implements Aggregate {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(identifiedEntity, "identifiedEntity")
                .valueObject(taxonomy, "taxonomy")
                .namedEntity(image, "image")
                .namedEntity(observation, "observation")
                .notNull(newFeatures, "newFeatures")
                .notNull(featureAssignments, "featureAssignments")
                .notNull(parentDescriptions, "parentDescriptions")
                .isTrue(imageParentMatchesIdentifiedRank(),
                        "imageParentMatchesIdentifiedRank")
                .isTrue(observationSubjectMatchesIdentifiedRank(),
                        "observationSubjectMatchesIdentifiedRank")
                .isTrue(imageObservationIdMatchesObservation(),
                        "imageObservationIdMatchesObservation");
    }

    private boolean imageParentMatchesIdentifiedRank() {
        return identifiedEntity == null || image == null
                || image.parentName().equals(identifiedEntity.rankName());
    }

    private boolean observationSubjectMatchesIdentifiedRank() {
        return identifiedEntity == null || observation == null
                || observation.subject().equals(identifiedEntity.rankName());
    }

    private boolean imageObservationIdMatchesObservation() {
        return image == null || observation == null
                || Objects.equals(image.observationId(), observation.id());
    }
}
```

- [ ] **Step 3: Widen `InsectIdentificationResult.java`**

Replace the record in `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java`:

```java
package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicClassification;

import java.util.List;

/**
 * Domain-typed result of vision identification — carries the identified rank
 * entity (polymorphic), taxonomy, identification metadata, and structured
 * features. Ready to feed into authority validation and catalog persistence.
 */
record InsectIdentificationResult(
        IdentifiedRankEntity identifiedEntity,
        TaxonomicClassification taxonomy,
        Identification identification,
        List<String> features
) {}
```

- [ ] **Step 4: Update `InsectCatalogIdentificationTransactionTest` fixtures**

The test fixtures at lines 127-166 of `InsectCatalogIdentificationTransactionTest.java` use the old `CatalogIdentification` constructor. Update the `catalogIdentification()` factory method:

```java
    private static CatalogIdentification catalogIdentification() {
        return new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()),
                taxonomy(), image(), observation(),
                List.of(), List.of(), Map.of());
    }
```

Update the second-identification fixture in `executeIsIdempotentForExistingSpecies` (line 69):

```java
        var second = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()),
                taxonomy(), secondImage, secondObs,
                List.of(), List.of(), Map.of());
```

Update the `executeSkipsExistingParentRanks` fixture (line 106):

```java
        var second = new CatalogIdentification(
                new IdentifiedRankEntity.Species(secondSpecies),
                taxonomy(), secondImage, secondObs,
                List.of(), List.of(), Map.of());
```

Add imports:

```java
import java.util.List;
import java.util.Map;
```

- [ ] **Step 5: Grep for all `new CatalogIdentification(` call sites**

```bash
grep -rn "new CatalogIdentification(" --include="*.java" .
```

Update every call site to match the new constructor signature. This includes `InsectIdentificationCommand.java` (updated fully in Task 6).

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/IdentifiedRankEntity.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/CatalogIdentification.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCatalogIdentificationTransactionTest.java
# Include any other files found by the grep in Step 5
git commit -m "feat(insects): generalize CatalogIdentification to rank-polymorphic aggregate"
```

---

### Task 6: Vision Tool Schema + Rank-Polymorphic Parsing

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommand.java` — `buildToolSchema()`, `buildSystemPrompt()`, `parseResult()` methods only (the `identify()` orchestration is Task 8)

**Interfaces:**
- Consumes: `IdentifiedRankEntity` (Task 5), `InsectIdentificationResult` (Task 5), existing rank entity constructors
- Produces: Updated `buildToolSchema()`, `buildSystemPrompt()`, `parseResult()` — consumed by Task 8

- [ ] **Step 1: Update `buildToolSchema()` — add `identifiedRank` and `features`, make genus/species conditional**

Replace the `buildToolSchema()` method (lines 104-135) in `InsectIdentificationCommand.java`:

```java
    private ToolSchema buildToolSchema() {
        var schema = """
                {
                  "type": "object",
                  "required": ["name", "identifiedRank", "order", "family", "commonName",
                               "descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity",
                               "guilds", "beneficial", "confidence", "evidence", "features"],
                  "properties": {
                    "name":                    { "type": "string", "description": "Kebab-case slug for the identified rank, e.g. battus-philenor for species, syrphidae for family" },
                    "identifiedRank":          { "type": "string", "enum": ["ORDER", "FAMILY", "GENUS", "SPECIES"], "description": "The most specific Linnaean rank you can confidently identify" },
                    "order":                   { "type": "string", "description": "Taxonomic order, e.g. Lepidoptera" },
                    "family":                  { "type": "string", "description": "Taxonomic family, e.g. Papilionidae. Required for FAMILY, GENUS, and SPECIES ranks." },
                    "genus":                   { "type": ["string", "null"], "description": "Taxonomic genus, e.g. Battus. Required for GENUS and SPECIES ranks, null otherwise." },
                    "species":                 { "type": ["string", "null"], "description": "Species epithet, e.g. philenor. Required for SPECIES rank, null otherwise." },
                    "commonName":              { "type": "string", "description": "Most widely used common name for the identified rank" },
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description (simple, sensory, wonder-focused)" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description (observable features, life cycle basics)" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description (ecology, adaptations, relationships)" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description (taxonomy, research context, conservation)" },
                    "guilds":                  { "type": "array", "items": { "type": "string", "enum": ["PARASITOID","PREDATOR","APEX_PREDATOR","POLLINATOR","DECOMPOSER","FOOD_WEB","MIGRATORY","KEYSTONE"] }, "description": "Functional ecological guilds" },
                    "beneficial":              { "type": "boolean", "description": "Whether this insect is beneficial in a garden/agricultural context" },
                    "features":                { "type": "array", "items": { "type": "string" }, "description": "Morphological field marks observed, ordered conspicuous to diagnostic: wing shape, coloration, antennae type, mouthparts, body segmentation, etc." },
                    "sightingNotes":           { "type": ["string", "null"], "description": "Notable observations about this sighting" },
                    "confidence":              { "type": "number", "minimum": 0, "maximum": 1, "description": "Confidence in identification (0.0-1.0)" },
                    "evidence":                { "type": "string", "description": "Which visible features support this identification" },
                    "alternatives":            { "type": ["string", "null"], "description": "JSON array of alternative candidates with name and confidence, or null if highly confident" }
                  }
                }
                """;
        return new ToolSchema(TOOL_NAME,
                "Propose an insect identification based on the provided photograph.",
                schema);
    }
```

- [ ] **Step 2: Update `buildSystemPrompt()` — instruct rank-level identification**

Replace the `buildSystemPrompt()` method (lines 137-163):

```java
    private String buildSystemPrompt(String location) {
        var prompt = """
                You are an expert entomologist assisting a naturalist in identifying insects
                from photographs. For each identification:

                1. Examine the photograph carefully, noting morphological features (wing
                   venation, body shape, coloration, antennae, leg structure).
                2. Consider the geographic location if provided — use it to narrow range maps
                   and eliminate look-alike species from other regions.
                3. Identify to the MOST SPECIFIC Linnaean rank your confidence supports:
                   - SPECIES: you can confidently name the species (e.g. Battus philenor)
                   - GENUS: you can identify the genus but not the species
                   - FAMILY: you can identify the family but not the genus
                   - ORDER: you can only identify the order
                   Set identifiedRank accordingly. Only provide genus/species fields when
                   your identifiedRank includes them. Do NOT guess a species if you are
                   not confident — identify at family or order level instead.
                4. Provide four Durrell-level descriptions for the identified rank:
                   - Preschool: simple, sensory, wonder-focused (what a 4-year-old would notice)
                   - Elementary: observable features, life cycle basics (what a 10-year-old learns)
                   - Secondary: ecology, adaptations, relationships (high school biology level)
                   - University: taxonomy, research context, conservation status (expert level)
                5. List the morphological features you observed in the photo, ordered from
                   most conspicuous to most diagnostic. These should be specific, normalised
                   field marks (e.g. "halteres", "clubbed antennae", "elytra").
                6. Assess your confidence honestly. Below 0.7, name the specific features you
                   cannot confirm from the photo.
                7. List alternative candidates if confidence is below 0.9.
                8. Assign functional ecological guilds from the allowed list.

                Generate the kebab-case slug name from the identified rank's name
                (e.g. battus-philenor for a species, syrphidae for a family).
                Use the propose_insect_species tool to return your identification.
                """;
        if (location != null && !location.isBlank()) {
            prompt += "\nLocation context: " + location;
        }
        return prompt;
    }
```

- [ ] **Step 3: Update `parseResult()` for rank-polymorphic output**

Replace the `parseResult()` method (lines 165-209):

```java
    private InsectIdentificationResult parseResult(ToolResult result) {
        try {
            var node = MAPPER.readTree(result.argumentsJson());
            var identifiedRank = node.get("identifiedRank").asText();
            var slug = node.get("name").asText();
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var commonName = node.get("commonName").asText();
            var sightingNotes = node.hasNonNull("sightingNotes")
                    ? node.get("sightingNotes").asText() : null;

            var taxonomicOrder = TaxonomicOrder.of(node.get("order").asText());
            var taxonomicFamily = node.hasNonNull("family")
                    ? TaxonomicFamily.of(node.get("family").asText()) : null;
            var taxonomicGenus = node.hasNonNull("genus")
                    ? TaxonomicGenus.of(node.get("genus").asText()) : null;
            var taxonomicSpecies = node.hasNonNull("species")
                    ? TaxonomicSpecies.of(node.get("species").asText()) : null;
            var taxonomy = new TaxonomicClassification(
                    taxonomicOrder, taxonomicFamily, taxonomicGenus, taxonomicSpecies);

            var identifiedEntity = buildIdentifiedEntity(
                    identifiedRank, slug, description, commonName, sightingNotes, taxonomy);

            var features = new java.util.ArrayList<String>();
            if (node.hasNonNull("features") && node.get("features").isArray()) {
                for (var f : node.get("features")) {
                    if (f.isTextual() && !f.asText().isBlank()) {
                        features.add(f.asText());
                    }
                }
            }

            var confidence = node.get("confidence").asDouble();
            var evidence = node.get("evidence").asText();
            var identification = new Identification(confidence, evidence, parseAlternatives(node));

            return new InsectIdentificationResult(identifiedEntity, taxonomy, identification,
                    List.copyOf(features));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vision identification result", e);
        }
    }

    private IdentifiedRankEntity buildIdentifiedEntity(
            String rank, String slug, Description description, String commonName,
            @Nullable String sightingNotes, TaxonomicClassification taxonomy) {
        return switch (rank) {
            case "SPECIES" -> {
                var speciesName = InsectSpeciesName.of(slug);
                var genusName = InsectGenusName.of(
                        taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Species(new InsectSpecies(
                        speciesName, genusName, taxonomy.species(), description,
                        Set.of(CommonName.of(commonName)), sightingNotes,
                        null, null, null, null, null, null, null, null));
            }
            case "GENUS" -> {
                var genusName = InsectGenusName.of(slug);
                var familyName = InsectFamilyName.of(
                        taxonomy.family().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Genus(new InsectGenus(
                        genusName, familyName, taxonomy.genus(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            case "FAMILY" -> {
                var familyName = InsectFamilyName.of(slug);
                var orderName = InsectOrderName.of(
                        taxonomy.order().value().toLowerCase(java.util.Locale.ROOT));
                yield new IdentifiedRankEntity.Family(new InsectFamily(
                        familyName, orderName, taxonomy.family(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            case "ORDER" -> {
                var orderName = InsectOrderName.of(slug);
                yield new IdentifiedRankEntity.Order(new InsectOrder(
                        orderName, taxonomy.order(), description,
                        Set.of(CommonName.of(commonName)), null));
            }
            default -> throw new IllegalArgumentException(
                    "Unknown identified rank: " + rank);
        };
    }
```

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommand.java
git commit -m "feat(insects): update vision schema and parsing for rank-polymorphic identification"
```

---

### Task 7: InsectCatalogIdentificationTransaction Widening

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCatalogIdentificationTransaction.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCatalogIdentificationTransactionTest.java`

**Interfaces:**
- Consumes: `IdentifiedRankEntity` (Task 5), widened `CatalogIdentification` (Task 5), `InsectCommand.FeatureCommand`, `InsectCommand.FeatureAssignmentCommand` (Task 4)
- Produces: Widened transaction that handles polymorphic rank entity, features, and pre-resolved parent descriptions — consumed by Task 8

- [ ] **Step 1: Write failing test — polymorphic rank (family-level identification)**

Add to `InsectCatalogIdentificationTransactionTest.java`:

```java
    @Test
    void executePersistsFamilyLevelIdentification() {
        var familyName = InsectFamilyName.of("syrphidae");
        var orderName = InsectOrderName.of("diptera");
        var obsId = FieldObservationId.create();
        var family = new InsectFamily(
                familyName, orderName,
                TaxonomicFamily.of("Syrphidae"), description(),
                Set.of(CommonName.of("Hover Flies")), null);
        var taxonomy = new TaxonomicClassification(
                TaxonomicOrder.of("Diptera"), TaxonomicFamily.of("Syrphidae"),
                null, null);
        var image = new InsectImage(
                InsectImageId.create(), familyName, Instant.now(),
                FileName.of("IMG_0010.jpg"), obsId);
        var observation = new FieldObservation(
                obsId, NaturalistName.of("pat"), familyName,
                Instant.now(), null, null, null);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Family(family), taxonomy,
                image, observation, List.of(), List.of(), Map.of());

        transaction.execute(id);

        assertThat(query.orders().getByName(orderName)).isPresent();
        assertThat(query.families().getByName(familyName)).isPresent();
        assertThat(query.images().forParentName(familyName).stream().toList()).hasSize(1);
        assertThat(query.fieldObservations().getByName(obsId)).isPresent();
    }
```

- [ ] **Step 2: Write failing test — features persisted**

```java
    @Test
    void executePersistsFeatures() {
        var featureId = InsectFeatureId.create();
        var feature = InsectFeature.of(featureId, "halteres");
        var assignment = InsectFeatureAssignment.of(
                InsectFeatureAssignmentId.create(), featureId,
                SPECIES_NAME, 0);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()), taxonomy(),
                image(), observation(),
                List.of(feature), List.of(assignment), Map.of());

        transaction.execute(id);

        var featureView = query.features().findByRankName(SPECIES_NAME);
        assertThat(featureView).isPresent();
        assertThat(featureView.get().features()).hasSize(1);
        assertThat(featureView.get().features().getFirst().feature().value())
                .isEqualTo("halteres");
    }
```

- [ ] **Step 3: Write failing test — parent rank uses pre-resolved description**

```java
    @Test
    void executeUsesPreResolvedDescriptionForNewParentRank() {
        var resolvedDescription = new Description(
                "Flies are everywhere!",
                "Diptera have one pair of wings and halteres.",
                "Order Diptera demonstrates remarkable ecological diversity.",
                "Diptera is one of the four megadiverse insect orders.");
        var parentDescriptions = Map.<InsectRankName, Description>of(
                InsectOrderName.of("diptera"), resolvedDescription);
        var id = new CatalogIdentification(
                new IdentifiedRankEntity.Species(species()), taxonomy(),
                image(), observation(),
                List.of(), List.of(), parentDescriptions);

        transaction.execute(id);

        var order = query.orders().getByName(InsectOrderName.of("diptera"));
        assertThat(order).isPresent();
        assertThat(order.get().description().preschool())
                .isEqualTo("Flies are everywhere!");
    }
```

- [ ] **Step 4: Update `InsectCatalogIdentificationTransaction.doExecute()`**

Replace the `doExecute` method in `InsectCatalogIdentificationTransaction.java`:

```java
    @Override
    protected void doExecute(CatalogIdentification identification) {
        var taxonomy = identification.taxonomy();
        var identifiedEntity = identification.identifiedEntity();
        var parentDescriptions = identification.parentDescriptions();

        // 1. Resolve parent ranks (order → family → genus) — only those
        //    above the identified rank
        var orderName = resolveOrder(taxonomy, parentDescriptions);
        InsectFamilyName familyName = null;
        if (taxonomy.family() != null) {
            familyName = resolveFamily(taxonomy, orderName, parentDescriptions);
        }
        if (taxonomy.genus() != null && familyName != null) {
            resolveGenus(taxonomy, familyName, parentDescriptions);
        }

        // 2. Insert identified rank entity if new
        insertIdentifiedEntity(identifiedEntity);

        // 3. Insert image and observation
        insectCommand.images().insert(identification.image());
        insectCommand.fieldObservations().insert(identification.observation());

        // 4. Insert features and assignments
        for (var feature : identification.newFeatures()) {
            insectCommand.features().insert(feature);
        }
        for (var assignment : identification.featureAssignments()) {
            insectCommand.featureAssignments().insert(assignment);
        }
    }

    private void insertIdentifiedEntity(IdentifiedRankEntity entity) {
        switch (entity) {
            case IdentifiedRankEntity.Species(var species) -> {
                if (insectQuery.species().getByName(species.name()).isEmpty()) {
                    insectCommand.species().insert(species);
                }
            }
            case IdentifiedRankEntity.Genus(var genus) -> {
                if (insectQuery.genera().getByName(genus.name()).isEmpty()) {
                    insectCommand.genera().insert(genus);
                }
            }
            case IdentifiedRankEntity.Family(var family) -> {
                if (insectQuery.families().getByName(family.name()).isEmpty()) {
                    insectCommand.families().insert(family);
                }
            }
            case IdentifiedRankEntity.Order(var order) -> {
                if (insectQuery.orders().getByName(order.name()).isEmpty()) {
                    insectCommand.orders().insert(order);
                }
            }
        }
    }

    private InsectOrderName resolveOrder(TaxonomicClassification taxonomy,
                                         Map<InsectRankName, Description> parentDescriptions) {
        var orderSlug = taxonomy.order().value().toLowerCase(java.util.Locale.ROOT);
        var orderName = InsectOrderName.of(orderSlug);
        if (insectQuery.orders().getByName(orderName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(orderName, PLACEHOLDER);
            insectCommand.orders().insert(new InsectOrder(
                    orderName, taxonomy.order(), description, Set.of(), null));
        }
        return orderName;
    }

    private InsectFamilyName resolveFamily(TaxonomicClassification taxonomy,
                                           InsectOrderName orderName,
                                           Map<InsectRankName, Description> parentDescriptions) {
        var familySlug = taxonomy.family().value().toLowerCase(java.util.Locale.ROOT);
        var familyName = InsectFamilyName.of(familySlug);
        if (insectQuery.families().getByName(familyName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(familyName, PLACEHOLDER);
            insectCommand.families().insert(new InsectFamily(
                    familyName, orderName, taxonomy.family(), description, Set.of(), null));
        }
        return familyName;
    }

    private void resolveGenus(TaxonomicClassification taxonomy,
                              InsectFamilyName familyName,
                              Map<InsectRankName, Description> parentDescriptions) {
        var genusSlug = taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT);
        var genusName = InsectGenusName.of(genusSlug);
        if (insectQuery.genera().getByName(genusName).isEmpty()) {
            var description = parentDescriptions.getOrDefault(genusName, PLACEHOLDER);
            insectCommand.genera().insert(new InsectGenus(
                    genusName, familyName, taxonomy.genus(), description, Set.of(), null));
        }
    }
```

- [ ] **Step 5: Run tests to verify all pass**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectCatalogIdentificationTransactionTest`

Expected: All tests pass — existing tests updated in Task 5, new tests pass with the widened transaction.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCatalogIdentificationTransaction.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCatalogIdentificationTransactionTest.java
git commit -m "feat(insects): widen transaction for polymorphic rank, features, and resolved descriptions"
```

---

### Task 8: InsectIdentificationCommand Orchestration + Controller

This is the integration task — wiring authority validation, parent rank enrichment, feature resolution, citation creation, and the full `identify()` orchestration. All logic stays in `insects-core`.

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommand.java` — `identify()` method + new private orchestration methods
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — return type change
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java` — expose `InsectIdentificationCommand` with new dependencies
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java` — same
- Modify: pom files as needed — `insects-core/pom.xml` needs `text-generation`, `library-api` dependencies

**Interfaces:**
- Consumes: `VisionService` (existing), `TextGenerationService` (Task 1), `ExternalAuthority` (Task 2), `LibraryCommand` (Task 3), `InsectCommand` with feature commands (Task 4), `CatalogIdentification` (Task 5), `InsectIdentificationResult` (Task 5), `InsectCatalogIdentificationTransaction` (Task 7)
- Produces: `InsectRankName identify(Image, FileName, NaturalistName, String)` — consumed by controller

- [ ] **Step 1: Add dependencies to `insects-core/pom.xml`**

Read `domains/insects/insects-core/pom.xml`. Add these dependencies if not already present:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>text-generation</artifactId>
</dependency>
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>library-api</artifactId>
</dependency>
```

The `authority` and `vision` dependencies should already be present.

- [ ] **Step 2: Rewrite `InsectIdentificationCommand` constructor with new dependencies**

Replace the constructor and fields (lines 38-50):

```java
public class InsectIdentificationCommand {

    private static final String TOOL_NAME = "propose_insect_species";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VisionService visionService;
    private final TextGenerationService textGenerationService;
    private final ExternalAuthority externalAuthority;
    private final LibraryCommand libraryCommand;
    private final InsectQuery insectQuery;
    private final InsectCatalogIdentificationTransaction transaction;

    public InsectIdentificationCommand(VisionService visionService,
                                        TextGenerationService textGenerationService,
                                        ExternalAuthority externalAuthority,
                                        LibraryCommand libraryCommand,
                                        InsectQuery insectQuery,
                                        InsectCatalogIdentificationTransaction transaction) {
        this.visionService = visionService;
        this.textGenerationService = textGenerationService;
        this.externalAuthority = externalAuthority;
        this.libraryCommand = libraryCommand;
        this.insectQuery = insectQuery;
        this.transaction = transaction;
    }
```

Add imports:

```java
import com.naturalist.textgeneration.TextGenerationService;
import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.catalog.EntityRef;
import com.naturalist.library.CitationAssociation;
import com.naturalist.library.CitationAssociationId;
import com.naturalist.library.LibraryCommand;
```

- [ ] **Step 3: Rewrite `identify()` — the full orchestration**

Replace the `identify()` method (lines 62-93):

```java
    /**
     * Identifies an insect from a photograph, validates against an external
     * authority, enriches parent ranks with grounded descriptions and features,
     * creates citations, and persists everything atomically.
     *
     * <p>All external calls (vision, authority lookup, content fetch, text
     * generation) complete before the transaction boundary. The transaction
     * does pure DB writes.
     *
     * @return the confirmed rank name for redirect, or empty if authority
     *         validation rejected the identification at all ranks
     */
    public java.util.Optional<InsectRankName> identify(Image image, FileName storedFileName,
                                                        NaturalistName naturalist,
                                                        @Nullable String notes) {
        // 1. VISION — external call
        var visionResult = identifyViaVision(image);

        // 2. AUTHORITY VALIDATION — external calls, top-down fallback
        var confirmed = validateWithAuthority(visionResult);
        if (confirmed == null) {
            return java.util.Optional.empty();
        }

        // 3. PARENT RANK ENRICHMENT — external calls, only for new ranks
        var parentDescriptions = enrichParentRanks(confirmed.taxonomy());
        var parentFeatures = new java.util.ArrayList<RankFeatures>();
        parentDescriptions.forEach((rankName, enrichment) ->
                parentFeatures.add(new RankFeatures(rankName, enrichment.features())));

        // 4. FEATURE RESOLUTION — queries only
        var allFeatures = new java.util.ArrayList<>(visionResult.features());
        var identifiedRankFeatures = new RankFeatures(
                confirmed.identifiedEntity().rankName(), visionResult.features());
        var combined = new java.util.ArrayList<RankFeatures>();
        combined.add(identifiedRankFeatures);
        combined.addAll(parentFeatures);
        var featureResolution = resolveFeatures(combined);

        // 5. CITATION PREPARATION + LIBRARY WRITES — cross-domain
        writeCitations(confirmed.authorityReferences());

        // 6. INSECT TRANSACTION — pure DB writes
        var observationId = FieldObservationId.create();
        var capturedAt = image.metadata().capturedAt() != null
                ? image.metadata().capturedAt() : Instant.now();
        var rankName = confirmed.identifiedEntity().rankName();

        var insectImage = new InsectImage(
                InsectImageId.create(), rankName, Instant.now(),
                storedFileName, observationId);

        var observation = new FieldObservation(
                observationId, naturalist, rankName, capturedAt,
                (notes == null || notes.isBlank()) ? null : notes,
                image.metadata().location(),
                confirmed.identification());

        var descriptionMap = new java.util.HashMap<InsectRankName, Description>();
        parentDescriptions.forEach((name, enrichment) ->
                descriptionMap.put(name, enrichment.description()));

        var catalogId = new CatalogIdentification(
                confirmed.identifiedEntity(), confirmed.taxonomy(),
                insectImage, observation,
                featureResolution.newFeatures(), featureResolution.assignments(),
                descriptionMap);

        transaction.execute(catalogId);

        return java.util.Optional.of(rankName);
    }

    private InsectIdentificationResult identifyViaVision(Image image) {
        var toolSchema = buildToolSchema();
        var systemPrompt = buildSystemPrompt(
                image.metadata().location() != null
                        ? image.metadata().location() : null);
        var result = visionService.identify(image, toolSchema, systemPrompt);
        return parseResult(result);
    }
```

- [ ] **Step 4: Add authority validation method**

```java
    // ----- authority validation -----

    private record ConfirmedIdentification(
            IdentifiedRankEntity identifiedEntity,
            TaxonomicClassification taxonomy,
            Identification identification,
            Map<InsectRankName, Set<AuthorityReference>> authorityReferences
    ) {}

    private @Nullable ConfirmedIdentification validateWithAuthority(
            InsectIdentificationResult visionResult) {
        var taxonomy = visionResult.taxonomy();
        var authorityRefs = new java.util.LinkedHashMap<InsectRankName, Set<AuthorityReference>>();

        // Build the rank chain from most specific to least
        var rankChain = buildRankChain(visionResult, taxonomy);

        // Validate top-down (most specific first)
        IdentifiedRankEntity confirmedEntity = null;
        for (var candidate : rankChain) {
            var refs = externalAuthority.lookup(candidate.rankName());
            if (!refs.isEmpty()) {
                confirmedEntity = candidate;
                authorityRefs.put(candidate.rankName(), refs);
                break;
            }
        }

        if (confirmedEntity == null) {
            return null; // all ranks failed — reject identification
        }

        // Collect authority refs for parent ranks above the confirmed one
        collectParentAuthorityRefs(confirmedEntity, taxonomy, authorityRefs);

        // Rebuild taxonomy to match confirmed rank
        var confirmedTaxonomy = trimTaxonomy(confirmedEntity, taxonomy);

        return new ConfirmedIdentification(
                confirmedEntity, confirmedTaxonomy,
                visionResult.identification(), authorityRefs);
    }

    private List<IdentifiedRankEntity> buildRankChain(
            InsectIdentificationResult result, TaxonomicClassification taxonomy) {
        var chain = new java.util.ArrayList<IdentifiedRankEntity>();
        chain.add(result.identifiedEntity());

        // Add parent ranks as fallback candidates (genus → family → order)
        var entity = result.identifiedEntity();
        if (entity instanceof IdentifiedRankEntity.Species && taxonomy.genus() != null) {
            var genusSlug = taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT);
            var familySlug = taxonomy.family().value().toLowerCase(java.util.Locale.ROOT);
            chain.add(new IdentifiedRankEntity.Genus(new InsectGenus(
                    InsectGenusName.of(genusSlug), InsectFamilyName.of(familySlug),
                    taxonomy.genus(), FALLBACK_DESCRIPTION, Set.of(), null)));
        }
        if ((entity instanceof IdentifiedRankEntity.Species
                || entity instanceof IdentifiedRankEntity.Genus)
                && taxonomy.family() != null) {
            var familySlug = taxonomy.family().value().toLowerCase(java.util.Locale.ROOT);
            var orderSlug = taxonomy.order().value().toLowerCase(java.util.Locale.ROOT);
            chain.add(new IdentifiedRankEntity.Family(new InsectFamily(
                    InsectFamilyName.of(familySlug), InsectOrderName.of(orderSlug),
                    taxonomy.family(), FALLBACK_DESCRIPTION, Set.of(), null)));
        }
        var orderSlug = taxonomy.order().value().toLowerCase(java.util.Locale.ROOT);
        if (!(entity instanceof IdentifiedRankEntity.Order)) {
            chain.add(new IdentifiedRankEntity.Order(new InsectOrder(
                    InsectOrderName.of(orderSlug), taxonomy.order(),
                    FALLBACK_DESCRIPTION, Set.of(), null)));
        }
        return chain;
    }

    private static final Description FALLBACK_DESCRIPTION = new Description(
            "Identified via vision — description pending.",
            "Identified via vision — description pending.",
            "Identified via vision — description pending.",
            "Identified via vision — description pending.");

    private void collectParentAuthorityRefs(IdentifiedRankEntity confirmed,
                                             TaxonomicClassification taxonomy,
                                             Map<InsectRankName, Set<AuthorityReference>> refs) {
        // Look up parent ranks above the confirmed one
        if (confirmed instanceof IdentifiedRankEntity.Species && taxonomy.genus() != null) {
            var genusName = InsectGenusName.of(
                    taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT));
            var genusRefs = externalAuthority.lookup(genusName);
            if (!genusRefs.isEmpty()) refs.put(genusName, genusRefs);
        }
        if (!(confirmed instanceof IdentifiedRankEntity.Order)) {
            if (taxonomy.family() != null) {
                var familyName = InsectFamilyName.of(
                        taxonomy.family().value().toLowerCase(java.util.Locale.ROOT));
                if (!refs.containsKey(familyName)) {
                    var familyRefs = externalAuthority.lookup(familyName);
                    if (!familyRefs.isEmpty()) refs.put(familyName, familyRefs);
                }
            }
            var orderName = InsectOrderName.of(
                    taxonomy.order().value().toLowerCase(java.util.Locale.ROOT));
            if (!refs.containsKey(orderName)) {
                var orderRefs = externalAuthority.lookup(orderName);
                if (!orderRefs.isEmpty()) refs.put(orderName, orderRefs);
            }
        }
    }

    private TaxonomicClassification trimTaxonomy(IdentifiedRankEntity confirmed,
                                                  TaxonomicClassification original) {
        return switch (confirmed) {
            case IdentifiedRankEntity.Species _ -> original;
            case IdentifiedRankEntity.Genus _ -> new TaxonomicClassification(
                    original.order(), original.family(), original.genus(), null);
            case IdentifiedRankEntity.Family _ -> new TaxonomicClassification(
                    original.order(), original.family(), null, null);
            case IdentifiedRankEntity.Order _ -> new TaxonomicClassification(
                    original.order(), null, null, null);
        };
    }
```

- [ ] **Step 5: Add parent rank enrichment method**

```java
    // ----- parent rank enrichment -----

    private record RankEnrichment(Description description, List<String> features) {}

    private Map<InsectRankName, RankEnrichment> enrichParentRanks(
            TaxonomicClassification taxonomy) {
        var enrichments = new java.util.LinkedHashMap<InsectRankName, RankEnrichment>();

        // Check each parent rank — only enrich if it doesn't already exist
        if (taxonomy.genus() != null) {
            var genusName = InsectGenusName.of(
                    taxonomy.genus().value().toLowerCase(java.util.Locale.ROOT));
            enrichIfNew(genusName, taxonomy.genus().value(), enrichments);
        }
        if (taxonomy.family() != null) {
            var familyName = InsectFamilyName.of(
                    taxonomy.family().value().toLowerCase(java.util.Locale.ROOT));
            enrichIfNew(familyName, taxonomy.family().value(), enrichments);
        }
        var orderName = InsectOrderName.of(
                taxonomy.order().value().toLowerCase(java.util.Locale.ROOT));
        enrichIfNew(orderName, taxonomy.order().value(), enrichments);

        return enrichments;
    }

    private void enrichIfNew(InsectRankName rankName, String displayName,
                             Map<InsectRankName, RankEnrichment> enrichments) {
        // Check existence by querying the appropriate rank
        boolean exists = switch (rankName) {
            case InsectOrderName n -> insectQuery.orders().getByName(n).isPresent();
            case InsectFamilyName n -> insectQuery.families().getByName(n).isPresent();
            case InsectGenusName n -> insectQuery.genera().getByName(n).isPresent();
            default -> true; // species/subspecies not parent ranks
        };
        if (exists) return;

        try {
            var refs = externalAuthority.lookup(rankName);
            if (refs.isEmpty()) {
                enrichments.put(rankName, new RankEnrichment(FALLBACK_DESCRIPTION, List.of()));
                return;
            }
            var ref = refs.iterator().next();
            var content = externalAuthority.fetchContent(ref);
            var enrichment = generateEnrichment(displayName, content);
            enrichments.put(rankName, enrichment);
        } catch (Exception e) {
            enrichments.put(rankName, new RankEnrichment(FALLBACK_DESCRIPTION, List.of()));
        }
    }

    private RankEnrichment generateEnrichment(String taxonName, AuthorityContent content) {
        var schema = """
                {
                  "type": "object",
                  "required": ["descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity", "features"],
                  "properties": {
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description" },
                    "features":                { "type": "array", "items": { "type": "string" }, "description": "Diagnostic morphological features for this rank, ordered conspicuous to diagnostic" }
                  }
                }
                """;
        var tool = new ToolSchema("describe_taxon",
                "Generate Durrell descriptions and diagnostic features for a taxon.", schema);
        var systemPrompt = """
                You are an expert entomologist. Given authoritative reference content about
                an insect taxon, produce four Durrell-level descriptions grounded in the
                provided content. Do not invent facts — only reshape what the source says.
                Also extract the diagnostic morphological features that characterise this
                taxon, ordered from most conspicuous to most diagnostic.
                """;
        var userPrompt = "Taxon: " + taxonName + "\n\nAuthority content:\n" + content.content();

        var result = textGenerationService.generate(tool, systemPrompt, userPrompt);
        try {
            var node = MAPPER.readTree(result.argumentsJson());
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var features = new java.util.ArrayList<String>();
            if (node.hasNonNull("features") && node.get("features").isArray()) {
                for (var f : node.get("features")) {
                    if (f.isTextual() && !f.asText().isBlank()) {
                        features.add(f.asText());
                    }
                }
            }
            return new RankEnrichment(description, List.copyOf(features));
        } catch (Exception e) {
            return new RankEnrichment(FALLBACK_DESCRIPTION, List.of());
        }
    }
```

- [ ] **Step 6: Add feature resolution method**

```java
    // ----- feature resolution -----

    private record RankFeatures(InsectRankName rankName, List<String> featureValues) {}

    private record FeatureResolution(
            List<InsectFeature> newFeatures,
            List<InsectFeatureAssignment> assignments
    ) {}

    private FeatureResolution resolveFeatures(List<RankFeatures> allRankFeatures) {
        var newFeatures = new java.util.ArrayList<InsectFeature>();
        var assignments = new java.util.ArrayList<InsectFeatureAssignment>();
        // Track features we've already created in this invocation
        var createdFeatures = new java.util.HashMap<String, InsectFeatureId>();

        for (var rankFeatures : allRankFeatures) {
            int ordinal = 0;
            for (var value : rankFeatures.featureValues()) {
                var normalized = value.trim().toLowerCase();
                if (normalized.isBlank()) continue;

                InsectFeatureId featureId;
                if (createdFeatures.containsKey(normalized)) {
                    featureId = createdFeatures.get(normalized);
                } else {
                    // Check if feature already exists in repository
                    // FeatureRepository is Entity-based (by ID), so we need
                    // to search by value. For now, create and let unique
                    // constraint handle dedup — or use query if available.
                    featureId = InsectFeatureId.create();
                    newFeatures.add(InsectFeature.of(featureId, normalized));
                    createdFeatures.put(normalized, featureId);
                }

                assignments.add(InsectFeatureAssignment.of(
                        InsectFeatureAssignmentId.create(),
                        featureId, rankFeatures.rankName(), ordinal++));
            }
        }

        return new FeatureResolution(List.copyOf(newFeatures), List.copyOf(assignments));
    }
```

- [ ] **Step 7: Add citation creation method**

```java
    // ----- citation creation -----

    private static final InsectsDomain INSECTS_DOMAIN = new InsectsDomain();

    private void writeCitations(Map<InsectRankName, Set<AuthorityReference>> authorityRefs) {
        for (var entry : authorityRefs.entrySet()) {
            var rankName = entry.getKey();
            for (var ref : entry.getValue()) {
                try {
                    var citationSlug = ref.source().id() + "-" + rankName.value();
                    var citationName = CitationName.of(citationSlug);
                    var title = capitalize(rankName.value()) + " — " + ref.source().displayName();
                    var citation = new OnlineSource(
                            citationName, ref, title, null, null, null);
                    libraryCommand.citations().insert(citation);

                    var association = new CitationAssociation(
                            CitationAssociationId.create(),
                            citationName,
                            new EntityRef(INSECTS_DOMAIN, rankName),
                            "Identified via vision — authority-validated");
                    libraryCommand.citationAssociations().insert(association);
                } catch (Exception e) {
                    // Degraded — identification proceeds without this citation
                }
            }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
```

- [ ] **Step 8: Update controller — change return type and redirect**

In `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`, find the `identify` POST handler (around line 303-330).

Change from:
```java
var speciesName = identificationCommand.identify(image, storedFileName, naturalist, notes);
return "redirect:/insects/" + speciesName.value();
```

To:
```java
var rankName = identificationCommand.identify(image, storedFileName, naturalist, notes);
if (rankName.isEmpty()) {
    // Authority validation rejected identification at all ranks
    return "redirect:/insects?error=identification-unverified";
}
return "redirect:/insects/" + rankName.get().value();
```

- [ ] **Step 9: Grep for all `new InsectIdentificationCommand(` call sites**

```bash
grep -rn "new InsectIdentificationCommand(" --include="*.java" .
```

Update every call site to pass the new constructor parameters (`textGenerationService`, `externalAuthority`, `libraryCommand`, `insectQuery`).

This includes:
- `InsectsTestContext.java` — needs new dependencies. For testing, use `NoOpTextGenerationService` and the `EolClientMock` from the `NaturalistDatabase`. The `LibraryCommand` comes from `LibraryTestContext`.
- `InsectsTestContextInternal.java` — same pattern.
- Any console bootstrap wiring.

- [ ] **Step 10: Verify `insects-console/pom.xml` has no new dependencies needed**

The controller's return type changes from `InsectSpeciesName` to `Optional<InsectRankName>`. Since `InsectRankName` is already in the identifiers module (which insects-console depends on), no new pom dependency is needed.

- [ ] **Step 11: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommand.java \
       domains/insects/insects-core/pom.xml \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
       domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsTestContextInternal.java
# Include any other files found by the grep
git commit -m "feat(insects): wire full identification orchestration with authority validation, enrichment, features, and citations"
```

---

### Task 9: Anthropic TextGenerationService Adapter

**Files:**
- Create: `adapters/anthropic-text-generation/pom.xml`
- Create: `adapters/anthropic-text-generation/src/main/java/com/naturalist/textgeneration/anthropic/AnthropicTextGenerationService.java`
- Create: `adapters/anthropic-text-generation/src/main/java/com/naturalist/textgeneration/anthropic/AnthropicTextGenerationConfig.java`
- Modify: `adapters/pom.xml` — add `<module>anthropic-text-generation</module>`
- Modify: `pom.xml` (root) — add dependency-management entry

**Interfaces:**
- Consumes: `TextGenerationService` (Task 1), `ToolSchema`, `ToolResult` from vision kernel, Anthropic Java SDK
- Produces: `AnthropicTextGenerationService` — wired by the composition root at deployment time

- [ ] **Step 1: Create `adapters/anthropic-text-generation/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>adapters</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>anthropic-text-generation</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>text-generation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.anthropic</groupId>
            <artifactId>anthropic-java</artifactId>
            <version>${anthropic-sdk-version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create `AnthropicTextGenerationConfig.java`**

```java
package com.naturalist.textgeneration.anthropic;

public record AnthropicTextGenerationConfig(String model, int maxTokens) {
}
```

- [ ] **Step 3: Create `AnthropicTextGenerationService.java`**

Model the implementation on `AnthropicVisionService` (at `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java`) but without image content blocks.

```java
package com.naturalist.textgeneration.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.infrastructure.Resilient;
import com.naturalist.textgeneration.TextGenerationService;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Resilient(name = "textgeneration.enrichment")
public class AnthropicTextGenerationService implements TextGenerationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnthropicTextGenerationConfig config;
    private final AnthropicClient client;

    public AnthropicTextGenerationService(AnthropicTextGenerationConfig config) {
        this.config = Objects.requireNonNull(config);
        var apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is required");
        }
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt) {
        try {
            var toolProperties = MAPPER.readValue(
                    tool.parametersJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            var properties = (Map<String, Object>) toolProperties.get("properties");
            @SuppressWarnings("unchecked")
            var required = (List<String>) toolProperties.get("required");

            var toolDef = Tool.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .inputSchema(Tool.InputSchema.builder()
                            .properties(com.anthropic.core.JsonValue.from(properties))
                            .required(required)
                            .build())
                    .build();

            var systemBlock = TextBlock.builder()
                    .text(systemPrompt)
                    .cacheControl(CacheControlEphemeral.builder().build())
                    .build();

            var userBlock = TextBlockParam.builder().text(userPrompt).build();
            var userMessage = MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .content(List.of(ContentBlockParam.ofText(userBlock)))
                    .build();

            var response = client.messages().create(MessageCreateParams.builder()
                    .model(config.model())
                    .maxTokens(config.maxTokens())
                    .system(List.of(systemBlock))
                    .messages(List.of(userMessage))
                    .tools(List.of(toolDef))
                    .toolChoice(ToolChoice.ofTool(
                            ToolChoiceTool.builder().name(tool.name()).build()))
                    .build());

            var toolUseBlock = response.content().stream()
                    .filter(block -> block.isToolUse())
                    .map(ContentBlock::asToolUse)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "No tool use block in text generation response"));

            var argumentsJson = MAPPER.writeValueAsString(toolUseBlock.input());
            return new ToolResult(toolUseBlock.name(), argumentsJson);
        } catch (Exception e) {
            throw new RuntimeException("Text generation failed", e);
        }
    }
}
```

**Note:** The exact Anthropic SDK class names and builder patterns should be verified against the version used in the project. Mirror the patterns in `AnthropicVisionService` at `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java` — the SDK API is identical for text-only messages, just without the `ImageBlockParam`.

- [ ] **Step 4: Add module to `adapters/pom.xml`**

Add `<module>anthropic-text-generation</module>` in alphabetical order within the `<modules>` block.

- [ ] **Step 5: Add dependency-management entry to root `pom.xml`**

In the ADAPTERS section, add alphabetically:

```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>anthropic-text-generation</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 6: Commit**

```bash
git add adapters/anthropic-text-generation/ adapters/pom.xml pom.xml
git commit -m "feat(adapter): add AnthropicTextGenerationService for text-only AI generation"
```

---

## Post-Implementation Notes

### Composition Root Wiring

The `apps/management-console/` composition root needs to wire the new dependencies:
- `TextGenerationService` → `AnthropicTextGenerationService` (or `NoOpTextGenerationService` for local dev)
- `ExternalAuthority` → `EolClientMock` (already wired for existing features)
- `LibraryCommand` → `LibraryCommandImpl` (new wiring)
- Updated `InsectIdentificationCommand` constructor call

This wiring depends on the Spring Boot configuration in the console app and should be handled as a follow-up once the domain changes are verified.

### Feature Deduplication

The current `resolveFeatures` implementation creates new `InsectFeature` entities without checking for existing features by value (the `FeatureRepository` is keyed by `InsectFeatureId`, not by value). The `@UniqueValue` constraint on `InsectFeature.value` will reject duplicates at insert time. The `InsectFeatureRepositoryMock` should enforce this constraint. If it does not, the transaction will silently create duplicate features with different IDs.

To handle this properly, `InsectQuery.FeatureQuery` or the `FeatureRepository` needs a `findByValue(String)` method. This should be added as a follow-up if the mock's unique constraint enforcement is insufficient.

### Console Error Handling

The controller redirect for rejected identifications (`?error=identification-unverified`) needs a corresponding error display on the insects list page. This is a console-layer concern and should be handled as a follow-up.