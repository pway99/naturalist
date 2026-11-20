# Named-value strategy, step 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `EntityName`, `NamedValue` (numeric included) and `EntityId` read themselves through one
strategy, from JSON and from a URL, handing on what they are given; a recipe keeps every subtype
readable.

**Architecture:** `NamedValues.read(type, text)` in `kernels/framework` finds the subtype's
single-argument `of(String | BigDecimal | UUID)` and reads `text` as that value type (unreadable
text → `null`). `NamedValueDeserializer` (class-level `@JsonDeserialize` on the three supertypes)
and a Spring `GenericConverter` in `adapters/spring-runtime` both call it. `NamedValueHasFactory`
(OpenRewrite) marks a subtype without that factory.

**Tech Stack:** Java 25, Jackson 2.21.5 (`com.fasterxml`), Spring Framework 7 / Boot 4.1.1,
OpenRewrite (`tooling/naturalist-rewrite`), JUnit 5, AssertJ.

**Spec:** [`docs/plans/2026-11-18-named-value-serialization-design.md`](2026-11-18-named-value-serialization-design.md)

## Global Constraints

- The strategy **hands on**: a slug or file name is always built; validity is the consumer's question. Never refuse an invalid value.
- Unreadable text (not a number for a `BigDecimal` factory, not a UUID for a `UUID` one) → `null`, decided **without catching an exception** (a pattern test first), so no swallowed failure needs noticing (J3).
- A missing factory and a failed reflective call are observed and thrown (`state(...).throwWhenInvalid()`, `noticeError(..., Handling.WRAPPED)`).
- Observer fields are `static final Observer observer`; a method observing twice uses one `MethodObserver methodObserver` (M4, M8, M13). No `requireNonNull`, no logging (M5, M6).
- `kernels/framework` may use only Jackson, Commons, Micrometer, JSpecify — the Spring converter lives in `adapters/spring-runtime`.
- No version literal outside the root `pom.xml`.
- Worktree builds pass `-Dmaven.repo.local=/Users/pat/dev/naturalist/.claude/worktrees/observable-request-472254/temp/m2` to every Maven command (written `$M2` below).
- **Stage, never commit.** Pat commits on an explicit "commit".
- Out of scope: removing subtype `@JsonCreator`s (step 2), `TypedPathVariable` and controller changes (step 3).

## Review Focus

1. **A target type with no factory** (a field typed as the abstract `EntityName`, or a generic `ID extends EntityId` Jackson resolves only to its bound) — expected: refused with a `factoryFound` count, never a silent `null`. Test in Task 1.
2. **A numeric with trailing zeros** (`6.50` as a JSON number) — expected: `BigDecimal("6.50")`, scale kept; read from the token's text, not `getDoubleValue()`. Test in Task 1.
3. **A numeric written as a JSON string** (`"6.5"`) — expected: read the same as the number. Test in Task 1.
4. **A well-formed but non-v7 UUID** — expected: built and handed on (`isValid()` false); only malformed text is `null`. Test in Task 1.
5. **Spring's own `ObjectToObjectConverter` winning over ours** for `String → FileName` or `String → XId` — expected: ours runs (a malformed id converts to `null` instead of a `ConversionFailedException`). Test in Task 2.

---

### Task 1: `NamedValues` and `NamedValueDeserializer` in `kernels/framework`

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/ddd/NamedValues.java`
- Create: `kernels/framework/src/main/java/com/naturalist/ddd/NamedValueDeserializer.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/EntityName.java` (class annotation)
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/EntityId.java` (class annotation)
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/NamedValue.java` (interface annotation)
- Test: `kernels/framework/src/test/java/com/naturalist/ddd/NamedValuesTest.java`
- Test: `kernels/framework/src/test/java/com/naturalist/ddd/NamedValueDeserializerTest.java`

**Interfaces:**
- Produces: `public final class NamedValues` with
  `public static <T> @Nullable T read(Class<T> type, @Nullable String text)`.
- Produces: `public final class NamedValueDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer` (public no-arg constructor for the annotation).

Test fixtures (nested in each test class, so the tests own them): `SlugName extends EntityName`
(`maxLength` 64, `@JsonCreator public static SlugName of(String)` that returns
`new SlugName("creator:" + value)` — so a test can see the creator was *not* used), `Label(String
value) implements NamedValue<String>` (`isValid`: not blank), `Grams(BigDecimal value) implements
NumericNamedValue` (scale 2, `HALF_UP`), `ThingId extends EntityId` (`of(UUID)`), and a `Holder`
record with a field of each, a `List<SlugName>` and a `Map<String, SlugName>`.

- [ ] **Step 1: Write the failing tests**

`NamedValuesTest`:
- `read_string_buildsEvenWhenInvalid`: `read(SlugName.class, "Not A Slug")` → non-null, `value()` `"Not A Slug"`, `isValid()` false.
- `read_decimal_keepsScale`: `read(Grams.class, "6.50").value()` equals `new BigDecimal("6.50")` (`isEqualTo`, not `isEqualByComparingTo`).
- `read_decimal_unreadable_isNull`: `read(Grams.class, "six")` → `null`; also `""` → `null`.
- `read_uuid_nonV7_isHandedOn`: `read(ThingId.class, "123e4567-e89b-42d3-a456-426614174000")` → non-null, `isValid()` false.
- `read_uuid_malformed_isNull`: `read(ThingId.class, "not-a-uuid")` → `null`.
- `read_null_isNull`: `read(SlugName.class, null)` → `null`.
- `read_noFactory_refusedAndCounted`: `read(EntityName.class, "x")` throws `InvariantViolationException`; a `SimpleMeterRegistry` added to `Metrics` shows a counter `naturalist.invariant.check` with `name=NamedValues.read.factoryFound` at 1.0.

`NamedValueDeserializerTest` (plain `new ObjectMapper()`):
- `entityName_supertypeWinsOverSubtypeCreator`: `readValue("\"bee\"", SlugName.class).value()` is `"bee"` (not `"creator:bee"`).
- `namedValue_interfaceAnnotationApplies`: `readValue("\"x\"", Label.class).value()` is `"x"`.
- `numeric_fromNumberToken_keepsScale`: `readValue("6.50", Grams.class).value()` equals `new BigDecimal("6.50")`.
- `numeric_fromStringToken`: `readValue("\"6.5\"", Grams.class).value()` equals `new BigDecimal("6.5")`.
- `entityId_malformed_isNull`: `readValue("\"nope\"", ThingId.class)` is `null`.
- `nested_fieldListAndMapValue`: `Holder` read from `{"slug":"a","label":"b","grams":1.25,"id":"<v7 uuid>","slugs":["c"],"bySlot":{"k":"d"}}` has each value built with the matching text.
- `nonScalarToken_isAMismatch`: `readValue("{\"value\":\"a\"}", SlugName.class)` throws `MismatchedInputException`.
- `serialization_unchanged`: `writeValueAsString(new Holder(...))` writes each value as its plain scalar.

- [ ] **Step 2: Run the tests to see them fail**

Run: `mvn -q -Dmaven.repo.local=$M2 -pl kernels/framework test -Dtest='NamedValuesTest,NamedValueDeserializerTest'`
Expected: compilation fails — `NamedValues` / `NamedValueDeserializer` do not exist.

- [ ] **Step 3: Implement `NamedValues.read`**

- Factory: the one `public static` method named `of` with exactly one parameter of type `String`, `BigDecimal` or `UUID` whose return type is assignable to `type`. Multi-argument `of` overloads (e.g. `LifeStageName.of(InsectSpeciesName, LifeStageKind)`) are not factories. Cache per class with a `ClassValue<Optional<Method>>`.
- Observations, through one `methodObserver` for `"read"`: `arguments(i -> i.notNull(type, "type")).throwWhenInvalid()`; then `state(i -> i.isTrue(factory.isPresent(), "factoryFound")).throwWhenInvalid()`.
- `null` text → `null` (after the checks above).
- Value conversion, no exception caught: `String` as is; `BigDecimal` when `text.strip()` matches `[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d{1,9})?`, else `null`; `UUID` when it matches `[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}`, else `null`.
- `ReflectiveOperationException` on invoke → `methodObserver.noticeError("built", e, Handling.WRAPPED)`, then throw `IllegalStateException` with the cause.

- [ ] **Step 4: Implement `NamedValueDeserializer` and annotate the supertypes**

- `createContextual(ctxt, property)` returns a deserializer bound to `ctxt.getContextualType().getRawClass()` (the element type for a list or map value — not `property.getType()`, which is the container; the 2026-11-18 spike proved this).
- `deserialize`: a scalar token (`p.currentToken().isScalarValue()`) → `NamedValues.read(type, p.getValueAsString())`; anything else → `ctxt.handleUnexpectedToken(type, p)`.
- `@JsonDeserialize(using = NamedValueDeserializer.class)` on `EntityName`, `EntityId` and `NamedValue`. Leave `@JsonValue` and every subtype as they are.
- If `namedValue_interfaceAnnotationApplies` fails because Jackson ignores the interface annotation, stop and report: the design leaves the fallback (a mix-in, or per-record annotation) to a decision.

- [ ] **Step 5: Run the tests to see them pass**

Run: the Step 2 command.
Expected: `Tests run: 15, Failures: 0, Errors: 0`.

- [ ] **Step 6: Run everything that reads JSON**

Run: `mvn -q -Dmaven.repo.local=$M2 install -DskipTests && mvn -Dmaven.repo.local=$M2 verify` (in the background; ~15 min).
Expected: `BUILD SUCCESS`. Every `TestEntitySource` catalog now reads through `NamedValues`; a failure here is a real difference (e.g. a field typed as a bare supertype) to fix in the strategy, not by touching the catalog or the test.

- [ ] **Step 7: Stage**

```bash
git add kernels/framework/src/main/java/com/naturalist/ddd/{NamedValues,NamedValueDeserializer,EntityName,EntityId,NamedValue}.java kernels/framework/src/test/java/com/naturalist/ddd/{NamedValuesTest,NamedValueDeserializerTest}.java
```

### Task 2: The Spring converter, registered in the app and the console slices

**Files:**
- Create: `adapters/spring-runtime/src/main/java/com/naturalist/spring/NamedValueConversion.java`
- Test: `adapters/spring-runtime/src/test/java/com/naturalist/spring/NamedValueConversionTest.java`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/WebConfiguration.java` (add `addFormatters`)
- Modify: `adapters/spring-console-test/src/main/java/com/naturalist/spring/console/ConsoleSliceRenderConfiguration.java` (a `WebMvcConfigurer` bean)
- Modify: `adapters/spring-console-test/pom.xml` (depend on `spring-runtime`, no version — it is a reactor module)
- Modify: `adapters/CLAUDE.md` (one line under `spring-runtime/`: it ships the named-value converter)

**Interfaces:**
- Consumes: `NamedValues.read(Class<T>, String)` (Task 1).
- Produces: `public final class NamedValueConversion implements GenericConverter`, convertible pairs `String → EntityName`, `String → EntityId`, `String → NamedValue`; `convert` returns `NamedValues.read(targetType.getType(), (String) source)`; and `public static void register(ConverterRegistry registry)`.

- [ ] **Step 1: Write the failing test**

`NamedValueConversionTest`, on a `DefaultConversionService` with `NamedValueConversion.register(service)` applied, using test-local subtypes as in Task 1:
- `slug_convertsThroughTheStrategy`: `convert("Not A Slug", SlugName.class).value()` is `"Not A Slug"`.
- `malformedId_isNull_notAConversionFailure`: `convert("nope", ThingId.class)` is `null` (an unregistered `DefaultConversionService` throws `ConversionFailedException` here — assert that too, so the test proves ours ran).
- `numeric_converts`: `convert("6.50", Grams.class).value()` equals `new BigDecimal("6.50")`.
- `recordNamedValue_converts`: `convert("a.jpg", Label.class).value()` is `"a.jpg"`.

- [ ] **Step 2: Run it to see it fail**

Run: `mvn -q -Dmaven.repo.local=$M2 -pl adapters/spring-runtime test -Dtest=NamedValueConversionTest`
Expected: compilation fails — `NamedValueConversion` does not exist.

- [ ] **Step 3: Implement `NamedValueConversion`, then register it**

- `WebConfiguration.addFormatters(FormatterRegistry registry)` → `NamedValueConversion.register(registry)`.
- `ConsoleSliceRenderConfiguration`: a `@Bean WebMvcConfigurer namedValueConversion()` whose `addFormatters` does the same, so every console `@WebMvcTest` (each imports this configuration through its `*ControllerTestConfig`) converts as the app does.

- [ ] **Step 4: Run the test, then the console slices**

Run: the Step 2 command; then `mvn -q -Dmaven.repo.local=$M2 install -DskipTests -pl adapters/spring-runtime,adapters/spring-console-test -am && mvn -Dmaven.repo.local=$M2 -pl domains/insects/insects-console,domains/plants/plants-console verify`
Expected: `Tests run: 4, Failures: 0`; then `BUILD SUCCESS` (the `serve` endpoints' `@PathVariable FileName` now converts through `NamedValueConversion`; `InsectsImageNameWebMvcTest` / `PlantsImageNameWebMvcTest` keep passing unchanged).

- [ ] **Step 5: Stage**

```bash
git add adapters/spring-runtime/src adapters/spring-console-test/pom.xml adapters/spring-console-test/src/main/java/com/naturalist/spring/console/ConsoleSliceRenderConfiguration.java apps/management-console/src/main/java/com/naturalist/console/WebConfiguration.java adapters/CLAUDE.md
```

### Task 3: `NamedValueHasFactory`, armed

**Files:**
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NamedValueHasFactory.java`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NamedValueHasFactoryTest.java`
- Modify: `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml` (add to `EnforceArchitecture`'s `recipeList` and its description)

**Interfaces:**
- Produces: `public class NamedValueHasFactory extends Recipe` with
  `public static final String MESSAGE = "a named value needs public static <Type> of(String | BigDecimal | UUID): NamedValues reads it through that factory";`

Rule: a non-abstract class or record (not an interface) assignable to `com.naturalist.ddd.EntityName`, `com.naturalist.ddd.EntityId` or `com.naturalist.ddd.NamedValue` that declares no `public static` method named `of`, with one parameter of type `java.lang.String`, `java.math.BigDecimal` or `java.util.UUID`, returning its own type, is marked on its declaration with `SearchResult.found(cd, MESSAGE)`. Follow `NoSystemGetenv` / `NoSystemGetenvTest` for shape; give the test stub sources for the three supertypes and `NumericNamedValue` in `com.naturalist.ddd`.

- [ ] **Step 1: Write the failing tests**

- `flagsANameWithoutAFactory`: `final class BareName extends EntityName { BareName(String v) { super(v); } protected int maxLength() { return 8; } }` → marked with `MESSAGE`.
- `flagsAWrongParameterType`: a `NamedValue<String>` record whose only `of` takes `int` → marked.
- `flagsANumericWithoutAFactory`: a `NumericNamedValue` record with no `of` → marked.
- `acceptsEachFactoryShape`: an `EntityName` with `of(String)`, a `NumericNamedValue` with `of(BigDecimal)`, an `EntityId` with `of(UUID)` → no change.
- `acceptsAComposerBesideTheFactory`: a name with both `of(String)` and `of(OtherName, OtherName)` → no change.
- `ignoresAbstractTypesAndInterfaces`: an `abstract class X extends EntityName` and `interface Y extends NamedValue<String>` → no change.
- `enforceArchitectureComposite_includesTheRecipe`: `recipeFromResources("com.naturalist.EnforceArchitecture")` marks `BareName`.

- [ ] **Step 2: Run them to see them fail**

Run: `mvn -q -Dmaven.repo.local=$M2 -pl tooling/naturalist-rewrite test -Dtest=NamedValueHasFactoryTest`
Expected: compilation fails — `NamedValueHasFactory` does not exist.

- [ ] **Step 3: Implement the recipe and add it to `EnforceArchitecture`**

- [ ] **Step 4: Run the tests, then the gate's dry run**

Run: the Step 2 command; then `mvn -q -Dmaven.repo.local=$M2 install -DskipTests && mvn -Dmaven.repo.local=$M2 rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: `Tests run: 7, Failures: 0`; then `BUILD SUCCESS` — no subtype in the repo lacks its factory (86 checked when designed; if the dry run marks one, give it its `of`, do not exempt it).

- [ ] **Step 5: Stage**

```bash
git add tooling/naturalist-rewrite/src tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml
```

### Task 4: Full gate and observability review

- [ ] **Step 1: Run the gate** (background, not alongside any other Maven run)

```bash
mvn -Dmaven.repo.local=$M2 install -DskipTests && mvn -Dmaven.repo.local=$M2 verify && mvn -Dmaven.repo.local=$M2 rewrite:dryRun -Drewrite.failOnDryRunResults=true
```

Then the credential grep and the version grep from the root `CLAUDE.md`. Expected: `BUILD SUCCESS` three times; both greps return nothing (the version grep: only the root pom's comment line).

- [ ] **Step 2: Observability review**

After Maven finishes, dispatch `observability-reviewer` with `checkout: /Users/pat/dev/naturalist/.claude/worktrees/observable-request-472254`. Fix every finding in its main section, re-dispatch until `No findings` or only *uncertain* / Pat's-decision findings; report those and every "Existing code" row to Pat without fixing them.

- [ ] **Step 3: Hand to Pat** — the staged diff, the gate result, the reviewer's verdict. Commit only on his "commit".
