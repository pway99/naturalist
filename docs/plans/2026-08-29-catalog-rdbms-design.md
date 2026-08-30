# catalog-rdbms — a Postgres-backed Catalog adapter for type-ahead search

Status: **design, awaiting review**
Date: 2026-08-29
Supersedes/relates: [`catalog-kernel.md`](catalog-kernel.md) (the forward/inverse SPI and
the in-memory adapter this parallels)

## 1. Context and drivers

The catalog kernel (`kernels/catalog`) defines a cross-domain navigation surface — forward
`search`/`findBySlug` and inverse `findReferencesTo`/`domainsReferencing` — with one adapter
today, the in-memory `InMemoryCatalog` in `kernels/catalog-inmem`. That adapter builds a
`Map<String, Set<EntityRef>>` token index from every domain's `CatalogContribution` **once at
assembly** and is immutable thereafter. The kernel's own docs already anticipate "a Lucene-backed
or persistent adapter … as its own sibling module."

Four drivers motivate a persistent adapter now:

1. **Volume.** The dataset is tiny today but grows substantially in production; an in-heap index
   re-derived from all domains at boot does not ride along indefinitely.
2. **Runtime mutation (sharpest).** The in-memory index is immutable post-assembly, so an entity
   added at runtime — e.g. a user adds a taxonomic rank — is **not searchable until re-assembly**.
   Managing that staleness is exactly the pain to remove.
3. **Type-ahead.** As the user types a word, present the matching `EntityName`s live.
4. **Data already carries the tokens.** Every entity's searchable surface forms (slug, scientific
   name, common names) already exist as columns in the domain's own Postgres tables, and
   `@DboSchema(entity = …)` ties each row-mapped object to its `EntityName` type.

**Postgres suffices; Lucene is not needed at this scale.** Postgres offers built-in full-text
search (`tsvector`, no plugin), the `pg_trgm` contrib extension (fuzzy/substring, one-line
`CREATE EXTENSION`), and plain indexed `LIKE`/`=`. The catalog's matching, however, is *not*
natural-language document search — it is exact/prefix token matching with distinct `MatchKind`s
and deterministic ordering. FTS would change those semantics (stemming, relevance ranking, no
slug/token distinction). So this design keeps the catalog's token model and adds `pg_trgm` only
for the new fuzzy tier.

## 2. Decision summary

- **Approach: query-through the domains' own tables via plain (non-materialized) SQL views** —
  no separate materialized index, so freshness is automatic (driver #2). A plain view is a stored
  query; it is always live.
- **Two-level "view of views" (Option 1).** Each participating rdbms domain owns a
  `<domain>_catalog_token` view over *its own* tables; the app owns one `catalog_search_token`
  view that unions the per-domain views by name. The app never references a domain's base tables —
  it references stable per-domain-view contracts. This is the DB mirror of the Java
  `CatalogContribution` model (each domain contributes its own projection; the app only fans in).
- **Each row carries a stable `entity_type` discriminator** (`'insect-species'`, `'compound'`, …).
  Because the view is per-table, it knows the exact `EntityName` subtype of every row — knowledge a
  bare `(domain, slug)` pair loses (a single domain owns many name types across ranks). The
  discriminator lets the adapter rebuild the correctly *typed* `EntityRef`, which then drives the
  existing `EntityRefLinker` to the right page with no kernel-side reflection into domain classes.
- **Fuzzy tier = `MatchKind.FUZZY`, ranked last**, backed by `pg_trgm`. The enum's javadoc already
  reserves this ("append it at the end … e.g. a `FUZZY` once a fuzzy-capable adapter lands").
- **The inverse direction (`findReferencesTo`, `domainsReferencing`) is storage-independent** —
  live "routing, not graph" against `EntityReferences` providers, identical to in-memory. It is
  extracted into a shared kernel collaborator and reused, not reimplemented.

## 3. Scope and non-goals

**In scope (this effort):**
- New module `kernels/catalog-rdbms` with `RdbmsCatalog implements Catalog` + its MyBatis mapper.
- The per-domain `<domain>_catalog_token` views, added to each participating domain's
  `schema/<domain>.sql`, plus `pg_trgm` GIN indexes on the searched base columns.
- The app-level `catalog_search_token` fan-in view + `CREATE EXTENSION pg_trgm`, and the seeder
  ordering to create them (extension first, domains, fan-in view last).
- Extraction of the inverse-direction routing into `kernels/catalog` (shared by both adapters).
- Integration tests against the standing Postgres, mirroring the `*IT` pattern.

**Explicit non-goals (separate follow-on slices, named not built):**
- **Console type-ahead endpoint + UI.** The catalog is *consumed nowhere today* — there is no
  `CatalogAssembly` call site and no search route. Delivering an end-user type-ahead box is a
  downstream console slice that sits on top of this adapter. This spec stops at a wired,
  IT-tested `Catalog` the console can call.
- **Materialized view / off-heap perf tuning.** If a plain view's `UNION` fan-out ever measures
  too slow, the clean upgrade is a `MATERIALIZED VIEW` with its own GIN index (refreshable, no
  bespoke sync). Not built until a measured need appears.
- **FTS / relevance ranking / stemming.** Out — would change the token-match contract.
- **Non-rdbms domains.** Only the 8 domains with a `-repository-rdbms` module can participate;
  a domain contributes a token view only where searchable names are wanted.

## 4. Architecture

### 4.1 Module `kernels/catalog-rdbms`

Sibling of `catalog-inmem`. Depends on `catalog`, `framework`, `identifiers`, and `persistence`
(for `MyBatisSupport`/`RdbmsExceptions`); `persistence-test` in test scope for the IT harness.
Ships:

- `RdbmsCatalog` (package-private `final class implements Catalog`) — forward direction queries
  the `catalog_search_token` view through a mapper; inverse direction delegates to the shared
  router (§4.4).
- `CatalogSearchMapper` (`@Mapper`) — the SQL.
- `RdbmsCatalogAssembly` (public factory) — the composition-root entry point, mirroring
  `CatalogAssembly.from(...)`: takes the mapper (or `SqlSessionFactory`), the registered
  `DomainId`s, the `entity_type` → `Function<String, EntityName>` reconstructor registry (§4.4),
  the inverse `EntityReferences` providers, and a `Resilience`. Forward `CatalogContribution`s are
  **not** passed — the DB view is the forward source of truth. Slug-uniqueness/registration
  validation mirrors `CatalogAssembly` (a `domain`/`entity_type` present in the view but absent
  from the registry is a wiring error surfaced at startup, not a silent row drop at query time).

### 4.2 Per-domain token view (the contract)

Each participating domain adds to its `schema/<domain>.sql` a view with a fixed 4-column shape:

```sql
CREATE OR REPLACE VIEW insect_catalog_token AS
  -- slug row (is_slug = true): the canonical identifier
  SELECT name AS token, name AS slug, true AS is_slug,
         'insects' AS domain, 'insect-species' AS entity_type FROM insect_species
  -- non-slug surface forms (is_slug = false): scientific/taxonomic name, common names
  UNION ALL
  SELECT taxonomic_name, name, false, 'insects', 'insect-species' FROM insect_species
  UNION ALL
  SELECT common_name, species_name, false, 'insects', 'insect-species'
         FROM insect_species_common_name
  -- … genus / family / order rows likewise, each tagged with its own entity_type
  --     ('insect-genus', 'insect-family', 'insect-order')
  ;
```

Contract columns:

| column       | type    | meaning                                                        |
|--------------|---------|----------------------------------------------------------------|
| `token`      | text    | one surface form under which the entity is findable            |
| `slug`       | text    | the entity's canonical `EntityName.value()`                    |
| `is_slug`    | boolean | true when `token` *is* the slug (drives `EXACT_SLUG` vs `EXACT_TOKEN`) |
| `domain`     | text    | the `DomainId.value()` — reconstructs `EntityRef.domain()`     |
| `entity_type`| text    | stable discriminator for the row's `EntityName` subtype (reconstructs the *typed* `EntityRef.name()`) |

The domain owns which of its entities/columns it exposes, and tags each `UNION` branch with the
`entity_type` of the table it reads — the SQL analog of `CatalogContribution.searchableEntities()`
handing back a typed `EntityRef`. Multiple rows per entity (one per token) is normal, exactly as
multiple tokens index one entity in-memory. Discriminator tokens are a stable contract (they appear
in the DB and in the composition-root registry); treat renaming one as a contract change.

`pg_trgm` GIN indexes are added on the **base columns** the view reads (`insect_species.name`,
`insect_species_common_name.common_name`, …), because a plain view has no indexes of its own and
Postgres pushes the search predicate down into each `UNION` branch:

```sql
CREATE INDEX insect_species_name_trgm
    ON insect_species USING gin (lower(name) gin_trgm_ops);
```

### 4.3 App-level fan-in view + extension

The app (composition root) owns a single view over the per-domain views — it names no base tables:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- must precede any gin_trgm_ops index

CREATE OR REPLACE VIEW catalog_search_token AS
      SELECT * FROM insect_catalog_token
  UNION ALL SELECT * FROM plant_catalog_token
  UNION ALL SELECT * FROM chemistry_catalog_token
  -- … only the domains this app includes
  ;
```

If insects later renames `insect_species.name`, only `insect_catalog_token` changes; the app view
is untouched.

### 4.4 Reconstructing results

The view yields `(token, slug, is_slug, domain, entity_type)` rows. `RdbmsCatalog` maps each to a
`SearchHit`:

- `EntityRef(domain, name)`:
  - `domain` via the registered `DomainId` whose `value()` equals the row's `domain` column
    (a small `Map<String, DomainId>` built at assembly).
  - `name` via a **type registry** `Map<String, Function<String, EntityName>>` keyed by the row's
    `entity_type` discriminator: `reconstructors.get("insect-genus").apply(slug)` →
    `new InsectGenusName(slug)`. Each domain supplies the reconstructors for its own name types at
    the composition root, so no kernel-side reflection and no domain dependency in the kernel; the
    per-table `entity_type` tag is what makes the correctly *typed* name recoverable (a single
    domain owns many name types, so a per-domain reconstructor is insufficient — see §8 O1).
  - The primary guard is at assembly: a `SELECT DISTINCT domain, entity_type FROM
    catalog_search_token` pass verifies every pair the view can emit resolves in the registry, so a
    misconfigured wiring fails fast at startup rather than silently at query time. Query-time keeps a
    defensive backstop — an unresolved row is dropped and observed, matching how unrenderable refs
    are already handled — but that path should never fire once startup validation passes.
- `MatchKind` computed in SQL (§5), `matchedToken` = the row's `token`.

### 4.5 Inverse direction (shared)

`findReferencesTo`/`domainsReferencing` do not touch the DB. The provider-routing fan-out currently
inside `InMemoryCatalog` (resilience-wrapped, exception-quiet, grouped by `DomainId`) is extracted
into a package-visible `ReferenceRouting` collaborator in `kernels/catalog`. Both `InMemoryCatalog`
and `RdbmsCatalog` hold one and delegate to it. The public `@Resilient`-annotated method stays on
each adapter (so AspectJ weave scope is undisturbed); only the inner mechanics move. Existing
`InMemoryCatalogTest` coverage guards the refactor.

## 5. `MatchKind.FUZZY` and ranking

Append `FUZZY` to the enum after `PREFIX` (weakest signal, ordered last — the enum's documented
extension point). The search query computes the kind per row and keeps the strongest per
`EntityRef`:

- `lower(token) = lower(:q)` and `is_slug` → `EXACT_SLUG`
- `lower(token) = lower(:q)` and not `is_slug` → `EXACT_TOKEN`
- `lower(token) LIKE lower(:q) || '%'` → `PREFIX`
- `token % :q` (trigram similar) or `token ILIKE '%' || :q || '%'` → `FUZZY`

Ordering: by `MatchKind.ordinal()`, then `similarity(token, :q)` desc (FUZZY tie-break), then slug —
preserving the existing determinism contract while giving type-ahead a sensible fuzzy tail. Dedup
per `(EntityRef, MatchKind)` as today. Empty non-blank input still yields empty results and (open
point O2, §8) *may* fire `UnresolvedSearchObservation` — decide whether the growth-signal
observation belongs in the adapter or only in the console consumer.

## 6. Schema ordering and the view-dependency wrinkle

Adding views over base tables interacts with the seeder's valued property that each domain
"drops + recreates its tables in isolation." Resolution:

- Per-domain view DDL lives in the domain's own `schema/<domain>.sql` (domain authorship
  preserved), created with `CREATE OR REPLACE VIEW`.
- The domain schema's table drops become `DROP TABLE IF EXISTS … CASCADE` so a dependent view
  drops cleanly on re-seed.
- **Ordering constraint:** `CREATE EXTENSION pg_trgm` must run *before* any domain's
  `gin_trgm_ops` index; the app fan-in view must run *after* all per-domain views exist. The
  seeder therefore gains a pre-phase (extension) and a post-phase (fan-in view), owned by the app
  — a new `CatalogSeeding` step in `TestDbSeeder`, plus the same two statements in the app's
  runtime schema bootstrap.
- **Single-domain reseed caveat:** re-running one domain seeder with `CASCADE` drops the app
  fan-in view (it depends on the per-domain view). Documented remediation: re-run the catalog
  post-phase after any single-domain reseed. Called out so it is not a surprise.

## 7. Testing

- `RdbmsCatalogIT` against the standing seeded Postgres (the `@RegisterExtension
  RdbmsTestExtension.shared()` pattern, per-test rollback): asserts search across seeded domains —
  exact-slug, exact-token, prefix, and fuzzy hits; `findBySlug`; empty/blank input; cross-domain
  grouping via `SearchResults.groupedByDomain()`.
- Per-domain view sanity: each domain's `*SchemaDriftIT` (or a small companion) asserts its
  `<domain>_catalog_token` view exists with the 4-column contract and returns rows for a seeded
  entity.
- Inverse direction: reuse/mirror the existing in-memory fan-out assertions against `RdbmsCatalog`
  to prove the shared router behaves identically regardless of adapter.
- The mapper-select fan-out gate (`kernels/persistence-test` `nofanout`) applies — search must be a
  single query against the view, not a per-row select loop.

## 8. Open decisions for review

- **O1 — slug → typed `EntityName` (RESOLVED).** The view carries a per-row `entity_type`
  discriminator (§4.2); the adapter rebuilds the typed name via a
  `Map<String, Function<String, EntityName>>` keyed by that discriminator, registered at the
  composition root by each domain for its own name types (§4.4). This supersedes the original
  per-`DomainId` idea, which could not distinguish the several `EntityName` types a single domain
  owns (e.g. insect order/family/genus/species). Remaining confirmation: the exact registration
  surface on `RdbmsCatalogAssembly` (a `Map`, or a list of small `(discriminator, fn)` records).
- **O2 — unresolved-search observation (RESOLVED).** `RdbmsCatalog.search` fires
  `UnresolvedSearchObservation` on a non-blank miss, for parity with `InMemoryCatalog` — the growth
  signal is an adapter concern so every `Catalog` implementation emits it identically.
- **O3 — participating domains, first cut (RESOLVED).** Ship token views for the taxon/name-rich
  domains first — **insects, plants, chemistry** — and add the remaining rdbms domains as their
  console search value appears. The app fan-in view unions only the views that exist.
- **O4 — prefix index vs trgm-only (RESOLVED).** One `pg_trgm` GIN index per searched base column
  covers both `ILIKE '%…%'` (fuzzy) and `LIKE '…%'` (prefix); no separate btree
  `text_pattern_ops` index. Revisit only if prefix latency measures poorly.
