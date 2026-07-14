# naturalists-domain — Chat Briefing

**Purpose.** Domain vocabulary plus current shape of the naturalists module
(three sub-contexts: naturalist, credential, safety/PPE), sized for a chat
Claude session. Pair with `docs/briefings/framework-core.md` (framework /
structural glue).

**Primary rule.** Names, packages, components, and visibility below are
observed from the source tree at briefing time (2026-07-13), not
extrapolated. If you need a type not listed here, ask before inventing one.

---

## 1. Module Scope and DAG

```
naturalists-api  →  framework, identifiers
```

No cross-domain api dependencies. No taxonomy, habitat, clades, catalog, or
authority dependencies. Other domains reference naturalists by
`NaturalistName` slug only — never by importing naturalists-api.

---

## 2. Package Map

```
com.naturalist.naturalist/
  Naturalist                         — NamedEntity<NaturalistName>
  NaturalistCredential               — NamedEntity<NaturalistName> (1:1 keyed)
  NaturalistRole                     — enum (5 ecological roles)
  EcologicalStage                    — enum (4 Durrell progression stages)

  NaturalistQuery                    — public (N=1 collapse, EntityQuery)
  NaturalistCredentialQuery          — public (N=1 collapse, EntityQuery)
  NaturalistRepository               — package-private namespace class
                                       (NaturalistEntityRepository,
                                        CredentialRepository)
  NaturalistEntityCollections        — public namespace interface
                                       (NaturalistCollection,
                                        NaturalistCredentialCollection)

  safety/
    ProtectiveEquipment              — NamedEntity<ProtectiveEquipmentName>
```

### Identifier locations (in the `identifiers` module, not in naturalists-api)

| Type                      | Package                            | Max length |
|---------------------------|------------------------------------|------------|
| `NaturalistName`          | `com.naturalist.naturalist`        | 64         |
| `ProtectiveEquipmentName` | `com.naturalist.naturalist.safety` | 64         |

Both are `EntityName` subclasses with `@JsonCreator static of(String value)`
factory. The *module* is `identifiers`; the *package* mirrors the home domain.

---

## 3. Entity Summary

| Type                   | Identity                  | Branch                    | DDD role                      |
|------------------------|---------------------------|---------------------------|-------------------------------|
| `Naturalist`           | `NaturalistName`          | `NamedEntity` (slug)      | Ecological actor at Oak Vista |
| `NaturalistCredential` | `NaturalistName`          | `NamedEntity` (slug, 1:1) | Authentication credential     |
| `ProtectiveEquipment`  | `ProtectiveEquipmentName` | `NamedEntity` (slug)      | PPE catalog entry             |

### Sub-context independence

`Naturalist` + `NaturalistCredential` form the naturalist sub-context.
`ProtectiveEquipment` is an independent safety sub-context in its own
sub-package. The credential is keyed 1:1 by `NaturalistName` — same slug,
separate record. Ecological data never carries a password.

---

## 4. Naturalist — ecological actor

```java
public record Naturalist(
    NaturalistName name,
    String givenName,
    @Nullable String familyName,
    NaturalistRole role,
    EcologicalStage stage,
    @Nullable String notes
) implements NamedEntity<NaturalistName>
```

A person who participates in the life of Oak Vista. The name is inspired
by Gerald Durrell. Identity by slug (`NaturalistName`, max 64 chars).

Invariants: `entityName(name)`, `notNull(givenName)`, `notNull(role)`,
`notNull(stage)`.

### Behavior methods

| Method                | Returns   | Logic                                                            |
|-----------------------|-----------|------------------------------------------------------------------|
| `isTeacher()`         | `boolean` | `role == TEACHER`                                                |
| `isKeeper()`          | `boolean` | `role == KEEPER`                                                 |
| `isApiaryCompetent()` | `boolean` | `isKeeper() && (stage == PRACTITIONER \|\| stage == NATURALIST)` |
| `displayName()`       | `String`  | `givenName + " " + familyName` or `givenName` alone              |

`isApiaryCompetent()` gates apiary management decisions — a keeper at
WONDER or CURIOUS stage should not be attributed with independent chemical
treatment decisions.

### Required vs nullable fields

- `name`, `givenName`, `role`, `stage` — always populated.
- `familyName`, `notes` — nullable.

No `with*` methods — the record is structurally simple enough that the
canonical constructor suffices.

---

## 5. NaturalistCredential — authentication record

```java
public record NaturalistCredential(
    NaturalistName name,
    String passwordHash
) implements NamedEntity<NaturalistName>
```

Keyed 1:1 by the same `NaturalistName` slug as the `Naturalist` record.
`passwordHash` is always bcrypt-encoded (`{bcrypt}$2a$...`), never
plaintext. The encoding is performed by the console's `PasswordEncoder`.

Invariants: `entityName(name)`, `notBlank(passwordHash)`.

The deliberate separation from `Naturalist` ensures that ecological
records never carry passwords and that the credential record can be
managed (rotated, locked) without touching the ecological data.

---

## 6. ProtectiveEquipment — PPE catalog (safety sub-package)

```java
public record ProtectiveEquipment(
    ProtectiveEquipmentName name
) implements NamedEntity<ProtectiveEquipmentName>
```

Personal protective equipment worn by a naturalist when handling hazardous
compounds or performing management tasks. Belongs to the naturalist domain
because PPE is about the person wearing it, not the compound being handled.

Invariants: `entityName(name)`.

Compounds reference PPE requirements indirectly via
`SafetyProfile.requiresProtectiveEquipment` (a boolean flag on the
chemistry domain); the specific equipment catalog lives here.

Examples from Oak Vista: `chemical-resistant-gloves`, `eye-protection`,
`n95-respirator-for-vaporization-method`, `respirator-recommended`,
`gloves`.

---

## 7. NaturalistRole — five ecological roles

```
VISITOR     — read-only observer, no management responsibilities
CARETAKER   — garden tender; amendment, irrigation, management attribution
STUDENT     — formal learner; lesson plans, stage progression tracking
KEEPER      — apiary manager (requires PRACTITIONER+ stage for attribution)
TEACHER     — leads observation and interpretation; implicitly caretaker or keeper
```

A single person may hold multiple roles over time — a student who becomes
a caretaker is represented by a role change on the entity, not a new
entity.

---

## 8. EcologicalStage — Durrell progression

```
WONDER       — sensory, observational     (maps to preschool Description level)
CURIOUS      — simple ecological relations (maps to elementary Description level)
PRACTITIONER — mechanisms, field ID        (maps to secondary Description level)
NATURALIST   — taxonomy, systems thinking  (maps to university Description level)
```

Stage captures where a person sits in the Durrell learning progression.
The application layer uses it to select the appropriate `Description`
level when surfacing catalog knowledge. Stage is not age-gated —
advancement is through direct observation and guided learning.

Stage is not a value judgement: the wonder of WONDER is irreplaceable
and not superseded by NATURALIST. A naturalist at the NATURALIST stage
can also sit on a log with a child and share genuine wonder at a ground
beetle.

---

## 9. Query / Repository / Collection Surface

### `NaturalistQuery` (public)

```java
public interface NaturalistQuery
    extends EntityQuery<NaturalistName, Naturalist, NaturalistCollection>
```

No additional methods beyond the `EntityQuery` port. Identity at the port
is the `NaturalistName` slug.

### `NaturalistCredentialQuery` (public)

```java
public interface NaturalistCredentialQuery
    extends EntityQuery<NaturalistName, NaturalistCredential,
    NaturalistCredentialCollection>
```

No additional methods. The console's `UserDetailsService` is its primary
consumer — `getByName` resolves a credential by the naturalist's slug.

### `NaturalistRepository` (package-private namespace class)

```java
class NaturalistRepository {
    protected interface NaturalistEntityRepository
        extends EntityRepository<NaturalistName, Naturalist> {
    }

    protected interface CredentialRepository
        extends EntityRepository<NaturalistName, NaturalistCredential> {
    }
}
```

Non-instantiable (private constructor). Two nested repository contracts —
standard namespace pattern (ADR-020). No domain-specific repository
methods on either interface.

### BehavioralCollections

`NaturalistCollection`, `NaturalistCredentialCollection` — both
`final class extends BehavioralCollection<...>`, package-private
constructor, public `of(Collection<...>)` / `empty()` factories. No
domain-specific filtering methods.

---

## 10. Core Implementations (naturalists-core)

```
NaturalistQueryImpl          — @DomainService, extends AbstractEntityQuery
NaturalistCredentialQueryImpl — @DomainService, extends AbstractEntityQuery
```

Both are thin observe-dispatch-delegate adapters. Package-private. Each
validates arguments via `observer().arguments(...)`, then delegates to
its repository.

---

## 11. JSON Catalog Locations (naturalists-repository-test)

```
naturalists-repository-test/src/main/resources/naturalists/
  naturalists.json                — 4 Naturalist records
  naturalist-credentials.json     — 4 NaturalistCredential records (1:1 keyed)
  safety/protectiveEquipment.json — 5 ProtectiveEquipment records
```

Catalog conventions:

- `"name": "<slug>"` is the `EntityName` natural key.
- `NaturalistCredential` entries carry `"passwordHash"` as a
  bcrypt-encoded string (never plaintext, even in test fixtures).
- Enum values serialize by constant name (`"CARETAKER"`,
  `"PRACTITIONER"`).

### Test personas

| Slug            | Given / Family | Role      | Stage        | Note                                |
|-----------------|----------------|-----------|--------------|-------------------------------------|
| `patrick-way`   | Patrick Way    | CARETAKER | NATURALIST   | Principal architect, soil programs  |
| `delia-durrell` | Delia Durrell  | TEACHER   | PRACTITIONER | Leads ecology visits                |
| `flora-mendez`  | Flora Mendez   | STUDENT   | CURIOUS      | Secondary-school pollinator work    |
| `amir-hassan`   | Amir Hassan    | KEEPER    | PRACTITIONER | Hive inspections, Varroa monitoring |

---

## 12. Cross-domain References

| Reference                 | Direction               | Type                                      |
|---------------------------|-------------------------|-------------------------------------------|
| `NaturalistName`          | insects → naturalists   | `EntityName` slug                         |
| `ProtectiveEquipmentName` | chemistry → naturalists | `EntityName` slug (indirect boolean gate) |

The naturalists domain has no outbound api dependencies on other domains.
Other domains reference naturalists by `NaturalistName` slug only — the
insects domain's `FieldObservation.observedBy` is the primary consumer.

---

## 13. Console Integration

Login username = `NaturalistName` slug. Session identity is exposed to
the console through the `CurrentNaturalist` seam in `management-console`
(`com.naturalist.console.auth`); domain code does **not** depend on
Spring Security. The request-attribute convention
(`naturalist.currentNaturalistName`) is the only bridge — the attribute
key is duplicated as a same-literal constant on both sides rather than
shared through a type.

See `docs/plans/2026-07-06-naturalist-auth-design.md`.

---

## 14. Current State — What's Built, What's Not

**Built and stable.**

- `Naturalist` entity with `NaturalistQuery` and `NaturalistCollection`.
- `NaturalistCredential` entity with `NaturalistCredentialQuery` and
  `NaturalistCredentialCollection`.
- `ProtectiveEquipment` entity (safety sub-package) with
  `ProtectiveEquipmentTestEntitySource`.
- Both naturalist/credential repository mocks, behavioral contract
  tests, and test entity sources.
- Console login and session management via `CurrentNaturalist` seam.
- All query implementations in naturalists-core.

**Not yet built.**

- No `NaturalistCommand` — no write surface beyond console login.
- No `ProtectiveEquipmentQuery` or `ProtectiveEquipmentRepository` —
  PPE data loaded by test entity source but no query port yet.
- No domain-specific repository methods on either repository interface.
- No `ProtectiveEquipmentCollection` — no collection type yet.

---

## 15. Anti-patterns Specific to naturalists-api

- **Do not put passwords or auth tokens on the `Naturalist` record.**
  Credentials live on `NaturalistCredential`. The separation is
  deliberate and non-negotiable.
- **Do not import Spring Security types in naturalists-api or
  naturalists-core.** Auth integration is console-only, via the
  request-attribute convention.
- **Do not create an "account", "user", or "profile" entity.** A
  `Naturalist` is an ecological actor, not an account. The domain
  models people in relation to the ecosystem, not to a software system.
- **Do not invent a `NaturalistId` (surrogate).** Identity is
  `NaturalistName` (slug, `NamedEntity`). No `id()`, no `withId`.
- **Do not collapse `NaturalistCredential` into `Naturalist`.** The
  separation ensures ecological data never carries passwords.
- **Do not reference `ProtectiveEquipment` from compounds by entity
  FK.** Compounds carry a boolean flag
  (`SafetyProfile.requiresProtectiveEquipment`); the specific PPE
  catalog is person-domain.
- **Do not put `ProtectiveEquipment` in `com.naturalist.naturalist`
  directly.** It lives in `com.naturalist.naturalist.safety`
  (sub-package).
- **Do not add domain-specific repository methods without a concrete
  consumer.** Both repository interfaces currently extend
  `EntityRepository` with inherited methods only — that is correct
  while the only consumer is the console's `UserDetailsService`.
