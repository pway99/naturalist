# RDBMS Rollout — Handoff (library next)

Fresh-session instructions to continue converting domains from their mock `-rdms`
intermediate to a real `-rdbms` adapter (Postgres + MyBatis ACL), following the DAG.

## Status

- **Done (local `main`, unpushed):** naturalists, chemistry, library, plants, insects, garden, **soil**.
- **soil — COMPLETE 2026-08-29 (committed to local `main`, unpushed):** all 6 persisted entities
  (`SoilProfileInfo` + the 5 `observation/` types) converted in `domains/soil/soil-repository-rdbms`;
  `soil-repository-rdms` **deleted**. 156 rdbms ITs (drift + 6 contracts) + 82 app tests + gate green.
  - **Soft-ref decision (deviation from the within-domain-FK default):** soil's `TestEntitySource`s declare
    NO `foreignKeyConstraints()` and the contracts insert observations against random/non-existent parent ids,
    so `soil_profile_name` (slug) and every `lab_analysis_id` (UUID) are **plain columns, NO FK** — matching
    the domain's soft-reference model and avoiding contract churn. Composite logical keys
    (`(nutrient_name/input_name, lab_analysis_id)`, and `lab_analysis_id` unique on physical) are DDL-only
    UNIQUEs (not in `@DboSchema.unique`).
  - **Two sealed VOs flattened adapter-side** (neither has kind()/ofKind): `OptimumRange` (4 permits) →
    `range_shape` + nullable `range_min`/`range_max`; `RecommendedAmount` (3 permits) → `amount_kind` +
    nullable `amount_value`. A `Quantity(0)` (amount_value 0) round-trips distinctly from `None`
    (amount_value null) via the discriminator — the domain explicitly requires this.
  - **Measurements** (SoilPH, CEC, EC, SAR, …) unwrap to a single `BigDecimal` and store as **unconstrained
    NUMERIC** so scale round-trips exactly for `.equals()` comparison. Mixed key styles: `SoilProfileInfo` is
    BIGINT-id NamedEntity; the 5 observation entities are UUID `Entity`. Built by one subagent + hand schema.
- **garden — COMPLETE 2026-08-29 (committed to local `main`, unpushed):** its one persisted entity
  (`Planting`, `Entity<PlantingId>`) converted in `domains/garden/garden-repository-rdbms`;
  `garden-repository-rdms` **deleted**. 33 rdbms ITs + 82 app tests + gate green. Simplest domain: a single
  flat table, **zero FKs** — `plantName` is a nullable polymorphic `PlantRankName` (stays `(plant_rank,
  plant_name)` slug even though plants is converted; matched on BOTH columns so ranks don't collide),
  `zone_name`/`sub_zone_name` are zone-skeleton slugs, `cultivar_name` a cross-domain plants slug. `LocalDate`
  → `DATE` (MyBatis built-in handler). `PlantedZone` is a read model (not persisted).
- **insects — COMPLETE 2026-08-29 (committed to local `main`, unpushed):** all 10 persisted entities
  converted in `domains/insects/insects-repository-rdbms`; `insects-repository-rdms` **deleted**. 238 rdbms
  ITs (drift + 11 contracts) + 82 app tests + full architecture gate green. The reference domain, the
  largest. Rank chain / evidence stack / feature stack mirror plants (with `placed_in` on EVERY rank and
  96-char cross-rank slugs for subspecies). Two hard entities:
  - **`InsectSpecies`** — an aggregate with 7 nullable owned VOs (`ChemicalDefense`, `Voltinism`,
    `HabitatProfile`, `HabitatRequirements`, `GardenConnections`, `BeneficialProfile`,
    `EcologicalSignificance`) flattened to a column null-group + 5 child tables. Each VO reconstructs iff a
    presence signal holds (a required leaf non-null, or "any field non-null" / "a child row exists" for the
    all-nullable VOs). Only `battus-philenor` carries non-null VOs in seed; every other species is all-null
    and must round-trip as absent — so the presence checks are load-bearing.
  - **`InsectLifeStage`** — a sealed 4-permit `NamedEntity` (egg/larva/pupa/adult) persisted single-table by
    a `stage_kind` discriminator, with 6 child tables (phenology windows, habitat zones/layers, larva host
    plants + parasitoid hosts, adult nectar sources). `PupaStage.DiapauseRegulation` (a stateful sealed VO)
    flattens to `diapause_kind` + two generic permit fields via an adapter-side switch (no api change, unlike
    plants' PhytochemicalRole).
  - **`InsectFunctionalRole`** is a UUID entity here (a value type in chemistry) — `parent_name` UNIQUE +
    guild child table.
  - **Shared-contract fix:** `InsectSpeciesRepositoryTest`'s `newEntity`/`ghost`/`modifiedEntity` used an
    unseeded placeholder genus `carabus`; the real genus FK bit (mock had none). Changed to the seeded
    `empoasca` (chemistry-precedent fix; mock unaffected, each IT rolls back so no cross-test interference).
  - **`;`-in-comment trap (again):** the seeder/drift split DDL on `;`, so two inline `-- ...;...` comments in
    `insect_life_stage` broke the CREATE. Never put a semicolon inside a `.sql` comment.
  - All 10 entities HAVE a contract test (unlike plants' PlantEcologicalRole gap).
- **plants — COMPLETE 2026-08-29 (committed to local `main`, unpushed):** all 13 persisted entities
  converted in `domains/plants/plants-repository-rdbms`; `plants-repository-rdms` **deleted**. 280 rdbms
  ITs (drift + 12 contracts) + 82 app tests + the full architecture gate green. Notable design points:
  - **Rank chain** (order→family→genus→species) = real numeric upward FKs (nested-select on write, JOIN
    on read). `commonNames`/`nativeBioregions` are child tables (batched assembly, no N+1).
  - **Evidence stack** (`OrganismObservation`/`OrganismImage` kernel generics) persisted for the first
    time — no prior template. Polymorphic `PlantRankName` subject/parent stored as a `(rank, slug)` pair
    and rebuilt IN-MODULE via `PlantRankName.of(slug, LinealRank)` (no resolver — plants-api owns it,
    unlike library's cross-domain CitationAssociation). Nullable `Identification` flattens to a column
    group + a `plant_observation_candidate` child table.
  - **Cross-rank refs** (ecological role, program, phytochemistry `plantName`) → `(plant_rank, plant_name)`
    slug pair, NO FK. **Cross-domain/cross-kernel refs** (phyto `compound_name`, observation `observed_by`,
    species `native_bioregion`, order `placed_in`) → plain slug columns, NO FK (each domain's schema.sql
    builds in an isolated scratch schema, so a cross-schema FK would break its drift IT).
  - **`PhytochemicalRole`** gained a `kind()`/`ofKind(String)` pair in plants-api (mirroring chemistry's
    `StructuralType`) to persist its sealed permits as a child-table discriminator.
  - **Kernel tweak:** `@DboSchema.entity()` bound relaxed `Class<? extends Named<?>>` → `Class<? extends
    Named>` (raw) so kernel-generic entities (OrganismObservation/Image/FeatureAssignment) can name their
    raw class literal — a parameterized class literal is illegal in Java. `entity()` is inert doc the
    validator ignores; all existing concrete usages stay valid. **Insects will need the same for its own
    evidence stack.**
  - **Gap (pre-existing, carried forward):** `PlantEcologicalRole` has NO `EntityRepositoryTest` contract
    in the domain (only a mock + a catalog-data test), so its rdbms adapter ships without a contract IT —
    seeding smoke-tests its insert + role child table. Author a contract later if wanted.
- **library — COMPLETE 2026-08-28 (committed to local `main`, unpushed):** all four entities
  (`Concept`, `GlossaryTerm`, `Citation`, `CitationAssociation`) converted in
  `domains/library/library-repository-rdbms`; `library-repository-rdms` **deleted**. 90 rdbms ITs
  (drift + 4 contracts) + 82 app tests + the full architecture gate all green.
  - **EntityRef resolver — landed as Option A.** Port `EntityRefResolver` (`rankOf(EntityRef)` +
    `resolve(domain,rank,name)`) in **`library-api`**; production impl
    `InsectsEntityRefResolver` (`@DomainService`, **public**) in **`insects-core`** (which already deps
    `library-api`), so the app auto-wires it — zero app config. Both directions need domain knowledge
    (the rank discriminator lives in the concrete `EntityName` subtype), so the port carries both.
    The seeder and the contract IT construct `new InsectsEntityRefResolver()` directly (seeder deps
    `insects-core`; the rdbms module deps it **test-scope only**). When a second subject domain
    appears, grow it into a composite behind the port (mirroring `EntityRefLinker`).
  - **CitationAssociation storage:** `id UUID` PK; `citation_id BIGINT` within-library FK
    (nested-select on write, JOIN on read); `subject_domain/subject_rank/subject_name` flat, **no FK**
    (target is cross-domain); `note` nullable. `getBySubjects` batches via a Postgres row-value `IN`
    over `CitationSubjectKey` (one query, N+1-safe). No composite unique — the UUID PK covers the
    contract's duplicate-insert test and keeps the drift IT fully authoritative (`DboSchemaValidator`
    only validates single-column uniques).
  - **`Citation` note:** the sealed `authority.Citation` (permit `OnlineSource`) persists via a `kind`
    discriminator + exhaustive switch; `Instant lastModified` → `TIMESTAMPTZ` (instant-preserving
    across the separate seeder/test JVMs).
- **Read first:** [`docs/rdbms-key-management.md`](../rdbms-key-management.md) — the key/FK conventions.
- **Copy this template:** `domains/chemistry/chemistry-repository-rdbms/` (4 entities incl. an
  aggregate + a surrogate-UUID entity). Minimal template: `domains/naturalists/naturalists-repository-rdbms/`.
- **Standing DB:** `postgres`/`postgres` @ `localhost:5432/naturalist_test` (already up + seeded).
- **Seeder:** `mvn -q -pl apps/test-db-seeder exec:java -Dexec.mainClass=com.naturalist.seeder.TestDbSeeder`
  — add `-Dexec.args="<domain>"` to reseed one domain.
- **Full memory:** the `project_rdbms_persistence_kernel` auto-memory has the complete pattern.

## DAG order (do dependencies first — cross-domain data refs become FKs only once both sides are on RDBMS)

```
naturalists ✓   chemistry ✓   library ✓   plants ✓   insects ✓   garden ✓   soil ✓
usage    — deps naturalists✓   BUT BLOCKED by its in-flight event-log redesign (confirm with user before converting)
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
