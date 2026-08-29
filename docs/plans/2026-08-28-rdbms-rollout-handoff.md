# RDBMS Rollout — Handoff (library next)

Fresh-session instructions to continue converting domains from their mock `-rdms`
intermediate to a real `-rdbms` adapter (Postgres + MyBatis ACL), following the DAG.

## Status

- **Done (local `main`, unpushed):** naturalists, chemistry.
- **Read first:** [`docs/rdbms-key-management.md`](../rdbms-key-management.md) — the key/FK conventions.
- **Copy this template:** `domains/chemistry/chemistry-repository-rdbms/` (4 entities incl. an
  aggregate + a surrogate-UUID entity). Minimal template: `domains/naturalists/naturalists-repository-rdbms/`.
- **Standing DB:** `postgres`/`postgres` @ `localhost:5432/naturalist_test` (already up + seeded).
- **Seeder:** `mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder`
  — add `-Dexec.args="<domain>"` to reseed one domain.
- **Full memory:** the `project_rdbms_persistence_kernel` auto-memory has the complete pattern.

## DAG order (do dependencies first — cross-domain data refs become FKs only once both sides are on RDBMS)

```
naturalists ✓   chemistry ✓
library  — no deps                → READY (chosen next)
plants   — deps chemistry✓, naturalists✓   → READY
insects  — deps library, plants, naturalists✓
garden   — deps plants (+ zone*)   [plant_name is a polymorphic rank ref → stays slug even after plants; zone* is a skeleton]
soil     — deps chemistry✓, garden, weather*, zone*
usage    — deps naturalists✓  (but BLOCKED by its in-flight event-log redesign)
```
`*` = skeleton domains (zone, weather) with no `-rdms`; their refs stay slug columns (no FK) until they mature.

## The repeatable recipe (per entity, from chemistry)

1. **DBO** — package-private in `<domain>-repository-rdbms`, camelCase fields, `@DboSchema(table,
   primaryKey, unique, foreignKeys, entity)`. `from(entity)` runs `Observer.forClass(X).arguments("from",
   i -> i.observable(d,"dbo")).throwWhenInvalid()` inline; `toEntity()`; `invariants()` mirror column widths.
   - `NamedEntity` → `id BIGINT GENERATED ALWAYS AS IDENTITY` PK + `name VARCHAR UNIQUE`.
   - `Entity<UUID>` → `id UUID` PK; DBO field is `String`, mapper casts `#{id}::uuid` (no MyBatis UUID handler).
   - **References carry the referenced NAME** (never a stored id). Within-domain → real numeric FK, JOIN on
     read + nested-select on write. Cross-domain (to an un-converted domain) → plain slug column, NO FK.
   - Value objects flatten to columns; sealed value types persist via a `kind()`/`ofKind(String)` pair
     (exhaustive switch, like `StructuralType`).
2. **`src/main/resources/schema/<domain>.sql`** — hand DDL, DROP+CREATE, numeric-id anchors, real FKs only
   for within-domain refs.
3. **`<Domain>SchemaDriftIT`** — copy `ChemistrySchemaDriftIT`: build the DDL in a scratch schema and
   `DboSchemaValidator.validate(c, SCRATCH, DboSchemaValidator.discover("com.naturalist.<domain>"))`.
4. **Mappers** — `@Mapper`, annotated SQL, camelCase params / snake columns, `useGeneratedKeys` for BIGINT
   ids. Aggregates: ONE batched child-load query per child type keyed on the name set (never per-parent).
5. **`@DomainService` adapters** — `extends AbstractEntityRepository<..>` + `implements <Repo interface>`;
   6 `do*` hooks + any extra methods (validate args with `Observer.forClass(..).arguments("m", i ->
   i.identifier(arg,"name")).throwWhenInvalid()`). Aggregate update = update parent + delete/re-insert
   children. Unique violation → `PrimaryKeyConstraintException`; 0-row `INSERT…SELECT` or update →
   `EntityNotFoundException`.
6. **Seeder** — one seeder class per entity in the entity's OWN package inside `apps/test-db-seeder`
   (split-package → reaches the package-private adapter/mapper, nothing goes public); a `<Domain>Seeding`
   orchestrator in `com.naturalist.seeder` applies the schema + calls them in FK order; wire into
   `TestDbSeeder.main` (arg-targetable).
7. **Contract `*IT`s** — `implements <Entity>EntityRepositoryTest`, `@RegisterExtension static
   RdbmsTestExtension rdbms = RdbmsTestExtension.shared()`, `repository()` returns
   `new <Entity>EntityRepositoryRdbms(rdbms.mapper(<Entity>Mapper.class))`.
8. **Cutover** — delete `<domain>-repository-rdms` (dir + its `<module>` in `domains/<domain>/pom.xml` +
   its root-pom `dependencyManagement` entry); app pom dep `-rdms` → `-rdbms`. `@MapperScan` is already
   `"com.naturalist"`, so new mappers are picked up automatically.
9. **Verify** — reinstall, run the seeder, `mvn -pl <domain>-repository-rdbms verify` (drift + contracts),
   then `mvn -pl apps/management-console test`. Commit chemistry-style; keep naturalist-touching changes in
   their own commit.

## Gotchas already learned

- `RandomValue.string()` returns a **36-char UUID** — size string columns ≥ ~36, or match the contract's
  `newEntity()` value. `RandomValue.string(n)` truncates.
- A contract's `newEntity()`/`modifiedEntity()` that references another entity must reference a **seeded**
  one, or the real FK fails (chemistry's product → `thymol`). Fix the shared contract (mock is unaffected)
  or override in the IT.
- `mapUnderscoreToCamelCase=true` is set in `MyBatisSupport` AND both `management-console` `application.yml`
  files — DBO fields are camelCase.
- `LocalDate`/`Instant` use MyBatis's built-in handlers (fine); only domain types are unwrapped.
- `getByEntityNameSet` contract is now order-insensitive (fixed in framework-test) — no ordering worries.

## Library specifics (the next task)

Four persisted entities (four `-rdms` adapters to replace): **Concept**, **GlossaryTerm**, **Citation**
(from `kernels/authority`), **CitationAssociation**.

- **Concept** — `NamedEntity<ConceptName>`: `name`, `title`, `Description` VO → 4 columns
  (`description_preschool/elementary/secondary/university`, all NOT NULL).
- **GlossaryTerm** — `NamedEntity<GlossaryTermName>`: `name`, `term`, `definition`, `example?`.
- **Citation** — sealed `NamedEntity<CitationName>` (`com.naturalist.authority`), one permit `OnlineSource`
  today: `name`, `kind` (discriminator), `title`, `author?`, `year? Integer`, `lastModified? Instant`, and
  `AuthorityReference` VO → `authority_source` (enum) + `authority_url` (URI → text). Reconstruct via a
  `kind` switch (only `ONLINE_SOURCE` today).
- **CitationAssociation** — `Entity<CitationAssociationId>` (UUID): `citationName` → **within-library FK**
  `citation_association.citation_id → citation(id)` (nested-select); `subject` is a cross-domain `EntityRef`
  stored flat as `subject_domain` / `subject_rank` / `subject_name` (NO FK); `note?`. Extra repo methods:
  `getByCitationName`, `getBySubject`, `getBySubjects(Set)` (batch — one query).

**OPEN DESIGN DECISION — resolve before building `CitationAssociation` (the other three are pure mechanical
chemistry-pattern):** reconstructing the **typed** `EntityRef` from `(domain, rank, name)` needs cross-domain
knowledge — the mock's `CitationAssociationJson` (in `library-repository-test`) imports `InsectRankName` +
`InsectsDomain`. A `*-repository-rdbms` module **may not** import another domain (DAG: own api + persistence
+ framework only), so the adapter cannot do this directly. Options:
- **A — inject an `EntityRefResolver` port** (`(domain, rank, name) → EntityRef`) supplied at the composition
  root (the one place allowed to know every domain). Cleanest for the DAG; a new seam.
- **B — store & return a generic/untyped `EntityRef`** built from the slugs. Simplest, but the ref may not
  `.equals()` the catalog's typed refs, which can break catalog resolution of associations.
- **C — reuse a catalog-kernel resolution path** if one exists (`kernels/catalog` `EntityRef`/`DomainId`).
  **Investigate C first**, then most likely land on **A**.

Start the fresh session by reading `docs/rdbms-key-management.md` and the chemistry module, resolving the
`EntityRef` decision above, then applying the recipe to library's four entities.
