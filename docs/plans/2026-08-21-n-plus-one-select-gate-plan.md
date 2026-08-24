# N+1 Select Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A build-time test gate that fails the suite when a head-of-DAG query fans out into repeated repository selects (the N+1 pattern), using AspectJ load-time weaving to count selects and the JUnit `NaturalistTestExtension` lifecycle to evaluate the rule.

**Architecture:** A test-only AspectJ aspect maintains a per-thread head-of-DAG query stack and records each repository select into a `ThreadLocal` tally, keyed by the runtime class of the query (head) and the runtime class of the repository (select). The `NaturalistTestExtension` arms the recorder in `beforeEach` and, in `afterEach`, applies the rule — any select called more than once within one head is a violation — unless whitelisted by `@AllowRepeatedSelect`. The aspect is applied via a `-javaagent` load-time weaver wired once in the root POM's surefire config; nothing touches production artifacts.

**Tech Stack:** Java 25, Maven (surefire, maven-dependency-plugin), AspectJ 1.9.x load-time weaving, JUnit 5 (Jupiter) extensions, AssertJ.

**Source design:** [docs/plans/2026-08-21-n-plus-one-select-gate-design.md](2026-08-21-n-plus-one-select-gate-design.md)

## Global Constraints

- **No Spring, no AspectJ, in core or the kernel's production classpath.** The `framework` kernel keeps its dependency budget (Jackson, Commons, Micrometer, JSpecify). All new dependencies (`aspectjweaver`, `aspectjrt`) are **test scope only**.
- **All new instrument code lives in `kernels/framework-test`** (already every test module's dependency; already owns the extension). Package: `com.naturalist.data.count`.
- **Typed identifiers / record conventions** from [domains/CLAUDE.md](../../domains/CLAUDE.md) do not apply to this instrument (it is test infrastructure, not a domain type), but its own state classes must be `final` and its statics thread-safe.
- **AspectJ version:** use the latest release that supports `--release 25` (start with `1.9.25`; if load-time weaving warns about class-file format, bump to the newest available and re-verify).
- **Git:** print commands; commit only when the user says so. Each task's commit step shows the command; do not run it unprompted. Trunk-based — branch, do not open a PR unless asked.

---

## File Structure

- `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectCountRecorder.java` — thread-local head stack + tally; static feeder API. **(Task 2)**
- `kernels/framework-test/src/main/java/com/naturalist/data/count/RepeatedSelectException.java` — thrown on violation; extends `AssertionError`. **(Task 2)**
- `kernels/framework-test/src/main/java/com/naturalist/data/count/AllowRepeatedSelect.java` — repeatable method annotation + `List` container. **(Task 2)**
- `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectGate.java` — pure evaluation of a tally snapshot against the rule + allowlist. **(Task 2)**
- `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectCountAspect.java` — `@Aspect`; two pointcuts feeding the recorder. **(Task 3)**
- `kernels/framework-test/src/main/resources/META-INF/aop.xml` — declares the aspect + weave scope. **(Task 3)**
- `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistTestExtension.java` — renamed from `NaturalistDatabaseExtension`; gains arm/evaluate. **(Task 1 rename, Task 4 wire)**
- `pom.xml` (root) — `aspectj.version`, dependencyManagement entries, universal test-scope `aspectjweaver`, maven-dependency-plugin `properties` goal, surefire `argLine`. **(Task 3)**
- `kernels/framework-test/pom.xml` — `aspectjrt` (compile) + `aspectjweaver` (test). **(Task 3)**
- Test fixtures + tests in `kernels/framework-test/src/test/java/com/naturalist/data/count/`. **(Tasks 2–4)**

---

## Task 1: Rename `NaturalistDatabaseExtension` → `NaturalistTestExtension`

Mechanical rename landed first so the counter (Task 4) builds on the final name. 49 files reference the type.

**Files:**
- Rename: `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistDatabaseExtension.java` → `NaturalistTestExtension.java`
- Modify: the 49 referencing files (class name, `create()` call sites, field types, imports)

**Interfaces:**
- Produces: `com.naturalist.data.NaturalistTestExtension` with unchanged surface — `static NaturalistTestExtension create()`, `extends NaturalistDatabase implements BeforeEachCallback`, `beforeEach` still calls `clear()`.

- [ ] **Step 1: Enumerate references (pre-check)**

Run:
```bash
grep -rln "NaturalistDatabaseExtension" . | grep -v /target/ | grep '\.java$'
```
Expected: 49 files (1 definition + 48 consumers).

- [ ] **Step 2: Rename the class and file**

Rename the file to `NaturalistTestExtension.java`; inside, rename the type, the private constructor, and the `create()` return type:

```java
public class NaturalistTestExtension extends NaturalistDatabase implements BeforeEachCallback {

    private NaturalistTestExtension() {
        super();
    }

    public static NaturalistTestExtension create() {
        return new NaturalistTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
    }
}
```
Update the class javadoc's `new InsectSpeciesRepositoryMock(db)` example comment to keep referring to the field; the `NaturalistDatabaseExtension` name in prose becomes `NaturalistTestExtension`.

- [ ] **Step 3: Update all 48 call sites**

Replace every `NaturalistDatabaseExtension` token with `NaturalistTestExtension` across the referencing files (type in `@RegisterExtension` fields, `NaturalistDatabaseExtension.create()`, imports). Prefer the IDE "Rename Symbol" refactor; otherwise:
```bash
grep -rl "NaturalistDatabaseExtension" . | grep -v /target/ | grep '\.java$' \
  | xargs sed -i '' 's/NaturalistDatabaseExtension/NaturalistTestExtension/g'
```

- [ ] **Step 4: Verify no references remain**

Run:
```bash
grep -rn "NaturalistDatabaseExtension" . | grep -v /target/
```
Expected: no output.

- [ ] **Step 5: Build to confirm the rename compiles clean**

Run:
```bash
mvn -q -pl kernels/framework-test -am test-compile
```
Expected: BUILD SUCCESS. (Full `mvn verify` happens in Task 4; this proves the rename mechanically.)

- [ ] **Step 6: Commit**

```bash
git commit -am "refactor(framework-test): rename NaturalistDatabaseExtension to NaturalistTestExtension

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Pure-Java counting core (recorder, exception, annotation, gate)

The stateful thread-local engine and the rule evaluation — no AspectJ yet. Directly unit-tested: this is the "stateful/lifecycle behaviour" carve-out that the [kernel testing convention](../../kernels/CLAUDE.md) explicitly says warrants a direct test, so it is *not* the forbidden "test infra for test infra."

**Files:**
- Create: `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectCountRecorder.java`
- Create: `kernels/framework-test/src/main/java/com/naturalist/data/count/RepeatedSelectException.java`
- Create: `kernels/framework-test/src/main/java/com/naturalist/data/count/AllowRepeatedSelect.java`
- Create: `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectGate.java`
- Test: `kernels/framework-test/src/test/java/com/naturalist/data/count/SelectCountRecorderTest.java`
- Test: `kernels/framework-test/src/test/java/com/naturalist/data/count/SelectGateTest.java`

**Interfaces:**
- Produces:
  - `SelectCountRecorder.arm()`, `.disarm()`, `.enterQuery(String headFqn)`, `.exitQuery()`, `.recordSelect(String selectFqn)`, `Map<String,Map<String,Integer>> snapshot()` — all `static`, thread-local backed. `recordSelect` records only when armed **and** the head stack is non-empty; it attributes the select to the **outermost** query on the stack.
  - `RepeatedSelectException extends AssertionError` with `RepeatedSelectException(String message)`.
  - `@AllowRepeatedSelect(String query, String select)` — `@Repeatable(AllowRepeatedSelect.List.class)`, `RUNTIME` retention, `METHOD` target; nested `@interface List { AllowRepeatedSelect[] value(); }`.
  - `SelectGate.evaluate(Map<String,Map<String,Integer>> snapshot, Collection<AllowRepeatedSelect> allowlist)` — throws `RepeatedSelectException` naming every offending head+select+count; silent when clean. A select is "allowed" when some annotation's `query()` matches the head FQN and `select()` matches the select FQN (exact, `.`-suffix, or simple-name match).
- Consumes: nothing from other tasks.

- [ ] **Step 1: Write failing recorder tests**

`SelectCountRecorderTest.java`:
```java
package com.naturalist.data.count;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SelectCountRecorderTest {

    @AfterEach
    void tearDown() {
        SelectCountRecorder.disarm();
    }

    @Test
    void repeatedSelect_withinAHead_isTallied() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsEntry("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 2));
    }

    @Test
    void selectWithNoEnclosingQuery_isNotTallied() {
        SelectCountRecorder.arm();
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName"); // arrange/assert call — depth 0
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }

    @Test
    void selectWhenNotArmed_isNotTallied() {
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.exitQuery();
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }

    @Test
    void nestedQueries_attributeSelectsToTheOutermostHead() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("OuterQueryImpl.forOrder");
        SelectCountRecorder.enterQuery("InnerQueryImpl.forFamily");
        SelectCountRecorder.recordSelect("InnerRepositoryMock.getByParentName");
        SelectCountRecorder.exitQuery();
        SelectCountRecorder.exitQuery();

        assertThat(SelectCountRecorder.snapshot())
                .containsOnlyKeys("OuterQueryImpl.forOrder");
    }

    @Test
    void disarm_clearsAllState() {
        SelectCountRecorder.arm();
        SelectCountRecorder.enterQuery("FooQueryImpl.loadAll");
        SelectCountRecorder.recordSelect("FooRepositoryMock.getByName");
        SelectCountRecorder.disarm();
        assertThat(SelectCountRecorder.snapshot()).isEmpty();
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectCountRecorderTest
```
Expected: FAIL — `SelectCountRecorder` does not exist / does not compile.

- [ ] **Step 3: Implement `SelectCountRecorder`**

```java
package com.naturalist.data.count;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local recorder for the N+1 select gate. The AspectJ {@code SelectCountAspect}
 * feeds it; {@code NaturalistTestExtension} arms it before each test and reads its
 * {@link #snapshot()} after. State is per-thread and bounded to a single test: the head
 * stack self-clears at outermost exit, and {@link #arm()}/{@link #disarm()} reset it, so
 * a pooled thread reused across tests always starts clean.
 */
public final class SelectCountRecorder {

    private static final class Scope {
        boolean armed;
        final Deque<String> heads = new ArrayDeque<>();
        final Map<String, Map<String, Integer>> tally = new LinkedHashMap<>();
    }

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private SelectCountRecorder() {
    }

    public static void arm() {
        Scope s = SCOPE.get();
        s.armed = true;
        s.heads.clear();
        s.tally.clear();
    }

    public static void disarm() {
        SCOPE.remove();
    }

    /** Push a head-of-DAG query. Always maintained so nesting is correct even before a select. */
    public static void enterQuery(String headFqn) {
        SCOPE.get().heads.push(headFqn);
    }

    public static void exitQuery() {
        Deque<String> heads = SCOPE.get().heads;
        if (!heads.isEmpty()) {
            heads.pop();
        }
    }

    /** Record a repository select against the outermost enclosing head, when armed. */
    public static void recordSelect(String selectFqn) {
        Scope s = SCOPE.get();
        if (!s.armed || s.heads.isEmpty()) {
            return;
        }
        String head = s.heads.peekLast(); // outermost = bottom of the stack
        s.tally.computeIfAbsent(head, k -> new LinkedHashMap<>())
                .merge(selectFqn, 1, Integer::sum);
    }

    /** A defensive copy of the current tally: head FQN -> (select FQN -> count). */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        SCOPE.get().tally.forEach((head, selects) -> copy.put(head, new LinkedHashMap<>(selects)));
        return copy;
    }
}
```

- [ ] **Step 4: Run recorder tests to green**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectCountRecorderTest
```
Expected: PASS (5 tests).

- [ ] **Step 5: Write the annotation and exception (no test of their own — exercised by `SelectGateTest`)**

`RepeatedSelectException.java`:
```java
package com.naturalist.data.count;

/** Raised by the N+1 select gate when a head-of-DAG query repeats a repository select. */
public final class RepeatedSelectException extends AssertionError {
    public RepeatedSelectException(String message) {
        super(message);
    }
}
```

`AllowRepeatedSelect.java`:
```java
package com.naturalist.data.count;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whitelists a known-legitimate repeated select on a single test method, so the N+1 gate
 * does not fail it. {@code query} matches the head query FQN and {@code select} the repository
 * select FQN — by exact string, {@code .}-suffix, or simple name.
 */
@Repeatable(AllowRepeatedSelect.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowRepeatedSelect {

    String query();

    String select();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        AllowRepeatedSelect[] value();
    }
}
```

- [ ] **Step 6: Write failing `SelectGateTest`**

`SelectGateTest.java`:
```java
package com.naturalist.data.count;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SelectGateTest {

    private static AllowRepeatedSelect allow(String query, String select) {
        return new AllowRepeatedSelect() {
            @Override public Class<? extends Annotation> annotationType() { return AllowRepeatedSelect.class; }
            @Override public String query() { return query; }
            @Override public String select() { return select; }
        };
    }

    @Test
    void repeatedSelect_throwsNamingHeadAndSelectAndCount() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("InsectImageQueryImpl.forRankHierarchy", Map.of("InsectImageRepositoryMock.getByName", 14));

        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() -> SelectGate.evaluate(snapshot, List.of()))
                .withMessageContaining("InsectImageQueryImpl.forRankHierarchy")
                .withMessageContaining("InsectImageRepositoryMock.getByName")
                .withMessageContaining("14");
    }

    @Test
    void singleSelect_passes() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadOne", Map.of("FooRepositoryMock.getByName", 1));
        assertThatCode(() -> SelectGate.evaluate(snapshot, List.of())).doesNotThrowAnyException();
    }

    @Test
    void repeatedSelect_isSuppressedByMatchingAllowlistEntry() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 3));
        assertThatCode(() ->
                SelectGate.evaluate(snapshot, List.of(allow("FooQueryImpl.loadAll", "getByName"))))
                .doesNotThrowAnyException();
    }

    @Test
    void allowlistEntryForADifferentSelect_doesNotSuppress() {
        Map<String, Map<String, Integer>> snapshot =
                Map.of("FooQueryImpl.loadAll", Map.of("FooRepositoryMock.getByName", 3));
        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() ->
                        SelectGate.evaluate(snapshot, List.of(allow("FooQueryImpl.loadAll", "getPage"))));
    }
}
```

- [ ] **Step 7: Run to verify failure**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectGateTest
```
Expected: FAIL — `SelectGate` does not exist.

- [ ] **Step 8: Implement `SelectGate`**

```java
package com.naturalist.data.count;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Applies the N+1 rule (any select called more than once within one head is a violation). */
public final class SelectGate {

    private SelectGate() {
    }

    public static void evaluate(Map<String, Map<String, Integer>> snapshot,
                                Collection<AllowRepeatedSelect> allowlist) {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> head : snapshot.entrySet()) {
            for (Map.Entry<String, Integer> select : head.getValue().entrySet()) {
                if (select.getValue() > 1 && !allowed(head.getKey(), select.getKey(), allowlist)) {
                    violations.add("  head   %s%n  select %s  called %d×  (expected ≤ 1)"
                            .formatted(head.getKey(), select.getKey(), select.getValue()));
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new RepeatedSelectException(
                    "N+1 select detected%n%s".formatted(String.join(System.lineSeparator(), violations)));
        }
    }

    private static boolean allowed(String headFqn, String selectFqn, Collection<AllowRepeatedSelect> allowlist) {
        return allowlist.stream()
                .anyMatch(a -> matches(headFqn, a.query()) && matches(selectFqn, a.select()));
    }

    /** Match by exact string, by {@code .}-suffix, or by simple name (segment after the last dot). */
    private static boolean matches(String fqn, String pattern) {
        if (fqn.equals(pattern) || fqn.endsWith("." + pattern)) {
            return true;
        }
        int dot = fqn.lastIndexOf('.');
        String simple = dot < 0 ? fqn : fqn.substring(dot + 1);
        return simple.equals(pattern);
    }
}
```

- [ ] **Step 9: Run both test classes to green**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectCountRecorderTest,SelectGateTest
```
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add kernels/framework-test/src/main/java/com/naturalist/data/count kernels/framework-test/src/test/java/com/naturalist/data/count
git commit -m "feat(framework-test): N+1 gate counting core (recorder, gate, annotation)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: AspectJ aspect + load-time-weaving build wiring

Wire the aspect that feeds the recorder, and the `-javaagent` load-time weaver, then prove end-to-end that a woven looping query is detected.

**Files:**
- Create: `kernels/framework-test/src/main/java/com/naturalist/data/count/SelectCountAspect.java`
- Create: `kernels/framework-test/src/main/resources/META-INF/aop.xml`
- Modify: `pom.xml` (root) — `aspectj.version`, dependencyManagement, root `<dependencies>`, dependency-plugin, surefire
- Modify: `kernels/framework-test/pom.xml` — `aspectjrt` (compile), `aspectjweaver` (test)
- Test: `kernels/framework-test/src/test/java/com/naturalist/data/count/SelectGateWeavingTest.java`
- Test fixtures: `FooRepository`, `FooRepositoryMock`, `FooQueryImpl` in `kernels/framework-test/src/test/java/com/naturalist/data/count/`

**Interfaces:**
- Consumes: `SelectCountRecorder`, `SelectGate`, `RepeatedSelectException` (Task 2).
- Produces: `SelectCountAspect` (woven by `aop.xml`); a working `-javaagent` on every module's surefire run. The pointcuts:
  - Query head: `execution(public * com.naturalist..*QueryImpl.*(..)) || execution(public * com.naturalist.data.AbstractEntityQuery.*(..))` — the second disjunct is required because `getByName`/`findPage` are inherited from `AbstractEntityQuery`, not overridden.
  - Select: `execution(public * com.naturalist..*Repository+.*(..))` excluding `insert`/`update`/`save`. `public` excludes the protected `do*` template hooks (which would otherwise double-count `getByName`).
  - FQN keying uses `joinPoint.getTarget().getClass().getName() + "." + signature.getName()` — the **runtime** class, so `getByName` on different repositories does not collide under `AbstractEntityRepository.getByName`.

- [ ] **Step 1: Add AspectJ dependency management and version (root `pom.xml`)**

In `<properties>` add:
```xml
<aspectj.version>1.9.25</aspectj.version>
```
In `<dependencyManagement><dependencies>` (THIRD PARTY section) add:
```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <version>${aspectj.version}</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>${aspectj.version}</version>
</dependency>
```

- [ ] **Step 2: Add a universal test-scope weaver dependency + agent wiring (root `pom.xml`)**

The `-javaagent` path must resolve in **every** module that runs surefire (including api modules that have no `framework-test` dependency), so the weaver goes on every module's test classpath and its path is exposed as a property.

Add a root `<dependencies>` block (there is none yet), immediately after the closing `</dependencyManagement>`:
```xml
<dependencies>
    <!-- Test-only: AspectJ load-time weaver, present on every module's test classpath so
         the surefire -javaagent path resolves everywhere. Never in a production artifact. -->
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjweaver</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```
In `<build><plugins>` (create the block if only `<pluginManagement>` exists) add the dependency-plugin to expose the resolved weaver path as `${org.aspectj:aspectjweaver:jar}`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>weaver-agent-path</id>
            <goals>
                <goal>properties</goal>
            </goals>
            <phase>initialize</phase>
        </execution>
    </executions>
</plugin>
```
In the surefire `<pluginManagement>` entry (currently version-only), add configuration:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <argLine>-javaagent:${org.aspectj:aspectjweaver:jar}</argLine>
    </configuration>
</plugin>
```

- [ ] **Step 3: Add compile/test AspectJ deps to `kernels/framework-test/pom.xml`**

The aspect source (`@Aspect`, `@Around`) needs `aspectjrt` at compile; the weaver is inherited from root at test scope but declare it explicitly here too so framework-test's own tests weave regardless of reactor ordering:
```xml
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjrt</artifactId>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Write the aspect**

```java
package com.naturalist.data.count;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Load-time-woven aspect for the N+1 select gate. Maintains the head-of-DAG query stack and
 * records repository selects into {@link SelectCountRecorder}. Applied only under test
 * (see {@code META-INF/aop.xml}); never on a production classpath.
 */
@Aspect
public class SelectCountAspect {

    @Around("execution(public * com.naturalist..*QueryImpl.*(..)) "
            + "|| execution(public * com.naturalist.data.AbstractEntityQuery.*(..))")
    public Object aroundQuery(ProceedingJoinPoint pjp) throws Throwable {
        SelectCountRecorder.enterQuery(fqn(pjp));
        try {
            return pjp.proceed();
        } finally {
            SelectCountRecorder.exitQuery();
        }
    }

    @Before("execution(public * com.naturalist..*Repository+.*(..)) "
            + "&& !execution(* com.naturalist..*.insert(..)) "
            + "&& !execution(* com.naturalist..*.update(..)) "
            + "&& !execution(* com.naturalist..*.save(..))")
    public void beforeSelect(JoinPoint jp) {
        SelectCountRecorder.recordSelect(fqn(jp));
    }

    private static String fqn(JoinPoint jp) {
        return jp.getTarget().getClass().getName() + "." + jp.getSignature().getName();
    }
}
```

- [ ] **Step 5: Write `aop.xml`**

`kernels/framework-test/src/main/resources/META-INF/aop.xml`:
```xml
<!DOCTYPE aspectj PUBLIC "-//AspectJ//DTD//EN" "https://www.eclipse.org/aspectj/dtd/aspectj.dtd">
<aspectj>
    <aspects>
        <aspect name="com.naturalist.data.count.SelectCountAspect"/>
    </aspects>
    <weaver options="-Xlint:ignore">
        <include within="com.naturalist..*QueryImpl"/>
        <include within="com.naturalist..*Repository*"/>
        <include within="com.naturalist.data.AbstractEntityQuery"/>
    </weaver>
</aspectj>
```
(`com.naturalist..*Repository*` covers `AbstractEntityRepository`, `AbstractTestEntityRepository`, every `*RepositoryMock`, and future `*Repository*` adapters; `AbstractEntityQuery` is named explicitly because it does not match `*QueryImpl`.)

- [ ] **Step 6: Write the failing weaving fixtures + test**

Fixtures (same package, test sources) — names chosen to match the pointcuts:

`FooRepository.java`:
```java
package com.naturalist.data.count;

import java.util.List;

interface FooRepository {
    String getByName(String name);
    List<String> getByEntityNameSet(java.util.Set<String> names);
}
```
`FooRepositoryMock.java`:
```java
package com.naturalist.data.count;

import java.util.List;
import java.util.Set;

class FooRepositoryMock implements FooRepository {
    @Override public String getByName(String name) { return name.toUpperCase(); }
    @Override public List<String> getByEntityNameSet(Set<String> names) { return names.stream().sorted().toList(); }
}
```
`FooQueryImpl.java`:
```java
package com.naturalist.data.count;

import java.util.List;
import java.util.Set;

class FooQueryImpl {
    private final FooRepository repository;
    FooQueryImpl(FooRepository repository) { this.repository = repository; }

    /** Deliberate N+1: one getByName per element. */
    public List<String> loadAll(List<String> names) {
        return names.stream().map(repository::getByName).toList();
    }

    /** Batched sibling: a single select. */
    public List<String> loadBatched(Set<String> names) {
        return repository.getByEntityNameSet(names);
    }
}
```
`SelectGateWeavingTest.java`:
```java
package com.naturalist.data.count;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** End-to-end proof that the aspect weaves and feeds the recorder under load-time weaving. */
class SelectGateWeavingTest {

    @AfterEach
    void tearDown() {
        SelectCountRecorder.disarm();
    }

    @Test
    void loopingQuery_isDetectedAsAnNPlusOne() {
        SelectCountRecorder.arm();
        new FooQueryImpl(new FooRepositoryMock()).loadAll(List.of("a", "b", "c"));

        assertThatExceptionOfType(RepeatedSelectException.class)
                .isThrownBy(() -> SelectGate.evaluate(SelectCountRecorder.snapshot(), List.of()))
                .withMessageContaining("FooQueryImpl.loadAll")
                .withMessageContaining("getByName");
    }

    @Test
    void batchedQuery_passes() {
        SelectCountRecorder.arm();
        new FooQueryImpl(new FooRepositoryMock()).loadBatched(Set.of("a", "b", "c"));
        assertThatCode(() -> SelectGate.evaluate(SelectCountRecorder.snapshot(), List.of()))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 7: Run to verify failure (weaving not yet active / fixtures new)**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectGateWeavingTest
```
Expected: FAIL — `loopingQuery_isDetectedAsAnNPlusOne` fails because either the code does not compile yet or, if the agent is misconfigured, the snapshot is empty (no weaving → `getByName` count 0). This failure is the signal that weaving must be correctly wired before it passes.

- [ ] **Step 8: Verify the agent actually attaches**

Run with weaver messages on to confirm the `-javaagent` resolved and wove the fixtures:
```bash
mvn -pl kernels/framework-test test -Dtest=SelectGateWeavingTest -Dorg.aspectj.weaver.showWeaveInfo=true 2>&1 | grep -i "weav\|FooQueryImpl\|FooRepositoryMock" | head
```
Expected: weave-info lines mentioning `FooQueryImpl` and `FooRepositoryMock`. If instead you see `-javaagent:${org.aspectj:aspectjweaver:jar}` literally in an error, the dependency-plugin `properties` goal did not run — confirm it is in root `<build><plugins>` (not `<pluginManagement>`), and that `aspectjweaver` is on this module's test classpath.

- [ ] **Step 9: Run the weaving test to green**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=SelectGateWeavingTest
```
Expected: PASS (both tests).

- [ ] **Step 10: Confirm no regression in framework-test's own suite**

Run:
```bash
mvn -q -pl kernels/framework-test test
```
Expected: PASS. (framework-test tests now run under the agent; the recorder is inert unless armed, so existing tests are unaffected.)

- [ ] **Step 11: Commit**

```bash
git add pom.xml kernels/framework-test/pom.xml kernels/framework-test/src/main/java/com/naturalist/data/count/SelectCountAspect.java kernels/framework-test/src/main/resources/META-INF/aop.xml kernels/framework-test/src/test/java/com/naturalist/data/count
git commit -m "feat(framework-test): AspectJ N+1 select aspect + load-time weaving

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Activate the gate in `NaturalistTestExtension` + full-suite reckoning

Fold arm/evaluate/disarm into the shared extension so every test class that registers it is gated, prove the `@AllowRepeatedSelect` escape hatch, then run the whole suite and triage what the gate surfaces.

**Files:**
- Modify: `kernels/framework-test/src/main/java/com/naturalist/data/NaturalistTestExtension.java`
- Test: `kernels/framework-test/src/test/java/com/naturalist/data/count/AllowRepeatedSelectWeavingTest.java`
- Modify (triage, as needed): any domain `*QueryImpl` with a real N+1, or any test with a legitimate repeat (add `@AllowRepeatedSelect`).

**Interfaces:**
- Consumes: `SelectCountRecorder.arm()/.disarm()/.snapshot()`, `SelectGate.evaluate(...)`, `@AllowRepeatedSelect` (Tasks 2–3).
- Produces: `NaturalistTestExtension implements BeforeEachCallback, AfterEachCallback` — `beforeEach` = `clear()` + `arm()`; `afterEach` = evaluate the snapshot against the test method's `@AllowRepeatedSelect` annotations, always `disarm()` in a `finally`.

- [ ] **Step 1: Write the failing allowlist weaving test**

`AllowRepeatedSelectWeavingTest.java` — reuses the Task 3 fixtures; registers the real extension so `beforeEach`/`afterEach` drive the gate:
```java
package com.naturalist.data.count;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

/**
 * The gate is live via {@link NaturalistTestExtension#afterEach}. A looping query here would
 * fail the test in afterEach unless whitelisted — which is exactly what these two methods assert.
 */
class AllowRepeatedSelectWeavingTest {

    @RegisterExtension
    NaturalistTestExtension ext = NaturalistTestExtension.create();

    @Test
    @AllowRepeatedSelect(query = "FooQueryImpl.loadAll", select = "getByName")
    void loopingQuery_isSuppressedByAnnotation() {
        new FooQueryImpl(new FooRepositoryMock()).loadAll(List.of("a", "b", "c"));
        // No assertion: the gate runs in afterEach. Without the annotation this test would fail there.
    }

    @Test
    void batchedQuery_passesWithoutAnnotation() {
        new FooQueryImpl(new FooRepositoryMock()).loadBatched(java.util.Set.of("a", "b", "c"));
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=AllowRepeatedSelectWeavingTest
```
Expected: FAIL — `NaturalistTestExtension` does not yet implement `AfterEachCallback`/arm/evaluate, so `loopingQuery_isSuppressedByAnnotation` will either not be gated (annotation unread) or the extension will not compile against the new behaviour. (Before the wiring, the looping call is not evaluated at all, so confirm the test is meaningfully exercising the new path once Step 3 lands.)

- [ ] **Step 3: Wire the extension**

```java
package com.naturalist.data;

import com.naturalist.data.count.AllowRepeatedSelect;
import com.naturalist.data.count.SelectCountRecorder;
import com.naturalist.data.count.SelectGate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.List;

/**
 * JUnit 5 lifecycle wrapper around {@link NaturalistDatabase}, and host of the N+1 select gate.
 * {@code beforeEach} clears the source registry and arms the select recorder; {@code afterEach}
 * evaluates the recorded tally against {@link SelectGate}, honouring {@link AllowRepeatedSelect}
 * on the test method, and always disarms.
 *
 * <pre>{@code
 * @RegisterExtension
 * NaturalistTestExtension db = NaturalistTestExtension.create();
 * }</pre>
 */
public class NaturalistTestExtension extends NaturalistDatabase
        implements BeforeEachCallback, AfterEachCallback {

    private NaturalistTestExtension() {
        super();
    }

    public static NaturalistTestExtension create() {
        return new NaturalistTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        clear();
        SelectCountRecorder.arm();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        try {
            SelectGate.evaluate(SelectCountRecorder.snapshot(), allowlist(context));
        } finally {
            SelectCountRecorder.disarm();
        }
    }

    private static List<AllowRepeatedSelect> allowlist(ExtensionContext context) {
        return context.getTestMethod()
                .map(NaturalistTestExtension::readAllowlist)
                .orElseGet(List::of);
    }

    private static List<AllowRepeatedSelect> readAllowlist(Method method) {
        return List.of(method.getAnnotationsByType(AllowRepeatedSelect.class));
    }
}
```

- [ ] **Step 4: Run the allowlist test to green**

Run:
```bash
mvn -q -pl kernels/framework-test test -Dtest=AllowRepeatedSelectWeavingTest,SelectGateWeavingTest,SelectCountRecorderTest,SelectGateTest
```
Expected: PASS. The suppressed looping test passes; removing its annotation would make `afterEach` throw `RepeatedSelectException`.

- [ ] **Step 5: Full-suite reckoning — build everything with the gate live**

Run the full build (the gate now applies to every test class that registers `NaturalistTestExtension`):
```bash
mvn clean install
```
Expected: either BUILD SUCCESS (no N+1s hiding in the current suite) or one or more `RepeatedSelectException` failures. **Do not proceed past a failure without triage** — a full clean install matters here because kernel signature changes were involved (per project convention, incremental builds leave stale classes).

- [ ] **Step 6: Triage each `RepeatedSelectException` (repeat until green)**

For every failure, read the message (`head` + `select` + count) and decide:
- **Real N+1** (a `*QueryImpl` looping a select that has a batched sibling) — fix the query to call the batched method (`getByEntityNameSet` / `getByParentNames` / `findByNameSet`) once, following `InsectImageQueryImpl.forRankHierarchy` and the `getByRankNames`/`getByParentNames` pattern in [domains/CLAUDE.md](../../domains/CLAUDE.md). This is the bug the gate exists to catch.
- **Legitimate repeat** (genuinely two distinct single lookups the domain cannot batch) — add `@AllowRepeatedSelect(query = "...", select = "...")` to the specific test method, with a one-line comment explaining why it cannot batch. Each entry is a visible, greppable admission.
- Known pre-existing case to expect: `InsectsController` "calls `insectQuery.fieldObservations().forNaturalistAndSubjects` twice with identical arguments" ([domains/insects/CLAUDE.md](../../domains/insects/CLAUDE.md)). That is a *controller-level* repeat and is only caught if a test drives it through a registered extension; if a core test surfaces it, prefer fixing the double call over allowlisting.

Re-run `mvn clean install` after each fix/allowlist until green.

- [ ] **Step 7: Update docs**

- Add a short subsection to [kernels/CLAUDE.md](../../kernels/CLAUDE.md) under a new "N+1 Select Gate" heading: what it does, that `NaturalistTestExtension` hosts it, and how to use `@AllowRepeatedSelect` with a justifying comment.
- Update the design doc's status line to "shipped".
- Move both plan and design docs' tracker entry as the project convention dictates ([docs/work-tracker.md](../../docs/work-tracker.md)).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(framework-test): activate N+1 select gate in NaturalistTestExtension

Gate every test that registers the extension: arm the select recorder in
beforeEach, evaluate in afterEach, honour @AllowRepeatedSelect. Triaged the
existing suite (fixes/allowlists as noted).

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- No-Spring-in-core / test-only deps → Task 3 (test scope, dependencyManagement) + Global Constraints. ✓
- Domain-specific selects caught → `*Repository+` pointcut + `aop.xml` `*Repository*` include (Task 3); inherited `AbstractEntityQuery` heads handled by the second query disjunct. ✓
- Two pointcuts, armed + stack-non-empty gating, assert-phase exclusion → `SelectCountRecorder.recordSelect` (Task 2) + aspect (Task 3). ✓
- "Which query is under test" dissolved via outermost-head attribution → `peekLast()` + nested-query test (Task 2). ✓
- Rule 1 + `@AllowRepeatedSelect` → `SelectGate` (Task 2) + weaving allowlist test (Task 4). ✓
- Evaluate at native `afterEach`, disarm in `finally`, same-thread → `NaturalistTestExtension` (Task 4). ✓
- ThreadLocal safety (self-clear on exit, arm/disarm reset) → recorder impl + tests (Task 2). ✓
- Prove-the-gate fixtures (looping + batched + allowlisted) → Tasks 3–4. ✓
- Rename `NaturalistDatabaseExtension` → `NaturalistTestExtension` → Task 1. ✓
- Full-suite reckoning (turn on, triage) → Task 4 Steps 5–6. ✓

**Design decisions locked (were "open questions" in the design doc):**
1. Module home → `kernels/framework-test` (no new module).
2. Weave scope → restricted `within` includes (`*QueryImpl`, `*Repository*`, `AbstractEntityQuery`), not all of `com.naturalist..*`.
3. Rename timing → its own commit first (Task 1).

**Placeholder scan:** none — every code/config step carries literal content.

**Type consistency:** `arm/disarm/enterQuery/exitQuery/recordSelect/snapshot`, `SelectGate.evaluate(Map, Collection<AllowRepeatedSelect>)`, `RepeatedSelectException(String)`, `@AllowRepeatedSelect(query, select)` are used identically across Tasks 2–4. Aspect FQN helper and recorder key format (`Class.getName() + "." + method`) match the tally keys asserted in tests. ✓

**Residual risk to watch during execution:**
- The `-javaagent:${org.aspectj:aspectjweaver:jar}` property resolution (Task 3 Step 8 verifies it). If a module's surefire is overridden elsewhere (e.g. `apps/management-console/pom.xml:139`), confirm it does not drop the inherited `argLine`; if it sets its own, merge the agent flag there too.
- AspectJ `1.9.25` vs Java 25 class files — Task 3 Step 8's weave-info check catches a version mismatch early.
