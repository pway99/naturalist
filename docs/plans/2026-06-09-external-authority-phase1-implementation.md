# Authority Seam (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Land the `kernels/authority` port + value objects and an EOL fixture client (`external-authorities/eol/eol-api` + `eol-client-mock`), so a consumer can later look up authority deep-links for a taxon without any real network call.

**Architecture:** A domain-neutral kernel port `ExternalAuthority` returns `Set<AuthorityReference>` (a `(source, url)` deep-link VO) for an `EntityName` subject. Each provider implements the port in its own module family; this slice ships only the EOL mock. External-record identity (`EolPageId`) lives in the provider's `*-api`, never the kernel. No console, storage, real HTTP, or bibliography.

**Tech Stack:** Java records + `com.naturalist.ddd` identity model (`ValueObject`, `NamedValue<String>`, `EntityName`), the `Observer` observability framework, Maven multi-module, JUnit 5 + AssertJ.

**Source of truth:** [`2026-06-09-external-authority-phase1-design.md`](2026-06-09-external-authority-phase1-design.md).

**Conventions for this repo (read before executing):**
- **Builds:** the human runs `mvn verify` (or the per-task `mvn -pl … test`) locally. The executor writes code and **stages** changes, then waits for a green build at each task checkpoint before continuing — it does not run the full build itself unprompted.
- **Commits:** trunk-based, direct to `main`. The `git commit` steps below are the *intended* commit points, but the executor **stages only**; the human reviews the diff and commits (or authorises it). Every Claude-authored commit keeps the `Co-Authored-By: Claude` footer.
- **New module scaffolding** follows `domains/CLAUDE.md` §"Creating a New Module": module pom → parent `<modules>` → **root `<dependencyManagement>` entry with `${project.version}`** → dependent poms.

---

## File Structure

**New module `kernels/authority`** (`com.naturalist.authority`):
- `ExternalAuthority.java` — the port interface
- `AuthoritySource.java` — `ValueObject(id, displayName)`
- `AuthorityReference.java` — `ValueObject(source, url)`
- tests: `AuthoritySourceTest.java`, `AuthorityReferenceTest.java`

**New grouping `external-authorities/`** (aggregator) → `eol/` (aggregator):
- `eol-api/` (`com.naturalist.authority.eol`): `EolPageId.java` (`NamedValue<String>`), `Eol.java` (constants + `deepLink`); test `EolPageIdTest.java`
- `eol-client-mock/` (`com.naturalist.authority.eol`): `EolClientMock.java`; test `EolClientMockTest.java`

**Modified build files:** root `pom.xml` (`<modules>` + `<dependencyManagement>`), `kernels/pom.xml` (`<modules>`).

**Modified docs:** `docs/work-tracker.md`.

---

### Task 1: Scaffold `kernels/authority` module + build wiring

**Files:**
- Create: `kernels/authority/pom.xml`
- Create dir: `kernels/authority/src/main/java/com/naturalist/authority/`
- Create dir: `kernels/authority/src/test/java/com/naturalist/authority/`
- Modify: `kernels/pom.xml` (add module)
- Modify: `pom.xml` (root — add dependencyManagement entry)

- [ ] **Step 1: Create the module pom**

`kernels/authority/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>kernels</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>authority</artifactId>
    <name>kernels :: authority</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>

        <!-- TEST DEPENDENCIES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Register the module in `kernels/pom.xml`**

Add `<module>authority</module>` to the `<modules>` list in `kernels/pom.xml` (place it right after `<module>framework-test</module>`):
```xml
        <module>framework</module>
        <module>framework-test</module>
        <module>authority</module>
        <module>catalog</module>
```

- [ ] **Step 3: Add the root dependency-management entry**

In root `pom.xml`, inside `<dependencyManagement><dependencies>`, in the KERNELS section, add (alphabetically near the other kernel entries):
```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>authority</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 4: Verify the empty module is in the reactor**

Run: `mvn -q -pl :authority -am validate`
Expected: BUILD SUCCESS, `authority` appears in the reactor build order.

- [ ] **Step 5: Stage (commit point)**

```bash
git add kernels/authority/pom.xml kernels/pom.xml pom.xml
# commit message: "chore: scaffold kernels/authority module"
```

---

### Task 2: `AuthoritySource` value object (TDD)

**Files:**
- Test: `kernels/authority/src/test/java/com/naturalist/authority/AuthoritySourceTest.java`
- Create: `kernels/authority/src/main/java/com/naturalist/authority/AuthoritySource.java`

- [ ] **Step 1: Write the failing test**

`AuthoritySourceTest.java`:
```java
package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoritySourceTest {

    private static final Observer observer = Observer.forClass(AuthoritySourceTest.class);

    @Test
    void wellFormedSourcePassesInvariants() {
        AuthoritySource source = new AuthoritySource("eol", "Encyclopedia of Life");

        InvariantObservation result = observer.forMethod("wellFormedSourcePassesInvariants")
                .observable(source, "source");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void blankIdViolatesInvariants() {
        AuthoritySource source = new AuthoritySource("  ", "Encyclopedia of Life");

        InvariantObservation result = observer.forMethod("blankIdViolatesInvariants")
                .observable(source, "source");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".id"));
    }

    @Test
    void blankDisplayNameViolatesInvariants() {
        AuthoritySource source = new AuthoritySource("eol", "  ");

        InvariantObservation result = observer.forMethod("blankDisplayNameViolatesInvariants")
                .observable(source, "source");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".displayName"));
    }
}
```

- [ ] **Step 2: Run it; verify it fails to compile**

Run: `mvn -q -pl :authority test`
Expected: FAIL — `AuthoritySource` does not exist.

- [ ] **Step 3: Write the implementation**

`AuthoritySource.java`:
```java
package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The external authority a reference came from — e.g. EOL, iNaturalist.
 * Open provider metadata: the kernel names no concrete source; each
 * provider defines its own {@code AuthoritySource} constant in its api.
 */
public record AuthoritySource(String id, String displayName) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(id, "id")
                .notBlank(displayName, "displayName");
    }
}
```

- [ ] **Step 4: Run tests; verify pass**

Run: `mvn -q -pl :authority test`
Expected: PASS (3 tests).

- [ ] **Step 5: Stage (commit point)**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/AuthoritySource.java \
        kernels/authority/src/test/java/com/naturalist/authority/AuthoritySourceTest.java
# commit message: "feat(authority): add AuthoritySource value object"
```

---

### Task 3: `AuthorityReference` value object (TDD)

**Files:**
- Test: `kernels/authority/src/test/java/com/naturalist/authority/AuthorityReferenceTest.java`
- Create: `kernels/authority/src/main/java/com/naturalist/authority/AuthorityReference.java`

- [ ] **Step 1: Write the failing test**

`AuthorityReferenceTest.java`:
```java
package com.naturalist.authority;

import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorityReferenceTest {

    private static final Observer observer = Observer.forClass(AuthorityReferenceTest.class);
    private static final AuthoritySource EOL = new AuthoritySource("eol", "Encyclopedia of Life");

    @Test
    void wellFormedReferencePassesInvariants() {
        AuthorityReference ref = new AuthorityReference(EOL, URI.create("https://eol.org/pages/1188585"));

        InvariantObservation result = observer.forMethod("wellFormedReferencePassesInvariants")
                .observable(ref, "ref");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullSourceViolatesInvariants() {
        AuthorityReference ref = new AuthorityReference(null, URI.create("https://eol.org/pages/1188585"));

        InvariantObservation result = observer.forMethod("nullSourceViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".source"));
    }

    @Test
    void nullUrlViolatesInvariants() {
        AuthorityReference ref = new AuthorityReference(EOL, null);

        InvariantObservation result = observer.forMethod("nullUrlViolatesInvariants")
                .observable(ref, "ref");

        assertThat(result.violationNames()).anyMatch(n -> n.endsWith(".url"));
    }
}
```

- [ ] **Step 2: Run it; verify it fails**

Run: `mvn -q -pl :authority test`
Expected: FAIL — `AuthorityReference` does not exist.

- [ ] **Step 3: Write the implementation**

`AuthorityReference.java`:
```java
package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.net.URI;
import java.util.function.Consumer;

/**
 * A deep-link pointer into an external authority's catalogue for some
 * subject. Carries its own {@link AuthoritySource} so it is
 * self-describing once stored or passed around. The {@code url} is a
 * fully resolvable deep-link, precomputed by the producing client.
 */
public record AuthorityReference(AuthoritySource source, URI url) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(source, "source")
                .notNull(url, "url");
    }
}
```

- [ ] **Step 4: Run tests; verify pass**

Run: `mvn -q -pl :authority test`
Expected: PASS (6 tests total in module).

- [ ] **Step 5: Stage (commit point)**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/AuthorityReference.java \
        kernels/authority/src/test/java/com/naturalist/authority/AuthorityReferenceTest.java
# commit message: "feat(authority): add AuthorityReference value object"
```

---

### Task 4: `ExternalAuthority` port interface

**Files:**
- Create: `kernels/authority/src/main/java/com/naturalist/authority/ExternalAuthority.java`

No standalone test — an interface is exercised by its first implementation (`EolClientMock`, Task 7), per the kernel-testing convention.

- [ ] **Step 1: Write the interface**

`ExternalAuthority.java`:
```java
package com.naturalist.authority;

import com.naturalist.ddd.EntityName;

import java.util.Set;

/**
 * A single external authority a consumer can consult for deep-links to
 * its catalogue. Consumers choose <em>which</em> authority to call;
 * there is no fan-out across providers.
 *
 * <p>Network-backed implementations MUST be Resilience-wrapped
 * (bulkhead + timeout + retry) per ADR-026. An in-memory implementation
 * carries {@code @ResilienceExempt} instead, because it performs no I/O.
 */
public interface ExternalAuthority {

    /** The authority this client consults (e.g. EOL). */
    AuthoritySource source();

    /**
     * Deep-link references this source knows for {@code subject}.
     * Never null; an empty set means "nothing known", not an error.
     */
    Set<AuthorityReference> lookup(EntityName subject);
}
```

- [ ] **Step 2: Compile**

Run: `mvn -q -pl :authority test`
Expected: PASS — module compiles, existing 6 tests still green.

- [ ] **Step 3: Stage (commit point)**

```bash
git add kernels/authority/src/main/java/com/naturalist/authority/ExternalAuthority.java
# commit message: "feat(authority): add ExternalAuthority port interface"
```

---

### Task 5: Scaffold the `external-authorities/eol` grouping

**Files:**
- Create: `external-authorities/pom.xml`
- Create: `external-authorities/eol/pom.xml`
- Modify: `pom.xml` (root — add `<module>external-authorities</module>`)

- [ ] **Step 1: Create the top-level grouping pom**

`external-authorities/pom.xml`:
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
    <artifactId>external-authorities</artifactId>
    <packaging>pom</packaging>
    <name>amateur-naturalist :: external-authorities</name>

    <modules>
        <module>eol</module>
    </modules>
</project>
```

- [ ] **Step 2: Create the per-provider grouping pom**

`external-authorities/eol/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>external-authorities</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>eol</artifactId>
    <packaging>pom</packaging>
    <name>external-authorities :: eol</name>

    <modules>
        <module>eol-api</module>
        <module>eol-client-mock</module>
    </modules>
</project>
```

- [ ] **Step 3: Register the grouping in the root `pom.xml`**

Add `<module>external-authorities</module>` to the root `<modules>` (alphabetical — between `domains` and `kernels`):
```xml
    <modules>
        <module>adapters</module>
        <module>apps</module>
        <module>domains</module>
        <module>external-authorities</module>
        <module>kernels</module>
    </modules>
```

Note: the child module dirs (`eol-api`, `eol-client-mock`) do not exist yet — they are created in Tasks 6–7. Do **not** run a full reactor build until then; this step only edits poms.

- [ ] **Step 4: Stage (commit point)**

```bash
git add external-authorities/pom.xml external-authorities/eol/pom.xml pom.xml
# commit message: "chore: scaffold external-authorities/eol grouping"
```

---

### Task 6: `eol-api` — `EolPageId` (TDD) + `Eol` constants

**Files:**
- Create: `external-authorities/eol/eol-api/pom.xml`
- Create dirs: `external-authorities/eol/eol-api/src/main/java/com/naturalist/authority/eol/`, `.../src/test/java/com/naturalist/authority/eol/`
- Test: `external-authorities/eol/eol-api/src/test/java/com/naturalist/authority/eol/EolPageIdTest.java`
- Create: `EolPageId.java`, `Eol.java` (same package)
- Modify: root `pom.xml` (dependencyManagement entry for `eol-api`)

- [ ] **Step 1: Create the module pom**

`external-authorities/eol/eol-api/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>eol</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>eol-api</artifactId>
    <name>eol :: eol-api</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>authority</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>

        <!-- TEST DEPENDENCIES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Add the root dependency-management entry**

In root `pom.xml` `<dependencyManagement><dependencies>`, add a new EXTERNAL-AUTHORITIES section (or alongside the existing sections) the entry:
```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>eol-api</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 3: Write the failing test**

`EolPageIdTest.java`:
```java
package com.naturalist.authority.eol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EolPageIdTest {

    @Test
    void nonBlankValueIsValid() {
        assertThat(EolPageId.of("1188585").isValid()).isTrue();
    }

    @Test
    void nullValueIsNotValid() {
        assertThat(new EolPageId(null).isValid()).isFalse();
    }

    @Test
    void blankValueIsNotValid() {
        assertThat(new EolPageId("  ").isValid()).isFalse();
    }
}
```

- [ ] **Step 4: Run it; verify it fails**

Run: `mvn -q -pl :eol-api -am test`
Expected: FAIL — `EolPageId` does not exist.

- [ ] **Step 5: Write `EolPageId`**

`EolPageId.java`:
```java
package com.naturalist.authority.eol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;

/**
 * EOL's native record identifier — a page id, shaped to EOL's API.
 * A {@link NamedValue}, not a {@code ValueObject}, and not a kernel id
 * type: it never crosses the authority port. Used inside the EOL family
 * to build deep-links (and to key the trait fetch in Phase 4).
 */
public record EolPageId(String value) implements NamedValue<String> {

    @JsonCreator
    public static EolPageId of(String value) {
        return new EolPageId(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank();
    }
}
```

- [ ] **Step 6: Write `Eol`**

`Eol.java`:
```java
package com.naturalist.authority.eol;

import com.naturalist.authority.AuthoritySource;

import java.net.URI;

/**
 * The EOL provider contract shared by all EOL clients (mock + real):
 * the {@code eol} {@link AuthoritySource} and EOL's deep-link URL
 * pattern. EOL knowledge stays here, out of the kernel.
 */
public final class Eol {

    public static final AuthoritySource SOURCE =
            new AuthoritySource("eol", "Encyclopedia of Life");

    public static URI deepLink(EolPageId id) {
        return URI.create("https://eol.org/pages/" + id.value());
    }

    private Eol() {
    }
}
```

- [ ] **Step 7: Run tests; verify pass**

Run: `mvn -q -pl :eol-api -am test`
Expected: PASS (3 tests).

- [ ] **Step 8: Stage (commit point)**

```bash
git add external-authorities/eol/eol-api/ pom.xml
# commit message: "feat(eol): add eol-api with EolPageId and Eol contract"
```

---

### Task 7: `eol-client-mock` — `EolClientMock` (TDD)

**Files:**
- Create: `external-authorities/eol/eol-client-mock/pom.xml`
- Create dirs: `.../eol-client-mock/src/main/java/com/naturalist/authority/eol/`, `.../src/test/java/com/naturalist/authority/eol/`
- Test: `EolClientMockTest.java`
- Create: `EolClientMock.java`
- Modify: root `pom.xml` (dependencyManagement entry for `eol-client-mock`)

- [ ] **Step 1: Create the module pom**

`external-authorities/eol/eol-client-mock/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>eol</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>eol-client-mock</artifactId>
    <name>eol :: eol-client-mock</name>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>eol-api</artifactId>
        </dependency>

        <!-- TEST DEPENDENCIES -->
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```
(`eol-api` brings `authority` + `framework` transitively onto the compile classpath.)

- [ ] **Step 2: Add the root dependency-management entry**

In root `pom.xml`, add alongside the `eol-api` entry:
```xml
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>eol-client-mock</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 3: Write the failing test**

`EolClientMockTest.java`:
```java
package com.naturalist.authority.eol;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.InvariantViolationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EolClientMockTest {

    private static final TestSubjectName SWALLOWTAIL = new TestSubjectName("battus-philenor");
    private static final TestSubjectName UNKNOWN = new TestSubjectName("unobtainium-bug");

    private static AuthorityReference swallowtailRef() {
        return new AuthorityReference(Eol.SOURCE, Eol.deepLink(new EolPageId("1188585")));
    }

    @Test
    void sourceIsEol() {
        EolClientMock client = EolClientMock.seededWith(Map.of());

        assertThat(client.source()).isEqualTo(Eol.SOURCE);
    }

    @Test
    void knownSubjectReturnsSeededReferences() {
        EolClientMock client = EolClientMock.seededWith(
                Map.of(SWALLOWTAIL, Set.of(swallowtailRef())));

        assertThat(client.lookup(SWALLOWTAIL)).containsExactly(swallowtailRef());
    }

    @Test
    void unknownSubjectReturnsEmptySet() {
        EolClientMock client = EolClientMock.seededWith(
                Map.of(SWALLOWTAIL, Set.of(swallowtailRef())));

        assertThat(client.lookup(UNKNOWN)).isEmpty();
    }

    @Test
    void nullSubjectIsRejected() {
        EolClientMock client = EolClientMock.seededWith(Map.of());

        assertThatThrownBy(() -> client.lookup(null))
                .isInstanceOf(InvariantViolationException.class);
    }

    @Test
    void seededWithDefensivelyCopies() {
        Map<EntityName, Set<AuthorityReference>> mutable = new HashMap<>();
        mutable.put(SWALLOWTAIL, Set.of(swallowtailRef()));
        EolClientMock client = EolClientMock.seededWith(mutable);

        mutable.clear();

        assertThat(client.lookup(SWALLOWTAIL)).containsExactly(swallowtailRef());
    }

    /** Test-only EntityName subtype — keeps external-authorities free of any domain dependency. */
    private static final class TestSubjectName extends EntityName {
        private TestSubjectName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 100;
        }
    }
}
```

- [ ] **Step 4: Run it; verify it fails**

Run: `mvn -q -pl :eol-client-mock -am test`
Expected: FAIL — `EolClientMock` does not exist.

- [ ] **Step 5: Write `EolClientMock`**

`EolClientMock.java`:
```java
package com.naturalist.authority.eol;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;
import com.naturalist.resilience.ResilienceExempt;

import java.util.Map;
import java.util.Set;

/**
 * Fixture EOL client: an in-memory {@link ExternalAuthority} answering
 * from a seeded map. The Phase-1 stand-in for the real EOL HTTP client.
 */
@ResilienceExempt(reason = "in-memory fixture client; performs no I/O")
public final class EolClientMock implements ExternalAuthority {

    private static final Observer observer = Observer.forClass(EolClientMock.class);

    private final Map<EntityName, Set<AuthorityReference>> fixtures;

    private EolClientMock(Map<EntityName, Set<AuthorityReference>> fixtures) {
        this.fixtures = Map.copyOf(fixtures);
    }

    public static EolClientMock seededWith(Map<EntityName, Set<AuthorityReference>> fixtures) {
        return new EolClientMock(fixtures);
    }

    @Override
    public AuthoritySource source() {
        return Eol.SOURCE;
    }

    @Override
    public Set<AuthorityReference> lookup(EntityName subject) {
        observer.arguments("lookup", a -> a.notNull(subject, "subject")).throwWhenInvalid();
        return fixtures.getOrDefault(subject, Set.of());
    }
}
```

- [ ] **Step 6: Run tests; verify pass**

Run: `mvn -q -pl :eol-client-mock -am test`
Expected: PASS (5 tests).

If `InvariantViolationException` is not found at that import, locate it with `find kernels/framework -name 'InvariantViolationException.java'` and correct the import; the type is what `throwWhenInvalid()` raises.

- [ ] **Step 7: Stage (commit point)**

```bash
git add external-authorities/eol/eol-client-mock/ pom.xml
# commit message: "feat(eol): add EolClientMock fixture client"
```

---

### Task 8: Work-tracker correction + full-reactor checkpoint

**Files:**
- Modify: `docs/work-tracker.md`

- [ ] **Step 1: Correct the bibliography phase mislabel**

In `docs/work-tracker.md`, the "Current phase" line (~line 16) and the candidate-slice line (~line 24) describe `kernels/bibliography` as a Phase 1 candidate. Update them so the current phase reads as the **external-authority seam (`kernels/authority` port + EOL mock client)** for Phase 1, and `kernels/bibliography` is shown under **Phase 2** (with the workflow that consumes it), matching `plans/identification.md`. Keep the edit to those two lines plus any one-line "last updated" note; do not restructure the file.

- [ ] **Step 2: Full reactor build (human-run checkpoint)**

Run: `mvn verify`
Expected: BUILD SUCCESS; the new modules `authority`, `eol-api`, `eol-client-mock` are in the reactor and all their tests pass.

- [ ] **Step 3: Stage (commit point)**

```bash
git add docs/work-tracker.md
# commit message: "docs: point current phase at external-authority seam; bibliography → Phase 2"
```

---

## Self-Review (completed during authoring)

- **Spec coverage:** kernel port (`ExternalAuthority`) ✓ Task 4; `AuthorityReference(source,url)` ✓ Task 3; `AuthoritySource` ✓ Task 2; per-provider identity in `eol-api` (`EolPageId` as `NamedValue`, `Eol.deepLink`) ✓ Task 6; EOL mock with boundary validation + empty-for-unknown + defensive copy + `@ResilienceExempt` ✓ Task 7; module grouping `external-authorities/eol/{eol-api,eol-client-mock}` ✓ Tasks 5–7; work-tracker correction ✓ Task 8. Deferred items (real `eol-client`, console/storage, API-key, bibliography) carry no task — correct, they are out of scope.
- **Type consistency:** `ExternalAuthority.lookup(EntityName)` / `source()` match `EolClientMock`; `AuthorityReference(AuthoritySource, URI)` constructor used identically in Tasks 3 and 7; `Eol.SOURCE` / `Eol.deepLink(EolPageId)` / `EolPageId.of` consistent across Tasks 6–7; `invariants()` returns `Consumer<? extends Constraints>` matching the framework signature.
- **Placeholder scan:** none — every step carries complete code or exact pom XML.
