# RDBMS Persistence Kernel + Naturalists Pilot — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the project's first real RDBMS repository adapter (Postgres + MyBatis) as an Anti-Corruption-Layer vertical slice through the `naturalists` domain, verified by the existing `EntityRepositoryTest` contract re-run against real Postgres.

**Architecture:** Two new kernels (`kernels/persistence`, `kernels/persistence-test`) provide the reusable RDBMS machinery — DBO annotations, MyBatis `SqlSessionFactory` assembly, a connect-and-rollback JUnit extension, and a `SchemaApplier`. `naturalists-repository-rdbms` holds the domain's DBOs (plain snake_case POJOs, auto-mapped, no type handlers), hand-authored annotated SQL mappers, hand DDL, and the two repository adapters. A manually-invoked `apps/test-db-seeder` materializes the JSON seed into a standing Postgres by replaying each entity through the adapter's own `insert()`. The rdbms contract tests run as Failsafe `*IT`s in `mvn verify`.

**Tech Stack:** Java 25, MyBatis 3.5, PostgreSQL JDBC 42.7, HikariCP, JUnit 5, AssertJ, Maven.

## Global Constraints

- **Java version:** 25 (records, text blocks, switch expressions all available).
- **Typed identifiers never cross a boundary as raw String/Long/UUID** — only *inside* a DBO, which is package-private and never leaves the persistence layer.
- **No MyBatis type handlers and no `@Results`/`@ResultMap`** — DBO field names are snake_case, identical to column names; MyBatis auto-mapping does everything. Do not enable `mapUnderscoreToCamelCase`.
- **DBOs are package-private** in `naturalists-repository-rdbms`, in package `com.naturalist.naturalist` (split-package, to reach the package-private repository interfaces).
- **New-module checklist (domains/CLAUDE.md):** every new module must be (a) added to its parent pom `<modules>`, and (b) given a root `<dependencyManagement>` entry at `${project.version}`. Third-party versions go in root `<properties>` using the existing hyphen style (e.g. `<mybatis-version>`), referenced as `${mybatis-version}`.
- **Never weaken a test or gate to make code pass** (root CLAUDE.md non-negotiable).
- **Design source of truth:** `docs/plans/2026-08-27-rdbms-persistence-kernel-naturalists-design.md`.
- **Connection config (all reads from env, with defaults):** `NATURALIST_TEST_JDBC_URL` (default `jdbc:postgresql://localhost:5432/naturalist_test`), `NATURALIST_TEST_DB_USER` (default `postgres`), `NATURALIST_TEST_DB_PASSWORD` (default `postgres`).
- **Local Postgres required for `mvn verify`** (not `mvn test`). Either a Docker container
  (`docker run --name naturalist-pg -e POSTGRES_DB=naturalist_test -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16`)
  or a Homebrew `postgresql@18` service. With Homebrew, create the matching role + DB once so
  the connection defaults work:
  `createuser -s postgres 2>/dev/null; psql -d postgres -c "ALTER ROLE postgres PASSWORD 'postgres';"; createdb -O postgres naturalist_test`
  The seeder's DDL creates the tables; the database and role must exist first.

---

## File Structure

**`kernels/framework`** (modify)
- `.../observability/constraints/StringLengthLessThanConstraint.java` — new constraint.
- `.../observability/Constraints.java` — add `maxLength(...)` builder methods.

**`kernels/persistence`** (new — prod support)
- `.../persistence/DboSchema.java`, `Fk.java` — pure-metadata annotations.
- `.../persistence/Dbo.java` — `interface Dbo extends Observable`.
- `.../persistence/MyBatisSupport.java` — build a `SqlSessionFactory` from a `DataSource` + mapper classes.
- `.../persistence/RdbmsExceptions.java` — SQLState→domain-exception translation helper.

**`kernels/persistence-test`** (new — test support)
- `.../persistence/test/RdbmsDataSource.java` — shared HikariCP `DataSource` from env.
- `.../persistence/test/RdbmsTestExtension.java` — connect + per-test transaction rollback; `mapper(Class)`.
- `.../persistence/test/SchemaApplier.java` — run a `.sql` resource on a `Connection`.

**`domains/naturalists/naturalists-repository-rdbms`** (new — prod adapter)
- `src/main/resources/schema/naturalists.sql` — hand DDL (drop + create).
- `.../naturalist/NaturalistDbo.java`, `NaturalistCredentialDbo.java` — DBOs.
- `.../naturalist/NaturalistMapper.java`, `NaturalistCredentialMapper.java` — annotated SQL.
- `.../naturalist/NaturalistEntityRepositoryRdbms.java`, `NaturalistCredentialRepositoryRdbms.java` — adapters.
- `src/test/java/.../naturalist/NaturalistDboTest.java`, `NaturalistCredentialDboTest.java` — DBO round-trip + validation unit tests (no DB).
- `src/test/java/.../naturalist/NaturalistEntityRepositoryRdbmsIT.java`, `NaturalistCredentialRepositoryRdbmsIT.java` — contract `*IT`s (need seeded DB).

**`apps/test-db-seeder`** (new — manual tool)
- `.../seeder/TestDbSeeder.java` — `main()`: apply schema, replay JSON entities through `insert()`, commit.

**Build/CI**
- Root `pom.xml`, `kernels/pom.xml`, `domains/naturalists/pom.xml`, `apps/pom.xml` (or `naturalist-apps`) — module + dependency-management registration.
- `.github/workflows/ci.yml` — Postgres service + seeder step.

---

## Task 1: `StringLengthLessThanConstraint` in `kernels/framework`

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/observability/constraints/StringLengthLessThanConstraint.java`
- Modify: `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`
- Test: `kernels/framework/src/test/java/com/naturalist/observability/constraints/StringLengthLessThanConstraintTest.java`

**Interfaces:**
- Produces: `Constraints.maxLength(String value, int max, String name)` and `Constraints.maxLength(T t, Function<T,String> valueFunction, int max, String name)` — null-tolerant, asserts `value == null || value.length() <= max`.

- [ ] **Step 1: Write the failing test**

```java
package com.naturalist.observability.constraints;

import org.junit.jupiter.api.Test;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.assertThat;

class StringLengthLessThanConstraintTest {

    private static StringLengthLessThanConstraint<String> c(String v, int max) {
        return new StringLengthLessThanConstraint<>(v, Function.identity(), max, "field");
    }

    @Test void nullValue_isValid() { assertThat(c(null, 5).isValid()).isTrue(); }

    @Test void atLimit_isValid() { assertThat(c("abcde", 5).isValid()).isTrue(); }

    @Test void underLimit_isValid() { assertThat(c("abc", 5).isValid()).isTrue(); }

    @Test void overLimit_isInvalid() { assertThat(c("abcdef", 5).isValid()).isFalse(); }

    @Test void overLimit_errorMessageNamesTheLengths() {
        assertThat(c("abcdef", 5).errorMessage()).contains("6").contains("5");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl kernels/framework test -Dtest=StringLengthLessThanConstraintTest`
Expected: FAIL — `StringLengthLessThanConstraint` does not exist (compile error).

- [ ] **Step 3: Create the constraint (mirror `NotBlankConstraint`)**

```java
package com.naturalist.observability.constraints;

import com.naturalist.observability.Constraint;
import java.util.function.Function;

/**
 * Asserts a string's length does not exceed {@code max} (inclusive), mirroring a
 * {@code VARCHAR(max)} column width. Null-tolerant: a null value passes — pair with
 * {@link com.naturalist.observability.Constraints#notNull} when presence is also required.
 */
public record StringLengthLessThanConstraint<T>(
        T o,
        Function<T, String> valueFunction,
        int max,
        String name
) implements Constraint<String> {

    @Override
    public String value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        String v = value();
        return v == null || v.length() <= max;
    }

    @Override
    public StringLengthLessThanConstraint<T> withName(String name) {
        return new StringLengthLessThanConstraint<>(o, valueFunction, max, name);
    }

    @Override
    public String errorMessage() {
        String v = value();
        return v == null ? "" : "Length %d exceeds max %d".formatted(v.length(), max);
    }
}
```

- [ ] **Step 4: Add the `maxLength` builder to `Constraints`**

In `Constraints.java`, after the `notBlank(...)` methods, add:

```java
    /**
     * Asserts the string's length does not exceed {@code max} (inclusive), matching a
     * {@code VARCHAR(max)} column. Null values pass — pair with {@link #notNull} when
     * presence is also required.
     */
    public Constraints maxLength(String value, int max, String name) {
        return maxLength(value, Function.identity(), max, name);
    }

    public <T> Constraints maxLength(T t, Function<T, String> valueFunction, int max, String name) {
        return add(new StringLengthLessThanConstraint<>(t, valueFunction, max, name));
    }
```

(`Constraints` already imports `com.naturalist.observability.constraints.*` and `java.util.function.Function`.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q -pl kernels/framework test -Dtest=StringLengthLessThanConstraintTest`
Expected: PASS (5 tests).

- [ ] **Step 6: Add a builder-wiring test**

Append to the test class:

```java
    @Test void constraintsBuilder_maxLength_flagsOverLimit() {
        var observer = com.naturalist.observability.Observer.forClass(StringLengthLessThanConstraintTest.class);
        assertThat(observer.arguments("t", i -> i.maxLength("abcdef", 5, "field")).violations()).isNotEmpty();
        assertThat(observer.arguments("t", i -> i.maxLength("abc", 5, "field")).violations()).isEmpty();
    }
```

Run: `mvn -q -pl kernels/framework test -Dtest=StringLengthLessThanConstraintTest`
Expected: PASS (6 tests).

- [ ] **Step 7: Commit**

```bash
git add kernels/framework/src/main/java/com/naturalist/observability/constraints/StringLengthLessThanConstraint.java \
        kernels/framework/src/main/java/com/naturalist/observability/Constraints.java \
        kernels/framework/src/test/java/com/naturalist/observability/constraints/StringLengthLessThanConstraintTest.java
git commit -m "feat(framework): add StringLengthLessThanConstraint + Constraints.maxLength"
```

---

## Task 2: `kernels/persistence` — annotations, `Dbo`, MyBatis assembly

**Files:**
- Create: `kernels/persistence/pom.xml`
- Create: `.../persistence/DboSchema.java`, `Fk.java`, `Dbo.java`, `MyBatisSupport.java`, `RdbmsExceptions.java`
- Modify: `kernels/pom.xml` (add module), root `pom.xml` (properties + dependencyManagement)
- Test: `.../persistence/MyBatisSupportTest.java` (H2-free smoke is out of scope; a construction test that the factory builds is enough)

**Interfaces:**
- Produces:
  - `@DboSchema(table, primaryKey, unique, foreignKeys)` and `@Fk(columns, references)` annotations.
  - `interface Dbo extends com.naturalist.observability.Observable {}`.
  - `MyBatisSupport.sessionFactory(DataSource ds, Class<?>... mappers) -> org.apache.ibatis.session.SqlSessionFactory`.
  - `RdbmsExceptions.isUniqueViolation(Throwable) -> boolean` (Postgres SQLState `23505`).

- [ ] **Step 1: Add versions to root `pom.xml` `<properties>`**

Add (hyphen style, matching existing):

```xml
        <mybatis-version>3.5.16</mybatis-version>
        <postgresql-version>42.7.4</postgresql-version>
        <hikaricp-version>5.1.0</hikaricp-version>
```

- [ ] **Step 2: Add the module + dependencyManagement entries**

In `kernels/pom.xml` `<modules>`, add `<module>persistence</module>` (alphabetical). In root `pom.xml` `<dependencyManagement>` KERNELS section, add:

```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>persistence</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 3: Create `kernels/persistence/pom.xml`**

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>kernels</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>persistence</artifactId>
    <name>kernels :: persistence</name>
    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis</groupId>
            <artifactId>mybatis</artifactId>
            <version>${mybatis-version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql-version}</version>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>${hikaricp-version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: Create the annotations and `Dbo` marker**

`DboSchema.java`:

```java
package com.naturalist.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Pure, inert persistence metadata recorded alongside a {@link Dbo}. Generates nothing. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DboSchema {
    String table();
    String primaryKey();
    String[] unique() default {};
    Fk[] foreignKeys() default {};
}
```

`Fk.java`:

```java
package com.naturalist.persistence;

/** One foreign-key edge for {@link DboSchema}. `references` is e.g. "naturalist(id)". */
public @interface Fk {
    String columns();
    String references();
}
```

`Dbo.java`:

```java
package com.naturalist.persistence;

import com.naturalist.observability.Observable;

/** Marker for a database object: a plain, snake_case, auto-mapped persistence POJO that
 *  declares its own {@code invariants()} so the adapter can validate it at the ACL boundary. */
public interface Dbo extends Observable {
}
```

- [ ] **Step 5: Create `MyBatisSupport` and `RdbmsExceptions`**

`MyBatisSupport.java`:

```java
package com.naturalist.persistence;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

/** Builds a MyBatis {@link SqlSessionFactory} for the naturalist schema. Auto-mapping only:
 *  underscore-to-camel is OFF because DBO field names already equal the snake_case columns. */
public final class MyBatisSupport {

    private MyBatisSupport() {}

    public static SqlSessionFactory sessionFactory(DataSource dataSource, Class<?>... mappers) {
        Environment environment = new Environment("naturalist", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setMapUnderscoreToCamelCase(false);
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }
}
```

`RdbmsExceptions.java`:

```java
package com.naturalist.persistence;

import java.sql.SQLException;

/** Translates driver/SQLState signals into decisions the adapter maps to domain exceptions. */
public final class RdbmsExceptions {

    private RdbmsExceptions() {}

    private static final String UNIQUE_VIOLATION = "23505"; // Postgres

    /** True if {@code t}'s cause chain carries a SQLState {@code 23505} (unique/PK violation). */
    public static boolean isUniqueViolation(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 6: Build the module**

Run: `mvn -q -pl kernels/persistence -am install -DskipTests`
Expected: BUILD SUCCESS (a kernel signature was added — clean install per project convention).

- [ ] **Step 7: Commit**

```bash
git add kernels/persistence/ kernels/pom.xml pom.xml
git commit -m "feat(persistence): new kernel — DboSchema/Fk/Dbo annotations + MyBatis assembly"
```

---

## Task 3: `kernels/persistence-test` — extension, DataSource, SchemaApplier

**Files:**
- Create: `kernels/persistence-test/pom.xml`
- Create: `.../persistence/test/RdbmsDataSource.java`, `RdbmsTestExtension.java`, `SchemaApplier.java`
- Modify: `kernels/pom.xml`, root `pom.xml`

**Interfaces:**
- Produces:
  - `RdbmsDataSource.shared() -> javax.sql.DataSource` (HikariCP, from env; process-wide singleton).
  - `RdbmsTestExtension.shared() -> RdbmsTestExtension` — a `@RegisterExtension static` field value; `<M> M mapper(Class<M>)` returns a mapper bound to the current test's rolled-back transaction.
  - `SchemaApplier.applyResource(java.sql.Connection, String classpathResource)` — executes a `.sql` resource (statements split on `;`).

- [ ] **Step 1: Register module + dependencyManagement**

In `kernels/pom.xml` `<modules>` add `<module>persistence-test</module>`. In root `pom.xml` dependencyManagement KERNELS section add a `persistence-test` entry at `${project.version}` (mirror the `persistence` entry from Task 2 Step 2).

- [ ] **Step 2: Create `kernels/persistence-test/pom.xml`**

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>kernels</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>persistence-test</artifactId>
    <name>kernels :: persistence-test</name>
    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>persistence</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit-version}</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create `RdbmsDataSource` (env-driven HikariCP singleton)**

```java
package com.naturalist.persistence.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/** Process-wide test DataSource, configured from env with localhost defaults. */
public final class RdbmsDataSource {

    private RdbmsDataSource() {}

    private static volatile DataSource instance;

    public static DataSource shared() {
        DataSource local = instance;
        if (local == null) {
            synchronized (RdbmsDataSource.class) {
                local = instance;
                if (local == null) {
                    instance = local = build();
                }
            }
        }
        return local;
    }

    private static DataSource build() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env("NATURALIST_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/naturalist_test"));
        config.setUsername(env("NATURALIST_TEST_DB_USER", "postgres"));
        config.setPassword(env("NATURALIST_TEST_DB_PASSWORD", "postgres"));
        config.setMaximumPoolSize(4);
        config.setPoolName("naturalist-test");
        return new HikariDataSource(config);
    }

    private static String env(String key, String dflt) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? dflt : v;
    }
}
```

- [ ] **Step 4: Create `SchemaApplier`**

```java
package com.naturalist.persistence.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/** Runs a `.sql` classpath resource. The seam a DDL generator or Flyway swaps in later. */
public final class SchemaApplier {

    private SchemaApplier() {}

    public static void applyResource(Connection connection, String classpathResource) {
        String sql = read(classpathResource);
        try (Statement statement = connection.createStatement()) {
            for (String stmt : sql.split(";")) {
                if (!stmt.isBlank()) {
                    statement.execute(stmt);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed applying schema " + classpathResource, e);
        }
    }

    private static String read(String classpathResource) {
        try (InputStream in = SchemaApplier.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) throw new IllegalStateException("Schema resource not found: " + classpathResource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading schema " + classpathResource, e);
        }
    }
}
```

> **Note for the implementer:** the naturalists DDL (Task 4) uses only plain `;`-terminated statements, so a naive `split(";")` is correct here. Do not add PL/pgSQL blocks to seed DDL without upgrading this splitter.

- [ ] **Step 5: Create `RdbmsTestExtension` (connect + per-test rollback + `mapper`)**

```java
package com.naturalist.persistence.test;

import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Binds one MyBatis {@link SqlSession} per test (autocommit off) and rolls it back after,
 * so the seeded standing DB stays pristine. Assumes the DB is already seeded by
 * {@code apps/test-db-seeder}; this extension performs NO schema or seed work.
 */
public final class RdbmsTestExtension implements BeforeEachCallback, AfterEachCallback {

    private static final SqlSessionFactory FACTORY =
            MyBatisSupport.sessionFactory(RdbmsDataSource.shared());
    // mapper classes are registered lazily on first mapper(...) call
    private static final java.util.Set<Class<?>> REGISTERED = ConcurrentHashMap.newKeySet();

    private final ThreadLocal<SqlSession> current = new ThreadLocal<>();

    private RdbmsTestExtension() {}

    public static RdbmsTestExtension shared() {
        return new RdbmsTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        current.set(FACTORY.openSession(false)); // autocommit off
    }

    @Override
    public void afterEach(ExtensionContext context) {
        SqlSession session = current.get();
        if (session != null) {
            session.rollback();
            session.close();
            current.remove();
        }
    }

    /** Returns a mapper bound to the current test's rolled-back transaction. */
    public <M> M mapper(Class<M> mapperType) {
        if (REGISTERED.add(mapperType)) {
            FACTORY.getConfiguration().addMapper(mapperType);
        }
        SqlSession session = current.get();
        if (session == null) throw new IllegalStateException("No active test session; use @RegisterExtension");
        return session.getMapper(mapperType);
    }
}
```

- [ ] **Step 6: Build the module**

Run: `mvn -q -pl kernels/persistence-test -am install -DskipTests`
Expected: BUILD SUCCESS. (No unit tests here — this is test infrastructure; per project convention `kernels/framework-test`-style infra is smoke-verified by its consumers, Tasks 4/6.)

- [ ] **Step 7: Commit**

```bash
git add kernels/persistence-test/ kernels/pom.xml pom.xml
git commit -m "feat(persistence-test): RdbmsTestExtension, RdbmsDataSource, SchemaApplier"
```

---

## Task 4: `naturalists-repository-rdbms` — DBOs, DDL, mappers, adapters

**Files:**
- Create: `domains/naturalists/naturalists-repository-rdbms/pom.xml`
- Create: `.../src/main/resources/schema/naturalists.sql`
- Create (package `com.naturalist.naturalist`): `NaturalistDbo.java`, `NaturalistCredentialDbo.java`, `NaturalistMapper.java`, `NaturalistCredentialMapper.java`, `NaturalistEntityRepositoryRdbms.java`, `NaturalistCredentialRepositoryRdbms.java`
- Modify: `domains/naturalists/pom.xml`, root `pom.xml`
- Test: `NaturalistDboTest.java`, `NaturalistCredentialDboTest.java` (unit, no DB)

**Interfaces:**
- Consumes: `@DboSchema`/`@Fk`/`Dbo` (Task 2), `AbstractEntityRepository`, `NaturalistRepository.NaturalistEntityRepository`/`.CredentialRepository`, `RdbmsExceptions.isUniqueViolation` (Task 2).
- Produces:
  - `NaturalistDbo` with `static NaturalistDbo from(Naturalist)` and `Naturalist toEntity()`.
  - `NaturalistCredentialDbo` with `static NaturalistCredentialDbo from(NaturalistCredential, long naturalistId)` and `NaturalistCredential toEntity()`.
  - `NaturalistMapper`, `NaturalistCredentialMapper` (MyBatis).
  - `new NaturalistEntityRepositoryRdbms(NaturalistMapper)`, `new NaturalistCredentialRepositoryRdbms(NaturalistCredentialMapper)`.
  - Schema resource path: `schema/naturalists.sql`.

- [ ] **Step 1: Register the module**

In `domains/naturalists/pom.xml` `<modules>` add `<module>naturalists-repository-rdbms</module>`. In root `pom.xml` dependencyManagement (naturalists section) add a `naturalists-repository-rdbms` entry at `${project.version}`.

- [ ] **Step 2: Create the module pom**

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>naturalists</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>naturalists-repository-rdbms</artifactId>
    <name>naturalists :: repository-rdbms</name>
    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-api</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>persistence</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- test scope: the shared contract + JSON, and the rdbms test harness -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-repository-test</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>persistence-test</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Write the hand DDL**

`domains/naturalists/naturalists-repository-rdbms/src/main/resources/schema/naturalists.sql`:

```sql
DROP TABLE IF EXISTS naturalist_credential;
DROP TABLE IF EXISTS naturalist;

CREATE TABLE naturalist (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL UNIQUE,
    given_name  VARCHAR(100) NOT NULL,
    family_name VARCHAR(100),
    role        VARCHAR(32)  NOT NULL,
    stage       VARCHAR(32)  NOT NULL,
    notes       TEXT
);

CREATE TABLE naturalist_credential (
    naturalist_id BIGINT PRIMARY KEY REFERENCES naturalist(id),
    password_hash VARCHAR(80) NOT NULL
);
```

- [ ] **Step 4: Write the DBO round-trip + validation unit tests (no DB)**

`src/test/java/com/naturalist/naturalist/NaturalistDboTest.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NaturalistDboTest {

    private static Naturalist sample(String slug, String family, String notes) {
        return new Naturalist(NaturalistName.of(slug), "Given", family,
                NaturalistRole.KEEPER, EcologicalStage.NATURALIST, notes);
    }

    @Test void roundTrip_preservesAllFields() {
        Naturalist n = sample("amir-hassan", "Hassan", "some notes");
        assertThat(NaturalistDbo.from(n).toEntity()).usingRecursiveComparison().isEqualTo(n);
    }

    @Test void roundTrip_preservesNulls() {
        Naturalist n = sample("flora-mendez", null, null);
        assertThat(NaturalistDbo.from(n).toEntity()).usingRecursiveComparison().isEqualTo(n);
    }

    @Test void invariants_flagOverLongName() {
        NaturalistDbo dbo = NaturalistDbo.from(sample("amir-hassan", "Hassan", null));
        dbo.name = "x".repeat(65); // exceeds VARCHAR(64)
        var violations = Observer.forClass(NaturalistDboTest.class)
                .arguments("t", i -> i.observable(dbo, "dbo")).violations();
        assertThat(violations).isNotEmpty();
    }
}
```

- [ ] **Step 5: Run it to verify it fails**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-rdbms test -Dtest=NaturalistDboTest`
Expected: FAIL — `NaturalistDbo` does not exist.

- [ ] **Step 6: Write the DBOs**

`NaturalistDbo.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.observability.Constraints;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.function.Consumer;

@DboSchema(table = "naturalist", primaryKey = "id", unique = {"name"})
final class NaturalistDbo implements Dbo {
    Long id;              // null before insert; DB identity fills it
    String name;
    String given_name;
    String family_name;   // nullable
    String role;
    String stage;
    String notes;         // nullable

    static NaturalistDbo from(Naturalist n) {
        NaturalistDbo d = new NaturalistDbo();
        d.name = n.name().value();
        d.given_name = n.givenName();
        d.family_name = n.familyName();
        d.role = n.role().name();
        d.stage = n.stage().name();
        d.notes = n.notes();
        return d;
    }

    Naturalist toEntity() {
        return new Naturalist(
                NaturalistName.of(name),
                given_name,
                family_name,
                NaturalistRole.valueOf(role),
                EcologicalStage.valueOf(stage),
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(name, "name").kebabFormat(name, "name").maxLength(name, 64, "name")
                .notNull(given_name, "given_name").maxLength(given_name, 100, "given_name")
                .maxLength(family_name, 100, "family_name")
                .notNull(role, "role").maxLength(role, 32, "role")
                .notNull(stage, "stage").maxLength(stage, 32, "stage");
    }
}
```

`NaturalistCredentialDbo.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.observability.Constraints;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

@DboSchema(table = "naturalist_credential", primaryKey = "naturalist_id",
           foreignKeys = @Fk(columns = "naturalist_id", references = "naturalist(id)"))
final class NaturalistCredentialDbo implements Dbo {
    Long naturalist_id;   // resolved from naturalist.name at insert time
    String password_hash;
    String name;          // read-only projection (aliased n.name); null on the write path

    static NaturalistCredentialDbo from(NaturalistCredential c, long naturalistId) {
        NaturalistCredentialDbo d = new NaturalistCredentialDbo();
        d.naturalist_id = naturalistId;
        d.password_hash = c.passwordHash();
        d.name = c.name().value();
        return d;
    }

    NaturalistCredential toEntity() {
        return new NaturalistCredential(NaturalistName.of(name), password_hash);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notNull(naturalist_id, "naturalist_id")
                .notBlank(password_hash, "password_hash").maxLength(password_hash, 80, "password_hash");
    }
}
```

- [ ] **Step 7: Add the credential DBO round-trip test, then run both DBO tests to pass**

`src/test/java/com/naturalist/naturalist/NaturalistCredentialDboTest.java`:

```java
package com.naturalist.naturalist;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NaturalistCredentialDboTest {

    @Test void roundTrip_recoversNameAndHash() {
        NaturalistCredential c = new NaturalistCredential(
                NaturalistName.of("amir-hassan"), "{bcrypt}$2a$10$abc");
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(c, 7L);
        assertThat(dbo.naturalist_id).isEqualTo(7L);
        assertThat(dbo.toEntity()).usingRecursiveComparison().isEqualTo(c);
    }
}
```

Run: `mvn -q -pl domains/naturalists/naturalists-repository-rdbms test -Dtest=NaturalistDboTest,NaturalistCredentialDboTest`
Expected: PASS (4 tests).

- [ ] **Step 8: Write the mappers (annotated, auto-mapped, text-block SQL)**

`NaturalistMapper.java`:

```java
package com.naturalist.naturalist;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

interface NaturalistMapper {

    @Select("""
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist WHERE name = #{name}
        """)
    NaturalistDbo selectByName(String name);

    @Select("""
        <script>
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist
        WHERE name IN
        <foreach item='n' collection='names' open='(' separator=',' close=')'>#{n}</foreach>
        </script>
        """)
    List<NaturalistDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("""
        SELECT id, name, given_name, family_name, role, stage, notes
        FROM naturalist ORDER BY name LIMIT #{limit} OFFSET #{offset}
        """)
    List<NaturalistDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM naturalist ORDER BY name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO naturalist (name, given_name, family_name, role, stage, notes)
        VALUES (#{name}, #{given_name}, #{family_name}, #{role}, #{stage}, #{notes})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(NaturalistDbo dbo);

    @Update("""
        UPDATE naturalist SET given_name = #{given_name}, family_name = #{family_name},
               role = #{role}, stage = #{stage}, notes = #{notes}
        WHERE name = #{name}
        """)
    int updateByName(NaturalistDbo dbo);
}
```

`NaturalistCredentialMapper.java`:

```java
package com.naturalist.naturalist;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

interface NaturalistCredentialMapper {

    @Select("""
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        WHERE n.name = #{name}
        """)
    NaturalistCredentialDbo selectByName(String name);

    @Select("""
        <script>
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        WHERE n.name IN
        <foreach item='x' collection='names' open='(' separator=',' close=')'>#{x}</foreach>
        </script>
        """)
    List<NaturalistCredentialDbo> selectByNameSet(@Param("names") Collection<String> names);

    @Select("""
        SELECT c.password_hash, n.name AS name
        FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
        ORDER BY n.name LIMIT #{limit} OFFSET #{offset}
        """)
    List<NaturalistCredentialDbo> selectPage(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
        SELECT count(*) FROM (
            SELECT 1 FROM naturalist_credential c JOIN naturalist n ON n.id = c.naturalist_id
            ORDER BY n.name OFFSET #{skip} LIMIT #{window}
        ) t
        """)
    int countInWindow(@Param("skip") int skip, @Param("window") int window);

    @Insert("""
        INSERT INTO naturalist_credential (naturalist_id, password_hash)
        SELECT id, #{password_hash} FROM naturalist WHERE name = #{name}
        """)
    void insert(NaturalistCredentialDbo dbo);

    @Update("""
        UPDATE naturalist_credential SET password_hash = #{password_hash}
        WHERE naturalist_id = (SELECT id FROM naturalist WHERE name = #{name})
        """)
    int updateByName(NaturalistCredentialDbo dbo);
}
```

> **Note:** `NaturalistCredentialDbo` needs `name` populated for the write-path `insert`/`updateByName` (they read `#{name}`). `from(credential, id)` already sets `d.name`. The `insert`'s `SELECT … WHERE name = #{name}` resolves the FK inline; the FK/NOT NULL enforces the naturalist exists.

- [ ] **Step 9: Write the adapters**

`NaturalistEntityRepositoryRdbms.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class NaturalistEntityRepositoryRdbms
        extends AbstractEntityRepository<NaturalistName, Naturalist>
        implements NaturalistRepository.NaturalistEntityRepository {

    private final NaturalistMapper mapper;

    NaturalistEntityRepositoryRdbms(NaturalistMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Naturalist> doGetByName(NaturalistName name) {
        NaturalistDbo dbo = mapper.selectByName(name.value());
        return Optional.ofNullable(dbo).map(NaturalistDbo::toEntity);
    }

    @Override
    protected List<Naturalist> doGetByNameSet(Set<NaturalistName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(NaturalistName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(NaturalistDbo::toEntity).toList();
    }

    @Override
    protected Page<Naturalist> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<NaturalistDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Naturalist> content = rows.stream().limit(pageSize).map(NaturalistDbo::toEntity).toList();

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), beyond / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(Naturalist entity) {
        NaturalistDbo dbo = NaturalistDbo.from(entity);
        observer().arguments("doInsert", i -> i.observable(dbo, "dbo")).throwWhenInvalid();
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) {
                throw new PrimaryKeyConstraintException("naturalist", entity.name().value());
            }
            throw e;
        }
    }

    @Override
    protected void doUpdate(Naturalist entity) {
        NaturalistDbo dbo = NaturalistDbo.from(entity);
        observer().arguments("doUpdate", i -> i.observable(dbo, "dbo")).throwWhenInvalid();
        if (mapper.updateByName(dbo) == 0) {
            throw new EntityNotFoundException("naturalist", entity.name().value());
        }
    }

    @Override
    protected Naturalist doSave(Naturalist entity) {
        if (mapper.selectByName(entity.name().value()) != null) {
            doUpdate(entity);
        } else {
            doInsert(entity);
        }
        return entity;
    }
}
```

`NaturalistCredentialRepositoryRdbms.java` (same shape; paging/error handling identical, using `NaturalistCredentialMapper` and `NaturalistCredentialDbo`):

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class NaturalistCredentialRepositoryRdbms
        extends AbstractEntityRepository<NaturalistName, NaturalistCredential>
        implements NaturalistRepository.CredentialRepository {

    private final NaturalistCredentialMapper mapper;

    NaturalistCredentialRepositoryRdbms(NaturalistCredentialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<NaturalistCredential> doGetByName(NaturalistName name) {
        NaturalistCredentialDbo dbo = mapper.selectByName(name.value());
        return Optional.ofNullable(dbo).map(NaturalistCredentialDbo::toEntity);
    }

    @Override
    protected List<NaturalistCredential> doGetByNameSet(Set<NaturalistName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(NaturalistName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(NaturalistCredentialDbo::toEntity).toList();
    }

    @Override
    protected Page<NaturalistCredential> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<NaturalistCredentialDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<NaturalistCredential> content =
                rows.stream().limit(pageSize).map(NaturalistCredentialDbo::toEntity).toList();
        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), beyond / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(NaturalistCredential entity) {
        // naturalist_id is resolved inline by the mapper's INSERT … SELECT; DBO carries name.
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(entity, -1L);
        // validation asserts password_hash width; naturalist_id is filled by SQL, so skip its notNull here
        observer().arguments("doInsert", i -> i
                .notBlank(dbo.password_hash, "password_hash")
                .maxLength(dbo.password_hash, 80, "password_hash")).throwWhenInvalid();
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) {
                throw new PrimaryKeyConstraintException("naturalist_credential", entity.name().value());
            }
            throw e;
        }
    }

    @Override
    protected void doUpdate(NaturalistCredential entity) {
        NaturalistCredentialDbo dbo = NaturalistCredentialDbo.from(entity, -1L);
        if (mapper.updateByName(dbo) == 0) {
            throw new EntityNotFoundException("naturalist_credential", entity.name().value());
        }
    }

    @Override
    protected NaturalistCredential doSave(NaturalistCredential entity) {
        if (mapper.selectByName(entity.name().value()) != null) {
            doUpdate(entity);
        } else {
            doInsert(entity);
        }
        return entity;
    }
}
```

> **Note on credential `doInsert` validation:** the DBO's own `invariants()` require `naturalist_id` non-null, but on the write path the id is resolved *inside* SQL (not in the DBO), so the adapter validates only the fields it actually carries. This is a deliberate, per-site partial validation — do not weaken the DBO's `invariants()`.

- [ ] **Step 10: Verify `PrimaryKeyConstraintException`/`EntityNotFoundException` constructors**

Run: `grep -n "public PrimaryKeyConstraintException\|public EntityNotFoundException" kernels/framework/src/main/java/com/naturalist/exception/*.java`
Adjust the `throw new …(…)` argument lists in Step 9 to match the real constructor signatures (they may take a single message `String`). Re-edit if needed.

- [ ] **Step 11: Compile the module**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-rdbms -am install -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 12: Run the DBO unit tests**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-rdbms test -Dtest=NaturalistDboTest,NaturalistCredentialDboTest`
Expected: PASS.

- [ ] **Step 13: Commit**

```bash
git add domains/naturalists/naturalists-repository-rdbms/ domains/naturalists/pom.xml pom.xml
git commit -m "feat(naturalists): rdbms adapter — DBOs, DDL, MyBatis mappers, repository adapters"
```

---

## Task 5: `apps/test-db-seeder` — manual seeder

**Files:**
- Create: `apps/test-db-seeder/pom.xml`
- Create: `.../seeder/TestDbSeeder.java`
- Modify: `apps/pom.xml` (the `naturalist-apps` aggregator), root `pom.xml`

**Interfaces:**
- Consumes: `SchemaApplier`, `RdbmsDataSource`, `MyBatisSupport` (kernels); `NaturalistEntityRepositoryRdbms`, `NaturalistCredentialRepositoryRdbms`, `NaturalistMapper`, `NaturalistCredentialMapper` (Task 4); `NaturalistDatabase`, `NaturalistTestEntitySource`, `NaturalistCredentialTestEntitySource` (naturalists-repository-test).
- Produces: an executable `main` that seeds the standing Postgres.

- [ ] **Step 1: Register the module**

In `apps/pom.xml` (artifactId `naturalist-apps`) `<modules>` add `<module>test-db-seeder</module>`. Add a root dependencyManagement entry `test-db-seeder` at `${project.version}`.

- [ ] **Step 2: Create the pom (plain executable, no Spring needed for one domain)**

```xml
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>naturalist-apps</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>test-db-seeder</artifactId>
    <name>apps :: test-db-seeder</name>
    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>persistence-test</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-repository-rdbms</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-repository-test</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <archive><manifest><mainClass>com.naturalist.seeder.TestDbSeeder</mainClass></manifest></archive>
                    <descriptorRefs><descriptorRef>jar-with-dependencies</descriptorRef></descriptorRefs>
                </configuration>
                <executions>
                    <execution><id>make-jar</id><phase>package</phase>
                        <goals><goal>single</goal></goals></execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Write the seeder**

`apps/test-db-seeder/src/main/java/com/naturalist/seeder/TestDbSeeder.java`. It lives in package `com.naturalist.seeder`, so it can only touch the *public* surface of the domain module — but the adapters/mappers are package-private. Therefore expose a single public entrypoint from the rdbms module (Step 4) and call it here.

```java
package com.naturalist.seeder;

import com.naturalist.naturalist.NaturalistRdbmsSeed;
import com.naturalist.persistence.test.RdbmsDataSource;
import com.naturalist.persistence.test.SchemaApplier;

import javax.sql.DataSource;
import java.sql.Connection;

/** Materializes the JSON seed into the standing Postgres. Run after cloning and whenever
 *  seed JSON or DDL changes. Idempotent: drops + recreates the schema, then reseeds. */
public final class TestDbSeeder {

    public static void main(String[] args) throws Exception {
        DataSource dataSource = RdbmsDataSource.shared();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            SchemaApplier.applyResource(connection, "schema/naturalists.sql");
        }
        NaturalistRdbmsSeed.seed(dataSource);
        System.out.println("Seed complete.");
    }
}
```

- [ ] **Step 4: Add the public seed entrypoint in the rdbms module**

Because the adapters/mappers/DBOs are package-private, add one public class in `com.naturalist.naturalist` (in `naturalists-repository-rdbms/src/main/java`) that the seeder calls. It replays JSON entities through the adapters' `insert()`.

`domains/naturalists/naturalists-repository-rdbms/src/main/java/com/naturalist/naturalist/NaturalistRdbmsSeed.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/** Public seed entrypoint: loads naturalists + credentials from the JSON-backed
 *  TestEntitySources and replays each through its rdbms adapter's insert(), FK-ordered
 *  (naturalists before credentials), committing once. */
public final class NaturalistRdbmsSeed {

    private NaturalistRdbmsSeed() {}

    public static void seed(DataSource dataSource) {
        SqlSessionFactory factory = MyBatisSupport.sessionFactory(
                dataSource, NaturalistMapper.class, NaturalistCredentialMapper.class);
        NaturalistDatabase database = NaturalistDatabase.create();

        try (SqlSession session = factory.openSession(false)) {
            var naturalists = new NaturalistEntityRepositoryRdbms(session.getMapper(NaturalistMapper.class));
            database.getNamed(NaturalistTestEntitySource.class).entityStream()
                    .forEach(naturalists::insert);

            var credentials = new NaturalistCredentialRepositoryRdbms(session.getMapper(NaturalistCredentialMapper.class));
            database.getNamed(NaturalistCredentialTestEntitySource.class).entityStream()
                    .forEach(credentials::insert);

            session.commit();
        }
    }
}
```

> **Verify while implementing:** confirm `NaturalistCredentialTestEntitySource` exists and loads `naturalists/naturalist-credentials.json` (it does per the repository-test module), and that `NaturalistDatabase.create()` is the sanctioned constructor here (this is a dev tool, an allowed `create()` site alongside `spring-test-data`/`NaturalistTestExtension`).

- [ ] **Step 5: Build, then run the seeder against local Postgres**

Ensure Postgres is up (Global Constraints). Then:

Run: `mvn -q -pl apps/test-db-seeder -am install -DskipTests`
Run: `java -jar apps/test-db-seeder/target/test-db-seeder-1.0.0-SNAPSHOT-jar-with-dependencies.jar`
Expected: prints `Seed complete.`

- [ ] **Step 6: Verify the data landed**

Run: `docker exec naturalist-pg psql -U postgres -d naturalist_test -c "SELECT name, role FROM naturalist ORDER BY name;"`
Expected: 4 rows (amir-hassan, delia-durrell, flora-mendez, patrick-way).
Run: `docker exec naturalist-pg psql -U postgres -d naturalist_test -c "SELECT n.name FROM naturalist_credential c JOIN naturalist n ON n.id=c.naturalist_id ORDER BY n.name;"`
Expected: the same 4 names (credentials resolved their FK by name).

- [ ] **Step 7: Commit**

```bash
git add apps/test-db-seeder/ apps/pom.xml pom.xml \
        domains/naturalists/naturalists-repository-rdbms/src/main/java/com/naturalist/naturalist/NaturalistRdbmsSeed.java
git commit -m "feat(test-db-seeder): manual seeder replaying JSON through rdbms insert()"
```

---

## Task 6: rdbms contract `*IT`s + Failsafe + CI

**Files:**
- Create: `.../naturalists-repository-rdbms/src/test/java/com/naturalist/naturalist/NaturalistEntityRepositoryRdbmsIT.java`
- Create: `.../NaturalistCredentialRepositoryRdbmsIT.java`
- Modify: `.../naturalists-repository-rdbms/pom.xml` (add Failsafe)
- Create/Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `NaturalistEntityRepositoryTest`/`NaturalistCredentialEntityRepositoryTest` (naturalists-repository-test), `RdbmsTestExtension` (Task 3), `NaturalistEntityRepositoryRdbms`/`NaturalistCredentialRepositoryRdbms` + `NaturalistMapper`/`NaturalistCredentialMapper` (Task 4).

- [ ] **Step 1: Add Failsafe to the rdbms module pom**

In `domains/naturalists/naturalists-repository-rdbms/pom.xml`, add:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <goals><goal>integration-test</goal><goal>verify</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

(Failsafe runs `**/*IT.java` in the `integration-test`/`verify` phases; Surefire runs `*Test` in `test`. So `mvn test` stays DB-free, `mvn verify` runs the `*IT`s.)

- [ ] **Step 2: Write the Naturalist contract `*IT`**

`NaturalistEntityRepositoryRdbmsIT.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The behavioral contract ({@link NaturalistEntityRepositoryTest}) re-run against real
 * Postgres. Requires the standing DB to be seeded (apps/test-db-seeder); each test runs in a
 * transaction the extension rolls back. `source()` (inherited) is the in-memory JSON oracle.
 */
class NaturalistEntityRepositoryRdbmsIT implements NaturalistEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public NaturalistRepository.NaturalistEntityRepository repository() {
        return new NaturalistEntityRepositoryRdbms(rdbms.mapper(NaturalistMapper.class));
    }
}
```

- [ ] **Step 3: Write the Credential contract `*IT`**

`NaturalistCredentialRepositoryRdbmsIT.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.persistence.test.RdbmsTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class NaturalistCredentialRepositoryRdbmsIT implements NaturalistCredentialEntityRepositoryTest {

    @RegisterExtension
    static RdbmsTestExtension rdbms = RdbmsTestExtension.shared();

    @Override
    public NaturalistRepository.CredentialRepository repository() {
        return new NaturalistCredentialRepositoryRdbms(rdbms.mapper(NaturalistCredentialMapper.class));
    }
}
```

> **Verify while implementing:** open `NaturalistCredentialEntityRepositoryTest` in `naturalists-repository-test` and confirm the exact `repository()` return type and the abstract-hook names to override (mirror how `NaturalistEntityRepositoryTest` is shaped — Task context read it). Adjust the override signature to match.

- [ ] **Step 4: Seed, then run the `*IT`s**

Ensure Postgres is up and re-run the seeder (Task 5 Step 5) so the DB matches current JSON/DDL. Then:

Run: `mvn -q -pl domains/naturalists/naturalists-repository-rdbms verify`
Expected: PASS — all inherited contract cases (getByName, getByEntityNameSet, getPage, insert, update) green for both adapters against Postgres.

- [ ] **Step 5: Debug loop (only if red)**

Use `superpowers:systematic-debugging`. Likely first-run issues and their fixes:
- **`insert_duplicateName` expects `PrimaryKeyConstraintException`** — confirm `RdbmsExceptions.isUniqueViolation` catches the MyBatis-wrapped `PSQLException` (SQLState 23505) and that Step 10 of Task 4 used the real exception constructor.
- **`update_unknownName` expects `EntityNotFoundException`** — confirm `updateByName` returns 0 rows for an absent name and the adapter throws.
- **Recursive-comparison mismatch** — a DBO `toEntity()` field drift (e.g. enum case, a trimmed string); fix the factory, not the test.
- **Auto-mapping empty fields** — if columns aren't populating DBO fields, MyBatis may need field access; confirm `mapUnderscoreToCamelCase` is false and the DBO field names exactly equal the selected column names/aliases.

- [ ] **Step 6: Full-reactor verify (integration gate)**

Ensure the seeder has run against the DB, then:

Run: `mvn verify`
Expected: BUILD SUCCESS across the reactor (the new `*IT`s run under Failsafe; mock `*Test`s unchanged).

- [ ] **Step 7: Add CI (Postgres service + seeder step)**

Create `.github/workflows/ci.yml` if absent, else merge the `services` block and the seeder step into the existing `mvn verify` job:

```yaml
name: ci
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: naturalist_test
          POSTGRES_PASSWORD: postgres
        ports: ["5432:5432"]
        options: >-
          --health-cmd pg_isready --health-interval 5s
          --health-timeout 5s --health-retries 10
    env:
      NATURALIST_TEST_JDBC_URL: jdbc:postgresql://localhost:5432/naturalist_test
      NATURALIST_TEST_DB_USER: postgres
      NATURALIST_TEST_DB_PASSWORD: postgres
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '25' }
      - run: mvn -q -DskipTests install
      - run: java -jar apps/test-db-seeder/target/test-db-seeder-1.0.0-SNAPSHOT-jar-with-dependencies.jar
      - run: mvn -q verify
```

- [ ] **Step 8: Architectural-enforcement gate**

Run: `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: clean (no new N+1s — the adapters do no per-element repository calls; the seeder's per-entity `insert()` is a write loop over a fixed JSON list, not a query fan-out. If `NoSelectInIteration` flags the seeder loop, that is a false positive on a one-time seed — annotate per-site with justification only as a last resort, per root CLAUDE.md).

- [ ] **Step 9: Commit**

```bash
git add domains/naturalists/naturalists-repository-rdbms/pom.xml \
        domains/naturalists/naturalists-repository-rdbms/src/test/ \
        .github/workflows/ci.yml
git commit -m "test(naturalists): rdbms contract *ITs in mvn verify + CI Postgres service"
```

---

## Self-Review

**Spec coverage:**
- Two kernels (`persistence`, `persistence-test`) — Tasks 2, 3. ✓
- ACL DBOs: Observable, snake_case, auto-mapped, no type handlers, `from`/`toEntity` — Task 4 Steps 6–7. ✓
- Pure-metadata `@DboSchema`/`@Fk` — Task 2 Step 4. ✓
- `StringLengthLessThanConstraint` + `maxLength` — Task 1. ✓
- One `naturalist` table (numeric anchor, `name VARCHAR(64)` unique) + credential FK — Task 4 Step 3. ✓
- Annotated text-block mappers, no `@Results` — Task 4 Step 8. ✓
- Nested-select FK resolution (credential insert/update) — Task 4 Step 8. ✓
- Standing test DB, out-of-band manual seeder replaying through `insert()` — Task 5. ✓
- `SchemaApplier` seam — Task 3 Step 4, used Task 5 Step 3. ✓
- Contract `*IT`s in `mvn verify` (Failsafe); `mvn test` DB-free — Task 6 Steps 1–2. ✓
- CI Postgres service + seeder step — Task 6 Step 7. ✓
- No app runtime wiring (phase boundary) — nothing in the plan wires the running app; the rdbms beans are never `@DomainService`/`@Profile`. ✓
- String enums via factory conversion — Task 4 Step 6 (`role.name()`/`valueOf`). ✓

**Placeholder scan:** No TBDs. Two "Verify while implementing" notes (Task 4 Step 10 exception constructors; Task 5 Step 4 / Task 6 Step 3 sibling-type shapes) point the implementer at exact files to confirm signatures — these are verification steps, not placeholders, because the surrounding code is fully written and only the argument list may need matching.

**Type consistency:** `NaturalistMapper`/`NaturalistCredentialMapper` method names (`selectByName`, `selectByNameSet`, `selectPage`, `countInWindow`, `insert`, `updateByName`) are used identically in the adapters (Task 4 Step 9). `RdbmsExceptions.isUniqueViolation`, `MyBatisSupport.sessionFactory`, `SchemaApplier.applyResource`, `RdbmsDataSource.shared`, `RdbmsTestExtension.shared`/`.mapper`, `NaturalistRdbmsSeed.seed` all match between producer and consumer tasks. DBO field names equal the DDL columns and the mapper column names/aliases.

**Known cross-task ordering:** the `*IT`s (Task 6) require the seeder (Task 5) to have run, which requires the adapters (Task 4). This is why Task 6 is last and its run steps re-invoke the seeder. Tasks 1–4 are each independently testable without a DB; Task 5 is verified by inspecting the seeded rows; Task 6 by `mvn verify`.
