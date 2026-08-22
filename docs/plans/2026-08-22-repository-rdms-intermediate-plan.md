# Repository-RDMS Intermediate Layer (insects) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the management-console a production-named `insects-repository-rdms` module whose repository beans Spring discovers and injects, temporarily backed by the existing in-memory mocks — so the future swap to real SQL is "fill in the body," not "rewire the app."

**Architecture:** A new `insects-repository-rdms` module (split-package `com.naturalist.insects`, depending on `insects-repository-test`) holds one `<Entity>RepositoryRdms` class per repository. Each `extends` the existing `<Entity>RepositoryMock`, carries the `@DomainService` marker, and takes the shared `NaturalistDatabase` bean — so `DomainServiceScan` registers it as the single repository bean, reading/writing the one shared in-memory catalog via `getNamed`. The mocks lose `@DomainService` so there is exactly one bean per repository interface. The obsolete `InsectDataConfiguration` (dead `@Bean` sources) is deleted.

**Tech Stack:** Java 25, Maven (multi-module), Spring Boot (component scan + `adapters/spring-runtime` `DomainServiceScan`), JUnit 5, `@SpringBootTest`.

**Source design:** [docs/plans/2026-08-22-repository-rdms-intermediate-design.md](2026-08-22-repository-rdms-intermediate-design.md)

## Global Constraints

- **Scope is insects only.** Other domains follow as fast-follow PRs; do not touch them.
- **Leave the controllers forking.** Do **not** change `InsectsController` or any controller's `XTestContext.create(NaturalistDatabase.create())`. That is a separate, later, ArchUnit-enforced step. This plan does not touch controllers.
- **Naming:** the impl class is `<Entity>RepositoryRdms` (parallels `<Entity>RepositoryMock`).
- **Package:** rdms classes live in the same package as the mock they extend — `com.naturalist.insects` for the nine core repositories, `com.naturalist.insects.lifestage` for the life-stage one (split-package across modules, as `insects-repository-test`/`insects-test-context` already do).
- **Marker:** discovery uses `@DomainService` (`com.naturalist.infrastructure.DomainService`), never Spring's `@Repository`, for this intermediate.
- **Bean uniqueness:** exactly one `@DomainService` bean per repository interface. The rdms class is annotated; the mock is de-annotated.
- **No new dependencies.** `insects-repository-rdms` depends only on `insects-repository-test` (which transitively supplies `insects-api`, `framework`, `framework-test`).
- **Temporary DAG inversion is intended.** `insects-repository-rdms` → `insects-repository-test` is a bounded, named exception, severed when the real SQL body lands.
- **Build:** verify with `mvn clean install` from the repo root — a new module plus classpath changes; per project convention incremental builds leave stale classes ([kernel change → clean install](../../MEMORY.md)).
- **Git:** print commit commands; commit only when the user says so. Keep the `Co-Authored-By` footer. Trunk-based — branch, do not open a PR unless asked.

---

## File Structure

Created:
- `domains/insects/insects-repository-rdms/pom.xml` — the new module.
- `domains/insects/insects-repository-rdms/src/main/java/com/naturalist/insects/InsectSpeciesRepositoryRdms.java`
- `.../com/naturalist/insects/InsectImageRepositoryRdms.java`
- `.../com/naturalist/insects/InsectOrderRepositoryRdms.java`
- `.../com/naturalist/insects/InsectGenusRepositoryRdms.java`
- `.../com/naturalist/insects/InsectFamilyRepositoryRdms.java`
- `.../com/naturalist/insects/InsectFunctionalRoleRepositoryRdms.java`
- `.../com/naturalist/insects/InsectFeatureRepositoryRdms.java`
- `.../com/naturalist/insects/InsectFeatureAssignmentRepositoryRdms.java`
- `.../com/naturalist/insects/InsectObservationRepositoryRdms.java`
- `.../com/naturalist/insects/lifestage/InsectLifeStageEntityRepositoryRdms.java`

Modified:
- `domains/insects/pom.xml` — add `<module>insects-repository-rdms</module>`.
- `pom.xml` (root) — add the `insects-repository-rdms` dependency-management entry.
- `apps/management-console/pom.xml` — add the `insects-repository-rdms` runtime dependency (puts the beans on the app classpath for discovery).
- The ten insects repository mocks — remove `@DomainService` and its import:
  - `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectSpeciesRepositoryMock.java`
  - `.../InsectImageRepositoryMock.java`, `.../InsectOrderRepositoryMock.java`, `.../InsectGenusRepositoryMock.java`, `.../InsectFamilyRepositoryMock.java`, `.../InsectFunctionalRoleRepositoryMock.java`, `.../InsectFeatureRepositoryMock.java`, `.../InsectFeatureAssignmentRepositoryMock.java`, `.../InsectObservationRepositoryMock.java`
  - `.../lifestage/InsectLifeStageEntityRepositoryMock.java`

Deleted:
- `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectDataConfiguration.java`

### The ten repositories (mock → rdms)

| Mock class (extends)                       | rdms class to create             | Package                            |
|--------------------------------------------|----------------------------------|------------------------------------|
| `InsectSpeciesRepositoryMock`              | `InsectSpeciesRepositoryRdms`            | `com.naturalist.insects`           |
| `InsectImageRepositoryMock`                | `InsectImageRepositoryRdms`              | `com.naturalist.insects`           |
| `InsectOrderRepositoryMock`                | `InsectOrderRepositoryRdms`              | `com.naturalist.insects`           |
| `InsectGenusRepositoryMock`                | `InsectGenusRepositoryRdms`              | `com.naturalist.insects`           |
| `InsectFamilyRepositoryMock`               | `InsectFamilyRepositoryRdms`             | `com.naturalist.insects`           |
| `InsectFunctionalRoleRepositoryMock`       | `InsectFunctionalRoleRepositoryRdms`     | `com.naturalist.insects`           |
| `InsectFeatureRepositoryMock`              | `InsectFeatureRepositoryRdms`            | `com.naturalist.insects`           |
| `InsectFeatureAssignmentRepositoryMock`    | `InsectFeatureAssignmentRepositoryRdms`  | `com.naturalist.insects`           |
| `InsectObservationRepositoryMock`          | `InsectObservationRepositoryRdms`        | `com.naturalist.insects`           |
| `InsectLifeStageEntityRepositoryMock`      | `InsectLifeStageEntityRepositoryRdms`    | `com.naturalist.insects.lifestage` |

---

## Task 1: Create and wire the `insects-repository-rdms` layer

Establish the module, its ten rdms repository classes, flip discovery from the mocks to the rdms classes, and put the module on the app classpath. The deliverable is a bootable app context in which every insects repository bean is an `…RepositoryRdms` instance backed by the one shared `NaturalistDatabase`.

**Files:** all Created and Modified paths above (not the delete — that is Task 2).

**Interfaces:**
- Consumes: `com.naturalist.data.NaturalistDatabase` (framework-test), `com.naturalist.infrastructure.DomainService` (framework), and the ten package-private/protected `<Entity>RepositoryMock` constructors `(NaturalistDatabase)` in `insects-repository-test`.
- Produces: ten `@DomainService`-annotated `<Entity>RepositoryRdms` beans, each `extends` its mock, each with an `(NaturalistDatabase)` constructor calling `super(...)`. These become the sole beans for `InsectRepository.*Repository` / `LifeStageRepository.LifeStageEntityRepository`.

- [ ] **Step 1: Create the module POM**

Create `domains/insects/insects-repository-rdms/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>insects</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>insects-repository-rdms</artifactId>

    <dependencies>
        <!-- Temporary intermediate: the rdms repositories extend the in-memory mocks
             until the real SQL bodies land. This repository-module-on-repository-module
             dependency is the bounded DAG inversion documented in the design. -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>insects-repository-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register the module in the insects reactor**

In `domains/insects/pom.xml`, add the module in the `<modules>` block (alphabetical, after `insects-repository-test`):

```xml
        <module>insects-repository-rdms</module>
```

- [ ] **Step 3: Add the root dependency-management entry**

In `pom.xml` (root), in `<dependencyManagement><dependencies>`, alongside the other `insects-*` entries (after `insects-repository-test`):

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>insects-repository-rdms</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Put the module on the app classpath**

In `apps/management-console/pom.xml`, add to `<dependencies>` (no `<version>` — inherited):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>insects-repository-rdms</artifactId>
        </dependency>
```

- [ ] **Step 5: Create the nine `com.naturalist.insects` rdms classes**

Each is identical except for the type name. Create all nine under
`domains/insects/insects-repository-rdms/src/main/java/com/naturalist/insects/`.

`InsectSpeciesRepositoryRdms.java`:

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects species repository: the production-named adapter the
 * app wires, temporarily backed by {@link InsectSpeciesRepositoryMock} until
 * the SQL body lands (design: docs/plans/2026-08-22-repository-rdms-intermediate-design.md).
 */
@DomainService
class InsectSpeciesRepositoryRdms extends InsectSpeciesRepositoryMock {
    InsectSpeciesRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

Repeat with the same shape (adjust the class name and the `extends` target, and trim the Javadoc's first noun) for:
`InsectImageRepositoryRdms extends InsectImageRepositoryMock`,
`InsectOrderRepositoryRdms extends InsectOrderRepositoryMock`,
`InsectGenusRepositoryRdms extends InsectGenusRepositoryMock`,
`InsectFamilyRepositoryRdms extends InsectFamilyRepositoryMock`,
`InsectFunctionalRoleRepositoryRdms extends InsectFunctionalRoleRepositoryMock`,
`InsectFeatureRepositoryRdms extends InsectFeatureRepositoryMock`,
`InsectFeatureAssignmentRepositoryRdms extends InsectFeatureAssignmentRepositoryMock`,
`InsectObservationRepositoryRdms extends InsectObservationRepositoryMock`.

- [ ] **Step 6: Create the life-stage rdms class**

Create `domains/insects/insects-repository-rdms/src/main/java/com/naturalist/insects/lifestage/InsectLifeStageEntityRepositoryRdms.java`:

```java
package com.naturalist.insects.lifestage;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

/**
 * Intermediate insects life-stage repository: production-named adapter,
 * temporarily backed by {@link InsectLifeStageEntityRepositoryMock}.
 */
@DomainService
class InsectLifeStageEntityRepositoryRdms extends InsectLifeStageEntityRepositoryMock {
    InsectLifeStageEntityRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

(`InsectLifeStageEntityRepositoryMock` is a `public` class with a `protected` constructor; a same-package subclass calls `super(...)` fine.)

- [ ] **Step 7: Verify the duplicate-bean failure (the red)**

The mocks are still `@DomainService` at this point, so both mock and rdms are beans for each interface. Boot the full context to prove the rdms beans are discovered:

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest
```
Expected: FAIL — context startup throws `NoUniqueBeanDefinitionException` for an `InsectRepository.*Repository` (two candidates: the mock and the rdms). This failure is the proof that `DomainServiceScan` registered the new rdms beans. If it instead passes, the module is not on the app classpath — recheck Steps 2–4.

- [ ] **Step 8: Remove `@DomainService` from the ten mocks**

In each of the ten mock files, delete the `@DomainService` annotation line and the
`import com.naturalist.infrastructure.DomainService;` import. Change nothing else — the classes stay package-private (the life-stage one stays `public`), keep their constructors, and remain used directly by `InsectsTestContext` / `InsectLifeStageTestContext` and the `insects-core` tests (which construct them with `new`, not via Spring).

- [ ] **Step 9: Verify the context boots (the green)**

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest,CatalogConfigurationTest
```
Expected: PASS. Exactly one bean per repository interface (the rdms), the catalog bean graph assembles, and the admin page still lists the `insects` domain (the rdms classes live under `com.naturalist.insects`, so the domain grouping is unchanged).

- [ ] **Step 10: Full clean build**

```bash
mvn clean install
```
Expected: BUILD SUCCESS across all modules. (`insects-core`/`insects-test-context` are unaffected — they construct mocks directly; only the app context swapped mock→rdms.)

- [ ] **Step 11: Commit**

```bash
git add domains/insects/insects-repository-rdms domains/insects/pom.xml pom.xml apps/management-console/pom.xml domains/insects/insects-repository-test/src/main/java/com/naturalist/insects
git commit -m "feat(insects): add insects-repository-rdms intermediate repository layer

Production-named repository beans (@DomainService) backed temporarily by the
in-memory mocks; mocks lose @DomainService so there is one bean per repository
interface. Wires the app onto the shared NaturalistDatabase via getNamed.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Delete the obsolete `InsectDataConfiguration`

`InsectDataConfiguration` publishes only `@Bean <Entity>TestEntitySource` methods built with `new` — the console workaround from before repositories were in the context. It is unconsumed by any injection point and is superseded by the rdms repository beans on the shared `NaturalistDatabase`. Remove it.

**Files:**
- Delete: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectDataConfiguration.java`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing (removal only).

- [ ] **Step 1: Confirm it is unconsumed**

```bash
grep -rn "InsectDataConfiguration\|insectSpeciesSource\|insectImageSource" --include="*.java" . | grep -v /target/
```
Expected: only the definition file itself (no `@Autowired`/parameter consumer). If any consumer exists, stop and report — do not delete.

- [ ] **Step 2: Delete the file**

```bash
git rm domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectDataConfiguration.java
```

- [ ] **Step 3: Verify the app still builds and boots**

```bash
mvn -q -pl apps/management-console -am test -Dtest=AdminDomainServicesControllerWebMvcTest,CatalogConfigurationTest,InsectsControllerWebMvcTest
```
Expected: PASS. (`InsectsControllerWebMvcTest` still passes because the controller forks its own graph via `InsectsTestContext` — untouched by this plan.)

- [ ] **Step 4: Full clean build**

```bash
mvn clean install
```
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(insects): remove obsolete InsectDataConfiguration @Bean sources

Dead workaround superseded by the insects-repository-rdms beans on the shared
NaturalistDatabase; the @Bean sources were unconsumed and duplicated instances
outside the getNamed registry.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (design → tasks):**
- Add `<domain>-repository-rdms` module (insects) → Task 1 Steps 1–4. ✓
- Intermediate impl extends the mock, `@DomainService`, injects shared `NaturalistDatabase` → Task 1 Steps 5–6. ✓
- Single bean per interface (mock loses `@DomainService`) → Task 1 Step 8, proven by Steps 7 (red) → 9 (green). ✓
- Remove dead `*DataConfiguration` (insects) → Task 2. ✓
- Bounded DAG inversion (`-rdms` → `-repository-test`) → Task 1 Step 1 POM + comment. ✓
- Controllers left forking; no controller edits → Global Constraints + Task 2 Step 3 note. ✓
- Insects-only scope → Global Constraints. ✓

**Placeholder scan:** none — every code/config/command step carries literal content, and the nine identical rdms classes are fully specified by the one worked example plus the mock→rdms table.

**Type consistency:** every `<Entity>RepositoryRdms` `extends` the exact `<Entity>RepositoryMock` from the inventory table; all constructors are `(NaturalistDatabase naturalistDatabase)` calling `super(naturalistDatabase)`, matching the mocks' `(NaturalistDatabase)` constructors. The marker is `com.naturalist.infrastructure.DomainService` in both the create (rdms) and remove (mock) steps.

**Residual risk to watch during execution:**
- If `AdminDomainServicesControllerWebMvcTest` asserts a specific mock *class name* anywhere beyond the domain headings (Steps 7/9 exercise it), update that assertion to the rdms class name. The visible assertions key on `>insects</h2>` headings and per-domain representative `DomainId` beans, which are unaffected — but confirm the full test body.
- Split-package `com.naturalist.insects` now spans `-api`, `-repository-test`, and `-repository-rdms`. The project uses classpath (not JPMS), and `-test-context` already split-packages, so this is supported; if a JPMS `module-info` is ever introduced this becomes a concern.
