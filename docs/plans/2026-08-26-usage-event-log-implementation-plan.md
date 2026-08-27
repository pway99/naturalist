# Usage Event-Log Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `usage` module's rolling-tally counter with an append-only `UsageEvent` log whose counts are derived on demand, move burst control to a new Resilience4J rate-limiter primitive, and make the monthly budget an operator-editable `FixedSince` window — while keeping the public bucket's behavior, the alert/dispatch/dashboard surface, and the single-JVM `synchronized` reserve.

**Architecture:** `UsageEvent(id, counterName, naturalist, instant)` is the sole source of truth; every usage figure is `count(events matching a window + scope)`, computed in the `usage-core` read adapter over a **single batched windowed fetch** (the repository stays a pure entity cache — no `count(*)` pushdown). `UsageCounter` becomes the editable budget **rule** (`scope`, `windowKind`, `since`, `limit`, `active`); multiple rules share the `identification` counter name. Buckets: the **public** bucket (built) is gated by the active rules; the **entitled** bucket (credits) is an inert `isEntitled → false` seam. Burst is a new global `RateLimiter` on the Resilience facade, wrapping the vision spend in `AnthropicVisionService`.

**Tech Stack:** Java 21 records, `com.naturalist.ddd` framework markers, `com.naturalist.data.EntityRepository`, `kernels/framework-test` (`NaturalistDatabase`, `NaturalistTestExtension`, `EntityRepositoryTest`, `TestEntitySource`), Observer validation, Jackson 2.19 record deserialization, Resilience4j via the `Resilience` facade, Spring Boot (management-console), JTE, JUnit 5 + AssertJ.

## Global Constraints

- **Typed identifiers only** — `UsageEventId`/`UsageCounterId`/`UsageAlertId` (UUIDv7 via `EntityId.newUUID()`, never `UUID.randomUUID()`); `UsageCounterName` (`EntityName`, kebab, `maxLength()==64`). `NaturalistName` is `com.naturalist.naturalist.NaturalistName`.
- **Framework markers** — `UsageEvent`/`UsageCounter`/`UsageAlert` are `Entity<ID>` records; `ReserveState`/`UsageSnapshot` are `ReadModel`/plain records. Every marker type declares `invariants()`. No `@JsonCreator` on entity records; JSON field names match components exactly. Enums serialize by constant name.
- **api visibility** — repository interfaces are package-private (nested `protected interface` in the `UsageRepository` namespace class, ADR-020). `usage-api` depends only on framework, identifiers, field-notes.
- **Query hygiene** — reads that fan out per element are forbidden; the N+1 no-fan-out gate is armed. Every repository select in this plan is called **once** (never inside a loop/stream-map). Count in memory, not by per-rule repository calls.
- **`@DomainService`** (`com.naturalist.infrastructure.DomainService`) goes on `usage-core` impls and `usage-repository-rdms` adapters, never on api interfaces.
- **Repo workflow (this repo):** subagents **stage only** (`git add`); the controller/human commits after review. "Commit" steps below show the intended message — stage the listed files and hand back for review rather than committing autonomously.
- **Completeness gate (run before declaring done):** `mvn verify`, then `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`. **Phase 5 touches `kernels/framework` → do a clean `mvn install` (not `-pl -am` incremental) to avoid stale-class `NoSuchMethodError`.**
- **Reference domain:** insects. Copy its idioms, not chemistry/plants.

## Plan-level concretizations (faithful to the design doc)

1. **Window flattened onto the entity.** The design's `UsageWindow` value object (`FixedCalendarDay` / `FixedSince`) is persisted as two record components — `WindowKind windowKind` (enum `CALENDAR_DAY | SINCE`) + `@Nullable Instant since` — to avoid polymorphic Jackson in the JSON seeds. Window math (start / reset instants) lives in a `usage-core` helper `UsageWindows`, exactly where `UsagePeriods` lived.
2. **`UsageLimits` collapses to `warningPercent`.** All numeric limits now live on `UsageCounter` rows, so the injected limits value shrinks to a single `int warningPercent`. `UsageLimits` is deleted; query/command take an `int warningPercent` ctor arg.
3. **Entitlement seam is a boolean.** No `UsageEntitlement` type is created. The command depends on `EntitlementLookup { boolean isEntitled(NaturalistName) }` with a `none()` returning `false`; the entitled branch exists and is unit-tested as inert.
4. **`LimitKind`** loses `RATE`, gains `CREDITS` → `{ PER_USER, DAILY, MONTHLY, CREDITS }`. `AlertScope` (`DAILY, MONTHLY`) and `AlertKind` (`WARNING, HARD_STOP`) are unchanged; a rule's `AlertScope` derives from its `windowKind` (`CALENDAR_DAY→DAILY`, `SINCE→MONTHLY`), and only `GLOBAL` rules raise alerts.

---

## File Structure

**`domains/identifiers/src/main/java/com/naturalist/usage/`**
- `UsageCounterId.java` *(new)* — `EntityId` surrogate for the reshaped counter rule.
- `UsageEventId.java` *(new)* — `EntityId` surrogate for events.
- `UsageTallyId.java` *(delete, last)*.
- `UsageCounterName.java`, `UsageAlertId.java` — unchanged.

**`domains/identifiers-test/src/main/java/com/naturalist/usage/TestUsageIdentifiers.java`** — add `UsageEvents`/reshaped `UsageCounters` constants; drop `UsageTallies`.

**`domains/usage/usage-api/src/main/java/com/naturalist/usage/`**
- `UsageEvent.java` *(new)*, `UsageCounter.java` *(reshape)*, `UsageScope.java` *(new)*, `WindowKind.java` *(new)*, `EntitlementLookup.java` *(new)*.
- `UsageRepository.java` *(reshape nested repos)*, `UsageQuery.java` *(reshape `ReserveState`, keep `snapshot`/`activeAlerts`)*, `UsageCommand.java` *(unchanged signatures)*.
- `UsageSnapshot.java` *(drop rate fields)*, `LimitKind.java` *(edit)*, `UsagePolicy.java` *(edit `scopeOf`)*.
- `UsageTally.java`, `UsageLimits.java` *(delete, last)*. `UsageAlert.java`, `AlertScope.java`, `AlertKind.java`, `BudgetExceededException.java`, `IdentificationBudget.java`, `NoOpIdentificationBudget.java` — unchanged.

**`domains/usage/usage-core/src/main/java/com/naturalist/usage/`**
- `UsageWindows.java` *(new — replaces `UsagePeriods`)*, `UsageQueryImpl.java` *(rewrite)*, `UsageCommandImpl.java` *(rewrite)*, `NoOpEntitlementLookup.java` *(new)*, `IdentificationBudgetImpl.java` — unchanged. `UsagePeriods.java` *(delete)*.

**`domains/usage/usage-repository-test/`** — `UsageEvent*` mock/contract/source (+ `usage-events.json`), reshaped `UsageCounter*` + `usage-counters.json`; delete `UsageTally*` + `usage-tallies.json`. `UsageAlert*` unchanged.

**`domains/usage/usage-repository-rdms/`** — `UsageEventRepositoryRdms` (new), delete `UsageTallyRepositoryRdms`; the other two swap only if the mock class name changed (it doesn't).

**`apps/management-console/src/main/java/com/naturalist/console/usage/`** — `UsageProperties.java` (drop `perUserDaily/globalDaily/globalMonthly/globalRatePerMinute`, keep `warningPercent/alertEmail`), `UsageConfiguration.java` (`warningPercent` bean instead of `UsageLimits`). `admin/AdminUsageController.java` + `jte/admin/usage.jte` (drop rate gauge). `pom.xml` unchanged (already depends on `usage-core` + `usage-repository-rdms`).

**Phase 5 — Resilience (`kernels/framework`, `adapters/resilience-resilience4j`, `adapters/anthropic-vision`, `apps/management-console`)** — new `RateLimiter` primitive + `RateLimiterConfig` + bridge + `vision.identification` rate config bean + wrap in `AnthropicVisionService` + `/admin/resilience` fifth group.

---

# PHASE 1 — Event entity + identifiers (additive, build stays green)

*Nothing references `UsageEvent` yet, so this phase compiles and tests in isolation.*

### Task 1: `UsageEventId` identifier

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/usage/UsageEventId.java`
- Test: covered by downstream contract tests (identifiers have no standalone test per repo convention; mirror `UsageAlertId`).

**Interfaces:**
- Produces: `UsageEventId.of(UUID)`, `UsageEventId.create()`.

- [ ] **Step 1: Write `UsageEventId`** (copy `UsageAlertId` verbatim, rename)

```java
package com.naturalist.usage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityId;

import java.util.UUID;

public final class UsageEventId extends EntityId {

    private UsageEventId(UUID value) {
        super(value);
    }

    @JsonCreator
    public static UsageEventId of(UUID value) {
        return new UsageEventId(value);
    }

    public static UsageEventId create() {
        return new UsageEventId(EntityId.newUUID());
    }
}
```

- [ ] **Step 2: Build** — `mvn -q -pl domains/identifiers compile`. Expected: SUCCESS.
- [ ] **Step 3: Stage** — `git add domains/identifiers/src/main/java/com/naturalist/usage/UsageEventId.java` (commit msg: `feat(usage): UsageEventId identifier`).

### Task 2: `UsageEvent` entity

**Files:**
- Create: `domains/usage/usage-api/src/main/java/com/naturalist/usage/UsageEvent.java`
- Test: none standalone (validated via the contract test in Task 4).

**Interfaces:**
- Consumes: `UsageEventId`, `UsageCounterName`, `NaturalistName`.
- Produces: `UsageEvent(UsageEventId id, UsageCounterName counterName, NaturalistName naturalist, Instant instant)` — component accessors `id()/counterName()/naturalist()/instant()`.

- [ ] **Step 1: Write `UsageEvent`**

```java
package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;

import java.time.Instant;
import java.util.function.Consumer;

/** Append-only identification event. Every event names the naturalist who caused it;
 *  global usage is count(all events), per-user usage is count(events for that naturalist). */
public record UsageEvent(UsageEventId id, UsageCounterName counterName,
                         NaturalistName naturalist, Instant instant) implements Entity<UsageEventId> {

    @Override
    public UsageEventId key() {
        return id;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counterName, "counterName")
                .identifier(naturalist, "naturalist")
                .notNull(instant, "instant");
    }
}
```

> Confirm `Entity<ID>` requires `key()` — mirror `UsageAlert` (which returns `id`). If `UsageAlert` does **not** declare `key()`, drop it here too and match that file exactly.

- [ ] **Step 2: Build** — `mvn -q -pl domains/usage/usage-api -am compile`. Expected: SUCCESS.
- [ ] **Step 3: Stage** (`feat(usage): UsageEvent append-only event entity`).

### Task 3: `UsageEvent` repository port, mock, TestEntitySource, seed

**Files:**
- Modify: `domains/usage/usage-api/src/main/java/com/naturalist/usage/UsageRepository.java` — add nested `EventRepository`.
- Create: `domains/usage/usage-repository-test/src/main/java/com/naturalist/usage/UsageEventRepositoryMock.java`
- Create: `.../UsageEventTestEntitySource.java`
- Create: `domains/usage/usage-repository-test/src/main/resources/usage/usage-events.json`

**Interfaces:**
- Produces: `UsageRepository.EventRepository extends EntityRepository<UsageEventId, UsageEvent>` with `List<UsageEvent> findByCounterSince(UsageCounterName counter, @Nullable NaturalistName naturalist, Instant since)` — `naturalist == null` ⇒ all events (global); non-null ⇒ that naturalist only. `since` is inclusive (`instant >= since`).

- [ ] **Step 1: Add the nested port** to `UsageRepository` (beside `TallyRepository`)

```java
protected interface EventRepository extends EntityRepository<UsageEventId, UsageEvent> {
    List<UsageEvent> findByCounterSince(UsageCounterName counter,
                                        @Nullable NaturalistName naturalist,
                                        Instant since);
}
```

- [ ] **Step 2: Write the mock finder** (`UsageEventRepositoryMock`, mirror `UsageTallyRepositoryMock`)

```java
package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.test.NaturalistDatabase;
import com.naturalist.test.repository.AbstractTestEntityRepository;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

class UsageEventRepositoryMock
        extends AbstractTestEntityRepository<UsageEventId, UsageEvent, UsageEventTestEntitySource>
        implements UsageRepository.EventRepository {

    UsageEventRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<UsageEvent> findByCounterSince(UsageCounterName counter,
                                               @Nullable NaturalistName naturalist,
                                               Instant since) {
        observer().arguments("findByCounterSince", i -> i
                        .identifier(counter, "counter")
                        .notNull(since, "since"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(e -> e.counterName().equals(counter)
                        && !e.instant().isBefore(since)
                        && (naturalist == null || e.naturalist().equals(naturalist)))
                .toList();
    }
}
```

> Import paths (`com.naturalist.test.NaturalistDatabase`, `AbstractTestEntityRepository`): copy from `UsageTallyRepositoryMock` — match that file's imports exactly.

- [ ] **Step 3: Write `UsageEventTestEntitySource`** (mirror `UsageTallyTestEntitySource`; FK on `counterName`, no unique constraint — events have no business key beyond `id`)

```java
package com.naturalist.usage;

import com.naturalist.test.NaturalistDatabase;
import com.naturalist.test.source.ForeignKeyConstraint;
import com.naturalist.test.source.TestEntitySource;

import java.util.List;

public class UsageEventTestEntitySource extends TestEntitySource<UsageEventId, UsageEvent> {

    public UsageEventTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("usage/usage-events.json");
    }

    @Override
    protected List<ForeignKeyConstraint<UsageEvent, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "counterName",
                UsageEvent::counterName,
                UsageCounterTestEntitySource.class));
    }
}
```

> `naturalist` is cross-domain → no FK (matches how `UsageTally.naturalist` was left un-constrained). Copy `ForeignKeyConstraint`/`TestEntitySource` import paths from `UsageTallyTestEntitySource`.

- [ ] **Step 4: Seed** `usage-events.json` = `[]` (events accrue at runtime, like tallies did).
- [ ] **Step 5: Build** — `mvn -q -pl domains/usage/usage-repository-test -am compile`. Expected: SUCCESS.
- [ ] **Step 6: Stage** (`feat(usage): UsageEvent repository port, mock, source, empty seed`).

### Task 4: `UsageEvent` behavioral contract + mock test + source test

**Files:**
- Create: `.../usage-repository-test/src/main/java/com/naturalist/usage/UsageEventRepositoryTest.java` (`@Test default` interface)
- Create: `.../src/test/java/com/naturalist/usage/UsageEventRepositoryMockTest.java`
- Create: `.../src/test/java/com/naturalist/usage/UsageEventTestEntitySourceTest.java`
- Modify: `domains/identifiers-test/.../TestUsageIdentifiers.java` — add `UsageEvents` constants.

**Interfaces:**
- Consumes: `UsageRepository.EventRepository`, `findByCounterSince`.
- Produces: contract coverage (3 hook set + null-arg / empty / expected for the finder). Since the JSON seed is empty, follow the **empty-catalog pattern** (agent-confirmed): the source test does **not** extend `TestEntitySourceTest`; the contract test seeds its own known rows in `@BeforeEach`.

- [ ] **Step 1: Add identifiers** to `TestUsageIdentifiers` (inside a new `UsageEvents` static class; keep the existing `NotFound` pattern)

```java
public static class UsageEvents {
    private UsageEvents() {}
    public static final UsageEventId Known1 =
            UsageEventId.of(UUID.fromString("019dbdb8-3a33-7eee-3a33-3a33a33a33a3"));
    public static final UsageEventId Known2 =
            UsageEventId.of(UUID.fromString("019dbdb8-4a44-7eee-4a44-4a44a44a44a4"));
    public static final class NotFound {
        public static final UsageEventId id =
                UsageEventId.of(UUID.fromString("019dbdb8-5a55-7eee-5a55-5a55a55a55a5"));
    }
}
```

- [ ] **Step 2: Write the failing contract interface** `UsageEventRepositoryTest extends EntityRepositoryTest<UsageEventId, UsageEvent>`. Hooks (mirror `UsageTallyRepositoryTest`): `repository()` → `UsageRepository.EventRepository`; `source()` → `db.getNamed(UsageEventTestEntitySource.class)`; a `@BeforeEach default void seedKnownEvents()` inserting two events for `TestUsageIdentifiers.UsageCounters.Identification` — `Known1` for `NaturalistName.of("patrick-way")` at `Instant.parse("2026-08-25T10:00:00Z")`, `Known2` for `NaturalistName.of("someone-else")` at `"2026-08-25T11:00:00Z"`; `notFoundName()` → `UsageEvents.NotFound.id`; `knownEntityNames()` → `List.of(Known1, Known2)`; `newEntity()`/`ghostEntity()` → full 4-arg constructors (ghost uses `NotFound.id`); `modifiedEntity(original)` → return `original` unchanged (events are immutable — document it, like the counter's no-op modify).

  Custom `@Test default` methods for `findByCounterSince`:
  - `findByCounterSince_nullCounter_throwsInvariantViolationException` → `assertThatThrownBy(() -> repository().findByCounterSince(null, null, Instant.parse("2026-08-25T00:00:00Z"))).isInstanceOf(InvariantViolationException.class).hasMessageContaining("counter")`
  - `findByCounterSince_nullSince_throwsInvariantViolationException` → same, null `since`, `.hasMessageContaining("since")`
  - `findByCounterSince_beforeWindow_returnsEmpty` → `since = "2026-08-26T00:00:00Z"`, null naturalist, `.isEmpty()`
  - `findByCounterSince_nullNaturalist_returnsAllInWindow` → `since = "2026-08-25T00:00:00Z"`, null naturalist, `.extracting(UsageEvent::id).containsExactlyInAnyOrder(Known1, Known2)`
  - `findByCounterSince_withNaturalist_returnsOnlyThatNaturalist` → `since = "2026-08-25T00:00:00Z"`, `NaturalistName.of("patrick-way")`, `.extracting(UsageEvent::id).containsExactly(Known1)`
  - `findByCounterSince_sinceIsInclusive` → `since = "2026-08-25T10:00:00Z"` (exactly Known1's instant), null naturalist, `.extracting(UsageEvent::id).contains(Known1)`

- [ ] **Step 3: Write mock test** — `class UsageEventRepositoryMockTest implements UsageEventRepositoryTest { @Override public UsageRepository.EventRepository repository() { return new UsageEventRepositoryMock(db); } }` (mirror `UsageTallyRepositoryMockTest`).
- [ ] **Step 4: Write source test** — `UsageEventTestEntitySourceTest` in the **standalone** (non-`TestEntitySourceTest`) form: `@RegisterExtension NaturalistTestExtension nte = NaturalistTestExtension.create();` + a `@Test roundTripInsertIsRetrievable()` that inserts one event via `nte.getNamed(UsageEventTestEntitySource.class)` and re-reads by id. Mirror `UsageTallyTestEntitySourceTest`.
- [ ] **Step 5: Run** — `mvn -q -pl domains/usage/usage-repository-test test`. Expected: PASS (all new contract cases green; the N+1 gate stays quiet — `findByCounterSince` is called once per test).
- [ ] **Step 6: Stage** (`test(usage): UsageEvent behavioral contract + mock/source tests`).

### Task 5: `UsageEventRepositoryRdms`

**Files:**
- Create: `domains/usage/usage-repository-rdms/src/main/java/com/naturalist/usage/UsageEventRepositoryRdms.java`

- [ ] **Step 1: Write the stub** (intermediate pattern — extend the mock)

```java
package com.naturalist.usage;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.test.NaturalistDatabase;

@DomainService
class UsageEventRepositoryRdms extends UsageEventRepositoryMock {
    UsageEventRepositoryRdms(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 2: Build** — `mvn -q -pl domains/usage/usage-repository-rdms -am compile`. Expected: SUCCESS.
- [ ] **Step 3: Stage** (`feat(usage): UsageEvent rdms adapter (mock-backed placeholder)`).

---

# PHASE 2 — Reshape `UsageCounter` into the editable rule

*`UsageCounter` is referenced only inside the usage module (source/mock/contract/seed + the two `*Impl` reads). This phase changes it and its consumers together so the module still compiles; the `*Impl` reads are fully rewritten in Phase 3, so here they are updated just enough to compile.*

### Task 6: `UsageScope`, `WindowKind`, reshaped `UsageCounter`, `UsageCounterId`

**Files:**
- Create: `domains/identifiers/.../usage/UsageCounterId.java` (copy `UsageAlertId`, rename).
- Create: `domains/usage/usage-api/.../UsageScope.java`, `.../WindowKind.java`.
- Modify: `domains/usage/usage-api/.../UsageCounter.java` — from `NamedEntity<UsageCounterName>(name)` to `Entity<UsageCounterId>`.

**Interfaces:**
- Produces:
  - `enum UsageScope { GLOBAL, PER_USER }`
  - `enum WindowKind { CALENDAR_DAY, SINCE }`
  - `UsageCounter(UsageCounterId id, UsageCounterName counterName, UsageScope scope, WindowKind windowKind, @Nullable Instant since, int limit, boolean active)` with `withLimit(int)`, `withActive(boolean)`, `withSince(Instant)`; accessors `id()/counterName()/scope()/windowKind()/since()/limit()/active()`.

- [ ] **Step 1: Write the two enums** (trivial; each `public enum`).
- [ ] **Step 2: Write `UsageCounterId`** (identical to `UsageEventId` in Task 1, renamed).
- [ ] **Step 3: Rewrite `UsageCounter`**

```java
package com.naturalist.usage;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

public record UsageCounter(UsageCounterId id, UsageCounterName counterName, UsageScope scope,
                           WindowKind windowKind, @Nullable Instant since, int limit,
                           boolean active) implements Entity<UsageCounterId> {

    public UsageCounter withLimit(int newLimit) {
        return new UsageCounter(id, counterName, scope, windowKind, since, newLimit, active);
    }

    public UsageCounter withActive(boolean newActive) {
        return new UsageCounter(id, counterName, scope, windowKind, since, limit, newActive);
    }

    public UsageCounter withSince(Instant newSince) {
        return new UsageCounter(id, counterName, scope, windowKind, newSince, limit, active);
    }

    @Override
    public UsageCounterId key() {
        return id;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(counterName, "counterName")
                .notNull(scope, "scope")
                .notNull(windowKind, "windowKind")
                .min(limit, 0, "limit")
                .whenTrue(windowKind == WindowKind.SINCE,
                        c -> c.notNull(since, "since"));
    }
}
```

> Verify the exact Constraints DSL names (`min`, `whenTrue`) against `UsageTally`/`UsageAlert` invariants — if `whenTrue`/`min` don't exist, use the same idiom those files use (e.g. `whenNotNull` for the `since` case, and drop the `min` if unavailable). `key()` present iff `UsageAlert` declares it.

- [ ] **Step 4: Build api** — `mvn -q -pl domains/usage/usage-api -am compile`. Expected: FAIL in `UsageQueryImpl`/`UsageCommandImpl` (they reference the old `UsageCounter`) — that's next.
- [ ] **Step 5: Stage** (`feat(usage): UsageCounter becomes editable rule entity (scope/window/limit/active)`).

### Task 7: Counter repository finder, source, seed, contract

**Files:**
- Modify: `.../UsageRepository.java` — `CounterRepository` now `EntityRepository<UsageCounterId, UsageCounter>` + `List<UsageCounter> findByCounterName(UsageCounterName counterName)`.
- Modify: `.../UsageCounterRepositoryMock.java`, `.../UsageCounterTestEntitySource.java`, `.../UsageCounterRepositoryTest.java`, `.../UsageCounterRepositoryMockTest.java`, `.../UsageCounterTestEntitySourceTest.java`.
- Modify: `.../resources/usage/usage-counters.json` — three seeded rules.
- Modify: `TestUsageIdentifiers.UsageCounters` — add rule ids.

**Interfaces:**
- Produces: `CounterRepository.findByCounterName(UsageCounterName)` → all rules for that activity (active + inactive). Unique constraint on `(counterName, scope, windowKind)`.

- [ ] **Step 1: Reshape the port**

```java
protected interface CounterRepository extends EntityRepository<UsageCounterId, UsageCounter> {
    List<UsageCounter> findByCounterName(UsageCounterName counterName);
}
```

- [ ] **Step 2: Mock finder** (in `UsageCounterRepositoryMock`, now `AbstractTestEntityRepository<UsageCounterId, UsageCounter, UsageCounterTestEntitySource>`)

```java
@Override
public List<UsageCounter> findByCounterName(UsageCounterName counterName) {
    observer().arguments("findByCounterName", i -> i.identifier(counterName, "counterName"))
            .throwWhenInvalid();
    return testEntitySource().entityStream()
            .filter(c -> c.counterName().equals(counterName))
            .toList();
}
```

- [ ] **Step 3: Source constraints** (`UsageCounterTestEntitySource` — add unique on the rule key; keep no FK, it's the FK target)

```java
@Override
protected List<UniqueConstraint<UsageCounter>> uniqueConstraints() {
    return List.of(new UniqueConstraint<>() {
        @Override public String name() { return "counterName+scope+windowKind"; }
        @Override public Function<UsageCounter, ?> valueFunction() {
            return c -> c.counterName().value() + ":" + c.scope() + ":" + c.windowKind();
        }
    });
}
```

- [ ] **Step 4: Seed `usage-counters.json`** (three rules; ids are fixed UUIDv7 constants)

```json
[
  { "id": "019dbdb9-0d01-7eee-0d01-0d010d010d01", "counterName": "identification",
    "scope": "PER_USER", "windowKind": "CALENDAR_DAY", "since": null, "limit": 10, "active": true },
  { "id": "019dbdb9-0d02-7eee-0d02-0d020d020d02", "counterName": "identification",
    "scope": "GLOBAL", "windowKind": "CALENDAR_DAY", "since": null, "limit": 50, "active": true },
  { "id": "019dbdb9-0d03-7eee-0d03-0d030d030d03", "counterName": "identification",
    "scope": "GLOBAL", "windowKind": "SINCE", "since": "2026-08-01T00:00:00Z", "limit": 650, "active": true }
]
```

- [ ] **Step 5: Identifiers** — in `TestUsageIdentifiers.UsageCounters`, keep `Identification`/`InsectIdentification` (the `UsageCounterName`s) and add rule-id constants `PerUserDailyId`/`GlobalDailyId`/`GlobalMonthlyId` matching the seed ids, plus a `NotFound.id` (`UsageCounterId`). Update any usage of the old `UsageCounters.NotFound.name` (a `UsageCounterName`) that referred to the counter *entity* — the entity key is now an id.
- [ ] **Step 6: Rewrite `UsageCounterRepositoryTest`** to `EntityRepositoryTest<UsageCounterId, UsageCounter>`: `repository()`/`source()` as before; `notFoundName()` → `UsageCounters.NotFound.id`; `knownEntityNames()` → `List.of(PerUserDailyId, GlobalDailyId, GlobalMonthlyId)`; `newEntity()`/`ghostEntity()` → full 7-arg constructors with fresh `UsageCounterId.create()` (ghost uses `NotFound.id`), unique `(scope,windowKind)` combos not colliding with seeds (e.g. `newEntity` = a `PER_USER, SINCE` rule); `modifiedEntity(original)` → `original.withLimit(original.limit() + 5).withActive(!original.active())`. Add custom `@Test default` for `findByCounterName`: null-arg throws (`.hasMessageContaining("counterName")`); unknown name → empty; `Identification` → `hasSize(3)`. Remove the old `seedSecondKnownCounter` `@BeforeEach` (three rules now seed from JSON).
- [ ] **Step 7: Source test** — `UsageCounterTestEntitySourceTest` now has 3 seeded rows: `minimumEntities()` → `3`; assert the monthly rule loads with `windowKind()==SINCE` and `since()!=null`.
- [ ] **Step 8: Mock test** — `UsageCounterRepositoryMockTest.repository()` returns `new UsageCounterRepositoryMock(db)` (unchanged shape).
- [ ] **Step 9: Run** — `mvn -q -pl domains/usage/usage-repository-test test`. Expected: FAIL only in `usage-core` downstream (not this module); this module's tests PASS. (If core is in the reactor, defer full green to Phase 3.)
- [ ] **Step 10: Stage** (`feat(usage): counter rule repository finder, 3-rule seed, contract`).

---

# PHASE 3 — Rewrite `usage-core` (window math, reserve, snapshot, seam)

### Task 8: `UsageWindows` helper (replaces `UsagePeriods`)

**Files:**
- Create: `domains/usage/usage-core/.../UsageWindows.java`
- Delete: `domains/usage/usage-core/.../UsagePeriods.java` (after Task 10).
- Test: `domains/usage/usage-core/src/test/java/com/naturalist/usage/UsageWindowsTest.java`

**Interfaces:**
- Produces (all take the rule + a UTC `now`):
  - `static Instant windowStart(UsageCounter rule, Instant now)` — `CALENDAR_DAY` → start of `now`'s UTC day; `SINCE` → `rule.since()`.
  - `static Instant resetAt(UsageCounter rule, Instant now)` — `CALENDAR_DAY` → start of next UTC day; `SINCE` → start of next UTC month of `now` (nominal, display-only).
  - `static AlertScope alertScope(UsageCounter rule)` — `CALENDAR_DAY` → `DAILY`; `SINCE` → `MONTHLY`.
  - `static String alertPeriod(UsageCounter rule, Instant now)` — `DAILY` → `"daily-" + utcDate(now)`; `MONTHLY` → `"monthly-" + utcYearMonth(now)`.

- [ ] **Step 1: Write the failing test**

```java
@Test
void calendarDayWindowStartsAndResetsAtUtcMidnight() {
    var rule = new UsageCounter(UsageCounterId.create(), UsageCounterName.of("identification"),
            UsageScope.GLOBAL, WindowKind.CALENDAR_DAY, null, 50, true);
    var now = Instant.parse("2026-08-25T14:30:00Z");
    assertThat(UsageWindows.windowStart(rule, now)).isEqualTo(Instant.parse("2026-08-25T00:00:00Z"));
    assertThat(UsageWindows.resetAt(rule, now)).isEqualTo(Instant.parse("2026-08-26T00:00:00Z"));
    assertThat(UsageWindows.alertPeriod(rule, now)).isEqualTo("daily-2026-08-25");
}

@Test
void sinceWindowStartsAtConfiguredInstant() {
    var since = Instant.parse("2026-08-01T00:00:00Z");
    var rule = new UsageCounter(UsageCounterId.create(), UsageCounterName.of("identification"),
            UsageScope.GLOBAL, WindowKind.SINCE, since, 650, true);
    var now = Instant.parse("2026-08-25T14:30:00Z");
    assertThat(UsageWindows.windowStart(rule, now)).isEqualTo(since);
    assertThat(UsageWindows.alertPeriod(rule, now)).isEqualTo("monthly-2026-08");
}
```

- [ ] **Step 2: Run → FAIL** (`UsageWindows` not defined).
- [ ] **Step 3: Implement `UsageWindows`** — pure `LocalDate.ofInstant(now, ZoneOffset.UTC)` / `YearMonth.from(...)` math (mirror the deleted `UsagePeriods` UTC anchoring, javadoc the UTC-clock assumption). `windowStart`/`resetAt` switch on `windowKind`; `alertScope`/`alertPeriod` as specified.
- [ ] **Step 4: Run → PASS**.
- [ ] **Step 5: Stage** (`feat(usage): UsageWindows window-math helper`).

### Task 9: `EntitlementLookup` seam + no-op

**Files:**
- Create: `domains/usage/usage-api/.../EntitlementLookup.java`
- Create: `domains/usage/usage-core/.../NoOpEntitlementLookup.java`

**Interfaces:**
- Produces: `interface EntitlementLookup { boolean isEntitled(NaturalistName naturalist); static EntitlementLookup none(); }`; `@DomainService`-registered `NoOpEntitlementLookup` returning `false`.

- [ ] **Step 1: Write `EntitlementLookup`** (api)

```java
package com.naturalist.usage;

import com.naturalist.naturalist.NaturalistName;

/** Seam for the future paid/credit bucket. Today always false (public bucket). */
public interface EntitlementLookup {
    boolean isEntitled(NaturalistName naturalist);
    static EntitlementLookup none() { return naturalist -> false; }
}
```

- [ ] **Step 2: Write `NoOpEntitlementLookup`** (core, `@DomainService`, `isEntitled` → `false`).
- [ ] **Step 3: Build** — `mvn -q -pl domains/usage/usage-core -am compile`. Expected: still failing in the two `*Impl` (next tasks).
- [ ] **Step 4: Stage** (`feat(usage): inert EntitlementLookup seam for future credits`).

### Task 10: Rewrite `UsageQuery.ReserveState` + `UsageQueryImpl`

**Files:**
- Modify: `domains/usage/usage-api/.../UsageQuery.java` — new `ReserveState`; keep `snapshot()`/`activeAlerts()`.
- Modify: `domains/usage/usage-api/.../UsageSnapshot.java` — drop `rateUsed`/`ratePerMinute`.
- Rewrite: `domains/usage/usage-core/.../UsageQueryImpl.java`.
- Test: `domains/usage/usage-core/src/test/java/com/naturalist/usage/UsageQueryImplTest.java` (new) + update `UsageCoreTestContext`.

**Interfaces:**
- Produces:
  - `UsageSnapshot(int dailyUsed, int dailyLimit, int monthlyUsed, int monthlyLimit, List<UserUsage> users)` (nested `UserUsage(String naturalist, int used, int limit)` unchanged).
  - `UsageQuery.reserveState(UsageCounterName counter, NaturalistName naturalist, Instant now)` → `ReserveState(List<CounterUsage> counters)`, where `record CounterUsage(UsageCounter rule, int used)` (both `ReadModel`). `used` = count of events matching the rule's window start and scope filter, from **one** batched read.
  - Constructor: `UsageQueryImpl(int warningPercent, Clock clock, UsageRepository.CounterRepository counters, UsageRepository.EventRepository events, UsageRepository.AlertRepository alerts)`.

- [ ] **Step 1: Rewrite `UsageSnapshot`** (drop the two rate ints + their args).
- [ ] **Step 2: Rewrite `ReserveState`** in `UsageQuery` (a `List<CounterUsage>`; `invariants()` asserts each `rule` non-null and `used >= 0`). Keep `snapshot()`/`activeAlerts()` signatures identical so the dashboard controller is untouched at the port level.
- [ ] **Step 3: Write the failing `UsageQueryImplTest`** — wire `UsageCoreTestContext.create(nte, 80 /*warningPercent*/, fixedClock)`; insert two events for `patrick-way` today via the event source; assert `reserveState(Identification, patrick-way, now).counters()` contains the per-user-daily rule with `used == 2` and the global-daily rule with `used == 2`; assert `snapshot().dailyUsed() == 2` and `snapshot().users()` has one row `(patrick-way, 2, 10)`.
- [ ] **Step 4: Run → FAIL**.
- [ ] **Step 5: Implement `UsageQueryImpl`** — `@DomainService`. `reserveState`:

```java
@Override
public ReserveState reserveState(UsageCounterName counter, NaturalistName naturalist, Instant now) {
    observer.arguments("reserveState", i -> i
            .identifier(counter, "counter").identifier(naturalist, "naturalist").notNull(now, "now"))
            .throwWhenInvalid();

    List<UsageCounter> rules = counters.findByCounterName(counter).stream()
            .filter(UsageCounter::active).toList();
    if (rules.isEmpty()) {
        return new ReserveState(List.of());
    }
    Instant earliest = rules.stream()
            .map(r -> UsageWindows.windowStart(r, now))
            .min(Comparator.naturalOrder()).orElse(now);

    List<UsageEvent> all = events.findByCounterSince(counter, null, earliest);   // ONE gated read

    List<CounterUsage> usages = rules.stream()
            .map(r -> new CounterUsage(r, countFor(all, r, naturalist, now)))
            .toList();
    return new ReserveState(usages);
}

private static int countFor(List<UsageEvent> all, UsageCounter rule, NaturalistName naturalist, Instant now) {
    Instant start = UsageWindows.windowStart(rule, now);
    return (int) all.stream()
            .filter(e -> !e.instant().isBefore(start))
            .filter(e -> rule.scope() == UsageScope.GLOBAL || e.naturalist().equals(naturalist))
            .count();
}
```

  `snapshot()` — read the active rules + one `findByCounterSince(counter, null, earliestOf(daily,monthly))`; pull `dailyUsed`/`monthlyUsed` from the two `GLOBAL` rules (`CALENDAR_DAY`/`SINCE`), `dailyLimit`/`monthlyLimit` from their `limit()`; build `users` by grouping the current-day events by `naturalist` in memory (`Collectors.groupingBy` + count), `limit` from the `PER_USER` rule. `activeAlerts()` → `alerts.getUnacknowledged()` (unchanged).

  > **N+1 gate:** exactly two selects (`counters.findByCounterName` once, `events.findByCounterSince` once) — no per-rule/per-user repository call. The user grouping is in-memory. Keep it that way.

- [ ] **Step 6: Update `UsageCoreTestContext`** — factory `create(NaturalistDatabase db, int warningPercent, Clock clock)`, wiring `UsageCounterRepositoryMock` + `UsageEventRepositoryMock` + `UsageAlertRepositoryMock`, then `UsageQueryImpl(warningPercent, clock, counters, events, alerts)` and (Task 11) `UsageCommandImpl`. Expose `counters()`/`events()`/`alerts()`/`query()`/`command()`.
- [ ] **Step 7: Run → PASS**.
- [ ] **Step 8: Stage** (`feat(usage): count-over-events read adapter (reserveState + snapshot)`).

### Task 11: Rewrite `UsageCommandImpl.reserve`

**Files:**
- Rewrite: `domains/usage/usage-core/.../UsageCommandImpl.java`.
- Modify: `.../UsageCommand.java` — `reserve(NaturalistName)` signature unchanged (the `identification` counter is fixed inside).
- Modify: `LimitKind.java` (drop `RATE`, add `CREDITS`), `UsagePolicy.scopeOf` (throw for `PER_USER`/`CREDITS`).
- Test: rewrite `UsageCommandImplTest` (preserve every scenario; drop the rate one).

**Interfaces:**
- Consumes: `UsageQuery.reserveState`, `UsageWindows`, `EntitlementLookup`, `UsageRepository.EventRepository`/`AlertRepository`.
- Produces: `UsageCommandImpl(UsageQuery query, EntitlementLookup entitlements, int warningPercent, Clock clock, UsageRepository.EventRepository events, UsageRepository.AlertRepository alerts)`; `reserve`/`acknowledge`/`claimUnsentAlerts` all `synchronized` (unchanged behavior for the latter two).

- [ ] **Step 1: Edit `LimitKind`** → `{ PER_USER, DAILY, MONTHLY, CREDITS }`; edit `UsagePolicy.scopeOf` to `DAILY→AlertScope.DAILY`, `MONTHLY→AlertScope.MONTHLY`, default (`PER_USER`/`CREDITS`) → `throw new IllegalArgumentException(...)`.
- [ ] **Step 2: Write the failing `UsageCommandImplTest`** scenarios (mirror the current ones over the new wiring; use `EntitlementLookup.none()`):
  - `per_user_daily_quota_blocks_after_limit` — seed a `PER_USER CALENDAR_DAY limit 2` rule (via the counter source), reserve 3× for one naturalist → 3rd throws `BudgetExceededException`, `limitKind()==PER_USER`, and `context.events()` holds exactly 2 events.
  - `global_daily_cap_blocks_across_users` — `GLOBAL CALENDAR_DAY limit 2`; two different naturalists reserve; 3rd (a third naturalist) throws `DAILY`.
  - `rejected_reserve_inserts_no_event` — per-user limit 1; after the rejected 2nd reserve, `query().snapshot().dailyUsed() == 1`.
  - `warning_and_hard_stop_alerts_recorded_once` — `GLOBAL SINCE limit 5`, `warningPercent 80` (`warningThreshold(5,80)=4`); reserve 5×, then 3 rejected; `alerts().getUnacknowledged()` has exactly one `WARNING` (recorded at the 4th event) and one `HARD_STOP` (recorded on the first rejection), deduped.
  - `entitled_naturalist_bypasses_public_rules` — inject an `EntitlementLookup` returning `true`; with a `GLOBAL CALENDAR_DAY limit 0` rule that would block everyone, an entitled naturalist's reserve **succeeds** and inserts an event (proves the branch is wired; today production uses `none()`).
  - `concurrent_reserves_do_not_overshoot` — 16 threads, 500 attempts, `GLOBAL` limit 100 → exactly 100 events inserted (keep the existing thread-safety assertion).
- [ ] **Step 3: Run → FAIL**.
- [ ] **Step 4: Implement `reserve`** (`synchronized`)

```java
@Override
public synchronized void reserve(NaturalistName naturalist) {
    observer.arguments("reserve", i -> i.identifier(naturalist, "naturalist")).throwWhenInvalid();
    Instant now = clock.instant();

    if (!entitlements.isEntitled(naturalist)) {                 // PUBLIC bucket
        UsageQuery.ReserveState state = query.reserveState(COUNTER, naturalist, now);
        for (UsageQuery.CounterUsage cu : state.counters()) {
            UsageCounter rule = cu.rule();
            if (cu.used() >= rule.limit()) {
                if (rule.scope() == UsageScope.GLOBAL) {
                    recordAlert(AlertKind.HARD_STOP, rule, cu.used(), now);
                }
                throw new BudgetExceededException(limitKindOf(rule), UsageWindows.resetAt(rule, now));
            }
            if (rule.scope() == UsageScope.GLOBAL
                    && cu.used() + 1 == UsagePolicy.warningThreshold(rule.limit(), warningPercent)) {
                recordAlert(AlertKind.WARNING, rule, cu.used() + 1, now);
            }
        }
    }
    // ENTITLED bucket falls straight through (future: credit-balance check).
    events.insert(new UsageEvent(UsageEventId.create(), COUNTER, naturalist, now));
}
```

  Helpers: `COUNTER = UsageCounterName.of("identification")`; `limitKindOf(rule)` → `PER_USER` when `scope==PER_USER`, else `windowKind==CALENDAR_DAY ? DAILY : MONTHLY`; `recordAlert` mirrors the current one but takes the rule (uses `UsageWindows.alertScope(rule)`/`alertPeriod(rule, now)` for the dedup key and `UsagePolicy.alertMessage(scope, kind, used, rule.limit())`). Keep `acknowledge`/`claimUnsentAlerts` bodies as-is (still `synchronized`, still on the `alerts` repo).
- [ ] **Step 5: Run → PASS** (all scenarios, incl. no-overshoot).
- [ ] **Step 6: Delete `UsagePeriods.java`**; `mvn -q -pl domains/usage/usage-core test`.
- [ ] **Step 7: Stage** (`feat(usage): reserve over the event log with public/entitled buckets`).

---

# PHASE 4 — App wiring + remove the tally

### Task 12: Console config, properties, dashboard

**Files:**
- Modify: `apps/management-console/.../usage/UsageProperties.java` — record `(int warningPercent, String alertEmail, ...dispatch)`; drop `perUserDaily/globalDaily/globalMonthly/globalRatePerMinute`.
- Modify: `.../usage/UsageConfiguration.java` — replace the `UsageLimits` bean with `@Bean int usageWarningPercent(UsageProperties p) { return p.warningPercent(); }` (or inject `warningPercent` directly where the adapters are discovered). Keep the `Clock`/`@EnableScheduling` beans.
- Modify: `.../admin/AdminUsageController.java` — drop the rate `Gauge` (snapshot no longer has `rateUsed/ratePerMinute`); keep daily + monthly gauges + per-user table + alerts.
- Modify: `.../jte/admin/usage.jte` — delete the rate-gauge rendering.
- Modify: `apps/management-console/src/main/resources/application.yml` + test yml — drop `per-user-daily/global-daily/global-monthly/global-rate-per-minute`; keep `warning-percent`, `alert-email`, `dispatch-interval-ms`. (The limits now live in `usage-counters.json`.)

**Interfaces:**
- Consumes: `UsageQuery.snapshot()` (new 5-field `UsageSnapshot`), `activeAlerts()`, `UsageCommand.acknowledge()` — all unchanged in signature, so only the rate-gauge code changes.

- [ ] **Step 1: Edit `UsageProperties`/`UsageConfiguration`/yml** as above.

> The three `@DomainService` adapters (`UsageQueryImpl`/`UsageCommandImpl`/`IdentificationBudgetImpl`) now need an `int warningPercent` constructor arg discovered by `DomainServiceScan`. Confirm how `DomainServiceScan` supplies scalar constructor args (it resolved `UsageLimits` before). If it injects beans by type, expose `warningPercent` as a small wrapper bean/record (e.g. `record WarningPercent(int value)`) rather than a bare `int`, and take that in the impls. Match whatever mechanism `UsageLimits` used.

- [ ] **Step 2: Edit `AdminUsageController`** — remove the rate `Gauge` from the `gauges` list; the JSON `UsageReport`/`report()` is unchanged (it wraps `snapshot()` + `activeAlerts()`).
- [ ] **Step 3: Edit `usage.jte`** — remove the rate gauge block only.
- [ ] **Step 4: Build the app** — `mvn -q -pl apps/management-console -am compile`. Expected: SUCCESS.
- [ ] **Step 5: Run the admin web-mvc tests** — `mvn -q -pl apps/management-console test -Dtest='AdminUsage*'`. Fix assertions that referenced rate or the old per-user-daily yml keys; the `reserve()`-drives-page and ack-clears-banner flows should otherwise hold (they exercise the same ports). Expected: PASS.
- [ ] **Step 6: Stage** (`feat(usage): dashboard + config read off the event-log counters`).

### Task 13: Remove `UsageTally` and update `TestUsageIdentifiers`

**Files:**
- Delete: `UsageTally.java`, `UsageTallyId.java`, `UsageLimits.java`, `UsageTallyRepositoryMock.java`, `UsageTallyRepositoryTest.java`, `UsageTallyTestEntitySource.java`, `UsageTallyRepositoryMockTest.java`, `UsageTallyTestEntitySourceTest.java`, `UsageTallyRepositoryRdms.java`, `resources/usage/usage-tallies.json`.
- Modify: `.../UsageRepository.java` — remove `TallyRepository`. `TestUsageIdentifiers` — remove `UsageTallies`.
- Grep guard: `grep -rn "UsageTally\|UsageLimits\|findByCounterAndPeriods\|usage-tallies" --include=*.java --include=*.json --include=*.jte .` must return nothing after deletion.

- [ ] **Step 1: Delete the files** listed above and the `TallyRepository` nested interface.
- [ ] **Step 2: Run the grep guard** — expect zero hits (fix any straggler).
- [ ] **Step 3: Full module + app build** — `mvn -q -pl domains/usage/usage-api,domains/usage/usage-core,domains/usage/usage-repository-test,domains/usage/usage-repository-rdms,apps/management-console -am verify`. Expected: SUCCESS.
- [ ] **Step 4: Completeness gate** — `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`. Expected: no pending fixes/markers (query hygiene clean — reads are single-shot).
- [ ] **Step 5: Stage** (`refactor(usage): remove UsageTally in favor of the event log`).

---

# PHASE 5 — Resilience `RateLimiter` primitive + wrap the vision spend (standalone PR)

> Independent of Phases 1–4. Touches `kernels/framework` → **clean `mvn install`** after the framework change. Adds the fifth Resilience primitive following the existing four exactly.

### Task 14: `RateLimiter` primitive + `RateLimiterConfig` + facade methods

**Files:**
- Create: `kernels/framework/.../resilience/RateLimiter.java` (interface: `<T> T execute(Supplier<T>)`, `void execute(Runnable)` — copy `Timeout`).
- Modify: `kernels/framework/.../resilience/Resilience.java` — add `RateLimiter rateLimiter(String name)` + `Set<String> rateLimiterNames()`.
- Modify: `.../resilience/NoOpResilience.java` — implement both (return a pass-through `RateLimiter`, empty name set).
- Modify: `.../resilience/ResilienceConfig.java` (sealed) — add `record RateLimiterConfig(String name, int limitForPeriod, Duration limitRefreshPeriod, Duration timeoutDuration) implements ResilienceConfig` with `invariants()` (positive `limitForPeriod`, non-null durations, a sane ceiling e.g. `limitRefreshPeriod <= 1m`).
- Test: `kernels/framework` — extend the resilience facade/no-op test to cover `rateLimiter`/`rateLimiterNames`.

- [ ] **Step 1: Write the failing test** — `NoOpResilience.rateLimiter("x").execute(() -> 42)` returns `42`; `rateLimiterNames()` is empty; a `RateLimiterConfig` with `limitForPeriod <= 0` fails its invariants.
- [ ] **Step 2: Run → FAIL**.
- [ ] **Step 3: Implement** the interface method, no-op, and config variant (mirror `TimeoutConfig`/`BulkheadConfig` shape + ceilings).
- [ ] **Step 4: Run → PASS**; then **clean install** `mvn -q -pl kernels/framework install`.
- [ ] **Step 5: Stage** (`feat(resilience): RateLimiter primitive + config on the facade`).

### Task 15: Resilience4j bridge for `RateLimiter`

**Files:**
- Create: `adapters/resilience-resilience4j/.../Resilience4jRateLimiter.java` (wraps `io.github.resilience4j.ratelimiter.RateLimiter`, maps `RequestNotPermitted` through — let it propagate).
- Modify: `.../Resilience4jResilience.java` — add a `RateLimiterRegistry`, a fifth immutable map, the `rateLimiter(name)`/`rateLimiterNames()` overrides (throw `UnconfiguredResilienceException` on miss), a `toR4j(RateLimiterConfig)` converter, and the `switch` case populating the map.
- Test: `adapters/resilience-resilience4j` — a `RateLimiterConfig(limitForPeriod=2, refresh=1s)` allows 2 calls then the 3rd throws `RequestNotPermitted`.

- [ ] **Step 1: Write the failing adapter test** (2 permitted, 3rd throws `RequestNotPermitted` within the window).
- [ ] **Step 2: Run → FAIL**.
- [ ] **Step 3: Implement** the bridge (mirror `Resilience4jTimeout` + `toR4j(TimeoutConfig)`; `RateLimiterConfig.custom().limitForPeriod(...).limitRefreshPeriod(...).timeoutDuration(...)`).
- [ ] **Step 4: Run → PASS**.
- [ ] **Step 5: Stage** (`feat(resilience): resilience4j RateLimiter bridge`).

### Task 16: Register the vision rate config + admin page + name validator

**Files:**
- Modify: `apps/management-console/.../resilience/ResilienceConfiguration.java` — add `@Bean ResilienceConfig visionRateLimit() { return new RateLimiterConfig("vision.identification", 3, Duration.ofMinutes(1), Duration.ZERO); }` (3 vision requests/min; `timeoutDuration=0` → reject immediately when over, so it surfaces as a budget-style flash rather than blocking the request thread).
- Modify: `.../admin/AdminResilienceController.java` — add a fifth `Group("Rate Limiter", sorted(resilience.rateLimiterNames()))`.
- Modify: `.../resilience/ResilienceNameValidator.java` — union `rateLimiterNames()` into the validated-names set.
- Test: an app context test asserting boot succeeds with the new bean and `/admin/resilience` lists `vision.identification` under rate limiters.

- [ ] **Step 1: Write the failing test** (context loads; `resilience.rateLimiterNames()` contains `vision.identification`).
- [ ] **Step 2: Implement** the bean + admin group + validator union.
- [ ] **Step 3: Run → PASS**.
- [ ] **Step 4: Stage** (`feat(usage): register global vision rate limit (3/min)`).

### Task 17: Wrap the vision spend

**Files:**
- Modify: `adapters/anthropic-vision/.../AnthropicVisionService.java` — wrap the single `issueRequest`/`client.messages().create` call site: `resilience.rateLimiter(STRATEGY).execute(() -> resilience.timeout(STRATEGY).execute(() -> client.messages().create(params)))` (nested `execute`, exactly the `InMemoryCatalog` breaker+timeout idiom). `STRATEGY` stays `"vision.identification"`.
- Modify: `domains/insects/insects-console/.../InsectsController.java` — extend the `identify` catch to also map `io.github.resilience4j.ratelimiter.RequestNotPermitted` to the same friendly redirect, e.g. `redirect:/insects/identify?limit=RATE` (add a `?limit=RATE` copy branch to the identify GET view). Since `RequestNotPermitted` is a resilience4j type, catch it where the composition root already imports resilience4j, or translate it to `BudgetExceededException(... )` — **decision:** catch `RequestNotPermitted` in the controller and redirect with `limit=RATE` (no new `LimitKind`; the query-param copy is UI-only).
- Test: `AnthropicVisionService` test — a `Resilience` stub whose `rateLimiter` rejects the 2nd call makes the 2nd `identify` throw `RequestNotPermitted` (vision client never called the 2nd time). `InsectsController` test — a rejecting rate limiter yields `redirect:/insects/identify?limit=RATE`.

- [ ] **Step 1: Write the failing `AnthropicVisionService` test** (rate-limiter stub rejects → `RequestNotPermitted`, client not invoked).
- [ ] **Step 2: Run → FAIL**.
- [ ] **Step 3: Implement** the nested wrap in `AnthropicVisionService`.
- [ ] **Step 4: Write + pass the `InsectsController` rate-rejection test**; add the `limit=RATE` copy in the identify GET template.
- [ ] **Step 5: Run → PASS**; **clean install + gate** — `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, then `mvn verify`.
- [ ] **Step 6: Stage** (`feat(usage): rate-limit vision spend via Resilience, friendly over-rate flash`).

---

## Self-Review

**Spec coverage** (design §Model / §Buckets / §Reserve / §Rate / §Alerting / §What changes / §Testing):
- Event log as source of truth → Tasks 2–5. Counting in core over a windowed fetch, repo stays a pure cache → Task 10 (`reserveState`/`snapshot`), Task 3 (`findByCounterSince`). Batched, no N+1 → Task 10 note.
- `UsageCounter` reshaped to editable rule (scope/window/limit/active) → Tasks 6–7. `FixedCalendarDay` + `FixedSince` (Option B) → Task 8 (`WindowKind`/`UsageWindows`), seed Task 7.
- Public bucket built, entitled bucket inert seam → Tasks 9, 11 (`isEntitled` branch + test). `A + blocked`, no `chargedTo` tag → `UsageEvent` has no bucket field (Task 2).
- Rate → Resilience4j, out of the module → Tasks 14–17. `LimitKind` loses `RATE`, gains `CREDITS` → Task 11.
- Alerts / dispatcher / dashboard retained, reading events → Tasks 11 (`recordAlert`), 12 (dashboard). `UsageAlert` unchanged.
- What-changes (rewrite, no data migration; `UsageTally`→`UsageEvent`; `UsagePeriods` shrinks; CQS split kept) → Tasks 8, 10, 11, 13.
- Testing bar (no-overshoot, window boundaries, all-or-nothing + `LimitKind`, alert dedup, rate rejection, repo contract, inert seam) → Tasks 4, 8, 10, 11, 15, 17.

**Placeholder scan:** no "TBD"/"handle edge cases" — the two `>`-flagged spots (`key()` presence, Constraints DSL names, `DomainServiceScan` scalar-arg mechanism) are **verify-against-reference** notes with a named fallback, not deferred work.

**Type consistency:** `findByCounterSince(UsageCounterName, @Nullable NaturalistName, Instant)` and `findByCounterName(UsageCounterName)` used identically across Tasks 3/7/10; `UsageCounter` 7-arg ctor + `withLimit/withActive/withSince` consistent Tasks 6/7/8/10/11; `reserveState(UsageCounterName, NaturalistName, Instant)` + `ReserveState(List<CounterUsage>)` + `CounterUsage(UsageCounter, int)` consistent Tasks 10/11; `warningPercent:int` threaded through `UsageQueryImpl`/`UsageCommandImpl`/`UsageConfiguration` (Tasks 10/11/12); `LimitKind{PER_USER,DAILY,MONTHLY,CREDITS}` consistent Tasks 11/17 (rate uses the UI-only `?limit=RATE` param, not the enum).

## PR boundaries (ADR-019, ≤~400 line diffs)

- **PR A** = Phases 1–4 (the module rewrite lands atomically — removing `UsageTally` ripples through core + app, so the reactor is green only at Task 13). If A exceeds the size budget, split at the Phase 3/4 line only if Task 13's deletion is deferred to keep both halves compiling.
- **PR B** = Phase 5 (Resilience rate-limiter), fully independent.
