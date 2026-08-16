# Nutrient → Chemistry Links Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every nutrient row on `/soil/profiles/{name}` link to the chemistry entity it measures, and give the chemistry console an element page for those links to land on.

**Architecture:** Soil declares its own outbound reference — `Nutrients` (soil-api) gains a map from `NutrientName` to a `NutrientChemistry` value carrying an `EntityName` (`ElementName` today, `CompoundName` if ever needed) plus how the lab reports it. The soil console resolves that name through the `Catalog` + composite `EntityRefLinker` seam, exactly as `CladeRankLinks` does, so soil never depends on chemistry. Chemistry gains element list/detail routes, six missing elements, and an element catalog contribution so the slug resolves.

**Tech Stack:** Java 21 records + sealed types, Maven multi-module, JTE templates, Spring Boot (management-console only), JUnit 5 + AssertJ, in-memory `TestEntitySource` catalogs backed by JSON.

**Source spec:** [`2026-08-15-nutrient-chemistry-links-design.md`](2026-08-15-nutrient-chemistry-links-design.md)

## Global Constraints

- **Run only the module-scoped `mvn` commands this plan gives you**, exactly as written. Each is scoped with `-pl <modules> -am`, which builds dependencies from source in the same reactor — so a signature change in an upstream module (Task 2) is picked up without staleness. Never run a bare `mvn clean verify`, `mvn install`, or any unscoped build: the full clean build is slow and the user runs it themselves at each PR boundary (after Tasks 7, 9, and 12), where cross-module and Spring-context breakage surfaces.
- **Never commit.** Stage with `git add`, then put the exact `git commit` command in your report — do not run it. The controller hands accumulated commit commands to the user at PR boundaries. Every commit message ends with `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- **Work happens directly in `/Users/pat/dev/naturalist` on `main`.** No branch, no worktree. Because commits are deferred, your task's changes sit in the working tree alongside earlier tasks' staged changes — never `git checkout`, `git stash`, `git reset`, or `git clean` anything you did not create, and never revert another task's work.
- **Three PRs, in order: A (Tasks 1–7, chemistry), B (Tasks 8–9, soil-api), C (Tasks 10–12, soil-console).** A and B are independent; C depends on both.
- **Module dependency rules (root `CLAUDE.md`):** api modules may depend only on `framework`, `identifiers`, `field-notes`, `catalog` (chemistry-api already does), and — organisms only — `taxonomy`. `core` may import another domain's **api** only. No new pom dependency is needed anywhere in this plan except one in Task 11 (`soil-console` → `catalog`), because `NutrientName`, `ElementName`, and `CompoundName` all live in `domains/identifiers`.
- **`EntityName` values are lower-kebab-case** (`LOWER_KEBAB` in `kernels/framework/.../EntityName.java`). Any `ElementName` built from a display string must be lowercased.
- **No `new Foo(...)` at call sites outside the type's own class** — use the static factory (`ElementName.of`, `NutrientChemistry.of`). ADR-012.
- **Records for value objects; accessor names match component names exactly** (`substance()`, never `getSubstance()`).
- **JSON catalog field names match record component names exactly.** Enum values serialize by constant name. No `id` field on `NamedEntity` catalogs.
- Element atomic weights are IUPAC 2021 values; `AtomicWeight` has scale 4, `HALF_UP`.

---

## Task 1: Six micronutrient elements in the chemistry catalog

**Files:**
- Modify: `domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/element/elements.json`
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/chemistry/TestChemistryIdentifiers.java:47-55` (the `Elements` inner class constants)
- Modify: `domains/chemistry/chemistry-repository-test/src/main/java/com/naturalist/chemistry/element/ElementEntityRepositoryTest.java:34-39` (`knownEntityNames()`)
- Create: `domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/element/ElementCatalogDataTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: element slugs `sodium`, `zinc`, `manganese`, `iron`, `copper`, `boron` in the catalog; constants `TestChemistryIdentifiers.Elements.Na`, `.Zn`, `.Mn`, `.Fe`, `.Cu`, `.B` (type `ElementName`).

- [ ] **Step 1: Write the failing data test**

Create `domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/element/ElementCatalogDataTest.java`:

```java
package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the element catalog carries every element a soil nutrient row references.
 * Data assertions, not framework assertions: a missing element silently degrades a
 * nutrient link to plain text, which no other test would notice.
 */
class ElementCatalogDataTest {

    private static final Map<String, String> NUTRIENT_ELEMENT_SYMBOLS = Map.of(
            "calcium", "Ca",
            "magnesium", "Mg",
            "potassium", "K",
            "nitrogen", "N",
            "sulfur", "S",
            "phosphorus", "P",
            "chlorine", "Cl",
            "sodium", "Na",
            "zinc", "Zn",
            "manganese", "Mn");

    private static final Map<String, String> REMAINING_SYMBOLS = Map.of(
            "iron", "Fe",
            "copper", "Cu",
            "boron", "B");

    private ElementTestEntitySource source() {
        return new ElementTestEntitySource(NaturalistDatabase.create());
    }

    @Test
    void everyNutrientElementIsCatalogued() {
        var slugs = source().entityStream().map(e -> e.name().value()).toList();
        assertThat(slugs).containsAll(NUTRIENT_ELEMENT_SYMBOLS.keySet());
        assertThat(slugs).containsAll(REMAINING_SYMBOLS.keySet());
    }

    @Test
    void eachNutrientElementCarriesItsIupacSymbol() {
        var bySlug = source().entityStream()
                .collect(java.util.stream.Collectors.toMap(e -> e.name().value(), Element::symbol));
        NUTRIENT_ELEMENT_SYMBOLS.forEach((slug, symbol) ->
                assertThat(bySlug).containsEntry(slug, symbol));
        REMAINING_SYMBOLS.forEach((slug, symbol) ->
                assertThat(bySlug).containsEntry(slug, symbol));
    }

    @Test
    void everyElementNameIsAValidSlug() {
        source().entityStream().forEach(e ->
                assertThat(e.name().isValid())
                        .as("element slug '%s' must be lower-kebab-case", e.name().value())
                        .isTrue());
    }

    @Test
    void boronIsNeitherCationNorAnion() {
        // Boron is taken up as undissociated boric acid at Oak Vista's pH 7.2, so
        // its charge is 0 — an honest value, not a missing one.
        Element boron = source().entityStream()
                .filter(e -> e.name().value().equals("boron"))
                .findFirst().orElseThrow();
        assertThat(boron.ionicCharge()).isZero();
        assertThat(boron.isCation()).isFalse();
        assertThat(boron.isAnion()).isFalse();
        assertThat(boron.ionicForm()).isEqualTo("H3BO3");
    }

    @Test
    void micronutrientCationsAreDivalent() {
        var byName = source().entityStream()
                .collect(java.util.stream.Collectors.toMap(e -> e.name().value(), e -> e));
        for (String slug : java.util.List.of("zinc", "manganese", "iron", "copper")) {
            assertThat(byName.get(slug).isDivalent())
                    .as("%s is reported as a divalent cation", slug)
                    .isTrue();
        }
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

Print for the user:

```bash
mvn -q verify -pl domains/chemistry/chemistry-repository-test -am -Dtest=ElementCatalogDataTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — `everyNutrientElementIsCatalogued` reports sodium, zinc, manganese, iron, copper, boron missing.

- [ ] **Step 3: Add the six elements to the JSON catalog**

Append to the array in `domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/element/elements.json` (before the closing `]`, comma after the current last entry). Existing entries carry the *agronomically relevant* ion — nitrogen is `NO3-`, not `N3-` — so these follow suit:

```json
  {
    "name": "sodium",
    "symbol": "Na",
    "atomicWeight": 22.99,
    "ionicForm": "Na+",
    "ionicCharge": 1
  },
  {
    "name": "zinc",
    "symbol": "Zn",
    "atomicWeight": 65.38,
    "ionicForm": "Zn2+",
    "ionicCharge": 2
  },
  {
    "name": "manganese",
    "symbol": "Mn",
    "atomicWeight": 54.94,
    "ionicForm": "Mn2+",
    "ionicCharge": 2
  },
  {
    "name": "iron",
    "symbol": "Fe",
    "atomicWeight": 55.85,
    "ionicForm": "Fe2+",
    "ionicCharge": 2
  },
  {
    "name": "copper",
    "symbol": "Cu",
    "atomicWeight": 63.55,
    "ionicForm": "Cu2+",
    "ionicCharge": 2
  },
  {
    "name": "boron",
    "symbol": "B",
    "atomicWeight": 10.81,
    "ionicForm": "H3BO3",
    "ionicCharge": 0
  }
```

If you edit this file with a Python script rather than by hand, pass `ensure_ascii=False` to `json.dump` — the default escapes UTF-8 and corrupts other catalog files on round-trip.

- [ ] **Step 4: Add the identifier constants**

In `TestChemistryIdentifiers.Elements`, the constants are ordered by symbol. Replace the existing eight-constant block with:

```java
        public static final ElementName B = ElementName.of("boron");
        public static final ElementName C = ElementName.of("carbon");
        public static final ElementName Ca = ElementName.of("calcium");
        public static final ElementName Cl = ElementName.of("chlorine");
        public static final ElementName Cu = ElementName.of("copper");
        public static final ElementName Fe = ElementName.of("iron");
        public static final ElementName H = ElementName.of("hydrogen");
        public static final ElementName K = ElementName.of("potassium");
        public static final ElementName Mg = ElementName.of("magnesium");
        public static final ElementName Mn = ElementName.of("manganese");
        public static final ElementName Na = ElementName.of("sodium");
        public static final ElementName O = ElementName.of("oxygen");
        public static final ElementName S = ElementName.of("sulfur");
        public static final ElementName Zn = ElementName.of("zinc");
```

Leave the `NotFound` inner class untouched — `unobtainium` stays the fictitious name.

- [ ] **Step 5: Exercise the new data through the repository contract**

In `ElementEntityRepositoryTest`, widen `knownEntityNames()` so the ADR-002 contract cases run against a newly added element as well as an original:

```java
    @Override
    default List<ElementName> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Elements.Ca,
                TestChemistryIdentifiers.Elements.K,
                TestChemistryIdentifiers.Elements.Zn
        );
    }
```

- [ ] **Step 6: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/chemistry/chemistry-repository-test -am
```

Expected: PASS — `ElementCatalogDataTest` (5 tests), `ElementTestEntitySourceTest`, and `ElementEntityRepositoryMockTest` all green.

- [ ] **Step 7: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/element/elements.json domains/identifiers-test/src/main/java/com/naturalist/chemistry/TestChemistryIdentifiers.java domains/chemistry/chemistry-repository-test/src/main/java/com/naturalist/chemistry/element/ElementEntityRepositoryTest.java domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/element/ElementCatalogDataTest.java
```

Print for the user (do not run):

```bash
git commit -m "feat(chemistry): catalog the six elements soil nutrients reference" -m "Sodium plus the five micronutrients (zinc, manganese, iron, copper, boron). Ionic forms follow the existing agronomic convention; boron carries charge 0 because it is taken up as undissociated boric acid at pH 7.2." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 2: `PeriodicElement` yields a valid slug

`PeriodicElement.Ca.elementName()` currently returns `ElementName.of("Calcium")` — capitalized, so `isValid()` is false against `EntityName`'s lower-kebab rule. It is latent because the single call site renders it as display text. Linking element chips (Task 7) needs a real slug, so the two concerns get separated: `elementName()` returns the slug, a new `displayName()` returns the printed form.

**Files:**
- Modify: `domains/chemistry/chemistry-api/src/main/java/com/naturalist/chemistry/element/PeriodicElement.java:146-170`
- Modify: `domains/chemistry/chemistry-console/src/main/jte/chemistry/detail.jte:66`
- Create: `domains/chemistry/chemistry-api/src/test/java/com/naturalist/chemistry/element/PeriodicElementTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `PeriodicElement.elementName()` → `ElementName` with a valid lower-kebab slug (`"calcium"`); `PeriodicElement.displayName()` → `String` (`"Calcium"`). `periodicName()` is deleted.

- [ ] **Step 1: Write the failing test**

Create `domains/chemistry/chemistry-api/src/test/java/com/naturalist/chemistry/element/PeriodicElementTest.java`:

```java
package com.naturalist.chemistry.element;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodicElementTest {

    @Test
    void elementNameIsAValidLowerKebabSlug() {
        for (PeriodicElement element : PeriodicElement.values()) {
            assertThat(element.elementName().isValid())
                    .as("%s.elementName() = '%s' must be a valid EntityName",
                            element.name(), element.elementName().value())
                    .isTrue();
        }
    }

    @Test
    void elementNameMatchesTheCatalogSlug() {
        assertThat(PeriodicElement.Ca.elementName().value()).isEqualTo("calcium");
        assertThat(PeriodicElement.Zn.elementName().value()).isEqualTo("zinc");
        assertThat(PeriodicElement.B.elementName().value()).isEqualTo("boron");
    }

    @Test
    void displayNameKeepsThePrintedForm() {
        assertThat(PeriodicElement.Ca.displayName()).isEqualTo("Calcium");
        assertThat(PeriodicElement.Zn.displayName()).isEqualTo("Zinc");
    }

    @Test
    void symbolIsTheEnumConstantName() {
        assertThat(PeriodicElement.Ca.symbol()).isEqualTo("Ca");
        assertThat(PeriodicElement.Fe.symbol()).isEqualTo("Fe");
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/chemistry/chemistry-api -am -Dtest=PeriodicElementTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — `displayName()` does not exist. (Once it compiles, `elementNameIsAValidLowerKebabSlug` fails for all 118 constants.)

- [ ] **Step 3: Confirm `periodicName()` is unused before deleting it**

```bash
grep -rn "periodicName()" --include="*.java" --include="*.jte" domains apps kernels | grep -v PeriodicElement.java
```

Expected: no output. `periodicName()` returns `ElementName.of("Ca")` — also invalid — and has no callers, so it goes rather than getting a second fix.

- [ ] **Step 4: Implement**

In `PeriodicElement.java`, replace the field block, constructor, and the two name accessors:

```java
    private final int atomicNumber;
    private final String displayName;
    private final ElementName name;
    private final BigDecimal atomicWeight;

    PeriodicElement(int atomicNumber, String displayName, double atomicWeight) {
        this.atomicNumber = atomicNumber;
        this.displayName = displayName;
        this.name = ElementName.of(displayName.toLowerCase(Locale.ROOT));
        this.atomicWeight = BigDecimal.valueOf(atomicWeight).setScale(4, RoundingMode.HALF_UP);
    }
```

Add `import java.util.Locale;`. Then, in place of the existing `periodicName()` and `elementName()` methods:

```java
    /** The element's printed name, e.g. {@code "Calcium"}. Display only. */
    public String displayName() {
        return displayName;
    }

    /**
     * The element's catalog slug, e.g. {@code "calcium"} — a valid lower-kebab
     * {@link ElementName}, and the key an {@code Element} entity is stored under.
     */
    public ElementName elementName() {
        return name;
    }
```

No enum constant declarations change: all 118 display names are single words, so lowercasing yields a valid slug for every one.

- [ ] **Step 5: Update the one call site**

`domains/chemistry/chemistry-console/src/main/jte/chemistry/detail.jte:66` currently prints the (capitalized) `elementName().value()`. Keep the page text identical by switching to the display accessor:

```html
                    <li>${element.symbol()} &mdash; ${element.displayName()}</li>
```

- [ ] **Step 6: Run the tests and make sure they pass**

A `chemistry-api` method signature changed; the scoped `-am` build compiles it and its console consumer from source:

```bash
mvn -q verify -pl domains/chemistry/chemistry-api,domains/chemistry/chemistry-console -am
```

Expected: PASS across all modules — `PeriodicElementTest` (4 tests) plus the existing `ChemistryDetailTemplateTest`, which renders every catalogued compound and would catch a template/api mismatch.

- [ ] **Step 7: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-api/src/main/java/com/naturalist/chemistry/element/PeriodicElement.java domains/chemistry/chemistry-api/src/test/java/com/naturalist/chemistry/element/PeriodicElementTest.java domains/chemistry/chemistry-console/src/main/jte/chemistry/detail.jte
```

```bash
git commit -m "fix(chemistry): PeriodicElement.elementName() returns a valid slug" -m "It returned ElementName.of(\"Calcium\") for all 118 constants, which fails EntityName's lower-kebab rule. Splits the slug from the printed form via a new displayName(), and drops the unused periodicName() that had the same defect." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 3: Expose `ElementQuery` on the chemistry test context

**Files:**
- Create: `domains/chemistry/chemistry-test-context/src/main/java/com/naturalist/chemistry/element/ElementTestContext.java`
- Modify: `domains/chemistry/chemistry-test-context/src/main/java/com/naturalist/chemistry/ChemistryTestContext.java`
- Rename: `domains/chemistry/chemistry-repository-test/src/main/java/com/naturalist/chemistry/element/ElemenEntitytRepositoryMock.java` → `ElementEntityRepositoryMock.java`
- Modify: `domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/element/ElementEntityRepositoryMockTest.java`
- Create: `domains/chemistry/chemistry-test-context/src/test/java/com/naturalist/chemistry/ChemistryTestContextTest.java` (first test in this module — **no pom change needed**: `framework-test` carries junit-jupiter and assertj at compile scope and arrives transitively through `chemistry-repository-test`)

**Interfaces:**
- Consumes: `ElementRepository` (package-private, `chemistry-api`), `ElementQueryImpl` (package-private, `chemistry-core`), `ElementEntityRepositoryMock` (package-private, `chemistry-repository-test`).
- Produces: `ElementTestContext.createQuery(NaturalistDatabase) → ElementQuery`; `ChemistryTestContext.elementQuery() → ElementQuery`.

- [ ] **Step 1: Write the failing test**

Create `domains/chemistry/chemistry-test-context/src/test/java/com/naturalist/chemistry/ChemistryTestContextTest.java`:

```java
package com.naturalist.chemistry;

import com.naturalist.chemistry.TestChemistryIdentifiers.Elements;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryTestContextTest {

    private final ChemistryTestContext context =
            ChemistryTestContext.create(NaturalistDatabase.create());

    @Test
    void elementQueryReadsTheElementCatalog() {
        assertThat(context.elementQuery().getByName(Elements.Ca))
                .get()
                .satisfies(element -> assertThat(element.symbol()).isEqualTo("Ca"));
    }

    @Test
    void elementQueryPagesTheWholeCatalog() {
        assertThat(context.elementQuery().findPage(PageRequest.console(0)).content())
                .hasSizeGreaterThanOrEqualTo(16);
    }

    @Test
    void elementQueryReturnsEmptyForAnUncataloguedName() {
        assertThat(context.elementQuery().getByName(Elements.NotFound.name)).isEmpty();
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/chemistry/chemistry-test-context -am -Dtest=ChemistryTestContextTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — `elementQuery()` does not exist on `ChemistryTestContext`.

- [ ] **Step 3: Rename the misspelled mock**

The class we are about to instantiate is spelled `ElemenEntitytRepositoryMock`. Rename the file and the class to `ElementEntityRepositoryMock`, including its constructor name, and update the single reference in `ElementEntityRepositoryMockTest`:

```java
package com.naturalist.chemistry.element;

class ElementEntityRepositoryMockTest implements ElementEntityRepositoryTest {
    @Override
    public ElementRepository repository() {
        return new ElementEntityRepositoryMock(db);
    }
}
```

Verify nothing else referenced the old name:

```bash
grep -rn "ElemenEntitytRepositoryMock" --include="*.java" domains apps kernels
```

Expected: no output.

- [ ] **Step 4: Create `ElementTestContext`**

`domains/chemistry/chemistry-test-context/src/main/java/com/naturalist/chemistry/element/ElementTestContext.java` — the package placement is what grants access to the package-private repository, mock, and query impl across the three modules:

```java
package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;

/**
 * Assembly helper for the element sub-context. Lives in
 * {@code com.naturalist.chemistry.element} so it can instantiate the
 * package-private {@link ElementRepository} implementation
 * ({@code ElementEntityRepositoryMock}) and the package-private
 * {@code ElementQueryImpl} adapter in {@code chemistry-core}.
 * <p>
 * Mirrors {@code ProductTestContext} — one entity in the package, so the
 * N=1 collapse applies and there is no namespace to assemble.
 */
public final class ElementTestContext {

    private ElementTestContext() {
    }

    public static ElementQuery createQuery(NaturalistDatabase db) {
        return new ElementQueryImpl(new ElementEntityRepositoryMock(db));
    }
}
```

- [ ] **Step 5: Wire it into `ChemistryTestContext`**

Add the import `com.naturalist.chemistry.element.ElementQuery` and `com.naturalist.chemistry.element.ElementTestContext`, then the field, constructor line, and accessor, keeping the existing compound/product members untouched:

```java
    private final ElementQuery elementQuery;
```

```java
        this.elementQuery = ElementTestContext.createQuery(db);
```

```java
    public ElementQuery elementQuery() {
        return elementQuery;
    }
```

- [ ] **Step 6: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/chemistry/chemistry-repository-test,domains/chemistry/chemistry-test-context -am
```

Expected: PASS — `ChemistryTestContextTest` (3 tests) green, and the renamed mock still satisfies `ElementEntityRepositoryMockTest`.

- [ ] **Step 7: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-test-context domains/chemistry/chemistry-repository-test/src/main/java/com/naturalist/chemistry/element domains/chemistry/chemistry-repository-test/src/test/java/com/naturalist/chemistry/element/ElementEntityRepositoryMockTest.java
```

```bash
git commit -m "feat(chemistry): expose elementQuery on the chemistry test context" -m "Adds ElementTestContext mirroring ProductTestContext, and fixes the misspelled ElemenEntitytRepositoryMock now that it has a second caller." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 4: Elements join the catalog contribution

Without this, `Catalog.findBySlug("calcium")` resolves to nothing and the soil link degrades to plain text. It also makes elements findable from the cross-domain search page.

**Files:**
- Modify: `domains/chemistry/chemistry-core/src/main/java/com/naturalist/chemistry/catalog/ChemistryCatalogContribution.java`
- Create: `domains/chemistry/chemistry-core/src/test/java/com/naturalist/chemistry/element/ElementQueryTestSupport.java`
- Modify: `domains/chemistry/chemistry-core/src/test/java/com/naturalist/chemistry/ChemistryCatalogContributionTest.java`

**Interfaces:**
- Consumes: `ChemistryTestContext.elementQuery()` (Task 3) is *not* used here — `chemistry-core` cannot depend on `chemistry-test-context` (reactor cycle); use the new `ElementQueryTestSupport` instead. `PeriodicElement.elementName()` (Task 2) is not used here either.
- Produces: `ChemistryCatalogContribution(CompoundQuery.CompoundEntityQuery, ProductQuery, ElementQuery)` — a **three-argument** constructor; Spring supplies all three via `@DomainService` scanning. `ElementQueryTestSupport.createQuery(NaturalistDatabase) → ElementQuery`.

- [ ] **Step 1: Write the failing tests**

First create the test-scope wiring, `domains/chemistry/chemistry-core/src/test/java/com/naturalist/chemistry/element/ElementQueryTestSupport.java`:

```java
package com.naturalist.chemistry.element;

import com.naturalist.data.NaturalistDatabase;

/**
 * Test-scope wiring for {@link ElementQuery}. Lives in
 * {@code com.naturalist.chemistry.element} so it can instantiate the
 * package-private {@code ElementEntityRepositoryMock} and
 * {@link ElementQueryImpl} without exposing either to the wider test classpath.
 * <p>
 * Mirrors the production {@code ElementTestContext} (in
 * {@code chemistry-test-context}); duplicated here because {@code chemistry-core}
 * cannot depend on {@code chemistry-test-context} without forming a reactor cycle.
 */
public final class ElementQueryTestSupport {

    private ElementQueryTestSupport() {
    }

    public static ElementQuery createQuery(NaturalistDatabase db) {
        return new ElementQueryImpl(new ElementEntityRepositoryMock(db));
    }
}
```

Then, in `ChemistryCatalogContributionTest`, add the field and update the existing construction (the existing two-arg `new ChemistryCatalogContribution(compounds, products)` call and both null-rejection tests must gain the third argument):

```java
    private final ElementQuery elements = ElementQueryTestSupport.createQuery(db);
    private final ChemistryCatalogContribution contribution =
            new ChemistryCatalogContribution(compounds, products, elements);
```

Update the two existing null tests to pass `elements` as the third argument, and add these tests:

```java
    @Test
    void constructorRejectsNullElementQuery() {
        assertThatThrownBy(() -> new ChemistryCatalogContribution(compounds, products, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("elements");
    }

    @Test
    void elementIsReachableThroughItsSlug() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Elements.Ca);

        assertThat(catalog.search("calcium").stream())
                .as("slug 'calcium' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
    }

    @Test
    void elementIsReachableThroughItsSymbol() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Elements.Zn);

        assertThat(catalog.search("Zn").stream())
                .as("symbol 'Zn' should resolve to %s", expected)
                .anyMatch(h -> h.target().equals(expected));
    }

    @Test
    void findBySlugResolvesAnElementToTheChemistryDomain() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.findBySlug("boron"))
                .get()
                .isEqualTo(new EntityRef(new ChemistryDomain(), Elements.B));
    }
```

Also update the existing count assertion so it accounts for elements:

```java
    @Test
    void contributionEmitsOneSearchableEntityPerCompoundAndProductAndElement() {
        long compoundCount = Pages.stream(1000, compounds::findPage).count();
        long productCount = Pages.stream(1000, products::findPage).count();
        long elementCount = Pages.stream(1000, elements::findPage).count();
        assertThat(contribution.searchableEntities().count())
                .isEqualTo(compoundCount + productCount + elementCount);
    }
```

Add imports: `com.naturalist.chemistry.TestChemistryIdentifiers.Elements`, `com.naturalist.chemistry.element.ElementQuery`, `com.naturalist.chemistry.element.ElementQueryTestSupport`.

- [ ] **Step 2: Run them to make sure they fail**

```bash
mvn -q verify -pl domains/chemistry/chemistry-core -am -Dtest=ChemistryCatalogContributionTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — no three-argument constructor.

- [ ] **Step 3: Implement**

In `ChemistryCatalogContribution`, add the field, extend the constructor and its validation, and emit elements. Tokens are slug + symbol; the element's own name is already the slug, so no `PeriodicElement` involvement:

```java
    private final CompoundQuery.CompoundEntityQuery compounds;
    private final ProductQuery products;
    private final ElementQuery elements;

    public ChemistryCatalogContribution(CompoundQuery.CompoundEntityQuery compounds,
                                        ProductQuery products,
                                        ElementQuery elements) {
        Observer.forClass(ChemistryCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(compounds, "compounds")
                        .notNull(products, "products")
                        .notNull(elements, "elements"))
                .throwWhenInvalid();
        this.compounds = compounds;
        this.products = products;
        this.elements = elements;
    }

    @Override
    public Stream<SearchableEntity> searchableEntities() {
        return Stream.concat(Stream.concat(compoundEntities(), productEntities()), elementEntities());
    }

    private Stream<SearchableEntity> elementEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, elements::findPage)
                .map(ChemistryCatalogContribution::toSearchableElement);
    }

    private static SearchableEntity toSearchableElement(Element element) {
        EntityRef target = new EntityRef(DOMAIN, element.name());
        return new SearchableEntity(target, elementTokens(element));
    }

    private static Stream<String> elementTokens(Element element) {
        return Stream.of(
                element.name().value(),
                element.symbol());
    }
```

Add imports `com.naturalist.chemistry.element.Element` and `com.naturalist.chemistry.element.ElementQuery`. Update the class javadoc's bullet list with an `Element` entry: *slug and IUPAC symbol (e.g. `"calcium"`, `"Ca"`)*.

- [ ] **Step 4: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/chemistry/chemistry-core -am
```

Expected: PASS. Note in your report that `CatalogConfigurationTest` and `SearchControllerWebMvcTest` in `apps/management-console` are **not** covered by this scoped run — the app assembles every contribution and fails fast on duplicate slugs, and that check happens in the user's PR-boundary verify. None of the 16 element slugs collides with an existing compound, product, or glossary slug, so it is expected to pass there.

- [ ] **Step 5: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-core
```

```bash
git commit -m "feat(chemistry): contribute elements to the catalog" -m "Elements are now searchable by slug and IUPAC symbol, and resolvable via Catalog.findBySlug — the seam a soil nutrient row uses to find its element without depending on chemistry." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 5: `ChemistryLinker` links elements

**Files:**
- Modify: `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/catalog/ChemistryLinker.java`
- Create: `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/catalog/ChemistryLinkerTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `linkFor(EntityRef)` returns `/chemistry/elements/{slug}` for an `ElementName`. Task 6 must serve exactly that route; Task 10 depends on this return value.

- [ ] **Step 1: Write the failing test**

Create `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/catalog/ChemistryLinkerTest.java`, modelled on `InsectsLinkerTest`:

```java
package com.naturalist.chemistry.console.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.chemistry.product.ProductName;
import com.naturalist.ddd.EntityName;
import com.naturalist.soil.observation.NutrientName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryLinkerTest {

    // The linker only switches on ref.name(); the domain is irrelevant here.
    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    private final ChemistryLinker linker = new ChemistryLinker();

    private String link(EntityName name) {
        return linker.linkFor(new EntityRef(new TestDomain(), name));
    }

    @Test
    void linksElementToElementDetail() {
        assertThat(link(ElementName.of("calcium"))).isEqualTo("/chemistry/elements/calcium");
    }

    @Test
    void linksCompoundToCompoundDetail() {
        assertThat(link(CompoundName.of("calcium-sulfate-dihydrate")))
                .isEqualTo("/chemistry/calcium-sulfate-dihydrate");
    }

    @Test
    void linksProductToProductDetail() {
        assertThat(link(ProductName.of("apiguard"))).isEqualTo("/chemistry/products/apiguard");
    }

    @Test
    void returnsNullForANameTypeItDoesNotOwn() {
        assertThat(link(NutrientName.of("calcium-soluble"))).isNull();
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am -Dtest=ChemistryLinkerTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — `linksElementToElementDetail` gets `null` (the `default` branch).

- [ ] **Step 3: Implement**

Add the `ElementName` case to the switch, above the `default`:

```java
        return switch (ref.name()) {
            case ElementName n -> "/chemistry/elements/" + n.value();
            case CompoundName n -> "/chemistry/" + n.value();
            case ProductName n -> "/chemistry/products/" + n.value();
            default -> null;
        };
```

Add `import com.naturalist.chemistry.element.ElementName;`.

- [ ] **Step 4: Run the test and make sure it passes**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am
```

Expected: PASS (4 tests).

- [ ] **Step 5: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/catalog/ChemistryLinker.java domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/catalog/ChemistryLinkerTest.java
```

```bash
git commit -m "feat(chemistry): link element refs to the element detail route" -m "First test for ChemistryLinker; covers all three name types plus the foreign-name null contract." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 6: Element list and detail pages

**Files:**
- Create: `domains/chemistry/chemistry-console/src/main/jte/chemistry/elements/list.jte`
- Create: `domains/chemistry/chemistry-console/src/main/jte/chemistry/elements/detail.jte`
- Modify: `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/ChemistryController.java`
- Modify: `domains/chemistry/chemistry-console/src/main/jte/chemistry/list.jte:10` (the sub-link line)
- Create: `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/ChemistryElementsTemplateTest.java`

**Interfaces:**
- Consumes: `ChemistryTestContext.elementQuery()` (Task 3).
- Produces: routes `GET /chemistry/elements` and `GET /chemistry/elements/{name}` — the URLs Task 5's linker promises. Template params: `list.jte` takes `Page<Element> elementsPage`; `detail.jte` takes `Element element`.

- [ ] **Step 1: Write the failing test**

Create `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/ChemistryElementsTemplateTest.java`:

```java
package com.naturalist.chemistry.console;

import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.element.Element;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryElementsTemplateTest {

    private final ChemistryTestContext context =
            ChemistryTestContext.create(NaturalistDatabase.create());

    @Test
    void list_rendersEveryElementLinkedToItsDetailPage() {
        var page = context.elementQuery().findPage(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/list.jte",
                Map.of("elementsPage", page), output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).contains("/chemistry/elements/boron");
        assertThat(html).contains("Ca");
        assertThat(html).contains("40.08");
    }

    @Test
    void detail_rendersEveryElementWithoutError() {
        for (Element element : context.elementQuery()
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content()) {
            StringOutput output = new StringOutput();
            TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                    Map.of("element", element), output);
            assertThat(output.toString())
                    .as("rendered output for %s", element.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void detail_showsIonicFormAndChargeCharacter() {
        Element calcium = context.elementQuery().getByName(ElementName.of("calcium")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                Map.of("element", calcium), output);

        String html = output.toString();
        assertThat(html).contains("Ca2+");
        assertThat(html).contains("cation");
        assertThat(html).contains("40.08");
    }

    @Test
    void detail_describesBoronAsNeitherCationNorAnion() {
        Element boron = context.elementQuery().getByName(ElementName.of("boron")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                Map.of("element", boron), output);

        String html = output.toString();
        assertThat(html).contains("H3BO3");
        assertThat(html).contains("uncharged");
        assertThat(html).doesNotContain(">cation<");
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am -Dtest=ChemistryElementsTemplateTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — `TemplateNotFoundException: chemistry/elements/list.jte`.

- [ ] **Step 3: Create the list template**

`domains/chemistry/chemistry-console/src/main/jte/chemistry/elements/list.jte` — a table rather than the compound grid, because an element carries four facts and a card would be mostly whitespace:

```html
@import com.naturalist.chemistry.element.Element
@import com.naturalist.data.Page

@param Page<Element> elementsPage

@template.layout.page(title = "Elements", content = @`
    <a href="/chemistry" class="naturalist-backlink">&larr; Back to compounds</a>

    <h1>Elements</h1>
    <p>The elements the catalog knows, with the ionic form each takes in soil solution.</p>

    <table>
        <thead>
            <tr><th>Element</th><th>Symbol</th><th>Atomic weight</th><th>Ionic form</th><th>Charge</th></tr>
        </thead>
        <tbody>
            @for(var e : elementsPage.content())
                <tr>
                    <td><a href="/chemistry/elements/${e.name().value()}">${e.name().value()}</a></td>
                    <td><code>${e.symbol()}</code></td>
                    <td>${e.atomicWeight().value().toString()} g/mol</td>
                    <td><code>${e.ionicForm()}</code></td>
                    @if(e.isCation())
                        <td>+${e.ionicCharge()} &middot; cation</td>
                    @elseif(e.isAnion())
                        <td>${e.ionicCharge()} &middot; anion</td>
                    @else
                        <td class="not-reported">uncharged</td>
                    @endif
                </tr>
            @endfor
        </tbody>
    </table>

    @template.components.pager(page = elementsPage, baseUrl = "/chemistry/elements")
`)
```

- [ ] **Step 4: Create the detail template**

`domains/chemistry/chemistry-console/src/main/jte/chemistry/elements/detail.jte`:

```html
@import com.naturalist.chemistry.element.Element

@param Element element

@template.layout.page(title = element.name().value(), content = @`
    <a href="/chemistry/elements" class="naturalist-backlink">&larr; Back to elements</a>

    <h1>${element.symbol()} &mdash; ${element.name().value()}</h1>
    <p><code>${element.name().value()}</code></p>

    <section>
        <h2>Identity</h2>
        <ul>
            <li><strong>Symbol:</strong> <code>${element.symbol()}</code></li>
            <li><strong>Atomic weight:</strong> ${element.atomicWeight().value().toString()} g/mol</li>
            <li><strong>Ionic form:</strong> <code>${element.ionicForm()}</code></li>
            @if(element.isCation())
                <li><strong>Charge:</strong> +${element.ionicCharge()} &mdash; a <span>cation</span>,
                    held on soil surfaces and exchanged from them</li>
            @elseif(element.isAnion())
                <li><strong>Charge:</strong> ${element.ionicCharge()} &mdash; an <span>anion</span>,
                    which moves with soil water rather than being held</li>
            @else
                <li><strong>Charge:</strong> <span class="not-reported">uncharged</span> in the form
                    plants take up &mdash; neither held on exchange sites nor repelled from them</li>
            @endif
        </ul>
    </section>
`)
```

- [ ] **Step 5: Add the controller routes**

In `ChemistryController`, add the field, assign it in the constructor from the existing `ChemistryTestContext context` local, and add two mappings. Place them **above** the existing `@GetMapping("/{name}")` compound route is not required — Spring prefers the more specific literal path — but keep them next to each other for readability:

```java
    private final ElementQuery elementQuery;
```

```java
        this.elementQuery = context.elementQuery();
```

```java
    @GetMapping("/elements")
    String elementList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Element> elementsPage = elementQuery.findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("elementsPage", elementsPage);
        return "chemistry/elements/list";
    }

    @GetMapping("/elements/{name}")
    String elementDetail(@PathVariable String name, Model model) {
        var element = elementQuery.getByName(ElementName.of(name));
        if (element.isEmpty()) {
            return "redirect:/chemistry/elements";
        }
        model.addAttribute("element", element.get());
        return "chemistry/elements/detail";
    }
```

Add imports: `com.naturalist.chemistry.element.Element`, `com.naturalist.chemistry.element.ElementName`, `com.naturalist.chemistry.element.ElementQuery`.

- [ ] **Step 6: Add the sub-link from the compound catalog**

Replace line 10 of `domains/chemistry/chemistry-console/src/main/jte/chemistry/list.jte`:

```html
    <p>Compounds catalogued. <a href="/chemistry/products">View products &rarr;</a>
        &middot; <a href="/chemistry/elements">View elements &rarr;</a></p>
```

- [ ] **Step 7: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am
```

Expected: PASS — `ChemistryElementsTemplateTest` (4 tests) plus the existing `ChemistryListTemplateTest`, which renders the modified list template.

- [ ] **Step 8: Verify in the running app**

Ask the user to start the console and confirm three things: `/chemistry` shows the new "View elements" link, `/chemistry/elements` lists 16 rows, and `/chemistry/elements/boron` renders "uncharged" rather than a charge. Report what they see.

- [ ] **Step 9: Stage and print the commit**

```bash
git add domains/chemistry/chemistry-console
```

```bash
git commit -m "feat(chemistry): element list and detail pages" -m "Gives ElementName refs somewhere to land: a table of the 16 catalogued elements and a per-element page naming the ionic form and what its charge means in soil. Reached from the compound catalog's sub-link line." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 7: Constituent-element chips link to element pages

The compound detail page already lists a compound's constituent elements as text. Only 16 of the 118 `PeriodicElement` constants have an `Element` entity, so the controller passes the set of catalogued slugs and the template links only those — the same shape the compound list page already uses for `depictableSlugs`.

**Files:**
- Modify: `domains/chemistry/chemistry-console/src/main/java/com/naturalist/chemistry/console/ChemistryController.java` (the `detail` method)
- Modify: `domains/chemistry/chemistry-console/src/main/jte/chemistry/detail.jte:62-68`
- Modify: `domains/chemistry/chemistry-console/src/test/java/com/naturalist/chemistry/console/ChemistryDetailTemplateTest.java`

**Interfaces:**
- Consumes: `PeriodicElement.elementName()` / `.displayName()` (Task 2), `ChemistryTestContext.elementQuery()` (Task 3), the `/chemistry/elements/{slug}` route (Task 6).
- Produces: `chemistry/detail.jte` gains param `Set<String> linkableElements = java.util.Set.of()` — defaulted, so existing renders keep working.

- [ ] **Step 1: Write the failing test**

Replace the body of `ChemistryDetailTemplateTest` with the existing smoke test plus two new cases (keep the existing test method exactly as it is — it renders every compound with no `linkableElements`, which is the defaulted path):

```java
    @Test
    void detail_linksConstituentElementsThatAreCatalogued() {
        var db = NaturalistDatabase.create();
        Compound gypsum = new CompoundTestEntitySource(db).entityStream()
                .filter(c -> c.name().value().equals("calcium-sulfate-dihydrate"))
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/detail.jte",
                Map.of("compound", gypsum,
                        "linkableElements", Set.of("calcium", "sulfur", "oxygen", "hydrogen")),
                output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).contains("/chemistry/elements/sulfur");
        assertThat(html).contains("Calcium");
    }

    @Test
    void detail_leavesUncataloguedElementsAsPlainText() {
        var db = NaturalistDatabase.create();
        Compound gypsum = new CompoundTestEntitySource(db).entityStream()
                .filter(c -> c.name().value().equals("calcium-sulfate-dihydrate"))
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        // Only calcium is catalogued: sulfur, oxygen and hydrogen must not become links.
        TestTemplateEngine.create().render("chemistry/detail.jte",
                Map.of("compound", gypsum, "linkableElements", Set.of("calcium")),
                output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).doesNotContain("/chemistry/elements/sulfur");
        assertThat(html).contains("Sulfur");
    }
```

Add imports `java.util.Set` and `com.naturalist.chemistry.compound.Compound` (the latter is already present).

- [ ] **Step 2: Run them to make sure they fail**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am -Dtest=ChemistryDetailTemplateTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — no `/chemistry/elements/calcium` in the output (the chips are still plain text).

- [ ] **Step 3: Update the template**

Add the param at the top of `chemistry/detail.jte`, next to the existing `@param Compound compound`:

```html
@param java.util.Set<String> linkableElements = java.util.Set.of()
```

Then replace the constituent-elements loop body (line 66 as edited in Task 2):

```html
                @for(var element : compound.compoundInfo().constituentElements())
                    @if(linkableElements.contains(element.elementName().value()))
                        <li>${element.symbol()} &mdash;
                            <a href="/chemistry/elements/${element.elementName().value()}">${element.displayName()}</a></li>
                    @else
                        <li>${element.symbol()} &mdash; ${element.displayName()}</li>
                    @endif
                @endfor
```

- [ ] **Step 4: Pass the catalogued slugs from the controller**

In `ChemistryController.detail`, add the model attribute. Reuse the pattern the `list` method already uses for `depictableSlugs`:

```java
        var linkableElements = elementQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content().stream()
                .map(e -> e.name().value())
                .collect(Collectors.toSet());
        model.addAttribute("linkableElements", linkableElements);
```

`Collectors` and `PageRequest` are already imported.

- [ ] **Step 5: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/chemistry/chemistry-console -am
```

Expected: PASS — all three `ChemistryDetailTemplateTest` cases.

- [ ] **Step 6: Stage and print the commit — end of PR A**

```bash
git add domains/chemistry/chemistry-console
```

```bash
git commit -m "feat(chemistry): link constituent-element chips to element pages" -m "Only elements that exist in the catalog become links; the other 102 periodic-table constants stay as text rather than pointing at a redirect." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

**PR A boundary.** Elements exist, are searchable, have pages, and are reachable from compounds. The controller hands the user the seven accumulated commit commands and the full `mvn clean verify` before Task 8 begins.

---

## Task 8: `NutrientChemistry` and `ReportedForm` in soil-api

**Files:**
- Create: `domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/ReportedForm.java`
- Create: `domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/NutrientChemistry.java`
- Create: `domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientChemistryTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `NutrientChemistry.of(NutrientName, EntityName, ReportedForm) → NutrientChemistry` with accessors `nutrient()`, `substance()`, `reportedForm()`; `ReportedForm.{ELEMENTAL, OXIDE_EQUIVALENT, ION}` each with `label()`. Task 9 builds the map from these; Task 10 reads `substance()`.

- [ ] **Step 1: Write the failing test**

Create `domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientChemistryTest.java`:

```java
package com.naturalist.soil.observation;

import com.naturalist.chemistry.element.ElementName;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NutrientChemistryTest {

    @Test
    void carriesNutrientSubstanceAndForm() {
        NutrientChemistry chemistry = NutrientChemistry.of(
                Nutrients.PHOSPHORUS_P2O5, ElementName.of("phosphorus"), ReportedForm.OXIDE_EQUIVALENT);

        assertThat(chemistry.nutrient()).isEqualTo(Nutrients.PHOSPHORUS_P2O5);
        assertThat(chemistry.substance().value()).isEqualTo("phosphorus");
        assertThat(chemistry.reportedForm()).isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
    }

    @Test
    void everyReportedFormHasAReaderFacingLabel() {
        for (ReportedForm form : ReportedForm.values()) {
            assertThat(form.label()).isNotBlank();
        }
        assertThat(ReportedForm.OXIDE_EQUIVALENT.label()).isEqualTo("reported as an oxide equivalent");
        assertThat(ReportedForm.ELEMENTAL.label()).isEqualTo("reported as the element");
        assertThat(ReportedForm.ION.label()).isEqualTo("reported as an ion");
    }

    @Test
    void rejectsAMissingSubstance() {
        NutrientChemistry chemistry =
                NutrientChemistry.of(Nutrients.SULFATE, null, ReportedForm.ION);

        assertThatThrownBy(() -> Observer.forClass(NutrientChemistryTest.class)
                .arguments("invariants", i -> i.valueObject(chemistry, "chemistry"))
                .throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("substance");
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/soil/soil-api -am -Dtest=NutrientChemistryTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — neither type exists.

- [ ] **Step 3: Create `ReportedForm`**

```java
package com.naturalist.soil.observation;

/**
 * How a lab prints a nutrient's value relative to the substance it measures.
 * <p>
 * The distinction is not cosmetic: an oxide-equivalent figure is not the mass of the
 * element, and a reader following a nutrient row to the element it references is owed
 * that caveat. The label is what the console shows.
 */
public enum ReportedForm {

    /** The value is the element itself (calcium, zinc). */
    ELEMENTAL("reported as the element"),

    /** The value is an oxide equivalent of the element (P₂O₅ for phosphorus, K₂O for potassium). */
    OXIDE_EQUIVALENT("reported as an oxide equivalent"),

    /** The value is an ion the element occurs in (nitrate, sulfate, chloride). */
    ION("reported as an ion");

    private final String label;

    ReportedForm(String label) {
        this.label = label;
    }

    /** Reader-facing phrase for this form, e.g. for a link's title attribute. */
    public String label() {
        return label;
    }
}
```

- [ ] **Step 4: Create `NutrientChemistry`**

```java
package com.naturalist.soil.observation;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * What substance a soil nutrient row measures, and in what form the lab prints it —
 * soil's outbound reference into the chemistry catalog.
 * <p>
 * <b>Why soil owns this.</b> The referencing domain owns its outbound reference, as
 * {@code PhytochemicalConstituent.compoundName} does in plants. Chemistry knows what
 * nitrogen is; only soil knows that its {@code nitrate-n} row measures nitrogen.
 * <p>
 * {@code substance} is typed {@link EntityName} rather than a concrete subclass because
 * a nutrient may reference either an element ({@code ElementName}) or a compound
 * ({@code CompoundName}). The console resolves whichever it is through the catalog seam
 * and never switches on the type; the chemistry console's linker already dispatches on
 * the concrete class. Every current nutrient references an element.
 * <p>
 * Carries its own {@code nutrient} key so the value travels without losing its subject —
 * a consumer asking "which nutrients reference this substance?" can filter a stream of
 * these, which a bare map value could not answer.
 *
 * @param nutrient     the soil nutrient row, e.g. {@code nitrate-n}
 * @param substance    the chemistry entity it measures, e.g. {@code nitrogen}
 * @param reportedForm how the lab prints the value relative to that substance
 */
public record NutrientChemistry(
        NutrientName nutrient,
        EntityName substance,
        ReportedForm reportedForm
) implements ValueObject {

    /** The chemistry a nutrient row references. */
    public static NutrientChemistry of(NutrientName nutrient,
                                       EntityName substance,
                                       ReportedForm reportedForm) {
        return new NutrientChemistry(nutrient, substance, reportedForm);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(nutrient, "nutrient")
                .entityName(substance, "substance")
                .notNull(reportedForm, "reportedForm");
    }
}
```

- [ ] **Step 5: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/soil/soil-api -am
```

Expected: PASS (3 tests). `Constraints.entityName` is declared `<E extends EntityName> Constraints entityName(E e, String name)`, so passing the `EntityName` base type binds `E = EntityName` and compiles — no overload needed.

- [ ] **Step 6: Stage and print the commit**

```bash
git add domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/ReportedForm.java domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/NutrientChemistry.java domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientChemistryTest.java
```

```bash
git commit -m "feat(soil): NutrientChemistry — the substance a nutrient row measures" -m "Soil's outbound reference into chemistry, following the plants->chemistry precedent. substance is typed EntityName so a nutrient may reference an element or a compound without a new type." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 9: `Nutrients.chemistryOf`

**Files:**
- Modify: `domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/Nutrients.java`
- Create: `domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientsTest.java`

**Interfaces:**
- Consumes: `NutrientChemistry.of(...)`, `ReportedForm` (Task 8).
- Produces: `Nutrients.chemistryOf(NutrientName) → Optional<NutrientChemistry>`. Task 10 calls exactly this.

- [ ] **Step 1: Write the failing test**

Create `domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientsTest.java`:

```java
package com.naturalist.soil.observation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NutrientsTest {

    @Test
    void everyCataloguedNutrientDeclaresItsChemistry() {
        // Drift guard: a nutrient added to ALL without a chemistry entry silently
        // renders as an unlinked row, which no other test would notice.
        for (NutrientName name : Nutrients.ALL) {
            assertThat(Nutrients.chemistryOf(name))
                    .as("nutrient '%s' must declare the substance it measures", name.value())
                    .isPresent();
        }
    }

    @Test
    void bothCalciumFractionsReferenceTheSameElement() {
        // The exchangeable/soluble split is how the lab extracts, not two substances.
        assertThat(Nutrients.chemistryOf(Nutrients.CALCIUM_EXCHANGEABLE).orElseThrow().substance())
                .isEqualTo(Nutrients.chemistryOf(Nutrients.CALCIUM_SOLUBLE).orElseThrow().substance());
    }

    @Test
    void oxideEquivalentsAreMarkedAsSuch() {
        assertThat(Nutrients.chemistryOf(Nutrients.PHOSPHORUS_P2O5).orElseThrow())
                .satisfies(c -> {
                    assertThat(c.substance().value()).isEqualTo("phosphorus");
                    assertThat(c.reportedForm()).isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
                });
        assertThat(Nutrients.chemistryOf(Nutrients.POTASSIUM_EXCHANGEABLE).orElseThrow().reportedForm())
                .isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
    }

    @Test
    void ionsAreMarkedAsIons() {
        assertThat(Nutrients.chemistryOf(Nutrients.SULFATE).orElseThrow())
                .satisfies(c -> {
                    assertThat(c.substance().value()).isEqualTo("sulfur");
                    assertThat(c.reportedForm()).isEqualTo(ReportedForm.ION);
                });
        assertThat(Nutrients.chemistryOf(Nutrients.CHLORIDE).orElseThrow().substance().value())
                .isEqualTo("chlorine");
        assertThat(Nutrients.chemistryOf(Nutrients.NITRATE_N).orElseThrow().substance().value())
                .isEqualTo("nitrogen");
    }

    @Test
    void micronutrientsReferenceTheirOwnElement() {
        assertThat(Nutrients.chemistryOf(Nutrients.BORON).orElseThrow().substance().value())
                .isEqualTo("boron");
        assertThat(Nutrients.chemistryOf(Nutrients.ZINC).orElseThrow().reportedForm())
                .isEqualTo(ReportedForm.ELEMENTAL);
    }

    @Test
    void chemistryOfCarriesTheNutrientItDescribes() {
        assertThat(Nutrients.chemistryOf(Nutrients.IRON).orElseThrow().nutrient())
                .isEqualTo(Nutrients.IRON);
    }

    @Test
    void anUncataloguedNutrientHasNoDeclaredChemistry() {
        assertThat(Nutrients.chemistryOf(NutrientName.of("molybdenum-dtpa"))).isEmpty();
    }
}
```

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/soil/soil-api -am -Dtest=NutrientsTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — `chemistryOf` does not exist.

- [ ] **Step 3: Implement**

Add to `Nutrients.java`, after the `PRINTED_NAMES` map and before `printedNameOf`. Note `substance` values are `ElementName` instances — the import is `com.naturalist.chemistry.element.ElementName`, which lives in `domains/identifiers` and so is already on soil-api's classpath:

```java
    /**
     * The substance each nutrient row measures, and the form the lab prints it in — soil's
     * outbound reference into the chemistry catalog. Kept here beside the categories and
     * printed names because all three answer "what is this row?" for the same 17 slugs.
     * <p>
     * The exchangeable / soluble fractions collapse to one substance on purpose: both
     * calcium rows are calcium, and the split describes the extraction, not the element.
     */
    private static final Map<NutrientName, NutrientChemistry> CHEMISTRY = Map.ofEntries(
            chemistry(NITRATE_N, "nitrogen", ReportedForm.ION),
            chemistry(PHOSPHORUS_P2O5, "phosphorus", ReportedForm.OXIDE_EQUIVALENT),
            chemistry(POTASSIUM_EXCHANGEABLE, "potassium", ReportedForm.OXIDE_EQUIVALENT),
            chemistry(POTASSIUM_SOLUBLE, "potassium", ReportedForm.OXIDE_EQUIVALENT),
            chemistry(CALCIUM_EXCHANGEABLE, "calcium", ReportedForm.ELEMENTAL),
            chemistry(CALCIUM_SOLUBLE, "calcium", ReportedForm.ELEMENTAL),
            chemistry(MAGNESIUM_EXCHANGEABLE, "magnesium", ReportedForm.ELEMENTAL),
            chemistry(MAGNESIUM_SOLUBLE, "magnesium", ReportedForm.ELEMENTAL),
            chemistry(SODIUM_EXCHANGEABLE, "sodium", ReportedForm.ELEMENTAL),
            chemistry(SODIUM_SOLUBLE, "sodium", ReportedForm.ELEMENTAL),
            chemistry(SULFATE, "sulfur", ReportedForm.ION),
            chemistry(ZINC, "zinc", ReportedForm.ELEMENTAL),
            chemistry(MANGANESE, "manganese", ReportedForm.ELEMENTAL),
            chemistry(IRON, "iron", ReportedForm.ELEMENTAL),
            chemistry(COPPER, "copper", ReportedForm.ELEMENTAL),
            chemistry(BORON, "boron", ReportedForm.ELEMENTAL),
            chemistry(CHLORIDE, "chlorine", ReportedForm.ION));

    private static Map.Entry<NutrientName, NutrientChemistry> chemistry(
            NutrientName nutrient, String elementSlug, ReportedForm form) {
        return Map.entry(nutrient,
                NutrientChemistry.of(nutrient, ElementName.of(elementSlug), form));
    }

    /**
     * The substance this nutrient measures, or empty for a nutrient outside the catalog —
     * the same liberty {@link #printedNameOf} takes. A reader's row renders either way;
     * only the link is lost.
     */
    public static Optional<NutrientChemistry> chemistryOf(NutrientName name) {
        return Optional.ofNullable(CHEMISTRY.get(name));
    }
```

Add imports `com.naturalist.chemistry.element.ElementName` and `java.util.Optional`.

- [ ] **Step 4: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/soil/soil-api -am
```

Expected: PASS (7 tests in `NutrientsTest`).

- [ ] **Step 5: Confirm no api-module rule was broken**

```bash
grep -n "artifactId" domains/soil/soil-api/pom.xml
```

Expected: `framework`, `identifiers`, `field-notes` (plus test-scope entries) — and **no** `chemistry-api`. `ElementName` comes from `identifiers`, which is why this holds.

- [ ] **Step 6: Stage and print the commit — end of PR B**

```bash
git add domains/soil/soil-api/src/main/java/com/naturalist/soil/observation/Nutrients.java domains/soil/soil-api/src/test/java/com/naturalist/soil/observation/NutrientsTest.java
```

```bash
git commit -m "feat(soil): declare the chemistry each nutrient row measures" -m "A third map in Nutrients beside categories and printed names, with a drift guard asserting every catalogued nutrient declares a substance. No dependency on chemistry-api: ElementName lives in identifiers." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

**PR B boundary.** The controller hands the user the two accumulated commit commands and the full `mvn clean verify` before Task 10 begins.

---

## Task 10: `NutrientChemistryLinks` — the crossing

**Files:**
- Create: `domains/soil/soil-console/src/main/java/com/naturalist/soil/console/catalog/NutrientChemistryLinks.java`
- Create: `domains/soil/soil-console/src/test/java/com/naturalist/soil/console/catalog/NutrientChemistryLinksTest.java`
- Modify: `domains/soil/soil-console/pom.xml`

**Interfaces:**
- Consumes: `Nutrients.chemistryOf` (Task 9), `Catalog.findBySlug`, `EntityRefLinker.linkFor`.
- Produces: `NutrientChemistryLinks.of(Catalog, EntityRefLinker) → NutrientChemistryLinks`, `NutrientChemistryLinks.none()`, instance methods `String linkFor(NutrientName)` (null when unresolvable) and `String titleFor(NutrientName)` (empty string when unresolvable). Task 11's templates call both.

- [ ] **Step 1: Write the failing test**

Create `domains/soil/soil-console/src/test/java/com/naturalist/soil/console/catalog/NutrientChemistryLinksTest.java`:

```java
package com.naturalist.soil.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.catalog.MatchKind;
import com.naturalist.catalog.SearchResults;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.ddd.EntityName;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NutrientChemistryLinksTest {

    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    /** Owns exactly the slugs it is given; everything else is unknown. */
    private record StubCatalog(Set<String> knownSlugs) implements Catalog {
        @Override
        public SearchResults search(String text) {
            return SearchResults.empty();
        }

        @Override
        public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
            return Set.of();
        }

        @Override
        public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
            return Map.of();
        }

        @Override
        public Optional<EntityRef> findBySlug(String slug) {
            return knownSlugs.contains(slug)
                    ? Optional.of(new EntityRef(new TestDomain(), ElementName.of(slug)))
                    : Optional.empty();
        }
    }

    private static final EntityRefLinker LINKER = ref ->
            ref.name() instanceof ElementName n ? "/chemistry/elements/" + n.value() : null;

    private NutrientChemistryLinks links(String... knownSlugs) {
        return NutrientChemistryLinks.of(new StubCatalog(Set.of(knownSlugs)), LINKER);
    }

    @Test
    void linksBothCalciumFractionsToTheCalciumElement() {
        NutrientChemistryLinks links = links("calcium");

        assertThat(links.linkFor(Nutrients.CALCIUM_EXCHANGEABLE))
                .isEqualTo("/chemistry/elements/calcium");
        assertThat(links.linkFor(Nutrients.CALCIUM_SOLUBLE))
                .isEqualTo("/chemistry/elements/calcium");
    }

    @Test
    void linksAnOxideEquivalentToItsElement() {
        assertThat(links("phosphorus").linkFor(Nutrients.PHOSPHORUS_P2O5))
                .isEqualTo("/chemistry/elements/phosphorus");
    }

    @Test
    void returnsNullWhenTheCatalogOwnsNoSuchSlug() {
        // The element is declared by soil but absent from chemistry — plain text, no 404.
        assertThat(links("calcium").linkFor(Nutrients.BORON)).isNull();
    }

    @Test
    void returnsNullForANutrientWithNoDeclaredChemistry() {
        assertThat(links("calcium").linkFor(NutrientName.of("molybdenum-dtpa"))).isNull();
    }

    @Test
    void returnsNullWhenNoLinkerOwnsTheRef() {
        NutrientChemistryLinks links =
                NutrientChemistryLinks.of(new StubCatalog(Set.of("calcium")), ref -> null);

        assertThat(links.linkFor(Nutrients.CALCIUM_SOLUBLE)).isNull();
    }

    @Test
    void titleNamesTheSubstanceAndTheReportedForm() {
        assertThat(links("phosphorus").titleFor(Nutrients.PHOSPHORUS_P2O5))
                .isEqualTo("phosphorus — reported as an oxide equivalent");
        assertThat(links("sulfur").titleFor(Nutrients.SULFATE))
                .isEqualTo("sulfur — reported as an ion");
    }

    @Test
    void titleIsEmptyForANutrientWithNoDeclaredChemistry() {
        assertThat(links("calcium").titleFor(NutrientName.of("molybdenum-dtpa"))).isEmpty();
    }

    @Test
    void noneLinksNothing() {
        assertThat(NutrientChemistryLinks.none().linkFor(Nutrients.CALCIUM_SOLUBLE)).isNull();
        assertThat(NutrientChemistryLinks.none().titleFor(Nutrients.CALCIUM_SOLUBLE)).isEmpty();
    }
}
```

`SearchResults` is a `BehavioralCollection` with a package-private constructor — `SearchResults.empty()` is the only way to build one from outside the kernel, and the stub never needs a populated result.

- [ ] **Step 2: Add the catalog dependency**

`soil-console` gets `catalog` transitively through `library-console`, but the dependency is now direct and must be declared. In `domains/soil/soil-console/pom.xml`, add to `<dependencies>`, alphabetically before `library-console`:

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>catalog</artifactId>
        </dependency>
```

- [ ] **Step 3: Run the test to make sure it fails**

```bash
mvn -q verify -pl domains/soil/soil-console -am -Dtest=NutrientChemistryLinksTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL to compile — `NutrientChemistryLinks` does not exist.

- [ ] **Step 4: Implement**

Create `domains/soil/soil-console/src/main/java/com/naturalist/soil/console/catalog/NutrientChemistryLinks.java`:

```java
package com.naturalist.soil.console.catalog;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.soil.observation.NutrientChemistry;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.Nutrients;

/**
 * Resolves a nutrient row to the console URL of the substance it measures.
 *
 * <p>Three independent answers have to line up, and any of them coming up empty
 * degrades the row to plain text rather than a broken link:
 *
 * <ol>
 *   <li>soil declares the substance ({@link Nutrients#chemistryOf}) — empty for a lab
 *       slug outside the catalog;</li>
 *   <li>the {@link Catalog} says which domain owns that slug — empty until the owning
 *       domain contributes the entity;</li>
 *   <li>the composite {@link EntityRefLinker} renders it — null when no console module
 *       serves a route for that name type.</li>
 * </ol>
 *
 * <p>Resolution goes through the catalog seam rather than a direct dependency on
 * chemistry, exactly as {@code CladeRankLinks} does for clade rank eyebrows. Soil does
 * not know that chemistry is the domain on the other end, nor whether the substance is
 * an element or a compound.
 */
public final class NutrientChemistryLinks {

    private static final NutrientChemistryLinks NONE = new NutrientChemistryLinks(null, null);

    private final Catalog catalog;
    private final EntityRefLinker linker;

    private NutrientChemistryLinks(Catalog catalog, EntityRefLinker linker) {
        this.catalog = catalog;
        this.linker = linker;
    }

    /** A resolver backed by the assembled catalog and the app's composite linker. */
    public static NutrientChemistryLinks of(Catalog catalog, EntityRefLinker linker) {
        if (catalog == null || linker == null) {
            return NONE;
        }
        return new NutrientChemistryLinks(catalog, linker);
    }

    /** A resolver that links nothing — the default for templates rendered without a catalog. */
    public static NutrientChemistryLinks none() {
        return NONE;
    }

    /**
     * The console URL for the substance this nutrient measures, or {@code null} when any
     * of the three steps above has no answer.
     */
    public String linkFor(NutrientName nutrient) {
        if (catalog == null) {
            return null;
        }
        return Nutrients.chemistryOf(nutrient)
                .map(chemistry -> chemistry.substance().value())
                .flatMap(catalog::findBySlug)
                .map(linker::linkFor)
                .orElse(null);
    }

    /**
     * A phrase naming the substance and the form the lab printed — the link's title, so a
     * reader following "Phosphorus-P₂O₅" to phosphorus knows the value was an oxide
     * equivalent. Empty when the nutrient declares no chemistry.
     */
    public String titleFor(NutrientName nutrient) {
        return Nutrients.chemistryOf(nutrient)
                .map(NutrientChemistryLinks::describe)
                .orElse("");
    }

    private static String describe(NutrientChemistry chemistry) {
        return chemistry.substance().value() + " — " + chemistry.reportedForm().label();
    }
}
```

- [ ] **Step 5: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/soil/soil-console -am
```

Expected: PASS (8 tests).

- [ ] **Step 6: Stage and print the commit**

```bash
git add domains/soil/soil-console/pom.xml domains/soil/soil-console/src/main/java/com/naturalist/soil/console/catalog domains/soil/soil-console/src/test/java/com/naturalist/soil/console/catalog
```

```bash
git commit -m "feat(soil-console): resolve nutrient rows to their substance URL" -m "Goes through Catalog.findBySlug plus the composite linker, mirroring CladeRankLinks, so soil-console gains no dependency on chemistry. Any unresolved step yields no link rather than a broken one." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 11: The nutrient tables link

**Files:**
- Modify: `domains/soil/soil-console/src/main/jte/soil/readingRow.jte`
- Modify: `domains/soil/soil-console/src/main/jte/soil/nutrientTable.jte`
- Modify: `domains/soil/soil-console/src/main/jte/soil/profile.jte:7-8,41-47`
- Modify: `domains/soil/soil-console/src/main/java/com/naturalist/soil/console/SoilsController.java`
- Modify: `domains/soil/soil-console/src/test/java/com/naturalist/soil/console/SoilConsoleTemplateTest.java`

**Interfaces:**
- Consumes: `NutrientChemistryLinks.of/none/linkFor/titleFor` (Task 10).
- Produces: `profile.jte`, `nutrientTable.jte`, and `readingRow.jte` each take `NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()`; `SoilsController` constructor becomes `SoilsController(Catalog catalog, EntityRefLinker linker)`.

- [ ] **Step 1: Write the failing test**

Add to `SoilConsoleTemplateTest`:

```java
    @Test
    void profile_linksNutrientLabelsToTheirSubstance() {
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilProfile box1 = SoilTestContext.create(db).soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        // A catalog that owns the element slugs, and a linker that routes them —
        // the same answers the assembled app gives.
        NutrientChemistryLinks links = NutrientChemistryLinks.of(
                new StubElementCatalog(Set.of("calcium", "phosphorus", "boron")),
                ref -> ref.name() instanceof ElementName n
                        ? "/chemistry/elements/" + n.value() : null);
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", box1, "glossaryLinker", GlossaryLinker.none(),
                        "chemistryLinks", links), output);

        String html = output.toString();
        assertThat(html).contains("href=\"/chemistry/elements/calcium\"");
        assertThat(html).contains("href=\"/chemistry/elements/phosphorus\"");
        assertThat(html).contains("reported as an oxide equivalent");
        // Not catalogued in this stub: zinc stays plain text.
        assertThat(html).doesNotContain("/chemistry/elements/zinc");
        assertThat(html).contains("Zinc");
    }

    @Test
    void profile_rendersWithoutLinksWhenNoResolverIsSupplied() {
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilProfile box1 = SoilTestContext.create(db).soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", box1, "glossaryLinker", GlossaryLinker.none()), output);

        String html = output.toString();
        assertThat(html).doesNotContain("/chemistry/elements/");
        assertThat(html).contains("Calcium (Sol)");
    }
```

Reuse the stub from Task 10 by copying it as a nested record in this test class (a test-scope class in another module is not on this classpath):

```java
    private record StubElementCatalog(Set<String> knownSlugs) implements Catalog {
        @Override
        public SearchResults search(String text) {
            return SearchResults.empty();
        }

        @Override
        public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
            return Set.of();
        }

        @Override
        public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
            return Map.of();
        }

        @Override
        public Optional<EntityRef> findBySlug(String slug) {
            return knownSlugs.contains(slug)
                    ? Optional.of(new EntityRef(new ChemistryStubDomain(), ElementName.of(slug)))
                    : Optional.empty();
        }
    }

    private record ChemistryStubDomain() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }
```

Add imports: `com.naturalist.catalog.Catalog`, `.DomainId`, `.EntityRef`, `.SearchResults`, `com.naturalist.chemistry.element.ElementName`, `com.naturalist.ddd.EntityName`, `com.naturalist.soil.console.catalog.NutrientChemistryLinks`, `java.util.Optional`, `java.util.Set`.

- [ ] **Step 2: Run it to make sure it fails**

```bash
mvn -q verify -pl domains/soil/soil-console -am -Dtest=SoilConsoleTemplateTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — `profile.jte` has no `chemistryLinks` param, so JTE reports an unknown parameter.

- [ ] **Step 3: Link the label in `readingRow.jte`**

The label appears in both branches (measured and not-reported), so compute the anchor once above them. Replace the whole file:

```html
@import com.naturalist.soil.console.catalog.NutrientChemistryLinks
@import com.naturalist.soil.observation.NutrientLine
@import com.naturalist.soil.observation.Nutrients
@param NutrientLine line
@param NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()
!{var label = Nutrients.printedNameOf(line.nutrientName());}
!{var substanceUrl = chemistryLinks.linkFor(line.nutrientName());}
!{var substanceTitle = chemistryLinks.titleFor(line.nutrientName());}
@if(line.wasMeasured())
    <tr>
        @if(substanceUrl != null)
            <td><a href="${substanceUrl}" title="${substanceTitle}">${label}</a></td>
        @else
            <td>${label}</td>
        @endif
        <td>${line.reading().get().value().toString()}</td>
        <td>${line.reading().get().unit().symbol()}</td>
        @if(line.optimum().isPresent())
            <td>${line.optimum().get().range().printedForm()}</td>
        @else
            <td class="not-reported">no printed optimum</td>
        @endif
        @if(line.comparison().isConclusive())
            <td class="verdict verdict-${line.comparison().verdict().name().toLowerCase()}" title="${line.comparison().source().label()}">${line.comparison().verdict().name().toLowerCase()} range</td>
        @else
            <td></td>
        @endif
    </tr>
@else
    <tr class="not-reported">
        @if(substanceUrl != null)
            <td><a href="${substanceUrl}" title="${substanceTitle}">${label}</a></td>
        @else
            <td>${label}</td>
        @endif
        <td title="This lab did not report this nutrient. Not the same as a measured zero.">not reported</td>
        <td></td>
        @if(line.optimum().isPresent())
            <td>${line.optimum().get().range().printedForm()}</td>
        @else
            <td></td>
        @endif
        <td></td>
    </tr>
@endif
```

- [ ] **Step 4: Thread the resolver through `nutrientTable.jte`**

```html
@import com.naturalist.soil.console.catalog.NutrientChemistryLinks
@import com.naturalist.soil.observation.NutrientLine
@import java.util.List
@param List<NutrientLine> lines
@param NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()
<table>
    <thead>
        <tr><th>Nutrient</th><th>Value</th><th>Unit</th><th>Optimum (as printed)</th><th>Position</th></tr>
    </thead>
    <tbody>
        @for(var line : lines)
            @template.soil.readingRow(line = line, chemistryLinks = chemistryLinks)
        @endfor
    </tbody>
</table>
```

- [ ] **Step 5: Pass it from `profile.jte`**

Add the import and the defaulted param:

```html
@import com.naturalist.soil.console.catalog.NutrientChemistryLinks
```

```html
@param NutrientChemistryLinks chemistryLinks = NutrientChemistryLinks.none()
```

Then update all three `nutrientTable` calls (primary, secondary, micro):

```html
                <h3>Primary nutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.PRIMARY), chemistryLinks = chemistryLinks)

                <h3>Secondary nutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.SECONDARY), chemistryLinks = chemistryLinks)

                <h3>Micronutrients</h3>
                @template.soil.nutrientTable(lines = analysis.nutrientLines(NutrientCategory.MICRO), chemistryLinks = chemistryLinks)
```

- [ ] **Step 6: Wire the controller**

In `SoilsController`, take the two beans (the composite `EntityRefLinker` and assembled `Catalog` are already Spring beans — `ChemistryController` injects the same pair), build the resolver once, and add it to the detail model:

```java
    private final NutrientChemistryLinks chemistryLinks;
```

```java
    SoilsController(Catalog catalog, EntityRefLinker linker) {
```

```java
        // Nutrient rows link to the substance each measures, resolved through the
        // cross-domain catalog seam — soil-console never depends on chemistry.
        this.chemistryLinks = NutrientChemistryLinks.of(catalog, linker);
```

```java
        model.addAttribute("chemistryLinks", chemistryLinks);
```

Add imports `com.naturalist.catalog.Catalog`, `com.naturalist.catalog.EntityRefLinker`, `com.naturalist.soil.console.catalog.NutrientChemistryLinks`. Leave the rest of the constructor (the `NaturalistDatabase`/`SoilTestContext`/`GlossaryLinker` block) untouched.

- [ ] **Step 7: Run the tests and make sure they pass**

```bash
mvn -q verify -pl domains/soil/soil-console -am
```

Expected: PASS — both new `SoilConsoleTemplateTest` cases and the pre-existing profile/list cases. The `SoilsController` constructor changed, so note in your report that every `@SpringBootTest` in `apps/management-console` is the real gate for context startup and runs in the user's PR-boundary verify, not here.

- [ ] **Step 8: Verify in the running app**

Ask the user to open `http://localhost:8080/soil/profiles/backyard` and confirm: nutrient labels are links, "Phosphorus-P₂O₅" hovers as *phosphorus — reported as an oxide equivalent*, both calcium fractions land on `/chemistry/elements/calcium`, and every micronutrient row links (they will, now that Task 1 catalogued them). Report what they see.

- [ ] **Step 9: Stage and print the commit**

```bash
git add domains/soil/soil-console/src/main/jte/soil domains/soil/soil-console/src/main/java/com/naturalist/soil/console/SoilsController.java domains/soil/soil-console/src/test/java/com/naturalist/soil/console/SoilConsoleTemplateTest.java
```

```bash
git commit -m "feat(soil-console): link nutrient rows to the chemistry catalog" -m "Each nutrient label on a lab analysis now links to the element it measures, titled with the reported form so an oxide-equivalent value does not read as elemental mass. Templates default to a no-link resolver, so an unresolved substance renders exactly as before." -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 12: Record the effort

**Files:**
- Modify: `docs/work-tracker.md`
- Modify: `docs/plans/2026-08-15-nutrient-chemistry-links-design.md` (the `Status:` line)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Update the design doc status**

Change the header line to:

```markdown
**Status:** shipped 2026-08-15 (PRs A–C)
```

- [ ] **Step 2: Add the tracker row**

In `docs/work-tracker.md`, add a shipped entry in the same voice as the existing "Present the evidence shipped" paragraph — one short paragraph naming what shipped and the two enrichments deliberately deferred (compounds-by-element, soil back-references on the element page). Update the `Last updated:` line at the top to today's date with a one-clause summary.

- [ ] **Step 3: Stage and print the commit — end of PR C**

```bash
git add docs/work-tracker.md docs/plans/2026-08-15-nutrient-chemistry-links-design.md
```

```bash
git commit -m "docs: record the nutrient-chemistry link effort as shipped" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Self-review notes

**Spec coverage.** Every spec section maps to a task: element test data → Task 1; element pages + test context + linker + contribution → Tasks 3–6; the `PeriodicElement` slug fix and chip links → Tasks 2 and 7; `NutrientChemistry`/`ReportedForm`/`chemistryOf` + drift guard → Tasks 8–9; `NutrientChemistryLinks` + template/controller wiring → Tasks 10–11. The spec's *Out of scope* list stays out: no conversion factors, no compounds-by-element query, no soil back-reference provider.

**Type consistency.** `NutrientChemistry.of(NutrientName, EntityName, ReportedForm)` is defined in Task 8 and called in Task 9 (`chemistry(...)` helper) and read in Task 10 (`substance()`, `reportedForm()`). `NutrientChemistryLinks.linkFor/titleFor/none` are defined in Task 10 and called from the templates in Task 11 with the same names. `ChemistryCatalogContribution`'s constructor gains its third parameter in Task 4 and every existing call site in that test is listed for update. `PeriodicElement.displayName()`/`elementName()` are introduced in Task 2 and consumed in Task 7.

**Ordering risk.** Task 4 changes a constructor that Spring instantiates by scanning; if the app context fails at startup, check that `ElementQueryImpl` still carries `@DomainService` (it does today) — a factory-backed query without that marker is what broke the app context in the soil and garden efforts.
