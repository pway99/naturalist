# Identification Cost Controls Implementation Plan (Revision 2 — `usage` domain module)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cap Anthropic API spend on organism identification with a per-user daily quota plus global rate/daily/monthly ceilings, enforced inside the identification use case and monitored from the management console (no operational logging exists — alerts surface in-app and by email).

**Architecture:** A new `usage` **domain module** (`domains/usage/{usage-api,usage-core,usage-repository-test,usage-repository-rdms}`) owns the counters and alerts. `usage-api` exposes the `IdentificationBudget` (write) and `UsageMonitor` (read) service ports; `insects-core` calls `budget.reserve(naturalist)` at the top of `identify(...)`, so every UI surface inherits enforcement. Counting is by identification, keyed by `NaturalistName`. Persistence follows the house pattern: an in-memory mock in `usage-repository-test`, an `@DomainService` `repository-rdms` subclass that production wires (in-memory placeholder today, like all six domains). The reserve is a `synchronized` core operation — single-JVM atomic, resets on restart — until the project wires a real DB app-wide.

**Tech Stack:** Java 21 records/sealed types, Maven multi-module, JUnit 5 + AssertJ, the `EntityRepository`/`TestEntitySource`/`NaturalistDatabase` framework, Spring Boot (management-console app), JTE. New app infrastructure this plan introduces: `spring-boot-starter-mail`, `@EnableScheduling`.

> **Starting state:** The branch is reset to base `5aa81694` before executing; the earlier kernel-based commits (`kernels/usage`, in-memory budget) are superseded by this revision. The design/plan docs under `docs/plans/` are untracked and survive the reset.

## Global Constraints

- **Typed identifiers only.** Ports and entities key on `com.naturalist.naturalist.NaturalistName`, `UsageCounterName`, `UsageTallyId`, `UsageAlertId` — never raw `String`/`UUID` across a boundary. `EntityName` values are lower-kebab-case (`^[a-z0-9]+(-[a-z0-9]+)*$`); `EntityId` is UUIDv7 via the kernel generator (never `UUID.randomUUID()`).
- **New module registration** (root `CLAUDE.md` §"Creating a New Module"): each sub-module gets its own `pom.xml`; add every sub-module to `domains/usage/pom.xml` `<modules>`; add a `${project.version}` entry per sub-module to the ROOT `pom.xml` `<dependencyManagement>`; add `<module>usage</module>` to `domains/pom.xml`. Missing a root entry breaks the reactor.
- **DAG:** `usage-api` → framework, identifiers, field-notes only. `usage-core` → usage-api (+ framework). `usage-repository-test` → usage-api, framework-test, identifiers-test. `usage-repository-rdms` → usage-repository-test. `insects-core` → usage-api (core may import another domain's **api**). No Spring in api/core/repository code — only the app wires beans.
- **Repository rules:** repository interfaces are package-private (nested `protected` in the `UsageRepository` namespace class). Domain-specific repo methods validate arguments via `observer().arguments(...).throwWhenInvalid()` and get a null-rejection contract test. The reserve issues a fixed set of finder calls (not a select looped over a prior result set), so the N+1 gate does not apply — keep it that way.
- **Never weaken a test or gate to go green.**
- **Kernel/signature changes need a clean build:** after adding modules or changing a cross-module signature, run `mvn -q install -DskipTests` from the worktree root, not an incremental `-pl` build.
- **Completeness gate** at each milestone boundary: `mvn -q verify` then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`.
- **Defaults (Conservative ~$40/mo):** per-user-daily 10, global-rate-per-minute 3, global-daily 50, global-monthly 650, warning-percent 80 — from `application.yml`.

---

## Authoritative interfaces (every task uses these exact names)

All in package `com.naturalist.usage` unless noted.

```java
// ---- usage-api: value types ----
public enum LimitKind { PER_USER, RATE, DAILY, MONTHLY }
public enum AlertScope { DAILY, MONTHLY }
public enum AlertKind { WARNING, HARD_STOP }

public record UsageLimits(int perUserDaily, int globalRatePerMinute,
                          int globalDaily, int globalMonthly, int warningPercent) {}

public record UsageSnapshot(int dailyUsed, int dailyLimit, int monthlyUsed, int monthlyLimit,
                            int rateUsed, int ratePerMinute, java.util.List<UserUsage> users) {
    public record UserUsage(String naturalist, int used, int limit) {}
}

public final class BudgetExceededException extends RuntimeException {   // (LimitKind, Instant)
    public LimitKind limitKind();
    public java.time.Instant resetAt();
}

// ---- usage-api: service ports ----
public interface IdentificationBudget {
    void reserve(com.naturalist.naturalist.NaturalistName naturalist); // throws BudgetExceededException
    static IdentificationBudget noOp() { return NoOpIdentificationBudget.INSTANCE; }
}
public interface UsageMonitor {
    UsageSnapshot snapshot();
    java.util.List<UsageAlert> activeAlerts();     // unacknowledged, newest-first
    void acknowledge(UsageAlertId id);
    java.util.List<UsageAlert> claimUnsentAlerts(); // marks emailed=true, returns those just claimed
}

// ---- usage-api: entities ----
// UsageCounter : NamedEntity<UsageCounterName>  — component: name
// UsageTally   : Entity<UsageTallyId>           — id, counter, @Nullable naturalist, period, count ; withCount
// UsageAlert   : Entity<UsageAlertId>           — id, counter, scope, kind, period, message, at, emailed, acknowledged ; withEmailed, withAcknowledged

// ---- usage-api: repository namespace (package-private class, protected nested interfaces) ----
// UsageRepository.CounterRepository extends EntityRepository<UsageCounterName, UsageCounter>
// UsageRepository.TallyRepository   extends EntityRepository<UsageTallyId, UsageTally> {
//     Optional<UsageTally> findBusinessKey(UsageCounterName counter, @Nullable NaturalistName naturalist, String period);
// }
// UsageRepository.AlertRepository   extends EntityRepository<UsageAlertId, UsageAlert> {
//     Optional<UsageAlert> findDedupKey(UsageCounterName counter, AlertScope scope, AlertKind kind, String period);
//     List<UsageAlert> getUnacknowledged();
//     List<UsageAlert> getUnsent();
// }
```

Period slugs: `daily-<yyyy-mm-dd>`, `monthly-<yyyy-mm>`, `rate-<yyyy-mm-dd-hh-mm>`. The single seeded `UsageCounter` name is `identification`.

---

# MILESTONE A — the `usage` domain module

Delivers the module end-to-end: entities, persistence (mock + rdms), and the budget service with the synchronized reserve, all unit- and contract-tested. Nothing wired into insects yet.

## Task A1: Scaffold `domains/usage/` + api value types

**Files:**
- Create: `domains/usage/pom.xml`; `domains/usage/usage-api/pom.xml`, `usage-core/pom.xml`, `usage-repository-test/pom.xml`, `usage-repository-rdms/pom.xml`
- Modify: `domains/pom.xml` (`<modules>`), root `pom.xml` (`<dependencyManagement>` — one `${project.version}` entry each for `usage-api`, `usage-core`, `usage-repository-test`, `usage-repository-rdms`)
- Create in `usage-api/src/main/java/com/naturalist/usage/`: `LimitKind`, `AlertScope`, `AlertKind`, `UsageLimits`, `UsageSnapshot`, `BudgetExceededException`, `UsagePolicy`
- Test: `usage-api/src/test/java/com/naturalist/usage/UsagePolicyTest.java`

**Interfaces produced:** the value types above; `UsagePolicy.warningThreshold(int limit, int warningPercent)`, `UsagePolicy.alertMessage(AlertScope, AlertKind, int used, int limit)`, `UsagePolicy.scopeOf(LimitKind)`.

- [ ] **Step 1: Create the four sub-module poms + parent.** `domains/usage/pom.xml` mirrors `domains/insects/pom.xml` (parent `domains`, `packaging=pom`, artifactId `usage`, `<modules>` listing the four sub-modules). Each sub-module pom mirrors the matching insects one: `usage-api/pom.xml` like `insects-api/pom.xml` (deps: framework, identifiers, field-notes; test: framework-test); `usage-core` like `insects-core` but dep `usage-api` only (+ framework); `usage-repository-test` like `insects-repository-test` (deps: usage-api, framework-test, identifiers-test); `usage-repository-rdms` like `insects-repository-rdms` (single dep: usage-repository-test).

- [ ] **Step 2: Register modules.** Add `<module>usage</module>` to `domains/pom.xml`; add the four `${project.version}` entries to the root `pom.xml` KERNELS-style DOMAIN block (a new `<!-- USAGE -->` group, alphabetical among domains).

- [ ] **Step 3: Write the value types.** In `usage-api`, the enums and records exactly as the Interfaces block. `BudgetExceededException` (mirror the framework exception style — `RuntimeException`, accessors without `get`):

```java
package com.naturalist.usage;
import java.time.Instant;
public final class BudgetExceededException extends RuntimeException {
    private final LimitKind limitKind; private final Instant resetAt;
    public BudgetExceededException(LimitKind limitKind, Instant resetAt) {
        super("Identification budget exceeded: %s (resets at %s)".formatted(limitKind, resetAt));
        this.limitKind = limitKind; this.resetAt = resetAt;
    }
    public LimitKind limitKind() { return limitKind; }
    public Instant resetAt() { return resetAt; }
}
```

- [ ] **Step 4: Write the failing `UsagePolicyTest`:**

```java
class UsagePolicyTest {
    @org.junit.jupiter.api.Test void warning_threshold_rounds_up() {
        org.assertj.core.api.Assertions.assertThat(UsagePolicy.warningThreshold(650, 80)).isEqualTo(520);
        org.assertj.core.api.Assertions.assertThat(UsagePolicy.warningThreshold(50, 80)).isEqualTo(40);
        org.assertj.core.api.Assertions.assertThat(UsagePolicy.warningThreshold(3, 80)).isEqualTo(3);
    }
    @org.junit.jupiter.api.Test void message_names_scope_and_numbers() {
        var m = UsagePolicy.alertMessage(AlertScope.MONTHLY, AlertKind.HARD_STOP, 520, 650);
        org.assertj.core.api.Assertions.assertThat(m).contains("monthly").contains("520").contains("650");
    }
    @org.junit.jupiter.api.Test void scopeOf_maps_and_rejects() {
        org.assertj.core.api.Assertions.assertThat(UsagePolicy.scopeOf(LimitKind.DAILY)).isEqualTo(AlertScope.DAILY);
        org.assertj.core.api.Assertions.assertThat(UsagePolicy.scopeOf(LimitKind.MONTHLY)).isEqualTo(AlertScope.MONTHLY);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> UsagePolicy.scopeOf(LimitKind.PER_USER));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> UsagePolicy.scopeOf(LimitKind.RATE));
    }
}
```

- [ ] **Step 5: Run — expect FAIL** (`UsagePolicy` missing). `mvn -q -pl domains/usage/usage-api -am test`.

- [ ] **Step 6: Implement `UsagePolicy`.** `warningThreshold` = `(int) Math.ceil(limit * (double) warningPercent / 100)`; `alertMessage` returns e.g. `"%s identification budget %s: %d/%d used".formatted(scope.name().toLowerCase(), kind, used, limit)`; `scopeOf` maps DAILY/MONTHLY, throws `IllegalArgumentException` for PER_USER/RATE.

- [ ] **Step 7: Clean install + run** — `mvn -q install -DskipTests` from worktree root (new modules), then `mvn -q -pl domains/usage/usage-api test` → PASS.

- [ ] **Step 8: Commit**

```bash
git add domains/usage domains/pom.xml pom.xml
git commit -m "feat(usage): scaffold usage domain module + api value types"
```

## Task A2: Identifiers + service ports

**Files:**
- Create in `domains/identifiers/src/main/java/com/naturalist/usage/`: `UsageCounterName.java` (`EntityName`), `UsageTallyId.java` (`EntityId`), `UsageAlertId.java` (`EntityId`)
- Create in `usage-api/.../usage/`: `IdentificationBudget.java`, `NoOpIdentificationBudget.java`, `UsageMonitor.java`
- Test: `usage-api/.../NoOpIdentificationBudgetTest.java`

**Interfaces produced:** the three identifier types; `IdentificationBudget` (+ `noOp()`), `UsageMonitor` per the Interfaces block. (Note `UsageMonitor` references `UsageAlert`/`UsageAlertId` which land in A2/A3 — declare `UsageMonitor` after `UsageAlertId` exists; it is fine for it to reference `UsageAlert` created in A3 within the same module — order A3 before finalizing `UsageMonitor`'s compile, or stub `UsageAlert` first. Simplest: create the three identifiers here, `IdentificationBudget`+NoOp here, and move `UsageMonitor` to A3 where `UsageAlert` exists.)

- [ ] **Step 1: Write the identifiers.** Mirror `InsectImageId` for the ids and a slug `EntityName` for the name:

```java
// UsageCounterName.java  (mirror an existing EntityName subclass; maxLength 64)
package com.naturalist.usage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
public final class UsageCounterName extends EntityName {
    private UsageCounterName(String value) { super(value); }
    @JsonCreator public static UsageCounterName of(String value) { return new UsageCounterName(value); }
    @Override protected int maxLength() { return 64; }
}
// UsageTallyId.java / UsageAlertId.java  (mirror InsectImageId exactly)
package com.naturalist.usage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;
import java.util.UUID;
public final class UsageTallyId extends EntityId {
    private UsageTallyId(UUID value) { super(value); }
    @JsonCreator public static UsageTallyId of(UUID value) { return new UsageTallyId(value); }
    public static UsageTallyId create() { return new UsageTallyId(EntityId.newUUID()); }
}
```

(Repeat `UsageAlertId` identically.)

- [ ] **Step 2: Write `IdentificationBudget` + `NoOpIdentificationBudget`** (mirror the framework `Resilience`/`NoOpResilience` port+singleton shape; `noOp().reserve(...)` does nothing).

- [ ] **Step 3: Write the failing `NoOpIdentificationBudgetTest`** — `noOp().reserve(NaturalistName.of("pat"))` does not throw; `new BudgetExceededException(MONTHLY, instant)` accessors return what was passed.

- [ ] **Step 4: Run FAIL → implement → PASS.** `mvn -q -pl domains/usage/usage-api -am test`. (`identifiers` is a separate module — clean install first.)

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/usage domains/usage/usage-api
git commit -m "feat(usage): identifiers + IdentificationBudget port"
```

## Task A3: Entities + `UsageMonitor`

**Files:**
- Create in `usage-api/.../usage/`: `UsageCounter.java`, `UsageTally.java`, `UsageAlert.java`, and `UsageMonitor.java`
- Test: `usage-api/.../UsageTallyTest.java`, `UsageAlertTest.java`, `UsageCounterTest.java` (Observable-invariants tests, per the domain api test convention)

**Interfaces produced:** the three entity records + `UsageMonitor`.

- [ ] **Step 1: Write the entities.** Records implementing the framework markers with `invariants()` and `with*` methods. `UsageCounter`:

```java
package com.naturalist.usage;
import com.naturalist.ddd.Constraints;
import com.naturalist.ddd.NamedEntity;
import java.util.function.Consumer;
public record UsageCounter(UsageCounterName name) implements NamedEntity<UsageCounterName> {
    @Override public Consumer<? extends Constraints> invariants() {
        return i -> i.identifier(name, "name");
    }
}
```

`UsageTally` (`Entity<UsageTallyId>`; nullable naturalist via `whenNotNull`; `withCount`):

```java
public record UsageTally(UsageTallyId id, UsageCounterName counter,
                         @org.jspecify.annotations.Nullable com.naturalist.naturalist.NaturalistName naturalist,
                         String period, int count) implements com.naturalist.ddd.Entity<UsageTallyId> {
    public UsageTally withCount(int newCount) { return new UsageTally(id, counter, naturalist, period, newCount); }
    @Override public java.util.function.Consumer<? extends com.naturalist.ddd.Constraints> invariants() {
        return i -> i.entityId(id, "id").identifier(counter, "counter").notBlank(period, "period")
                     .whenNotNull(naturalist, c -> c.identifier(naturalist, "naturalist"));
    }
}
```

`UsageAlert` (`Entity<UsageAlertId>`; `withEmailed`, `withAcknowledged`; invariants over id/counter/scope/kind/period/message/at). Match the exact `Constraints` method names used by `OrganismImage`/other entities (read one for the available methods: `entityId`, `identifier`, `notNull`, `notBlank`, `whenNotNull`).

- [ ] **Step 2: Write `UsageMonitor`** exactly as the Interfaces block (now that `UsageAlert`/`UsageAlertId` exist).

- [ ] **Step 3: Write the failing invariants tests** — follow the api-module Observable test pattern (one `Observer` static field, `MethodObserver` per method; valid case = all fields populated via `RandomValue`, assert `invalidInvariants().isEmpty()`; invalid case = nulls, assert the exact invariant-name set with `containsExactlyInAnyOrder`). Read `domains/insects/insects-api/src/test/java/.../OrganismImage`-style or `InsectSpeciesTest` for the exact shape before writing.

- [ ] **Step 4: Run FAIL → implement → PASS.** `mvn -q -pl domains/usage/usage-api test`.

- [ ] **Step 5: Commit**

```bash
git add domains/usage/usage-api
git commit -m "feat(usage): UsageCounter, UsageTally, UsageAlert entities + UsageMonitor"
```

## Task A4: TestEntitySources + JSON seeds

**Files:**
- Create in `usage-repository-test/src/main/java/com/naturalist/usage/`: `UsageCounterTestEntitySource.java`, `UsageTallyTestEntitySource.java`, `UsageAlertTestEntitySource.java`
- Create in `domains/identifiers-test/.../usage/`: `TestUsageIdentifiers.java`
- Create resources: `usage-repository-test/src/main/resources/usage/usage-counters.json` (seed the single `identification` row), `usage-tallies.json` (`[]`), `usage-alerts.json` (`[]`)
- Test: `usage-repository-test/src/test/java/.../UsageCounterTestEntitySourceTest.java` (+ tally, alert)

**Interfaces produced:** the three `TestEntitySource` subclasses; `TestUsageIdentifiers` constants (at least two `UsageCounterName`, sample `UsageTallyId`/`UsageAlertId`, a `NotFound` inner class).

- [ ] **Step 1: Write `TestUsageIdentifiers`** — mirror `TestInsectsIdentifiers` structure: `UsageCounters.Identification = UsageCounterName.of("identification")`, a second `UsageCounters.InsectIdentification` (fictitious but valid) for set tests, a `NotFound` inner class with a fictitious `UsageCounterName` (`"unobtainium-counter"`) and fictitious ids.

- [ ] **Step 2: Write the three `TestEntitySource`s** — mirror `InsectImageTestEntitySource` (for the `Entity` sources: tally, alert) and a `NamedEntity` source (for counter). Declare:
  - `UsageCounterTestEntitySource` (key `UsageCounterName`): `uniqueConstraints()` default; loads `usage/usage-counters.json`.
  - `UsageTallyTestEntitySource` (key `UsageTallyId`): a secondary `UniqueConstraint` over the business key `(counter, naturalist, period)` — declare it per the `UniqueConstraint` API (read `InsectImageTestEntitySource` and a source that declares `uniqueConstraints()`); `foreignKeyConstraints()` = one `ForeignKeyConstraint.of(counter-accessor, UsageCounterTestEntitySource.class)`; the `naturalist` FK is cross-domain → none. Loads `usage/usage-tallies.json` (`[]`).
  - `UsageAlertTestEntitySource` (key `UsageAlertId`): secondary `UniqueConstraint` over `(counter, scope, kind, period)`; FK on `counter`; loads `usage/usage-alerts.json` (`[]`).
  Each declares the `(NaturalistDatabase)` constructor. Register the `NaturalistName`/`RankName`-style reconstructors only if a component needs one — `UsageCounterName`/`AlertScope`/`AlertKind` are `@JsonValue`/enum, no custom reconstructor needed; `naturalist` is a `NaturalistName` (`@JsonValue` string) — confirm it round-trips as a bare string.

- [ ] **Step 3: Seed JSON.** `usage-counters.json`: `[ { "name": "identification" } ]`. Tally/alert JSON: `[]`.

- [ ] **Step 4: Write the failing `*TestEntitySourceTest`s** — mirror `InsectImageTestEntitySourceTest` (a `@RegisterExtension NaturalistTestExtension`; assert the seeded counter loads, and that a round-trip insert of a `newEntity()` is retrievable). RED (sources missing) → implement → GREEN.

- [ ] **Step 5: Run.** `mvn -q install -DskipTests` (identifiers-test changed), then `mvn -q -pl domains/usage/usage-repository-test test` → PASS.

- [ ] **Step 6: Commit**

```bash
git add domains/usage/usage-repository-test domains/identifiers-test/src/main/java/com/naturalist/usage
git commit -m "feat(usage): TestEntitySources + seeds + TestUsageIdentifiers"
```

## Task A5: Repository namespace + mocks + behavioral contracts

**Files:**
- Create in `usage-api/.../usage/`: `UsageRepository.java` (package-private namespace class with the three `protected interface`s)
- Create in `usage-repository-test/.../usage/`: `UsageCounterRepositoryMock.java`, `UsageTallyRepositoryMock.java`, `UsageAlertRepositoryMock.java`; the contract interfaces `UsageTallyRepositoryTest.java`, `UsageAlertRepositoryTest.java`, `UsageCounterRepositoryTest.java`; and mock test classes
**Interfaces produced:** `UsageRepository.CounterRepository/TallyRepository/AlertRepository` (with the custom finders in the Interfaces block).

- [ ] **Step 1: Write `UsageRepository`** — mirror `InsectRepository` (package-private `class`; nested `protected interface`s extending `EntityRepository<KEY,ENTITY>`), adding the custom finders: `TallyRepository.findBusinessKey(...)`, `AlertRepository.findDedupKey(...)`, `getUnacknowledged()`, `getUnsent()`.

- [ ] **Step 2: Write the mocks** — mirror `InsectImageRepositoryMock` (extend `AbstractTestEntityRepository<KEY, ENTITY, SOURCE>`, `(NaturalistDatabase)` ctor). Implement each custom finder over `testEntitySource().entityStream()`, validating args first, e.g.:

```java
@Override public java.util.Optional<UsageTally> findBusinessKey(
        UsageCounterName counter, com.naturalist.naturalist.NaturalistName naturalist, String period) {
    observer().arguments("findBusinessKey", i -> i.identifier(counter, "counter").notBlank(period, "period")).throwWhenInvalid();
    return testEntitySource().entityStream()
            .filter(t -> t.counter().equals(counter)
                      && java.util.Objects.equals(t.naturalist(), naturalist)
                      && t.period().equals(period))
            .findFirst();
}
```

`AlertRepository.getUnacknowledged()` filters `!acknowledged`, newest-first (sort by `at` desc); `getUnsent()` filters `!emailed`.

- [ ] **Step 3: Write the behavioral contract tests** — mirror `InsectImageRepositoryTest`: extend `EntityRepositoryTest<KEY,ENTITY>`, supply hooks (`repository()`, `source()`, `notFoundName()`/id, `knownEntityNames()` from `TestUsageIdentifiers`, `newEntity()`/`ghostEntity()`/`modifiedEntity()`), plus `@Test default` cases for each custom finder (null-rejection, empty, expected-result). Add the mock test classes that implement the contract interface against the mock.

- [ ] **Step 4: Run.** `mvn -q -pl domains/usage/usage-repository-test test` → the inherited CRUD contract + custom-finder tests PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/usage/usage-api domains/usage/usage-repository-test
git commit -m "feat(usage): repository namespace, mocks, and behavioral contracts"
```

## Task A6: repository-rdms (production-wired placeholder)

**Files:**
- Create in `usage-repository-rdms/.../usage/`: `UsageCounterRepositoryRdms.java`, `UsageTallyRepositoryRdms.java`, `UsageAlertRepositoryRdms.java`

- [ ] **Step 1: Write the three rdms classes** — byte-for-byte the insects shape: `@DomainService class UsageTallyRepositoryRdms extends UsageTallyRepositoryMock { UsageTallyRepositoryRdms(NaturalistDatabase db){ super(db);} }` (repeat for counter, alert). Only the `@DomainService` marker distinguishes them; storage stays the shared in-memory `NaturalistDatabase`.

- [ ] **Step 2: Run.** `mvn -q -pl domains/usage/usage-repository-rdms -am install -DskipTests` builds; `mvn -q -pl domains/usage/usage-repository-rdms test` (if any) or the reactor build proves it compiles.

- [ ] **Step 3: Commit**

```bash
git add domains/usage/usage-repository-rdms
git commit -m "feat(usage): repository-rdms @DomainService placeholders"
```

## Task A7: `usage-core` budget service (reserve + monitor)

**Files:**
- Create in `usage-core/src/main/java/com/naturalist/usage/`: `UsageBudgetService.java` (implements `IdentificationBudget` + `UsageMonitor`), and a package-private `UsageCoreTestContext` in `usage-core/src/test/java/...` wiring the mocks (mirror `InsectsTestContextInternal`)
- Test: `usage-core/.../UsageBudgetServiceTest.java`

**Interfaces produced:** `UsageBudgetService(UsageLimits, java.time.Clock, CounterRepository, TallyRepository, AlertRepository)` — the `@DomainService` production bean (constructor-injected). Implements the reserve + monitor.

- [ ] **Step 1: Write the failing tests** (fixed `Clock`; small limits; mocks wired via the test context). Cover exactly:

```java
class UsageBudgetServiceTest {
    private final java.time.Clock clock =
        java.time.Clock.fixed(java.time.Instant.parse("2026-08-25T10:00:00Z"), java.time.ZoneOffset.UTC);
    // build service with mocks from UsageCoreTestContext + given UsageLimits + clock

    @org.junit.jupiter.api.Test void per_user_daily_quota_blocks_after_limit() { /* limits perUser=2; reserve(pat)x2 ok; 3rd throws PER_USER */ }
    @org.junit.jupiter.api.Test void global_daily_cap_blocks_across_users()   { /* daily=2; a,b ok; c throws DAILY */ }
    @org.junit.jupiter.api.Test void rejected_reserve_does_not_consume_other_counters() { /* perUser=1; pat ok; pat throws; snapshot().dailyUsed()==1 */ }
    @org.junit.jupiter.api.Test void warning_and_hard_stop_alerts_recorded_once() { /* monthly=5 warn@4; drive to cap; assert one WARNING + one HARD_STOP UsageAlert, deduped */ }
    @org.junit.jupiter.api.Test void concurrent_reserves_do_not_overshoot() { /* daily=100, others non-binding; 500 reserves/16 threads over distinct users; exactly 100 succeed */ }
}
```

(Model these on the proven kernel `InMemoryIdentificationBudgetTest` scenarios from the superseded revision — same assertions, now over the repository-backed service. The concurrency test's limits must leave rate/monthly/per-user non-binding: `UsageLimits(99, 9999, 100, 9999, 80)`.)

- [ ] **Step 2: Run — expect FAIL.** `mvn -q -pl domains/usage/usage-core test`.

- [ ] **Step 3: Implement `UsageBudgetService`.** `reserve(naturalist)` is `synchronized`. Algorithm:
  1. `counter = UsageCounterName.of("identification")`; from `clock`: `day = LocalDate.now(clock)`, `month = YearMonth.now(clock)`, `minute = LocalDateTime.now(clock).truncatedTo(MINUTES)`. Period slugs: `"daily-"+day`, `"monthly-"+month`, `"rate-"+minute` formatted with hyphens (kebab).
  2. Resolve the four current counts via `tallyRepository.findBusinessKey(counter, subject, period)` → `.map(UsageTally::count).orElse(0)`: monthly (naturalist null, monthly slug), daily (null, daily slug), rate (null, rate slug), user (naturalist, daily slug).
  3. Check in order MONTHLY → DAILY → RATE → PER_USER: if `count >= limit`, and the kind is MONTHLY/DAILY, `recordAlert(HARD_STOP, scope, period)` (dedup); throw `new BudgetExceededException(kind, resetAt)` (`resetAt` = start of next month/day/minute UTC). Increment NOTHING on rejection.
  4. All pass → upsert each tally to `count+1` (`findBusinessKey(...)` present → `tallyRepository.save(existing.withCount(n+1))`, absent → `tallyRepository.insert(new UsageTally(UsageTallyId.create(), counter, subject, period, 1))`). For MONTHLY and DAILY, if the new value equals `UsagePolicy.warningThreshold(limit, warningPercent)`, `recordAlert(WARNING, scope, period)` (dedup).
  `recordAlert(kind, scope, period)`: if `alertRepository.findDedupKey(counter, scope, kind, period).isEmpty()`, `alertRepository.insert(new UsageAlert(UsageAlertId.create(), counter, scope, kind, period, UsagePolicy.alertMessage(scope, kind, used, limit), clock.instant(), false, false))`.
  `UsageMonitor`: `snapshot()` reads the current daily/monthly/rate tallies + per-user tallies for today into `UsageSnapshot`; `activeAlerts()` = `alertRepository.getUnacknowledged()`; `acknowledge(id)` = `alertRepository.save(getByName(id).withAcknowledged(true))`; `claimUnsentAlerts()` = for each `getUnsent()`, `save(withEmailed(true))`, return them. Whole service methods `synchronized`.

- [ ] **Step 4: Run — PASS** (all five). `mvn -q -pl domains/usage/usage-core test`.

- [ ] **Step 5: Commit**

```bash
git add domains/usage/usage-core
git commit -m "feat(usage): budget service with synchronized reserve + monitor"
```

- [ ] **Milestone A gate:** `mvn -q install -DskipTests && mvn -q -pl domains/usage/... test`, then the completeness gate. **Pause for human review.**

---

# MILESTONE B — enforcement + app wiring

## Task B1: Enforce in `InsectIdentificationCommand`

**Files:** Modify `domains/insects/insects-core/pom.xml` (add `usage-api` dep), `InsectIdentificationCommand.java`, `InsectIdentificationCommandTest.java`.

- [ ] **Step 1** Add `usage-api` dep (no version) to `insects-core/pom.xml`.
- [ ] **Step 2** Write the failing test `identify_reserves_budget_before_calling_vision` — a `com.naturalist.usage.IdentificationBudget` stub that throws `BudgetExceededException`, and a tracking `VisionService`; assert vision is never called and `BudgetExceededException` propagates. (Same test as the superseded revision, import path `com.naturalist.usage`.)
- [ ] **Step 3** Add `IdentificationBudget budget` as the **first** constructor parameter of `InsectIdentificationCommand` (field `private final`), and `budget.reserve(naturalist);` as the first statement of `identify(...)`.
- [ ] **Step 4** Fix the two existing `new InsectIdentificationCommand(...)` sites in the test to prepend `com.naturalist.usage.IdentificationBudget.noOp(),`.
- [ ] **Step 5** Run `mvn -q -pl domains/insects/insects-core test` RED→GREEN. (Note: `insects-console` won't compile until B2 — scoped build only.)
- [ ] **Step 6** Commit `feat(insects): reserve identification budget before vision`.

## Task B2: App wiring

**Files:** Create `apps/management-console/.../console/usage/UsageProperties.java`, `UsageConfiguration.java`; modify `apps/management-console/pom.xml` (add deps `usage-core`, `usage-repository-rdms`), `application.yml`; modify `domains/insects/insects-console/pom.xml` (add `usage-api`) and `InsectsController.java`.

- [ ] **Step 1** App pom: add `usage-core` and `usage-repository-rdms` deps (no version). insects-console pom: add `usage-api`.
- [ ] **Step 2** `UsageProperties` = `@ConfigurationProperties("naturalist.usage")` (`perUserDaily, globalRatePerMinute, globalDaily, globalMonthly, warningPercent, alertEmail`).
- [ ] **Step 3** `UsageConfiguration` = `@Configuration @EnableConfigurationProperties(UsageProperties.class)` providing `@Bean UsageLimits` (from props) and `@Bean java.time.Clock` (`Clock.systemUTC()`). The `UsageBudgetService` (`@DomainService`) and the three `*RepositoryRdms` (`@DomainService`) are discovered automatically; Spring injects `UsageLimits`, `Clock`, and the three repositories into the service; the service satisfies both the `IdentificationBudget` and `UsageMonitor` injection points. (If a same-type ambiguity arises at startup, resolve it minimally — do not expose duplicate assignable beans; verify via the app context-load tests, do not guess.)
- [ ] **Step 4** `application.yml`: add the `naturalist.usage.*` block (10/3/50/650/80, alert-email patway99@gmail.com).
- [ ] **Step 5** `InsectsController`: inject `com.naturalist.usage.IdentificationBudget budget` (last ctor param), pass as first arg to `new InsectIdentificationCommand(...)`.
- [ ] **Step 6** Verify (subagent-safe): `mvn -q install -DskipTests` (whole reactor — proves insects-console + app compile), then `mvn -q -pl apps/management-console test` (the `@SpringBootTest` context-load tests prove the beans wire). Report commands + results. Do NOT run a foreground `spring-boot:run`.
- [ ] **Step 7** Commit `feat(console): wire usage domain into insects identify`.

## Task B3: Over-budget UX

**Files:** Modify `InsectsController.java`, `insects/identify.jte`; test in `insects-console/src/test`.

- [ ] **Step 1** Failing test: when the injected budget throws `BudgetExceededException`, the identify POST redirects to a URL containing `limit=` (mirror the harness the superseded A6 built via `InsectsTestContext`, constructing the controller directly). If none exists, build the focused test the same way.
- [ ] **Step 2** POST handler: wrap the `identificationCommand.identify(...)` call in `try { ... } catch (com.naturalist.usage.BudgetExceededException over) { return "redirect:/insects/identify?limit=" + over.limitKind(); }`.
- [ ] **Step 3** GET `identifyForm`: read `@RequestParam(name="limit", required=false) String limit`, `model.addAttribute("limit", limit)` when present (mirror the existing `identified` param). `identify.jte`: `@param String limit = null`; render a `<p role="alert">` when non-null — PER_USER = your daily limit (resets tomorrow); RATE/DAILY/MONTHLY = temporarily unavailable.
- [ ] **Step 4** Run `mvn -q -pl domains/insects/insects-console test` RED→GREEN.
- [ ] **Step 5** Commit `feat(insects-console): friendly over-budget message`.

- [ ] **Milestone B gate:** full `mvn -q verify` + completeness gate. **Pause for human review.**

---

# MILESTONE C — admin monitoring UI

## Task C1: `/admin/usage` dashboard
**Files:** Create `apps/management-console/.../console/admin/AdminUsageController.java`, `src/main/jte/admin/usage.jte`; modify `jte/admin/nav.jte`; test `AdminUsageControllerWebMvcTest`.
- [ ] **Step 1** Failing WebMvc test (mirror `AdminResilienceControllerWebMvcTest`): mock `UsageMonitor`, `@WithMockUser(roles="ADMIN")`, GET `/admin/usage` → 200, body contains the monthly number.
- [ ] **Step 2** Run FAIL.
- [ ] **Step 3** Implement the controller (mirror `AdminResilienceController`: package-private ctor injecting `UsageMonitor`, populate model from `snapshot()`+`activeAlerts()`, return `"admin/usage"`); compute per-gauge state (green/amber at warning-percent/red at 100%) in the controller as a small `record Gauge(String label, int used, int limit, String state)`. `usage.jte` mirrors `resilience.jte`; add the `usage` nav entry to `nav.jte`.
- [ ] **Step 4** Run PASS.
- [ ] **Step 5** Commit `feat(console): /admin/usage dashboard`.

## Task C2: `/admin/usage.json`
**Files:** Modify `AdminUsageController.java`; extend the WebMvc test.
- [ ] **Step 1** Failing test: GET `/admin/usage.json` as ADMIN → 200, JSON, `$.usage.monthlyLimit` == configured. **Step 2** FAIL. **Step 3** Add `@GetMapping(value="/admin/usage.json", produces="application/json") @ResponseBody UsageReport report()` (`record UsageReport(UsageSnapshot usage, List<UsageAlert> alerts)`). **Step 4** PASS. **Step 5** Commit `feat(console): /admin/usage.json endpoint`.

## Task C3: Acknowledge + banner + security test
**Files:** Modify `AdminUsageController.java`, `auth/NaturalistHeaderInterceptor.java`, `jte/layout/page.jte`; test `AdminUsageSecurityWebMvcTest`.
- [ ] **Step 1** Failing security test (mirror `AdminSecurityWebMvcTest`): anonymous `/admin/usage` → redirect to login; `roles="NATURALIST"` → 403. **Step 2** Run (may already pass via `/admin/**` — keep as regression). **Step 3** Add `@PostMapping("/admin/usage/alerts/{id}/ack")` (`acknowledge(UsageAlertId.of(...))` → redirect); in `NaturalistHeaderInterceptor.preHandle` inject `UsageMonitor` and set request attr `usageAlertsPending = !activeAlerts().isEmpty()`; `page.jte` renders a persistent banner linking `/admin/usage` when true. **Step 4** Add the ack case to the WebMvc test. **Step 5** PASS. **Step 6** Commit `feat(console): acknowledge alerts + usage banner`.

- [ ] **Milestone C gate:** `mvn -q verify` + completeness gate. **Pause for human review.**

---

# MILESTONE D — alert delivery (email + scheduling)

## Task D1: `AlertEmailer` (guarded JavaMailSender)
**Files:** Modify `apps/management-console/pom.xml` (add `spring-boot-starter-mail`); create `console/usage/AlertEmailer.java`; modify `application.yml` (commented mail block); test `AlertEmailerTest`.
- [ ] **Step 1** Add the starter (no version). **Step 2** Failing test: with a mock `JavaMailSender` provided, `send(alert)` sends a `SimpleMailMessage` to `alertEmail` with the alert message; with an empty `ObjectProvider<JavaMailSender>`, `send` no-ops (no throw). **Step 3** FAIL. **Step 4** Implement `AlertEmailer(ObjectProvider<JavaMailSender>, UsageProperties)`; resolve via `getIfAvailable()`, null → return; else build+send inside `try/catch(Exception)` (best-effort, no logging). Add commented `spring.mail.*` env block to `application.yml`. **Step 5** PASS. **Step 6** Commit `feat(console): best-effort email alerts`.

## Task D2: Scheduled dispatcher
**Files:** Create `console/usage/AlertDispatchJob.java`; add `@EnableScheduling` to `UsageConfiguration`; test `AlertDispatchJobTest`.
- [ ] **Step 1** Failing test: a `UsageMonitor` stub returning one unsent alert on first `claimUnsentAlerts()` then empty; assert `AlertEmailer.send` called exactly once across two `dispatch()` calls. **Step 2** FAIL. **Step 3** Implement `@Scheduled(fixedDelayString="${naturalist.usage.dispatch-interval-ms:60000}") void dispatch()` = `for (var a : monitor.claimUnsentAlerts()) emailer.send(a);`; add `@EnableScheduling`. **Step 4** PASS. **Step 5** Commit `feat(console): scheduled alert dispatch`.

- [ ] **Milestone D gate:** `mvn -q verify` + completeness gate; confirm the app boots with no `spring.mail.*` set (email no-ops). **Pause for human review.**

---

## Self-Review (plan author)

- **Spec coverage (Design Revision 2):** module structure → A1–A7; `UsageCounter`/`UsageTally`/`UsageAlert` entities → A3; identifiers → A2; TestEntitySources/seeds → A4; repositories + mock + contract → A5; rdms placeholder (production-wired) → A6; synchronized reserve + monitor + no-overshoot → A7; enforce-in-core → B1; app wiring + limits-from-yml → B2; over-budget UX → B3; admin dashboard/json/ack/banner → C; email + scheduled dispatch at 80%/100% daily+monthly → A7 (alert recording) + D; generic `identification` counter → A4 seed + A7; limits movable to `UsageCounter` later → noted in A1/A3 (no task needed now). Authority text-gen remains out of scope (only insects calls `reserve`).
- **Type consistency:** `IdentificationBudget.reserve(NaturalistName)` first-arg on the 8-param command (B1) matches B2 wiring; `UsageMonitor` methods (`acknowledge(UsageAlertId)`, `claimUnsentAlerts()`) used identically in A7/C/D; period slug format (`daily-`/`monthly-`/`rate-`) consistent A7↔A4.
- **Placeholder scan:** logic-bearing code (UsagePolicy, entities, reserve, dispatcher) is given in full; domain boilerplate (poms, TestEntitySources, mocks, contract tests) is specified by exact reference file + exact field/constraint list, which is concrete, not a placeholder. The one intentionally-deferred item (real DB) is out of scope by decision.
- **Durability caveat is explicit** (Global Constraints + Architecture): single-JVM, resets on restart, until the project wires a real DB app-wide.
