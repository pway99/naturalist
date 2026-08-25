# Reuse-Aware Feature Identification (Slice B) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the insect identification process from minting near-duplicate features: after the vision model proposes feature strings, search existing features and let the model (in a second tool-use turn) resolve each proposed feature to an existing one or a genuinely new one.

**Architecture:** A new `kernels/feature-search` port + in-memory Jaccard mock (per-domain isolated, catalog-style, production adapter deferred); a multi-turn extension to `VisionService` (`VisionExchange`); and a reworked `InsectIdentificationCommand.resolveFeatures` that consumes the model's resolution. Design: [2026-08-25-feature-dedup-identification-design.md](2026-08-25-feature-dedup-identification-design.md).

**Tech Stack:** Java records + generics over `EntityId`; the Anthropic Java SDK tool-use API (`adapters/anthropic-vision`); the `Pages.stream` paging idiom; JUnit + the `Observer`/`NaturalistTestExtension`/`InsectsTestContextInternal` test stack.

## Global Constraints

- **Per-domain isolation.** `FeatureSearch` is instantiated once per domain over that domain's own features. Insect features and plant features never share an index or search. **Insects only** in this plan.
- **Ports & adapters (catalog-style).** Build the `FeatureSearch` port + the in-memory mock only. The production Solr/Postgres implementation is a future `adapters/` module — do NOT build it. The mock is the dev/test runtime, exactly like `InMemoryCatalog`.
- **No new search engine, no persistent index.** The mock is an O(n) token-set Jaccard scan over the corpus. Corpus fetch is a single batched paged stream (`Pages.stream`), never a per-element select (the N+1 no-fan-out gate forbids fan-out).
- **Behavior-preserving prerequisites.** Tasks 2 and 3 change signatures/wiring only; the reuse behavior lands in Task 4. Each task ends green.
- **Kernel signature changes need a clean install** (`mvn clean install -DskipTests`) before verify (stale-class trap).
- **Completeness gate before done:** `mvn install -DskipTests && mvn verify` then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
- **Never weaken a test or gate.** No raw `List<T>` at a query port boundary (return a `Stream`/`BehavioralCollection`); mock finders validate args; preserve the N+1 gate.
- **Insects is the reference domain.**

---

## File Structure

**New — `kernels/feature-search/` (module):**
- `pom.xml` — parent `kernels`, deps `framework` + `identifiers` (needs `EntityId`).
- `src/main/java/com/naturalist/featuresearch/FeatureSearch.java` — the port + `FeatureMatch` record.
- `src/main/java/com/naturalist/featuresearch/FeatureCorpus.java` — the corpus SPI + `Indexed` record.
- `src/main/java/com/naturalist/featuresearch/InMemoryFeatureSearch.java` — the mock.
- `src/test/java/com/naturalist/featuresearch/InMemoryFeatureSearchTest.java` — Jaccard/threshold unit test.

**New — `kernels/vision/`:**
- `src/main/java/com/naturalist/vision/VisionExchange.java` — multi-turn handle.

**Modified:**
- `kernels/vision/.../VisionService.java` — `identify(...)` returns `VisionExchange`.
- `kernels/vision/.../NoOpVisionService.java` — signature update (still throws).
- `adapters/anthropic-vision/.../AnthropicVisionService.java` — implement `VisionExchange`.
- `domains/insects/insects-api/.../InsectQuery.java` — `FeatureQuery.corpus()`.
- `domains/insects/insects-core/.../InsectFeatureQueryImpl.java` — implement `corpus()`.
- `domains/insects/insects-core/.../InsectIdentificationCommand.java` — the reuse-aware flow.
- `domains/insects/insects-console/.../InsectsController.java` — build + pass the mock.
- Test contexts + `InsectIdentificationCommandTest`.
- Root `pom.xml`, `kernels/pom.xml`, `insects-core/pom.xml` — module scaffolding + deps.

---

## Task 1: `kernels/feature-search` — port + in-memory mock

**Files:**
- Create: `kernels/feature-search/pom.xml`, `FeatureSearch.java`, `FeatureCorpus.java`, `InMemoryFeatureSearch.java`, `InMemoryFeatureSearchTest.java`
- Modify: `kernels/pom.xml` (`<modules>`), root `pom.xml` (`<dependencyManagement>`)

**Interfaces:**
- Produces: `FeatureSearch<ID extends EntityId>.findSimilar(String value, int limit) -> List<FeatureMatch<ID>>`; `FeatureMatch<ID>(ID id, String value, double score)`; `FeatureCorpus<ID extends EntityId>.load() -> Stream<Indexed<ID>>`, `Indexed<ID>(ID id, String value)`; `InMemoryFeatureSearch<ID>(FeatureCorpus<ID> corpus)`.

- [ ] **Step 1: Scaffold the module.** Create `kernels/feature-search/pom.xml` mirroring `kernels/catalog/pom.xml` (parent `kernels`, artifactId `feature-search`, name `kernels :: feature-search`), deps: `framework` + `identifiers` (compile), `framework-test` + `identifiers-test` (test). Add `<module>feature-search</module>` to `kernels/pom.xml`. Add the `<dependency>` block (artifactId `feature-search`, `<version>${project.version}</version>`) to root `pom.xml` `<dependencyManagement>` KERNELS section, alphabetically (after `field-notes`/`framework-test`, near `feature-search`'s alpha slot).

- [ ] **Step 2: Write `FeatureSearch.java`**

```java
package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;
import java.util.List;

/**
 * Finds existing features whose text is similar to a candidate value — a
 * per-domain, per-entity-id search seam. The in-memory {@link InMemoryFeatureSearch}
 * backs tests and the dev runtime; a production Solr/Postgres adapter implements the
 * same port later. Each domain wires its own instance over its own features; insect
 * and plant features never share a search.
 */
public interface FeatureSearch<ID extends EntityId> {

    /** Existing features similar to {@code value}, best first, at most {@code limit}. */
    List<FeatureMatch<ID>> findSimilar(String value, int limit);

    record FeatureMatch<ID extends EntityId>(ID id, String value, double score) {}
}
```

- [ ] **Step 3: Write `FeatureCorpus.java`** (the domain-supplied data source, keeps the kernel free of domain deps)

```java
package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;
import java.util.stream.Stream;

/**
 * Supplies the current set of a domain's features to an in-memory search. The
 * domain implements this by streaming its features (batched — never a per-element
 * select). The production adapter has its own index and ignores this SPI.
 */
@FunctionalInterface
public interface FeatureCorpus<ID extends EntityId> {

    Stream<Indexed<ID>> load();

    record Indexed<ID extends EntityId>(ID id, String value) {}
}
```

- [ ] **Step 4: Write `InMemoryFeatureSearch.java`** (normalized token-set Jaccard)

```java
package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory feature search: normalized token-set Jaccard over the corpus, returning
 * the closest existing features above {@link #THRESHOLD}. Dependency-free and
 * gate-safe — a single corpus load then an in-memory scan, no per-feature select.
 */
public final class InMemoryFeatureSearch<ID extends EntityId> implements FeatureSearch<ID> {

    /** Minimum Jaccard overlap to be considered a candidate. Tunable. */
    static final double THRESHOLD = 0.4;
    private static final Pattern SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    private final FeatureCorpus<ID> corpus;

    public InMemoryFeatureSearch(FeatureCorpus<ID> corpus) {
        this.corpus = corpus;
    }

    @Override
    public List<FeatureMatch<ID>> findSimilar(String value, int limit) {
        if (value == null || value.isBlank() || limit <= 0) return List.of();
        Set<String> queryTokens = tokenise(value);
        if (queryTokens.isEmpty()) return List.of();

        try (var entries = corpus.load()) {
            return entries
                    .map(e -> new FeatureMatch<>(e.id(), e.value(),
                            jaccard(queryTokens, tokenise(e.value()))))
                    .filter(m -> m.score() >= THRESHOLD)
                    .sorted(Comparator.comparingDouble(FeatureMatch<ID>::score).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    private static Set<String> tokenise(String text) {
        return Arrays.stream(SPLIT.split(text.toLowerCase()))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toSet());
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        long intersection = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }
}
```

- [ ] **Step 5: Write `InMemoryFeatureSearchTest.java`** (direct kernel test — the scoring is non-orthogonal logic, warranting one per the kernel-test convention)

```java
package com.naturalist.featuresearch;

import com.naturalist.insects.InsectFeatureId;   // a concrete EntityId for the test
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFeatureSearchTest {

    private static InMemoryFeatureSearch<InsectFeatureId> searchOver(String... values) {
        var indexed = List.of(values).stream()
                .map(v -> new FeatureCorpus.Indexed<>(InsectFeatureId.create(), v))
                .toList();
        return new InMemoryFeatureSearch<>(indexed::stream);
    }

    @Test
    void surfacesPunctuationOnlyVariantAsTopMatch() {
        var search = searchOver(
                "dark (black) pronotum contrasting with red elytra",
                "long slender cursorial legs");
        var hits = search.findSimilar("dark/black pronotum contrasting with red elytra", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).value()).isEqualTo("dark (black) pronotum contrasting with red elytra");
        assertThat(hits.get(0).score()).isGreaterThan(0.6);
    }

    @Test
    void dropsBelowThreshold() {
        var search = searchOver("hardened elytra");
        assertThat(search.findSimilar("six short slender black legs", 5)).isEmpty();
    }

    @Test
    void respectsLimitAndOrdersByScoreDescending() {
        var search = searchOver("small head concealed under pronotum",
                "small rounded head partially concealed under pronotum",
                "shiny metallic coloration");
        var hits = search.findSimilar("small head largely concealed beneath pronotum", 1);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).value()).contains("concealed under pronotum");
    }

    @Test
    void emptyOrBlankQueryReturnsNothing() {
        var search = searchOver("hardened elytra");
        assertThat(search.findSimilar("", 5)).isEmpty();
        assertThat(search.findSimilar(null, 5)).isEmpty();
    }
}
```

*Note:* the test depends on `identifiers` (`InsectFeatureId`) as a concrete `EntityId` — `identifiers-test` is already a test-scope dep from Step 1. If `InsectFeatureId` is not reachable (it is in `domains/identifiers`, which `identifiers` module provides), use any available concrete `EntityId` subclass from `identifiers`; confirm the import resolves.

- [ ] **Step 6: Build + test**

Run: `mvn install -DskipTests -pl kernels/feature-search -am && mvn verify -pl kernels/feature-search`
Expected: BUILD SUCCESS, 4/4 tests pass.

- [ ] **Step 7: Commit**

```bash
git add kernels/feature-search kernels/pom.xml pom.xml
git commit -m "feat(feature-search): add FeatureSearch port + in-memory Jaccard mock"
```

---

## Task 2: Multi-turn `VisionService` (`VisionExchange`)

Behavior-preserving signature change: `identify(...)` returns a `VisionExchange` (turn 1); a `respond(...)` continues to turn 2. Existing single-turn callers just read `.result()`.

**Files:**
- Create: `kernels/vision/.../VisionExchange.java`
- Modify: `VisionService.java`, `NoOpVisionService.java`, `AnthropicVisionService.java`, `InsectIdentificationCommand.identifyViaVision`, `InsectIdentificationCommandTest` (+ any other `VisionService` stub)

**Interfaces:**
- Produces: `VisionService.identify(Image, ToolSchema, String) -> VisionExchange`; `VisionExchange.result() -> ToolResult`; `VisionExchange.respond(String toolResultJson, ToolSchema nextTool) -> VisionExchange`.

- [ ] **Step 1: Write `VisionExchange.java`**

```java
package com.naturalist.vision;

/**
 * A tool-use conversation with the vision model. {@link #result()} is the tool call
 * from the latest turn; {@link #respond} sends a tool result back and returns the next
 * turn. Lets the caller run a second turn (e.g. hand the model similar existing
 * features and get a reuse-or-new resolution) without the kernel knowing the
 * provider's conversation mechanics.
 */
public interface VisionExchange {

    ToolResult result();

    /** Send {@code toolResultJson} as the tool result for {@link #result()} and get the next turn, forcing {@code nextTool}. */
    VisionExchange respond(String toolResultJson, ToolSchema nextTool);
}
```

- [ ] **Step 2: Change `VisionService.identify` return type** to `VisionExchange` (was `ToolResult`). Update the javadoc.

- [ ] **Step 3: Update `NoOpVisionService`** — change the `identify` return type to `VisionExchange`; the body still throws `UnsupportedOperationException`/`IllegalStateException` (whatever it throws today), so no return is produced.

- [ ] **Step 4: Implement `VisionExchange` in `AnthropicVisionService`.** Refactor `identify(...)` so the request/response becomes a private helper that hoists the message list into a growing `List<MessageParam>` (or the SDK's message-list builder), issues the request forcing the given tool, extracts the `ToolUseBlock`, and returns a `VisionExchange` implementation (a private inner class) that holds: the `AnthropicClient`, the config, the accumulated message list, and the latest assistant `ToolUseBlock`. `result()` returns `new ToolResult(block.name(), serializeInput(block))`. `respond(toolResultJson, nextTool)` appends the assistant `tool_use` block and a user `tool_result` block (referencing the latest tool_use's id, content = `toolResultJson`) to the message list, re-issues the request forcing `nextTool`, extracts the next `ToolUseBlock`, and returns a new exchange over the extended list. Keep `@Resilient` on the outward method. Reuse the existing `buildTool`, `serializeInput`, `resolveMediaType` helpers. (Read the current `identify` body for the exact SDK calls — `MessageCreateParams.builder()`, `.addUserMessageOfBlockParams`, `.toolChoice(ToolChoice.ofTool(...))`, `message.content().stream().flatMap(b -> b.toolUse().stream())`.)

- [ ] **Step 5: Adapt the one production caller.** In `InsectIdentificationCommand.identifyViaVision` (lines ~159-164), change `var result = visionService.identify(...)` to `var exchange = visionService.identify(...)` and pass `exchange.result()` to `parseResult(...)`. No behavior change (single turn still). *(The second turn is added in Task 4.)*

- [ ] **Step 6: Adapt the test stubs.** In `InsectIdentificationCommandTest`, the `VisionService` stubs are lambdas returning a `ToolResult`. They must now return a `VisionExchange`. Add a small test helper in the test class:

```java
private static VisionExchange fixedExchange(String toolName, String argumentsJson) {
    return new VisionExchange() {
        @Override public ToolResult result() { return new ToolResult(toolName, argumentsJson); }
        @Override public VisionExchange respond(String toolResultJson, ToolSchema nextTool) {
            throw new AssertionError("no second turn expected in this test");
        }
    };
}
```

Rewrite each existing stub lambda `(image, tool, prompt) -> new ToolResult(...)` as `(image, tool, prompt) -> fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON)` (and the `withAlternatives`/prompt-capturing variants accordingly — they keep capturing `prompt`, just wrap the result in `fixedExchange`).

- [ ] **Step 7: Clean-build + verify** (kernel signature change → clean install)

Run: `mvn clean install -DskipTests && mvn verify`
Expected: BUILD SUCCESS across the reactor incl. `adapters/anthropic-vision`, `insects-core`, and the management-console `@SpringBootTest`s (the vision bean still wires). Behavior unchanged.

- [ ] **Step 8: Commit**

```bash
git add kernels/vision adapters/anthropic-vision domains/insects/insects-core
git commit -m "refactor(vision): make VisionService multi-turn via VisionExchange (no behavior change)"
```

---

## Task 3: Insects feature corpus + `FeatureSearch` wiring

Expose the corpus through the public query port and wire the mock into the command's construction. No dedup behavior yet.

**Files:**
- Modify: `InsectQuery.java` (`FeatureQuery.corpus()`), `InsectFeatureQueryImpl.java` (impl), `InsectIdentificationCommand.java` (new ctor param), `InsectsController.java` (build+pass mock), `InsectsTestContextInternal.java`, `InsectIdentificationCommandTest.java`, `insects-core/pom.xml` (dep on `feature-search`)

**Interfaces:**
- Produces: `InsectQuery.FeatureQuery.corpus() -> Stream<InsectFeature>`; `InsectIdentificationCommand(..., FeatureSearch<InsectFeatureId> featureSearch, ...)`.

- [ ] **Step 1: Add the corpus method to the port.** In `InsectQuery.java`, add to the nested `FeatureQuery` interface:

```java
/** Every catalogued feature, streamed for search-corpus assembly (paged; never fans out). */
java.util.stream.Stream<InsectFeature> corpus();
```

- [ ] **Step 2: Implement `corpus()` in `InsectFeatureQueryImpl`** using the paging idiom (single batched stream, gate-safe):

```java
@Override
public Stream<InsectFeature> corpus() {
    return Pages.stream(1000, featureRepository::getPage);
}
```

Add imports `com.naturalist.data.Pages`, `java.util.stream.Stream`. (`featureRepository` is already a field; `getPage(PageRequest)` is inherited from `EntityRepository`. `Pages.stream(int pageSize, Function<PageRequest, Page<E>>)` exists in `kernels/framework`.)

- [ ] **Step 3: Add `feature-search` dependency** to `insects-core/pom.xml` (`<artifactId>feature-search</artifactId>`, no version — from dependencyManagement).

- [ ] **Step 4: Give the command a `FeatureSearch` dependency.** In `InsectIdentificationCommand`, add a `private final FeatureSearch<InsectFeatureId> featureSearch;` field and constructor parameter (place it after `insectQuery`), with a null-check in the existing constructor `Observer.arguments(...)` block. Import `com.naturalist.featuresearch.FeatureSearch`. Do NOT use it yet (Task 4).

- [ ] **Step 5: Build the mock in the controller and pass it.** In `InsectsController` where the command is constructed (~line 119), build the mock from the injected `insectQuery` and pass it:

```java
FeatureSearch<InsectFeatureId> featureSearch =
        new InMemoryFeatureSearch<>(() ->
                insectQuery.features().corpus()
                        .map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value())));
this.identificationCommand = new InsectIdentificationCommand(
        visionService, new NoOpTextGenerationService(), eolAuthority,
        libraryCommand, insectQuery, featureSearch, transaction);
```

Add imports (`com.naturalist.featuresearch.*`, `InsectFeatureId`). *(This mirrors the existing inline `new NoOpTextGenerationService()` construction — the not-yet-real service built at the composition root. When a Solr adapter lands it becomes an injected `FeatureSearch` bean; leave a one-line comment saying so.)*

- [ ] **Step 6: Wire the test paths.** In `InsectsTestContextInternal` (and anywhere it assembles the command / exposes it), and in `InsectIdentificationCommandTest.buildCommand`, construct the same `InMemoryFeatureSearch<>` from the test `InsectQuery` and pass it to the command constructor. In the test, this makes the corpus reflect the `InsectFeatureTestEntitySource` data.

- [ ] **Step 7: Clean-build + verify**

Run: `mvn clean install -DskipTests && mvn verify -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-test-context,apps/management-console -am`
Expected: BUILD SUCCESS; existing identification tests unchanged (search built but unused). N+1 gate green (`corpus()` is one paged stream).

- [ ] **Step 8: Commit**

```bash
git add domains/insects pom.xml
git commit -m "feat(insects): expose feature corpus + wire in-memory FeatureSearch (unused)"
```

---

## Task 4: Reuse-aware resolution in `InsectIdentificationCommand`

**Files:**
- Modify: `InsectIdentificationCommand.java` (flow + `resolveFeatures` + new schema/parse helpers), `InsectIdentificationCommandTest.java`

**Interfaces:**
- Consumes: `featureSearch.findSimilar(...)` (Task 3), `VisionExchange.respond(...)` (Task 2), `InsectQuery.FeatureQuery` (existing features by id when reusing).

- [ ] **Step 1: Search the proposed features and, when candidates exist, run turn 2.** Rework `identifyViaVision` (or `identify` steps 1→4) so it:
  1. Runs turn 1 (`exchange = visionService.identify(image, buildToolSchema(), systemPrompt)`), parses to `InsectIdentificationResult proposed`.
  2. For each proposed feature value, calls `featureSearch.findSimilar(value, 5)`.
  3. If ANY value has ≥1 candidate, builds a tool-result JSON listing, per proposed value, its candidate existing values (Step 3), calls `exchange.respond(candidatesJson, buildResolveToolSchema())`, and parses the resolution (Step 4) into a `Map<String,String> reuse` (proposed-normalized-value → chosen existing value) — values the model marked "new" are absent from the map.
  4. If NO value has candidates, `reuse` is empty (unchanged behavior).
  Thread `reuse` into feature resolution.

- [ ] **Step 2: Add `buildResolveToolSchema()`** — a `ToolSchema` named e.g. `resolve_features` whose parameters are an array of `{ proposed: string, decision: "reuse"|"new", existingValue: string|null }`. Model it on the existing `buildToolSchema()` JSON-literal style. (Concrete JSON in the implementer's discretion, matching the tool-result shape from Step 3.)

- [ ] **Step 3: Add the candidates→tool-result JSON builder.** Given the proposed values with their `findSimilar` candidates, produce a JSON string like `{"features":[{"proposed":"<value>","candidates":["<existing value>", ...]},...]}` (only include proposed values that HAVE candidates, or all — the schema instructs "reuse only if the same trait"). Serialize with the class's existing Jackson `MAPPER`.

- [ ] **Step 4: Add the resolution parser** — parse the turn-2 `ToolResult.argumentsJson()` into `Map<String,String> reuse` (proposed→existingValue) for entries whose `decision == "reuse"`, normalizing both sides with `trim().toLowerCase()`.

- [ ] **Step 5: Change `resolveFeatures` to honor `reuse` + existing catalog.** Update the signature to `resolveFeatures(List<RankFeatures> allRankFeatures, Map<String,String> reuse)`. For each normalized value:
  - If `reuse` maps it to an existing value `v`, resolve the feature id by looking up the existing feature for `v`. Obtain it via a batched pre-fetch: collect all reuse-target values, fetch their `InsectFeature`s once (a by-value lookup). **This needs a by-value fetch** — add `Optional<InsectFeature> findByValue(String value)` to `InsectRepository.FeatureRepository` (+ mock/rdms) and a `FeatureQuery` pass-through, OR reuse `corpus()` filtered in-memory (acceptable at dev scale; the corpus is already streamed). Prefer filtering the already-loaded corpus to avoid a new port method: build a `Map<String,InsectFeatureId> existingByValue` once from `insectQuery.features().corpus()`, and resolve reuse targets against it. A reused value contributes an assignment pointing at the existing id and **no** new `InsectFeature`.
  - Otherwise mint as today (fresh `InsectFeatureId`, add to `newFeatures`). Keep the intra-call `createdFeatures` `HashMap` as the innermost guard.
  Return `FeatureResolution(newFeatures, assignments)` as before.

- [ ] **Step 6: Update `InsectIdentificationCommandTest` with a stateful two-turn stub and cases.** Add a `VisionExchange` test double that returns a scripted turn-1 result and, on `respond(...)`, returns a scripted turn-2 resolution:

```java
private static VisionExchange twoTurn(String turn1Json, String turn2ResolveJson) {
    return new VisionExchange() {
        @Override public ToolResult result() { return new ToolResult("propose_insect_species", turn1Json); }
        @Override public VisionExchange respond(String toolResultJson, ToolSchema nextTool) {
            return fixedExchange("resolve_features", turn2ResolveJson);
        }
    };
}
```

Cases:
  - **(a) No similar candidates → unchanged single-turn behavior.** With the default test corpus (features that don't resemble the sample's), `respond` is never called (assert the sample's features are all minted as new).
  - **(b) Resolved-to-existing → reused id, no new row.** Seed an existing `InsectFeature` whose value is a near-variant of a `turn1Json` feature; script `turn2ResolveJson` to `reuse` that proposed value → the existing value; assert the persisted state links the assignment to the EXISTING feature id and that NO new `InsectFeature` was added for it (inspect via `nte.getNamed(InsectFeatureTestEntitySource.class)` / `InsectFeatureAssignmentTestEntitySource`).
  - **(c) Resolved-new → minted.** Script `turn2` to mark it `new`; assert a new feature row.

- [ ] **Step 7: Full verify + completeness gate**

Run: `mvn clean install -DskipTests && mvn verify`
Then: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS on both; N+1 gate green (corpus is one paged stream, existing-by-value map built once); management-console `@SpringBootTest`s green.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-core
git commit -m "feat(insects): reuse-aware feature identification via search + second vision turn"
```

---

## Self-Review

**Spec coverage:** FeatureSearch port + mock → Task 1; multi-turn vision → Task 2; corpus + wiring → Task 3; search+clarify+resolve flow and `resolveFeatures` change → Task 4. Per-domain isolation (insects-only instance) → Tasks 1/3. Ports-&-adapters (mock now, Solr later) → Task 1 + Task 3 Step 5 comment. Testing (kernel Jaccard test + command two-turn cases) → Tasks 1/4. Slice C, plants, real adapter → out of scope (design doc).

**Placeholder scan:** Task 4 Steps 2-3 leave the exact tool-schema/tool-result JSON to the implementer's discretion (bounded by the parse contract in Steps 3-4) — this is the one deliberate latitude, because the JSON shape must round-trip with the model and is best pinned against the existing `buildToolSchema` style; the parse/consume contract IS concrete. Everything else carries concrete code.

**Type consistency:** `FeatureSearch<InsectFeatureId>` / `FeatureMatch<ID>(ID,String,double)` / `FeatureCorpus.Indexed<ID>(ID,String)` used consistently across Tasks 1/3/4; `VisionExchange.result()/respond(String,ToolSchema)` consistent across Tasks 2/4; `corpus() -> Stream<InsectFeature>` consistent across Tasks 3/4; `resolveFeatures(List<RankFeatures>, Map<String,String>)` is the amended signature used in Task 4.

**Open latitude flagged for the executor:** whether reuse-target resolution uses a filtered `corpus()` map (recommended, no new port method) or a new `findByValue` port method — Task 4 Step 5 recommends the former.
