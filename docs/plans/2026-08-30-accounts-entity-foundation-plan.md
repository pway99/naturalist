# Accounts Entity Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the new `accounts` domain module with its identity foundation — the `AccountName` identifier, the `Account` `NamedEntity` record, and its two enums — so later PRs can add the repository, query, command, and RDBMS adapter on top.

**Architecture:** `accounts` is a new domain following the standard module split. This PR delivers only the "entity + identifiers" layer (ADR-019 PR #1 of the domain): the opaque `AccountName` key in the shared `identifiers` module, plus the `Account` record and its `AccessLevel`/`AccountStatus` enums in a new `accounts-api` module. No repository, query, command, or RDBMS adapter yet — those are separate PRs. Student/classroom fields are **postponed** (see the design doc) and are deliberately absent; `email` is the `@UniqueValue` login for this adult-only shape.

**Tech Stack:** Java 21 records, Maven multi-module, `com.naturalist.framework` identity kernel (`NamedEntity`, `EntityName`, `Constraints`), Jackson 2.19 (native record deserialization), JUnit 5.

**Spec:** [`docs/plans/2026-08-30-accounts-auth-redesign-design.md`](2026-08-30-accounts-auth-redesign-design.md) — §Domain model, §Decisions D3/D5.

## Global Constraints

- **Typed identifiers only.** `Account` is a `NamedEntity<AccountName>`; never a raw `String`/`UUID` key. (`AccountName` is a cross-domain key, so it lives in `identifiers`.)
- **Records for entities.** `Account` is a Java record. No Lombok. Accessor names match component names exactly (`name()`, not `getName()`).
- **`api` module dependencies are frozen:** `accounts-api` depends only on `framework` and `identifiers` (+ `framework-test` at test scope). Nothing else.
- **Every mutable field on a `NamedEntity` needs an explicit `with*` method.** `name()` is immutable.
- **New-module checklist (root `CLAUDE.md`):** create poms, add the module to the `domains` aggregator, **and add a `<dependency>` entry to the root `<dependencyManagement>` with `<version>${project.version}</version>`** — omitting the last breaks the reactor.
- **Completeness gate (run before declaring done):** `mvn install -DskipTests && mvn rewrite:dryRun -Drewrite.failOnDryRunResults=true`, in addition to `mvn verify`.
- **Package:** `com.naturalist.account` (singular, mirroring `naturalists` → `com.naturalist.naturalist`).

---

## File Structure

- `domains/accounts/pom.xml` — new domain aggregator (packaging `pom`), parent `domains`.
- `domains/accounts/accounts-api/pom.xml` — the api module, parent `accounts`.
- `domains/identifiers/src/main/java/com/naturalist/account/AccountName.java` — the opaque key (in the existing `identifiers` module).
- `domains/identifiers/src/test/java/com/naturalist/account/AccountNameTest.java` — validity test.
- `domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccessLevel.java` — enum.
- `domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccountStatus.java` — enum.
- `domains/accounts/accounts-api/src/main/java/com/naturalist/account/Account.java` — the record.
- `domains/accounts/accounts-api/src/test/java/com/naturalist/account/AccountTest.java` — construction / `with*` / equality test.
- `domains/pom.xml` — add `<module>accounts</module>`.
- `pom.xml` (root) — add the `accounts-api` `<dependencyManagement>` entry.

---

### Task 1: Scaffold the `accounts` domain module + `accounts-api`

**Files:**
- Create: `domains/accounts/pom.xml`
- Create: `domains/accounts/accounts-api/pom.xml`
- Modify: `domains/pom.xml` (module list)
- Modify: `pom.xml` (root `<dependencyManagement>`)

**Interfaces:**
- Produces: the Maven artifact `com.naturalist:accounts-api` on the reactor, resolvable by later modules.

- [ ] **Step 1: Create the domain aggregator pom** — `domains/accounts/pom.xml`, mirroring `domains/naturalists/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>domains</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>accounts</artifactId>
    <packaging>pom</packaging>
    <name>domains :: accounts</name>

    <modules>
        <module>accounts-api</module>
    </modules>
</project>
```

- [ ] **Step 2: Create the api pom** — `domains/accounts/accounts-api/pom.xml`, mirroring `domains/naturalists/naturalists-api/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.naturalist</groupId>
        <artifactId>accounts</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>accounts-api</artifactId>

    <dependencies>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>framework</artifactId>
        </dependency>
        <dependency>
            <groupId>com.naturalist</groupId>
            <artifactId>identifiers</artifactId>
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

- [ ] **Step 3: Register the module** in `domains/pom.xml` — add `<module>accounts</module>` to the `<modules>` list (place it next to `naturalists`):

```xml
        <module>naturalists</module>
        <module>accounts</module>
        <module>usage</module>
```

- [ ] **Step 4: Add the root dependency-management entry** in `pom.xml`, immediately after the `<!-- NATURALISTS -->` block's entries (create an `<!-- ACCOUNTS -->` comment):

```xml
            <!-- ACCOUNTS -->
            <dependency>
                <groupId>com.naturalist</groupId>
                <artifactId>accounts-api</artifactId>
                <version>${project.version}</version>
            </dependency>
```

- [ ] **Step 5: Verify the reactor resolves the empty module**

Run: `mvn -q -pl domains/accounts/accounts-api -am install -DskipTests`
Expected: BUILD SUCCESS (module builds empty — no sources yet).

- [ ] **Step 6: Commit**

```bash
git add domains/accounts/pom.xml domains/accounts/accounts-api/pom.xml domains/pom.xml pom.xml
git commit -m "build(accounts): scaffold accounts domain + accounts-api module"
```

---

### Task 2: Add the `AccountName` identifier

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/account/AccountName.java`
- Test: `domains/identifiers/src/test/java/com/naturalist/account/AccountNameTest.java`

**Interfaces:**
- Produces: `com.naturalist.account.AccountName` — `final class ... extends EntityName`, factory `AccountName.of(String)`, `maxLength() == 64`. Used as `Account`'s key and (later) referenced by `naturalists`.

- [ ] **Step 1: Write the failing test** — `AccountNameTest.java`:

```java
package com.naturalist.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNameTest {

    @Test
    void opaqueSlugIsValid() {
        AccountName name = AccountName.of("acct-018f3a2e9b71");
        assertThat(name.isValid()).isTrue();
        assertThat(name.value()).isEqualTo("acct-018f3a2e9b71");
    }

    @Test
    void nonKebabIsNotValid() {
        assertThat(AccountName.of("Bad Name").isNotValid()).isTrue();
    }

    @Test
    void overMaxLengthIsNotValid() {
        String tooLong = "a".repeat(65);
        assertThat(AccountName.of(tooLong).isNotValid()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl domains/identifiers test -Dtest=AccountNameTest`
Expected: FAIL — `AccountName` does not exist (compilation error).

- [ ] **Step 3: Write the identifier** — `AccountName.java`, mirroring `com/naturalist/naturalist/NaturalistName.java`:

```java
package com.naturalist.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * Strongly typed, opaque natural key for {@code Account} entities.
 * <p>
 * The slug is a stable, non-self-identifying handle for an authentication
 * account (e.g. {@code AccountName.of("acct-018f3a2e9b71")}). It is minted by the
 * accounts domain and is the auth identity referenced across domain boundaries;
 * it carries no personal information and is never a login credential.
 */
public final class AccountName extends EntityName {

    private AccountName(String value) {
        super(value);
    }

    @JsonCreator
    public static AccountName of(String value) {
        return new AccountName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl domains/identifiers test -Dtest=AccountNameTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/identifiers/src/main/java/com/naturalist/account/AccountName.java domains/identifiers/src/test/java/com/naturalist/account/AccountNameTest.java
git commit -m "feat(identifiers): add opaque AccountName key for the accounts domain"
```

---

### Task 3: Add the `AccessLevel` and `AccountStatus` enums

**Files:**
- Create: `domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccessLevel.java`
- Create: `domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccountStatus.java`

**Interfaces:**
- Produces: `AccessLevel { BROWSE_ONLY, VISION }` and `AccountStatus { ACTIVE, SUSPENDED }` — consumed by `Account` (Task 4) and, later, by the login→authority mapping in `management-console`.

- [ ] **Step 1: Write `AccessLevel`:**

```java
package com.naturalist.account;

/**
 * The vision-identification entitlement of an {@link Account}.
 *
 * <p>{@code BROWSE_ONLY} is the state of a freshly registered account: it may sign
 * in and browse, but the {@code VISION} authority is withheld. {@code VISION} is
 * granted by email verification (self-serve policy) or by an admin (request policy);
 * that grant flips this field. See the accounts auth redesign design doc.
 */
public enum AccessLevel {
    BROWSE_ONLY,
    VISION
}
```

- [ ] **Step 2: Write `AccountStatus`:**

```java
package com.naturalist.account;

/**
 * Lifecycle state of an {@link Account}. {@code SUSPENDED} maps to Spring Security's
 * {@code accountNonLocked = false} at the console boundary — the abuse ban switch.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn -q -pl domains/accounts/accounts-api -am install -DskipTests`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccessLevel.java domains/accounts/accounts-api/src/main/java/com/naturalist/account/AccountStatus.java
git commit -m "feat(accounts): add AccessLevel and AccountStatus enums"
```

---

### Task 4: Add the `Account` record

**Files:**
- Create: `domains/accounts/accounts-api/src/main/java/com/naturalist/account/Account.java`
- Test: `domains/accounts/accounts-api/src/test/java/com/naturalist/account/AccountTest.java`

**Interfaces:**
- Consumes: `AccountName` (Task 2), `AccessLevel` + `AccountStatus` (Task 3).
- Produces: `Account` — `record Account(AccountName name, String email, String passwordHash, boolean emailVerified, AccessLevel access, AccountStatus status) implements NamedEntity<AccountName>`, with `withEmailVerified(boolean)`, `withAccess(AccessLevel)`, `withPasswordHash(String)`, `withStatus(AccountStatus)`. This is the adult-only shape; the `loginName`/nullable-`email`/student fields are added in the postponed classroom PR.

> **Note on `email` uniqueness:** `email` is the login and must be unique, but the `@UniqueValue` annotation + `uniqueConstraints()` override are consumed by the `TestEntitySource` and are added in the next PR (TestEntitySource + JSON), mirroring an existing `@UniqueValue` entity such as `Compound`. This PR keeps `Account` to the record shape + invariants, matching how `naturalists`' entity/repository PRs were split.

- [ ] **Step 1: Write the failing test** — `AccountTest.java`:

```java
package com.naturalist.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private static Account sample() {
        return new Account(
                AccountName.of("acct-018f3a2e9b71"),
                "delia@example.org",
                "{bcrypt}$2a$10$abcdefghijklmnopqrstuv",
                false,
                AccessLevel.BROWSE_ONLY,
                AccountStatus.ACTIVE);
    }

    @Test
    void exposesComponentsByValue() {
        Account account = sample();
        assertThat(account.name()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
        assertThat(account.email()).isEqualTo("delia@example.org");
        assertThat(account.emailVerified()).isFalse();
        assertThat(account.access()).isEqualTo(AccessLevel.BROWSE_ONLY);
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void withEmailVerifiedAndVisionLeavesKeyUnchanged() {
        Account verified = sample().withEmailVerified(true).withAccess(AccessLevel.VISION);
        assertThat(verified.name()).isEqualTo(AccountName.of("acct-018f3a2e9b71"));
        assertThat(verified.emailVerified()).isTrue();
        assertThat(verified.access()).isEqualTo(AccessLevel.VISION);
        // original is unchanged (records are immutable copies)
        assertThat(sample().emailVerified()).isFalse();
    }

    @Test
    void withStatusSuspends() {
        assertThat(sample().withStatus(AccountStatus.SUSPENDED).status())
                .isEqualTo(AccountStatus.SUSPENDED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl domains/accounts/accounts-api test -Dtest=AccountTest`
Expected: FAIL — `Account` does not exist (compilation error).

- [ ] **Step 3: Write the record** — `Account.java`. The `invariants()` shape mirrors `NaturalistCredential`/`Naturalist` (`entityName`, `notBlank`, `notNull` on `Constraints`):

```java
package com.naturalist.account;

import com.naturalist.account.AccountName;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Authentication account — the pure-auth identity, keyed by the opaque
 * {@link AccountName}. Login is by {@code email} (decoupled from any public
 * naturalist handle); {@code passwordHash} is a bcrypt-encoded hash, never
 * plaintext. {@code access} is the vision entitlement; {@code status} is the
 * lifecycle/ban state. The accounts domain knows nothing about naturalists — the
 * {@code NaturalistName ↔ AccountName} link is maintained by the naturalists domain.
 */
public record Account(
        AccountName name,
        String email,
        String passwordHash,
        boolean emailVerified,
        AccessLevel access,
        AccountStatus status
) implements NamedEntity<AccountName> {

    public Account withEmailVerified(boolean emailVerified) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withAccess(AccessLevel access) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withPasswordHash(String passwordHash) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    public Account withStatus(AccountStatus status) {
        return new Account(name, email, passwordHash, emailVerified, access, status);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notBlank(email, "email")
                .notBlank(passwordHash, "passwordHash")
                .notNull(access, "access")
                .notNull(status, "status");
    }
}
```

> If `mvn` reports that a `Constraints` method used here (`entityName` / `notBlank` / `notNull`) has a different name, open `domains/naturalists/naturalists-api/src/main/java/com/naturalist/naturalist/Naturalist.java` and `NaturalistCredential.java` and copy the exact method names they call — those two files are the confirmed reference for this `Constraints` API.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl domains/accounts/accounts-api test -Dtest=AccountTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/accounts/accounts-api/src/main/java/com/naturalist/account/Account.java domains/accounts/accounts-api/src/test/java/com/naturalist/account/AccountTest.java
git commit -m "feat(accounts): add Account NamedEntity record"
```

---

### Task 5: Full-build gate

- [ ] **Step 1: Run the full verify**

Run: `mvn -q verify`
Expected: BUILD SUCCESS across the reactor (the new module participates; nothing else changed).

- [ ] **Step 2: Run the architectural-enforcement gate**

Run: `mvn -q install -DskipTests && mvn -q rewrite:dryRun -Drewrite.failOnDryRunResults=true`
Expected: BUILD SUCCESS with no pending rewrite results.

- [ ] **Step 3: (No commit)** — gate only; nothing to add.

---

## Self-Review

**Spec coverage (this PR's slice — §Domain model `accounts`, identity layer only):**
- `AccountName` opaque key → Task 2. ✅
- `Account` `NamedEntity<AccountName>` with `email`/`passwordHash`/`emailVerified`/`access`/`status` (adult shape) → Task 4. ✅
- `AccessLevel { BROWSE_ONLY, VISION }`, `AccountStatus { ACTIVE, SUSPENDED }` → Task 3. ✅
- Module scaffolding + reactor wiring → Task 1. ✅
- **Deliberately deferred (documented, not gaps):** `EmailVerificationToken`, ports (`AccountQuery`/`AccountCommand`), repository/mock/contract, `TestEntitySource` + JSON, RDBMS adapter, `@UniqueValue email` + `uniqueConstraints()`, and all student/classroom fields. These are subsequent PRs / postponed per the design doc.

**Placeholder scan:** No TBD/TODO; every code step carries full source. The one forward-reference (`@UniqueValue` in the next PR) is explicitly called out, not left implicit.

**Type consistency:** `AccountName.of(String)` used identically in Tasks 2 and 4; `AccessLevel`/`AccountStatus` values (`BROWSE_ONLY`/`VISION`, `ACTIVE`/`SUSPENDED`) match between Tasks 3 and 4; `Account`'s six components and four `with*` methods match between the record (Task 4 Step 3) and its test (Task 4 Step 1).

---

## Next PRs (not in this plan)

Each is its own plan when reached, following the accounts design doc's slice 2:
1. `TestEntitySource` + JSON catalog for `Account` (adds `@UniqueValue email` + `uniqueConstraints()`) — use `/test-entity-source Account in accounts module`.
2. `AccountRepository` + mock + behavioral contract — use `/entity-repository Account`.
3. `AccountQuery` (`byEmail`, `byName`) — use `/entity-query Account`.
4. `AccountCommand` (`register`, `verifyEmail`, `grantVision`, `revokeVision`, `beginPasswordReset`, `resetPassword`, `suspend`/`reinstate`) — custom.
5. `EmailVerificationToken` entity + its stack.
6. `accounts-repository-rdbms` adapter (DBO, mapper, seed).

Then slices 3–6 (naturalists link + credential deletion, security-chain inversion, registration + email, reset + admin grant UI).
