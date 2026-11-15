# Observe every throw: census and slice plan

**Status:** approved by Pat 2026-11-14 with every §3 decision as recommended (Decision 2:
keep `WARN`). Slices in progress.
**Branch:** `claude/observe-every-throw-8d0d18` (worktree), at `0ab335c1`.
**Rule (Pat, 2026-11-14):** every error that is thrown is observed first through the kernel
`Observer` at `Level.ERROR`. Argument refusals go through `m.arguments(...).throwWhenInvalid()`.
A swallowed failure is observed at `ERROR` too. Observations are the only signal; nobody reads
the logs.
**Out of scope:** `kernels/image-store-filesystem` (being fixed in the main checkout).

## 1. How the census was taken

- Scope: every git-tracked `src/main/**/*.java`, minus `kernels/framework-test` and
  `kernels/image-store-filesystem`. That is 1,159 files.
- Throw sites: `throw` statements and `orElseThrow(...)` calls. I found these with a text scan
  because the engine has no throw-statement finder. I then classified each site by its kind and
  by whether an `observe(Level.…)` or `throwWhenInvalid()` comes before it in the same block.
- Catch sites: 130 `catch` clauses. I read every one whose body does not throw, to sort the
  swallowed failures from the catches that return an answer on purpose. The engine's
  `java_failure_handling_census` finds catches by the exception they would catch, so it cannot
  list every catch in one call. I used it as a cross-check, not as the source.
- Engine envelope: `kaname-wt-observe-every-throw`, built from this worktree.
- At edit time, each slice re-checks its sites with the engine (`java_file_outline`,
  `java_class_source`) before it previews an atom.

**Headline: 309 throw sites. 0 are observed at `ERROR`. 10 are observed at `WARN` as a
decision. 9 catches swallow a failure without observing it.**

## 2. Categories and proposed treatment

| Cat | What it is | Sites | Treatment |
|----|----|----:|----|
| **W1** | rdbms write path: `PrimaryKeyConstraintException` from a unique-violation translate, the `throw e;` that rethrows any other failure, and `EntityNotFoundException` when 0 rows are touched. Found in 44 `*RepositoryRdbms` classes and their package-private `*Transaction` helpers. | 144 | **Decision 1.** Recommended: observe once in the kernel. `AbstractEntityRepository.insert/update/save` are `final` and already call `doInsert/doUpdate/doSave`. Give each one a single `MethodObserver`, catch `RuntimeException`, observe `state(i -> i.isTrue(false, "inserted"/"updated"/"saved")).observe(Level.ERROR)`, then rethrow. The observer is already scoped to the concrete adapter (`forClass(getClass())`). Every rdbms adapter and every in-memory mock extends this class, so one kernel file covers all 144 sites. |
| **A1** | Argument refusal spelled as a bare `throw new IllegalArgumentException` (or `IllegalStateException` on a parameter), including static `of(slug)` / `fromSlug` parsers and record constructors. | 25 | Convert to `m.arguments(i -> i …).throwWhenInvalid()`, one constraint per line. Static methods get a `private static final Observer` (the pattern `Clade`-style types already use, where present). See the catcher ripple in Decision 3. |
| **A2** | `default -> throw` in a switch over a caller's value (`FunctionalRole.ofKind`, `InsectRankName.of`, `Bioregion.of`, `Clade.of`, `UsagePolicy.scopeOf`, `AccountConfiguration`, seeder `NOT_EMPTY`) | 8 | Same as A1 when the switched value is a parameter. When it is a DB column read back (the `*Dbo.toEntity` switches, counted under S1), it is `state`. |
| **S1** | `throw new X` on a value the method acquired or computed: an unconfigured resilience name, a missing template, a missing project root, a missing system property, unknown DB enum codes in `*Dbo.toEntity`, a catalog view emitting an unknown domain, the `NoOp*Service` `UnsupportedOperationException`s, and `BudgetExceededException` in `UsageCommandImpl.reserve`. | 27 | `m.state(i -> i.isTrue(false, "<name>")).observe(Level.ERROR);` then the existing throw. The exception type does not change. |
| **S2** | `orElseThrow(...)`, both bare and with a supplier | 16 | Split the call: `Optional<X> found = …;` then `m.state(i -> i.isTrue(found.isPresent(), "found")).observe(Level.ERROR);` then `found.orElseThrow(...)`. A violation is only recorded when the value is absent, because the observer runs in `ON_FAILURE` mode. In `accounts-core` the observer runs in `ALWAYS` mode, so presence is counted too, at INFO. This uses the same idiom as `NaturalistUserDetailsService`. |
| **C1** | `catch` → throw: wrap checked (`UncheckedIOException`, `RuntimeException(e)`), translate (`TransientFailure`, `BulkheadFullException`, `ResilienceTimeoutException`, `IllegalStateException(…, e)`) or rethrow (`throw e;` in `InsectPhotoCommandImpl` / `InsectIdentificationCommandImpl` compensation, `Resilience4jTimeout`) | 30 | `m.state(i -> i.isTrue(false, "<what failed>")).observe(Level.ERROR);` as the first statement of the catch, then the existing throw. Use one name per catch (`readable`, `transportReachable`, `withinTimeout`, …). |
| **D1** | Decision throws already observed at `WARN`, as `kernels/CLAUDE.md` prescribes today. These are the rate-limit throws in `RegistrationTransaction` (×2), `AccountCommandImpl` (DuplicateEmail, InvalidToken ×2), `NaturalistUserDetailsService`, `ChallengeConfiguration`, and the `refusal(...)` helper in `InsectIdentificationCommandImpl` (×3). | 10 | **Left as is (Decision 2: keep `WARN`).** Counted as observed. |
| **S3** | Swallowed failures: `SmtpEmailSender:69` (logs WARNING, returns false); `InsectIdentificationCommandImpl` 406, 446, 506, 554, 637, 850 (degrade to a fallback); `ReferenceRouting:55` (`RuntimeException ignored → List.of()`); `RegistrationController:223` (empty catch) | 9 | `m.state(i -> i.isTrue(false, "<what degraded>")).observe(Level.ERROR);` in the catch, then the existing fallback. Behaviour does not change. |
| **X1** | Test code that lives in `src/main`: the `*RepositoryTest` contract interfaces in `*-repository-test` | 12 | Exempt (test code). |
| **X2** | Test infrastructure: `kernels/persistence-test` (7), `adapters/spring-console-test` (3), `domains/*-repository-test` mocks and `TestEntitySource`s (16), `insects-test-context` (1) | 27 | **Proposed exemption**, for the same reason as `kernels/framework-test` ("no test infra for test infra"). See Decision 4. |
| **X3** | The observability framework observing itself: `InvariantObservation.throwWhenInvalid` (which *is* the observation), the null checks in the `Observer` and `MethodObserver` constructors, and the `catch (Exception ignored)` in metric emission | 3 + 1 catch | **Proposed exemption.** These cannot observe through themselves without recursing. |
| **X4** | `ImageMetadataSegments.Malformed`: a private control-flow exception that is thrown and caught inside one class. Each catch already reports it through `malformed(m)` / `violation(m, "wellFormed")`. | 7 | **Proposed exemption.** The failure is observed once, at the catch. Observing at every throw would count each bad image twice. |
| — | Catches that answer bad input on purpose (exempt under the brief): clade-slug catches → redirect (`InsectsController` 284/584/686, `PlantsController:312`, `CladesController:92`, `CladeViewFactory:22`, `InsectCladeAnchors:78`, `UsageController:65`), `ClientKey:60`, `GridCell:91`, `ImageDownscaler:85`, `URI.create` at `InsectIdentificationCommandImpl:790`, `DateTimeParseException` at `InsectsController:436`, `ResilienceNameValidator:118`, and the `RateLimitExceeded` / `BulkheadFull` / `IdentificationRefused` / `DuplicateEmail` / `InvariantViolation` catches in controllers and filters. Each of these gets observed where it is thrown once the slices land. | ~25 | Exempt. Most catch something that a slice below makes observed at its throw site. |

**To fix: 154 throw sites (W1 144 through one kernel edit, plus A1 25, A2 8, S1 27, S2 16,
C1 30, minus overlap) plus 9 swallows. D1's 10 stay at `WARN` (Decision 2).**
**Exempt: 12 (X1) + proposed 37 (X2–X4).**

### Methods that need a `MethodObserver` first

Many target methods have no observer yet, or spell the name at the call site
(`observer.arguments("reserve", …)`, `refusals.forMethod("identify")` next to an existing
`m`). Each slice introduces one `MethodObserver m = observer.forMethod("…")` per touched method.
It routes the existing calls through `m` too, because a method gets one observer.
`InsectIdentificationCommandImpl.refusal(...)` is a D1 `WARN` decision helper, so it stays as
it is under Decision 2. It spells `forMethod("identify")` through the `ALWAYS` `refusals`
observer; that is noted as a follow-up, not part of this sweep.

## 3. Decisions for Pat

1. **W1: DECIDED (Pat, 2026-11-14): kernel template method.** Was: kernel template method or per site? The recommended option is one `ERROR`
   observation in `AbstractEntityRepository.insert/update/save` around `doX`, tagged with the
   concrete adapter class and the public method name. The literal option is an observation
   before every one of the 144 throws. That means about 150 atom edits across 48 files, and it
   gives an `Observer` to every package-private `*Transaction` helper that has none today. The
   template option observes the failure before it crosses the adapter's public boundary, but not
   on the line before the `throw`.
2. **D1: DECIDED (Pat, 2026-11-14): keep `WARN`.** A throw that is already preceded by a
   decision observed at `WARN` stays as it is. The sweep respects that decision and does not
   raise it to `ERROR`. All 10 D1 sites are counted as observed. `kernels/CLAUDE.md` is
   unchanged. A *new* decision throw still follows the existing `kernels/CLAUDE.md` rule
   (observe at `WARN` first). The sweep adds `ERROR` only where a throw has no observation at
   all.
3. **A1/A2 catcher ripple: DECIDED (Pat, 2026-11-14): option (a), convert and update catchers and tests.** `throwWhenInvalid()` throws `InvariantViolationException`, which
   extends `RuntimeException`, not `IllegalArgumentException`. Converting `Clade.of`,
   `Bioregion.of`, `*RankName.of`, `LifeStageKind.fromSlug` and the others changes what their
   callers catch. Nine console and `library-core` catches use `catch (IllegalArgumentException …)`
   as "not a clade → redirect", and tests that expect `IllegalArgumentException` will need to
   expect the new type, just as strictly. Options:
   (a) convert them and update each catcher and test (the brief's default);
   (b) leave those few `of(slug)` parsers throwing `IllegalArgumentException`, observed at
   `ERROR` first.
   I recommend (a). Slices that touch a parser also update its catchers.
4. **X2–X4 exemptions: DECIDED (Pat, 2026-11-14): accepted.** Do you accept test-support modules (`*-repository-test` mocks and
   `TestEntitySource`s, `persistence-test`, `spring-console-test`, `*-test-context`), the
   observability framework itself, and the in-class `Malformed` control flow as exempt?
   The repository mocks are the one doubtful case: `feedback_repository_mock_validation` already
   has them validate arguments through `throwWhenInvalid()`. Their write throws are covered by
   Decision 1 anyway, because the mocks extend `AbstractEntityRepository`.

## 4. Slices

Each slice stays under the `domains/CLAUDE.md` PR-size limit (≤ 400 lines of meaningful diff)
and ends with me stopping for your review of the staged diff. Every Java edit goes through engine
atoms. Each slice runs a scoped `mvn -pl … -am verify`. Slice 8 runs the full gate.

| # | Slice | Sites | Notes |
|---|---|---:|---|
| 1 | **Kernel framework**: the `AbstractEntityRepository` write template (W1, per Decision 1); `secrets` (`FileSecretProvider`, `Secret`, `SecretName`, `SecretProvider.require`) | 144 + 6 | Do this first. It closes the W1 sites. Needs a clean `mvn install`, because it changes a kernel. |
| 2 | **Insects** (reference domain): `insects-core` C1/A1 (10), its 6 S3 swallows, `insects-console` S2 (2), `insects-repository-rdbms` `InsectLifeStageDbo` switches (2), `identifiers` `LifeStageName`/`InsectRankName`/`LifeStageKind`/`PlantRankName` (4) | ~27 | Sets the pattern the other domains copy. `InsectIdentificationCommandImpl` is the largest single file. |
| 3 | **Other kernels**: `clades` (2, ripples to the clade catchers), `biogeography`, `taxonomy`, `catalog` (`ReferenceRouting` swallow), `catalog-inmem`, `catalog-rdbms` (3), `observation` (`StoredImage`), `vision` / `text-generation` NoOps | 12 | Depends on Decision 3 for `Clade.of`. |
| 4 | **Accounts** (`MonitoringMode.ALWAYS`): `accounts-api` `JdkSecureTokens`, `accounts-core` S2 ×5 | 6 | Security-critical, so it stays in `ALWAYS` mode. |
| 5 | **Other domains**: chemistry (api 2, console 1), plants (api 1, core 2, console 1), soil (api 1, console 1, rdbms Dbo switches 2), library (rdbms `CitationDbo` 1), garden console 1, zone api 1, usage (api 1, core 2), naturalists core 1 | ~19 | Can be split in two if the diff grows. |
| 6 | **Adapters**: resilience4j (12), challenge-turnstile (6), anthropic-vision (4), anthropic-text-generation (3), mail-spring swallow (1) | 26 | Vendor-failure translations: `C1` before each translate. |
| 7 | **Apps**: `management-console` (22 + `RegistrationController:223` swallow; its 4 D1 sites stay), `test-db-seeder` (7) | 26 | Configuration-startup throws (`VisionProperties`, `AdminProperties`, `SecretsConfiguration`, …) are `arguments` on bound properties: a record constructor validating its own components → `throwWhenInvalid`. |
| 8 | **Full gate + census re-run**: `mvn verify`, `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, the credential grep, and a re-run of this census to show zero unobserved sites outside the agreed exemptions | — | |

## 5. Enforcement afterwards (a proposal; nothing will be built unless you ask)

Add an OpenRewrite recipe **`NoUnobservedThrow`** to `tooling/naturalist-rewrite`, as a
sibling of `NoSystemGetenv`, and add it to `EnforceArchitecture`. It would mark:

- a `throw` statement, or an `orElseThrow` invocation, in `src/main` that is not directly
  preceded in the same block by a statement ending in `.observe(Level.ERROR)`,
  `.observe(Level.WARN)` (a decision, per Decision 2) or `.throwWhenInvalid()`;
- a `catch` block that neither throws nor contains an `.observe(Level.ERROR)`, unless its
  method returns an answer from the catch (`return`), which keeps the "bad input → answer"
  exemption.

The agreed exemptions become package or path exclusions in the recipe: the observability
package, test-support modules, and `@SuppressUnobservedThrow("why")` for a single justified
site, following the `@SuppressRepeatedSelect` precedent. It is a marker-only recipe, so the
dry-run fails and nothing is auto-fixed. This is a lexical check. It cannot prove that the
observation names the failure, but it stops a new unobserved throw from landing.

## Appendix: per-module site list

Kinds: A1/A2 argument, S1 state, S2 `orElseThrow`, C1 catch→throw, D1 WARN decision,
W1 rdbms write (counted, not listed), X1–X4 exempt. Lines are at `0ab335c1`.

**adapters/anthropic-text-generation** — 3 sites

- `AnthropicTextGenerationService.java:96` S2 — `.orElseThrow(() -> new IllegalStateException(`
- `AnthropicTextGenerationService.java:142` C1 — `throw new IllegalArgumentException(`
- `AnthropicTextGenerationService.java:151` C1 — `throw new IllegalStateException(`

**adapters/anthropic-vision** — 4 sites

- `AnthropicVisionService.java:159` S2 — `.orElseThrow(() -> new IllegalStateException(`
- `AnthropicVisionService.java:283` C1 — `throw new IllegalArgumentException(`
- `AnthropicVisionService.java:292` C1 — `throw new IllegalStateException(`
- `ImageDownscaler.java:115` S1 — `throw new IOException("no JPEG ImageWriter available");`

**adapters/challenge-turnstile** — 6 sites

- `TurnstileChallengeVerifier.java:118` S1 — `throw new TransientFailure("status " + response.statusCode(), null);`
- `TurnstileChallengeVerifier.java:124` S1 — `throw new TransientFailure(INTERNAL_ERROR, null);`
- `TurnstileChallengeVerifier.java:150` C1 — `throw new TransientFailure("transport", e);`
- `TurnstileChallengeVerifier.java:153` C1 — `throw new TransientFailure("interrupted", e);`
- `TurnstileChallengeVerifier.java:161` S1 — `throw new TransientFailure("no verdict in the response", null);`
- `TurnstileChallengeVerifier.java:165` C1 — `throw new TransientFailure("unreadable response", null);`

**adapters/resilience-resilience4j** — 12 sites

- `Resilience4jBulkhead.java:20` C1 — `throw new com.naturalist.resilience.BulkheadFullException(`
- `Resilience4jBulkhead.java:30` C1 — `throw new com.naturalist.resilience.BulkheadFullException(`
- `Resilience4jRateLimiter.java:63` C1 — `throw rejected();`
- `Resilience4jRateLimiter.java:72` C1 — `throw rejected();`
- `Resilience4jResilience.java:96` S1 — `if (r == null) throw new UnconfiguredResilienceException("retry", name);`
- `Resilience4jResilience.java:103` S1 — `if (t == null) throw new UnconfiguredResilienceException("timeout", name);`
- `Resilience4jResilience.java:110` S1 — `if (b == null) throw new UnconfiguredResilienceException("circuit-breaker", name);`
- `Resilience4jResilience.java:117` S1 — `if (b == null) throw new UnconfiguredResilienceException("bulkhead", name);`
- `Resilience4jResilience.java:124` S1 — `if (r == null) throw new UnconfiguredResilienceException("rate-limiter", name);`
- `Resilience4jTimeout.java:34` C1 — `throw new ResilienceTimeoutException(delegate.getName(), e);`
- `Resilience4jTimeout.java:36` C1 — `throw e;`
- `Resilience4jTimeout.java:38` C1 — `throw new RuntimeException(e);`

**adapters/spring-console-test** — 3 sites

- `ConsoleSliceTemplates.java:65` X2 — `throw new UncheckedIOException(e);`
- `ConsoleSliceTemplates.java:77` X2 — `throw new IllegalStateException("Could not locate project root from " + Path.of("").toAbso`
- `ConsoleSliceTemplates.java:97` X2 — `throw new TemplateNotFoundException("Template not found: " + name);`

**apps/management-console** — 22 sites

- `JteConfiguration.java:65` S1 — `throw new IllegalStateException("Could not locate project root from " + Path.of("").toAbso`
- `JteConfiguration.java:101` C1 — `throw new UncheckedIOException(e);`
- `JteConfiguration.java:126` S1 — `throw new TemplateNotFoundException("Template not found: " + name);`
- `VisionConfiguration.java:39` C1 — `throw new IllegalStateException("naturalist.vision.provider=anthropic but secret '" + e.na`
- `VisionProperties.java:17` A1 — `throw new IllegalArgumentException("naturalist.vision.provider must be set: none or anthro`
- `VisionProperties.java:21` A1 — `throw new IllegalArgumentException(`
- `AccountConfiguration.java:34` A2 — `default -> throw new IllegalStateException(`
- `RegistrationTransaction.java:129` D1 — `throw new RateLimitExceededException("registration rate limit exceeded for this client", C`
- `RegistrationTransaction.java:132` D1 — `throw new RateLimitExceededException("registration rate limit exceeded", RATE_STRATEGY);`
- `AdminProperties.java:21` A1 — `throw new IllegalArgumentException(`
- `AdminProperties.java:25` A1 — `throw new IllegalArgumentException(`
- `NaturalistUserDetailsService.java:97` D1 — `throw new UsernameNotFoundException(username);`
- `CompositeEntityRefResolver.java:51` S1 — `throw new IllegalStateException(`
- `CompositeEntityRefResolver.java:62` S1 — `throw new UnsupportedOperationException(`
- `CompositeEntityRefResolver.java:69` A1 — `throw new IllegalArgumentException("Cannot read the rank of a null subject");`
- `CompositeEntityRefResolver.java:82` A1 — `throw new IllegalArgumentException(`
- `ChallengeConfiguration.java:45` D1 — `throw new IllegalStateException(`
- `ChallengeConfiguration.java:52` S1 — `throw new IllegalStateException(`
- `EmailConfiguration.java:56` S1 — `throw new IllegalStateException(`
- `ResilienceNameValidator.java:83` S1 — `throw new IllegalStateException(message(unresolved, registered));`
- `SecretsConfiguration.java:28` S1 — `throw new IllegalStateException(`
- `SecretsConfiguration.java:33` A2 — `default -> throw new IllegalStateException(`

**apps/test-db-seeder** — 7 sites

- `DeploymentDbInitializer.java:61` A2 — `case NOT_EMPTY -> throw new IllegalStateException(`
- `DeploymentDbInitializer.java:139` S1 — `throw new IllegalStateException("Schema resource not found: " + resource);`
- `DeploymentDbInitializer.java:143` C1 — `throw new IllegalStateException("Failed reading " + resource, e);`
- `DeploymentDbInitializer.java:150` S1 — `throw new IllegalStateException("System property " + name + " must be set");`
- `Seeding.java:29` C1 — `throw new IllegalStateException("Failed applying schema " + resource, e);`
- `Seeding.java:66` C1 — `throw new UncheckedIOException("Failed copying " + space + " seed photos to " + directory,`
- `Seeding.java:70` C1 — `throw new IllegalStateException(missing.size() + " seeded " + space + " image rows have no`

**domains/accounts/accounts-api** — 1 sites

- `JdkSecureTokens.java:40` C1 — `throw new IllegalStateException("SHA-256 is required but unavailable", e);`

**domains/accounts/accounts-core** — 8 sites

- `AccountCommandImpl.java:90` D1 — `throw new DuplicateEmailException("email already registered");`
- `AccountCommandImpl.java:137` S2 — `.orElseThrow(() -> new AccountNotFoundException(account));`
- `AccountCommandImpl.java:148` S2 — `.orElseThrow(() -> new AccountNotFoundException(account));`
- `AccountCommandImpl.java:159` S2 — `.orElseThrow(() -> new AccountNotFoundException(account));`
- `AccountCommandImpl.java:170` S2 — `.orElseThrow(() -> new AccountNotFoundException(account));`
- `AccountCommandImpl.java:240` D1 — `throw new InvalidTokenException("invalid token");`
- `AccountCommandImpl.java:253` S2 — `.orElseThrow(() -> new InvalidTokenException("invalid token"));`
- `AccountCommandImpl.java:258` D1 — `throw new InvalidTokenException("invalid token");`

**domains/accounts/accounts-repository-rdbms** — 7 sites (7 W1, not listed)

**domains/chemistry/chemistry-api** — 2 sites

- `FunctionalRole.java:72` A1 — `default -> throw new IllegalArgumentException("Unknown FunctionalRole kind: " + kind);`
- `StructuralType.java:248` A1 — `default -> throw new IllegalArgumentException("Unknown StructuralType kind: " + kind);`

**domains/chemistry/chemistry-console** — 1 sites

- `DepictionRenderer.java:35` C1 — `throw new IllegalStateException("Failed to render SMILES: " + smiles, e);`

**domains/chemistry/chemistry-repository-rdbms** — 14 sites (14 W1, not listed)

**domains/chemistry/chemistry-repository-test** — 1 sites

- `DepictionEntityRepositoryTest.java:98` X1 — `CompoundDepiction expected = source().getByName(expectedId).orElseThrow();`

**domains/garden/garden-console** — 1 sites

- `GardenController.java:50` S2 — `.map(zoneName -> plantedZoneQuery.getByZoneName(zoneName).orElseThrow())`

**domains/garden/garden-repository-rdbms** — 3 sites (3 W1, not listed)

**domains/garden/garden-repository-test** — 2 sites

- `PlantingEntityRepositoryTest.java:156` X1 — `.orElseThrow();`
- `PlantingEntityRepositoryTest.java:178` X1 — `Planting stored = repository().getByName(genusRank.id()).orElseThrow();`

**domains/identifiers** — 4 sites

- `InsectRankName.java:63` A1 — `default -> throw new IllegalArgumentException("Unsupported insect rank: " + rank);`
- `LifeStageKind.java:28` A1 — `throw new IllegalArgumentException("Unknown life stage kind slug: " + slug);`
- `LifeStageName.java:69` S1 — `throw new IllegalStateException(`
- `PlantRankName.java:83` A1 — `default -> throw new IllegalArgumentException("Unsupported plant rank: " + rank);`

**domains/insects/insects-console** — 2 sites

- `InsectsController.java:262` S2 — `CladeView view = cladeQuery.getBySlug(anchor.cladeSlug()).orElseThrow(`
- `InsectsController.java:307` S2 — `CladeView view = cladeQuery.getBySlug(slug).orElseThrow(`

**domains/insects/insects-core** — 10 sites

- `InsectIdentificationCommandImpl.java:193` D1 — `throw refusal(naturalist, "withinRate",`
- `InsectIdentificationCommandImpl.java:203` D1 — `throw refusal(naturalist, "notBusy",`
- `InsectIdentificationCommandImpl.java:348` C1 — `throw new RuntimeException("Failed to serialize feature candidates for turn 2", e);`
- `InsectIdentificationCommandImpl.java:798` C1 — `throw new RuntimeException("Failed to parse vision identification result", e);`
- `InsectIdentificationCommandImpl.java:837` A1 — `default -> throw new IllegalArgumentException(`
- `InsectIdentificationCommandImpl.java:876` D1 — `throw refusal(naturalist, "withinBudget", new IdentificationRefused(`
- `InsectIdentificationCommandImpl.java:890` C1 — `throw e;`
- `InsectPhotoCommandImpl.java:90` C1 — `throw e;`
- `InsectsEntityRefResolver.java:42` A1 — `throw new IllegalArgumentException(`
- `InsectsEntityRefResolver.java:49` A1 — `throw new IllegalArgumentException("Unknown citation subject domain: " + domain);`

**domains/insects/insects-repository-rdbms** — 35 sites (33 W1, not listed)

- `InsectLifeStageDbo.java:171` A2 — `default -> throw new IllegalStateException("Unknown life stage kind: " + stageKind);`
- `InsectLifeStageDbo.java:182` A2 — `default -> throw new IllegalStateException("Unknown diapause kind: " + diapauseKind);`

**domains/insects/insects-repository-test** — 4 sites

- `InsectFeatureAssignmentTestEntitySource.java:40` X2 — `throw new UncheckedIOException(e);`
- `InsectImageTestEntitySource.java:36` X2 — `throw new UncheckedIOException(e);`
- `InsectObservationEntityRepositoryTest.java:88` X1 — `var persisted = repository().getByName(entity.key()).orElseThrow();`
- `InsectObservationTestEntitySource.java:36` X2 — `throw new UncheckedIOException(e);`

**domains/insects/insects-test-context** — 1 sites

- `NoOpExternalAuthority.java:33` X2 — `throw new UnsupportedOperationException(`

**domains/library/library-repository-rdbms** — 14 sites (13 W1, not listed)

- `CitationDbo.java:70` A2 — `default -> throw new IllegalStateException("Unknown citation kind: " + kind);`

**domains/library/library-repository-test** — 10 sites

- `CitationAssociationJson.java:67` X2 — `throw new UncheckedIOException(`
- `CompositeEntityRefResolverMock.java:38` X2 — `throw new IllegalArgumentException("A composite needs at least one delegate resolver");`
- `CompositeEntityRefResolverMock.java:45` X2 — `throw new IllegalArgumentException(`
- `CompositeEntityRefResolverMock.java:56` X2 — `throw new UnsupportedOperationException(`
- `CompositeEntityRefResolverMock.java:64` X2 — `throw new IllegalArgumentException("Cannot read the rank of a null subject");`
- `CompositeEntityRefResolverMock.java:77` X2 — `throw new IllegalArgumentException(`
- `InsectsEntityRefResolverMock.java:50` X2 — `throw new IllegalArgumentException(`
- `InsectsEntityRefResolverMock.java:58` X2 — `throw new IllegalArgumentException("Unknown citation subject domain: " + domain);`
- `PlantsEntityRefResolverMock.java:45` X2 — `throw new IllegalArgumentException(`
- `PlantsEntityRefResolverMock.java:53` X2 — `throw new IllegalArgumentException("Unknown citation subject domain: " + domain);`

**domains/naturalists/naturalists-core** — 1 sites

- `NaturalistCommandImpl.java:58` S2 — `Naturalist existing = naturalists.getByName(name).orElseThrow();`

**domains/naturalists/naturalists-repository-rdbms** — 3 sites (3 W1, not listed)

**domains/plants/plants-api** — 1 sites

- `PhytochemicalRole.java:151` A1 — `default -> throw new IllegalArgumentException("Unknown phytochemical role kind: " + kind);`

**domains/plants/plants-console** — 1 sites

- `PlantsController.java:172` S2 — `CladeView view = cladeQuery.getBySlug(clade.slug()).orElseThrow(`

**domains/plants/plants-core** — 2 sites

- `PlantsEntityRefResolver.java:53` A1 — `throw new IllegalArgumentException(`
- `PlantsEntityRefResolver.java:61` A1 — `throw new IllegalArgumentException("Unknown citation subject domain: " + domain);`

**domains/plants/plants-repository-rdbms** — 44 sites (44 W1, not listed)

**domains/plants/plants-repository-test** — 4 sites

- `PlantFeatureAssignmentTestEntitySource.java:48` X2 — `throw new UncheckedIOException(e);`
- `PlantImageTestEntitySource.java:44` X2 — `throw new UncheckedIOException(e);`
- `PlantObservationRepositoryTest.java:95` X1 — `var persisted = repository().getByName(entity.key()).orElseThrow();`
- `PlantObservationTestEntitySource.java:42` X2 — `throw new UncheckedIOException(e);`

**domains/soil/soil-api** — 1 sites

- `Nutrients.java:159` A1 — `throw new IllegalArgumentException("Unknown nutrient: " + name);`

**domains/soil/soil-console** — 1 sites

- `SoilsController.java:56` S2 — `.map(info -> soilProfileQuery.getBySoilProfileName(info.name()).orElseThrow())`

**domains/soil/soil-repository-rdbms** — 20 sites (18 W1, not listed)

- `ReportedOptimumDbo.java:66` A2 — `default -> throw new IllegalStateException("unknown range shape: " + rangeShape);`
- `ReportedRecommendationDbo.java:63` A2 — `default -> throw new IllegalStateException("unknown amount kind: " + amountKind);`

**domains/soil/soil-repository-test** — 5 sites

- `ReportedOptimumEntityRepositoryTest.java:143` X1 — `.orElseThrow();`
- `ReportedRecommendationEntityRepositoryTest.java:140` X1 — `.orElseThrow();`
- `ReportedRecommendationEntityRepositoryTest.java:151` X1 — `.orElseThrow();`
- `ReportedRecommendationEntityRepositoryTest.java:168` X1 — `.orElseThrow();`
- `ReportedRecommendationEntityRepositoryTest.java:171` X1 — `.orElseThrow();`

**domains/usage/usage-api** — 1 sites

- `UsagePolicy.java:20` A1 — `case PER_USER, CREDITS -> throw new IllegalArgumentException(`

**domains/usage/usage-core** — 2 sites

- `UsageCommandImpl.java:87` S1 — `throw new BudgetExceededException(limitKindOf(rule), UsageWindows.resetAt(rule, now));`
- `UsageCommandImpl.java:121` S2 — `UsageAlert alert = alerts.getByName(id).orElseThrow();`

**domains/usage/usage-repository-rdbms** — 9 sites (9 W1, not listed)

**domains/usage/usage-repository-test** — 2 sites

- `UsageAlertRepositoryTest.java:191` X1 — `repository().save(repository().getByName(KnownAlert1Id).orElseThrow().withAcknowledged(tru`
- `UsageAlertRepositoryTest.java:213` X1 — `repository().save(repository().getByName(KnownAlert1Id).orElseThrow().withEmailed(true));`

**domains/zone/zone-api** — 1 sites

- `Zone.java:216` S2 — `.orElseThrow(() -> new IllegalArgumentException(`

**kernels/biogeography** — 1 sites

- `Bioregion.java:62` A1 — `default -> throw new IllegalArgumentException("Unknown bioregion: " + slug);`

**kernels/catalog-inmem** — 1 sites

- `CatalogAssembly.java:162` A1 — `throw new IllegalArgumentException(`

**kernels/catalog-rdbms** — 3 sites

- `RdbmsCatalogAssembly.java:34` A1 — `throw new IllegalArgumentException("Duplicate domain slug \"" + d.value() + "\"");`
- `RdbmsCatalogAssembly.java:48` S1 — `throw new IllegalStateException("catalog view emits domain '" + row.domain`
- `RdbmsCatalogAssembly.java:52` S1 — `throw new IllegalStateException("catalog view emits entity_type '" + row.entityType`

**kernels/clades** — 2 sites

- `Clade.java:124` A1 — `default -> throw new IllegalArgumentException("Unknown clade: " + slug);`
- `CladeCatalog.java:42` C1 — `throw new IllegalStateException("Cannot instantiate clade permit: " + permit, e);`

**kernels/framework** — 9 sites

- `InvariantObservation.java:105` X3 — `throw new InvariantViolationException(scope, violated);`
- `MethodObserver.java:39` X3 — `throw new IllegalArgumentException("methodName must not be null or blank");`
- `Observer.java:70` X3 — `throw new IllegalArgumentException("referenceClass must not be null");`
- `FileSecretProvider.java:30` S1 — `throw new IOException("not a regular file");`
- `FileSecretProvider.java:35` C1 — `throw new IllegalStateException("secret file could not be read: " + path, e);`
- `Secret.java:27` A1 — `throw new IllegalArgumentException("secret value must not be blank");`
- `Secret.java:43` C1 — `throw new IllegalStateException("SHA-256 is unavailable", e);`
- `SecretName.java:17` A1 — `throw new IllegalArgumentException("secret name must be kebab-case: '" + value + "'");`
- `SecretProvider.java:24` S2 — `return lookup(name).orElseThrow(() -> new MissingSecretException(name, source()));`

**kernels/observation** — 8 sites

- `ImageMetadataSegments.java:433` X4 — `if (marker < 0) throw new Malformed();`
- `ImageMetadataSegments.java:448` X4 — `if (length < 2) throw new Malformed();`
- `ImageMetadataSegments.java:528` X4 — `else throw new Malformed();`
- `ImageMetadataSegments.java:530` X4 — `if (t.u16(2) != 42) throw new Malformed();`
- `ImageMetadataSegments.java:554` X4 — `throw new Malformed();`
- `ImageMetadataSegments.java:557` X4 — `if (letter != positive && letter != negative) throw new Malformed();`
- `ImageMetadataSegments.java:597` X4 — `if (at < 0 \|\| count < 0 \|\| at + count > data.length) throw new Malformed();`
- `StoredImage.java:34` C1 — `throw new UncheckedIOException(e);`

**kernels/persistence-test** — 7 sites

- `DboSchemaValidator.java:56` X2 — `throw new IllegalStateException("Failed scanning for @DboSchema in " + basePackage, e);`
- `DboSchemaValidator.java:77` X2 — `throw new IllegalStateException("Schema validation failed reading database metadata", e);`
- `RdbmsTestExtension.java:49` X2 — `if (session == null) throw new IllegalStateException("No active test session; use @Registe`
- `RdbmsTestExtension.java:61` X2 — `if (session == null) throw new IllegalStateException("No active test session; use @Registe`
- `SchemaApplier.java:25` X2 — `throw new IllegalStateException("Failed applying schema " + classpathResource, e);`
- `SchemaApplier.java:31` X2 — `if (in == null) throw new IllegalStateException("Schema resource not found: " + classpathR`
- `SchemaApplier.java:34` X2 — `throw new IllegalStateException("Failed reading schema " + classpathResource, e);`

**kernels/taxonomy** — 1 sites

- `RankNameDeserializer.java:24` S1 — `throw new IllegalStateException(`

**kernels/text-generation** — 1 sites

- `NoOpTextGenerationService.java:13` S1 — `throw new UnsupportedOperationException(`

**kernels/vision** — 1 sites

- `NoOpVisionService.java:11` S1 — `throw new UnsupportedOperationException(`
