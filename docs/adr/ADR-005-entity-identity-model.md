# ADR-005: Entity Identity Model — CatalogEntity and FactEntity

**Status:** Accepted (Amended ×2, Amendment 3 Draft)
**Full rationale:** [rationale/ADR-005-entity-identity-model.md](rationale/ADR-005-entity-identity-model.md)

## Decision

- **Every entity has a non-nullable `name()`.** No `@Nullable` default returning null.
- **`Entity<ID, NAME>`** — signature:
  ```java
  public interface Entity<ID extends PersistenceId<?>, NAME extends EntityName<?>> extends Observable {
      ID id();
      NAME name();
      <E extends Entity<ID, NAME>> E withId(ID id);
  }
  ```
- **`CatalogEntity<ID, NAME extends CatalogName>`** — slug identity, stable named
  classification. Examples: `Element`, `Compound`, `InsectSpecies`, `Plant`, `ZoneInfo`.
- **`FactEntity<ID, NAME extends FactName>`** — GUID identity, record of something that
  happened/was observed. Two informal sub-kinds: **Events** (human-initiated:
  `AmendmentEvent`, `IrrigationEvent`, `TillageEvent`) and **Observations** (passive:
  `LabAnalysis`, sensor readings).
- **`FactName`** — abstract `EntityName` wrapping a UUID. Concrete subclass per fact entity
  for compile-time type safety (`AmendmentEventName`, `LabAnalysisName`). Assigned at
  creation time, not at persistence time. Enables offline deduplication via existing name
  unique constraint.
- **Domain laws and constants** are neither catalog nor fact entities — they are
  ValueObjects, static constants, or domain service methods. No RDBMS table, no `name()`.
- **Unified infrastructure:** single `EntityRepository`, `TestEntitySource`,
  `AbstractTestEntityRepository` — no Catalog/Fact bifurcation. Catalog/Fact remain as
  semantic markers only.

### Classification

| Category | Type | Name kind | Persistent |
|---|---|---|---|
| Named stable classification | `CatalogEntity` | slug | Yes |
| Singular occurrence | `FactEntity` | UUID | Yes |
| Domain law / constant | `ValueObject` / static / service | None | No |

## Amendment 2 — `Entity<ID, NAME>` and automatic name uniqueness

- `Entity` gains `NAME` type parameter bounded by `EntityName<?>`. Call sites retrieve
  concrete name types without casts.
- `TestEntitySource<ID, NAME, ENTITY>` enforces name uniqueness automatically in
  `preSaveChecks` before iterating `uniqueConstraints()`. No per-subclass declaration.
- `uniqueConstraints()` default returns `List.of()`. Override only for additional
  constraints beyond canonical name.
- **`@EntityIdentifier` scope narrowed:** only on *secondary* `EntityName` fields. Never on
  canonical `name`.
- **`@UniqueValue`** for secondary unique plain-value fields (`String commonName`, etc.).

## Amendment 3 (Draft) — CatalogName canonical form and name/display separation

- **`CatalogName` enforces `^[a-z0-9]+(-[a-z0-9]+)*$`** (lower-kebab-case) via `isValid()`.
- Each concrete `CatalogName` subclass declares its own `protected abstract int maxLength()`
  — maps directly to `VARCHAR(n)` in RDBMS DDL.
- `FactName` unchanged — UUID format self-validates.
- **`name()` is the machine key; display values are separate fields.** Human-readable
  labels (titles, common names, IUPAC symbols) are explicit distinct fields, unconstrained
  by kebab format.
- `Element`: `name` is slug (`"calcium"`), `symbol` carries IUPAC (`"Ca"`). Former
  `commonName` dropped.
- `ReactionProfile`: gained `String title`; `name` is slug.
- `Compound.commonName`: `@EntityIdentifier CompoundCommonName` → `@UniqueValue String`.
  `CompoundCommonName` class deleted.

## Consequences

- `FactName` added to `kernels/framework`; concrete subclasses in `domains/identifiers/`
- `CatalogEntityRepository`, `FactEntityRepository`, and `AbstractCatalogTestEntityRepository` deleted
- `getByName` / `getByEntityNameSet` available on every repository
- Offline-collected observations deduplicated by name unique constraint — no sync logic
- `Invariants` validates both `entityId` and `entityName` uniformly
- All catalog JSON data uses lower-kebab-case slugs
- `@EntityIdentifier` not currently used on any field; spec remains valid for future secondary fields
