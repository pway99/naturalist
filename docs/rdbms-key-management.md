# RDBMS Key Management

How the RDBMS ACL (`domains/<domain>/<domain>-repository-rdbms`) maps the domain's two
identity branches onto database keys, and how cross-references resolve. Read this before
converting a domain to a real RDBMS adapter. Worked references: `naturalists-repository-rdbms`
(minimal `NamedEntity` + its 1:1 child) and `chemistry-repository-rdbms` (aggregate +
surrogate-UUID entity).

The domain has two identity branches (ADR-022): **`NamedEntity<NAME extends EntityName>`**
(natural-key slug) and **`Entity<ID extends EntityId>`** (surrogate UUIDv7). The database uses
**two kinds of key** to serve them: a natural key and a sequence-based surrogate.

## Sequence-based key — the physical PK and FK target

Every `NamedEntity` table carries a numeric surrogate primary key:

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

- It is the **target of every foreign key**. References survive a natural-key re-slug — a rename
  touches one row, not every referrer.
- It **never surfaces in the domain.** The domain record (`Naturalist`, `Element`, `Compound`, …)
  has no id component; the numeric id is DB-internal.
- The DBO does **not** carry it as a write-path value. On insert it is DB-generated
  (`@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")`); child rows do not
  need it threaded back because they nested-select it from the parent's name (below).

## Natural key — the stable reference and the lookup

Every `NamedEntity` table also carries its slug as a unique column:

```sql
name VARCHAR(n) NOT NULL UNIQUE
```

- The `EntityName` slug is the identity **at the port**: every repository read and write is keyed
  by name (`getByName`, `update`, …), and it is the stable cross-domain reference (ADR-001).
- The DBO carries the name; it round-trips 1:1 with the domain `EntityName`.
- Secondary unique columns (`@UniqueValue`, e.g. `common_name`, `display_name`) are `UNIQUE` too.

## Surrogate UUID key — `Entity<EntityId>` records

Tables for `Entity<ID>` records (e.g. `compound_depiction`) use the UUID **itself** as the PK —
there is no separate BIGINT, because the UUIDv7 *is* the domain identity:

```sql
id UUID PRIMARY KEY
```

- The DBO carries it. MyBatis has no UUID type handler and the ACL forbids adding one, so the DBO
  stores it as a **`String`** and the mapper casts it in SQL: `#{id}::uuid`.
- Reconstruct with `EntityId.of(UUID.fromString(id))`.

## Cross-reference resolution — DBOs speak names, the DB speaks ids

The load-bearing rule. A DBO that references another entity carries that entity's **name**, never a
stored id and never a name column on the referencing table. The numeric `<parent>_id` FK column is
resolved and recovered entirely in SQL:

- **Read** — the mapper JOINs the referenced table and projects its `name`:
  ```sql
  SELECT d.id, c.name AS compound_name, ...
  FROM child d JOIN compound c ON c.id = d.compound_id
  ```
  The name is JOIN-derived from the FK, so it **cannot drift** from it.

- **Write** — the mapper nested-selects the id from the name:
  ```sql
  INSERT INTO child (compound_id, ...)
  SELECT id, ... FROM compound WHERE name = #{compoundName}
  ```
  If the parent is absent the `INSERT … SELECT` writes **zero rows** and no FK/NOT NULL violation
  ever fires — so the adapter checks the affected-row count and throws `EntityNotFoundException`.
  The stored column is always the numeric FK, DB-enforced.

This keeps every reference numeric and durable in storage while the Java stays in the domain's own
vocabulary (names). No sentinel ids: an earlier `-1L` placeholder in the naturalist credential DBO
was removed once this rule was uniform.

### Owned children vs independent references

Both use the same name-carrying + nested-select mechanism:

- **Owned aggregate children** (a compound's roles / constituent elements / properties): inserted in
  the same transaction as the parent; on update they are deleted and re-inserted wholesale.
- **Independent entities and cross-aggregate references** (a depiction → its compound; a product →
  its compounds): identical resolution.

## What the DBO validates (`invariants()`)

- Natural key: `notNull` + `kebabFormat` + `maxLength` matching the `VARCHAR(n)` width.
- Referenced name: `notNull` + `kebabFormat` (it is another entity's slug).
- Numeric id: **not** validated on the write path — it is DB-generated.
- Surrogate UUID: `notBlank` (the text form).

## Metadata + drift-guard

Each DBO declares `@DboSchema(table, primaryKey, unique, foreignKeys, entity)` — pure metadata. The
reusable `DboSchemaValidator` (`kernels/persistence-test`) classpath-discovers every `@DboSchema` and
asserts the **live** schema matches (table, PK, unique, FK). Ship one `*SchemaDriftIT` per domain that
applies `schema/<domain>.sql` into a scratch schema and validates it — pinning DDL, `@DboSchema`, and
the database together so a mistyped column fails the build instead of mis-mapping at runtime.

## Summary

| Domain identity | PK column | Natural key column | Cross-ref FK target | DBO id field |
|---|---|---|---|---|
| `NamedEntity<NAME>` | `id BIGINT IDENTITY` | `name VARCHAR UNIQUE` | `<parent>_id` → `parent(id)` | none (DB-generated) |
| `Entity<ID>` (UUIDv7) | `id UUID` | — | `<parent>_id` → `parent(id)` | `String` + `::uuid` cast |
