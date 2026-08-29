# RDBMS Mapper-Select Fan-out Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a test-time gate that fails an RDBMS `*IT` when a single repository method fires the same MyBatis mapper `@Select` more than once (the SQL-layer N+1), mirroring the in-memory `com.naturalist.test.query.nofanout` gate.

**Architecture:** A hybrid feeder writes into a per-test ThreadLocal tally, evaluated by the existing pure `SelectGate`. An AspectJ `@Around` (load-time woven, already wired in the root pom) marks each repository-method invocation as a *head*; a MyBatis `Interceptor` on `Executor.query` records each mapper select against the current head, keyed by mapped-statement id. `RdbmsTestExtension` arms the recorder before each test and evaluates the gate after. New code lives in a new subpackage `com.naturalist.persistence.test.nofanout`; the rule, annotation, and exception are reused from `framework-test`.

**Tech Stack:** Java (records, JSpecify), MyBatis 3.5.17, AspectJ 1.9.25 load-time weaving, JUnit 5, AssertJ, Maven.

## Global Constraints

- **Never weaken the gate to make a test pass.** A `RepeatedSelectException` reports a real N+1 in the code under test — fix the fan-out at the source (add/use a batched mapper method). Do NOT edit the recorder/aspect/interceptor/gate to stop counting, drop a test's `@RegisterExtension RdbmsTestExtension`, narrow the `aop.xml` weave scope, or blanket-`@AllowRepeatedSelect`. `@AllowRepeatedSelect` is a per-site last resort with a written justification. (Root `CLAUDE.md` non-negotiable.)
- **Never on a production classpath.** `aspectjweaver` stays test-scope (root dep); `aspectjrt` is compile-scope only for the `@Aspect` annotations. The MyBatis interceptor is registered only by `RdbmsTestExtension` (test infra), never by production `MyBatisSupport`.
- **No "test infra for test infra" beyond what's specified here.** The recorder gets a direct unit test (stateful carve-out); the interceptor a minimal no-DB unit test; the aspect an end-to-end weaving test. Real coverage comes from the domain `*IT` suites. Add nothing more.
- **Reuse, do not duplicate, the rule.** `SelectGate`, `@AllowRepeatedSelect`, `RepeatedSelectException` come from `com.naturalist.test.query.nofanout` (in `framework-test`, already a compile dep). Only the recorder is re-implemented (separate ThreadLocal, by design — see Task 1).
- **Version placeholders:** aspectjrt version is inherited from root `dependencyManagement` (no `<version>` tag in the module pom). assertj version via `${assertj-version}`. junit via `${junit-version}`.
- **Commit convention:** stage the change and print the commit command for the user to run; do not auto-commit unless the user says "commit". End every commit message with:
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

---

## File Structure

**New (main):**
- `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorder.java` — ThreadLocal head/select tally.
- `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptor.java` — MyBatis plugin feeding the recorder.
- `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/RepositoryHeadAspect.java` — AspectJ head marker.
- `kernels/persistence-test/src/main/resources/META-INF/aop.xml` — declares the aspect + weave scope.
- `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/CLAUDE.md` — package doc.

**New (test):**
- `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorderTest.java`
- `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptorTest.java`
- `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/Gadget.java` — minimal fake entity for the weaving test.
- `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/GadgetRepositoryRdbms.java` — minimal fake repository for the weaving test.
- `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/RepositoryHeadWeavingTest.java`

**Modified:**
- `kernels/persistence-test/pom.xml` — add `aspectjrt` (compile) and `assertj-core` (test).
- `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/RdbmsTestExtension.java` — arm/evaluate/disarm + register interceptor.
- `kernels/CLAUDE.md` — note the sibling gate in the persistence-test section.

---

## Task 1: MapperSelectRecorder + pom deps

**Files:**
- Modify: `kernels/persistence-test/pom.xml`
- Create: `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorder.java`
- Test: `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorderTest.java`

**Interfaces:**
- Produces: `MapperSelectRecorder` with static methods `arm()`, `disarm()`, `enterRepository(String headFqn)`, `exitRepository()`, `recordSelect(String statementId)`, `Map<String,Map<String,Integer>> snapshot()`. Per-outermost-invocation counting; `snapshot()` reflects only completed (exited) head invocations, folded via `Math.max`.

- [ ] **Step 1: Add the pom dependencies**

In `kernels/persistence-test/pom.xml`, inside `<dependencies>` (after the existing junit-jupiter entry), add:

```xml
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjrt</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj-version}</version>
            <scope>test</scope>
        </dependency>
```

(`aspectjweaver` is already present as a root-level test dependency — do not add it here. `mybatis` is already on the compile classpath transitively via `persistence` — do not add it.)

- [ ] **Step 2: Write the failing test**

Create `MapperSelectRecorderTest.java`:

```java
package com.naturalist.persistence.test.nofanout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSelectRecorderTest {

    @BeforeEach
    void arm() {
        MapperSelectRecorder.arm();
    }

    @AfterEach
    void disarm() {
        MapperSelectRecorder.disarm();
    }

    @Test
    void repeatedSelectInOneHeadTalliesTwo() {
        MapperSelectRecorder.enterRepository("Repo.getByNameSet");
        MapperSelectRecorder.recordSelect("Mapper.selectById");
        MapperSelectRecorder.recordSelect("Mapper.selectById");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getByNameSet", Map.of("Mapper.selectById", 2));
    }

    @Test
    void repeatedInvocationsEachOneSelectFoldToOne() {
        for (int i = 0; i < 3; i++) {
            MapperSelectRecorder.enterRepository("Repo.getByName");
            MapperSelectRecorder.recordSelect("Mapper.selectById");
            MapperSelectRecorder.exitRepository();
        }

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getByName", Map.of("Mapper.selectById", 1));
    }

    @Test
    void twoDistinctSelectsInOneHeadAreEachCountOne() {
        MapperSelectRecorder.enterRepository("Repo.getPage");
        MapperSelectRecorder.recordSelect("Mapper.selectPage");
        MapperSelectRecorder.recordSelect("Mapper.countInWindow");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("Repo.getPage", Map.of("Mapper.selectPage", 1, "Mapper.countInWindow", 1));
    }

    @Test
    void recordWhenDisarmedIsIgnored() {
        MapperSelectRecorder.disarm(); // fresh scope, armed == false
        MapperSelectRecorder.enterRepository("Repo.x");
        MapperSelectRecorder.recordSelect("Mapper.y");
        MapperSelectRecorder.exitRepository();

        assertThat(MapperSelectRecorder.snapshot()).isEmpty();
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=MapperSelectRecorderTest`
Expected: FAIL — compilation error, `MapperSelectRecorder` does not exist.

- [ ] **Step 4: Write the implementation**

Create `MapperSelectRecorder.java`:

```java
package com.naturalist.persistence.test.nofanout;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local recorder for the RDBMS mapper-select fan-out gate — the sibling of
 * {@code com.naturalist.test.query.nofanout.SelectCountRecorder}. {@link RepositoryHeadAspect}
 * pushes/pops repository-method heads; {@link MapperSelectInterceptor} records one select per
 * MyBatis {@code Executor.query} against the current head; {@code RdbmsTestExtension} arms it
 * before each test and evaluates its {@link #snapshot()} after.
 *
 * <p><b>Gating is per repository-method INVOCATION, not per-statement-across-the-test.</b>
 * Selects fold into the persistent {@code tally} via {@link Math#max} only when the outermost
 * head exits, so a test that calls one repository method N times (each doing one select) reads
 * as count 1 — only a <i>single</i> invocation looping a select {@code >1×} is an N+1.
 *
 * <p>Deliberately a <b>separate</b> ThreadLocal from the in-memory gate's recorder: the
 * in-memory {@code SelectCountAspect} also weaves the {@code *Rdbms} classes but is never armed
 * during an RDBMS {@code *IT}, so keeping the recorders apart prevents any cross-talk.
 */
public final class MapperSelectRecorder {

    private static final class Scope {
        boolean armed;
        final Deque<String> heads = new ArrayDeque<>();
        /** Per-outermost-invocation counts; reset when a new outermost head is entered. */
        final Map<String, Map<String, Integer>> current = new LinkedHashMap<>();
        /** Persistent per-test tally; folds {@code current} via MAX at each outermost exit. */
        final Map<String, Map<String, Integer>> tally = new LinkedHashMap<>();
    }

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private MapperSelectRecorder() {
    }

    public static void arm() {
        Scope s = SCOPE.get();
        s.armed = true;
        s.heads.clear();
        s.current.clear();
        s.tally.clear();
    }

    public static void disarm() {
        SCOPE.remove();
    }

    /** Push a repository-method head. A new outermost head starts a fresh per-invocation count. */
    public static void enterRepository(String headFqn) {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            s.current.clear();
        }
        s.heads.push(headFqn);
    }

    public static void exitRepository() {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            return;
        }
        s.heads.pop();
        if (s.heads.isEmpty()) {
            s.current.forEach((head, selects) -> {
                Map<String, Integer> agg = s.tally.computeIfAbsent(head, k -> new LinkedHashMap<>());
                selects.forEach((sel, cnt) -> agg.merge(sel, cnt, Math::max));
            });
            s.current.clear();
        }
    }

    /** Record one mapper select against the outermost enclosing head, when armed. */
    public static void recordSelect(String statementId) {
        Scope s = SCOPE.get();
        if (!s.armed || s.heads.isEmpty()) {
            return;
        }
        String head = s.heads.peekLast(); // outermost = bottom of the stack
        s.current.computeIfAbsent(head, k -> new LinkedHashMap<>())
                .merge(statementId, 1, Integer::sum);
    }

    /** Defensive copy of the tally: head FQN -> (mapped-statement id -> max-per-invocation count). */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        SCOPE.get().tally.forEach((head, selects) -> copy.put(head, new LinkedHashMap<>(selects)));
        return copy;
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=MapperSelectRecorderTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Stage and print the commit command**

```bash
git add kernels/persistence-test/pom.xml \
        kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorder.java \
        kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectRecorderTest.java
git commit -m "feat(persistence-test): add MapperSelectRecorder for rdbms fan-out gate

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: MapperSelectInterceptor

**Files:**
- Create: `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptor.java`
- Test: `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptorTest.java`

**Interfaces:**
- Consumes: `MapperSelectRecorder.recordSelect(String)`, `enterRepository`, `exitRepository`, `arm`, `disarm`, `snapshot` (Task 1).
- Produces: `MapperSelectInterceptor implements org.apache.ibatis.plugin.Interceptor` — `@Intercepts` on `Executor.query(MappedStatement, Object, RowBounds, ResultHandler)`; records `MappedStatement.getId()` then proceeds. Registered via `Configuration.addInterceptor(new MapperSelectInterceptor())` (done in Task 4).

- [ ] **Step 1: Write the failing test**

Create `MapperSelectInterceptorTest.java`:

```java
package com.naturalist.persistence.test.nofanout;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSelectInterceptorTest {

    @Test
    void recordsStatementIdAgainstCurrentHeadAndProceeds() throws Throwable {
        Configuration cfg = new Configuration();
        SqlSource src = new StaticSqlSource(cfg, "select 1");
        MappedStatement ms = new MappedStatement
                .Builder(cfg, "com.example.FooMapper.selectThing", src, SqlCommandType.SELECT)
                .build();

        Executor target = (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class[]{Executor.class},
                (proxy, method, args) -> "query".equals(method.getName()) ? List.of("row") : null);

        Method query = Executor.class.getMethod(
                "query", MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class);
        Invocation invocation = new Invocation(
                target, query, new Object[]{ms, null, RowBounds.DEFAULT, null});

        MapperSelectRecorder.arm();
        MapperSelectRecorder.enterRepository("TestHead");
        try {
            Object result = new MapperSelectInterceptor().intercept(invocation);
            assertThat(result).isEqualTo(List.of("row"));
        } finally {
            MapperSelectRecorder.exitRepository();
        }

        assertThat(MapperSelectRecorder.snapshot())
                .containsEntry("TestHead", Map.of("com.example.FooMapper.selectThing", 1));
        MapperSelectRecorder.disarm();
    }
}
```

Note: `org.apache.ibatis.builder.StaticSqlSource` is `public`. If the `MappedStatement.Builder` chain ever rejects an empty result-map at `build()` on this MyBatis version, append `.resultMaps(java.util.List.of())` before `.build()` — but 3.5.17 defaults it, so the code above should build as written.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=MapperSelectInterceptorTest`
Expected: FAIL — compilation error, `MapperSelectInterceptor` does not exist.

- [ ] **Step 3: Write the implementation**

Create `MapperSelectInterceptor.java`:

```java
package com.naturalist.persistence.test.nofanout;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis plugin that feeds {@link MapperSelectRecorder} — the select-side feeder of the RDBMS
 * mapper-select fan-out gate. It intercepts {@code Executor.query} (selects only; inserts and
 * updates route through {@code Executor.update} and are never seen), records one select against
 * the current repository head keyed by {@link MappedStatement#getId()}
 * (e.g. {@code com.naturalist.usage.UsageEventMapper.selectById}), then proceeds unchanged.
 *
 * <p>Never evaluates and never throws — the rule lives in
 * {@code com.naturalist.test.query.nofanout.SelectGate}, invoked from {@code RdbmsTestExtension}.
 * Registered only by that test extension via {@code Configuration.addInterceptor}; never on a
 * production {@code SqlSessionFactory}.
 */
@Intercepts(@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public final class MapperSelectInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        MapperSelectRecorder.recordSelect(statement.getId());
        return invocation.proceed();
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=MapperSelectInterceptorTest`
Expected: PASS (1 test).

- [ ] **Step 5: Stage and print the commit command**

```bash
git add kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptor.java \
        kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/MapperSelectInterceptorTest.java
git commit -m "feat(persistence-test): add MapperSelectInterceptor select feeder

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: RepositoryHeadAspect + aop.xml + weaving proof

**Files:**
- Create: `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/RepositoryHeadAspect.java`
- Create: `kernels/persistence-test/src/main/resources/META-INF/aop.xml`
- Create: `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/Gadget.java`
- Create: `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/GadgetRepositoryRdbms.java`
- Test: `kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/RepositoryHeadWeavingTest.java`

**Interfaces:**
- Consumes: `MapperSelectRecorder` (Task 1); `com.naturalist.data.AbstractEntityRepository<NAME,ENTITY>`, `com.naturalist.ddd.Named<KEY>`; reused `com.naturalist.test.query.nofanout.{SelectGate, AllowRepeatedSelect, RepeatedSelectException}`.
- Produces: `RepositoryHeadAspect` — `@Around("execution(public * com.naturalist.data.AbstractEntityRepository+.*(..))")` calling `enterRepository`/`exitRepository` with head FQN `target-class-name + "." + method-name`.

- [ ] **Step 1: Write the failing weaving test and its fixtures**

Create `Gadget.java`:

```java
package com.naturalist.persistence.test.nofanout;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/** Minimal fake entity for the weaving proof — never persisted. */
record Gadget(String key) implements Named<String> {
    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> {
        };
    }
}
```

Create `GadgetRepositoryRdbms.java` (top-level so its name matches the `com.naturalist..*Rdbms` weave include unambiguously):

```java
package com.naturalist.persistence.test.nofanout;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Minimal fake repository for {@link RepositoryHeadWeavingTest}. Its public methods stand in for
 * real repository operations so {@link RepositoryHeadAspect} weaves a head around them; the bodies
 * call {@link MapperSelectRecorder#recordSelect} directly, standing in for
 * {@link MapperSelectInterceptor} (which needs a live MyBatis {@code Executor}). The {@code do*}
 * hooks are never invoked.
 */
final class GadgetRepositoryRdbms extends AbstractEntityRepository<String, Gadget> {

    static final String SELECT = "com.naturalist.persistence.test.nofanout.GadgetMapper.selectById";

    @Override
    protected Optional<Gadget> doGetByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<Gadget> doGetByNameSet(Set<String> nameSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Page<Gadget> doGetPage(PageRequest pageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doInsert(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doUpdate(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Gadget doSave(Gadget entity) {
        throw new UnsupportedOperationException();
    }

    /** One select per element — the N+1 shape the gate must catch. */
    public List<Gadget> fannedOut(Set<String> ids) {
        for (String id : ids) {
            MapperSelectRecorder.recordSelect(SELECT);
        }
        return List.of();
    }

    /** One select for the whole set — the correct batched shape. */
    public List<Gadget> batched(Set<String> ids) {
        MapperSelectRecorder.recordSelect(SELECT);
        return List.of();
    }
}
```

Create `RepositoryHeadWeavingTest.java`:

```java
package com.naturalist.persistence.test.nofanout;

import com.naturalist.test.query.nofanout.AllowRepeatedSelect;
import com.naturalist.test.query.nofanout.RepeatedSelectException;
import com.naturalist.test.query.nofanout.SelectGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end proof that {@link RepositoryHeadAspect} is load-time woven around repository methods
 * and scopes mapper selects per head. If weaving does not happen, no head is pushed,
 * {@code recordSelect} early-returns, and {@code loopedSelectIsFlagged} fails to throw — so this
 * test also guards the aspect wiring itself.
 */
class RepositoryHeadWeavingTest {

    private final GadgetRepositoryRdbms repo = new GadgetRepositoryRdbms();

    @BeforeEach
    void arm() {
        MapperSelectRecorder.arm();
    }

    @AfterEach
    void disarm() {
        MapperSelectRecorder.disarm();
    }

    @Test
    void loopedSelectIsFlagged() {
        repo.fannedOut(Set.of("a", "b", "c"));

        assertThatThrownBy(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), List.of()))
                .isInstanceOf(RepeatedSelectException.class)
                .hasMessageContaining(GadgetRepositoryRdbms.SELECT);
    }

    @Test
    void batchedSelectPasses() {
        repo.batched(Set.of("a", "b", "c"));

        assertThatCode(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @AllowRepeatedSelect(query = "fannedOut", select = "selectById")
    void allowlistedRepeatIsSuppressed(TestInfo info) {
        repo.fannedOut(Set.of("a", "b"));

        List<AllowRepeatedSelect> allowlist = Arrays.asList(
                info.getTestMethod().orElseThrow().getAnnotationsByType(AllowRepeatedSelect.class));

        assertThatCode(() -> SelectGate.evaluate(MapperSelectRecorder.snapshot(), allowlist))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=RepositoryHeadWeavingTest`
Expected: FAIL — compilation error, `RepositoryHeadAspect` does not exist (and no `aop.xml`).

- [ ] **Step 3: Write the aspect**

Create `RepositoryHeadAspect.java`:

```java
package com.naturalist.persistence.test.nofanout;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Load-time-woven head marker for the RDBMS mapper-select fan-out gate — the sibling of
 * {@code com.naturalist.test.query.nofanout.SelectCountAspect}, but keyed on repository methods
 * (the RDBMS {@code *IT} suites exercise repositories directly, with no {@code *QueryImpl} on the
 * stack). It wraps every public method on an {@link com.naturalist.data.AbstractEntityRepository}
 * subtype, pushing/popping the head so {@link MapperSelectInterceptor}'s selects attribute to the
 * right invocation. Never evaluates, never throws.
 *
 * <p>The pointcut targets {@code AbstractEntityRepository+} because the standard operations
 * ({@code getByName}, {@code getByEntityNameSet}, {@code getPage}, {@code insert}, {@code update},
 * {@code save}) are {@code final} on the base class — where a batched-vs-looped fan-out actually
 * hides — while domain-specific selects are declared on the concrete {@code *Rdbms} subclass.
 * Applied only under test (see {@code META-INF/aop.xml}); never on a production classpath.
 */
@Aspect
public class RepositoryHeadAspect {

    @Around("execution(public * com.naturalist.data.AbstractEntityRepository+.*(..))")
    public Object aroundRepository(ProceedingJoinPoint pjp) throws Throwable {
        MapperSelectRecorder.enterRepository(fqn(pjp));
        try {
            return pjp.proceed();
        } finally {
            MapperSelectRecorder.exitRepository();
        }
    }

    private static String fqn(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getName() + "." + pjp.getSignature().getName();
    }
}
```

- [ ] **Step 4: Write the aop.xml**

Create `kernels/persistence-test/src/main/resources/META-INF/aop.xml`:

```xml
<!DOCTYPE aspectj PUBLIC "-//AspectJ//DTD//EN" "https://www.eclipse.org/aspectj/dtd/aspectj.dtd">
<aspectj>
    <aspects>
        <aspect name="com.naturalist.persistence.test.nofanout.RepositoryHeadAspect"/>
    </aspects>
    <weaver options="-Xlint:ignore">
        <include within="com.naturalist.data.AbstractEntityRepository"/>
        <include within="com.naturalist..*Rdbms"/>
        <include within="com.naturalist.persistence.test.nofanout..*"/>
    </weaver>
</aspectj>
```

Notes:
- `com.naturalist.data.AbstractEntityRepository` must be woven so the inherited `final` methods (`getByName`, `getByEntityNameSet`, `getPage`, `insert`, `update`, `save`) get the head advice at their declaring class.
- `com.naturalist..*Rdbms` weaves the concrete adapters for their own public methods (e.g. `findByCounterSince`).
- `com.naturalist.persistence.test.nofanout..*` covers this package's own types, including the weaving test's `GadgetRepositoryRdbms`.
- AspectJ merges every `META-INF/aop.xml` on the classpath, so this composes with `framework-test`'s aop.xml. This aop.xml is present only where `persistence-test` is a dependency (the rdbms IT modules), so the `AbstractEntityRepository` weave is scoped to those modules.

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -pl kernels/persistence-test test -Dtest=RepositoryHeadWeavingTest`
Expected: PASS (3 tests). If `loopedSelectIsFlagged` fails with "no exception thrown", the aspect did not weave — check the `aop.xml` weave includes and that the root `-javaagent:aspectjweaver` argLine is active (it is a root dependency).

- [ ] **Step 6: Stage and print the commit command**

```bash
git add kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/RepositoryHeadAspect.java \
        kernels/persistence-test/src/main/resources/META-INF/aop.xml \
        kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/Gadget.java \
        kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/GadgetRepositoryRdbms.java \
        kernels/persistence-test/src/test/java/com/naturalist/persistence/test/nofanout/RepositoryHeadWeavingTest.java
git commit -m "feat(persistence-test): weave repository heads for rdbms fan-out gate

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Wire the gate into RdbmsTestExtension

**Files:**
- Modify: `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/RdbmsTestExtension.java`

**Interfaces:**
- Consumes: `MapperSelectRecorder`, `MapperSelectInterceptor` (Tasks 1–3, same parent package via the `nofanout` subpackage); `com.naturalist.test.query.nofanout.{SelectGate, AllowRepeatedSelect}`.
- Produces: the running gate — every test registering `@RegisterExtension RdbmsTestExtension` is armed in `beforeEach` and evaluated in `afterEach`. No new public method.

This task has no isolated unit test — its behavior is the composition of Tasks 1–3 (already tested) plus MyBatis registration, and is exercised end-to-end by the domain `*IT` suites in Task 6. The step is a careful edit followed by a compile + full-module test run.

- [ ] **Step 1: Add imports**

In `RdbmsTestExtension.java`, add to the import block:

```java
import com.naturalist.persistence.test.nofanout.MapperSelectInterceptor;
import com.naturalist.persistence.test.nofanout.MapperSelectRecorder;
import com.naturalist.test.query.nofanout.AllowRepeatedSelect;
import com.naturalist.test.query.nofanout.SelectGate;

import java.util.Arrays;
```

- [ ] **Step 2: Register the interceptor on the factory**

Immediately after the existing `FACTORY` field declaration, add a static initializer:

```java
    private static final SqlSessionFactory FACTORY =
            MyBatisSupport.sessionFactory(RdbmsDataSource.shared());

    static {
        // Test-only: feeds the mapper-select fan-out gate. Never registered by production MyBatisSupport.
        FACTORY.getConfiguration().addInterceptor(new MapperSelectInterceptor());
    }
```

- [ ] **Step 3: Arm the recorder in beforeEach**

Change `beforeEach` from:

```java
    @Override
    public void beforeEach(ExtensionContext context) {
        current.set(FACTORY.openSession(false)); // autocommit off
    }
```

to:

```java
    @Override
    public void beforeEach(ExtensionContext context) {
        current.set(FACTORY.openSession(false)); // autocommit off
        MapperSelectRecorder.arm();
    }
```

- [ ] **Step 4: Evaluate the gate in afterEach**

Change `afterEach` from:

```java
    @Override
    public void afterEach(ExtensionContext context) {
        SqlSession session = current.get();
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
                current.remove();
            }
        }
    }
```

to:

```java
    @Override
    public void afterEach(ExtensionContext context) {
        try {
            AllowRepeatedSelect[] allowlist =
                    context.getRequiredTestMethod().getAnnotationsByType(AllowRepeatedSelect.class);
            SelectGate.evaluate(MapperSelectRecorder.snapshot(), Arrays.asList(allowlist));
        } finally {
            MapperSelectRecorder.disarm();
            SqlSession session = current.get();
            if (session != null) {
                try {
                    session.rollback();
                } finally {
                    session.close();
                    current.remove();
                }
            }
        }
    }
```

The `finally` guarantees the DB session is rolled back and closed and the recorder disarmed even when the gate throws `RepeatedSelectException`.

- [ ] **Step 5: Compile and re-run the module's own tests**

Run: `mvn -q -pl kernels/persistence-test test`
Expected: PASS — all Task 1–3 tests still green (no regression from the wiring edit). This step compiles the modified extension; the wiring's real exercise is Task 6.

- [ ] **Step 6: Stage and print the commit command**

```bash
git add kernels/persistence-test/src/main/java/com/naturalist/persistence/test/RdbmsTestExtension.java
git commit -m "feat(persistence-test): arm rdbms mapper-select gate in RdbmsTestExtension

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Documentation

**Files:**
- Create: `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/CLAUDE.md`
- Modify: `kernels/CLAUDE.md`

**Interfaces:** none (docs only).

- [ ] **Step 1: Write the package CLAUDE.md**

Create `kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/CLAUDE.md`:

```markdown
# `com.naturalist.persistence.test.nofanout` — the RDBMS mapper-select fan-out gate

A **test-time gate** that fails an RDBMS `*IT` when a single repository method resolves the
same MyBatis mapper `@Select` more than once — the SQL-layer N+1 (`domains/CLAUDE.md`:
"Fan-out must batch — never per-element in a loop"). It is the sibling of the in-memory gate
in `com.naturalist.test.query.nofanout` (framework-test): that one keys on `*QueryImpl` heads
and in-memory repository selects; this one keys on **repository-method heads** and **MyBatis
mapped-statement executions**. **Test-only** — never on a production classpath.

## How it works — hybrid feeder, reused rule

```
RepositoryHeadAspect  ──enter/exit──▶  MapperSelectRecorder  ──snapshot──▶  SelectGate
  (AspectJ LTW head)                     (ThreadLocal, per-invocation)      (framework-test rule)
MapperSelectInterceptor ──recordSelect──▶       ▲                                 ▲
  (MyBatis Executor.query)                RdbmsTestExtension arms (beforeEach) / evaluates (afterEach)
```

- **`RepositoryHeadAspect`** (load-time woven) — `@Around` on every public method of an
  `AbstractEntityRepository` subtype, pushing/popping the repository-method head. Mapper
  methods run through a MyBatis JDK proxy that AspectJ `execution()` cannot weave, so the
  select side is a MyBatis plugin, not an aspect.
- **`MapperSelectInterceptor`** (MyBatis `@Intercepts` on `Executor.query`) — records one
  select against the current head, keyed by `MappedStatement.getId()`. Only selects — writes
  route through `Executor.update`. Registered by `RdbmsTestExtension`, never by production
  `MyBatisSupport`.
- **`MapperSelectRecorder`** (ThreadLocal) — counts selects **per repository-method
  INVOCATION**, folding via `Math.max` at head exit, so a test calling one method N times
  (each one select) reads as 1; only a single invocation looping a select is an N+1. A
  **separate** ThreadLocal from the in-memory recorder, so the in-memory select aspect (which
  also weaves the `*Rdbms` classes but is never armed here) cannot cross-talk.
- **Rule reused from framework-test** — `SelectGate` (count > 1 → throw), `@AllowRepeatedSelect`
  (per-site escape hatch; `query` matches the repository-method head, `select` the statement id),
  `RepeatedSelectException`.

## Using it

- Nothing to call — register `@RegisterExtension RdbmsTestExtension` (every rdbms `*IT` already
  does) and the gate is live.
- **Escape hatch:** `@AllowRepeatedSelect(query = "<repo method>", select = "<mapper statement>")`
  on the specific test method (repeatable), with a one-line comment saying why it cannot batch.
  Match by exact FQN, `.`-suffix, or simple name.

## ⛔ Do NOT weaken this gate to make a test pass

Root-`CLAUDE.md` non-negotiable. A `RepeatedSelectException` reports a real N+1 in the code
under test — fix the fan-out (add/use a batched mapper method: `selectByIdSet`,
`findByNameSet`, …). Do **not** edit the recorder/aspect/interceptor/gate to stop counting,
drop a test's `@RegisterExtension RdbmsTestExtension`, narrow the `aop.xml` weave scope, or
blanket-`@AllowRepeatedSelect`. If you believe the gate is wrong, raise it with the human.

## Constraints / gotchas

- **Never on a production classpath.** `aspectjweaver` is a root test-scope dep; `aspectjrt` is
  compile-scope only for the `@Aspect`. The interceptor is registered solely by
  `RdbmsTestExtension`. If you move/rename the aspect, update `META-INF/aop.xml` and reinstall
  `persistence-test` (downstream rdbms modules weave from the installed jar).
- **Proof tests** (no DB): `MapperSelectRecorderTest` (stateful), `MapperSelectInterceptorTest`
  (id extraction), `RepositoryHeadWeavingTest` (weaving + head scoping). Real end-to-end
  coverage is the domain `*IT` suites against the standing Postgres. Per the framework-test
  "no test infra for test infra" rule, do not add more scaffolding.
```

- [ ] **Step 2: Note the sibling gate in kernels/CLAUDE.md**

In `kernels/CLAUDE.md`, in the `### framework-test` section, immediately after the paragraph that ends "See `docs/plans/2026-08-21-n-plus-one-select-gate-plan.md`." and before the "A static backstop `NoSelectInIteration`…" sentence, insert:

```markdown
An RDBMS sibling of this gate lives in `kernels/persistence-test`
(`com.naturalist.persistence.test.nofanout`): `RepositoryHeadAspect` (AspectJ head marker on
`AbstractEntityRepository+`) + `MapperSelectInterceptor` (MyBatis `Executor.query` plugin) →
`MapperSelectRecorder` → the same reused `SelectGate`. It is **wired and ARMED** in
`RdbmsTestExtension` and fails an rdbms `*IT` when one repository method fires the same mapper
`@Select` more than once. Same escape hatch (`@AllowRepeatedSelect`), same non-negotiable
(never weaken it). See `docs/plans/2026-08-29-rdbms-mapper-select-fanout-gate-design.md`.
```

- [ ] **Step 3: Stage and print the commit command**

```bash
git add kernels/persistence-test/src/main/java/com/naturalist/persistence/test/nofanout/CLAUDE.md \
        kernels/CLAUDE.md
git commit -m "docs(persistence-test): document rdbms mapper-select fan-out gate

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: Full verification

**Files:** none (verification only).

**Interfaces:** none.

- [ ] **Step 1: Clean install so downstream modules weave from the fresh jar**

Because kernel signature/resource changes are consumed from `~/.m2`, install without tests first:

Run: `mvn -q install -DskipTests -pl kernels/persistence-test -am`
Expected: BUILD SUCCESS. (Installs the new `aop.xml` + gate classes so the rdbms IT modules pick them up.)

- [ ] **Step 2: Run the persistence-test proof tests**

Run: `mvn -q -pl kernels/persistence-test test`
Expected: PASS — `MapperSelectRecorderTest`, `MapperSelectInterceptorTest`, `RepositoryHeadWeavingTest` all green. These need no database.

- [ ] **Step 3: Run one domain's rdbms ITs against the standing Postgres**

This step needs a standing Postgres seeded by `apps/test-db-seeder` (env: `NATURALIST_TEST_JDBC_URL`, `NATURALIST_TEST_DB_USER`, `NATURALIST_TEST_DB_PASSWORD`, defaulting to `jdbc:postgresql://localhost:5432/naturalist_test` / `postgres` / `postgres`).

First check whether Postgres is reachable:

Run: `pg_isready -h localhost -p 5432 || echo "NO POSTGRES"`

- If reachable: seed then run the usage rdbms ITs (smallest, flat surrogate-UUID entities — the clearest first target):
  - Run: `mvn -q -pl apps/test-db-seeder -am install -DskipTests` then run the seeder per its module README, then
  - Run: `mvn -q -pl domains/usage/usage-repository-rdbms verify`
  - Expected: BUILD SUCCESS with the gate armed. A `RepeatedSelectException` here is a **real pre-existing fan-out** in that `*Rdbms` adapter — fix it at the source (batch the mapper call); do not weaken the gate. Report any that surface.
- If NOT reachable: **stop and report honestly** that the domain ITs could not run without a standing Postgres, that the three no-DB proof tests passed, and ask the user whether to bring up the DB (or defer the IT run to them). Do not claim the ITs passed.

- [ ] **Step 4: Run the architectural-enforcement completeness gate**

Run: `mvn -q install -DskipTests && mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS with no pending markers.

- [ ] **Step 5: Report**

Summarize exactly what ran: the three proof tests (green), whether the domain ITs ran (and against what DB) or were deferred, and the completeness gate result. If any rdbms adapter surfaced a real fan-out, list it with the batched fix applied (or flagged for the user).

---

## Self-Review

**Spec coverage:**
- Problem / what-it-catches → Tasks 1 (recorder semantics), 3 (weaving proof of the head-scoped count).
- Hybrid mechanism (AspectJ head + MyBatis interceptor) → Tasks 3 (aspect + aop.xml) and 2 (interceptor).
- Per-invocation `Math.max` folding → Task 1 (`MapperSelectRecorder` + `repeatedInvocationsEachOneSelectFoldToOne`).
- Separate ThreadLocal / no cross-talk → Task 1 (recorder javadoc + design); enforced by using a distinct recorder class.
- Reused `SelectGate` / `@AllowRepeatedSelect` / `RepeatedSelectException` → Tasks 3 (weaving test uses all three) and 4 (afterEach evaluation).
- `RdbmsTestExtension` wiring (arm/afterEach/disarm + `addInterceptor`, no `MyBatisSupport` change) → Task 4.
- aop.xml weave scope incl. `AbstractEntityRepository` + `*Rdbms` → Task 3 Step 4.
- pom deps (`aspectjrt` compile, `assertj-core` test; not `aspectjweaver`/`mybatis`) → Task 1 Step 1.
- Testing strategy (recorder/interceptor/weaving proofs, no-DB) → Tasks 1–3; domain ITs → Task 6.
- Docs (package CLAUDE.md + kernels/CLAUDE.md) → Task 5.
- Verification caveat (standing Postgres) → Task 6 Step 3.

**Placeholder scan:** none — every code and doc block is complete literal content.

**Type consistency:** `MapperSelectRecorder` method names (`arm`/`disarm`/`enterRepository`/`exitRepository`/`recordSelect`/`snapshot`) are used identically in Tasks 1–4. `GadgetRepositoryRdbms.SELECT` referenced consistently in Task 3. `SelectGate.evaluate(Map, Collection<AllowRepeatedSelect>)` signature matches the reused framework-test class (verified: `evaluate(Map<String,Map<String,Integer>>, Collection<AllowRepeatedSelect>)`). `@AllowRepeatedSelect(query=…, select=…)` matches the reused annotation's attributes. `Executor.query(MappedStatement, Object, RowBounds, ResultHandler)` matches the MyBatis 3.5.17 signature (verified via javap).
