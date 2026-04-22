# ADR-021: PersistenceId is Adapter-Internal; Cross-Entity References Use EntityName

**Status:** Draft — piloted in the `insects` domain

## Context

During the `InsectAggregate` assembly work, the domain model forced a choice that
exposed a structural flaw. `InsectAggregateQueryImpl` needed to resolve a species,
then look up its images. Two paths were available on `InsectQuery.ImageQuery`:

- `forSpeciesName(InsectSpeciesName)` — resolve by slug
- `forSpeciesId(InsectSpeciesId)` — resolve by numeric persistence key

The `forSpeciesId` path existed because `InsectImage` carried an
`@Nullable InsectSpeciesId insectSpeciesId` component — the parent species's
persistence key denormalized onto the child. The field's own Javadoc admitted
the design pressure directly: *"the ID is a denormalized convenience for the
RDBMS join column."*

The field could never be populated from the JSON catalog. JSON fixtures do not
carry `PersistenceId` values (ADR-007 — "ask what it is, not where it lives";
identity assignment is a storage concern, not a domain concern). The numeric
id was assigned by `TestEntitySource.nextNumericId()` at insert time, which
meant the parent species received an id *only after* its `TestEntitySource`
ran. The child `InsectImage` records were inserted with `insectSpeciesId = null`
and no mechanism existed to backfill them. `InsectAggregateQueryImpl`'s
attempt to use `species.id()` produced a null parameter that never matched any
image row.

The proposed local fixes — teach `TestEntitySource` to resolve parent names to
ids at insert time, introduce a foreign-key-constraint contract, allow JSON to
declare `PersistenceId` values — all violated ADR-007. They addressed the
symptom ("`insectSpeciesId` is null at aggregate assembly time") without
questioning the structural claim underneath ("an `InsectImage` domain record
must carry its parent's numeric storage key").

Running the ADR-007 diagnostic exposed the claim as false. The `insectSpeciesName`
slug is the authoritative, stable, reload-surviving reference to the parent
species. The numeric id is meaningful only *inside* a single adapter's storage
after a specific insertion. Two adapters (in-memory `TestEntitySource`, future
RDBMS) would assign different ids for the same entity. The id is adapter state;
the slug is domain state.

A secondary structural concern surfaced alongside the first. The
`domains/identifiers` module is a shared bag of `PersistenceId<Long>` subclasses
reachable by every domain. Nothing prevents `chemistry-api` from importing
`InsectSpeciesId` and writing a cross-domain FK-by-id. A shared identifier
module turns PersistenceId — which is conceptually intra-domain, analogous to
a microservice's private primary key — into a promiscuously visible vocabulary.

## Decision

### Rule 1 — PersistenceId is adapter-internal

`PersistenceId<Long>` is a storage-layer concept. It exists to enable a specific
adapter to address a specific row. It does not belong in domain references
between entities.

- A domain record **MUST NOT** carry another entity's `PersistenceId` as a
  component. Cross-entity references use `EntityName` only.
- A domain record **MAY** carry its own `PersistenceId` on its `id()` component
  — that is how `Entity<ID, NAME>` is defined, and the kernel is not changing
  in this ADR.
- A repository or query method **MUST NOT** accept or return another domain's
  `PersistenceId` across a sub-context or domain boundary. Boundary-crossing
  references are always by `EntityName`.
- The RDBMS adapter for a domain **MAY** — and for performance under load,
  **SHOULD** — use a numeric surrogate key for its own foreign-key columns.
  That key lives entirely inside the adapter. It is resolved from
  `EntityName` at insert time (via a catalog-name-to-id lookup, cached) and
  joined on internally for read queries. The domain record it materializes
  carries only the `EntityName` reference.

### Rule 2 — Cross-entity references use EntityName

Every reference from one entity to another — whether intra-domain (parent→child,
sibling→sibling) or cross-domain — is expressed as an `EntityName` component on
the referring record. This applies uniformly:

- Catalog entities carry the parent's `CatalogName` slug.
- Fact entities carry the referenced entity's `FactName` (GUID) or `CatalogName`
  (slug) depending on what is referenced.
- No "denormalized convenience" id columns appear on the domain record.

### Rule 3 — Aggregate assembly goes by name

An aggregate factory composes its aggregate by resolving the root by name, then
fetching child collections by the same name (or by an explicitly-named query
method that takes an `EntityName`). The factory never threads `PersistenceId`
values between repositories.

```java
// correct
Optional<InsectSpecies> species = speciesQuery.getByName(name);
ImageCollection images = imageQuery.forSpeciesName(species.get().name());

// incorrect — threads storage key across a query boundary
ImageCollection images = imageQuery.forSpeciesId(species.get().id());
```

### Scope — pilot on the `insects` domain

This ADR is adopted immediately for the `insects` domain. The pilot is the
*broad-reading* application of the rules: `PersistenceId` is removed from
`insects-api` and `insects-core` entirely. `InsectSpecies` and `InsectImage`
cease to carry an `id()` component at the domain layer. The numeric primary
key continues to exist in the future RDBMS schema (Appendix A.1) but is
invisible across the port.

The pilot requires a kernel shape that permits an entity to be identified by
`EntityName` alone without breaking the existing `Entity<ID, NAME>` contract
used by every other domain. The implementation PR introduces the minimum
kernel addition needed — the working shape is a parallel `NamedEntity<NAME>`
interface with matching `NamedEntityRepository`, `NamedTestEntitySource`, and
`NamedEntityQuery` scaffolding — such that chemistry, plants, soil, apiary,
and every other existing domain remain byte-compatible and source-compatible
through the pilot.

The broader roll-out to other domains is deferred to a separate ADR written
after the insects pilot lands and is observed in practice. That ADR will
evaluate:

- Whether the pilot's kernel shape replaces `Entity<ID, NAME>` in the kernel
  outright or the two coexist permanently.
- Whether relocating each `<Domain>XxxId` from `domains/identifiers/` into
  its owning `<domain>-api` is worth the module-graph churn — or whether,
  with broad adoption of the pilot shape, the `PersistenceId` subclasses for
  migrated domains can be deleted entirely.

Those questions are out of scope here. This ADR establishes the rule and
pilots its full application on one domain; a later ADR decides how far to
propagate it.

### RDBMS adapter work is gated on api completeness

An RDBMS is durable infrastructure. Once a schema is in production, every
subsequent change — column rename, FK restructuring, table split — requires
orchestration: migration scripts, read/write compatibility windows, rollback
planning, coordination with anything reading the data out-of-band. That cost
is structural; it does not go away with better tooling.

The consequence for this ADR is a hard ordering rule:

- No `<domain>-repository-rdms` module is written, and no schema is committed,
  until the owning domain's api model is complete and approved for production
  testing.
- "Complete and approved" means the api surface (entities, queries,
  repositories, collections) has stabilized under exercise by real application
  code and is the shape the domain intends to carry into production. It is
  not merely "compiles" or "tests pass on the in-memory adapter."
- Until that gate, the in-memory `TestEntitySource` adapter is the only
  implementation. Schema shape, SQL mapper choice, and adapter scaffolding
  are all deferred.

The gate exists because schema decisions inherit the shape of the api they
serve. An api that is still moving produces a schema that will move with it,
and RDBMS schema motion is the expensive motion. Delaying the adapter until
the api is stable trades a recoverable delay (writing the adapter later) for
an unrecoverable cost (migrating production data through an unstable model).

### What this ADR does not change

- `Entity<ID, NAME>` remains the kernel entity interface for every non-pilot
  domain. Chemistry, plants, soil, apiary, and the rest continue to use it
  unchanged. Their `id()` and `withId(ID)` methods are untouched.
- `TestEntitySource` continues to assign numeric ids via `nextNumericId()` at
  insert time for entities that implement `Entity<ID, NAME>`. JSON catalog
  fixtures for those entities continue to omit ids.
- `EntityRepository.getById(ID)` remains on the repository contract for every
  non-pilot domain. Within a domain, the repository adapter is free to use
  its own id internally. What changes is that `getById` is never called with
  an id sourced from another entity's domain reference, because no such
  reference exists.
- The RDBMS schema for every domain — including insects — is free to carry
  numeric foreign-key columns. Those columns are invisible to the port.

## Consequences

- The `insects` domain aggregate assembly bug resolves by deletion, not
  addition. `InsectImage.insectSpeciesId` goes away, `withInsectSpeciesId`
  goes away, `ImageQuery.forSpeciesId` and the corresponding repository method
  go away, and `InsectAggregateQueryImpl` composes by name via the factory.
- `InsectSpecies` and `InsectImage` cease to carry `id()` components at the
  domain layer. They implement `NamedEntity<NAME>` (or the kernel's final
  chosen shape) which requires only an `EntityName`-based identity.
- The kernel gains a parallel entity interface and supporting scaffolding
  alongside `Entity<ID, NAME>`. Insects migrates; every other domain is
  untouched. The addition is opt-in and leaves the existing kernel surface
  intact.
- Domain records become smaller and carry no nullable persistence-artifact
  components. `@Nullable` on an id-shaped field is a review flag.
- The `insects-repository-rdms` adapter, when written, carries the numeric
  primary key privately in its schema and resolves slugs to ids inline in
  SQL (Appendix A). The port exposes no numeric ids.
- Under production load, RDBMS joins still use numeric foreign keys — the
  performance argument for numeric FKs stands, and is served by the
  adapter's internal schema, not by leaking numeric keys into the domain.
- Cross-domain and cross-sub-context references are uniformly slugs. Reviewers
  can flag any `*Id` import across a sub-context boundary as an ADR-021
  violation without further investigation.
- The broader question of whether `PersistenceId` belongs on `Entity` for
  every domain remains open. The insects pilot provides the evidence a
  follow-on ADR needs to decide.

## Applicability Signals

Flag an ADR-021 violation in review when any of the following appears:

- A domain record has a component of type `<OtherEntity>Id`.
- A method on a query, repository, or factory accepts or returns a
  `PersistenceId` subtype belonging to a different entity.
- A JSON catalog fixture declares an id field with a non-null value for a
  foreign-key reference (ids on an entity's own `id` field remain null).
- An aggregate factory threads `id()` between two repository calls.
- A new `forXxxId(...)` method is proposed on a query interface where a
  `forXxxName(...)` method already exists or would be equally serviceable.

## Related ADRs

- ADR-001 — Repository Architecture
- ADR-004 — Modular Monolith (sub-context boundaries via package-private visibility)
- ADR-005 — Entity Identity Model (CatalogEntity, FactEntity, FactName)
- ADR-007 — First Principles Problem Solving (the diagnostic discipline that produced this ADR)
- ADR-010 — Query Design Contract
- ADR-017 — Observability, Monitoring, and Validation

## Reference Implementation

The insects domain after the pilot PRs:

- `InsectSpecies` and `InsectImage` implement the new kernel `NamedEntity<NAME>`
  shape. Neither record carries an `id()` component.
- `InsectSpeciesId` and `InsectImageId` are deleted from
  `domains/identifiers/com/naturalist/insects/`.
- `InsectImage` carries `InsectSpeciesName` only.
- `InsectQuery.ImageQuery` exposes `forSpeciesName(InsectSpeciesName)` only.
- `InsectAggregateQueryImpl` delegates to `InsectAggregateFactory`, which
  composes by name.
- `insects.json` and `insect-images.json` fixtures omit all id fields.
- The kernel adds `NamedEntity<NAME>`, `NamedEntityRepository<NAME, ENTITY>`,
  `NamedTestEntitySource<NAME, ENTITY>`, and `NamedEntityQuery<NAME, ENTITY, COLLECTION>`.
  Only the insects domain consumes them in the pilot.

## Appendix A — Adapter Translation Sketch

This appendix specifies the shape of the future `insects-repository-rdms`
adapter so that ADR-021 can be evaluated independently of whether that adapter
has been written. No RDBMS code is being added to the build graph by this ADR;
this is prose and pseudocode only, sufficient to confirm the translation
mechanism is conventional and implementable.

### A.1 — Schema

Two tables. Numeric primary keys internal to the adapter. Domain-level
references (`insect_species_name` on `insect_images`) are absent from the
schema — the RDBMS foreign key is carried on a numeric column that the domain
never sees.

```sql
CREATE TABLE insect_species (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,
    common_name      VARCHAR(256) NOT NULL,
    -- ... remaining domain fields ...
    CONSTRAINT insect_species_name_uq UNIQUE (name)
);

CREATE INDEX insect_species_name_ix ON insect_species (name);

CREATE TABLE insect_images (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(128) NOT NULL,
    insect_species_id   BIGINT NOT NULL
                        REFERENCES insect_species(id)
                        ON DELETE RESTRICT,
    date_added          TIMESTAMP WITH TIME ZONE NOT NULL,
    resource_name       VARCHAR(256) NOT NULL,
    CONSTRAINT insect_images_name_uq UNIQUE (name)
);

CREATE INDEX insect_images_species_id_ix ON insect_images (insect_species_id);
```

Three observations:

1. `insect_images` has **no** `insect_species_name` column. The slug exists in
   the domain record; the schema stores only the numeric FK. Materialization
   on read reconstitutes the slug by joining to `insect_species.name`.
2. The FK join uses `insect_species_id` — a numeric column with a B-tree
   index — exactly the production performance profile that motivated the
   original concern.
3. `insect_species.name` carries a unique index, so name-to-id resolution is
   O(log n) even without a cache.

### A.2 — Name-to-id resolution: SQL-native

Name-to-id translation happens in the SQL, not in the adapter's Java.
Postgres's planner sees the `UNIQUE (name)` on `insect_species`, resolves the
slug via its B-tree index, and threads the numeric id into the insert or join
in a single round-trip statement. No Java cache is required in the default
adapter shape.

Two equivalent forms are serviceable. The CTE form reads cleanest and is the
recommended default:

```sql
-- MyBatis mapper fragment
WITH species AS (
    SELECT id FROM insect_species WHERE name = #{speciesName}
)
INSERT INTO insect_images (name, insect_species_id, date_added, resource_name)
SELECT #{name}, species.id, #{dateAdded}, #{resourceName}
FROM species
RETURNING id;
```

A scalar subquery is equally valid and slightly tighter when the referenced
entity is a single row:

```sql
INSERT INTO insect_images (name, insect_species_id, date_added, resource_name)
VALUES (
    #{name},
    (SELECT id FROM insect_species WHERE name = #{speciesName}),
    #{dateAdded},
    #{resourceName}
)
RETURNING id;
```

Three properties of the SQL-native form worth naming:

- **Transactional snapshot consistency.** The subquery sees the same MVCC
  snapshot as the containing statement. There is no window in which the
  resolved id could be stale relative to the insert.
- **One round trip, always.** The statement resolves and inserts atomically.
  There is no cold-path/warm-path branching.
- **Zero adapter state.** No cache, no warm-up, no invalidation logic, no
  memory budget. The adapter holds only its `SqlSession`.

`EXPLAIN ANALYZE` on the CTE form confirms two index scans — the unique-index
scan on `insect_species.name` and the FK-index use on the subsequent insert.
No sequential scans at any point.

### A.3 — Insert of a fact entity referencing a catalog entity

The MyBatis mapper carries the SQL; the adapter is thin:

```java
public void insert(InsectImage image) {
    observer().arguments("insert", i -> i.entity(image, "image")).throwWhenInvalid();
    int rowsInserted = sqlSession.insert(
            "InsectImageMapper.insert",
            InsectImageParameters.from(image));
    if (rowsInserted == 0) {
        throw new EntityNotFoundException(image.insectSpeciesName());
    }
}
```

Mapper:

```xml
<insert id="insert" parameterType="InsectImageParameters">
    WITH species AS (
        SELECT id FROM insect_species WHERE name = #{speciesName}
    )
    INSERT INTO insect_images (name, insect_species_id, date_added, resource_name)
    SELECT #{name}, species.id, #{dateAdded}, #{resourceName}
    FROM species
</insert>
```

If the species slug does not exist, the CTE returns zero rows, the
`INSERT ... SELECT` inserts zero rows, and the adapter detects the zero-row
result and throws `EntityNotFoundException`. Referential integrity
enforcement is a natural consequence of the statement shape — the adapter
neither checks nor translates a failed FK constraint because there is no
constraint violation to translate.

### A.4 — Read by foreign key

Joining on the slug directly keeps the domain vocabulary end-to-end. The
planner uses the unique index on `insect_species.name` and the FK index on
`insect_images.insect_species_id`:

```xml
<select id="getBySpeciesName" parameterType="string" resultMap="InsectImageResult">
    SELECT i.id,
           i.name,
           s.name AS species_name,
           i.date_added,
           i.resource_name
    FROM insect_images i
    JOIN insect_species s ON s.id = i.insect_species_id
    WHERE s.name = #{speciesName}
</select>
```

The result mapping materializes `insectSpeciesName` from `species_name`. The
numeric `insect_species_id` is never exposed to the result-set projection.

### A.5 — Aggregate assembly over the adapter

`InsectAggregateFactory` composes via two queries, both slug-typed at the
port. Each adapter resolves its own SQL independently; no numeric keys flow
between the two repositories:

```
speciesQuery.getByName("potato-leafhopper")
   ↓
   SpeciesRepositoryRdms.getByName(name)
   ↓  SELECT ... FROM insect_species WHERE name = 'potato-leafhopper'
   ↓  returns InsectSpecies (no id component on the domain record)
imageQuery.forSpeciesName(species.name())
   ↓
   ImageRepositoryRdms.getBySpeciesName(name)
   ↓  SELECT ... FROM insect_images i
   ↓    JOIN insect_species s ON s.id = i.insect_species_id
   ↓    WHERE s.name = 'potato-leafhopper'
   ↓  returns List<InsectImage> each carrying insectSpeciesName = "potato-leafhopper"
InsectAggregate.of(species, imageCollection)
```

The slug `"potato-leafhopper"` is the only reference threading the pipeline.
Postgres resolves it to numeric ids internally, twice, independently, in the
two statements — and never surfaces those ids across the port.

### A.6 — What's conventional and what's load-bearing

The pieces above are standard:

- `BIGSERIAL` primary key with numeric FKs on references — textbook Postgres schema.
- Unique index on the slug column — standard for natural-key lookup.
- `WITH` CTEs and scalar subqueries for correlated resolution — idiomatic Postgres.
- `INSERT ... SELECT` and `RETURNING` — standard Postgres idioms.
- Single join on indexed numeric FK — the reason RDBMSes exist.

The load-bearing decisions specific to ADR-021:

- The result-set slug materialization in A.4 (`s.name AS species_name`) is
  what keeps the port's return type free of numeric FKs. Without it the
  domain record could not populate `insectSpeciesName` without a second query.
- The SQL-native resolution in A.2 is what keeps the adapter stateless. The
  adapter owns no translation table, no warm-up logic, no invalidation code.

Both choices are easy. Neither requires an adapter to exist today to evaluate.

### A.7 — PersistenceId may never appear in Java at all

MyBatis is the preferred mapper framework for this project, and the appendix
is written with that choice load-bearing rather than incidental. The reason
is not a performance claim — it is that author-written SQL is the right
primitive for durable infrastructure, and that auto-generated SQL solves the
wrong problem.

Schema is not a derivative of a Java class. The Java class describes a domain
concept; the schema describes a persistence contract that, once in production,
is expensive to change (see *RDBMS adapter work is gated on api completeness*
above). When those two are coupled by a code generator, every motion in the
Java class proposes a motion in the schema, and the generator's convenience
argues for accepting it. That pressure is exactly backwards: the schema
should be the slower-moving, deliberately-governed artifact, and the api
model should stabilize *first* precisely so the schema that serves it can
stabilize *too*. MyBatis puts the SQL in the repository author's hands,
where governance lives. Generated SQL — Hibernate's DDL export, JPA metamodel
derivation, equivalent tooling — moves that governance into a tool that is
optimizing for a different problem (mapping Java classes to tables) than the
one we have (maintaining a durable schema under a stable api).

With author-written SQL in hand, a stronger possibility than this ADR's rule
strictly requires opens up: the Java entity and persistence model need have
no concept of, or awareness for, `PersistenceId` whatsoever.

Under that interpretation:

- The numeric primary key is a database-only concern. It exists in `BIGSERIAL`
  columns and in FK constraints, but the Java model never names it.
- The only place the numeric id can leak into source code is inside SQL
  statements defined in XML mappers or annotations — where it is a storage
  artifact in a storage artifact, not a domain concept crossing a boundary.
- The api model is governed by `EntityName` alone. `NamedEntity<NAME>` is not
  a pilot concession; it is the natural shape.
- `TestEntitySource` is a reflection of the api model, not the persistence
  model. It carries no `nextNumericId()` surface because no Java code
  consumes one.
- If `PersistenceId` ever needs to appear in Java — for auditing, for a
  specific administrative tool, for a migration utility — it appears as a
  DTO transformed to and from the `Entity` at the RDBMS adapter boundary,
  never as a component on the domain record.

JPA/Hibernate cannot accommodate this shape — its entity metamodel requires
an `@Id` annotated field on the Java class. MyBatis, jOOQ, and raw JDBC can.
The choice of mapper framework therefore constrains whether the stronger form
of ADR-021 is available; the insects pilot's `NamedEntity<NAME>` kernel shape
is the form compatible with that stronger stance, which is why MyBatis is
the project's chosen framework and this appendix's worked example.

### A.8 — What this appendix does not prove

It does not prove:

- That writing the adapter against a real Postgres instance will surface no
  surprises. Connection management, transaction boundaries, error translation —
  these are adapter-implementation concerns, not ADR-021 premises.
- That every future query shape in the insects domain will admit a clean
  single-statement translation. Some may require a two-step sequence; those
  cases are adapter-local decisions that do not affect the port.
- That the rule in Decision §1 depends on MyBatis. It does not. The rule —
  no cross-entity `PersistenceId` at the port — is framework-agnostic and
  holds under JDBC, jOOQ, and JPA/Hibernate native queries equally. What
  *does* depend on MyBatis is the stronger stance of A.7 (no `PersistenceId`
  in Java at all) and the governance posture that author-written SQL
  provides over durable schema. Those are the reasons MyBatis is the
  project's chosen framework, not a claim that the rule itself requires it.

These are implementation decisions for the adapter PR, not premises of this
ADR.
