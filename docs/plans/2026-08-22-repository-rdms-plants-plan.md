# Repository-RDMS Intermediate Layer (plants) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fast-follow the shipped insects slice: give the management-console production-named `plants-repository-rdms` beans (discovered via `@DomainService`), temporarily backed by the in-memory plant mocks, so the future SQL swap is "fill in the body," not "rewire."

**Architecture:** A new `plants-repository-rdms` module (split-package across `com.naturalist.plants` and its four sub-packages, depending on `plants-repository-test`) holds one `<Entity>RepositoryRdms` per repository. Each `extends` its `<Entity>RepositoryMock`, carries `@DomainService`, and takes the shared `NaturalistDatabase` bean. The mocks lose `@DomainService` so there is exactly one bean per repository interface. The obsolete `PlantsDataConfiguration` (dead `@Bean` sources) is deleted.

**Tech Stack:** Java 25, Maven (multi-module), Spring Boot (component scan + `adapters/spring-runtime` `DomainServiceScan`), JUnit 5, `@SpringBootTest`.

**Source design:** [docs/plans/2026-08-22-repository-rdms-intermediate-design.md](2026-08-22-repository-rdms-intermediate-design.md). **Precedent:** the insects slice (commits `ea82c1b` kernel fix, `2a8906d` feat, `d597761` refactor) is the exact template this mirrors.

## Global Constraints

- **Scope is plants only.** Do not touch other domains.
- **Leave the controllers forking.** Do **not** change `PlantsController` or any controller's `XTestContext.create(NaturalistDatabase.create())`. That de-fork is a separate later step.
- **Naming:** the impl class is `<Entity>RepositoryRdms` (parallels `<Entity>RepositoryMock`), e.g. `PlantSpeciesRepositoryRdms`.
- **Package:** each rdms class lives in the same package as the mock it extends (split-package across modules, as `plants-repository-test`/`plants-test-context` already do). Nine in `com.naturalist.plants`; one each in `com.naturalist.plants.cultivar`, `.heritage`, `.management`, `.phytochemistry`.
- **Marker:** `@DomainService` (`com.naturalist.infrastructure.DomainService`), never Spring's `@Repository`.
- **Bean uniqueness:** exactly one `@DomainService` bean per repository interface — rdms annotated, mock de-annotated.
- **No new dependencies.** `plants-repository-rdms` depends only on `plants-repository-test`.
- **Temporary DAG inversion is intended** (`plants-repository-rdms` → `plants-repository-test`), severed when the real SQL body lands.
- **No kernel change needed.** The `AbstractTestEntityRepository.ntsClass()` hierarchy-walk that `Rdms extends Mock` requires already shipped with insects (`ea82c1b`); this slice must not touch it.
- **Build:** verify with `mvn clean install` from the repo root.
- **Git:** print commit commands; commit only when the user says so. Keep the `Co-Authored-By` footer. Stay on `main` (trunk-based); do **not** run `git add -A`/`git add .` — unrelated untracked docs (`docs/plans/2026-08-21-n-plus-one-*`) must stay untracked. Use the scoped `git add` paths / `git rm` shown in each task.

---

## File Structure

Created:
- `domains/plants/plants-repository-rdms/pom.xml`
- Nine classes in `domains/plants/plants-repository-rdms/src/main/java/com/naturalist/plants/`:
  `PlantSpeciesRepositoryRdms.java`, `PlantOrderRepositoryRdms.java`, `PlantFamilyRepositoryRdms.java`, `PlantGenusRepositoryRdms.java`, `PlantEcologicalRoleRepositoryRdms.java`, `PlantFeatureRepositoryRdms.java`, `PlantFeatureAssignmentRepositoryRdms.java`, `PlantImageRepositoryRdms.java`, `PlantObservationRepositoryRdms.java`
- `.../com/naturalist/plants/cultivar/PlantCultivarRepositoryRdms.java`
- `.../com/naturalist/plants/heritage/PlantSeedLineageRepositoryRdms.java`
- `.../com/naturalist/plants/management/PlantProgramRepositoryRdms.java`
- `.../com/naturalist/plants/phytochemistry/PlantPhytochemicalConstituentRepositoryRdms.java`

Modified:
- `domains/plants/pom.xml` — add `<module>plants-repository-rdms</module>`.
- `pom.xml` (root) — add the `plants-repository-rdms` dependency-management entry.
- `apps/management-console/pom.xml` — add the `plants-repository-rdms` runtime dependency.
- The thirteen plant mocks — remove `@DomainService` and its import (paths in the table below).

Deleted:
- `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsDataConfiguration.java`

### The thirteen repositories (mock → rdms)

| Mock class                                   | rdms class to create                          | Package                                 |
|----------------------------------------------|-----------------------------------------------|-----------------------------------------|
| `PlantSpeciesRepositoryMock`                 | `PlantSpeciesRepositoryRdms`                  | `com.naturalist.plants`                 |
| `PlantOrderRepositoryMock`                   | `PlantOrderRepositoryRdms`                    | `com.naturalist.plants`                 |
| `PlantFamilyRepositoryMock`                  | `PlantFamilyRepositoryRdms`                   | `com.naturalist.plants`                 |
| `PlantGenusRepositoryMock`                   | `PlantGenusRepositoryRdms`                    | `com.naturalist.plants`                 |
| `PlantEcologicalRoleRepositoryMock`          | `PlantEcologicalRoleRepositoryRdms`          | `com.naturalist.plants`                 |
| `PlantFeatureRepositoryMock`                 | `PlantFeatureRepositoryRdms`                  | `com.naturalist.plants`                 |
| `PlantFeatureAssignmentRepositoryMock`       | `PlantFeatureAssignmentRepositoryRdms`       | `com.naturalist.plants`                 |
| `PlantImageRepositoryMock`                   | `PlantImageRepositoryRdms`                    | `com.naturalist.plants`                 |
| `PlantObservationRepositoryMock`             | `PlantObservationRepositoryRdms`             | `com.naturalist.plants`                 |
| `PlantCultivarRepositoryMock`                | `PlantCultivarRepositoryRdms`                | `com.naturalist.plants.cultivar`        |
| `PlantSeedLineageRepositoryMock`             | `PlantSeedLineageRepositoryRdms`             | `com.naturalist.plants.heritage`        |
| `PlantProgramRepositoryMock`                 | `PlantProgramRepositoryRdms`                 | `com.naturalist.plants.management`      |
| `PlantPhytochemicalConstituentRepositoryMock`| `PlantPhytochemicalConstituentRepositoryRdms`| `com.naturalist.plants.phytochemistry`  |

All thirteen mocks live under `domains/plants/plants-repository-test/src/main/java/` in the package shown.

---

## Task 1: Create and wire the `plants-repository-rdms` layer

Establish the module, its thirteen rdms classes, flip discovery from the mocks to the rdms classes, and put the module on the app classpath. Deliverable: a bootable app context where every plant repository bean is a `<Entity>RepositoryRdms` backed by the shared `NaturalistDatabase`.

**Files:** all Created and Modified paths above (not the delete — that is Task 2).

**Interfaces:**
- Consumes: `com.naturalist.data.NaturalistDatabase`, `com.naturalist.infrastructure.DomainService`, and the thirteen package-private `<Entity>RepositoryMock` `(NaturalistDatabase)` constructors in `plants-repository-test`.
- Produces: thirteen `@DomainService` `<Entity>RepositoryRdms` beans, each `extends` its mock, each with an `(NaturalistDatabase)` constructor calling `super(...)`; they become the sole beans for `PlantRepository.*Repository` and the four collapsed sub-context repository interfaces.

- [ ] **Step 1: Create the module POM**

Create `domains/plants/plants-repository-rdms/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>plants</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>plants-repository-rdms</artifactId>

    <dependencies>
        <!-- Temporary intermediate: the rdms repositories extend the in-memory mocks
             until the real SQL bodies land. This repository-module-on-repository-module
             dependency is the bounded DAG inversion documented in the design. -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>plants-repository-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register the module in the plants reactor**

In `domains/plants/pom.xml`, add to the `<modules>` block after `plants-repository-test`:

```xml
        <module>plants-repository-rdms</module>
```

- [ ] **Step 3: Add the root dependency-management entry**

In `pom.xml` (root), in `<dependencyManagement><dependencies>`, alongside the other `plants-*` entries (after `plants-repository-test`):

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>plants-repository-rdms</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Put the module on the app classpath**

In `apps/management-console/pom.xml`, add to `<dependencies>` (no `<version>`):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>plants-repository-rdms</artifactId>
        </dependency>
```

- [ ] **Step 5: Create the nine `com.naturalist.plants` rdms classes**

Under `domains/plants/plants-repository-rdms/src/main/java/com/naturalist/plants/`, each identical except the class name and `extends` target.

`PlantSpeciesRepositoryRdms.java`:

```java
package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate plant species repository: the production-named adapter the app
 * wires, temporarily backed by {@link PlantSpeciesRepositoryMock} until the SQL
 * body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class PlantSpeciesRepositoryRdms extends PlantSpeciesRepositoryMock {
    PlantSpeciesRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

Repeat the same shape (class name, `extends` target, trim the Javadoc's first noun) for:
`PlantOrderRepositoryRdms extends PlantOrderRepositoryMock`,
`PlantFamilyRepositoryRdms extends PlantFamilyRepositoryMock`,
`PlantGenusRepositoryRdms extends PlantGenusRepositoryMock`,
`PlantEcologicalRoleRepositoryRdms extends PlantEcologicalRoleRepositoryMock`,
`PlantFeatureRepositoryRdms extends PlantFeatureRepositoryMock`,
`PlantFeatureAssignmentRepositoryRdms extends PlantFeatureAssignmentRepositoryMock`,
`PlantImageRepositoryRdms extends PlantImageRepositoryMock`,
`PlantObservationRepositoryRdms extends PlantObservationRepositoryMock`.

- [ ] **Step 6: Create the four sub-package rdms classes**

Same shape, one per sub-package (package line matches each):

- `.../com/naturalist/plants/cultivar/PlantCultivarRepositoryRdms.java` — `package com.naturalist.plants.cultivar;`, `extends PlantCultivarRepositoryMock`
- `.../com/naturalist/plants/heritage/PlantSeedLineageRepositoryRdms.java` — `package com.naturalist.plants.heritage;`, `extends PlantSeedLineageRepositoryMock`
- `.../com/naturalist/plants/management/PlantProgramRepositoryRdms.java` — `package com.naturalist.plants.management;`, `extends PlantProgramRepositoryMock`
- `.../com/naturalist/plants/phytochemistry/PlantPhytochemicalConstituentRepositoryRdms.java` — `package com.naturalist.plants.phytochemistry;`, `extends PlantPhytochemicalConstituentRepositoryMock`

Each is `@DomainService class <Name> extends <Mock> { <Name>(NaturalistDatabase naturalistDatabase) { super(naturalistDatabase); } }` with the two imports.

- [ ] **Step 7: Verify the duplicate-bean failure (the red)**

Mocks are still `@DomainService`, so both mock and rdms are beans for each interface. Boot the full context:

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: FAIL — `NoUniqueBeanDefinitionException` for a `PlantRepository.*Repository` (or a collapsed sub-context repository), two candidates (the mock and the rdms). This proves `DomainServiceScan` registered the new rdms beans. If it passes, the module is not on the app classpath — recheck Steps 2–4.

- [ ] **Step 8: Remove `@DomainService` from the thirteen mocks**

In each of the thirteen mock files, delete the `@DomainService` annotation line and the `import com.naturalist.infrastructure.DomainService;` import. Change nothing else — classes stay package-private, constructors unchanged; they remain used directly by the `*TestContext` classes and `plants-core` tests (constructed with `new`, not via Spring).

- [ ] **Step 9: Verify the context boots (the green)**

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest,CatalogConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: PASS. One bean per repository interface (the rdms), the catalog bean graph assembles, and the admin page still lists the `plants` domain (rdms classes live under `com.naturalist.plants[.*]`).

- [ ] **Step 10: Full clean build**

```bash
mvn clean install
```
Expected: BUILD SUCCESS across all modules. `plants-core`/`plants-test-context` are unaffected — they construct mocks directly.

- [ ] **Step 11: Commit**

```bash
git add domains/plants/plants-repository-rdms domains/plants/pom.xml pom.xml apps/management-console/pom.xml domains/plants/plants-repository-test/src/main/java/com/naturalist/plants
git commit -m "feat(plants): add plants-repository-rdms intermediate repository layer

Production-named repository beans (@DomainService) backed temporarily by the
in-memory mocks (<Entity>RepositoryRdms extends <Entity>RepositoryMock across
plants + cultivar/heritage/management/phytochemistry); mocks lose @DomainService
so there is one bean per repository interface. Fast-follow of the insects slice.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```
(The single `git add` of `.../com/naturalist/plants` covers all four sub-packages' mock edits too, since they nest under it.)

---

## Task 2: Delete the obsolete `PlantsDataConfiguration`

`PlantsDataConfiguration` publishes only `@Bean <Entity>TestEntitySource` methods built with `new` — unconsumed, superseded by the rdms repository beans on the shared `NaturalistDatabase`. Remove it.

**Files:**
- Delete: `domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsDataConfiguration.java`

**Interfaces:** none consumed or produced (removal only).

- [ ] **Step 1: Confirm it is unconsumed**

```bash
grep -rn "PlantsDataConfiguration\|plantSource\|cultivarSource\|seedLineageSource\|plantProgramSource\|phytochemicalConstituentSource" --include="*.java" . | grep -v /target/
```
Expected: only the definition file itself. If any `@Autowired`/parameter consumer exists, stop and report — do not delete.

- [ ] **Step 2: Delete the file**

```bash
git rm domains/plants/plants-console/src/main/java/com/naturalist/plants/console/PlantsDataConfiguration.java
```

- [ ] **Step 3: Verify the app still builds and boots**

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest,CatalogConfigurationTest,PlantsControllerWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: PASS. (If `PlantsControllerWebMvcTest` does not exist, drop it from the list — the two `@SpringBootTest` classes are the required nets. The plants controller still forks its own graph, untouched by this plan.)

- [ ] **Step 4: Full clean build**

```bash
mvn clean install
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

`git rm` already staged the deletion — do NOT run `git add -A`. Commit directly:

```bash
git commit -m "refactor(plants): remove obsolete PlantsDataConfiguration @Bean sources

Dead workaround superseded by the plants-repository-rdms beans on the shared
NaturalistDatabase; the @Bean sources were unconsumed and duplicated instances
outside the getNamed registry.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (design → tasks):**
- Add `plants-repository-rdms` module → Task 1 Steps 1–4. ✓
- Intermediate impl extends the mock, `@DomainService`, injects shared `NaturalistDatabase`, across all five packages → Task 1 Steps 5–6. ✓
- Single bean per interface (mock loses `@DomainService`) → Task 1 Step 8, proven by Steps 7 (red) → 9 (green). ✓
- Remove dead `PlantsDataConfiguration` → Task 2. ✓
- Bounded DAG inversion (`-rdms` → `-repository-test`) → Task 1 Step 1 POM + comment. ✓
- No kernel change (already shipped) → Global Constraints. ✓
- Controllers left forking; plants-only scope → Global Constraints. ✓

**Placeholder scan:** none — every step carries literal content; the thirteen classes are specified by the worked example plus the mock→rdms table.

**Type consistency:** every `<Entity>RepositoryRdms` `extends` the exact `<Entity>RepositoryMock` from the table; all constructors are `(NaturalistDatabase naturalistDatabase)` calling `super(naturalistDatabase)`, matching the mocks' package-private `(NaturalistDatabase)` constructors. Marker is `com.naturalist.infrastructure.DomainService` in both create (rdms) and remove (mock) steps.

**Residual risk to watch during execution:**
- `PlantSpeciesRepositoryMock` and the other eight core mocks share the package `com.naturalist.plants`; the four sub-context mocks are in distinct packages — get each rdms class's `package` line right (it must match its mock).
- If `AdminDomainServicesControllerWebMvcTest` asserts a specific mock *class name* beyond domain headings, update it to the rdms name (the insects slice found it keys on `>plants</h2>`-style headings, unaffected).
