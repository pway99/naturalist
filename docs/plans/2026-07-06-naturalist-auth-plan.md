# Naturalist Auth / Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let naturalists log into the management console with a password (alongside the existing config admin), and establish a session-bound "current naturalist" seam.

**Architecture:** Add a `NaturalistCredential` record + read stack to the naturalists domain (credentials stored off the `Naturalist` record). In the management-console app, a composite `UserDetailsService` authenticates the admin (config) first, then falls back to a naturalist credential lookup, building an explicit `NaturalistPrincipal` that carries the `NaturalistName`. A single `CurrentNaturalist` seam (bean + static template view) is the only place — besides the `UserDetailsService` — that reads the `SecurityContext`.

**Tech Stack:** Java 21 records, Spring Boot 3 / Spring Security 6, JTE templates, the project's kernel framework (`NamedEntity`, `EntityQuery`, `AbstractEntityQuery`, `BehavioralCollection`, `TestEntitySource`, `@DomainService` + `DomainServiceScan`).

## Global Constraints

- **Design source of truth:** `docs/plans/2026-07-06-naturalist-auth-design.md`. Every decision here traces to it.
- **Identity:** `Naturalist` and `NaturalistCredential` are both `NamedEntity<NaturalistName>` (natural-key slug). No UUIDs, no raw String/Long/UUID references. `NaturalistName` already exists in `domains/identifiers`.
- **Credentials off the ecological record:** never add a password field to `Naturalist`. Credentials are a separate record.
- **Login identity = the `NaturalistName` slug** as the form username (e.g. `patrick-way`). No email/username field.
- **Password hashing = bcrypt** via Spring's delegating `PasswordEncoder`; stored hashes are `{bcrypt}$2a$…`.
- **Seam discipline (rule):** nothing outside `NaturalistUserDetailsService`, `CurrentNaturalist` (bean), and `CurrentNaturalistView` (static template view) may read `SecurityContextHolder`, the raw `Authentication`, or usernames. Consumers get a `NaturalistName`/`Naturalist` from the seam.
- **No build tool invocation by the assistant.** The user runs `mvn verify` from the repo root. Where a step says "run the test," that is an instruction for the user/executor; do not invoke `mvn` yourself.
- **Commits:** stage changes (`git add`); the user runs the actual `git commit`. The commit commands shown are the suggested message for the user.
- **Package convention:** naturalists api/core/repository-test all use the split package `com.naturalist.naturalist`. App auth code lives in `com.naturalist.console.auth` (covered by the app's `scanBasePackages = "com.naturalist"`).
- **`@DomainService` autowiring:** query impls and repository mocks are annotated `@DomainService`; `DomainServiceScan` (already imported by the app) registers them as beans. No `DomainServiceScan` change is needed — `com.naturalist` is already the scan root.

---

## File Structure

**Domain — `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/`**
- `NaturalistCredential.java` — new `NamedEntity<NaturalistName>` record: `(name, passwordHash)`.
- `NaturalistEntityCollections.java` — `BehavioralCollection` namespace: `NaturalistCollection`, `NaturalistCredentialCollection`.
- `NaturalistQuery.java` — public read port for `Naturalist` (`EntityQuery`).
- `NaturalistCredentialQuery.java` — public read port for `NaturalistCredential` (`EntityQuery`).
- `NaturalistRepository.java` — package-private namespace class with two nested `protected` repository interfaces.

**Domain — `domains/naturalists/naturalists-core/src/main/java/com/naturalist/naturalist/`** (currently empty)
- `NaturalistQueryImpl.java` — `@DomainService`, extends `AbstractEntityQuery`.
- `NaturalistCredentialQueryImpl.java` — `@DomainService`, extends `AbstractEntityQuery`.

**Domain — `domains/naturalists/naturalists-repository-test/`**
- `src/main/java/.../NaturalistEntityRepositoryMock.java` — `@DomainService`.
- `src/main/java/.../NaturalistCredentialRepositoryMock.java` — `@DomainService`.
- `src/main/java/.../NaturalistCredentialTestEntitySource.java` — loads the credentials JSON.
- `src/main/java/.../NaturalistEntityRepositoryTest.java` — contract interface.
- `src/main/java/.../NaturalistCredentialEntityRepositoryTest.java` — contract interface.
- `src/main/resources/naturalists/naturalists.json` — **modify**: add a second naturalist.
- `src/main/resources/naturalists/naturalist-credentials.json` — **new** seed file.
- `src/test/java/.../NaturalistEntityRepositoryMockTest.java` — binds contract to mock.
- `src/test/java/.../NaturalistCredentialRepositoryMockTest.java` — binds contract to mock.

**App — `apps/management-console/`**
- `pom.xml` — **modify**: add `naturalists-api`, `naturalists-core`, `naturalists-repository-test`.
- `src/main/java/com/naturalist/console/SecurityConfiguration.java` — **modify**: drop the in-memory admin `UserDetailsService`, add a `PasswordEncoder` bean.
- `src/main/java/com/naturalist/console/auth/NaturalistPrincipal.java` — new `UserDetails`.
- `src/main/java/com/naturalist/console/auth/NaturalistUserDetailsService.java` — composite service.
- `src/main/java/com/naturalist/console/auth/CurrentNaturalist.java` — seam interface.
- `src/main/java/com/naturalist/console/auth/SecurityContextCurrentNaturalist.java` — seam bean impl.
- `src/main/java/com/naturalist/console/auth/CurrentNaturalistView.java` — static template-facing view.
- `src/main/jte/layout/page.jte` — **modify**: header "logged in as … · Logout".
- `src/test/java/com/naturalist/console/auth/NaturalistLoginWebMvcTest.java` — form-login + route authorization.
- `src/test/java/com/naturalist/console/auth/CurrentNaturalistTest.java` — seam unit test.
- `domains/naturalists/CLAUDE.md` — **new** short domain note (credential invariant + seam pointer).

---

## Task 1: Naturalist read stack (query + repository + contract)

Establishes the naturalists domain's first read port. No auth yet — pure domain plumbing, verified by the repository contract test.

**Files:**
- Create: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistEntityCollections.java`
- Create: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistQuery.java`
- Create: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistRepository.java`
- Create: `domains/naturalists/naturalists-core/src/main/java/com/naturalist/naturalist/NaturalistQueryImpl.java`
- Create: `domains/naturalists/naturalists-repository-test/src/main/java/com/naturalist/naturalist/NaturalistEntityRepositoryMock.java`
- Create: `domains/naturalists/naturalists-repository-test/src/main/java/com/naturalist/naturalist/NaturalistEntityRepositoryTest.java`
- Modify: `domains/naturalists/naturalists-repository-test/src/main/resources/naturalists/naturalists.json`
- Test: `domains/naturalists/naturalists-repository-test/src/test/java/com/naturalist/naturalist/NaturalistEntityRepositoryMockTest.java`

**Interfaces:**
- Consumes: `Naturalist`, `NaturalistName` (existing); framework `NamedEntity`, `EntityQuery`, `AbstractEntityQuery`, `EntityRepository`, `AbstractTestEntityRepository`, `BehavioralCollection`, `NaturalistDatabase`, `@DomainService`; `NaturalistTestEntitySource` (existing).
- Produces: `NaturalistQuery extends EntityQuery<NaturalistName, Naturalist, NaturalistCollection>`; `NaturalistRepository.NaturalistEntityRepository extends EntityRepository<NaturalistName, Naturalist>`; `NaturalistEntityCollections.NaturalistCollection`.

- [ ] **Step 1: Add a second naturalist to the seed JSON** (contract tests need ≥2 known names)

Replace the contents of `domains/naturalists/naturalists-repository-test/src/main/resources/naturalists/naturalists.json` with:

```json
[
  {
    "name": "patrick-way",
    "givenName": "Patrick",
    "familyName": "Way",
    "role": "CARETAKER",
    "stage": "NATURALIST",
    "notes": "Principal caretaker and systems architect of Oak Vista. B.S. Environmental Science / Chemistry minor. 20+ years software engineering. Confirmed Pipevine Swallowtail eggs on Aristolochia californica April 2026. Manages Saskatraz honey bee colony and all soil amendment programmes."
  },
  {
    "name": "delia-durrell",
    "givenName": "Delia",
    "familyName": "Durrell",
    "role": "TEACHER",
    "stage": "PRACTITIONER",
    "notes": "Leads school ecology visits at Oak Vista."
  }
]
```

- [ ] **Step 2: Write the collections namespace**

Create `NaturalistEntityCollections.java` with **only** `NaturalistCollection` (Task 2 adds `NaturalistCredentialCollection` to this same file — keeping it out now avoids referencing `NaturalistCredential` before it exists):

```java
package com.naturalist.naturalist;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

/**
 * Namespace for the naturalists sub-context's {@link BehavioralCollection}
 * return types. One file per namespace, nested types for everything inside (ADR-020).
 */
public interface NaturalistEntityCollections {

    final class NaturalistCollection extends BehavioralCollection<Naturalist> {

        NaturalistCollection(Collection<Naturalist> naturalists) {
            super(naturalists);
        }

        public static NaturalistCollection of(Collection<Naturalist> naturalists) {
            return new NaturalistCollection(naturalists);
        }

        public static NaturalistCollection empty() {
            return new NaturalistCollection(List.of());
        }
    }
}
```

- [ ] **Step 3: Write the repository namespace**

Create `NaturalistRepository.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.EntityRepository;

/**
 * Namespace for the naturalists sub-context's write-side repositories.
 *
 * <p>A {@code class}, not an {@code interface}, so nested repository contracts stay
 * {@code protected} — hidden from foreign packages while permitting same-package
 * adapter implementations (ADR-020). Non-instantiable.
 */
class NaturalistRepository {

    private NaturalistRepository() {
    }

    protected interface NaturalistEntityRepository
            extends EntityRepository<NaturalistName, Naturalist> {
    }
}
```

- [ ] **Step 4: Write the query port**

Create `NaturalistQuery.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCollection;

/**
 * Read port for {@link Naturalist} entities. Identity at the port is the
 * {@link NaturalistName} (ADR-021).
 */
public interface NaturalistQuery
        extends EntityQuery<NaturalistName, Naturalist, NaturalistCollection> {
}
```

- [ ] **Step 5: Write the failing contract test binding**

Create the contract interface `NaturalistEntityRepositoryTest.java` (in `src/main/java`, mirroring `PlantEntityRepositoryTest`):

```java
package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link NaturalistRepository.NaturalistEntityRepository}.
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 */
interface NaturalistEntityRepositoryTest
        extends EntityRepositoryTest<NaturalistName, Naturalist> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");
    NaturalistName NOT_FOUND = NaturalistName.of("nobody-here");

    @Override
    NaturalistRepository.NaturalistEntityRepository repository();

    @Override
    default TestEntitySource<NaturalistName, Naturalist> source() {
        return db.getNamed(NaturalistTestEntitySource.class);
    }

    @Override
    default NaturalistName notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<NaturalistName> knownEntityNames() {
        return List.of(PATRICK, DELIA);
    }

    @Override
    default Naturalist newEntity() {
        return new Naturalist(
                NaturalistName.of(RandomValue.string()),
                "Given" + RandomValue.string(),
                "Family" + RandomValue.string(),
                NaturalistRole.VISITOR,
                EcologicalStage.WONDER,
                "notes " + RandomValue.string());
    }

    @Override
    default Naturalist ghostEntity() {
        return new Naturalist(
                NaturalistName.of(RandomValue.string()),
                "Given" + RandomValue.string(),
                null,
                NaturalistRole.VISITOR,
                EcologicalStage.WONDER,
                null);
    }

    @Override
    default Naturalist modifiedEntity(Naturalist original) {
        return new Naturalist(
                original.name(),
                "Changed" + RandomValue.string(),
                "Changed" + RandomValue.string(),
                NaturalistRole.KEEPER,
                EcologicalStage.NATURALIST,
                "changed " + RandomValue.string());
    }
}
```

Create the binding test `NaturalistEntityRepositoryMockTest.java` (in `src/test/java`):

```java
package com.naturalist.naturalist;

class NaturalistEntityRepositoryMockTest implements NaturalistEntityRepositoryTest {

    @Override
    public NaturalistRepository.NaturalistEntityRepository repository() {
        return new NaturalistEntityRepositoryMock(db);
    }
}
```

- [ ] **Step 6: Run the contract test to verify it fails**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-test test -Dtest=NaturalistEntityRepositoryMockTest`
Expected: FAIL — `NaturalistEntityRepositoryMock` does not exist (compile error), so the mock and query impl must be written.

- [ ] **Step 7: Write the query implementation**

Create `NaturalistQueryImpl.java` in naturalists-core:

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCollection;

import java.util.Set;

@DomainService
class NaturalistQueryImpl
        extends AbstractEntityQuery<
        NaturalistName,
        Naturalist,
        NaturalistCollection,
        NaturalistRepository.NaturalistEntityRepository>
        implements NaturalistQuery {

    NaturalistQueryImpl(NaturalistRepository.NaturalistEntityRepository repository) {
        super(repository);
    }

    @Override
    public NaturalistCollection findByNameSet(Set<NaturalistName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return NaturalistCollection.of(repository().getByEntityNameSet(names));
    }
}
```

- [ ] **Step 8: Write the repository mock**

Create `NaturalistEntityRepositoryMock.java` in naturalists-repository-test `src/main/java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class NaturalistEntityRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, Naturalist, NaturalistTestEntitySource>
        implements NaturalistRepository.NaturalistEntityRepository {

    NaturalistEntityRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 9: Run the contract test to verify it passes**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-test test -Dtest=NaturalistEntityRepositoryMockTest`
Expected: PASS — all inherited `EntityRepositoryTest` cases green (getByName, set lookups, paging, insert/update).

- [ ] **Step 10: Stage and commit**

```bash
git add domains/naturalists/naturalists-api domains/naturalists/naturalists-core domains/naturalists/naturalists-repository-test
git commit -m "feat(naturalists): add Naturalist read stack (query + repository + contract)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: NaturalistCredential store

Adds the credential record and its read stack, keyed by `NaturalistName`, off the `Naturalist` record. Verified by a second repository contract test.

**Files:**
- Create: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistCredential.java`
- Modify: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistEntityCollections.java` (add `NaturalistCredentialCollection`)
- Modify: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistRepository.java` (add `CredentialRepository`)
- Create: `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/NaturalistCredentialQuery.java`
- Create: `domains/naturalists/naturalists-core/src/main/java/com/naturalist/naturalist/NaturalistCredentialQueryImpl.java`
- Create: `domains/naturalists/naturalists-repository-test/src/main/java/com/naturalist/naturalist/NaturalistCredentialTestEntitySource.java`
- Create: `domains/naturalists/naturalists-repository-test/src/main/resources/naturalists/naturalist-credentials.json`
- Create: `domains/naturalists/naturalists-repository-test/src/main/java/com/naturalist/naturalist/NaturalistCredentialRepositoryMock.java`
- Create: `domains/naturalists/naturalists-repository-test/src/main/java/com/naturalist/naturalist/NaturalistCredentialEntityRepositoryTest.java`
- Test: `domains/naturalists/naturalists-repository-test/src/test/java/com/naturalist/naturalist/NaturalistCredentialRepositoryMockTest.java`

**Interfaces:**
- Consumes: everything from Task 1 plus `Constraints` (`entityName`, `notBlank`).
- Produces: `NaturalistCredential(name, passwordHash)`; `NaturalistCredentialQuery extends EntityQuery<NaturalistName, NaturalistCredential, NaturalistCredentialCollection>`; `NaturalistRepository.CredentialRepository`. `passwordHash` accessor: `String passwordHash()`.

- [ ] **Step 1: Write the credential record**

Create `NaturalistCredential.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Authentication credential for a {@link Naturalist}, keyed 1:1 by the same
 * {@link NaturalistName}. Deliberately separate from the {@code Naturalist}
 * ecological record: a naturalist is an actor, not an account. {@code passwordHash}
 * is an encoded (bcrypt, {@code {bcrypt}$2a$…}) hash produced by the app's
 * {@code PasswordEncoder} — never a plaintext password.
 */
public record NaturalistCredential(
        NaturalistName name,
        String passwordHash
) implements NamedEntity<NaturalistName> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(passwordHash, "passwordHash");
    }
}
```

- [ ] **Step 2: Add `NaturalistCredentialCollection` to the collections namespace**

In `NaturalistEntityCollections.java`, add this nested class after `NaturalistCollection`:

```java
    final class NaturalistCredentialCollection extends BehavioralCollection<NaturalistCredential> {

        NaturalistCredentialCollection(java.util.Collection<NaturalistCredential> credentials) {
            super(credentials);
        }

        public static NaturalistCredentialCollection of(java.util.Collection<NaturalistCredential> credentials) {
            return new NaturalistCredentialCollection(credentials);
        }

        public static NaturalistCredentialCollection empty() {
            return new NaturalistCredentialCollection(java.util.List.of());
        }
    }
```

- [ ] **Step 3: Add the credential repository to the namespace**

In `NaturalistRepository.java`, add inside the class body after `NaturalistEntityRepository`:

```java
    protected interface CredentialRepository
            extends EntityRepository<NaturalistName, NaturalistCredential> {
    }
```

- [ ] **Step 4: Write the credential query port**

Create `NaturalistCredentialQuery.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.EntityQuery;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCredentialCollection;

/**
 * Read port for {@link NaturalistCredential} entities. The console's
 * {@code UserDetailsService} is its only caller — {@code getByName} resolves a
 * credential by the naturalist's slug.
 */
public interface NaturalistCredentialQuery
        extends EntityQuery<NaturalistName, NaturalistCredential, NaturalistCredentialCollection> {
}
```

- [ ] **Step 5: Write the credential test entity source + seed JSON**

Create `NaturalistCredentialTestEntitySource.java`:

```java
package com.naturalist.naturalist;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class NaturalistCredentialTestEntitySource
        extends TestEntitySource<NaturalistName, NaturalistCredential> {

    public NaturalistCredentialTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("naturalists/naturalist-credentials.json");
    }
}
```

Create `naturalist-credentials.json` with placeholder hashes — **the real `{bcrypt}` values are filled in Task 3, Step 1** (they require the app's `PasswordEncoder`). For now use any non-blank strings so the contract test (which does not verify hashes) passes:

```json
[
  {
    "name": "patrick-way",
    "passwordHash": "{noop}REPLACE_IN_TASK_3"
  },
  {
    "name": "delia-durrell",
    "passwordHash": "{noop}REPLACE_IN_TASK_3"
  }
]
```

- [ ] **Step 6: Write the failing credential contract test**

Create contract interface `NaturalistCredentialEntityRepositoryTest.java` (`src/main/java`):

```java
package com.naturalist.naturalist;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;

/**
 * Behavioral contract for {@link NaturalistRepository.CredentialRepository}.
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 */
interface NaturalistCredentialEntityRepositoryTest
        extends EntityRepositoryTest<NaturalistName, NaturalistCredential> {

    NaturalistName PATRICK = NaturalistName.of("patrick-way");
    NaturalistName DELIA = NaturalistName.of("delia-durrell");
    NaturalistName NOT_FOUND = NaturalistName.of("nobody-here");

    @Override
    NaturalistRepository.CredentialRepository repository();

    @Override
    default TestEntitySource<NaturalistName, NaturalistCredential> source() {
        return db.getNamed(NaturalistCredentialTestEntitySource.class);
    }

    @Override
    default NaturalistName notFoundName() {
        return NOT_FOUND;
    }

    @Override
    default List<NaturalistName> knownEntityNames() {
        return List.of(PATRICK, DELIA);
    }

    @Override
    default NaturalistCredential newEntity() {
        return new NaturalistCredential(
                NaturalistName.of(RandomValue.string()),
                "{bcrypt}" + RandomValue.string());
    }

    @Override
    default NaturalistCredential ghostEntity() {
        return new NaturalistCredential(
                NaturalistName.of(RandomValue.string()),
                "{bcrypt}" + RandomValue.string());
    }

    @Override
    default NaturalistCredential modifiedEntity(NaturalistCredential original) {
        return new NaturalistCredential(
                original.name(),
                "{bcrypt}changed-" + RandomValue.string());
    }
}
```

Create binding `NaturalistCredentialRepositoryMockTest.java` (`src/test/java`):

```java
package com.naturalist.naturalist;

class NaturalistCredentialRepositoryMockTest implements NaturalistCredentialEntityRepositoryTest {

    @Override
    public NaturalistRepository.CredentialRepository repository() {
        return new NaturalistCredentialRepositoryMock(db);
    }
}
```

- [ ] **Step 7: Run to verify it fails**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-test test -Dtest=NaturalistCredentialRepositoryMockTest`
Expected: FAIL — `NaturalistCredentialRepositoryMock` and `NaturalistCredentialQueryImpl` do not exist.

- [ ] **Step 8: Write the credential query impl + mock**

Create `NaturalistCredentialQueryImpl.java` (naturalists-core):

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistEntityCollections.NaturalistCredentialCollection;

import java.util.Set;

@DomainService
class NaturalistCredentialQueryImpl
        extends AbstractEntityQuery<
        NaturalistName,
        NaturalistCredential,
        NaturalistCredentialCollection,
        NaturalistRepository.CredentialRepository>
        implements NaturalistCredentialQuery {

    NaturalistCredentialQueryImpl(NaturalistRepository.CredentialRepository repository) {
        super(repository);
    }

    @Override
    public NaturalistCredentialCollection findByNameSet(Set<NaturalistName> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return NaturalistCredentialCollection.of(repository().getByEntityNameSet(names));
    }
}
```

Create `NaturalistCredentialRepositoryMock.java` (naturalists-repository-test `src/main/java`):

```java
package com.naturalist.naturalist;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class NaturalistCredentialRepositoryMock
        extends AbstractTestEntityRepository<NaturalistName, NaturalistCredential, NaturalistCredentialTestEntitySource>
        implements NaturalistRepository.CredentialRepository {

    NaturalistCredentialRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 9: Run to verify it passes**

Run: `mvn -q -pl domains/naturalists/naturalists-repository-test test -Dtest=NaturalistCredentialRepositoryMockTest`
Expected: PASS.

- [ ] **Step 10: Stage and commit**

```bash
git add domains/naturalists
git commit -m "feat(naturalists): add NaturalistCredential store keyed by NaturalistName

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Wire naturalists into the app + composite authentication

Puts the naturalists domain on the app classpath, adds a bcrypt `PasswordEncoder`, and replaces the single in-memory admin `UserDetailsService` with a composite that also authenticates naturalists. Verified by a real form-login WebMvc test.

**Files:**
- Modify: `apps/management-console/pom.xml`
- Modify: `apps/management-console/src/main/java/com/naturalist/console/SecurityConfiguration.java`
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/NaturalistPrincipal.java`
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/NaturalistUserDetailsService.java`
- Modify: `domains/naturalists/naturalists-repository-test/src/main/resources/naturalists/naturalist-credentials.json` (real hashes)
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/NaturalistLoginWebMvcTest.java`

**Interfaces:**
- Consumes: `NaturalistQuery`, `NaturalistCredentialQuery`, `Naturalist`, `NaturalistCredential`, `NaturalistName`, `AdminProperties`, Spring Security `UserDetails`, `UserDetailsService`, `PasswordEncoder`.
- Produces: `NaturalistPrincipal implements UserDetails` with `NaturalistName naturalistName()` and `String givenName()`; `NaturalistUserDetailsService implements UserDetailsService`; a `PasswordEncoder` bean.

- [ ] **Step 1: Generate the real bcrypt seed hashes**

Create a throwaway printer test `apps/management-console/src/test/java/com/naturalist/console/auth/SeedHashPrinter.java`:

```java
package com.naturalist.console.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class SeedHashPrinter {

    @Test
    void printHashes() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        System.out.println("patrick-way -> " + encoder.encode("durrell"));
        System.out.println("delia-durrell -> " + encoder.encode("gerald"));
    }
}
```

Run: `mvn -q -pl apps/management-console test -Dtest=SeedHashPrinter`
Copy the two `{bcrypt}$2a$…` values from stdout into `naturalist-credentials.json`, replacing the `{noop}REPLACE_IN_TASK_3` placeholders:

```json
[
  { "name": "patrick-way",   "passwordHash": "{bcrypt}$2a$10$PASTE_PATRICK_HASH" },
  { "name": "delia-durrell", "passwordHash": "{bcrypt}$2a$10$PASTE_DELIA_HASH" }
]
```

Then delete `SeedHashPrinter.java`. (Passwords: `patrick-way`/`durrell`, `delia-durrell`/`gerald` — documented here so the login test and the user know them.)

- [ ] **Step 2: Add naturalists dependencies to the app pom**

In `apps/management-console/pom.xml`, add to the `<!-- DOMAIN CONSOLE MODULES -->` group (or a new `<!-- NATURALISTS -->` group):

```xml
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>naturalists-repository-test</artifactId>
        </dependency>
```

- [ ] **Step 3: Write the failing login WebMvc test**

Create `NaturalistLoginWebMvcTest.java`:

```java
package com.naturalist.console.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies naturalist form login and route authorization alongside the admin.
 */
@SpringBootTest
class NaturalistLoginWebMvcTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void naturalist_validCredentials_authenticates() throws Exception {
        mockMvc.perform(formLogin("/login").user("patrick-way").password("durrell"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/"));
    }

    @Test
    void naturalist_wrongPassword_redirectsToError() throws Exception {
        mockMvc.perform(formLogin("/login").user("patrick-way").password("wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void unknownUsername_redirectsToError() throws Exception {
        mockMvc.perform(formLogin("/login").user("no-such-naturalist").password("x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login?error"));
    }

    @Test
    void naturalist_forbiddenFromAdminRoute() throws Exception {
        mockMvc.perform(get("/admin/anything").with(user("patrick-way").roles("NATURALIST")))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 4: Run to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=NaturalistLoginWebMvcTest`
Expected: FAIL — the current in-memory `UserDetailsService` only knows the admin, so `patrick-way` login fails / returns `/login?error` for the valid-credentials case.

- [ ] **Step 5: Write the principal**

Create `NaturalistPrincipal.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated naturalist principal. Carries the {@link NaturalistName} so the
 * {@link CurrentNaturalist} seam can resolve identity without re-parsing a username.
 * Building the principal is the single point where a username becomes a NaturalistName;
 * every downstream reader sees the typed name (see design: seam discipline).
 */
public final class NaturalistPrincipal implements UserDetails {

    private static final Collection<GrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_NATURALIST"));

    private final NaturalistName naturalistName;
    private final String givenName;
    private final String passwordHash;

    public NaturalistPrincipal(NaturalistName naturalistName, String givenName, String passwordHash) {
        this.naturalistName = naturalistName;
        this.givenName = givenName;
        this.passwordHash = passwordHash;
    }

    public NaturalistName naturalistName() {
        return naturalistName;
    }

    public String givenName() {
        return givenName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return naturalistName.value();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 6: Write the composite UserDetailsService**

Create `NaturalistUserDetailsService.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.console.admin.AdminProperties;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistCredentialQuery;
import com.naturalist.naturalist.NaturalistQuery;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates the config admin first (unchanged {@link AdminProperties} path,
 * {@code ROLE_ADMIN}); otherwise resolves a {@link com.naturalist.naturalist.NaturalistCredential}
 * by slug and builds a {@link NaturalistPrincipal} ({@code ROLE_NATURALIST}). This class
 * and {@link CurrentNaturalist}/{@link CurrentNaturalistView} are the only places that
 * turn a username into a naturalist identity (design: seam discipline).
 */
@Service
class NaturalistUserDetailsService implements UserDetailsService {

    private final AdminProperties admin;
    private final PasswordEncoder passwordEncoder;
    private final NaturalistQuery naturalistQuery;
    private final NaturalistCredentialQuery credentialQuery;

    NaturalistUserDetailsService(AdminProperties admin,
                                 PasswordEncoder passwordEncoder,
                                 NaturalistQuery naturalistQuery,
                                 NaturalistCredentialQuery credentialQuery) {
        this.admin = admin;
        this.passwordEncoder = passwordEncoder;
        this.naturalistQuery = naturalistQuery;
        this.credentialQuery = credentialQuery;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (admin.username().equals(username)) {
            return User.withUsername(admin.username())
                    .password(passwordEncoder.encode(admin.password()))
                    .roles("ADMIN")
                    .build();
        }

        NaturalistName name = NaturalistName.of(username);
        if (name.isNotValid()) {
            throw new UsernameNotFoundException(username);
        }

        var credential = credentialQuery.getByName(name)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        var naturalist = naturalistQuery.getByName(name)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        return new NaturalistPrincipal(name, naturalist.givenName(), credential.passwordHash());
    }
}
```

- [ ] **Step 7: Update SecurityConfiguration — drop the in-memory admin, add PasswordEncoder**

Replace the body of `SecurityConfiguration.java` with (filter chain unchanged; the `userDetailsService` bean is removed because `NaturalistUserDetailsService` is now the `@Service`; a delegating `PasswordEncoder` bean is added):

```java
package com.naturalist.console;

import com.naturalist.console.admin.AdminProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AdminProperties.class)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
```

> The `/` path is `permitAll`, so a valid form login redirecting to `/` returns 200 for anyone; the test asserts the redirect target is `/`, which is Spring Security's default success URL. Admin authentication is now handled inside `NaturalistUserDetailsService`, matched by the delegating `PasswordEncoder` (both admin and naturalist passwords resolve to `{bcrypt}`).

- [ ] **Step 8: Run to verify it passes**

Run: `mvn -q -pl apps/management-console test -Dtest=NaturalistLoginWebMvcTest`
Expected: PASS — valid naturalist login redirects to `/`; wrong password and unknown user redirect to `/login?error`; naturalist is forbidden from `/admin/**`.

- [ ] **Step 9: Confirm the existing admin security test still passes**

Run: `mvn -q -pl apps/management-console test -Dtest=AdminSecurityWebMvcTest`
Expected: PASS — anonymous → `/login`, non-admin → 403, admin (`test-admin`) → 404 on unmapped `/admin/**`. (The test admin credentials come from `src/test/resources/application.yml`: `test-admin`/`test-password`.)

- [ ] **Step 10: Stage and commit**

```bash
git add apps/management-console domains/naturalists/naturalists-repository-test/src/main/resources/naturalists/naturalist-credentials.json
git commit -m "feat(console): authenticate naturalists alongside the config admin

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: CurrentNaturalist seam

The single, testable resolver of session identity that the future collection feature (and this task's header) consume.

**Files:**
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/CurrentNaturalist.java`
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/SecurityContextCurrentNaturalist.java`
- Create: `apps/management-console/src/main/java/com/naturalist/console/auth/CurrentNaturalistView.java`
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/CurrentNaturalistTest.java`

**Interfaces:**
- Consumes: `NaturalistPrincipal` (Task 3), `NaturalistQuery`, `Naturalist`, `NaturalistName`, Spring `SecurityContextHolder`, `RequestContextHolder`, `CsrfToken`.
- Produces: `CurrentNaturalist` with `Optional<NaturalistName> name()` and `Optional<Naturalist> naturalist()`; `CurrentNaturalistView` static `displayName()`, `isAuthenticated()`, `csrfToken()`.

- [ ] **Step 1: Write the failing seam unit test**

Create `CurrentNaturalistTest.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentNaturalistTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(NaturalistPrincipal principal) {
        var auth = new UsernamePasswordAuthenticationToken(
                principal, "n/a", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void displayName_forNaturalistPrincipal_returnsGivenName() {
        authenticateAs(new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x"));

        assertThat(CurrentNaturalistView.displayName()).isEqualTo("Patrick");
        assertThat(CurrentNaturalistView.isAuthenticated()).isTrue();
    }

    @Test
    void displayName_whenNotNaturalist_isNull() {
        // no authentication set → admin/anonymous
        assertThat(CurrentNaturalistView.displayName()).isNull();
    }

    @Test
    void currentNaturalistName_forNaturalistPrincipal_isPresent() {
        authenticateAs(new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x"));

        CurrentNaturalist seam = new SecurityContextCurrentNaturalist(name -> java.util.Optional.empty());
        assertThat(seam.name()).contains(NaturalistName.of("patrick-way"));
    }
}
```

> The test constructs `SecurityContextCurrentNaturalist` with a lambda `NaturalistQuery`-like function. To keep the seam trivially testable, `SecurityContextCurrentNaturalist` takes a small functional dependency rather than the full `NaturalistQuery`. See Step 3.

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=CurrentNaturalistTest`
Expected: FAIL — `CurrentNaturalist`, `SecurityContextCurrentNaturalist`, `CurrentNaturalistView` do not exist.

- [ ] **Step 3: Write the seam interface + bean**

Create `CurrentNaturalist.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;

import java.util.Optional;

/**
 * Session-identity seam. The only Java-facing way to learn who is acting.
 * Consumers (e.g. the future collection feature) depend on this, never on
 * {@code SecurityContextHolder} directly (design: seam discipline).
 */
public interface CurrentNaturalist {

    Optional<NaturalistName> name();

    Optional<Naturalist> naturalist();
}
```

Create `SecurityContextCurrentNaturalist.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.Naturalist;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.naturalist.NaturalistQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * Reads the {@link NaturalistPrincipal} off the {@link SecurityContextHolder}.
 * One of the three sanctioned SecurityContext readers.
 */
@Component
class SecurityContextCurrentNaturalist implements CurrentNaturalist {

    private final Function<NaturalistName, Optional<Naturalist>> byName;

    SecurityContextCurrentNaturalist(NaturalistQuery naturalistQuery) {
        this(naturalistQuery::getByName);
    }

    // Test-friendly constructor: inject the lookup directly.
    SecurityContextCurrentNaturalist(Function<NaturalistName, Optional<Naturalist>> byName) {
        this.byName = byName;
    }

    @Override
    public Optional<NaturalistName> name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return Optional.of(p.naturalistName());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Naturalist> naturalist() {
        return name().flatMap(byName);
    }
}
```

> Two constructors: Spring uses the `NaturalistQuery` one (single autowirable constructor is ambiguous with two, so annotate the Spring one). Add `@org.springframework.beans.factory.annotation.Autowired` to the `NaturalistQuery` constructor to disambiguate:

```java
    @org.springframework.beans.factory.annotation.Autowired
    SecurityContextCurrentNaturalist(NaturalistQuery naturalistQuery) {
        this(naturalistQuery::getByName);
    }
```

- [ ] **Step 4: Write the static template view**

Create `CurrentNaturalistView.java`:

```java
package com.naturalist.console.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Template-facing accessor for session identity, called from {@code layout/page.jte}.
 * A sanctioned SecurityContext reader (design: seam discipline). Static because JTE
 * templates cannot inject beans; the read is stateless.
 */
public final class CurrentNaturalistView {

    private CurrentNaturalistView() {
    }

    /** The current naturalist's given name, or {@code null} for admin/anonymous. */
    public static String displayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof NaturalistPrincipal p) {
            return p.givenName();
        }
        return null;
    }

    /** Whether the request is authenticated (naturalist or admin), not anonymous. */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** The CSRF token for the current request, or {@code null} outside a request. */
    public static CsrfToken csrfToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
            return (CsrfToken) sra.getRequest().getAttribute(CsrfToken.class.getName());
        }
        return null;
    }
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `mvn -q -pl apps/management-console test -Dtest=CurrentNaturalistTest`
Expected: PASS.

- [ ] **Step 6: Stage and commit**

```bash
git add apps/management-console/src/main/java/com/naturalist/console/auth apps/management-console/src/test/java/com/naturalist/console/auth
git commit -m "feat(console): add CurrentNaturalist session-identity seam

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Header UI + domain note

Surfaces the logged-in naturalist and a logout control in the shared layout, and documents the credential invariant + seam.

**Files:**
- Modify: `apps/management-console/src/main/jte/layout/page.jte`
- Create: `domains/naturalists/CLAUDE.md`
- Test: `apps/management-console/src/test/java/com/naturalist/console/auth/HeaderWebMvcTest.java`

**Interfaces:**
- Consumes: `CurrentNaturalistView` (static), Spring Security test support.
- Produces: header markup rendering "Logged in as {givenName}" + a logout form (POST `/logout` with CSRF).

- [ ] **Step 1: Write the failing header render test**

Create `HeaderWebMvcTest.java`:

```java
package com.naturalist.console.auth;

import com.naturalist.naturalist.NaturalistName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * Verifies the shared header renders the logged-in naturalist and a logout control.
 */
@SpringBootTest
class HeaderWebMvcTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void home_asNaturalist_showsNameAndLogout() throws Exception {
        var principal = new NaturalistPrincipal(
                NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        var auth = new UsernamePasswordAuthenticationToken(
                principal, "n/a", principal.getAuthorities());

        mockMvc.perform(get("/").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Logged in as Patrick")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout\"")));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q -pl apps/management-console test -Dtest=HeaderWebMvcTest`
Expected: FAIL — the header does not yet render the name or logout form.

- [ ] **Step 3: Edit the layout header**

In `apps/management-console/src/main/jte/layout/page.jte`, add an import block at the top (after the existing `@param` lines) and a user area in the header. Replace the `<div class="container site-header-bar">…</div>` block with:

```jte
@import org.springframework.security.web.csrf.CsrfToken
@import com.naturalist.console.auth.CurrentNaturalistView

<div class="container site-header-bar">
    <a class="site-brand" href="/">The Amateur Naturalist</a>
    <div class="site-search">
        @template.components.searchBox(query = query)
    </div>
    !{var displayName = CurrentNaturalistView.displayName();}
    !{CsrfToken logoutCsrf = CurrentNaturalistView.csrfToken();}
    @if(CurrentNaturalistView.isAuthenticated())
        <div class="site-user">
            @if(displayName != null)
                <span class="site-user-name">Logged in as ${displayName}</span>
            @endif
            <form method="post" action="/logout" class="site-logout">
                @if(logoutCsrf != null)
                    <input type="hidden" name="${logoutCsrf.getParameterName()}" value="${logoutCsrf.getToken()}">
                @endif
                <button type="submit">Logout</button>
            </form>
        </div>
    @endif
</div>
```

> `@import` lines in JTE must appear at the top of the template with the other `@import`/`@param` directives. If the template already has an `@import` section, add these two lines there instead of inline.

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q -pl apps/management-console test -Dtest=HeaderWebMvcTest`
Expected: PASS — the home page rendered as `patrick-way` contains "Logged in as Patrick" and a `/logout` form.

- [ ] **Step 5: Write the domain note**

Create `domains/naturalists/CLAUDE.md`:

```markdown
# Naturalists domain

A `Naturalist` (`NamedEntity<NaturalistName>`) is a person at Oak Vista — an
ecological actor (role, ecological stage), **not** an account. Authentication
credentials live in a **separate** record, `NaturalistCredential`
(`NamedEntity<NaturalistName>`, keyed 1:1 by the same slug), so the ecological
record never carries a password.

- `NaturalistCredential.passwordHash` is an encoded (`{bcrypt}$2a$…`) hash from the
  console's `PasswordEncoder`, never plaintext. Invariant: `entityName(name)` +
  `notBlank(passwordHash)`.
- Read ports: `NaturalistQuery`, `NaturalistCredentialQuery` (both `EntityQuery`).
- Login username = the `NaturalistName` slug. Session identity is exposed to the
  console through the `CurrentNaturalist` seam in `management-console`
  (`com.naturalist.console.auth`); domain code does not depend on Spring Security.

See `docs/plans/2026-07-06-naturalist-auth-design.md`.
```

- [ ] **Step 6: Full module build**

Run: `mvn -q verify` (from repo root)
Expected: PASS — all modules compile and all tests green, including the naturalists repository-test contracts and the console auth/header tests.

- [ ] **Step 7: Stage and commit**

```bash
git add apps/management-console/src/main/jte/layout/page.jte apps/management-console/src/test/java/com/naturalist/console/auth/HeaderWebMvcTest.java domains/naturalists/CLAUDE.md
git commit -m "feat(console): show logged-in naturalist and logout in header

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage** (against `2026-07-06-naturalist-auth-design.md`):
- §3 Credential store → Task 2 (`NaturalistCredential`, JSON, minimal read port, mock + contract). ✔
- §3 Naturalist read stack → Task 1 (`NaturalistQuery`). ✔
- §4 Authentication wiring (composite `UserDetailsService`, admin-first, bcrypt, preserved filter chain, `NATURALIST` authority, admin route still `ADMIN`) → Task 3. ✔
- §5 `CurrentNaturalist` seam + explicit `NaturalistPrincipal` + header "logged in as … / Logout" → Tasks 3 (principal), 4 (seam), 5 (header). ✔
- §6 Testing (credential + naturalist contracts, extended security test: admin route, naturalist login, 403, bad creds) → Tasks 1, 2, 3 (+ seam test Task 4, header test Task 5). ✔
- Key decision 2 (username = slug) → Task 3 `loadUserByUsername`. ✔
- Key decision 7 (explicit principal) → Task 3. ✔ Key decision 6 (seam discipline) → Tasks 3–5, documented in Task 5 CLAUDE.md. ✔
- Out-of-scope (registration, reset, roles beyond admin route, collection feature) → not implemented. ✔

**Placeholder scan:** The only intentional placeholders are the bcrypt hash values in `naturalist-credentials.json`, which Task 3 Step 1 fills with real generated values via a documented, then-deleted printer. No "TBD"/"add validation"/"similar to" left in code steps.

**Type consistency:** `NaturalistCollection`/`NaturalistCredentialCollection` names match between the collections namespace, the query ports, and the impls. `NaturalistRepository.NaturalistEntityRepository` / `.CredentialRepository` names match mocks, contracts, and query impls. `NaturalistPrincipal` accessors `naturalistName()` / `givenName()` match every consumer (`NaturalistUserDetailsService`, `CurrentNaturalistView`, `SecurityContextCurrentNaturalist`, tests). `getByName` (inherited from `EntityQuery`) is used consistently. Passwords `durrell`/`gerald` match between the seed printer and the login test.

**Note on `NaturalistEntityCollections`:** Task 1 creates it with only `NaturalistCollection`; Task 2 Step 2 adds `NaturalistCredentialCollection` to the same file. If executing out of order, ensure `NaturalistCredential` (Task 2 Step 1) exists before that class compiles.
