# naturalist-rewrite OpenRewrite Enforcement — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `tooling/naturalist-rewrite` module of OpenRewrite recipes that enforce the ADR-001 test-data-graph invariants across the *whole reactor's source* (main + test, every module) via a `mvn rewrite:dryRun` CI gate that ArchUnit structurally cannot cover.

**Architecture:** A new top-level `tooling/` tree holds one module, `naturalist-rewrite`, that ships three imperative OpenRewrite recipes plus a declarative composite `com.naturalist.EnforceArchitecture`. The `rewrite-maven-plugin` is configured once in the root pom with the composite as its single active recipe and `naturalist-rewrite` as a plugin dependency. CI runs two passes: `mvn install -DskipTests` (compiles + installs the recipe jar and gives rewrite full type attribution), then `mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true` (fails on any pending change — a fix *or* a marker). Developers run `mvn rewrite:run` to auto-apply fixes.

**Tech Stack:** Java 25, Maven, OpenRewrite (`rewrite-maven-plugin` 6.45.0, `rewrite-recipe-bom` 3.36.0 → rewrite core 8.88.0), `rewrite-java` (recipe LST/visitors/JavaTemplate), `rewrite-test` (RewriteTest harness), JUnit Jupiter 6.0.3 (repo standard).

**Source-of-truth design:** [docs/plans/2026-08-23-naturalist-rewrite-enforcement-design.md](2026-08-23-naturalist-rewrite-enforcement-design.md).
**Invariant background:** [docs/plans/2026-08-23-archunit-testentitysource-enforcement-strategy.md](2026-08-23-archunit-testentitysource-enforcement-strategy.md).

## Global Constraints

- **Java release:** 25 (`maven.compiler.release=25`, already root-managed). Recipe module compiles at 25.
- **Group/version:** all modules are `com.naturalist` at `1.0.0-SNAPSHOT` (`${project.version}`).
- **No `Date.now`/random in recipes:** recipes are pure LST transforms; determinism is required (OpenRewrite reruns them).
- **Marker-not-corrupt floor:** any construct a recipe cannot confidently transform emits an `org.openrewrite.marker.SearchResult` marker instead of a wrong rewrite. A marker is a pending change → `dryRun` still fails → a human fixes that one site. Never silent-corrupt.
- **Auto-format modified LST:** any recipe that *structurally* modifies the LST must normalize the changed subtree's formatting so indentation/annotation placement stays consistent — `maybeAutoFormat(before, after, ctx)` for a same-type replacement (e.g. R2's `VariableDeclarations`→`VariableDeclarations`), or `autoFormat(newNode, ctx, parentCursor)` for a freshly-synthesized node of a different type (e.g. R1's `NewClass`→`MethodInvocation`). Marker-only additions (R3) do not restructure and are exempt.
- **Test-source scope for R2/R3:** these two rules target *test* code only; main-wired `NaturalistDatabase.create()` (console bootstraps, `TestDataConfiguration`) is out of scope and must not be flagged.
- **The gate cannot be armed while known violations exist.** `failOnDryRunResults=true` is wired only in Task 7, *after* the 39-site backlog is cleared in Task 6.
- **Recipe unit tests are in-scope and required** (they verify logic that will rewrite the whole repo) — this is *not* the "no test-infra-for-test-infra" case.

## File Structure

```
tooling/                                                  # NEW top-level tree
  pom.xml                                                 # aggregator, parent = amateur-naturalist
  naturalist-rewrite/
    pom.xml                                               # leaf, parent = tooling
    src/main/java/com/naturalist/rewrite/
      NoDirectTestEntitySourceConstruction.java           # R1  (auto-fix)
      NoCachedTestEntitySourceField.java                  # R3  (marker gate)
      AcquireDatabaseViaExtension.java                    # R2  (Case-A auto-fix + marker)
    src/main/resources/META-INF/rewrite/
      naturalist.yml                                      # composite com.naturalist.EnforceArchitecture
    src/test/java/com/naturalist/rewrite/
      NoDirectTestEntitySourceConstructionTest.java
      NoCachedTestEntitySourceFieldTest.java
      AcquireDatabaseViaExtensionTest.java
      NaturalistTypeStubs.java                            # shared stub-source constants for tests
pom.xml                                                   # MODIFY: add <module>tooling</module>,
                                                           #  dependencyManagement entry, rewrite plugin
```

Modified existing test files (Task 6, backlog clear): the 29 Case-B/C/D/A2 sites listed in that task.

---

### Task 1: Scaffold the `tooling/` tree and empty `naturalist-rewrite` module

**Files:**
- Create: `tooling/pom.xml`
- Create: `tooling/naturalist-rewrite/pom.xml`
- Modify: `pom.xml` (root — add module + dependencyManagement entry)

**Interfaces:**
- Produces: reactor module `com.naturalist:naturalist-rewrite:1.0.0-SNAPSHOT`, buildable and installable. Later tasks add classes to it.

- [ ] **Step 1: Create the `tooling/` aggregator pom**

`tooling/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>amateur-naturalist</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <packaging>pom</packaging>

    <artifactId>tooling</artifactId>
    <name>amateur-naturalist :: tooling</name>

    <properties>
        <rewrite-recipe-bom.version>3.36.0</rewrite-recipe-bom.version>
        <junit-version>6.0.3</junit-version>
        <assertj-version>3.26.0</assertj-version>
    </properties>

    <modules>
        <module>naturalist-rewrite</module>
    </modules>
</project>
```

- [ ] **Step 2: Create the `naturalist-rewrite` leaf pom**

`tooling/naturalist-rewrite/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>tooling</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>naturalist-rewrite</artifactId>
    <name>tooling :: naturalist-rewrite</name>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.openrewrite.recipe</groupId>
                <artifactId>rewrite-recipe-bom</artifactId>
                <version>${rewrite-recipe-bom.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- rewrite-test transitively pulls an older junit-jupiter-api/-params; pin the
                 whole JUnit family to the repo-standard version so it wins dependency mediation. -->
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>${junit-version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Recipe authoring: LST, visitors, JavaTemplate, MethodMatcher, markers. -->
        <dependency>
            <groupId>org.openrewrite</groupId>
            <artifactId>rewrite-java</artifactId>
        </dependency>
        <!-- Recipe testing harness. -->
        <dependency>
            <groupId>org.openrewrite</groupId>
            <artifactId>rewrite-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Concrete Java-25 parser RewriteTest's Assertions.java() needs on the test classpath. -->
        <dependency>
            <groupId>org.openrewrite</groupId>
            <artifactId>rewrite-java-25</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Repo-standard JUnit; declared explicitly so it wins over rewrite-test's transitive Jupiter. -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit-version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj-version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Register the module and manage its version in the root pom**

In `pom.xml`, add to `<modules>` (after `<module>kernels</module>`):
```xml
        <module>tooling</module>
```
In root `<dependencyManagement><dependencies>`, add (near the APPS block, for consistency):
```xml
            <!-- TOOLING -->
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>naturalist-rewrite</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Verify the empty module builds and installs**

Run: `mvn -q -pl tooling/naturalist-rewrite -am install -DskipTests`
Expected: BUILD SUCCESS; `~/.m2/repository/com/naturalist/naturalist-rewrite/1.0.0-SNAPSHOT/` now holds the jar.

- [ ] **Step 5: Commit**

```bash
git add tooling/pom.xml tooling/naturalist-rewrite/pom.xml pom.xml
git commit -m "build(tooling): scaffold naturalist-rewrite OpenRewrite module"
```

---

### Task 2: R1 — `NoDirectTestEntitySourceConstruction` (auto-fix)

Rewrites `new <X>TestEntitySource(db)` → `db.getNamed(<X>TestEntitySource.class)`. 0 live sites — a regression gate that showcases a clean auto-fix. In OpenRewrite a `super(db)` call is a `J.MethodInvocation`, not a `J.NewClass`, so — unlike ArchUnit — no base-class exclusion gymnastics are needed; `visitNewClass` never sees the subclass's own `super(...)`.

**Files:**
- Create: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NaturalistTypeStubs.java`
- Create: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoDirectTestEntitySourceConstructionTest.java`
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoDirectTestEntitySourceConstruction.java`

**Interfaces:**
- Produces: `com.naturalist.rewrite.NoDirectTestEntitySourceConstruction extends org.openrewrite.Recipe`, no-arg constructible (used by name from `naturalist.yml`).
- Consumes: nothing (Task 1 module).

- [ ] **Step 1: Write the shared stub-source constants**

RewriteTest needs the project types on its parse classpath so `MethodMatcher`/type checks resolve. Provide them as extra source strings. `NaturalistTypeStubs.java`:
```java
package com.naturalist.rewrite;

/** Minimal source stubs for the naturalist data types the recipes match against.
 *  Passed as unchanged supporting sources in RewriteTest so type attribution resolves. */
final class NaturalistTypeStubs {
    private NaturalistTypeStubs() {}

    static final String NATURALIST_DATABASE = """
        package com.naturalist.data;
        public class NaturalistDatabase {
            public static NaturalistDatabase create() { return new NaturalistDatabase(); }
            public <T> T getNamed(Class<T> sourceClass) { return null; }
        }
        """;

    static final String TEST_ENTITY_SOURCE = """
        package com.naturalist.data;
        public abstract class TestEntitySource {
            protected TestEntitySource(NaturalistDatabase database) {}
        }
        """;

    static final String NATURALIST_TEST_EXTENSION = """
        package com.naturalist.data;
        public class NaturalistTestExtension extends NaturalistDatabase {
            public static NaturalistTestExtension create() { return new NaturalistTestExtension(); }
        }
        """;

    static final String FOO_SOURCE = """
        package com.naturalist.data;
        public final class FooTestEntitySource extends TestEntitySource {
            public FooTestEntitySource(NaturalistDatabase database) { super(database); }
        }
        """;
}
```

- [ ] **Step 2: Write the failing test**

`NoDirectTestEntitySourceConstructionTest.java`:
```java
package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoDirectTestEntitySourceConstructionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoDirectTestEntitySourceConstruction());
    }

    @Test
    void rewritesNewToGetNamed() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            java(
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        FooTestEntitySource s = new FooTestEntitySource(db);
                    }
                }
                """,
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m(NaturalistDatabase db) {
                        FooTestEntitySource s = db.getNamed(FooTestEntitySource.class);
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesTheAbstractBaseSuperCallAlone() {
        // FooTestEntitySource's own `super(database)` is a method invocation, not a NewClass;
        // parsing the stub as-is must produce no change.
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE)   // no `after` => asserts unchanged
        );
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoDirectTestEntitySourceConstructionTest`
Expected: FAIL — `NoDirectTestEntitySourceConstruction` does not compile/exist.

- [ ] **Step 4: Write the recipe**

`NoDirectTestEntitySourceConstruction.java`:
```java
package com.naturalist.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class NoDirectTestEntitySourceConstruction extends Recipe {

    private static final String BASE = "com.naturalist.data.TestEntitySource";

    @Override
    public String getDisplayName() {
        return "Acquire a TestEntitySource via NaturalistDatabase#getNamed, never `new`";
    }

    @Override
    public String getDescription() {
        return "Rewrites `new <X>TestEntitySource(db)` to `db.getNamed(<X>TestEntitySource.class)` "
             + "so every source is registered in the shared NaturalistDatabase (ADR-001).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass nc = super.visitNewClass(newClass, ctx);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(nc.getType());
                if (type == null) {
                    return nc;
                }
                boolean isConcreteSource = !type.getFullyQualifiedName().equals(BASE)
                        && TypeUtils.isAssignableTo(BASE, nc.getType());
                if (!isConcreteSource) {
                    return nc;
                }
                if (nc.getArguments().size() != 1) {
                    return SearchResult.found(nc,
                        "unexpected TestEntitySource constructor arity; acquire via db.getNamed(...)");
                }
                Expression db = nc.getArguments().get(0);
                String simpleName = type.getClassName(); // simple name for top-level type
                J.MethodInvocation replacement = JavaTemplate
                        .builder("#{any(com.naturalist.data.NaturalistDatabase)}.getNamed(" + simpleName + ".class)")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), nc.getCoordinates().replace(), db);
                // Structural change (NewClass -> MethodInvocation): normalize formatting of the
                // freshly-synthesized node in its enclosing context.
                return autoFormat(replacement, ctx, getCursor().getParentOrThrow());
            }
        };
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoDirectTestEntitySourceConstructionTest`
Expected: PASS (both tests).

- [ ] **Step 6: Commit**

```bash
git add tooling/naturalist-rewrite/src
git commit -m "feat(rewrite): R1 recipe rewriting `new *TestEntitySource` to getNamed"
```

---

### Task 3: R3 — `NoCachedTestEntitySourceField` (marker gate)

Flags any **field** typed as a `TestEntitySource` subtype (stale across the `@BeforeEach` registry reset). Method-**locals** and getNamed-returning helper methods are the sanctioned idiom and must NOT be flagged. 0 live sites → ships as a detection gate (auto-inline is a documented future increment, Task 8).

**Files:**
- Create: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/NoCachedTestEntitySourceFieldTest.java`
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoCachedTestEntitySourceField.java`

**Interfaces:**
- Produces: `com.naturalist.rewrite.NoCachedTestEntitySourceField extends org.openrewrite.Recipe`, no-arg.

- [ ] **Step 1: Write the failing test** (field flagged; local + helper method not)

`NoCachedTestEntitySourceFieldTest.java`:
```java
package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoCachedTestEntitySourceFieldTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoCachedTestEntitySourceField());
    }

    @Test
    void flagsACachedSourceField() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            java(
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    private final FooTestEntitySource src = new NaturalistDatabase().getNamed(FooTestEntitySource.class);
                }
                """,
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    /*~~(cache TestEntitySource in a field; fetch via db.getNamed(...) inside each test so sources reset per test)~~>*/private final FooTestEntitySource src = new NaturalistDatabase().getNamed(FooTestEntitySource.class);
                }
                """
            )
        );
    }

    @Test
    void doesNotFlagMethodLocalOrHelper() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.TEST_ENTITY_SOURCE),
            java(NaturalistTypeStubs.FOO_SOURCE),
            java(
                // local var + helper method returning getNamed — both allowed, no change.
                """
                import com.naturalist.data.FooTestEntitySource;
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    private final NaturalistDatabase db = new NaturalistDatabase();
                    private FooTestEntitySource source() { return db.getNamed(FooTestEntitySource.class); }
                    void m() {
                        FooTestEntitySource local = db.getNamed(FooTestEntitySource.class);
                    }
                }
                """
            )
        );
    }
}
```
> Note: the `/*~~(...)~~>*/` prefix is exactly how RewriteTest renders a `SearchResult` marker in the `after` text. If the executor's OpenRewrite version renders the marker slightly differently, align the expected string to the actual rendering (run once, copy the produced marker text) — the assertion is that the field, and only the field, is marked.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoCachedTestEntitySourceFieldTest`
Expected: FAIL — recipe does not exist.

- [ ] **Step 3: Write the recipe**

`NoCachedTestEntitySourceField.java`:
```java
package com.naturalist.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class NoCachedTestEntitySourceField extends Recipe {

    private static final String BASE = "com.naturalist.data.TestEntitySource";
    private static final String MESSAGE =
        "cache TestEntitySource in a field; fetch via db.getNamed(...) inside each test so sources reset per test";

    @Override
    public String getDisplayName() {
        return "Do not cache a TestEntitySource in a field";
    }

    @Override
    public String getDescription() {
        return "A TestEntitySource-typed field is populated at construction, before the extension's "
             + "@BeforeEach registry reset, so it goes stale. Fetch from NaturalistDatabase#getNamed "
             + "inside each test instead. Method-locals and getNamed-returning helpers are allowed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(vd, ctx);
                if (!isField(getCursor())) {
                    return v;
                }
                JavaType elementType = v.getType();
                if (elementType != null
                        && !TypeUtils.isOfClassType(elementType, BASE)   // not the abstract base itself
                        && TypeUtils.isAssignableTo(BASE, elementType)) {
                    return SearchResult.found(v, MESSAGE);
                }
                return v;
            }

            /** A field declaration sits directly in a class body block. */
            private boolean isField(Cursor cursor) {
                Cursor parent = cursor.getParentTreeCursor();
                if (!(parent.getValue() instanceof J.Block)) {
                    return false;
                }
                return parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
            }
        };
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=NoCachedTestEntitySourceFieldTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add tooling/naturalist-rewrite/src
git commit -m "feat(rewrite): R3 recipe flagging cached TestEntitySource fields"
```

---

### Task 4: R2 — `AcquireDatabaseViaExtension` (Case-A auto-fix + marker for the rest)

Matches `NaturalistDatabase.create()` in test code. **Case A** — a field `NaturalistDatabase x = NaturalistDatabase.create()` — is auto-fixed to `@RegisterExtension NaturalistTestExtension x = NaturalistTestExtension.create()`. Every other matched `create()` (method-locals, nested args, inline chains, TestContext-wrapped fields) is **marked**; auto-hoisting those is the deferred Task 8. `NaturalistTestExtension.create()` is a different declared method and is never matched.

**Files:**
- Create: `tooling/naturalist-rewrite/src/test/java/com/naturalist/rewrite/AcquireDatabaseViaExtensionTest.java`
- Create: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/AcquireDatabaseViaExtension.java`

**Interfaces:**
- Produces: `com.naturalist.rewrite.AcquireDatabaseViaExtension extends org.openrewrite.Recipe`, no-arg.

- [ ] **Step 1: Write the failing tests** (Case A fixed; a local is marked; the extension path is untouched)

`AcquireDatabaseViaExtensionTest.java`:
```java
package com.naturalist.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AcquireDatabaseViaExtensionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AcquireDatabaseViaExtension());
    }

    @Test
    void caseA_fieldInitializerIsRetypedToTheExtension() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            java(
                """
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    private final NaturalistDatabase db = NaturalistDatabase.create();
                }
                """,
                """
                import com.naturalist.data.NaturalistTestExtension;
                import org.junit.jupiter.api.extension.RegisterExtension;
                class T {
                    @RegisterExtension
                    final NaturalistTestExtension db = NaturalistTestExtension.create();
                }
                """
            )
        );
    }

    @Test
    void methodLocalIsMarkedNotRewritten() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            java(
                """
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m() {
                        NaturalistDatabase db = NaturalistDatabase.create();
                    }
                }
                """,
                """
                import com.naturalist.data.NaturalistDatabase;
                class T {
                    void m() {
                        NaturalistDatabase db = /*~~(hoist a @RegisterExtension NaturalistTestExtension field; do not create a bare NaturalistDatabase in a test)~~>*/NaturalistDatabase.create();
                    }
                }
                """
            )
        );
    }

    @Test
    void extensionCreateIsNotFlagged() {
        rewriteRun(
            java(NaturalistTypeStubs.NATURALIST_DATABASE),
            java(NaturalistTypeStubs.NATURALIST_TEST_EXTENSION),
            java(
                """
                import com.naturalist.data.NaturalistTestExtension;
                class T {
                    private final NaturalistTestExtension db = NaturalistTestExtension.create();
                }
                """   // no `after` => unchanged
            )
        );
    }
}
```
> Same marker-rendering caveat as Task 3, Step 1: if the `/*~~(...)~~>*/` rendering differs in the executor's version, run once and copy the actual text.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=AcquireDatabaseViaExtensionTest`
Expected: FAIL — recipe does not exist.

- [ ] **Step 3: Write the recipe**

`AcquireDatabaseViaExtension.java`:
```java
package com.naturalist.rewrite;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Comparator;

public class AcquireDatabaseViaExtension extends Recipe {

    private static final MethodMatcher CREATE =
        new MethodMatcher("com.naturalist.data.NaturalistDatabase create()");
    private static final String DB = "com.naturalist.data.NaturalistDatabase";
    private static final String EXT = "com.naturalist.data.NaturalistTestExtension";
    private static final String MARK =
        "hoist a @RegisterExtension NaturalistTestExtension field; "
      + "do not create a bare NaturalistDatabase in a test";

    @Override
    public String getDisplayName() {
        return "Obtain the test NaturalistDatabase from a @RegisterExtension NaturalistTestExtension";
    }

    @Override
    public String getDescription() {
        return "In test code, a bare NaturalistDatabase.create() is not reset per test. A field "
             + "initializer is retyped to NaturalistTestExtension with @RegisterExtension; every other "
             + "shape (locals, nested arguments, inline chains) is marked for a manual hoist.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                // Case A: a field `NaturalistDatabase x = NaturalistDatabase.create();`
                if (isCaseAField(vd, getCursor())) {
                    J.VariableDeclarations retyped = JavaTemplate
                        .builder("@org.junit.jupiter.api.extension.RegisterExtension\n"
                               + "final com.naturalist.data.NaturalistTestExtension #{} = "
                               + "com.naturalist.data.NaturalistTestExtension.create()")
                        .contextSensitive()
                        .imports(EXT, "org.junit.jupiter.api.extension.RegisterExtension")
                        .build()
                        .apply(getCursor(), vd.getCoordinates().replace(),
                               vd.getVariables().get(0).getSimpleName());
                    maybeAddImport(EXT);
                    maybeAddImport("org.junit.jupiter.api.extension.RegisterExtension");
                    maybeRemoveImport(DB);
                    // Same-type replacement (field VariableDeclarations): normalize the
                    // added @RegisterExtension annotation + retyped declaration formatting.
                    return maybeAutoFormat(vd, retyped.withPrefix(vd.getPrefix()), ctx);
                }
                return super.visitVariableDeclarations(vd, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(mi, ctx);
                if (CREATE.matches(m)) {
                    // Case A initializers are handled in visitVariableDeclarations (which replaces
                    // this node before we get here); anything else that still matches is marked.
                    return SearchResult.found(m, MARK);
                }
                return m;
            }

            private boolean isCaseAField(J.VariableDeclarations vd, Cursor cursor) {
                if (!isField(cursor)) {
                    return false;
                }
                if (!TypeUtils.isOfClassType(vd.getType(), DB) || vd.getVariables().size() != 1) {
                    return false;
                }
                J.VariableDeclarations.NamedVariable var = vd.getVariables().get(0);
                return var.getInitializer() instanceof J.MethodInvocation
                    && CREATE.matches((J.MethodInvocation) var.getInitializer());
            }

            private boolean isField(Cursor cursor) {
                Cursor parent = cursor.getParentTreeCursor();
                if (!(parent.getValue() instanceof J.Block)) {
                    return false;
                }
                return parent.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
            }
        };
    }
}
```
> Implementation note for the executor: the delicate part is ensuring the Case-A field's initializer is *replaced by the template* so the child `create()` call is no longer a `NaturalistDatabase.create()` and therefore not marked. Because `visitVariableDeclarations` returns the fully rebuilt node **without** calling `super` on the Case-A branch, the visitor does not descend into the old initializer, so no stray marker appears. If a stray marker shows up in the Case-A test, that is the cause — keep the early return. Iterate against the three tests until green; adjust whitespace/formatting expectations in the test to match OpenRewrite's printer (run once, copy actual output).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -pl tooling/naturalist-rewrite test -Dtest=AcquireDatabaseViaExtensionTest`
Expected: PASS (all three).

- [ ] **Step 5: Commit**

```bash
git add tooling/naturalist-rewrite/src
git commit -m "feat(rewrite): R2 recipe (Case-A auto-fix + marker) for bare NaturalistDatabase.create()"
```

---

### Task 5: Composite recipe + wire the plugin (gate OFF) and observe the backlog

**Files:**
- Create: `tooling/naturalist-rewrite/src/main/resources/META-INF/rewrite/naturalist.yml`
- Modify: `pom.xml` (root — add `rewrite-maven-plugin`, `failOnDryRunResults` NOT yet set)

**Interfaces:**
- Produces: declarative recipe `com.naturalist.EnforceArchitecture`, resolvable by the plugin.

- [ ] **Step 1: Write the composite recipe**

`naturalist.yml`:
```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.naturalist.EnforceArchitecture
displayName: Naturalist architectural enforcement
description: >-
  Enforces the ADR-001 test-data-graph invariants: acquire TestEntitySources via
  NaturalistDatabase#getNamed, obtain the test database from a @RegisterExtension
  NaturalistTestExtension, and never cache a TestEntitySource in a field.
recipeList:
  - com.naturalist.rewrite.NoDirectTestEntitySourceConstruction
  - com.naturalist.rewrite.NoCachedTestEntitySourceField
  - com.naturalist.rewrite.AcquireDatabaseViaExtension
```

- [ ] **Step 2: Add the plugin to the root pom (gate OFF)**

In root `pom.xml`, inside `<build><plugins>` (a sibling of the existing `maven-dependency-plugin`):
```xml
            <plugin>
                <groupId>org.openrewrite.maven</groupId>
                <artifactId>rewrite-maven-plugin</artifactId>
                <version>6.45.0</version>
                <configuration>
                    <activeRecipes>
                        <recipe>com.naturalist.EnforceArchitecture</recipe>
                    </activeRecipes>
                    <!-- failOnDryRunResults intentionally omitted until the backlog is cleared (Task 7). -->
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.naturalist</groupId>
                        <artifactId>naturalist-rewrite</artifactId>
                        <version>${project.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
```
> No `<executions>` — no rewrite goal binds to a lifecycle phase, so `mvn verify` is unaffected. The plugin only runs when invoked explicitly (`rewrite:dryRun` / `rewrite:run`).

- [ ] **Step 3: Install everything, then dry-run to observe the backlog**

Run:
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun
```
Expected: BUILD SUCCESS (gate off). The dry-run report (`target/rewrite/rewrite.patch` per module, and console summary) shows:
- **R1:** 0 changes.
- **R3:** 0 changes.
- **R2:** ~39 results — ~10 Case-A fixes (real diffs) + ~29 markers (Cases B/C/D/A2).

Sanity-check the count against the design's census (39 real sites). If R1 or R3 report any change, investigate before proceeding — the repo was grep-clean for both.

- [ ] **Step 4: Commit**

```bash
git add tooling/naturalist-rewrite/src/main/resources pom.xml
git commit -m "build(rewrite): register EnforceArchitecture composite + wire rewrite-maven-plugin (gate off)"
```

---

### Task 6: Clear the 39-site backlog

Hand-migrate the ~29 marker sites to the `@RegisterExtension NaturalistTestExtension` field idiom FIRST, then `rewrite:run` to auto-apply the ~10 Case-A retypes. Only when `rewrite:dryRun` is clean can the gate be armed (Task 7).

**CRITICAL ordering rationale:** `rewrite:run` writes a recipe's output to disk — and a `SearchResult` marker prints as a `/*~~(...)~~>*/` comment. So running `rewrite:run` while the 29 non-Case-A sites still contain `NaturalistDatabase.create()` would litter those files with marker comments. Migrating the markers away first means the subsequent `rewrite:run` produces ONLY the Case-A field retypes (R1/R3 are clean, and the 29 no longer match), with no marker comments written.

**Files (the ~29 marker sites — verify the live set with the grep in Step 1 before editing):**
- Modify: the Case-B/C/D/A2 test files under `domains/*/*/src/test/**`, `kernels/framework-test/src/test/**`, `adapters/spring-test-data/src/test/**`, `apps/*/src/test/**` that call `NaturalistDatabase.create()`. (Task 5's census: markers in `kernels/framework-test` (6), `adapters/spring-test-data` (1), chemistry (2), garden (1), `soil-console`/`soil-test-context` (several), `plants-core` (3), plus the plants-console inline/list/breadcrumb tests.)

- [ ] **Step 1: Enumerate every live site and separate Case A from the rest**

Run:
```bash
git grep -nE "NaturalistDatabase\.create\(\)" -- '*/src/test/*.java' | grep -v DataForkComplianceTest
```
The ~10 Case-A sites are field declarations of the exact shape `<modifiers> NaturalistDatabase <name> = NaturalistDatabase.create();` (all four `plants-console` `Plants*DetailTemplateTest` + the six `plants-repository-test` `*CatalogDataTest`). Everything else (~29) is a marker site to hand-migrate in Step 2. Leave the Case-A fields ALONE — `rewrite:run` handles them in Step 3.

- [ ] **Step 2: Hand-migrate every NON-Case-A site to the field idiom**

For each marker site, apply the reset-per-test field pattern:
```java
// after (class field):
@org.junit.jupiter.api.extension.RegisterExtension
final NaturalistTestExtension db = NaturalistTestExtension.create();
```
Concretely:
- **Case B (method-local `NaturalistDatabase db = NaturalistDatabase.create();`):** hoist the local to a `@RegisterExtension NaturalistTestExtension` field; delete the local; references resolve to the field.
- **Case C (nested arg `SomeTestContext.create(NaturalistDatabase.create())` as a local/return):** add the field, replace the inner `NaturalistDatabase.create()` argument with the field reference `db`.
- **Case A2 (field `= SomeTestContext.create(NaturalistDatabase.create())`):** add a separate `@RegisterExtension NaturalistTestExtension db` field, and change the TestContext field's initializer to `SomeTestContext.create(db)`.
- **Case D (inline `NaturalistDatabase.create().foo(...)`):** add the field, replace `NaturalistDatabase.create()` with `db`.

Add imports `com.naturalist.data.NaturalistTestExtension` and `org.junit.jupiter.api.extension.RegisterExtension`; remove the now-unused `NaturalistDatabase` import where it no longer appears.

> This mirrors the migration already done in commit `1a7b4230` (14 fixture tests moved onto `NaturalistTestExtension`) — follow those files as the reference shape.

- [ ] **Step 3: Auto-apply the Case-A retypes**

Run (long timeout, whole reactor):
```bash
mvn -q install -DskipTests
mvn -q rewrite:run
```
Then review the diff and assert two things:
```bash
git diff --stat
git diff | grep -n '~~>' && echo "!!! marker comments were written — investigate" || echo "OK: no marker comments written to source"
```
Expected: ONLY the ~10 Case-A field declarations retyped to `@RegisterExtension final NaturalistTestExtension <name> = NaturalistTestExtension.create();`, and NO `/*~~(...)~~>*/` marker comments anywhere (because Step 2 removed every non-Case-A `create()`). If any marker comment appears, a site was missed in Step 2 — migrate it and re-run.

- [ ] **Step 4: Clean any dead imports the Case-A retype left**

The R2 recipe retypes the field but may leave an unused `com.naturalist.data.NaturalistDatabase` import when no other reference remains in that file (known Minor). Remove any now-unused `NaturalistDatabase` import in the Case-A files:
```bash
# For each Case-A file rewrite:run touched, if NaturalistDatabase no longer appears in the body, drop its import.
git diff --name-only
```

- [ ] **Step 5: Verify the whole reactor compiles and tests pass**

Run (long timeout): `mvn verify`
Expected: BUILD SUCCESS. (JUnit re-creates the test instance per method, so each `@RegisterExtension` field is fresh and reset per test.) If a failure is clearly pre-existing and unrelated to the migration, capture it and report — do not fix unrelated modules.

- [ ] **Step 6: Verify the dry-run is now clean**

Run (long timeout):
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun
```
Expected: no results for any recipe — no fixes pending, no markers, whole reactor.

- [ ] **Step 7: Commit**

Stage only the migrated/retyped test files (NOT the untracked docs or other unrelated tree changes). Use explicit paths from `git diff --name-only`:
```bash
git add <each migrated/retyped test file>
git commit -m "refactor(test): migrate all bare NaturalistDatabase.create() to @RegisterExtension field idiom"
```

---

### Task 7: Scope R2/R3 to test sources, then arm the gate and document the CI step

**Prerequisite discovered in Task 6:** the whole-reactor `rewrite:dryRun` is zero in *test* sources but still reports **3 `src/main` over-matches** that R2 must never touch — `adapters/spring-test-data/.../TestDataConfiguration.java` (a Spring `@Bean` calling `NaturalistDatabase.create()`), `kernels/framework-test/.../TestEntitySourceTest.java` (the main-source contract base, a sanctioned `create()` caller per ADR-001 / the ArchUnit whitelist), and a Javadoc `{@link NaturalistDatabase#create()}` in `kernels/framework-test/.../NaturalistTestExtension.java`. The plan's Global Constraint always said R2/R3 are **test-source-scoped**, but the recipes never enforced it. `failOnDryRunResults=true` cannot be armed until these three are excluded — so scope the recipes to the test source set first.

**Files:**
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/AcquireDatabaseViaExtension.java` (add test-source-set guard)
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoCachedTestEntitySourceField.java` (add test-source-set guard)
- Modify: `tooling/naturalist-rewrite/src/test/.../AcquireDatabaseViaExtensionTest.java` and `NoCachedTestEntitySourceFieldTest.java` (wrap fixtures in `srcTestJava(...)`; add a `srcMainJava(...)` no-op case per recipe)
- Modify: `pom.xml` (root — set `failOnDryRunResults`)
- Modify: `docs/plans/2026-08-23-naturalist-rewrite-enforcement-design.md` (mark Status: implemented; record the CI command actually used)

> **Leave R1 (`NoDirectTestEntitySourceConstruction`) UNSCOPED** — `new *TestEntitySource(...)` is forbidden in *all* source (ADR-001; the ArchUnit `DataForkComplianceTest` enforces it on main too). Only R2 and R3 are test conventions.

- [ ] **Step 1: Add a test-source-set guard to R2 and R3**

OpenRewrite tags each parsed source file with an `org.openrewrite.java.marker.JavaSourceSet` marker whose `getName()` is `"main"` or `"test"`. Guard each recipe's visitor so it only acts on `"test"` sources. Add to BOTH `AcquireDatabaseViaExtension` and `NoCachedTestEntitySourceField`, at the top of the visitor, an override that short-circuits non-test files:
```java
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaSourceFile;
// ...
@Override
public J visit(@Nullable org.openrewrite.Tree tree, ExecutionContext ctx) {
    if (tree instanceof JavaSourceFile) {
        boolean isTest = ((JavaSourceFile) tree).getMarkers()
                .findFirst(JavaSourceSet.class)
                .map(s -> "test".equals(s.getName()))
                .orElse(false);
        if (!isTest) {
            return (J) tree; // main / unknown source set: do nothing
        }
    }
    return super.visit(tree, ctx);
}
```
(Use whatever idiom compiles cleanly against rewrite core 8.88.0 — e.g. `Preconditions`/`JavaIsoVisitor` may offer a tidier hook; the requirement is: the recipe makes no change to any file whose source set is not `test`.)

- [ ] **Step 2: Update the recipe unit tests for source-set scoping**

RewriteTest fixtures carry no source set by default, so the guard would make every test a no-op. Wrap each behavioral fixture in `org.openrewrite.java.Assertions.srcTestJava(...)` so it is seen as a test source. Add one negative case per recipe using `srcMainJava(...)` proving the same offending shape is NOT changed in main source:
```java
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.java.Assertions.srcMainJava;
// existing behavioral fixtures: wrap the java(before, after) in srcTestJava(...)
// new: rewriteRun(srcMainJava(java("<the offending shape>")))  // asserts no change in main
```
Run: `mvn -q -pl tooling/naturalist-rewrite test`
Expected: all recipe tests green — R2 Case-A still fixed (in test source), markers still fire (in test source), and the new main-source cases assert no change.

- [ ] **Step 3: Rebuild and confirm the whole-reactor dry-run is now globally zero**

Run (long timeout):
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun
```
Expected: ZERO results for every recipe across the ENTIRE reactor — the 3 `src/main` over-matches are gone (excluded by the test-source guard) and test sources remain clean (Task 6 cleared them). If any finding remains, investigate before arming.

- [ ] **Step 4: Commit the scoping fix**

```bash
git add tooling/naturalist-rewrite/src
git commit -m "fix(rewrite): scope R2/R3 to the test source set (exclude main-wired create() + contract base)"
```

- [ ] **Step 5: Turn on the failure gate**

In root `pom.xml`, replace the commented placeholder in the plugin `<configuration>` with:
```xml
                    <failOnDryRunResults>true</failOnDryRunResults>
```

- [ ] **Step 6: Prove the gate is green on a clean tree**

Run (long timeout):
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true
```
Expected: BUILD SUCCESS (no pending changes).

- [ ] **Step 7: Prove the gate actually fails on a violation (negative test)**

Temporarily introduce one violation in a **test** file (e.g. add a method-local `NaturalistDatabase x = NaturalistDatabase.create();` to an existing `src/test` class), then:
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true
```
Expected: BUILD FAILURE citing the R2 marker. Then revert the temporary edit:
```bash
git checkout -- <the file you edited>
```

- [ ] **Step 8: Record the CI invocation**

The pipeline gate (add to the project's CI config / document in the design doc) is exactly:
```bash
mvn install -DskipTests
mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true
```
Update the design doc: set `Status: implemented (<commit>)`, and note that `mvn rewrite:run` is the developer auto-fix command.

- [ ] **Step 9: Commit**

```bash
git add pom.xml docs/plans/2026-08-23-naturalist-rewrite-enforcement-design.md
git commit -m "build(rewrite): arm failOnDryRunResults gate for EnforceArchitecture"
```

---

### Task 8: Add `Preconditions` (UsesType/UsesMethod) to all three recipes

Per OpenRewrite best practices (https://docs.openrewrite.org/authoring-recipes/recipe-conventions-and-best-practices#use-preconditions), each recipe should wrap its visitor in `Preconditions.check(precondition, visitor)` so the LST walk is skipped entirely for files that cannot match. Today all three return a bare visitor that walks every file. This is a **pure performance optimization** — it must not change *what* any recipe flags or fixes.

**Files:**
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoDirectTestEntitySourceConstruction.java`
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/AcquireDatabaseViaExtension.java`
- Modify: `tooling/naturalist-rewrite/src/main/java/com/naturalist/rewrite/NoCachedTestEntitySourceField.java`
- (Tests should need no change beyond staying green; RewriteTest runs the full recipe including its preconditions.)

- [ ] **Step 1: Wrap each recipe's visitor in `Preconditions.check`**

Pattern (from the best-practices page):
```java
import org.openrewrite.Preconditions;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.search.UsesMethod;
// ...
@Override
public TreeVisitor<?, ExecutionContext> getVisitor() {
    return Preconditions.check(<precondition>, <the existing visitor>);
}
```
Preconditions to use:
- **R1 `NoDirectTestEntitySourceConstruction`:** `new UsesType<>("com.naturalist.data.TestEntitySource", true)` — files that use a `TestEntitySource` (the `true`/includeImplicit flag also admits uses of subtypes, which is how `new <X>TestEntitySource(...)` sites appear). Applies to all source (R1 is unscoped).
- **R2 `AcquireDatabaseViaExtension`:** `new UsesMethod<>("com.naturalist.data.NaturalistDatabase create()")` — files that call `NaturalistDatabase.create()`. Keep the existing `"test"` source-set short-circuit inside the visitor (the precondition and the source-set gate compose).
- **R3 `NoCachedTestEntitySourceField`:** `new UsesType<>("com.naturalist.data.TestEntitySource", true)`. Keep the existing `"test"` source-set short-circuit inside the visitor.

Do not remove the R2/R3 source-set guards — `Preconditions` handles "does this file use the type", the guard handles "is this a test source".

- [ ] **Step 2: Unit tests stay green (this is the primary proof the precondition does not over-exclude)**

Run: `mvn -q -pl tooling/naturalist-rewrite test`
Expected: all recipe tests green. Because RewriteTest executes the full recipe *including* the precondition, a passing `caseA_fieldInitializerIsRetypedToTheExtension` / `rewritesNewToGetNamed` / `flagsACachedSourceField` proves the precondition still admits genuinely-matching files. If any behavioral test now no-ops, the precondition is too narrow — fix it.

- [ ] **Step 3: Whole-reactor dry-run stays globally zero (gate still green)**

Run (long timeout):
```bash
mvn -q install -DskipTests
mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true
```
Expected: BUILD SUCCESS — identical to the armed post-Task-7 state. Preconditions must not introduce or remove any finding.

- [ ] **Step 4: Confirm the precondition does not neuter detection (inject-and-revert spot check)**

Because the live census is zero, add a temporary violation of each kind in an existing `src/test` file and confirm the gate still catches it WITH preconditions in place, then revert:
```bash
# e.g. add a method-local `NaturalistDatabase x = NaturalistDatabase.create();` (R2)
#      and a `new SomeConcreteTestEntitySource(db)` (R1) in a test class
mvn -q install -DskipTests && mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true   # expect BUILD FAILURE
git checkout -- <the file>                                                                # revert
```
Report the observed failure(s). This proves `UsesType`/`UsesMethod` admit the matching files rather than silently skipping them.

- [ ] **Step 5: Commit**

```bash
git add tooling/naturalist-rewrite/src
git commit -m "perf(rewrite): gate recipes with Preconditions (UsesType/UsesMethod) per OpenRewrite best practices"
```

---

### Task 9 (DEFERRED / documented, not executed now): general field-hoister for R2 Cases B/C/D/A2

**Not part of the initial shippable gate.** Once armed (Task 7), Cases B/C/D/A2 regressions are marker-gated and hand-fixed — rare, since the idiom is now house standard. If auto-fix for those shapes is later wanted (to save tokens on future regressions), extend `AcquireDatabaseViaExtension` to hoist a `@RegisterExtension NaturalistTestExtension` field:

- Convert R2 into a `ScanningRecipe<Accumulator>` (or a two-visitor pass): first pass records, per class, whether a suitable extension field already exists and a collision-free field name; second pass inserts the field (once per class) and replaces each matched `create()` expression with a reference to it, deleting dead locals.
- Keep the marker fallback for any class where a field cannot be safely placed (e.g. no class body reachable, name collision unresolved).
- Add RewriteTest cases for each of Cases B, C, D, A2 (before/after) plus a collision case.

Track as a follow-up; it does not block the gate.

---

## Self-Review

**Spec coverage:**
- General-purpose `tooling/naturalist-rewrite` home, composite recipe → Tasks 1, 5. ✅
- R1 auto-fix (regression gate) → Task 2. ✅
- R2 fix-where-safe (Case A) + marker floor (B/C/D/A2), test-scope, extension path excluded → Task 4; backlog clear → Task 6. ✅
- R3 field-only rule with local/helper allow-list (marker gate) → Task 3. ✅
- Two-pass CI + `failOnDryRunResults` gate, armed only after backlog clear → Tasks 5, 6, 7. ✅
- Keep ArchUnit `DataForkComplianceTest` (untouched); drop per-module Strategy A (never added). ✅ (no task needed — nothing to change)
- "Fix everything possible" for B/C/D deferred as Task 8 with a concrete design, per the sequencing reality that a general hoister is a large separate effort. ⚠️ (surfaced to the user in handoff)

**Placeholder scan:** every recipe/test/pom step carries full content; no TBD/TODO. The two "adjust the marker string to match your version" notes are legitimate version-rendering guidance, not missing content. ✅

**Type consistency:** `NoDirectTestEntitySourceConstruction`, `NoCachedTestEntitySourceField`, `AcquireDatabaseViaExtension` used identically in yml, tests, and plugin; recipe FQNs `com.naturalist.rewrite.*`; composite name `com.naturalist.EnforceArchitecture`; artifact `com.naturalist:naturalist-rewrite`; matcher target `com.naturalist.data.NaturalistDatabase create()`. ✅

## Notes / risks for the executor

- **Java 25 parsing:** rewrite core 8.88.0 (via BOM 3.36.0) supports Java 25. If parsing errors appear on modern syntax, bump `rewrite-recipe-bom.version` and the plugin `version` together to the latest aligned pair.
- **JUnit 6 vs rewrite-test's transitive Jupiter:** the explicit `junit-jupiter` 6.0.3 test dep is nearest-wins; if a version clash surfaces, add an exclusion on rewrite-test's `junit-jupiter-api`.
- **Marker text rendering** in RewriteTest `after` blocks may differ slightly by version — run the test once, copy the exact `/*~~(...)~~>*/` the printer emits, paste into the expected string.
- **Type attribution for `rewrite:dryRun`:** always precede it with `mvn install -DskipTests` (or a full reactor compile) so R1/R3 subtype checks and the R2 `MethodMatcher` resolve against real types; weak attribution can miss or mis-flag.
