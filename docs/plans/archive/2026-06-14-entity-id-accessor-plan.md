# Entity Identity Accessor (`id`, not `name`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `Entity` records carry their surrogate identity as `id` (accessor `id()`) instead of `name`, while keeping the unified `Named<KEY>` data-layer port intact.

**Architecture:** Split the infrastructural port from the branch-semantic accessors. `Named<KEY>` declares one neutral port method `key()`; `NamedEntity` declares `name()` and `Entity` declares `id()`, each supplying `key()` as a free delegating default. `EntityName`/`EntityId` value types and the data-layer method verbs (`getByName`, …) are untouched. Full rationale: [`2026-06-14-entity-id-accessor-design.md`](2026-06-14-entity-id-accessor-design.md).

**Tech Stack:** Java records, JSpecify nullability, Jackson (record-native deserialization), the project's `Observer`/`Constraints` invariant framework, Maven multi-module build.

---

## Repo conventions for this plan

- **Builds are run by the user.** Do not invoke `mvn`. Where a step says *Request build*, hand the exact command to the user and wait for the result before proceeding. Failures concentrate in console / Spring Boot / JTE modules — check those last.
- **Commits:** the executing agent **stages only** (`git add`). Stop, let the user review the diff, then the controller commits after approval. The `git commit` lines below are the intended commit boundaries, not authorization to auto-commit. Keep the standard `Co-Authored-By: Claude …` footer. Direct commits to `main` (trunk-based); the user runs `git push`.
- **This is a rename refactor, not new behavior.** The "test" is the existing suite plus the compiler. No new red-green TDD cycles are added; per the kernel testing convention, the delegating defaults are covered transitively by every domain's existing contract tests once they call `key()`. Sequence the kernel change first so the compiler enumerates the dependent edits.
- **Re-grep before trusting any file list.** The `Entity` branch is growing (an in-flight `CitationAssociation` in the library domain already uses the `…Id name` shape). Run `grep -rln "implements Entity<" --include="*.java" domains kernels | grep -v worktrees | grep -v /target/` at the start of Task 5 and treat its output as authoritative.

---

## File structure

| File | Responsibility | Task |
|------|----------------|------|
| `kernels/framework/src/main/java/com/naturalist/ddd/Named.java` | port: `key()` | 1 |
| `kernels/framework/src/main/java/com/naturalist/ddd/NamedEntity.java` | `name()` + `key()` default | 1 |
| `kernels/framework/src/main/java/com/naturalist/ddd/Entity.java` | `id()` + `key()` default | 1 |
| `kernels/framework-test/.../data/{TestEntitySource,AbstractTestEntityRepository,EntityQueryContractTest,EntityCommandContractTest,EntityRepositoryTest}.java` | internal `.name()` → `.key()` | 2 |
| `domains/chemistry/.../compound/CompoundDepiction.java` + `depictions.json` + read sites | chemistry Entity | 3 |
| `domains/insects/.../{InsectImage,InsectFunctionalRole}.java` + 2 fixtures + read sites | insects Entities | 4 |
| api-only `Entity` records (sensors/soil/zone/weather) | record + invariant only | 5 |
| any in-flight `Entity` records surfaced by re-grep | same treatment | 5 |
| `docs/adr/rationale/ADR-022-entity-identity-unified.md`, `domains/CLAUDE.md`, `domains/insects/CLAUDE.md` | docs | 6 |

---

## Task 1: Kernel port split

**Files:**
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/Named.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/NamedEntity.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/ddd/Entity.java`

- [ ] **Step 1: Rename the port method and type parameter in `Named.java`**

Replace the interface body (keep the existing class Javadoc, updating `name()` → `key()` references in prose):

```java
public interface Named<KEY> extends Observable {
    KEY key();
}
```

- [ ] **Step 2: Give `NamedEntity` its own `name()` and a delegating `key()`**

```java
public interface NamedEntity<NAME extends EntityName> extends Named<NAME> {
    NAME name();
    default NAME key() {
        return name();
    }
}
```

- [ ] **Step 3: Give `Entity` an `id()` and a delegating `key()`**

```java
public interface Entity<ID extends EntityId> extends Named<ID> {
    ID id();
    default ID key() {
        return id();
    }
}
```

- [ ] **Step 4: Request build of the framework module only**

Ask the user to run:
```
mvn -q -pl kernels/framework -am test-compile
```
Expected: BUILD SUCCESS. (`framework` has no internal callers of `name()` on `Named`, so it compiles standalone. Downstream modules will not compile until Task 2+; that is expected.)

- [ ] **Step 5: Stage for commit (commit after user review)**

```bash
git add kernels/framework/src/main/java/com/naturalist/ddd/Named.java \
        kernels/framework/src/main/java/com/naturalist/ddd/NamedEntity.java \
        kernels/framework/src/main/java/com/naturalist/ddd/Entity.java
# intended: git commit -m "refactor(framework): split Named port (key) from name()/id() branches"
```

---

## Task 2: Generic data layer (`framework-test`)

`Entity.name()` no longer exists, so the generic data layer that calls `.name()` on a `Named<KEY>`-typed value must call `.key()`. **Method names stay** (`getByName`, `getByNameSet`, `notFoundName`, `knownEntityNames`). **Two calls must NOT change** — they are constraint/column labels, not entity keys.

**Files:**
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/TestEntitySource.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/AbstractTestEntityRepository.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/EntityQueryContractTest.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/EntityCommandContractTest.java`
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/EntityRepositoryTest.java`

- [ ] **Step 1: `TestEntitySource.java` — swap entity-keyed `.name()` → `.key()`**

Change these (lines approximate — match on content):
- `.filter(e -> nameSet.contains(e.name()))` → `.filter(e -> nameSet.contains(e.key()))`
- `.sorted(Comparator.comparing(e -> e.name().toString()))` → `.key().toString()` (both occurrences, incl. the origin-file grouping block)
- `.filter(e -> excludeName == null || !e.name().equals(excludeName))` → `!e.key().equals(excludeName)`
- the three `NAME name = entity.name();` → `NAME name = entity.key();` (keep the local var name `name`; only the accessor changes)
- `originFile.put(entity.name(), relativePath)` → `originFile.put(entity.key(), relativePath)`
- `.filter(e -> originFile.get(e.name()) != null)` → `originFile.get(e.key())`
- `.collect(Collectors.groupingBy(e -> originFile.get(e.name())))` → `originFile.get(e.key())`

**Leave unchanged:**
- `throw new UniqueConstraintException(entity, uniqueConstraint.name(), entityValue);` — `uniqueConstraint.name()` is a constraint label.
- `throw new ForeignKeyConstraintException(entity, fk.name(), foreignValue);` — `fk.name()` is a column label.

- [ ] **Step 2: `AbstractTestEntityRepository.java`**

`.filter(e -> nameSet.contains(e.name()))` → `.filter(e -> nameSet.contains(e.key()))`

- [ ] **Step 3: `EntityQueryContractTest.java`**

- `assertThat(result.get().name()).isEqualTo(known);` → `result.get().key()`
- both `.map(e -> e.name())` → `.map(e -> e.key())`

- [ ] **Step 4: `EntityCommandContractTest.java` and `EntityRepositoryTest.java`**

In both files, the `Named`-typed reads become `.key()` while the **method name `getByName` stays**:
- `query().getByName(entity.name())` → `query().getByName(entity.key())`
- `query().getByName(original.name())` → `query().getByName(original.key())`
- `assertThat(persisted.name()).isEqualTo(original.name());` → `assertThat(persisted.key()).isEqualTo(original.key());`
- `repository().getByName(entity.name())` → `repository().getByName(entity.key())`
- `repository().getByName(original.name())` → `repository().getByName(original.key())`

- [ ] **Step 5: Request build of framework + framework-test**

Ask the user to run:
```
mvn -q -pl kernels/framework,kernels/framework-test -am test-compile
```
Expected: BUILD SUCCESS for both kernels. (Domains still red until their tasks land.)

- [ ] **Step 6: Stage for commit**

```bash
git add kernels/framework-test/src/main/java/com/naturalist/data/
# intended: git commit -m "refactor(framework-test): call Named.key() in generic data layer"
```

---

## Task 3: Chemistry — `CompoundDepiction`

**Files:**
- Modify: `domains/chemistry/chemistry-api/src/main/java/com/naturalist/chemistry/compound/CompoundDepiction.java`
- Modify: `domains/chemistry/chemistry-repository-test/src/main/resources/chemistry/compound/depictions.json`
- Modify (compiler-enumerated): chemistry `-core`, `-repository-test` read sites and `TestChemistryIdentifiers`

- [ ] **Step 1: Rename the record component and invariant**

In `CompoundDepiction.java`: rename the first component `DepictionId name` → `DepictionId id`, update the accessor reference in `invariants()`:
```java
.entityId(id, "id")
```
(from `.entityId(name, "name")`). Update any Javadoc referencing the component.

- [ ] **Step 2: Relabel the fixture field**

In `depictions.json`, rename every object's `"name"` key (which holds the UUID, e.g. `"01970000-0001-7001-8001-000000000001"`) to `"id"`. Leave `compoundName`, `smiles`, `note` untouched.

- [ ] **Step 3: Fix read sites the compiler flags**

Request:
```
mvn -q -pl domains/chemistry/chemistry-api,domains/chemistry/chemistry-core,domains/chemistry/chemistry-repository-test -am test-compile
```
For each `cannot find symbol: method name()` on a `CompoundDepiction`, replace `.name()` → `.id()`. Known candidate: any depiction read in `chemistry-core` query/command impls and their tests. Optional consistency: in `TestChemistryIdentifiers`, the `DepictionId depictionName = …` constant fields may be renamed `depictionId` (declaration-only; not required for compilation — do it only if it reads better and update references in the same edit).

- [ ] **Step 4: Request chemistry verify**

```
mvn -q -pl domains/chemistry/chemistry-api,domains/chemistry/chemistry-core,domains/chemistry/chemistry-repository-test -am verify
```
Expected: BUILD SUCCESS, all chemistry tests green. The depiction insert/expected-result contract test walks the full constraint graph and will fail loudly if the fixture field was mislabeled.

- [ ] **Step 5: Stage for commit**

```bash
git add domains/chemistry/
# intended: git commit -m "refactor(chemistry): CompoundDepiction identity component name -> id"
```

---

## Task 4: Insects — `InsectImage` and `InsectFunctionalRole`

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFunctionalRole.java`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-images.json`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-functional-roles.json`
- Modify (compiler-enumerated): insects `-core` read sites and `TestInsectsIdentifiers`

- [ ] **Step 1: `InsectImage.java` — component + invariant + Javadoc**

Rename the first component `InsectImageId name` → `InsectImageId id`. In `invariants()`: `.entityId(name, "name")` → `.entityId(id, "id")`. Update the class Javadoc (it currently refers to identity as the UUID-based id, which now matches the accessor).

- [ ] **Step 2: `InsectFunctionalRole.java` — component + invariant**

Rename `InsectFunctionalRoleId name` → `InsectFunctionalRoleId id`; `.entityId(name, "name")` → `.entityId(id, "id")`.

- [ ] **Step 3: Relabel both fixtures**

In `insect-images.json` and `insect-functional-roles.json`, rename each object's `"name"` key (the UUID) to `"id"`. Leave `parentRank`, `parentName`, `dateAdded`, `resourceName`, `guilds`, `beneficial` untouched. (If editing programmatically, dump with `ensure_ascii=False` to preserve any non-ASCII in `note`/description fields.)

- [ ] **Step 4: Fix read sites the compiler flags**

Request:
```
mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-repository-test -am test-compile
```
Replace `.name()` → `.id()` only where the receiver is an `InsectImage`/`InsectFunctionalRole`. Known site: `domains/insects/insects-core/src/test/java/com/naturalist/insects/ImageCommandImplTest.java` — `original.name()` → `original.id()`. **Do not** touch `image.parentName()` (a field) or `species.name()`/`genus.name()` in `InsectTaxonViewFactory` (those are `NamedEntity` slugs and stay). Optional consistency: `TestInsectsIdentifiers` `InsectImageId name` / `InsectFunctionalRoleId name` constant declarations may be renamed to `id` (declaration-only).

- [ ] **Step 5: Request insects verify**

```
mvn -q -pl domains/insects/insects-api,domains/insects/insects-core,domains/insects/insects-repository-test -am verify
```
Expected: BUILD SUCCESS. The image/role contract tests and `InsectTaxonViewFactoryTest` exercise the renamed fixtures end to end.

- [ ] **Step 6: Stage for commit**

```bash
git add domains/insects/
# intended: git commit -m "refactor(insects): InsectImage/InsectFunctionalRole identity component name -> id"
```

---

## Task 5: Remaining `Entity` records (api-only + in-flight)

These `Entity` records exist in `-api` but currently have **no repository-test fixtures**, so the change is the record component + invariant only. Re-grep first — the list below is a snapshot.

**Files (verify by grep):**
- `domains/sensors/.../SensorReading.java`
- `domains/zone/.../ZonePrecipitationEvent.java`
- `domains/soil/.../event/{PrecipitationEvent,IrrigationEvent,TillageEvent,AmendmentEvent}.java`
- `domains/soil/.../observation/LabAnalysis.java`
- `domains/weather/.../PrecipitationEvent.java`
- **Any record surfaced by the re-grep** (e.g. `domains/library/.../CitationAssociation.java` if that effort has landed).

- [ ] **Step 1: Authoritative re-grep**

Run:
```
grep -rln "implements Entity<" --include="*.java" domains kernels | grep -v worktrees | grep -v /target/
```
Cross off the records already done in Tasks 3–4. Everything remaining is this task.

- [ ] **Step 2: For each record, rename component + invariant**

In each file: rename the identity component `XxxId name` → `XxxId id`, and `.entityId(name, "name")` → `.entityId(id, "id")`. If the record has a fixture (re-grep `*-repository-test/**/*.json` for the file), relabel its `"name"` UUID field to `"id"` and fix the compiler-flagged read sites exactly as in Tasks 3–4.

- [ ] **Step 3: Request build of each affected domain**

For each domain `D` touched, request:
```
mvn -q -pl domains/D/D-api -am verify
```
(add `-core`/`-repository-test` for any domain that has them). Expected: BUILD SUCCESS.

- [ ] **Step 4: Stage for commit (one commit per domain, or grouped if trivial)**

```bash
git add domains/<domain>/
# intended: git commit -m "refactor(<domain>): Entity identity component name -> id"
```

---

## Task 6: Documentation

**Files:**
- Modify: `docs/adr/rationale/ADR-022-entity-identity-unified.md`
- Modify: `domains/CLAUDE.md`
- Modify: `domains/insects/CLAUDE.md`

- [ ] **Step 1: ADR-022 clarification**

Add a short note (Consequences or a new "Accessor convention" subsection): the shared `Named<KEY>` port accessor is `key()`; `NamedEntity` exposes `name()` (slug) and `Entity` exposes `id()` (UUIDv7), each delegating `key()`. Clarify the Applicability Signal: a JSON `"id"` field carrying the **domain `EntityId`** is correct; the forbidden case remains a persistence / `Long` surrogate key. Update the `Named<KEY>` snippet so the prose matches the Java.

- [ ] **Step 2: `domains/CLAUDE.md`**

Update the Identity Model bullet for `Entity<ID extends EntityId>` to note the accessor is `id()` (component `id`), and any line that implies the identity component is `name`.

- [ ] **Step 3: `domains/insects/CLAUDE.md`**

Update the `InsectImage` vocabulary line — it carries `InsectImageId id`, accessor `id()`.

- [ ] **Step 4: Stage for commit**

```bash
git add docs/adr/rationale/ADR-022-entity-identity-unified.md domains/CLAUDE.md domains/insects/CLAUDE.md
# intended: git commit -m "docs: Entity identity accessor is id(); Named port is key()"
```

---

## Task 7: Full verification

- [ ] **Step 1: Request full build**

Ask the user to run, from repo root:
```
mvn verify
```
Expected: BUILD SUCCESS across all modules. Pay extra attention to console / Spring Boot / JTE modules in the output.

- [ ] **Step 2: Grep for stragglers**

```
grep -rn "\.entityId([a-zA-Z]*, \"name\")" --include="*.java" domains | grep -v worktrees | grep -v /target/
```
Expected: no results (every `Entity` invariant now reads `.entityId(id, "id")`).

```
grep -rln "\"name\"" --include="*.json" domains | xargs grep -l "7[0-9a-f]\{3\}-" 2>/dev/null
```
Spot-check: no Entity fixture still labels a UUID under `"name"`. (NamedEntity fixtures legitimately keep `"name"` for slugs — verify each hit is a slug, not a UUID.)

- [ ] **Step 3: Final review + commit boundary**

Present the complete diff for user review. After approval, ensure each task's commit is in place (or squash per the user's preference).

---

## Self-review notes

- **Spec coverage:** kernel split (Task 1), data layer (Task 2), the two fixture-bearing domains (Tasks 3–4), api-only + in-flight records (Task 5), docs incl. ADR clarification (Task 6), full verify (Task 7) — every design section maps to a task.
- **Verbs unchanged:** the plan explicitly preserves `getByName`/`getByNameSet`/`notFoundName`/`knownEntityNames` and only swaps the accessor in their bodies — matching the "keep verbs" decision.
- **Non-key `name()` preserved:** `uniqueConstraint.name()`, `fk.name()`, `image.parentName()`, and `NamedEntity.name()` slug reads are called out as must-not-change.
- **Growing branch:** Task 5 leads with an authoritative re-grep rather than trusting the snapshot list, covering the in-flight `CitationAssociation`.
