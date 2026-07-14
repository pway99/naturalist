# Vision-Assisted Identification MVP — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add photo-driven insect identification (via Claude Vision) and direct observation with field notes to the management console, with server-side image storage and client-side EXIF extraction/resize.

**Architecture:** Two new modules (vision kernel + Anthropic adapter) following the Resilience facade precedent. `InsectIdentificationService` in insects-core defines the tool schema and prompt; the console controller orchestrates catalog entry, citation, image, and observation creation. FieldObservation gains `location` + `confidence` fields. Images stored on filesystem, served by the console.

**Tech Stack:** Anthropic Java SDK (`com.anthropic:anthropic-java`), Spring Boot (existing), JTE templates, browser JavaScript (EXIF extraction via DataView, canvas resize).

## Global Constraints

- DAG: `anthropic-vision → vision`; `insects-core → insects-api, vision`; citation orchestration in console controller (not insects-core) to avoid cross-domain dep.
- Kernel testing convention: no direct tests for `kernels/vision/`; tested through first consumer in insects-core.
- UUIDv7 only for EntityId — use `EntityId.newUUID()`, never `UUID.randomUUID()`.
- Records for entities/value objects. `@JsonCreator` on `EntityName.of()` factories only.
- `new FieldObservation(...)` arity change ripples to: `InsectsController` (2 sites), `FieldObservationCommandImplTest` (3 sites), `FieldObservationEntityRepositoryTest` (3 sites), `field-observations.json`.
- New modules require: (1) parent pom `<module>` entry, (2) root pom `<dependencyManagement>` entry, (3) consumer `<dependency>` entries.
- Image security: validate magic bytes server-side, reject non-image content types, size cap 20 MB.

---

### Task 1: Vision kernel module

Thin vendor-neutral port for vision identification, following the Resilience facade pattern.

**Files:**
- Create: `kernels/vision/pom.xml`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/VisionService.java`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/Image.java`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/ImageMetadata.java`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/ToolSchema.java`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/ToolResult.java`
- Create: `kernels/vision/src/main/java/com/naturalist/vision/NoOpVisionService.java`
- Modify: `kernels/pom.xml` — add `<module>vision</module>`
- Modify: `pom.xml` (root) — add `vision` to `<dependencyManagement>` under KERNELS

**Interfaces:**
- Produces: `VisionService.identify(Image, ToolSchema, String) → ToolResult` — the port that Task 2 implements and Task 4 consumes.
- Produces: `Image`, `ImageMetadata`, `ToolSchema`, `ToolResult` value objects used by Tasks 2, 4, and 6.

- [ ] **Step 1: Create `kernels/vision/pom.xml`**

```xml
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
    <artifactId>vision</artifactId>
    <name>kernels :: vision</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jspecify</groupId>
            <artifactId>jspecify</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Create value objects**

`kernels/vision/src/main/java/com/naturalist/vision/ImageMetadata.java`:
```java
package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

public record ImageMetadata(
        @Nullable String location,
        @Nullable Instant capturedAt
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> {};
    }
}
```

`kernels/vision/src/main/java/com/naturalist/vision/Image.java`:
```java
package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record Image(
        byte[] bytes,
        String mediaType,
        ImageMetadata metadata
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(bytes, "bytes")
                .notBlank(mediaType, "mediaType")
                .notNull(metadata, "metadata");
    }
}
```

`kernels/vision/src/main/java/com/naturalist/vision/ToolSchema.java`:
```java
package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ToolSchema(
        String name,
        String description,
        String parametersJson
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(name, "name")
                .notBlank(description, "description")
                .notBlank(parametersJson, "parametersJson");
    }
}
```

`kernels/vision/src/main/java/com/naturalist/vision/ToolResult.java`:
```java
package com.naturalist.vision;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

public record ToolResult(
        String toolName,
        String argumentsJson
) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(toolName, "toolName")
                .notBlank(argumentsJson, "argumentsJson");
    }
}
```

- [ ] **Step 3: Create `VisionService` interface**

`kernels/vision/src/main/java/com/naturalist/vision/VisionService.java`:
```java
package com.naturalist.vision;

/**
 * Vendor-neutral port for vision-based identification. Domain code calls this
 * interface; the Anthropic adapter (or any future provider) implements it.
 * Follows the Resilience facade precedent: thin kernel port, adapter in
 * {@code adapters/}.
 */
public interface VisionService {

    ToolResult identify(Image image, ToolSchema tool, String systemPrompt);
}
```

- [ ] **Step 4: Create `NoOpVisionService`**

`kernels/vision/src/main/java/com/naturalist/vision/NoOpVisionService.java`:
```java
package com.naturalist.vision;

/**
 * Default for tests and apps that don't wire vision. Throws on use —
 * vision is an explicit opt-in, not a silent degradation.
 */
public class NoOpVisionService implements VisionService {

    @Override
    public ToolResult identify(Image image, ToolSchema tool, String systemPrompt) {
        throw new UnsupportedOperationException(
                "VisionService is not configured. Set ANTHROPIC_API_KEY and wire the Anthropic adapter.");
    }
}
```

- [ ] **Step 5: Register module in parent poms**

Add `<module>vision</module>` to `kernels/pom.xml` `<modules>` (alphabetical — after `taxonomy`):
```xml
<module>taxonomy</module>
<module>vision</module>
```

Add to root `pom.xml` `<dependencyManagement>` under KERNELS (alphabetical — after `taxonomy`):
```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>vision</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 6: Verify build**

Run: `mvn compile -pl kernels/vision -am`
Expected: BUILD SUCCESS — the module compiles with its framework dependency.

- [ ] **Step 7: Commit**

```bash
git add kernels/vision/ kernels/pom.xml pom.xml
git commit -m "feat(vision): add vision kernel — VisionService port + value objects"
```

---

### Task 2: Anthropic adapter module

Implements `VisionService` using the Anthropic Java SDK with tool use, prompt caching, and resilience wrapping.

**Files:**
- Create: `adapters/anthropic-vision/pom.xml`
- Create: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionConfig.java`
- Create: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java`
- Modify: `adapters/pom.xml` — add `<module>anthropic-vision</module>`
- Modify: `pom.xml` (root) — add `anthropic-vision` to `<dependencyManagement>` under ADAPTERS
- Modify: `apps/management-console/pom.xml` — add `anthropic-vision` + `vision` dependencies

**Interfaces:**
- Consumes: `VisionService`, `Image`, `ToolSchema`, `ToolResult` from Task 1.
- Produces: `AnthropicVisionService` — the concrete implementation wired by the composition root.
- Produces: `AnthropicVisionConfig` — configuration record (model, max tokens, temperature).

- [ ] **Step 1: Create `adapters/anthropic-vision/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>naturalist-adapters</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>anthropic-vision</artifactId>
    <name>adapters :: anthropic-vision</name>

    <description>
        Bridges the kernel VisionService facade to the Anthropic Java SDK.
        Domain code references the kernel facade only; this adapter is the
        only module that imports com.anthropic.*.
    </description>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>vision</artifactId>
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

Note: add `<anthropic-sdk-version>` property to `adapters/pom.xml` `<properties>`. Check https://central.sonatype.com for the latest version of `com.anthropic:anthropic-java`.

- [ ] **Step 2: Create `AnthropicVisionConfig`**

`adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionConfig.java`:
```java
package com.naturalist.vision.anthropic;

/**
 * Configuration for the Anthropic Vision adapter. All fields have sensible
 * defaults; the API key comes from the {@code ANTHROPIC_API_KEY} environment
 * variable, not from this config.
 */
public record AnthropicVisionConfig(
        String model,
        int maxTokens,
        double temperature
) {
    public static AnthropicVisionConfig defaults() {
        return new AnthropicVisionConfig("claude-sonnet-4-5-20250514", 4096, 0.2);
    }
}
```

- [ ] **Step 3: Create `AnthropicVisionService`**

`adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java`:

This class wraps the Anthropic Java SDK. The exact SDK API should be verified against the current SDK documentation when implementing. The structure is:

```java
package com.naturalist.vision.anthropic;

import com.naturalist.resilience.Resilient;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;

import java.util.Base64;

/**
 * Anthropic Java SDK implementation of {@link VisionService}.
 * <p>
 * Uses tool use to get structured identification data. Prompt caching is enabled:
 * the system prompt and tool schema are cache-stable across calls; only the image
 * varies, so repeated identifications benefit from cached prefixes.
 * <p>
 * API key read from {@code ANTHROPIC_API_KEY} environment variable at construction.
 * Refuses to construct if the key is absent.
 */
@Resilient(name = "vision.identification")
public class AnthropicVisionService implements VisionService {

    private final AnthropicVisionConfig config;
    private final String apiKey;

    public AnthropicVisionService(AnthropicVisionConfig config) {
        this.config = config;
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY environment variable is required for vision identification");
        }
    }

    @Override
    public ToolResult identify(Image image, ToolSchema tool, String systemPrompt) {
        // 1. Build the Anthropic client
        //    var client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        //
        // 2. Base64-encode the image bytes
        //    var base64 = Base64.getEncoder().encodeToString(image.bytes());
        //
        // 3. Build the message with:
        //    - System prompt with cache_control for prompt caching
        //    - User message containing the image block + text prompt with location context
        //    - Tool definition from the ToolSchema (name, description, JSON schema)
        //    - Model, max_tokens, temperature from config
        //
        // 4. Send the request, extract the tool_use content block
        //
        // 5. Return new ToolResult(toolUseName, toolUseInputJson)
        //
        // The implementer MUST consult the Anthropic Java SDK documentation for the
        // exact builder API. Key SDK types to use:
        //   - AnthropicOkHttpClient (or AnthropicClient)
        //   - MessageCreateParams with .model(), .maxTokens(), .system(), .messages(), .tools()
        //   - ImageBlockParam with Base64ImageSource for the image
        //   - Tool for the tool definition
        //   - CacheControlEphemeral for prompt caching on system + tool blocks
        //   - Extract ToolUseBlock from the response content blocks
        //
        // Location context from image.metadata().location() should be included in
        // the user message text: "Identify this insect. Location: {location}"
        throw new UnsupportedOperationException("Implement with Anthropic Java SDK");
    }
}
```

The implementer should replace the placeholder body with the actual SDK calls. The key integration points are:
- Prompt caching: mark the system message block and tool definition with `cache_control: {"type": "ephemeral"}` so they are cached across calls.
- Tool use: force tool use so the response always contains structured data.
- Image encoding: `Base64.getEncoder().encodeToString(image.bytes())` with media type from `image.mediaType()`.
- Location context: append `image.metadata().location()` to the user prompt when non-null.
- Extract the tool use input JSON from the response's `ToolUseBlock` content block.

- [ ] **Step 4: Register module in parent poms**

Add to `adapters/pom.xml` `<modules>` (alphabetical):
```xml
<module>anthropic-vision</module>
<module>resilience-resilience4j</module>
```

Add `<anthropic-sdk-version>` to `adapters/pom.xml` `<properties>`:
```xml
<anthropic-sdk-version>1.2.0</anthropic-sdk-version>
```
(Check Maven Central for the latest version.)

Add to root `pom.xml` `<dependencyManagement>` under ADAPTERS (alphabetical — before `resilience-resilience4j`):
```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>anthropic-vision</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 5: Wire into management-console**

Add to `apps/management-console/pom.xml` `<dependencies>` under ADAPTERS:
```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>anthropic-vision</artifactId>
</dependency>
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>vision</artifactId>
</dependency>
```

Create a Spring `@Configuration` in the management-console to wire the bean:

`apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java`:
```java
package com.naturalist.console;

import com.naturalist.vision.NoOpVisionService;
import com.naturalist.vision.VisionService;
import com.naturalist.vision.anthropic.AnthropicVisionConfig;
import com.naturalist.vision.anthropic.AnthropicVisionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VisionConfiguration {

    @Bean
    VisionService visionService() {
        var apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return new NoOpVisionService();
        }
        return new AnthropicVisionService(AnthropicVisionConfig.defaults());
    }
}
```

This degrades gracefully: without an API key, the app starts but vision identification throws on use.

- [ ] **Step 6: Verify build**

Run: `mvn compile -pl adapters/anthropic-vision -am`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add adapters/anthropic-vision/ adapters/pom.xml pom.xml apps/management-console/pom.xml apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java
git commit -m "feat(vision): add Anthropic adapter implementing VisionService"
```

---

### Task 3: FieldObservation model change

Add `location` and `confidence` fields to `FieldObservation`. Ripple to all construction sites.

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/FieldObservationCommandImplTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationEntityRepositoryTest.java`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

**Interfaces:**
- Produces: `FieldObservation(id, observedBy, subject, observedOn, notes, location, confidence)` — the updated 7-arg constructor used by Tasks 6 and 7.

- [ ] **Step 1: Update `FieldObservation` record**

In `domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java`, add two nullable fields after `notes`:

```java
public record FieldObservation(
        FieldObservationId id,
        NaturalistName observedBy,
        @JsonTypeInfo(use = Id.NAME, property = "subjectRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName subject,
        Instant observedOn,
        @Nullable String notes,
        @Nullable String location,
        @Nullable Double confidence
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

- [ ] **Step 2: Update JSON test data**

In `domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json`, add the new nullable fields:

```json
[
  { "id": "019e9000-0001-7000-8000-000000000001", "observedBy": "patrick-way",   "subjectRank": "SPECIES", "subject": "battus-philenor", "observedOn": "2026-04-20T09:00:00Z", "notes": "On the pipevine by the gate.", "location": "Oak Vista, Chico, CA", "confidence": null },
  { "id": "019e9000-0002-7000-8000-000000000002", "observedBy": "patrick-way",   "subjectRank": "GENUS",   "subject": "empoasca",        "observedOn": "2026-04-22T14:30:00Z", "notes": null, "location": null, "confidence": null },
  { "id": "019e9000-0003-7000-8000-000000000003", "observedBy": "delia-durrell", "subjectRank": "SPECIES", "subject": "battus-philenor", "observedOn": "2026-05-02T11:00:00Z", "notes": "Shown to the school group.", "location": null, "confidence": null },
  { "id": "019e9000-0004-7000-8000-000000000004", "observedBy": "delia-durrell", "subjectRank": "GENUS",   "subject": "empoasca",        "observedOn": "2026-05-03T10:15:00Z", "notes": null, "location": null, "confidence": null }
]
```

- [ ] **Step 3: Ripple to `FieldObservationCommandImplTest`**

Update all three construction methods in `domains/insects/insects-core/src/test/java/com/naturalist/insects/FieldObservationCommandImplTest.java`:

`newEntity()`:
```java
return new FieldObservation(
        FieldObservationId.create(),
        NaturalistName.of("patrick-way"),
        TestInsectsIdentifiers.InsectGenus.Empoasca.name,
        Instant.parse("2026-06-10T08:00:00Z"),
        "new",
        null, null);
```

`ghostEntity()`:
```java
return new FieldObservation(
        FieldObservationId.create(),
        NaturalistName.of("patrick-way"),
        TestInsectsIdentifiers.InsectGenus.Empoasca.name,
        Instant.parse("2026-06-11T08:00:00Z"),
        null,
        null, null);
```

`modifiedEntity(original)`:
```java
return new FieldObservation(
        original.id(),
        NaturalistName.of("delia-durrell"),
        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
        Instant.parse("2026-06-12T08:00:00Z"),
        "changed",
        "Deer Creek, Butte County, CA", null);
```

- [ ] **Step 4: Ripple to `FieldObservationEntityRepositoryTest`**

Update all three construction methods in `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationEntityRepositoryTest.java`:

`newEntity()`:
```java
return new FieldObservation(
        FieldObservationId.create(),
        PATRICK,
        TestInsectsIdentifiers.InsectGenus.Empoasca.name,
        Instant.parse("2026-06-01T08:00:00Z"),
        "new observation",
        null, null);
```

`ghostEntity()`:
```java
return new FieldObservation(
        FieldObservationId.create(),
        PATRICK,
        TestInsectsIdentifiers.InsectGenus.Empoasca.name,
        Instant.parse("2026-06-02T08:00:00Z"),
        null,
        null, null);
```

`modifiedEntity(original)`:
```java
return new FieldObservation(
        original.id(),
        DELIA,
        TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name,
        Instant.parse("2026-06-03T08:00:00Z"),
        "changed",
        "Oak Vista, Chico, CA", null);
```

- [ ] **Step 5: Ripple to `InsectsController`**

Update `observe()` method (~line 588):
```java
var observation = new FieldObservation(
        FieldObservationId.create(),
        me.get(),
        InsectSpeciesName.of(name),
        Instant.now(),
        (notes == null || notes.isBlank()) ? null : notes,
        null, null);
```

Update `addImage()` method (~line 606):
```java
var observation = new FieldObservation(
        FieldObservationId.create(), me.get(), speciesName, Instant.now(), null,
        null, null);
```

- [ ] **Step 6: Verify build**

Run: `mvn verify -pl domains/insects/insects-core,domains/insects/insects-console -am`
Expected: BUILD SUCCESS, all existing tests pass.

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/FieldObservation.java \
       domains/insects/insects-repository-test/src/main/resources/insects/field-observations.json \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/FieldObservationCommandImplTest.java \
       domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FieldObservationEntityRepositoryTest.java \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "feat(insects): add location + confidence fields to FieldObservation"
```

---

### Task 4: Insect identification service

Defines the tool schema, system prompt, and result mapping for vision-based insect identification. Lives in insects-core.

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java`
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationService.java`
- Modify: `domains/insects/insects-core/pom.xml` — add `vision` kernel dependency
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationServiceTest.java`

**Interfaces:**
- Consumes: `VisionService.identify(Image, ToolSchema, String) → ToolResult` from Task 1.
- Produces: `InsectIdentificationService.identify(Image) → InsectIdentificationResult` — called by the console controller in Task 6.
- Produces: `InsectIdentificationResult` record — carries `InsectSpecies species`, `double confidence`, `String evidence`, `@Nullable String alternativesJson`.

- [ ] **Step 1: Add vision dependency to insects-core**

Add to `domains/insects/insects-core/pom.xml` `<dependencies>`:
```xml
<dependency>
    <groupId>com.naturalist</groupId>
    <artifactId>vision</artifactId>
</dependency>
```

- [ ] **Step 2: Create `InsectIdentificationResult`**

`domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java`:
```java
package com.naturalist.insects;

import org.jspecify.annotations.Nullable;

/**
 * The domain-typed result of a vision identification. Carries the candidate
 * {@link InsectSpecies} record (ready for catalog insert if new), the model's
 * confidence, supporting evidence, and an optional JSON string of alternatives.
 */
public record InsectIdentificationResult(
        InsectSpecies species,
        double confidence,
        String evidence,
        @Nullable String alternativesJson
) {}
```

- [ ] **Step 3: Create `InsectIdentificationService`**

`domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationService.java`:

```java
package com.naturalist.insects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicClassification;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;

import java.util.Set;

/**
 * Orchestrates vision identification for insects. Owns the tool schema,
 * system prompt, and result deserialization. The identify-insect Claude Code
 * skill has working prompts that informed this implementation.
 */
public class InsectIdentificationService {

    private static final String TOOL_NAME = "propose_insect_species";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VisionService visionService;

    InsectIdentificationService(VisionService visionService) {
        this.visionService = visionService;
    }

    InsectIdentificationResult identify(Image image) {
        var toolSchema = buildToolSchema();
        var systemPrompt = buildSystemPrompt(image.metadata().location());
        var result = visionService.identify(image, toolSchema, systemPrompt);
        return parseResult(result);
    }

    private ToolSchema buildToolSchema() {
        var schema = """
                {
                  "type": "object",
                  "required": ["name", "order", "family", "genus", "species", "commonName",
                               "descriptionPreschool", "descriptionElementary",
                               "descriptionSecondary", "descriptionUniversity",
                               "guilds", "beneficial", "confidence", "evidence"],
                  "properties": {
                    "name":                    { "type": "string", "description": "Kebab-case slug for the species, e.g. battus-philenor" },
                    "order":                   { "type": "string", "description": "Taxonomic order, e.g. Lepidoptera" },
                    "family":                  { "type": "string", "description": "Taxonomic family, e.g. Papilionidae" },
                    "genus":                   { "type": "string", "description": "Taxonomic genus, e.g. Battus" },
                    "species":                 { "type": "string", "description": "Species epithet, e.g. philenor" },
                    "commonName":              { "type": "string", "description": "Most widely used common name" },
                    "descriptionPreschool":    { "type": "string", "description": "Durrell preschool-level description (simple, sensory, wonder-focused)" },
                    "descriptionElementary":   { "type": "string", "description": "Durrell elementary-level description (observable features, life cycle basics)" },
                    "descriptionSecondary":    { "type": "string", "description": "Durrell secondary-level description (ecology, adaptations, relationships)" },
                    "descriptionUniversity":   { "type": "string", "description": "Durrell university-level description (taxonomy, research context, conservation)" },
                    "guilds":                  { "type": "array", "items": { "type": "string", "enum": ["PARASITOID","PREDATOR","APEX_PREDATOR","POLLINATOR","DECOMPOSER","FOOD_WEB","MIGRATORY","KEYSTONE"] }, "description": "Functional ecological guilds" },
                    "beneficial":              { "type": "boolean", "description": "Whether this insect is beneficial in a garden/agricultural context" },
                    "sightingNotes":           { "type": ["string", "null"], "description": "Notable observations about this sighting" },
                    "confidence":              { "type": "number", "minimum": 0, "maximum": 1, "description": "Confidence in identification (0.0-1.0)" },
                    "evidence":                { "type": "string", "description": "Which visible features support this identification" },
                    "alternatives":            { "type": ["string", "null"], "description": "JSON array of alternative candidates with name and confidence, or null if highly confident" }
                  }
                }
                """;
        return new ToolSchema(TOOL_NAME,
                "Propose an insect species identification based on the provided photograph.",
                schema);
    }

    private String buildSystemPrompt(String location) {
        var prompt = """
                You are an expert entomologist assisting a naturalist in identifying insects
                from photographs. For each identification:
                
                1. Examine the photograph carefully, noting morphological features (wing
                   venation, body shape, coloration, antennae, leg structure).
                2. Consider the geographic location if provided — use it to narrow range maps
                   and eliminate look-alike species from other regions.
                3. Provide four Durrell-level descriptions:
                   - Preschool: simple, sensory, wonder-focused (what a 4-year-old would notice)
                   - Elementary: observable features, life cycle basics (what a 10-year-old learns)
                   - Secondary: ecology, adaptations, relationships (high school biology level)
                   - University: taxonomy, research context, conservation status (expert level)
                4. Assess your confidence honestly. Below 0.7, name the specific features you
                   cannot confirm from the photo.
                5. List alternative candidates if confidence is below 0.9.
                6. Assign functional ecological guilds from the allowed list.
                
                Generate the kebab-case slug name from the binomial name (e.g. battus-philenor).
                Use the propose_insect_species tool to return your identification.
                """;
        if (location != null && !location.isBlank()) {
            prompt += "\nLocation context: " + location;
        }
        return prompt;
    }

    private InsectIdentificationResult parseResult(ToolResult result) {
        try {
            var node = MAPPER.readTree(result.argumentsJson());
            var name = InsectSpeciesName.of(node.get("name").asText());
            var genusSlug = node.get("genus").asText().toLowerCase();
            var genusName = InsectGenusName.of(genusSlug);
            var taxonomy = new TaxonomicClassification(
                    node.get("order").asText(),
                    node.get("family").asText(),
                    node.get("genus").asText(),
                    node.get("species").asText());
            var description = new Description(
                    node.get("descriptionPreschool").asText(),
                    node.get("descriptionElementary").asText(),
                    node.get("descriptionSecondary").asText(),
                    node.get("descriptionUniversity").asText());
            var commonName = node.get("commonName").asText();

            // Build the InsectSpecies — use the current constructor shape.
            // Nullable value-object fields (chemicalDefense, voltinism, etc.)
            // are null; they are populated incrementally by the naturalist.
            var species = new InsectSpecies(
                    name,
                    genusName,
                    taxonomy.species() != null
                            ? com.naturalist.taxonomy.TaxonomicSpecies.of(taxonomy.species())
                            : null,
                    description,
                    Set.of(com.naturalist.insects.CommonName.of(commonName)),
                    node.has("sightingNotes") && !node.get("sightingNotes").isNull()
                            ? node.get("sightingNotes").asText() : null,
                    null,  // placedIn (Clade) — not vision-determinable
                    null,  // chemicalDefense
                    null,  // voltinism
                    null,  // habitatRequirements
                    null,  // gardenConnections
                    null,  // beneficialProfile
                    null   // ecologicalSignificance
            );

            var confidence = node.get("confidence").asDouble();
            var evidence = node.get("evidence").asText();
            var alternativesJson = node.has("alternatives") && !node.get("alternatives").isNull()
                    ? node.get("alternatives").asText() : null;

            return new InsectIdentificationResult(species, confidence, evidence, alternativesJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse vision identification result", e);
        }
    }
}
```

**Important:** The `new InsectSpecies(...)` constructor call must match the current record shape exactly. The implementer should verify the current InsectSpecies component order by reading `InsectSpecies.java` before implementing. The species record has approximately 13 components; nullable value-object fields (chemicalDefense, voltinism, habitatRequirements, gardenConnections, beneficialProfile, ecologicalSignificance) are set to null — they are enriched later by the naturalist.

- [ ] **Step 4: Write test for result parsing**

`domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationServiceTest.java`:

```java
package com.naturalist.insects;

import com.naturalist.vision.Image;
import com.naturalist.vision.ImageMetadata;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsectIdentificationServiceTest {

    static final String SAMPLE_RESULT_JSON = """
            {
              "name": "vanessa-cardui",
              "order": "Lepidoptera",
              "family": "Nymphalidae",
              "genus": "Vanessa",
              "species": "cardui",
              "commonName": "Painted Lady",
              "descriptionPreschool": "A pretty orange and black butterfly with spots on its wings.",
              "descriptionElementary": "The Painted Lady is one of the most widespread butterflies in the world, found on every continent except Antarctica.",
              "descriptionSecondary": "Vanessa cardui is a highly migratory species known for its remarkable long-distance movements across continents.",
              "descriptionUniversity": "V. cardui exhibits one of the longest insect migration patterns known, with multi-generational movements spanning thousands of kilometers.",
              "guilds": ["POLLINATOR", "MIGRATORY"],
              "beneficial": true,
              "sightingNotes": "Nectaring on lantana in afternoon sun",
              "confidence": 0.85,
              "evidence": "Orange and black wing pattern with distinctive white spots on dark wing tips, four small eyespots on hindwing underside",
              "alternatives": null
            }
            """;

    private final VisionService stubService = (image, tool, prompt) ->
            new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);

    private final InsectIdentificationService service = new InsectIdentificationService(stubService);

    @Test
    void identify_parsesSpeciesFromToolResult() {
        var image = new Image(
                new byte[]{1, 2, 3}, "image/jpeg",
                new ImageMetadata("Chico, CA", null));

        var result = service.identify(image);

        assertThat(result.species().name().value()).isEqualTo("vanessa-cardui");
        assertThat(result.species().description().preschool()).contains("orange and black");
        assertThat(result.confidence()).isEqualTo(0.85);
        assertThat(result.evidence()).contains("wing pattern");
    }

    @Test
    void identify_includesLocationInPrompt() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var svc = new InsectIdentificationService(capturing);
        var image = new Image(
                new byte[]{1}, "image/jpeg",
                new ImageMetadata("Deer Creek, Butte County, CA", null));

        svc.identify(image);

        assertThat(promptCapture[0]).contains("Deer Creek, Butte County, CA");
    }

    @Test
    void identify_omitsLocationWhenNull() {
        var promptCapture = new String[1];
        VisionService capturing = (image, tool, prompt) -> {
            promptCapture[0] = prompt;
            return new ToolResult("propose_insect_species", SAMPLE_RESULT_JSON);
        };
        var svc = new InsectIdentificationService(capturing);
        var image = new Image(new byte[]{1}, "image/jpeg", new ImageMetadata(null, null));

        svc.identify(image);

        assertThat(promptCapture[0]).doesNotContain("Location context:");
    }
}
```

- [ ] **Step 5: Run tests**

Run: `mvn test -pl domains/insects/insects-core -am`
Expected: All tests pass, including the three new identification service tests.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-core/pom.xml \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationResult.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationService.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationServiceTest.java
git commit -m "feat(insects): add InsectIdentificationService with tool schema and result mapping"
```

---

### Task 5: Image storage and serving

Filesystem-based image storage with magic-byte validation and an authenticated serving endpoint.

**Files:**
- Create: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/ImageStorageService.java`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — add `GET /insects/uploads/{filename}` endpoint

**Interfaces:**
- Produces: `ImageStorageService.store(byte[], String originalFilename) → FileName` — stores image to filesystem, returns the UUID filename. Used by Tasks 6 and 7.
- Produces: `GET /insects/uploads/{filename}` — serves uploaded images with auth.

- [ ] **Step 1: Create `ImageStorageService`**

`domains/insects/insects-console/src/main/java/com/naturalist/insects/console/ImageStorageService.java`:
```java
package com.naturalist.insects.console;

import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityId;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores uploaded images to the filesystem under UUID filenames. Validates
 * content type via magic bytes — rejects anything that isn't JPEG, PNG, or WebP.
 * <p>
 * Storage directory is configurable; defaults to {@code data/images/insects/}
 * relative to the working directory.
 */
class ImageStorageService {

    private static final int MAX_SIZE = 20 * 1024 * 1024; // 20 MB

    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    // PNG: 89 50 4E 47
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    // WebP: RIFF....WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private final Path storageDir;

    ImageStorageService(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create image storage directory: " + storageDir, e);
        }
    }

    FileName store(byte[] imageBytes) {
        validateSize(imageBytes);
        var extension = detectExtension(imageBytes);
        var uuidName = EntityId.newUUID().toString() + extension;
        var target = storageDir.resolve(uuidName);
        try {
            Files.write(target, imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image: " + target, e);
        }
        return FileName.of(uuidName);
    }

    Path resolve(String filename) {
        // Prevent path traversal
        var resolved = storageDir.resolve(filename).normalize();
        if (!resolved.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        return resolved;
    }

    private void validateSize(byte[] imageBytes) {
        if (imageBytes.length > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "Image exceeds maximum size of 20 MB (got " + imageBytes.length + " bytes)");
        }
        if (imageBytes.length < 4) {
            throw new IllegalArgumentException("Image data too small to be valid");
        }
    }

    private String detectExtension(byte[] bytes) {
        if (startsWith(bytes, JPEG_MAGIC)) return ".jpg";
        if (startsWith(bytes, PNG_MAGIC)) return ".png";
        if (bytes.length >= 12 && startsWith(bytes, RIFF_MAGIC)
                && bytes[8] == WEBP_MAGIC[0] && bytes[9] == WEBP_MAGIC[1]
                && bytes[10] == WEBP_MAGIC[2] && bytes[11] == WEBP_MAGIC[3]) {
            return ".webp";
        }
        throw new IllegalArgumentException(
                "Unsupported image format. Only JPEG, PNG, and WebP are accepted.");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
```

- [ ] **Step 2: Add upload serving endpoint to `InsectsController`**

Add to `InsectsController`:

```java
@GetMapping("/uploads/{filename:.+}")
void serveUpload(@PathVariable String filename, HttpServletResponse response) throws IOException {
    var path = imageStorageService.resolve(filename);
    if (!Files.exists(path)) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }
    var contentType = Files.probeContentType(path);
    if (contentType == null) contentType = "application/octet-stream";
    response.setContentType(contentType);
    response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
    response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
    Files.copy(path, response.getOutputStream());
}
```

Add `ImageStorageService` as a constructor dependency of `InsectsController`. The controller constructs it with a configurable path (e.g., `Path.of("data/images/insects")`).

Also add the import for `HttpServletResponse` and `Files` (from `java.nio.file`).

- [ ] **Step 3: Verify build**

Run: `mvn compile -pl domains/insects/insects-console -am`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/ImageStorageService.java \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "feat(console): add filesystem image storage with magic-byte validation"
```

---

### Task 6: Identify route with client-side image handling

The full `/insects/identify` flow: client-side EXIF extraction + resize, server-side identification + catalog entry + citation + observation creation.

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/identify.jte`
- Create: `apps/management-console/src/main/resources/static/js/image-upload.js`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — add GET + POST `/insects/identify`

**Interfaces:**
- Consumes: `InsectIdentificationService.identify(Image) → InsectIdentificationResult` from Task 4.
- Consumes: `ImageStorageService.store(byte[]) → FileName` from Task 5.
- Consumes: `InsectCommand.species().insert(InsectSpecies)`, `InsectCommand.images().insert(InsectImage)`, `InsectCommand.fieldObservations().insert(FieldObservation)` — existing.
- Consumes: `FieldObservation(id, observedBy, subject, observedOn, notes, location, confidence)` from Task 3.

- [ ] **Step 1: Create client-side JavaScript**

`apps/management-console/src/main/resources/static/js/image-upload.js`:

```javascript
(function () {
    'use strict';

    /**
     * Reads EXIF data from a JPEG file to extract GPS coordinates and date.
     * Minimal parser — handles only GPS and DateTimeOriginal tags.
     */
    function readExif(file) {
        return new Promise(function (resolve) {
            var reader = new FileReader();
            reader.onload = function (e) {
                var view = new DataView(e.target.result);
                // Check for JPEG SOI marker
                if (view.getUint16(0) !== 0xFFD8) {
                    resolve({});
                    return;
                }
                var offset = 2;
                var result = {};
                while (offset < view.byteLength - 2) {
                    var marker = view.getUint16(offset);
                    if (marker === 0xFFE1) { // APP1 (EXIF)
                        var exifData = parseExifApp1(view, offset + 4);
                        if (exifData.lat !== undefined && exifData.lng !== undefined) {
                            result.lat = exifData.lat;
                            result.lng = exifData.lng;
                        }
                        if (exifData.dateTime) result.dateTime = exifData.dateTime;
                        break;
                    }
                    var size = view.getUint16(offset + 2);
                    offset += 2 + size;
                }
                resolve(result);
            };
            reader.readAsArrayBuffer(file);
        });
    }

    function parseExifApp1(view, start) {
        // MVP: returns empty — the user manually enters location in the text field.
        // A full EXIF IFD parser (byte-order detection, IFD0 traversal, GPS sub-IFD
        // with rational-to-decimal conversion) is ~150 lines. Implement when the
        // manual-entry friction is named. Alternatively, use a small library like
        // exif-js or piexifjs.
        return {};
    }

    /**
     * Resizes an image to fit within maxDim on its longest side,
     * re-encodes as JPEG quality 0.85, returns a Blob.
     */
    function resizeImage(file, maxDim) {
        return new Promise(function (resolve) {
            var img = new window.Image();
            img.onload = function () {
                var w = img.width, h = img.height;
                if (w > maxDim || h > maxDim) {
                    if (w > h) { h = Math.round(h * maxDim / w); w = maxDim; }
                    else { w = Math.round(w * maxDim / h); h = maxDim; }
                }
                var canvas = document.createElement('canvas');
                canvas.width = w;
                canvas.height = h;
                canvas.getContext('2d').drawImage(img, 0, 0, w, h);
                canvas.toBlob(function (blob) { resolve(blob); }, 'image/jpeg', 0.85);
            };
            img.src = URL.createObjectURL(file);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('identify-form');
        if (!form) return;

        var fileInput = form.querySelector('input[type="file"]');
        var locationInput = form.querySelector('input[name="location"]');
        var dateInput = form.querySelector('input[name="capturedAt"]');
        var preview = document.getElementById('image-preview');

        fileInput.addEventListener('change', function () {
            var file = fileInput.files[0];
            if (!file) return;

            // Show preview
            if (preview) {
                preview.src = URL.createObjectURL(file);
                preview.style.display = 'block';
            }

            // Extract EXIF
            readExif(file).then(function (exif) {
                if (exif.lat !== undefined && exif.lng !== undefined && locationInput) {
                    // For MVP, show coordinates; user edits to a place name
                    locationInput.value = exif.lat.toFixed(4) + ', ' + exif.lng.toFixed(4);
                }
                if (exif.dateTime && dateInput) {
                    dateInput.value = exif.dateTime;
                }
            });
        });

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var file = fileInput.files[0];
            if (!file) return;

            var submitBtn = form.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Identifying...';

            resizeImage(file, 1024).then(function (blob) {
                var formData = new FormData();
                formData.append('image', blob, file.name);
                // Copy text fields
                var fields = form.querySelectorAll('input[type="text"], input[type="hidden"], textarea');
                fields.forEach(function (f) { formData.append(f.name, f.value); });

                return fetch(form.action, { method: 'POST', body: formData });
            }).then(function (response) {
                if (response.redirected) {
                    window.location.href = response.url;
                } else {
                    window.location.reload();
                }
            }).catch(function (err) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Identify';
                alert('Identification failed: ' + err.message);
            });
        });
    });
}());
```

- [ ] **Step 2: Create `identify.jte` template**

`domains/insects/insects-console/src/main/jte/insects/identify.jte`:

```jte
@import jakarta.servlet.http.HttpServletRequest
@import org.springframework.security.web.csrf.CsrfToken

@param HttpServletRequest request
@param CsrfToken _csrf

!{var csrfName = _csrf != null ? _csrf.getParameterName() : "";}
!{var csrfValue = _csrf != null ? _csrf.getToken() : "";}

@template.layout.page(
    title = "Identify Insect",
    content = @`
        <div class="identify-page">
            <h1>Identify an Insect</h1>
            <p>Upload a photo and Claude will suggest an identification.
               You can verify the result against authoritative references.</p>

            <form id="identify-form" action="/insects/identify" method="post"
                  enctype="multipart/form-data">
                <input type="hidden" name="${csrfName}" value="${csrfValue}">

                <div class="form-group">
                    <label for="image">Photo</label>
                    <input type="file" id="image" name="image"
                           accept="image/*" capture="environment" required>
                </div>

                <img id="image-preview" style="display:none; max-width:400px; margin:1rem 0;"
                     alt="Preview">

                <div class="form-group">
                    <label for="location">Location</label>
                    <input type="text" id="location" name="location"
                           placeholder="e.g. Deer Creek, Butte County, CA">
                </div>

                <div class="form-group">
                    <label for="capturedAt">Date observed</label>
                    <input type="text" id="capturedAt" name="capturedAt"
                           placeholder="auto-detected from photo">
                </div>

                <div class="form-group">
                    <label for="notes">Field notes (optional)</label>
                    <textarea id="notes" name="notes" rows="3"
                              placeholder="Habitat, behavior, weather..."></textarea>
                </div>

                <button type="submit">Identify</button>
            </form>
        </div>

        <script src="/js/image-upload.js"></script>
    `
)
```

- [ ] **Step 3: Add controller methods**

Add to `InsectsController`:

Constructor: inject `InsectIdentificationService` (constructed from `VisionService` bean) and `ImageStorageService`. Also inject `LibraryTestContext` or the citation command/query needed for citation creation.

```java
@GetMapping("/identify")
String identifyForm(HttpServletRequest request, Model model) {
    return "insects/identify";
}

@PostMapping("/identify")
String identify(@RequestParam("image") MultipartFile imageFile,
                @RequestParam(name = "location", required = false) String location,
                @RequestParam(name = "capturedAt", required = false) String capturedAt,
                @RequestParam(name = "notes", required = false) String notes,
                HttpServletRequest request) throws IOException {
    var me = currentNaturalist(request);
    if (me.isEmpty()) {
        return "redirect:/insects/identify";
    }

    // 1. Store the image
    var imageBytes = imageFile.getBytes();
    var storedFileName = imageStorageService.store(imageBytes);

    // 2. Identify via vision
    var capturedInstant = capturedAt != null && !capturedAt.isBlank()
            ? Instant.parse(capturedAt) : null;
    var image = new com.naturalist.vision.Image(
            imageBytes, "image/jpeg",
            new com.naturalist.vision.ImageMetadata(location, capturedInstant));
    var result = identificationService.identify(image);

    // 3. Insert species into catalog if new
    var speciesName = result.species().name();
    var existingSpecies = insectQuery.species().getByName(speciesName);
    if (existingSpecies.isEmpty()) {
        insectCommand.species().insert(result.species());
    }

    // 4. Create EOL citation association for the suggested species
    //    (orchestrated here in the controller to avoid cross-domain dep in insects-core)
    createEolCitation(speciesName, result.species());

    // 5. Create InsectImage
    var observationId = FieldObservationId.create();
    var insectImage = new InsectImage(
            InsectImageId.create(),
            speciesName,
            Instant.now(),
            storedFileName,
            observationId);
    insectCommand.images().insert(insectImage);

    // 6. Create FieldObservation
    var visionNotes = buildVisionNotes(notes, result);
    var observation = new FieldObservation(
            observationId,
            me.get(),
            speciesName,
            capturedInstant != null ? capturedInstant : Instant.now(),
            visionNotes,
            location,
            result.confidence());
    insectCommand.fieldObservations().insert(observation);

    return "redirect:/insects/" + speciesName.value();
}

private String buildVisionNotes(String userNotes, InsectIdentificationResult result) {
    var sb = new StringBuilder();
    if (userNotes != null && !userNotes.isBlank()) {
        sb.append(userNotes).append("\n\n");
    }
    sb.append("Vision identification (")
      .append(String.format("%.0f%%", result.confidence() * 100))
      .append(" confidence): ")
      .append(result.evidence());
    if (result.alternativesJson() != null) {
        sb.append("\nAlternatives: ").append(result.alternativesJson());
    }
    return sb.toString();
}
```

The `createEolCitation` method constructs an `OnlineSource` citation with an EOL search URL and a `CitationAssociation` linking it to the suggested species. The exact implementation depends on how the library domain commands are accessed in the console — check the existing `InsectsController` constructor for the available library dependencies.

- [ ] **Step 4: Add nav link**

Add an "Identify" link in the insects navigation (header or sidebar) pointing to `/insects/identify`. The exact location depends on the current nav structure — grep for existing nav links like `/insects` in the layout or nav templates.

- [ ] **Step 5: Test the full flow manually**

Start the management console with `ANTHROPIC_API_KEY` set. Navigate to `/insects/identify`, upload a photo, verify:
- Client-side: preview shows, location field populates (if EXIF GPS present), resize happens on submit.
- Server-side: image stored to `data/images/insects/`, species created in catalog, citation created, observation created.
- Redirect lands on the species detail page showing the new catalog entry, photo, and field notes.

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/identify.jte \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java \
       apps/management-console/src/main/resources/static/js/image-upload.js
git commit -m "feat(console): add /insects/identify route with vision identification"
```

---

### Task 7: Rank page enhancements — photo upload, field notes, re-identify

Enhances the existing species/rank detail pages with device image upload, editable field notes, and identification narrowing.

**Files:**
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte` — enhanced photo form, field notes display/edit, re-identify action
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java` — POST notes, POST re-identify, enhanced addImage

**Interfaces:**
- Consumes: `ImageStorageService.store(byte[]) → FileName` from Task 5.
- Consumes: `FieldObservation(id, observedBy, subject, observedOn, notes, location, confidence)` from Task 3.
- Consumes: `InsectCommand.fieldObservations().update(FieldObservation)` — existing.
- Consumes: `InsectCommand.images().update(InsectImage)` — existing.

- [ ] **Step 1: Enhance the "Add Photo" form in `detail.jte`**

Replace the existing file-drop instruction + text-input form (lines ~113-129 in `detail.jte`) with a device upload form:

```jte
<section class="add-image">
    <h2>Add Photo</h2>
    <form id="identify-form" action="/insects/${species.name().value()}/images" method="post"
          enctype="multipart/form-data">
        @if(_csrf != null)
            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
        @endif
        <div class="form-group">
            <input type="file" name="image" accept="image/*" capture="environment" required>
        </div>
        <div class="form-group">
            <input type="text" name="location" placeholder="Location (e.g. Oak Vista, Chico, CA)">
        </div>
        <div class="form-group">
            <textarea name="notes" rows="2" placeholder="Field notes (optional)"></textarea>
        </div>
        <button type="submit">Add photo</button>
    </form>
</section>
```

- [ ] **Step 2: Add field notes display in the photo gallery**

In `detail.jte`, modify the photo gallery section. For each image that has a linked `FieldObservation`, render the observation's notes, location, and confidence. The controller must pass observations alongside images — add a `Map<InsectImageId, FieldObservation>` or a list of image-observation pairs to the model.

```jte
@if(!images.isEmpty())
    <section>
        <h2>Photo Gallery</h2>
        <div class="image-gallery">
            @for(var img : images)
                <figure class="observation-card">
                    <img src="/insects/uploads/${img.resourceName().value()}"
                         alt="${species.name().value()}"
                         loading="lazy"
                         onerror="this.src='/insects/images/${img.resourceName().value()}'">
                    <%-- Fall back to classpath images for pre-upload catalog images --%>
                    @if(observations.containsKey(img.id()))
                        !{var obs = observations.get(img.id());}
                        <figcaption>
                            @if(obs.location() != null)
                                <small class="obs-location">${obs.location()}</small>
                            @endif
                            @if(obs.confidence() != null)
                                <small class="obs-confidence">
                                    ${String.format("%.0f%%", obs.confidence() * 100)} confidence
                                </small>
                            @endif
                            <form method="post" action="/insects/${species.name().value()}/notes">
                                @if(_csrf != null)
                                    <input type="hidden" name="${_csrf.getParameterName()}"
                                           value="${_csrf.getToken()}">
                                @endif
                                <input type="hidden" name="observationId" value="${obs.id().value()}">
                                <textarea name="notes" rows="2"
                                          placeholder="Field notes...">${obs.notes() != null ? obs.notes() : ""}</textarea>
                                <button type="submit">Save notes</button>
                            </form>
                        </figcaption>
                    @endif
                </figure>
            @endfor
        </div>
    </section>
@endif
```

Note: the `onerror` fallback on `<img>` handles the transition — existing catalog images are served from classpath at `/insects/images/`, newly uploaded images from filesystem at `/insects/uploads/`. The implementer should verify the exact image-serving paths.

- [ ] **Step 3: Update `addImage` controller method**

Replace the existing `addImage` method to accept multipart upload instead of a filename string:

```java
@PostMapping("/{name}/images")
String addImage(@PathVariable String name,
                @RequestParam("image") MultipartFile imageFile,
                @RequestParam(name = "location", required = false) String location,
                @RequestParam(name = "notes", required = false) String notes,
                HttpServletRequest request) throws IOException {
    var rankName = InsectSpeciesName.of(name);
    var me = currentNaturalist(request);

    var storedFileName = imageStorageService.store(imageFile.getBytes());

    FieldObservationId observationId = null;
    if (me.isPresent()) {
        observationId = FieldObservationId.create();
        var observation = new FieldObservation(
                observationId, me.get(), rankName, Instant.now(),
                (notes == null || notes.isBlank()) ? null : notes,
                (location == null || location.isBlank()) ? null : location,
                null);
        insectCommand.fieldObservations().insert(observation);
    }

    var image = new InsectImage(
            InsectImageId.create(), rankName, Instant.now(),
            storedFileName, observationId);
    insectCommand.images().insert(image);
    return "redirect:/insects/" + name;
}
```

- [ ] **Step 4: Add `notes` update endpoint**

```java
@PostMapping("/{name}/notes")
String updateNotes(@PathVariable String name,
                   @RequestParam("observationId") String observationId,
                   @RequestParam("notes") String notes,
                   HttpServletRequest request) {
    var obsId = FieldObservationId.of(java.util.UUID.fromString(observationId));
    var existing = insectQuery.fieldObservations().getByName(obsId);
    if (existing.isEmpty()) {
        return "redirect:/insects/" + name;
    }
    var obs = existing.get();
    var updated = new FieldObservation(
            obs.id(), obs.observedBy(), obs.subject(), obs.observedOn(),
            (notes == null || notes.isBlank()) ? null : notes,
            obs.location(), obs.confidence());
    insectCommand.fieldObservations().update(updated);
    return "redirect:/insects/" + name;
}
```

- [ ] **Step 5: Add `re-identify` endpoint**

```java
@PostMapping("/{name}/re-identify")
String reIdentify(@PathVariable String name,
                  @RequestParam("observationId") String observationId,
                  @RequestParam("newSubject") String newSubject,
                  @RequestParam("newSubjectRank") String newSubjectRank,
                  HttpServletRequest request) {
    var obsId = FieldObservationId.of(java.util.UUID.fromString(observationId));
    var existing = insectQuery.fieldObservations().getByName(obsId);
    if (existing.isEmpty()) return "redirect:/insects/" + name;

    var obs = existing.get();
    InsectRankName newRankName = switch (newSubjectRank) {
        case "SPECIES" -> InsectSpeciesName.of(newSubject);
        case "GENUS" -> InsectGenusName.of(newSubject);
        case "FAMILY" -> InsectFamilyName.of(newSubject);
        case "ORDER" -> InsectOrderName.of(newSubject);
        default -> throw new IllegalArgumentException("Unknown rank: " + newSubjectRank);
    };

    // Update observation subject
    var updatedObs = new FieldObservation(
            obs.id(), obs.observedBy(), newRankName, obs.observedOn(),
            obs.notes(), obs.location(), obs.confidence());
    insectCommand.fieldObservations().update(updatedObs);

    // Update linked images
    var images = insectQuery.images().findByNameSet(
            java.util.Set.of(obs.id())); // need to find images by observationId
    // The implementer should use the image query to find images linked to this
    // observation and update their parentName to newRankName.

    return "redirect:/insects/" + newSubject;
}
```

Note: finding images by `observationId` may require adding a query method to `InsectQuery.ImageQuery` or filtering the image collection. The implementer should check the available query methods and add one if needed.

- [ ] **Step 6: Pass observations to the detail template**

In the controller method that renders the species detail page, query the current naturalist's observations for this species and build a `Map<InsectImageId, FieldObservation>` keyed by the observation ID linked from each image. Pass this map to the template model.

The implementer should find the existing `detail()` (or `species()`) method in InsectsController and add:
```java
// Build observation lookup for images with field notes
var observations = new java.util.HashMap<InsectImageId, FieldObservation>();
var me = currentNaturalist(request);
if (me.isPresent()) {
    var obs = insectQuery.fieldObservations()
            .forNaturalistAndSubjects(me.get(), java.util.Set.of(speciesName));
    // Match observations to images via observationId
    for (var img : imageList) {
        if (img.observationId() != null) {
            obs.stream()
                .filter(o -> o.id().equals(img.observationId()))
                .findFirst()
                .ifPresent(o -> observations.put(img.id(), o));
        }
    }
}
model.addAttribute("observations", observations);
```

- [ ] **Step 7: Test manually**

Start the console. On a species detail page:
- Upload a photo via the enhanced form — verify image stored, observation created with notes.
- Edit field notes on an existing observation — verify save works.
- Test the re-identify flow if the UI is wired.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/detail.jte \
       domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "feat(console): enhanced photo upload, field notes editing, re-identify on rank pages"
```
