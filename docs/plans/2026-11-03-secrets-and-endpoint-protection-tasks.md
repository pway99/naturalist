# Secrets and Endpoint Protection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Supply the model credential through a kernel `SecretProvider` port with environment and file sources, select the vision adapter by property, bound identification spend with a bulkhead, a per-client limiter and image downscaling, and write a rehearsed secret-rotation runbook.

**Architecture:** A new zero-dependency `com.naturalist.secrets` package in `kernels/framework` (port, typed name, redacting value, two sources, a recording decorator) is consumed by the two Anthropic adapters and wired at the composition root by `naturalist.secrets.source` and `naturalist.vision.provider`. Spend ceilings stack outermost-first: the existing usage budgets and global rate gate gain a bulkhead in `InsectIdentificationCommandImpl` (a kernel `BulkheadFullException` and a `BUSY` refusal), a `ClientKey`-keyed limiter on the identify routes, and a JDK-only downscale inside the vision adapter. Two build gates (an OpenRewrite `NoSystemGetenv` recipe and an ArchUnit `SecretsComplianceTest`) keep the credential path honest; `/admin/secrets` shows fingerprints only.

**Tech Stack:** Java 25, Maven reactor, Spring Boot 4 (composition root only), JTE, Anthropic Java SDK (`${anthropic-sdk-version}`), Resilience4j behind the kernel `Resilience` facade, `javax.imageio` (JDK), OpenRewrite (`tooling/naturalist-rewrite`), ArchUnit, JUnit 5 + AssertJ.

**Spec:** [ADR-028](../adr/ADR-028-secrets-and-model-spend-protection.md) (decisions, rejected alternatives, review flags) and the design plan [2026-11-02-secrets-and-endpoint-protection-plan.md](2026-11-02-secrets-and-endpoint-protection-plan.md) (survey §1, port design §3, abuse layers §4, hosting §5, rotation runbook §6, sequence §7, settled calls §8). Executors read both.

## Global Constraints

- Java edits go through the Kaname engine `apply_change` atoms (the repo hooks enforce it); YAML, JTE, Markdown and POM edits use Write/Edit. Run `search_text` before any grep.
- No new third-party dependency anywhere. `com.naturalist.secrets` and `ImageDownscaler` use the JDK only. `kernels/framework` keeps its allow-list (Jackson, Commons, Micrometer, JSpecify). Any version literal lives only in the root `pom.xml` `<properties>`.
- Adapters never import Spring. `adapters/anthropic-*` depend on `framework`, their kernel port module(s) and `anthropic-java` only.
- No `System.getenv` outside `com.naturalist.secrets`, with exactly one named exemption: `com.naturalist.persistence.test.RdbmsDataSource` (the `NATURALIST_TEST_*` CI contract).
- No `com.anthropic` import outside `com.naturalist.vision.anthropic` and `com.naturalist.textgeneration.anthropic`.
- `naturalist.vision.provider` has no default anywhere; main `application.yml` carries a comment, not a value. `naturalist.secrets.source` defaults to `environment`.
- A secret value never appears in a log line, an exception message, a template model or an admin page. Only `Secret.fingerprint()` (first eight lowercase hex digits of SHA-256) is ever displayed.
- Strategy names are exact: `vision.identification` (timeout + rate limiter + new bulkhead), `vision.identification.client` (new keyed rate limiter, 2 per minute, `Duration.ZERO` wait). The bulkhead is 2 concurrent, `Duration.ZERO` wait.
- Image bound: longest edge 1568 px, JPEG output, quality 0.85; formats `ImageIO` cannot decode pass through unchanged.
- CSP is exactly `connect-src 'self'; form-action 'self'; frame-ancestors 'none'`. No `default-src`, `script-src`, `style-src` or `frame-src`.
- Never weaken a gate or a test to pass. Where a test asserts a state this plan changes (the empty bulkhead section on `/admin/resilience`), the assertion moves to the new truth.
- Every task ends with the scoped build it names; Tasks 7, 12 and 14 end with the full gate: `mvn verify` then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
- Git is trunk-based on local `main`, no pull requests. Each task's last step stages its files and prints the commit command; Pat commits (or says "commit"). Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Prerequisite for Task 11 only: ADR-027 PRs 1 and 2 (the `keyed-rate-limits` worktree: `RateLimiter.forKey`, `Resilience4jRateLimiter` keyed/LRU, `ClientKey`, the `RemoteIpValve` properties) are on `main`. Tasks 1 to 10 and 12 to 14 do not depend on them.
- Operational step 0 (Console only, no code): dedicated `naturalist-prod` and `naturalist-dev` workspaces, monthly spend limit, scoped keys, fingerprints recorded. It runs in parallel with these tasks and must be in place before any public traffic.

## Review Focus

1. A secret file whose value ends in a newline (every editor and `echo` writes one): the key must still authenticate and the fingerprint must match the operator's `printf | shasum`. Pinned in Task 2 (`FileSecretProviderTest.trailingNewline_isTrimmed`).
2. A PNG with an alpha channel (phone screenshots): the JPEG encoder throws on ARGB rasters unless the image is flattened first. Pinned in Task 10 (`ImageDownscalerTest.argbPng_isFlattenedToJpeg`).
3. A JPEG `ImageIO` cannot decode (CMYK, some progressive files): the identification must proceed with the original bytes, never fail on the resize. Pinned in Task 10 (`ImageDownscalerTest.undecodableJpeg_passesThroughUnchanged`).
4. `naturalist.vision.provider` typed as `Anthropic` or ` anthropic ` (case, whitespace): the deployment must either work or fail naming the allowed values, never boot the no-op silently. Pinned in Task 6 (`VisionPropertiesTest.provider_isTrimmedAndCaseInsensitive`, `VisionPropertiesTest.unknownProvider_isRefusedNamingTheAllowedValues`).
5. An identification whose vision call throws must release its bulkhead permit, or two failures lock every later caller out with `BUSY`. Pinned in Task 8 (`Resilience4jResilienceTest.bulkhead_releasesPermit_whenTheSupplierThrows`).

---

### Task 1: `SecretName` and `Secret`

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/SecretName.java`
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/Secret.java`
- Test: `kernels/framework/src/test/java/com/naturalist/secrets/SecretNameTest.java`
- Test: `kernels/framework/src/test/java/com/naturalist/secrets/SecretTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `public record SecretName(String value)` with `static SecretName of(String value)`, `String environmentVariable()`, `toString()` returning `value`; `public final class Secret` with `static Secret of(String value)`, `String value()`, `String fingerprint()`, `equals`/`hashCode` by value, `toString()` returning `Secret[<fingerprint>]`.

- [ ] **Step 1: Write the failing tests**

```java
class SecretNameTest {
    @Test void kebabCase_isAccepted() {
        assertThat(SecretName.of("model-credential").value()).isEqualTo("model-credential");
    }
    @Test void environmentVariable_isUpperSnake() {
        assertThat(SecretName.of("model-credential").environmentVariable()).isEqualTo("MODEL_CREDENTIAL");
    }
    @Test void toString_isTheValue() {
        assertThat(SecretName.of("challenge-secret")).hasToString("challenge-secret");
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "Anthropic-Key", "anthropic_key", "-leading", "trailing-", "a--b"})
    void nonKebab_isRefused(String bad) {
        assertThatThrownBy(() -> SecretName.of(bad)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("kebab-case");
    }
    @Test void null_isRefused() {
        assertThatThrownBy(() -> SecretName.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

class SecretTest {
    @Test void fingerprint_isFirstEightHexOfSha256() {
        // printf %s "not-a-real-secret" | shasum -a 256 | cut -c1-8
        assertThat(Secret.of("not-a-real-secret").fingerprint()).isEqualTo("ed24bc0b");
    }
    @Test void fingerprint_isStableAcrossInstances() {
        assertThat(Secret.of("not-a-real-secret").fingerprint()).isEqualTo(Secret.of("not-a-real-secret").fingerprint());
    }
    @Test void toString_redactsTheValue() {
        assertThat(Secret.of("not-a-real-secret")).hasToString("Secret[ed24bc0b]");
        assertThat(Secret.of("not-a-real-secret").toString()).doesNotContain("not-a-real");
    }
    @Test void equality_isByValue() {
        assertThat(Secret.of("a")).isEqualTo(Secret.of("a")).hasSameHashCodeAs(Secret.of("a")).isNotEqualTo(Secret.of("b"));
    }
    @ParameterizedTest @ValueSource(strings = {"", "   "})
    void blank_isRefused(String bad) {
        assertThatThrownBy(() -> Secret.of(bad)).isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl kernels/framework test -Dtest='SecretNameTest,SecretTest'`
Expected: compilation failure, `SecretName` and `Secret` do not exist.

- [ ] **Step 3: Implement `SecretName` in `kernels/framework/src/main/java/com/naturalist/secrets/SecretName.java`**

`public record SecretName(String value)`; the compact constructor rejects null or anything not matching `^[a-z0-9]+(-[a-z0-9]+)*$` with `IllegalArgumentException("secret name must be kebab-case: '" + value + "'")`; `environmentVariable()` is `value.toUpperCase(Locale.ROOT).replace('-', '_')`. Javadoc: a typed, kebab-case name shared by every source; the kebab form is the file name, the upper-snake form the environment variable.

- [ ] **Step 4: Implement `Secret` in `kernels/framework/src/main/java/com/naturalist/secrets/Secret.java`**

`public final class Secret` holding a private `String value`; `of` rejects null/blank with `IllegalArgumentException("secret value must not be blank")`; `fingerprint()` hashes the UTF-8 bytes with `MessageDigest.getInstance("SHA-256")` and returns the first eight lowercase hex digits (`HexFormat.of()`); `equals` uses `MessageDigest.isEqual` on the UTF-8 bytes; `hashCode` is `value.hashCode()`; `toString()` is `"Secret[" + fingerprint() + "]"`. No Jackson annotations, no `getValue`. Javadoc states that `value()` is for adapters at construction and that nothing else may hold the string.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q -pl kernels/framework test -Dtest='SecretNameTest,SecretTest'`
Expected: PASS.

- [ ] **Step 6: Stage and print the commit command**

```bash
git add kernels/framework/src/main/java/com/naturalist/secrets kernels/framework/src/test/java/com/naturalist/secrets
git commit -m "feat(secrets): SecretName and redacting Secret value in kernels/framework (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: `SecretProvider` port, `MissingSecretException`, environment and file sources

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/SecretProvider.java`
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/MissingSecretException.java`
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/EnvironmentSecretProvider.java`
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/FileSecretProvider.java`
- Test: `kernels/framework/src/test/java/com/naturalist/secrets/EnvironmentSecretProviderTest.java`
- Test: `kernels/framework/src/test/java/com/naturalist/secrets/FileSecretProviderTest.java`

**Interfaces:**
- Consumes: `SecretName`, `Secret` (Task 1).
- Produces: `public interface SecretProvider { Optional<Secret> lookup(SecretName name); String source(); default Secret require(SecretName name); }`; `public class MissingSecretException extends RuntimeException` with constructor `(SecretName name, String source)` and accessors `name()`, `source()`; `public final class EnvironmentSecretProvider implements SecretProvider` with constructor `(Map<String, String> environment)` and `static EnvironmentSecretProvider fromProcess()`; `public final class FileSecretProvider implements SecretProvider` with constructor `(Path directory)`.

- [ ] **Step 1: Write the failing tests**

```java
class EnvironmentSecretProviderTest {
    private static final SecretName KEY = SecretName.of("model-credential");

    @Test void present_isResolvedFromTheUpperSnakeVariable() {
        var provider = new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret"));
        assertThat(provider.lookup(KEY)).contains(Secret.of("not-a-real-secret"));
        assertThat(provider.require(KEY).fingerprint()).isEqualTo("ed24bc0b");
    }
    @Test void absent_isEmpty() {
        assertThat(new EnvironmentSecretProvider(Map.of()).lookup(KEY)).isEmpty();
    }
    @Test void blank_isTreatedAsAbsent() {
        assertThat(new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "  ")).lookup(KEY)).isEmpty();
    }
    @Test void require_onAbsent_namesTheSecretAndTheSource() {
        assertThatThrownBy(() -> new EnvironmentSecretProvider(Map.of()).require(KEY))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining("model-credential").hasMessageContaining("environment")
                .satisfies(e -> {
                    assertThat(((MissingSecretException) e).name()).isEqualTo(KEY);
                    assertThat(((MissingSecretException) e).source()).isEqualTo("environment");
                });
    }
    @Test void source_isEnvironment() {
        assertThat(new EnvironmentSecretProvider(Map.of()).source()).isEqualTo("environment");
    }
}

class FileSecretProviderTest {
    private static final SecretName KEY = SecretName.of("model-credential");
    @TempDir Path dir;

    @Test void present_isReadFromTheFileNamedAfterTheSecret() throws IOException {
        Files.writeString(dir.resolve("model-credential"), "not-a-real-secret");
        assertThat(new FileSecretProvider(dir).require(KEY)).isEqualTo(Secret.of("not-a-real-secret"));
    }
    @Test void trailingNewline_isTrimmed() throws IOException {
        Files.writeString(dir.resolve("model-credential"), "not-a-real-secret\n");
        assertThat(new FileSecretProvider(dir).require(KEY).fingerprint()).isEqualTo("ed24bc0b");
    }
    @Test void trailingCrLf_isTrimmed() throws IOException {
        Files.writeString(dir.resolve("model-credential"), "not-a-real-secret\r\n");
        assertThat(new FileSecretProvider(dir).require(KEY).value()).isEqualTo("not-a-real-secret");
    }
    @Test void missingFile_isEmpty_andRequireNamesTheDirectory() {
        var provider = new FileSecretProvider(dir);
        assertThat(provider.lookup(KEY)).isEmpty();
        assertThatThrownBy(() -> provider.require(KEY))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining("model-credential").hasMessageContaining("file:" + dir);
    }
    @Test void emptyFile_isTreatedAsAbsent() throws IOException {
        Files.writeString(dir.resolve("model-credential"), "\n");
        assertThat(new FileSecretProvider(dir).lookup(KEY)).isEmpty();
    }
    @Test void aDirectoryInPlaceOfTheFile_isAnError() throws IOException {
        Files.createDirectory(dir.resolve("model-credential"));
        assertThatThrownBy(() -> new FileSecretProvider(dir).lookup(KEY))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("model-credential");
    }
    @Test void source_namesTheDirectory() {
        assertThat(new FileSecretProvider(dir).source()).isEqualTo("file:" + dir);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl kernels/framework test -Dtest='EnvironmentSecretProviderTest,FileSecretProviderTest'`
Expected: compilation failure.

- [ ] **Step 3: Implement the port and the exception**

`SecretProvider`: `lookup`, `source`, and `default Secret require(SecretName name) { return lookup(name).orElseThrow(() -> new MissingSecretException(name, source())); }`. Javadoc mirrors `com.naturalist.notification.EmailSender`: port in the kernel, sources as adapters, resolved once at construction, never per request (ADR-028 rejected alternatives).

`MissingSecretException(SecretName name, String source)`: message `"secret '" + name + "' not found in source '" + source + "'"`; stores both; never carries a value.

- [ ] **Step 4: Implement `EnvironmentSecretProvider`**

Holds an unmodifiable copy of the map; `lookup` reads `environment.get(name.environmentVariable())`, returning empty for null or blank; `source()` is `"environment"`; `fromProcess()` is `new EnvironmentSecretProvider(System.getenv())` (the one legitimate `System.getenv` call, inside this package).

- [ ] **Step 5: Implement `FileSecretProvider`**

`lookup` resolves `directory.resolve(name.value())`; a missing path is empty; a regular file is read as UTF-8 with trailing `\r` and `\n` characters removed (only those, a value may contain inner whitespace), and a result that is then blank is empty; a path that exists but is not a regular file, or an `IOException`, becomes `IllegalStateException("secret file could not be read: " + path, cause)`. `source()` is `"file:" + directory`. Javadoc names the shapes this matches: systemd `$CREDENTIALS_DIRECTORY`, Docker and Kubernetes secret mounts, Cloud Run secret volumes.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `mvn -q -pl kernels/framework test -Dtest='EnvironmentSecretProviderTest,FileSecretProviderTest'`
Expected: PASS.

- [ ] **Step 7: Stage and print the commit command**

```bash
git add kernels/framework/src/main/java/com/naturalist/secrets kernels/framework/src/test/java/com/naturalist/secrets
git commit -m "feat(secrets): SecretProvider port with environment and file sources (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `RecordingSecretProvider` and `SecretResolution`

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/SecretResolution.java`
- Create: `kernels/framework/src/main/java/com/naturalist/secrets/RecordingSecretProvider.java`
- Test: `kernels/framework/src/test/java/com/naturalist/secrets/RecordingSecretProviderTest.java`

**Interfaces:**
- Consumes: `SecretProvider`, `SecretName`, `Secret` (Tasks 1, 2).
- Produces: `public record SecretResolution(SecretName name, String source, String fingerprint, Instant resolvedAt)`; `public final class RecordingSecretProvider implements SecretProvider` with constructor `(SecretProvider delegate, Clock clock)` and `List<SecretResolution> resolutions()` (sorted by name, unmodifiable).

- [ ] **Step 1: Write the failing test**

```java
class RecordingSecretProviderTest {
    private static final SecretName KEY = SecretName.of("model-credential");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    @Test void successfulLookup_isRecordedWithFingerprintAndSource_neverTheValue() {
        var recording = new RecordingSecretProvider(
                new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret")), clock);
        recording.require(KEY);
        assertThat(recording.resolutions()).containsExactly(
                new SecretResolution(KEY, "environment", "ed24bc0b", Instant.parse("2026-11-03T10:00:00Z")));
        assertThat(recording.resolutions().toString()).doesNotContain("not-a-real");
    }
    @Test void absentLookup_recordsNothing_andStillThrowsOnRequire() {
        var recording = new RecordingSecretProvider(new EnvironmentSecretProvider(Map.of()), clock);
        assertThat(recording.lookup(KEY)).isEmpty();
        assertThatThrownBy(() -> recording.require(KEY)).isInstanceOf(MissingSecretException.class);
        assertThat(recording.resolutions()).isEmpty();
    }
    @Test void repeatedLookups_recordOnceAndKeepTheFirstTimestamp() {
        var recording = new RecordingSecretProvider(
                new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret")), clock);
        recording.require(KEY);
        recording.require(KEY);
        assertThat(recording.resolutions()).hasSize(1);
    }
    @Test void source_delegates() {
        assertThat(new RecordingSecretProvider(new EnvironmentSecretProvider(Map.of()), clock).source())
                .isEqualTo("environment");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl kernels/framework test -Dtest=RecordingSecretProviderTest`
Expected: compilation failure.

- [ ] **Step 3: Implement `SecretResolution` and `RecordingSecretProvider`**

`SecretResolution` is a plain record (no value component exists, so nothing can leak). `RecordingSecretProvider` keeps a `ConcurrentHashMap<SecretName, SecretResolution>`; `lookup` delegates and, when present and not yet recorded, stores a resolution stamped from the clock; nothing is logged; `resolutions()` returns the values sorted by `name().value()`. Javadoc: the one place a resolution is recorded, feeding `/admin/secrets`; nothing is logged (ADR-028 "Rotation is a rehearsed procedure").

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -pl kernels/framework verify`
Expected: BUILD SUCCESS (whole kernel, including the existing observability tests).

- [ ] **Step 5: Stage and print the commit command**

```bash
git add kernels/framework/src/main/java/com/naturalist/secrets kernels/framework/src/test/java/com/naturalist/secrets
git commit -m "feat(secrets): RecordingSecretProvider records source and fingerprint for /admin/secrets (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: The Anthropic adapters consume the port

**Files:**
- Modify: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java` (constructor, class javadoc, `buildTool` visibility)
- Modify: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionConfig.java` (javadoc only)
- Modify: `adapters/anthropic-text-generation/src/main/java/com/naturalist/textgeneration/anthropic/AnthropicTextGenerationService.java` (constructor, javadoc)
- Modify: `adapters/anthropic-text-generation/pom.xml` (add `junit-jupiter` and `assertj-core` test dependencies with `${junit-version}` / `${assertj-version}`, copied from `adapters/anthropic-vision/pom.xml`)
- Modify: `kernels/vision/src/main/java/com/naturalist/vision/NoOpVisionService.java` (message)
- Modify: `apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java` (bridge only: pass `EnvironmentSecretProvider.fromProcess()`; Task 6 replaces this class's body)
- Test: `adapters/anthropic-vision/src/test/java/com/naturalist/vision/anthropic/AnthropicVisionServiceTest.java`
- Test: `adapters/anthropic-text-generation/src/test/java/com/naturalist/textgeneration/anthropic/AnthropicTextGenerationServiceTest.java`

**Interfaces:**
- Consumes: `SecretProvider`, `SecretName`, `MissingSecretException`, `EnvironmentSecretProvider` (Tasks 1, 2).
- Produces: `public AnthropicVisionService(AnthropicVisionConfig config, SecretProvider secrets, Resilience resilience)`; `public AnthropicTextGenerationService(AnthropicTextGenerationConfig config, SecretProvider secrets)`; both declare `static final SecretName CREDENTIAL = SecretName.of("model-credential")`; `AnthropicVisionService.buildTool(ToolSchema)` becomes package-private `static`.

- [ ] **Step 1: Write the failing tests**

```java
class AnthropicVisionServiceTest {
    private static SecretProvider withKey() {
        return new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret"));
    }

    @Test void constructs_withAResolvableKey_andNoNetwork() {
        var service = new AnthropicVisionService(AnthropicVisionConfig.defaults(), withKey(), Resilience.noOp());
        assertThat(service).isNotNull();
    }
    @Test void missingKey_failsConstruction_namingTheSecretAndTheSource() {
        assertThatThrownBy(() -> new AnthropicVisionService(
                AnthropicVisionConfig.defaults(), new EnvironmentSecretProvider(Map.of()), Resilience.noOp()))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining("model-credential").hasMessageContaining("environment");
    }
    @Test void apiKeyName_isTheSharedContractName() {
        assertThat(AnthropicVisionService.CREDENTIAL).isEqualTo(SecretName.of("model-credential"));
    }
    @Test void buildTool_mapsNameDescriptionPropertiesAndRequired() {
        var tool = AnthropicVisionService.buildTool(new ToolSchema("propose", "Propose.",
                """
                {"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}
                """));
        assertThat(tool.name()).isEqualTo("propose");
        assertThat(tool.description()).contains("Propose.");
        assertThat(tool.inputSchema().required()).contains(List.of("name"));
        assertThat(tool.inputSchema().properties()).isPresent();
    }
}
```

`AnthropicTextGenerationServiceTest`: the same first three tests against `new AnthropicTextGenerationService(AnthropicTextGenerationConfig.defaults(), secrets)`. The two `inputSchema()` assertions follow the SDK's `Optional` accessors (`required()`, `properties()`); adjust the matcher, not the intent, if the accessor shape differs in `${anthropic-sdk-version}`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl adapters/anthropic-vision,adapters/anthropic-text-generation test`
Expected: compilation failure (no three-argument constructor, no `CREDENTIAL`, `buildTool` private).

- [ ] **Step 3: Rewrite the two constructors**

Each constructor validates the three arguments non-null, resolves `secrets.require(CREDENTIAL)` once, and builds `AnthropicOkHttpClient.builder().apiKey(secret.value()).build()`. Delete the `System.getenv` block and the `IllegalStateException`. Replace the "API key read from `MODEL_CREDENTIAL`" javadoc paragraphs with: the key is supplied by the kernel `SecretProvider` under `model-credential`, resolved once at construction, never from the process environment and never through the SDK's `fromEnv()` (ADR-028). `AnthropicVisionConfig` javadoc: the key comes from `SecretProvider`, not this config.

- [ ] **Step 4: Reword `NoOpVisionService`**

Message: `"VisionService is not configured: set naturalist.vision.provider=anthropic and supply the model-credential secret (see ADR-028)."`

- [ ] **Step 5: Bridge `VisionConfiguration`**

Keep the env-presence switch for now; construct with `new AnthropicVisionService(AnthropicVisionConfig.defaults(), EnvironmentSecretProvider.fromProcess(), resilience)`. Task 6 removes the switch and the `getenv`.

- [ ] **Step 6: Run the tests and the dependent modules**

Run: `mvn -q -pl adapters/anthropic-vision,adapters/anthropic-text-generation,apps/management-console -am verify`
Expected: BUILD SUCCESS; the two new test classes pass; management-console still boots on the bridge.

- [ ] **Step 7: Stage and print the commit command**

```bash
git add adapters/anthropic-vision adapters/anthropic-text-generation kernels/vision apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java
git commit -m "refactor(anthropic): adapters take SecretProvider; no System.getenv in adapters (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: `SecretsConfiguration` selects the source by property

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/secrets/SecretsProperties.java`
- Create: `apps/management-console/src/main/java/com/naturalist/console/secrets/SecretsConfiguration.java`
- Test: `apps/management-console/src/test/java/com/naturalist/console/secrets/SecretsConfigurationTest.java`

**Interfaces:**
- Consumes: `EnvironmentSecretProvider`, `FileSecretProvider`, `RecordingSecretProvider` (Tasks 2, 3).
- Produces: `@ConfigurationProperties("naturalist.secrets") public record SecretsProperties(String source, String directory)` (the compact constructor trims and lowercases `source`, defaulting it to `"environment"` when null or blank); `@Configuration @EnableConfigurationProperties(SecretsProperties.class) class SecretsConfiguration` with `@Bean RecordingSecretProvider secretProvider(SecretsProperties properties)`. Other configurations inject the bean as `SecretProvider`; `AdminSecretsController` (Task 13) injects it as `RecordingSecretProvider`.

- [ ] **Step 1: Write the failing test** (no Spring context, the `EmailConfigurationTest` shape)

```java
class SecretsConfigurationTest {
    private final SecretsConfiguration configuration = new SecretsConfiguration();

    @Test void defaultSource_isTheProcessEnvironment() {
        assertThat(configuration.secretProvider(new SecretsProperties(null, null)).source()).isEqualTo("environment");
    }
    @Test void fileSource_readsTheConfiguredDirectory(@TempDir Path dir) {
        assertThat(configuration.secretProvider(new SecretsProperties("file", dir.toString())).source())
                .isEqualTo("file:" + dir);
    }
    @Test void fileSource_withoutADirectory_refusesToBoot() {
        assertThatThrownBy(() -> configuration.secretProvider(new SecretsProperties("file", " ")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("naturalist.secrets.directory");
    }
    @Test void unknownSource_refusesToBoot_namingTheAllowedValues() {
        assertThatThrownBy(() -> configuration.secretProvider(new SecretsProperties("vault", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.secrets.source").hasMessageContaining("vault")
                .hasMessageContaining("environment").hasMessageContaining("file");
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=SecretsConfigurationTest`
Expected: compilation failure.

- [ ] **Step 3: Implement `SecretsProperties` and `SecretsConfiguration`**

`secretProvider` switches on `properties.source()` (already defaulted and trimmed/lowercased in the record): `environment` → `EnvironmentSecretProvider.fromProcess()`; `file` → `new FileSecretProvider(Path.of(directory))`, or `IllegalStateException("naturalist.secrets.source=file requires naturalist.secrets.directory")` when blank; anything else → `IllegalStateException("naturalist.secrets.source must be 'environment' or 'file' (was '<value>')")`. Wrap the chosen source in `new RecordingSecretProvider(source, Clock.systemUTC())`. Javadoc mirrors `EmailConfiguration`: the single place that knows the source; a managed-store adapter is added here as a third case only when a host cannot inject (ADR-028).

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -pl apps/management-console test -Dtest=SecretsConfigurationTest`
Expected: PASS.

- [ ] **Step 5: Stage and print the commit command**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/secrets apps/management-console/src/test/java/com/naturalist/console/secrets
git commit -m "feat(console): SecretsConfiguration selects the secret source by naturalist.secrets.source (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: `VisionConfiguration` selects the provider by property, fail closed

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/VisionProperties.java`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/VisionConfiguration.java`
- Modify: `apps/management-console/src/test/resources/application.yml` (add `naturalist.vision.provider: none`)
- Modify: `apps/management-console/src/main/resources/application.yml` (comment only, under `naturalist:`)
- Modify: `README.md` ("Running it": the run command and the `MODEL_CREDENTIAL` paragraph)
- Test: `apps/management-console/src/test/java/com/naturalist/console/VisionPropertiesTest.java`
- Test: `apps/management-console/src/test/java/com/naturalist/console/VisionConfigurationTest.java`

**Interfaces:**
- Consumes: `AnthropicVisionService(config, SecretProvider, Resilience)` (Task 4); `SecretProvider` bean (Task 5).
- Produces: `@ConfigurationProperties("naturalist.vision") public record VisionProperties(String provider)`; `VisionConfiguration.visionService(VisionProperties properties, SecretProvider secrets, Resilience resilience)` returning `VisionService`.

- [ ] **Step 1: Write the failing tests**

```java
class VisionPropertiesTest {
    @ParameterizedTest @NullSource @ValueSource(strings = {"", "  "})
    void missingProvider_isRefused_namingTheProperty(String bad) {
        assertThatThrownBy(() -> new VisionProperties(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("naturalist.vision.provider").hasMessageContaining("none").hasMessageContaining("anthropic");
    }
    @Test void provider_isTrimmedAndCaseInsensitive() {
        assertThat(new VisionProperties(" Anthropic ").provider()).isEqualTo("anthropic");
        assertThat(new VisionProperties("NONE").provider()).isEqualTo("none");
    }
    @Test void unknownProvider_isRefusedNamingTheAllowedValues() {
        assertThatThrownBy(() -> new VisionProperties("openai"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openai").hasMessageContaining("none").hasMessageContaining("anthropic");
    }
}

class VisionConfigurationTest {
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final VisionConfiguration configuration = new VisionConfiguration();
    @BeforeEach void setUp() { Metrics.addRegistry(meters); }
    @AfterEach void tearDown() { Metrics.removeRegistry(meters); }

    @Test void none_wiresTheNoOp_andIsObservedAsAStandingViolation() {
        var service = configuration.visionService(new VisionProperties("none"), noSecrets(), Resilience.noOp());
        assertThat(service).isInstanceOf(NoOpVisionService.class);
        assertThat(violations("VisionConfiguration.visionService.provider")).isEqualTo(1);
    }
    @Test void anthropic_withAResolvableKey_wiresTheAdapter() {
        var service = configuration.visionService(new VisionProperties("anthropic"),
                new EnvironmentSecretProvider(Map.of("MODEL_CREDENTIAL", "not-a-real-secret")), Resilience.noOp());
        assertThat(service).isInstanceOf(AnthropicVisionService.class);
        assertThat(violations("VisionConfiguration.visionService.provider")).isZero();
    }
    @Test void anthropic_withNoKey_refusesToBoot_namingPropertySecretAndSource() {
        assertThatThrownBy(() -> configuration.visionService(new VisionProperties("anthropic"), noSecrets(), Resilience.noOp()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.vision.provider").hasMessageContaining("model-credential")
                .hasMessageContaining("environment").hasCauseInstanceOf(MissingSecretException.class);
    }
    private static SecretProvider noSecrets() { return new EnvironmentSecretProvider(Map.of()); }
    private double violations(String name) { /* copy the helper from EmailConfigurationTest: naturalist.invariant.check, outcome=violated, name=<name> */ }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl apps/management-console test -Dtest='VisionPropertiesTest,VisionConfigurationTest'`
Expected: compilation failure.

- [ ] **Step 3: Implement `VisionProperties`**

Compact constructor: null/blank → `IllegalArgumentException("naturalist.vision.provider must be set: none or anthropic")`; otherwise `provider = provider.trim().toLowerCase(Locale.ROOT)`; not `none`/`anthropic` → `IllegalArgumentException("naturalist.vision.provider must be none or anthropic (was '<value>')")`. Javadoc mirrors `AdminProperties`: no default, a forgotten value fails boot rather than silently running the no-op.

- [ ] **Step 4: Rewrite `VisionConfiguration`**

`@Configuration @EnableConfigurationProperties(VisionProperties.class)`; an `Observer.forClass(VisionConfiguration.class, Observer.MonitoringMode.ALWAYS)`; `visionService` observes `isTrue("anthropic".equals(provider), "provider")` at `Level.WARN` (a `none` deployment is a standing dashboard violation, as `EmailConfiguration`'s `smtp`); `none` → `new NoOpVisionService()`; `anthropic` → `new AnthropicVisionService(AnthropicVisionConfig.defaults(), secrets, resilience)`, catching `MissingSecretException` and rethrowing `IllegalStateException("naturalist.vision.provider=anthropic but secret '" + e.name() + "' was not found in source '" + e.source() + "': supply it, or set naturalist.vision.provider=none", e)`. No `System.getenv` remains in the class.

- [ ] **Step 5: Configuration and docs**

Test `application.yml`: under `naturalist:` add `vision:\n    provider: none` with a comment that app tests never reach a vendor, whatever the shell exports. Main `application.yml`: under `naturalist:` add a comment block in the style of the admin-credentials one: `naturalist.vision.provider` is REQUIRED (`none` or `anthropic`), deliberately not set here, supplied at deploy time; `anthropic` needs the `model-credential` secret from `naturalist.secrets.source` (`environment`, read from `MODEL_CREDENTIAL`, or `file` with `naturalist.secrets.directory`). README "Running it": the `java -jar` command gains `--naturalist.vision.provider=none`; replace the "Set `MODEL_CREDENTIAL` to enable identification" paragraph with: to enable identification pass `--naturalist.vision.provider=anthropic` and supply the key as `MODEL_CREDENTIAL` (the default `environment` source) or as a file `<dir>/model-credential` with `--naturalist.secrets.source=file --naturalist.secrets.directory=<dir>`. Note for Pat: the gitignored `.claude/launch.json` run configuration needs the same flag.

- [ ] **Step 6: Run the module, then prove selection is by property**

Run: `mvn -q -pl apps/management-console verify`
Expected: BUILD SUCCESS.
Run: `MODEL_CREDENTIAL=not-a-real-secret mvn -q -pl apps/management-console verify`
Expected: BUILD SUCCESS with no network call (the no-op is wired because the test yml says `none`; before this task the exported variable would have selected the real adapter).

- [ ] **Step 7: Stage and print the commit command**

```bash
git add apps/management-console README.md
git commit -m "feat(console): naturalist.vision.provider selects the vision adapter, no default, fail closed (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Gates — `NoSystemGetenv` and `NoNamedCredentials` recipes, `SecretsComplianceTest`

**Files:**
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSystemGetenv.java`
- Modify: `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml` (add the recipe to `com.naturalist.EnforceArchitecture`, extend its description)
- Create: `apps/management-console/src/test/java/com/naturalist/console/architecture/SecretsComplianceTest.java`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSystemGetenvTest.java`

**Interfaces:**
- Consumes: the existing recipe pattern (`NoCachedTestEntitySourceField`, `RewriteTest` with `srcMainJava`).
- Produces: recipe `com.naturalist.rewrite.NoSystemGetenv` with `public static final String MESSAGE = "read the process environment outside com.naturalist.secrets; resolve the value through SecretProvider (ADR-028)"`; ArchUnit class `SecretsComplianceTest`.

- [ ] **Step 1: Write the failing recipe test**

```java
class NoSystemGetenvTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) { spec.recipe(new NoSystemGetenv()); }

    @Test void flagsGetenvOutsideTheSecretsPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k() { return System.getenv("MODEL_CREDENTIAL"); } }
            """,
            """
            package com.naturalist.console;
            class C { String k() { return /*~~(%s)~~>*/System.getenv("MODEL_CREDENTIAL"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }
    @Test void flagsTheNoArgumentForm() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.console;
            import java.util.Map;
            class C { Map<String, String> all() { return System.getenv(); } }
            """,
            """
            package com.naturalist.console;
            import java.util.Map;
            class C { Map<String, String> all() { return /*~~(%s)~~>*/System.getenv(); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }
    @Test void doesNotFlagTheSecretsPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.secrets;
            class EnvironmentSecretProvider { String k() { return System.getenv("X"); } }
            """)));
    }
    @Test void doesNotFlagTheStandingTestDatabaseWiring() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.persistence.test;
            final class RdbmsDataSource { static String env(String k) { return System.getenv(k); } }
            """)));
    }
    @Test void theExemptionIsPerClass_notPerPackage() {
        rewriteRun(srcMainJava(java(
            """
            package com.naturalist.persistence.test;
            class Other { String k() { return System.getenv("X"); } }
            """,
            """
            package com.naturalist.persistence.test;
            class Other { String k() { return /*~~(%s)~~>*/System.getenv("X"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }
    @Test void enforceArchitectureComposite_includesTheRecipe() {
        rewriteRun(spec -> spec.recipeFromResources("com.naturalist.EnforceArchitecture"),
            srcMainJava(java(
            """
            package com.naturalist.console;
            class C { String k() { return System.getenv("X"); } }
            """,
            """
            package com.naturalist.console;
            class C { String k() { return /*~~(%s)~~>*/System.getenv("X"); } }
            """.formatted(NoSystemGetenv.MESSAGE))));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSystemGetenvTest`
Expected: compilation failure.

- [ ] **Step 3: Implement the recipe and register it**

`NoSystemGetenv extends Recipe`: `MethodMatcher MATCHER = new MethodMatcher("java.lang.System getenv(..)")`; visitor under `Preconditions.check(new UsesMethod<>(MATCHER), ...)`; in `visitMethodInvocation`, when `MATCHER.matches(mi)` and the enclosing compilation unit's package does not start with `com.naturalist.secrets` and the enclosing top-level class's fully qualified name is not in `EXEMPT_CLASSES = Set.of("com.naturalist.persistence.test.RdbmsDataSource")`, return `SearchResult.found(mi, MESSAGE)`. The constant carries a comment: that class reads `NATURALIST_TEST_JDBC_URL` / `_DB_USER` / `_DB_PASSWORD`, the standing-test-database contract in `ci.yml`, not a deployed secret (design plan §8 item 5). `getDisplayName`/`getDescription` state the rule and the exemption. In `naturalist.yml` add `- com.naturalist.rewrite.NoSystemGetenv` to the `EnforceArchitecture` `recipeList` and extend its description with "and never read the process environment outside com.naturalist.secrets".

- [ ] **Step 4: Run the recipe tests**

Run: `mvn -q -pl tooling/naturalist-rewrite verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Write the ArchUnit gate**

```java
@AnalyzeClasses(packages = "com.naturalist", importOptions = ImportOption.DoNotIncludeTests.class)
class SecretsComplianceTest {
    @ArchTest
    static final ArchRule onlyTheAnthropicAdaptersTouchTheSdk =
            noClasses()
                    .that().resideOutsideOfPackages("com.naturalist.vision.anthropic", "com.naturalist.textgeneration.anthropic")
                    .should().dependOnClassesThat().resideInAPackage("com.anthropic..")
                    .because("the vendor SDK, and therefore the credential, is confined to adapters/anthropic-* — see ADR-028");
}
```

Javadoc: sibling of `ResilienceComplianceTest`; the `System.getenv` rule is the repo-wide OpenRewrite recipe because the console classpath does not carry `adapters/anthropic-text-generation`.

- [ ] **Step 6: Run the full gate**

Run: `mvn -q -pl apps/management-console verify`
Expected: BUILD SUCCESS, `SecretsComplianceTest` green.
Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS. If the dry run flags a `System.getenv` this plan did not survey, that is a finding: move it onto the port, do not widen the exemption.

- [ ] **Step 7: Stage and print the commit command**

```bash
git add tooling/naturalist-rewrite apps/management-console/src/test/java/com/naturalist/console/architecture/SecretsComplianceTest.java
git commit -m "build(gates): NoSystemGetenv and NoNamedCredentials recipes in EnforceArchitecture; SecretsComplianceTest confines com.anthropic (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Kernel `BulkheadFullException`, translated by the Resilience4j adapter

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/resilience/BulkheadFullException.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/resilience/Bulkhead.java` (javadoc)
- Modify: `adapters/resilience-resilience4j/src/main/java/com/naturalist/resilience/resilience4j/Resilience4jBulkhead.java`
- Test: `adapters/resilience-resilience4j/src/test/java/com/naturalist/resilience/resilience4j/Resilience4jResilienceTest.java` (`bulkhead_rejectsBeyondCapacity` plus one new test)

**Interfaces:**
- Consumes: `Bulkhead`, `RateLimitExceededException` as the shape to mirror.
- Produces: `public class BulkheadFullException extends RuntimeException` with constructors `(String message)` and `(String message, @Nullable String strategyName)` and `strategyName()`.

- [ ] **Step 1: Change the existing test and add the release test**

In `bulkhead_rejectsBeyondCapacity`, replace `.hasCauseInstanceOf(BulkheadFullException.class)` (the `io.github.resilience4j.bulkhead` import) with `.hasCauseInstanceOf(com.naturalist.resilience.BulkheadFullException.class)` and add `.cause().hasMessageContaining("narrow")`; drop the vendor import. Add:

```java
@Test
void bulkhead_releasesPermit_whenTheSupplierThrows() {
    Resilience resilience = new Resilience4jResilience(List.of(new BulkheadConfig("narrow", 1, Duration.ZERO)));
    Bulkhead bulkhead = resilience.bulkhead("narrow");
    assertThatThrownBy(() -> bulkhead.execute(() -> { throw new IllegalStateException("vision failed"); }))
            .isInstanceOf(IllegalStateException.class);
    assertThat(bulkhead.execute(() -> "next")).isEqualTo("next");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -pl adapters/resilience-resilience4j test -Dtest=Resilience4jResilienceTest`
Expected: compilation failure on the kernel type.

- [ ] **Step 3: Implement the kernel exception and the translation**

`BulkheadFullException` mirrors `RateLimitExceededException` field for field, javadoc included (kernel-owned so core catches one type; the adapter translates its vendor rejection). `Resilience4jBulkhead.execute` (both overloads) catches `io.github.resilience4j.bulkhead.BulkheadFullException` and throws `new BulkheadFullException("Bulkhead full for strategy '" + delegate.getName() + "'", delegate.getName())`. `Bulkhead` javadoc: rejection surfaces as the kernel `BulkheadFullException`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q -pl kernels/framework,adapters/resilience-resilience4j -am verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Stage and print the commit command**

```bash
git add kernels/framework/src/main/java/com/naturalist/resilience adapters/resilience-resilience4j
git commit -m "feat(resilience): kernel BulkheadFullException; Resilience4j adapter translates its rejection

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Bulkhead around the identification, `BUSY` refusal, controller and page

**Files:**
- Modify: `kernels/identification/src/main/java/com/naturalist/identification/IdentificationRefused.java` (add `Reason.BUSY`)
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectIdentificationCommandImpl.java` (field, constructor, `identify`, javadoc)
- Modify: `apps/management-console/src/main/java/com/naturalist/console/resilience/ResilienceConfiguration.java` (new `BulkheadConfig` bean + javadoc bullet)
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/InsectsController.java` (`case BUSY`)
- Modify: `domains/insects/insects-console/src/main/jte/insects/identify.jte` (`BUSY` message)
- Modify: `apps/management-console/src/test/java/com/naturalist/console/admin/AdminResilienceControllerWebMvcTest.java` (bulkhead section now lists `vision.identification`)
- Test: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectIdentificationCommandImplTest.java` (new test + `RejectingBulkheadResilience`)
- Test: `domains/insects/insects-console/src/test/java/com/naturalist/insects/InsectsControllerBusyTest.java`

**Interfaces:**
- Consumes: `BulkheadFullException`, `Bulkhead` (Task 8); `Resilience.bulkhead(String)`.
- Produces: `IdentificationRefused.Reason.BUSY`; the bulkhead permit for `vision.identification` taken in `InsectIdentificationCommandImpl.identify` after the rate gate and around budget reservation, upload storage and cataloguing; `redirect:/insects/identify?limit=BUSY`.

- [ ] **Step 1: Write the failing core test**

Add to `InsectIdentificationCommandImplTest`, beside `identify_rateGateRuns_beforeBudgetReservation_andBeforeVision`:

```java
@Test
void identify_bulkheadRejection_refusesBusy_beforeBudgetStoreAndVision() throws IOException {
    var visionCalled = new AtomicBoolean(false);
    VisionService trackingVision = (image, tool, prompt) -> { visionCalled.set(true); return fixedExchange("propose_insect_species", SAMPLE_RESULT_JSON); };
    var budgetReserveCalls = new AtomicInteger(0);
    IdentificationBudget trackingBudget = naturalist -> budgetReserveCalls.incrementAndGet();
    var cmd = buildCommand(new RejectingBulkheadResilience(), trackingBudget, trackingVision);

    assertThatThrownBy(() -> cmd.identify(ImageUpload.of(JPEG), new ImageMetadata("Chico, CA", null), NaturalistName.of("pat"), null))
            .isInstanceOfSatisfying(IdentificationRefused.class, refused ->
                    assertThat(refused.reason()).isEqualTo(IdentificationRefused.Reason.BUSY));

    assertThat(budgetReserveCalls.get()).as("budget.reserve() must not run once the bulkhead rejects").isZero();
    assertThat(visionCalled.get()).isFalse();
    assertThat(filesUnder(imageRoot)).as("nothing is stored for a refused identification").isEmpty();
}
```

(`imageRoot` is the test's existing image-store temp directory, the one `store` writes to; `filesUnder`, `fixedExchange`, `SAMPLE_RESULT_JSON`, `JPEG` and `buildCommand` already exist in the class.)

`RejectingBulkheadResilience` mirrors `RejectingRateLimiterResilience` (every primitive no-op) except `bulkhead(name)` returns a `Bulkhead` whose both `execute` overloads throw `new BulkheadFullException("Bulkhead full for strategy 'vision.identification'", "vision.identification")`, and `bulkheadNames()` returns `Set.of("vision.identification")`. Also extend the existing rate-gate test with a tracking bulkhead whose `execute` counts entries, asserting it is never entered on the rejected second call (rate gate before bulkhead).

- [ ] **Step 2: Write the failing console test**

`InsectsControllerBusyTest` is a copy of `InsectsControllerRateLimitedTest` whose `Resilience` rejects on `bulkhead` instead of `rateLimiter`, with one test:

```java
@Test
void identify_redirectsWithLimitBusy_whenTheBulkheadRejects() throws IOException {
    var request = new MockHttpServletRequest();
    request.setAttribute("naturalist.currentNaturalistName", "pat-way");
    var image = new MockMultipartFile("image", "test.jpg", "image/jpeg", JPEG_BYTES);
    assertThat(controller().identify(image, null, null, null, request)).isEqualTo("redirect:/insects/identify?limit=BUSY");
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `mvn -q -pl domains/insects/insects-core,domains/insects/insects-console test -Dtest='InsectIdentificationCommandImplTest,InsectsControllerBusyTest'`
Expected: compilation failure on `Reason.BUSY`.

- [ ] **Step 4: Implement**

`IdentificationRefused.Reason` gains `BUSY` with a one-line javadoc (the concurrency bulkhead was full; try again in seconds). `InsectIdentificationCommandImpl`: field `private final Bulkhead bulkhead` set from `resilience.bulkhead(RATE_STRATEGY)`; move steps 0b and 0c into `private InsectRankName reserveStoreAndCatalogue(ImageUpload, ImageMetadata, NaturalistName, @Nullable String notes)`; `identify` keeps 0a, then:

```java
// 0b. BULKHEAD -- at most two identifications in flight, the permit held around the whole
// identification (budget reservation, upload, vision turns, catalog write) so a rejected
// caller burns no budget and the count is of identifications, not vision turns.
try {
    return bulkhead.execute(() -> reserveStoreAndCatalogue(upload, metadata, naturalist, notes));
} catch (BulkheadFullException full) {
    throw new IdentificationRefused(IdentificationRefused.Reason.BUSY, null, null);
}
```

Update the class javadoc's gate list (rate gate → bulkhead → budget → store). `ResilienceConfiguration`: `@Bean BulkheadConfig visionBulkhead() { return new BulkheadConfig("vision.identification", 2, Duration.ZERO); }` and a javadoc sentence under the `vision.identification` bullet (two concurrent identifications, zero wait, taken in the core beside the rate gate; one more than a single honest user needs). `InsectsController`: `case BUSY -> "redirect:/insects/identify?limit=BUSY";`. `identify.jte`: before the `@else`, `@elseif("BUSY".equals(limit))` rendering `<p role="alert">Two identifications are already running. Please wait a few seconds and try again.</p>`. `AdminResilienceControllerWebMvcTest`: `assertThat(bulkheadSection).contains("vision.identification");` replaces the `None registered.` assertion, and the class javadoc's "bulkhead unused" sentence is corrected. If the Cloudflare PR 2 `ResilienceConfigurationTest` exists on `main` by now, add `vision.identification` to its expected bulkhead names.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q -pl kernels/identification,domains/insects/insects-core,domains/insects/insects-console,apps/management-console -am verify`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Stage and print the commit command**

```bash
git add kernels/identification domains/insects/insects-core domains/insects/insects-console apps/management-console
git commit -m "feat(insects): bulkhead of two identifications in the core; BUSY refusal and page notice (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Vision adapter downscales decodable images before base64

**Files:**
- Create: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/ImageDownscaler.java`
- Modify: `adapters/anthropic-vision/src/main/java/com/naturalist/vision/anthropic/AnthropicVisionService.java` (`identify` steps 1 and 2; class javadoc)
- Test: `adapters/anthropic-vision/src/test/java/com/naturalist/vision/anthropic/ImageDownscalerTest.java`

**Interfaces:**
- Consumes: nothing new (`javax.imageio`, `java.awt.image`).
- Produces: package-private `final class ImageDownscaler` with `static final int MAX_EDGE = 1568`, `record Sized(byte[] bytes, String mediaType)`, and `static Sized fit(byte[] bytes, String mediaType)`.

- [ ] **Step 1: Write the failing test**

```java
class ImageDownscalerTest {
    @Test void largePng_isDownscaledToTheLongestEdge_asJpeg() throws IOException {
        var sized = ImageDownscaler.fit(png(4000, 3000, BufferedImage.TYPE_INT_RGB), "image/png");
        var out = ImageIO.read(new ByteArrayInputStream(sized.bytes()));
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
        assertThat(out.getWidth()).isEqualTo(1568);
        assertThat(out.getHeight()).isEqualTo(1176);
        assertThat(sized.bytes()).startsWith((byte) 0xFF, (byte) 0xD8);
    }
    @Test void portrait_isScaledOnItsLongestEdge() throws IOException {
        var sized = ImageDownscaler.fit(png(1000, 4000, BufferedImage.TYPE_INT_RGB), "image/png");
        var out = ImageIO.read(new ByteArrayInputStream(sized.bytes()));
        assertThat(out.getHeight()).isEqualTo(1568);
        assertThat(out.getWidth()).isEqualTo(392);
    }
    @Test void argbPng_isFlattenedToJpeg() throws IOException {
        var sized = ImageDownscaler.fit(png(3000, 3000, BufferedImage.TYPE_INT_ARGB), "image/png");
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
        assertThat(ImageIO.read(new ByteArrayInputStream(sized.bytes())).getWidth()).isEqualTo(1568);
    }
    @Test void smallJpeg_passesThroughUnchanged() throws IOException {
        byte[] jpeg = jpeg(800, 600);
        var sized = ImageDownscaler.fit(jpeg, "image/jpeg");
        assertThat(sized.bytes()).isSameAs(jpeg);
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
    }
    @Test void exactlyTheLimit_passesThroughUnchanged() throws IOException {
        byte[] jpeg = jpeg(1568, 1000);
        assertThat(ImageDownscaler.fit(jpeg, "image/jpeg").bytes()).isSameAs(jpeg);
    }
    @Test void webp_passesThroughUnchanged() {
        byte[] webp = new byte[64];
        System.arraycopy("RIFF".getBytes(US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(US_ASCII), 0, webp, 8, 4);
        var sized = ImageDownscaler.fit(webp, "image/webp");
        assertThat(sized.bytes()).isSameAs(webp);
        assertThat(sized.mediaType()).isEqualTo("image/webp");
    }
    @Test void undecodableJpeg_passesThroughUnchanged() {
        byte[] broken = new byte[64];
        broken[0] = (byte) 0xFF; broken[1] = (byte) 0xD8; broken[2] = (byte) 0xFF;
        assertThat(ImageDownscaler.fit(broken, "image/jpeg").bytes()).isSameAs(broken);
    }
    // helpers: png(w, h, type) and jpeg(w, h) render a BufferedImage with a gradient and encode it with ImageIO.write
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl adapters/anthropic-vision test -Dtest=ImageDownscalerTest`
Expected: compilation failure.

- [ ] **Step 3: Implement `ImageDownscaler.fit`**

Decode with `ImageIO.read(new ByteArrayInputStream(bytes))`; a `null` result or any `IOException`/`RuntimeException` from the decoder returns `new Sized(bytes, mediaType)` with the same array. If `max(width, height) <= MAX_EDGE`, return the same. Otherwise compute the scale so the longest edge is `MAX_EDGE` (round the other edge), draw the source onto a new `BufferedImage(TYPE_INT_RGB)` pre-filled white (flattens alpha) with `RenderingHints.KEY_INTERPOLATION` = `VALUE_INTERPOLATION_BILINEAR`, encode through `ImageIO.getImageWritersByFormatName("jpeg")` with `JPEGImageWriteParam` compression quality `0.85f`, and return `new Sized(out, "image/jpeg")`. Javadoc: the vendor's effective maximum is 1568 px on the longest edge (ADR-028 spend layer 5); JPEG, PNG and GIF are decodable, WebP is not and passes through bounded only by `ImageUpload.MAX_BYTES`; EXIF orientation stays parked (PL-17); no image library is added.

- [ ] **Step 4: Hook it into `identify`**

Step 1 becomes `var sized = ImageDownscaler.fit(image.bytes(), image.mediaType());` with base64 over `sized.bytes()`; step 2 resolves the media type from `sized.mediaType()`. Add a class-javadoc paragraph "Image bound".

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q -pl adapters/anthropic-vision verify`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Stage and print the commit command**

```bash
git add adapters/anthropic-vision
git commit -m "feat(anthropic-vision): downscale decodable images to 1568 px JPEG before base64 (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: `IdentifyRateLimitFilter`, two per minute per client address

**Prerequisite:** ADR-027 PRs 1 and 2 are on `main` (`RateLimiter.forKey`, `ClientKey`, `RemoteIpValve` properties). Do not start this task before; before PR 1 every request keys to the connector and this limiter becomes a global lockout (ADR-028 spend layer 4).

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/IdentifyRateLimitFilter.java`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/SecurityConfiguration.java` (register the filter)
- Modify: `apps/management-console/src/main/java/com/naturalist/console/resilience/ResilienceConfiguration.java` (new `RateLimiterConfig` bean + javadoc bullet)
- Modify: `apps/management-console/src/test/java/com/naturalist/console/admin/AdminResilienceControllerWebMvcTest.java` (rate limiter section lists `vision.identification.client`)
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/IdentifyRateLimitFilterTest.java`

**Interfaces:**
- Consumes: `RateLimiter.forKey(String)`, `ClientKey.of(HttpServletRequest)`, `LoginRateLimitFilter` as the shape to mirror.
- Produces: `@Resilient(name = IdentifyRateLimitFilter.STRATEGY) public final class IdentifyRateLimitFilter extends OncePerRequestFilter` with `static final String STRATEGY = "vision.identification.client"`, constructor `(Resilience resilience)`; registered after Spring Security's `AuthorizationFilter`.

- [ ] **Step 1: Write the failing test** (the `LoginRateLimitFilterTest` shape, no Spring context)

```java
class IdentifyRateLimitFilterTest {
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private IdentifyRateLimitFilter filter;

    @BeforeEach void setUp() {
        Resilience resilience = new Resilience4jResilience(List.of(
                new RateLimiterConfig(IdentifyRateLimitFilter.STRATEGY, 2, Duration.ofMinutes(1), Duration.ZERO)));
        filter = new IdentifyRateLimitFilter(resilience);
        Metrics.addRegistry(meters);
    }
    @AfterEach void tearDown() { Metrics.removeRegistry(meters); }

    @Test void thirdIdentifyFromOneAddress_is429_andNeverReachesTheChain() throws Exception {
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
        var refused = identify("10.0.0.1", "/insects/identify");
        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refused.getContentAsString()).contains("try again");
        assertThat(violations("IdentifyRateLimitFilter.doFilterInternal.withinClientRate")).isEqualTo(1);
    }
    @Test void anotherAddress_hasItsOwnBudget() throws Exception {
        identify("10.0.0.1", "/insects/identify"); identify("10.0.0.1", "/insects/identify"); identify("10.0.0.1", "/insects/identify");
        assertThat(identify("10.0.0.2", "/insects/identify").getStatus()).isEqualTo(200);
    }
    @Test void plantsIdentify_sharesTheSameClientBudget() throws Exception {
        identify("10.0.0.1", "/insects/identify"); identify("10.0.0.1", "/plants/identify");
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(429);
    }
    @Test void otherRequests_areNotCounted() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(send("GET", "10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
            assertThat(send("POST", "10.0.0.1", "/insects/battus-philenor/images").getStatus()).isEqualTo(200);
        }
        assertThat(identify("10.0.0.1", "/insects/identify").getStatus()).isEqualTo(200);
        assertThat(violations("IdentifyRateLimitFilter.doFilterInternal.withinClientRate")).isZero();
    }
    // identify(addr, path) = send("POST", addr, path); send builds a MockHttpServletRequest with
    // setServletPath and setRemoteAddr and runs filter.doFilter with a MockFilterChain.
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=IdentifyRateLimitFilterTest`
Expected: compilation failure.

- [ ] **Step 3: Implement the filter and register it**

`doFilterInternal` passes through unless the method is `POST` and the servlet path matches `Pattern.compile("/[a-z]+/identify")`; otherwise it takes `perClient.forKey(ClientKey.of(request))`, observes `withinClientRate` at `Level.WARN` (`MonitoringMode.ALWAYS`), logs the client key at `WARNING` on rejection (never a metric tag), and answers `429`, `Retry-After: 60`, `text/plain;charset=UTF-8`, `"Too many identification requests from your address — please wait a minute and try again."`. Javadoc: two a minute per address, deliberately below the global three a minute so one address cannot hold every shared slot; client identity is ADR-027 D4; the global `vision.identification` gate and the bulkhead stay in the core. `SecurityConfiguration`: `.addFilterAfter(new IdentifyRateLimitFilter(resilience), AuthorizationFilter.class)` with a comment that only requests that passed authentication, CSRF and the `VISION` check spend a client permit. `ResilienceConfiguration`: `@Bean RateLimiterConfig visionClientRateLimit() { return new RateLimiterConfig("vision.identification.client", 2, Duration.ofMinutes(1), Duration.ZERO); }` plus a javadoc bullet. `AdminResilienceControllerWebMvcTest`: the rate limiter section also `.contains("vision.identification.client")`.

- [ ] **Step 4: Run the module**

Run: `mvn -q -pl apps/management-console verify`
Expected: BUILD SUCCESS; `InsectsRouteSecurityWebMvcTest` unchanged and green (its single `VISION` POST spends one permit).

- [ ] **Step 5: Stage and print the commit command**

```bash
git add apps/management-console
git commit -m "feat(console): per-client limiter on the identify routes, 2/min per address (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Content-Security-Policy header

**Files:**
- Modify: `apps/management-console/src/main/java/com/naturalist/console/SecurityConfiguration.java` (`headers`)
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/SecurityHeadersWebMvcTest.java`

**Interfaces:**
- Consumes: the existing `headers(...)` block (referrer policy).
- Produces: every response carries `Content-Security-Policy: connect-src 'self'; form-action 'self'; frame-ancestors 'none'`; `X-Content-Type-Options: nosniff` (Spring Security's default, now pinned).

- [ ] **Step 1: Write the failing test** (`@SpringBootTest`, the `InsectsRouteSecurityWebMvcTest` setup)

```java
@Test void everyPage_carriesTheConnectFormAndFrameDirectives_andNothingStricter() throws Exception {
    for (var path : List.of("/", "/login", "/insects/orders")) {
        var csp = mockMvc.perform(get(path)).andReturn().getResponse().getHeader("Content-Security-Policy");
        assertThat(csp).as(path).isEqualTo("connect-src 'self'; form-action 'self'; frame-ancestors 'none'");
        assertThat(csp).doesNotContain("default-src", "script-src", "style-src", "frame-src");
    }
}
@Test void identifyPage_carriesThePolicy_andNosniff() throws Exception {
    var response = mockMvc.perform(get("/insects/identify").with(user("naturalist").authorities(
            new SimpleGrantedAuthority("ROLE_NATURALIST"), new SimpleGrantedAuthority("VISION")))).andReturn().getResponse();
    assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("connect-src 'self'; form-action 'self'; frame-ancestors 'none'");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=SecurityHeadersWebMvcTest`
Expected: FAIL, the CSP header is null.

- [ ] **Step 3: Add the header**

In the `headers` lambda add `.contentSecurityPolicy(csp -> csp.policyDirectives("connect-src 'self'; form-action 'self'; frame-ancestors 'none'"))` with a comment: page scripts cannot reach a vendor host and forms cannot post elsewhere; deliberately not `default-src`, the templates use inline styles and scripts and ADR-027 loads Turnstile's script and frame; a stricter policy is a template effort of its own (design plan §8 item 1).

- [ ] **Step 4: Run the full gate**

Run: `mvn verify`
Expected: BUILD SUCCESS.
Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Stage and print the commit command**

```bash
git add apps/management-console
git commit -m "feat(console): Content-Security-Policy connect-src/form-action/frame-ancestors (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 13: `/admin/secrets` shows fingerprints only

> **Withdrawn 2026-11-03 (Pat).** Implemented, then removed before commit together with Task 3's `RecordingSecretProvider` and `SecretResolution`: the external store and a post-deactivation smoke test already prove which credential is live, and the page put a credential derivative and the secret directory's path into a UI. ADR-028 records it as a rejected alternative.

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/admin/AdminSecretsController.java`
- Create: `apps/management-console/src/main/jte/admin/secrets.jte`
- Modify: `apps/management-console/src/main/jte/admin/nav.jte` (add a `secrets` entry after `usage`)
- Test: `apps/management-console/src/test/java/com/naturalist/console/admin/AdminSecretsControllerWebMvcTest.java`

**Interfaces:**
- Consumes: `RecordingSecretProvider.resolutions()`, `SecretResolution` (Task 3); the `RecordingSecretProvider` bean (Task 5).
- Produces: `GET /admin/secrets` (behind `ADMIN` via the existing `/admin/**` rule) rendering `admin/secrets` with model attribute `resolutions` (`List<SecretResolution>`).

- [ ] **Step 1: Write the failing test**

```java
@SpringBootTest
class AdminSecretsControllerWebMvcTest {
    static final Path DIR;
    static { try { DIR = Files.createTempDirectory("secrets"); Files.writeString(DIR.resolve("smoke-secret"), "not-a-real-secret\n"); }
             catch (IOException e) { throw new UncheckedIOException(e); } }

    @DynamicPropertySource
    static void fileSource(DynamicPropertyRegistry registry) {
        registry.add("naturalist.secrets.source", () -> "file");
        registry.add("naturalist.secrets.directory", DIR::toString);
    }

    @Autowired WebApplicationContext context;
    @Autowired RecordingSecretProvider secrets;
    MockMvc mockMvc;
    @BeforeEach void setUp() { mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/secrets")).andExpect(status().is3xxRedirection())
                .andExpect(r -> assertThat(r.getResponse().getRedirectedUrl()).contains("/login"));
    }
    @Test void admin_seesTheFingerprintAndSource_neverTheValue() throws Exception {
        secrets.require(SecretName.of("smoke-secret"));
        var html = mockMvc.perform(get("/admin/secrets").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("smoke-secret").contains("ed24bc0b").contains("file:" + DIR);
        assertThat(html).doesNotContain("not-a-real-secret");
    }
}
```

(`ed24bc0b` is `printf %s "not-a-real-secret" | shasum -a 256 | cut -c1-8`.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=AdminSecretsControllerWebMvcTest`
Expected: FAIL, 404 or compilation failure.

- [ ] **Step 3: Implement controller, template and nav**

`AdminSecretsController` mirrors `AdminResilienceController`: constructor `(RecordingSecretProvider secrets)`, `@GetMapping("/admin/secrets")` adds `resolutions` and returns `"admin/secrets"`. `secrets.jte` takes `@param List<SecretResolution> resolutions`, renders `@template.admin.nav(current = "secrets")`, an `<h1>Secrets</h1>`, one sentence (fingerprint is the first eight hex digits of SHA-256; compare with `printf %s "$KEY" | shasum -a 256 | cut -c1-8`), and a table with columns Name, Source, Fingerprint, Resolved at, or `<p><em>None resolved.</em></p>` when empty. `nav.jte` gains the `secrets` link in the existing `@if/@else` shape.

- [ ] **Step 4: Run the module**

Run: `mvn -q -pl apps/management-console verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Stage and print the commit command**

```bash
git add apps/management-console
git commit -m "feat(admin): /admin/secrets lists resolved secrets by source and fingerprint (ADR-028)

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 14: Rotation runbook, ignore rules, indexes

**Files:**
- Create: `docs/runbooks/secret-rotation.md`
- Modify: `.gitignore` (add `.env` and `.env.*` under a `### Secrets ###` heading)
- Modify: `docs/plans/README.md` (this plan's row: status "in progress — tasks 1–14"), `docs/work-tracker.md` (the ADR-028 bullet: approved, tasks in progress), `docs/adr/README.md` (ADR-028 → Accepted) and the ADR's status line
- Modify: `README.md` "Documentation" list (link the runbook)

- [ ] **Step 1: Write the runbook**

Content is design plan §6 verbatim in structure: the fingerprint command, the seven routine steps (naming `/admin/secrets` as the one place the live fingerprint is confirmed), emergency rotation, rehearsal, "rotation without a restart is deliberately not built", and a **Rotation log** table with columns Date, Key name, Fingerprint, Deactivated old key at, Notes, seeded with one row for step 0's `naturalist-dev` key once Pat records it. Link ADR-028 and the design plan at the top.

- [ ] **Step 2: Update ignore rules and indexes**

`.gitignore`: `.env` and `.env.*`. `docs/plans/README.md`, `docs/work-tracker.md`, `docs/adr/README.md`, the ADR status line and the README documentation list as listed above.

- [ ] **Step 3: Verify nothing in the tree reads a key the old way**

Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS.
Run: `env -u MODEL_CREDENTIAL mvn verify`
Expected: BUILD SUCCESS with no key in the environment.

- [ ] **Step 4: Stage and print the commit command**

```bash
git add docs .gitignore README.md
git commit -m "docs(secrets): secret rotation runbook; ADR-028 accepted; ignore .env files

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

## After the tasks (operational, not code)

- **Rehearsal (design plan §7 step 7):** run the runbook end to end against the pre-public deployment with the `naturalist-dev` key; record timings and every deviation in the runbook's log; fix what it exposes.
- **Deploy configuration (step 8, after the host is chosen):** the host's store command for `model-credential`, `naturalist.secrets.source` (`environment` or `file` + `directory`), `naturalist.vision.provider=anthropic`, the Turnstile secret and tunnel token through the same source; forwarded-header trust and the edge checks per the Cloudflare plan. Smoke: one identification, `/admin/secrets` shows the prod fingerprint, `/admin/resilience` lists the bulkhead and `vision.identification.client`.
- **Reconciliation with ADR-027:** when `ChallengeConfiguration` lands, it resolves `challenge-secret` through `SecretProvider` instead of `naturalist.challenge.secret-key` (ADR-028, one line).
- **Later:** `naturalist.vision.credential=federation` once the host issues OIDC identity tokens (ADR-028, deferred).
