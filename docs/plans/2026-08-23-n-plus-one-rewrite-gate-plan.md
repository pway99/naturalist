# Static N+1 Select Gate (OpenRewrite Backstop) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A source-level CI gate — an OpenRewrite search recipe in `tooling/naturalist-rewrite` — that fails the build when a repository select or query invocation appears lexically inside an iteration construct (the N+1 fan-out pattern), planting a `SearchResult` marker and emitting a findings data table, discovered by the already-wired `rewrite:dryRun` + `failOnDryRunResults=true` gate.

**Architecture:** One imperative `Recipe` (`NoSelectInIteration`) with a `JavaIsoVisitor`. A "select site" is any `J.MethodInvocation`/`J.MemberReference` whose method's declaring type is assignable to `EntityRepository` or `EntityQuery` (excluding writes `insert`/`update`/`save`). It is a violation when, walking the cursor up within the enclosing method, it is contained by a classic loop (`for`/`for-each`/`while`/`do-while`) or by a lambda/method-ref that is the functional argument to a per-element fan-out op (`Stream`/`BaseStream` ops, `Iterable.forEach`, `Map.forEach`). The recipe is main-source-only, records each hit to a `Findings` data table, and is wired through a new declarative composite `EnforceQueryHygiene`. Cross-method and recursive fan-out are out of scope by design (delegated to the paused AspectJ runtime gate).

**Tech Stack:** Java 25, Maven, OpenRewrite (`rewrite-java`, `rewrite-test`, `rewrite-java-25`, `rewrite-maven-plugin` 6.45.0), JUnit 5, AssertJ.

**Source design:** [docs/plans/2026-08-23-n-plus-one-rewrite-gate-design.md](2026-08-23-n-plus-one-rewrite-gate-design.md)

## Global Constraints

- **Recipe lives in `tooling/naturalist-rewrite`**, package `com.naturalist.rewrite`, alongside the three existing recipes. No new module.
- **Type FQNs (verbatim):** repository port `com.naturalist.data.EntityRepository`; query port `com.naturalist.data.EntityQuery`.
- **Write methods excluded (verbatim):** `insert`, `update`, `save`.
- **Main-source-only:** skip any source file whose `org.openrewrite.java.marker.JavaSourceSet` marker name is not `"main"` (a file with no marker is treated as non-main and skipped — conservative, matching `NoCachedTestEntitySourceField`'s convention inverted).
- **No escape hatch** in the recipe initially (absolute invariant, like the existing three). A deferred `@Option` exclusion list configured in `naturalist.yml` is added only if the reckoning surfaces a real un-batchable case — not built in this plan.
- **Search recipe, not a transformation:** it plants `SearchResult` markers and emits a data table; it never rewrites code. Use `JavaIsoVisitor` (node identity preserved).
- **Test stubs, not real dependencies:** the recipe module must not depend on `framework`. Port types are stubbed as `RewriteTest` supporting sources (extend the existing `NaturalistTypeStubs`).
- **Git:** print commands; commit only when the user says so. Trunk-based — branch, do not open a PR unless asked. Each task's commit step shows the command; do not run it unprompted.

---

## File Structure

- `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java` — the recipe: select-site detection, iteration-construct classification, marker, and nested `Findings` data table. **(Tasks 1–3)**
- `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NaturalistTypeStubs.java` — extend with `ENTITY_REPOSITORY`, `ENTITY_QUERY`, and concrete `FooRepository`/`FooQuery` stubs. **(Task 1)**
- `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java` — fixture-table unit tests (loops, streams, exclusions, data table). **(Tasks 1–3)**
- `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml` — add the `EnforceQueryHygiene` composite. **(Task 4)**
- `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/EnforceQueryHygieneCompositeTest.java` — loads the composite by name and asserts it flags. **(Task 4)**
- `pom.xml` (root) — add `com.naturalist.EnforceQueryHygiene` to `<activeRecipes>`. **(Task 4)**

---

## Task 1: Recipe core — select sites in classic loops

The recipe skeleton, select-site detection for both ports, classic-loop enclosure, the `SearchResult` marker, the write exclusion, the main-source-only guard, and the out-of-loop / helper-boundary / test-source clean cases. No stream ops, no data table yet.

**Files:**
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java`
- Modify: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NaturalistTypeStubs.java`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java`

**Interfaces:**
- Produces:
  - `com.naturalist.rewrite.NoSelectInIteration extends org.openrewrite.Recipe`, zero-arg, no options.
  - Constant `NoSelectInIteration.MESSAGE` (the marker text): `"N+1 fan-out: repository or query select invoked inside an iteration construct; call the batched sibling once instead"`.
  - Stub constants on `NaturalistTypeStubs`: `ENTITY_REPOSITORY`, `ENTITY_QUERY`, `FOO_REPOSITORY`, `FOO_QUERY`.
- Consumes: nothing from other tasks.

- [ ] **Step 1: Add port + concrete stubs to `NaturalistTypeStubs`**

Append these constants inside `NaturalistTypeStubs` (before the closing brace). They give the recipe real types to attribute against — a repository port with the six methods, a query port with three, and one concrete impl of each:

```java
static final String ENTITY_REPOSITORY = """
    package com.naturalist.data;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    public interface EntityRepository<NAME, ENTITY> {
        Optional<ENTITY> getByName(NAME name);
        List<ENTITY> getByEntityNameSet(Set<NAME> nameSet);
        void insert(ENTITY entity);
        void update(ENTITY entity);
        ENTITY save(ENTITY entity);
    }
    """;

static final String ENTITY_QUERY = """
    package com.naturalist.data;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    public interface EntityQuery<NAME, E> {
        Optional<E> getByName(NAME name);
        List<E> findByNameSet(Set<NAME> nameSet);
    }
    """;

static final String FOO_REPOSITORY = """
    package com.naturalist.data;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    public interface FooRepository extends EntityRepository<String, String> {
        List<String> getByParentNames(Set<String> parents);
    }
    """;

static final String FOO_QUERY = """
    package com.naturalist.data;
    import java.util.List;
    import java.util.Optional;
    import java.util.Set;
    public interface FooQuery extends EntityQuery<String, String> {
    }
    """;
```

- [ ] **Step 2: Write the failing test class with the classic-loop fixtures**

Create `NoSelectInIterationTest.java`. The `SEEN` helper wraps a class body in the `com.naturalist.data` package importing the stub types; every fixture passes the four stubs as unchanged supporting sources and the code under test via `srcMainJava`:

```java
package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;

class NoSelectInIterationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoSelectInIteration());
    }

    private static final String MARK = "/*~~(" + NoSelectInIteration.MESSAGE + ")~~>*/";

    @Test
    void flagsRepositorySelectInForEachLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                repo.getByName(n);
                            }
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                %srepo.getByName(n);
                            }
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void flagsDomainSpecificSelectInWhileLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.Iterator;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Iterator<Set<String>> it) {
                            while (it.hasNext()) {
                                repo.getByParentNames(it.next());
                            }
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.Iterator;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Iterator<Set<String>> it) {
                            while (it.hasNext()) {
                                %srepo.getByParentNames(it.next());
                            }
                        }
                    }
                    """.formatted(MARK)
                )
            )
        );
    }

    @Test
    void doesNotFlagBatchedSelectOutsideLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.Set;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(Set<String> names) {
                            repo.getByEntityNameSet(names);
                        }
                    }
                    """  // no `after` => unchanged
                )
            )
        );
    }

    @Test
    void doesNotFlagWriteInLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void seed(List<String> names) {
                            for (String n : names) {
                                repo.insert(n);
                            }
                        }
                    }
                    """
                )
            )
        );
    }

    @Test
    void doesNotFlagSelectInHelperCalledFromLoop() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    // the loop and the select are in different methods — cross-method fan-out,
                    // delegated to the AspectJ runtime gate; this static recipe must not flag it.
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                one(n);
                            }
                        }
                        String one(String n) { return repo.getByName(n).orElse(null); }
                    }
                    """
                )
            )
        );
    }

    @Test
    void doesNotFlagLoopSelectInTestSource() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcTestJava(
                java(
                    // legitimate test arrange/assert loop — main-source-only guard leaves it alone.
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class QTest {
                        private final FooRepository repo = null;
                        void assertsEach(List<String> names) {
                            for (String n : names) {
                                repo.getByName(n);
                            }
                        }
                    }
                    """
                )
            )
        );
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest
```
Expected: FAIL — `NoSelectInIteration` does not exist / does not compile.

- [ ] **Step 4: Implement `NoSelectInIteration` (core — loops only)**

```java
package com.naturalist.rewrite;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

public class NoSelectInIteration extends Recipe {

    static final String MESSAGE =
        "N+1 fan-out: repository or query select invoked inside an iteration construct; "
        + "call the batched sibling once instead";

    private static final String REPOSITORY = "com.naturalist.data.EntityRepository";
    private static final String QUERY = "com.naturalist.data.EntityQuery";
    private static final Set<String> WRITES = Set.of("insert", "update", "save");

    @Override
    public String getDisplayName() {
        return "Do not invoke a repository or query select inside a loop or stream fan-out";
    }

    @Override
    public String getDescription() {
        return "Flags an EntityRepository/EntityQuery select (excluding insert/update/save) that "
             + "appears lexically inside a loop or a per-element stream operation — the N+1 "
             + "fan-out pattern. Call the batched sibling (getByEntityNameSet / findByNameSet / a "
             + "domain getBy…Names) once instead. Cross-method and recursive fan-out are out of "
             + "scope (covered by the runtime select-count gate).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    boolean isMain = ((JavaSourceFile) tree).getMarkers()
                        .findFirst(JavaSourceSet.class)
                        .map(s -> "main".equals(s.getName()))
                        .orElse(false);
                    if (!isMain) {
                        return (J) tree;
                    }
                }
                return super.visit(tree, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(mi, ctx);
                if (!isSelectSite(m.getMethodType())) {
                    return m;
                }
                String kind = enclosingIterationKind(getCursor());
                if (kind == null) {
                    return m;
                }
                return SearchResult.found(m, MESSAGE);
            }
        };
        return Preconditions.check(
            Preconditions.or(new UsesType<>(REPOSITORY, true), new UsesType<>(QUERY, true)),
            visitor);
    }

    static boolean isSelectSite(JavaType.@Nullable Method methodType) {
        if (methodType == null) {
            return false;
        }
        JavaType.FullyQualified declaring = methodType.getDeclaringType();
        if (declaring == null) {
            return false;
        }
        boolean isPort = TypeUtils.isAssignableTo(REPOSITORY, declaring)
                      || TypeUtils.isAssignableTo(QUERY, declaring);
        return isPort && !WRITES.contains(methodType.getName());
    }

    /**
     * Walk from the select site up to (not past) the enclosing method declaration. Returns a
     * short label for the enclosing iteration construct, or {@code null} if none is found in the
     * same lexical method scope. (Stream fan-out is added in Task 2.)
     */
    static @Nullable String enclosingIterationKind(Cursor siteCursor) {
        Cursor cursor = siteCursor.getParent();
        while (cursor != null) {
            Object value = cursor.getValue();
            if (value instanceof J.ForEachLoop) {
                return "for-each-loop";
            }
            if (value instanceof J.ForLoop) {
                return "for-loop";
            }
            if (value instanceof J.WhileLoop) {
                return "while-loop";
            }
            if (value instanceof J.DoWhileLoop) {
                return "do-while-loop";
            }
            if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
                return null;
            }
            cursor = cursor.getParent();
        }
        return null;
    }
}
```

- [ ] **Step 5: Run the tests to green**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest
```
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NaturalistTypeStubs.java tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java
git commit -m "feat(rewrite): flag repository/query selects inside classic loops (N+1 gate core)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Stream fan-out — lambdas and method references

Extend detection to per-element stream operations: a select inside a lambda passed to a fan-out op, and a method-reference select (`repo::getByName`) passed to one. This is where the real hand-fixed cases (`.stream().map(repo::getByName)`) get caught.

**Files:**
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java`

**Interfaces:**
- Consumes: `NoSelectInIteration.isSelectSite`, `MESSAGE` (Task 1).
- Produces: a private `boolean matchesFanOut(J.MethodInvocation)` predicate and a `visitMemberReference` override; `enclosingIterationKind` extended to recognize fan-out lambdas.

- [ ] **Step 1: Add the failing stream fixtures**

Append to `NoSelectInIterationTest`:

```java
@Test
void flagsRepositorySelectMethodReferenceInStreamMap() {
    rewriteRun(
        java(NaturalistTypeStubs.ENTITY_REPOSITORY),
        java(NaturalistTypeStubs.FOO_REPOSITORY),
        srcMainJava(
            java(
                """
                package com.naturalist.data;
                import java.util.List;
                import java.util.Optional;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    List<Optional<String>> load(List<String> names) {
                        return names.stream().map(repo::getByName).toList();
                    }
                }
                """,
                """
                package com.naturalist.data;
                import java.util.List;
                import java.util.Optional;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    List<Optional<String>> load(List<String> names) {
                        return names.stream().map(%srepo::getByName).toList();
                    }
                }
                """.formatted(MARK)
            )
        )
    );
}

@Test
void flagsQuerySelectLambdaInStreamForEach() {
    rewriteRun(
        java(NaturalistTypeStubs.ENTITY_QUERY),
        java(NaturalistTypeStubs.FOO_QUERY),
        srcMainJava(
            java(
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooQuery query;
                    Q(FooQuery query) { this.query = query; }
                    void load(List<String> names) {
                        names.stream().forEach(n -> query.getByName(n));
                    }
                }
                """,
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooQuery query;
                    Q(FooQuery query) { this.query = query; }
                    void load(List<String> names) {
                        names.stream().forEach(n -> %squery.getByName(n));
                    }
                }
                """.formatted(MARK)
            )
        )
    );
}

@Test
void flagsRepositorySelectInStreamFilterPredicate() {
    rewriteRun(
        java(NaturalistTypeStubs.ENTITY_REPOSITORY),
        java(NaturalistTypeStubs.FOO_REPOSITORY),
        srcMainJava(
            java(
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    List<String> load(List<String> names) {
                        return names.stream().filter(n -> repo.getByName(n).isPresent()).toList();
                    }
                }
                """,
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    List<String> load(List<String> names) {
                        return names.stream().filter(n -> %srepo.getByName(n).isPresent()).toList();
                    }
                }
                """.formatted(MARK)
            )
        )
    );
}

@Test
void doesNotFlagSelectInNonFanOutLambda() {
    rewriteRun(
        java(NaturalistTypeStubs.ENTITY_REPOSITORY),
        java(NaturalistTypeStubs.FOO_REPOSITORY),
        srcMainJava(
            java(
                // Optional.orElseGet supplier is not a per-element fan-out; a single deferred
                // lookup, not an N+1. Must stay clean.
                """
                package com.naturalist.data;
                import java.util.Optional;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    String load(Optional<String> maybe, String fallback) {
                        return maybe.orElseGet(() -> repo.getByName(fallback).orElse(null));
                    }
                }
                """
            )
        )
    );
}
```

- [ ] **Step 2: Run to verify failure**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest
```
Expected: FAIL — the three stream fixtures do not yet get marked (stream fan-out unimplemented). `doesNotFlagSelectInNonFanOutLambda` already passes.

- [ ] **Step 3: Add the fan-out matcher and member-reference handling**

In `NoSelectInIteration`, add imports:
```java
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import java.util.List;
```
Add the matcher list as a field on the recipe (static):
```java
private static final List<MethodMatcher> FAN_OUT = List.of(
    new MethodMatcher("java.util.stream.Stream *(..)"),
    new MethodMatcher("java.util.stream.IntStream *(..)"),
    new MethodMatcher("java.util.stream.LongStream *(..)"),
    new MethodMatcher("java.util.stream.DoubleStream *(..)"),
    new MethodMatcher("java.lang.Iterable forEach(..)"),
    new MethodMatcher("java.util.Map forEach(..)")
);

private static boolean matchesFanOut(Expression e) {
    return e instanceof J.MethodInvocation mi && FAN_OUT.stream().anyMatch(m -> m.matches(mi));
}
```
Extend `enclosingIterationKind` to recognize a fan-out lambda. Replace the `J.MethodDeclaration`/`J.ClassDeclaration` terminal block and add a `J.Lambda` case, so the loop body becomes:
```java
while (cursor != null) {
    Object value = cursor.getValue();
    if (value instanceof J.ForEachLoop) { return "for-each-loop"; }
    if (value instanceof J.ForLoop) { return "for-loop"; }
    if (value instanceof J.WhileLoop) { return "while-loop"; }
    if (value instanceof J.DoWhileLoop) { return "do-while-loop"; }
    if (value instanceof J.Lambda) {
        Cursor parent = cursor.getParent();
        if (parent != null && parent.getValue() instanceof J.MethodInvocation fan
                && matchesFanOut(fan)) {
            return "stream:" + fan.getSimpleName();
        }
        // a lambda that is not a per-element fan-out arg is a scope boundary: stop.
        return null;
    }
    if (value instanceof J.MethodDeclaration || value instanceof J.ClassDeclaration) {
        return null;
    }
    cursor = cursor.getParent();
}
```
Add the member-reference override (a `repo::getByName` passed straight to a fan-out op — the site is the reference itself, its cursor parent is the fan-out invocation):
```java
@Override
public J.MemberReference visitMemberReference(J.MemberReference mr, ExecutionContext ctx) {
    J.MemberReference m = super.visitMemberReference(mr, ctx);
    if (!isSelectSite(m.getMethodType())) {
        return m;
    }
    Cursor parent = getCursor().getParent();
    if (parent != null && parent.getValue() instanceof J.MethodInvocation fan
            && matchesFanOut(fan)) {
        return SearchResult.found(m, MESSAGE);
    }
    return m;
}
```

- [ ] **Step 4: Run the tests to green**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest
```
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java
git commit -m "feat(rewrite): flag selects in stream fan-out ops (map/filter/forEach, method refs)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Findings data table

Emit one row per finding to an OpenRewrite data table (`sourcePath`, `enclosingType`, `enclosingMethod`, `select`, `iterationKind`, `suggestedBatchedSibling`), so a run produces a scannable CSV summary. The marker still fails the gate; the table is the report.

**Files:**
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java`

**Interfaces:**
- Consumes: the visitor and `enclosingIterationKind` label (Tasks 1–2).
- Produces: nested `NoSelectInIteration.Findings extends org.openrewrite.DataTable<Findings.Row>` with `public record Row(String sourcePath, String enclosingType, String enclosingMethod, String select, String iterationKind, String suggestedBatchedSibling)`.

- [ ] **Step 1: Write the failing data-table assertion test**

Append to `NoSelectInIterationTest`. `dataTable(Row.class, consumer)` on the `RecipeSpec` captures the emitted rows:

```java
import org.openrewrite.test.SourceSpecs;                 // (add with the other imports)
import static org.assertj.core.api.Assertions.assertThat;

@Test
void emitsAFindingsRowForALoopSelect() {
    rewriteRun(
        spec -> spec.dataTable(NoSelectInIteration.Findings.Row.class, rows -> {
            assertThat(rows).hasSize(1);
            NoSelectInIteration.Findings.Row row = rows.get(0);
            assertThat(row.enclosingType()).isEqualTo("Q");
            assertThat(row.enclosingMethod()).isEqualTo("load");
            assertThat(row.select()).isEqualTo("getByName");
            assertThat(row.iterationKind()).isEqualTo("for-each-loop");
            assertThat(row.suggestedBatchedSibling()).isEqualTo("getByEntityNameSet");
        }),
        java(NaturalistTypeStubs.ENTITY_REPOSITORY),
        java(NaturalistTypeStubs.FOO_REPOSITORY),
        srcMainJava(
            java(
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    void load(List<String> names) {
                        for (String n : names) {
                            repo.getByName(n);
                        }
                    }
                }
                """,
                """
                package com.naturalist.data;
                import java.util.List;
                class Q {
                    private final FooRepository repo;
                    Q(FooRepository repo) { this.repo = repo; }
                    void load(List<String> names) {
                        for (String n : names) {
                            %srepo.getByName(n);
                        }
                    }
                }
                """.formatted(MARK)
            )
        )
    );
}
```

(The unused `SourceSpecs` import is not needed — omit it; listed only to note no new source-spec helper is required.)

- [ ] **Step 2: Run to verify failure**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest#emitsAFindingsRowForALoopSelect
```
Expected: FAIL — `NoSelectInIteration.Findings` does not exist; no rows captured.

- [ ] **Step 3: Add the `Findings` data table and emit rows**

Add imports:
```java
import org.openrewrite.Column;
import org.openrewrite.DataTable;
```
Add the data table as a nested type and a `transient` recipe field:
```java
private final transient Findings findings = new Findings(this);

public static class Findings extends DataTable<Findings.Row> {
    public Findings(Recipe recipe) {
        super(recipe,
            "N+1 select findings",
            "Repository or query selects invoked inside an iteration construct.");
    }

    public record Row(
        @Column(displayName = "Source path",
                description = "Path of the file containing the finding.") String sourcePath,
        @Column(displayName = "Enclosing type",
                description = "Simple name of the class holding the finding.") String enclosingType,
        @Column(displayName = "Enclosing method",
                description = "Name of the method holding the finding.") String enclosingMethod,
        @Column(displayName = "Select",
                description = "The repository/query method invoked per element.") String select,
        @Column(displayName = "Iteration kind",
                description = "The loop or stream operation that fans the select out.") String iterationKind,
        @Column(displayName = "Suggested batched sibling",
                description = "The batched method to call once instead.") String suggestedBatchedSibling) {
    }
}
```
Add helpers that read the enclosing context and pick a batched sibling:
```java
private String suggestedSibling(JavaType.Method methodType) {
    JavaType.FullyQualified declaring = methodType.getDeclaringType();
    if (declaring != null && TypeUtils.isAssignableTo(QUERY, declaring)) {
        return "findByNameSet";
    }
    return "getByEntityNameSet";
}

private String enclosingType() {
    J.ClassDeclaration c = getCursor().firstEnclosing(J.ClassDeclaration.class);
    return c == null ? "" : c.getSimpleName();
}

private String enclosingMethod() {
    J.MethodDeclaration m = getCursor().firstEnclosing(J.MethodDeclaration.class);
    return m == null ? "<initializer>" : m.getSimpleName();
}

private String sourcePath() {
    JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
    return sf == null ? "" : sf.getSourcePath().toString();
}
```
These helpers are instance methods of the anonymous `JavaIsoVisitor`, so they read that visitor's `getCursor()`. Route both hit sites through a single record-and-mark helper (also a visitor method) so the row and the marker stay in lockstep:
```java
private <T extends J> T recordAndMark(T site, String kind, JavaType.Method methodType,
                                       ExecutionContext ctx) {
    findings.insertRow(ctx, new Findings.Row(
        sourcePath(), enclosingType(), enclosingMethod(),
        methodType.getName(), kind, suggestedSibling(methodType)));
    return SearchResult.found(site, MESSAGE);
}
```
Then in `visitMethodInvocation`, replace `return SearchResult.found(m, MESSAGE);` with:
```java
return recordAndMark(m, kind, m.getMethodType(), ctx);
```
and in `visitMemberReference`, replace `return SearchResult.found(m, MESSAGE);` with:
```java
return recordAndMark(m, "stream:" + ((J.MethodInvocation) parent.getValue()).getSimpleName(),
        m.getMethodType(), ctx);
```

- [ ] **Step 4: Run the data-table test to green**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest#emitsAFindingsRowForALoopSelect
```
Expected: PASS.

- [ ] **Step 5: Run the whole recipe test class**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoSelectInIterationTest
```
Expected: PASS (11 tests) — markers unchanged, data table now populated.

- [ ] **Step 6: Commit**

```bash
git add tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoSelectInIteration.java tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoSelectInIterationTest.java
git commit -m "feat(rewrite): emit N+1 findings data table alongside the marker

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Wire the composite + repo-wide reckoning + arm

Add the `EnforceQueryHygiene` declarative composite, prove it loads and flags via a composite test, then run the repo-wide `rewrite:dryRun` reckoning, triage what it surfaces, and arm it in `<activeRecipes>`.

**Files:**
- Modify: `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml`
- Test: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/EnforceQueryHygieneCompositeTest.java`
- Modify: `pom.xml` (root)
- Modify (triage, as needed): any main-source `*QueryImpl` / controller with a real N+1; or `naturalist.yml` (deferred exclusion option) for a genuinely un-batchable case.

**Interfaces:**
- Consumes: `com.naturalist.rewrite.NoSelectInIteration` (Tasks 1–3).
- Produces: declarative recipe `com.naturalist.EnforceQueryHygiene` listing `NoSelectInIteration`; the root pom running it under `rewrite:dryRun` with `failOnDryRunResults=true`.

- [ ] **Step 1: Add the composite to `naturalist.yml`**

Append a second document to the file (recipes are separated by `---`):

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.naturalist.EnforceQueryHygiene
displayName: Naturalist query-hygiene enforcement
description: >-
  Fails the build on an N+1 fan-out: a repository or query select invoked inside a loop or a
  per-element stream operation. The lexical, same-method backstop to the runtime select-count
  gate; call the batched sibling once instead.
recipeList:
  - com.naturalist.rewrite.NoSelectInIteration
```

- [ ] **Step 2: Write the failing composite test**

Create `EnforceQueryHygieneCompositeTest.java` — loads the recipe by name from the classpath resource, proving the yml wiring resolves:

```java
package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;

class EnforceQueryHygieneCompositeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.naturalist.EnforceQueryHygiene");
    }

    @Test
    void compositeFlagsAnNPlusOne() {
        rewriteRun(
            java(NaturalistTypeStubs.ENTITY_REPOSITORY),
            java(NaturalistTypeStubs.FOO_REPOSITORY),
            srcMainJava(
                java(
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                repo.getByName(n);
                            }
                        }
                    }
                    """,
                    """
                    package com.naturalist.data;
                    import java.util.List;
                    class Q {
                        private final FooRepository repo;
                        Q(FooRepository repo) { this.repo = repo; }
                        void load(List<String> names) {
                            for (String n : names) {
                                /*~~(%s)~~>*/repo.getByName(n);
                            }
                        }
                    }
                    """.formatted(NoSelectInIteration.MESSAGE)
                )
            )
        );
    }
}
```

- [ ] **Step 3: Run to verify failure**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test -Dtest=EnforceQueryHygieneCompositeTest
```
Expected: FAIL — the `naturalist.yml` document does not yet define `com.naturalist.EnforceQueryHygiene` (recipe-not-found) until Step 1 is saved; if Step 1 is already saved, this step confirms it now resolves and PASSES. Run after Step 1 to confirm green.

- [ ] **Step 4: Run the composite test to green + the full module suite**

Run:
```bash
mvn -q -pl tooling/naturalist-rewrite test
```
Expected: PASS — `NoSelectInIterationTest`, `EnforceQueryHygieneCompositeTest`, and the three pre-existing recipe tests all green.

- [ ] **Step 5: Commit the recipe wiring (before the repo-wide reckoning)**

```bash
git add tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/EnforceQueryHygieneCompositeTest.java
git commit -m "feat(rewrite): add EnforceQueryHygiene composite for the N+1 select gate

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

- [ ] **Step 6: Install the recipe module so the plugin resolves it**

The root `rewrite-maven-plugin` loads `naturalist-rewrite` as a plugin dependency; a signature/resource change needs a fresh install (project convention — incremental builds leave stale classes):
```bash
mvn -q -pl tooling/naturalist-rewrite -am install -DskipTests
```
Expected: BUILD SUCCESS.

- [ ] **Step 7: Repo-wide dry-run reckoning (temporary activation)**

Temporarily add the composite to the root pom to see what the real tree surfaces. In `pom.xml` `<activeRecipes>` (currently only `EnforceArchitecture`), add:
```xml
<recipe>com.naturalist.EnforceQueryHygiene</recipe>
```
Then run the dry run from the repo root:
```bash
mvn -q rewrite:dryRun
```
Expected: either BUILD SUCCESS (no N+1s in main source) or a failure from `failOnDryRunResults=true`. Inspect the findings:
- Data table CSV: `target/rewrite/datatables/com.naturalist.rewrite.NoSelectInIteration$Findings.csv` (one row per finding — `sourcePath`, `enclosingType`, `enclosingMethod`, `select`, `iterationKind`, `suggestedBatchedSibling`).
- Marker diffs: `target/rewrite/rewrite.patch`.

- [ ] **Step 8: Triage each finding (repeat until the dry run is clean)**

For every row in the data table:
- **Real N+1** — a main-source `*QueryImpl` / controller fanning a select out — fix it to call the batched sibling once (`getByEntityNameSet` / `findByNameSet` / a domain `getBy…Names`), following `InsectImageQueryImpl` and the `getByParentNames` pattern in [domains/CLAUDE.md](../../domains/CLAUDE.md). Scope note: if the fix is more than a mechanical swap to an existing batched method, stop and raise it as its own follow-up rather than growing this task.
- **Genuinely un-batchable** — add the deferred `@Option List<String>` exclusion to `NoSelectInIteration` and list the site centrally in `naturalist.yml` (see design doc, "Escape hatch"), with a one-line justification. Only build the option if this case actually occurs.

Re-run `mvn -q rewrite:dryRun` after each fix until it reports nothing.

- [ ] **Step 9: Arm the gate**

Leave the `<recipe>com.naturalist.EnforceQueryHygiene</recipe>` entry in `<activeRecipes>` (added in Step 7) now that the dry run is clean — the gate is live. Confirm the full build is green under the armed gate:
```bash
mvn -q verify
```
Expected: BUILD SUCCESS.

- [ ] **Step 10: Update docs**

- In [kernels/CLAUDE.md](../../kernels/CLAUDE.md), under the `framework-test` N+1 note, add one line: the static backstop `NoSelectInIteration` in `tooling/naturalist-rewrite` (composite `EnforceQueryHygiene`) flags loop/stream fan-out of repository/query selects in main source at build time.
- Set this design doc's status line to "shipped".
- Update [docs/work-tracker.md](../../docs/work-tracker.md) per project convention.

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat(rewrite): arm the N+1 select gate (EnforceQueryHygiene) repo-wide

Triaged the main-source tree (fixes/exclusions as noted) and activated the
composite under rewrite:dryRun + failOnDryRunResults.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Select site = repo/query call, method-invocation + method-reference, writes excluded → Task 1 (`isSelectSite`, invocation) + Task 2 (member reference). ✓
- Iteration construct = classic loops + per-element stream ops (map/filter/flatMap/peek/forEach + Iterable/Map.forEach) → Task 1 (loops) + Task 2 (`FAN_OUT`, lambda + member-ref). ✓
- Lexical same-method scope; helper/recursion delegated to AspectJ → `enclosingIterationKind` stops at `J.MethodDeclaration`; `doesNotFlagSelectInHelperCalledFromLoop` (Task 1). ✓
- Main-source-only guard → Task 1 `visit(JavaSourceFile)` + `doesNotFlagLoopSelectInTestSource`. ✓
- No escape hatch initially; deferred yml `@Option` exclusion → Global Constraints + Task 4 Step 8. ✓
- SearchResult marker fails the gate → all flag fixtures assert the `/*~~(...)~~>*/` marker. ✓
- Findings data table (six columns) + CSV summary + fixture assertion → Task 3. ✓
- Separate composite `EnforceQueryHygiene`, wired into `<activeRecipes>` → Task 4. ✓
- Prove-the-gate fixtures (loop, stream map/forEach/filter, batched-clean, write-clean, helper-clean, test-source-clean, non-fanout-lambda-clean) → Tasks 1–2. ✓
- Repo-wide reckoning (dry run, triage, arm) → Task 4 Steps 7–9. ✓

**Placeholder scan:** none — every code/config step carries literal content. The lone "unused import" note in Task 3 Step 1 is explicitly flagged for omission.

**Type consistency:** `NoSelectInIteration`, `MESSAGE`, `isSelectSite(JavaType.Method)`, `enclosingIterationKind(Cursor)`, `matchesFanOut(Expression)`, `Findings.Row(sourcePath, enclosingType, enclosingMethod, select, iterationKind, suggestedBatchedSibling)`, `recordAndMark(...)`, and the stub constants (`ENTITY_REPOSITORY`, `ENTITY_QUERY`, `FOO_REPOSITORY`, `FOO_QUERY`) are named identically across Tasks 1–4. The `EnforceQueryHygiene` recipe name matches between `naturalist.yml`, the composite test, and the root pom. ✓

**Residual risk to watch during execution:**
- `MethodMatcher("java.util.stream.Stream *(..)")` matches on the static (interface) type of the stream receiver — correct for source-level `Stream<T>` expressions. If a fixture's stream type resolves to a subtype and a matcher misses, add the concrete interface to `FAN_OUT` (Task 2 Step 3).
- `spec.dataTable(Row.class, consumer)` is the `rewrite-test` API for capturing emitted rows; if the harness version exposes it as `dataTableAsCsv`, assert the CSV form instead (Task 3 Step 1).
- The root `rewrite-maven-plugin` must resolve the freshly-built `naturalist-rewrite`; Task 4 Step 6's install guards against a stale plugin dependency.
