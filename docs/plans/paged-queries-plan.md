# Paged Queries — Plan

**Status.** Accepted, 2026-05-07. Folds in MyBatis ORM decision, opt-in
lookahead probe in place of COUNT, default page size 25 (1000 for
Solr-style bulk export), configurable contract-test page size. Pat
flagged the unbounded-query hazard at the end of PR-2e; before PR-2f or
PR-3 grows the read surface further, the kernel lands the paged-result
contract and the existing query/repository ports migrate. Implementation
begins from step 1 of Section 3.

**Why it matters.** Several query methods today return whole-catalog lists
(`getAllSpeciesNames`, `getAllPlantNames`, `getAllFamilyNames`,
`entityStream`, `forCompoundName`, `getByFunctionalGuild`, …). With the
in-memory test source this is fine; under the eventual rdbms adapter it is a
production-availability hazard — a single console request iterating a
10⁵-row table pulls the whole table into the JVM, blocks I/O threads, and
makes catalog growth a release blocker rather than a data-load.

The hazard is dormant today (in-memory adapter, ~50 entities total) but
*every* new entity type extends it. Closing it now is cheaper than retrofitting
under a paging-shaped feature later.

---

## 1. Catalogue of unbounded surfaces

These are the methods that need to migrate. Two shapes:

### 1a. "Give me everything" — bounded by total catalog size

| Surface                                                                             | Owner                          | Today's caller(s)                                                                                                                               |
|-------------------------------------------------------------------------------------|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `<Domain>Repository::getAll<Subject>Names()`                                        | every `*-repository-test` mock | the matching `*EntityQuery::all<Subject>Names()`                                                                                                |
| `<Subject>EntityQuery::all<Subject>Names()` → `EntityNameSet<NAME>`                 | every `*-api`                  | `*CatalogContribution`, the `findByNameSet` round-trip pattern                                                                                  |
| `TestEntitySource::entityStream()`                                                  | kernel                         | `PlantsController` index, console template tests, every `getAll*Names` mock implementation, internal repository scans (FK / unique constraints) |
| `EntityRepository::getByEntityNameSet(Set<NAME>)` (input-bounded but full-row scan) | kernel                         | every entity query's `findByNameSet`                                                                                                            |

The first two surfaces feed the catalog contributions, which iterate every
entity at assembly time and again on each fresh `searchableEntities()` call.

### 1b. "Give me everything matching X" — bounded by selectivity, but unbounded over time

| Surface                                                               | Owner         | Today's caller(s)                         |
|-----------------------------------------------------------------------|---------------|-------------------------------------------|
| `InsectRepository.SpeciesRepository::getByFunctionalGuild(guild)`     | insects-api   | `InsectsController` guild page            |
| `InsectRepository.ImageRepository::getBySpeciesName(speciesName)`     | insects-api   | `InsectsController` species detail        |
| `LifeStageRepository::getBySpeciesName(speciesName)`                  | insects-api   | `InsectsController` species detail        |
| `CultivarRepository::getByPlantName(plantName)`                       | plants-api    | future console — already declared         |
| `PlantProgramRepository::getByPlantName(plantName)`                   | plants-api    | future console                            |
| `SeedLineageRepository::getByCultivarName(cultivarName)`              | plants-api    | future console                            |
| `PhytochemicalConstituentRepository::getByPlantName(plantName)`       | plants-api    | `PlantCompoundReferences`, future console |
| `PhytochemicalConstituentRepository::getByCompoundName(compoundName)` | plants-api    | `PlantCompoundReferences`                 |
| `ProductRepository::getByCompoundName(compoundName)`                  | chemistry-api | future product detail                     |

These are "page size 10 by default" candidates — the worst offender is
constituents-by-compound, which can return many rows per compound under a
realistic catalog.

### 1c. Internal — does NOT need to page

- `entityStream()` use sites inside `TestEntitySource` (unique constraint
  loop, FK loop, `getByEntityNameSet`) — these run on insert/update, not
  on the read path. They stay as-is.
- Search-index assembly under Postgres tsvector + pg_trgm — that's a
  single `INSERT INTO search_index ... SELECT ...` statement entirely
  inside the database, no row marshaling, no pager involved. (The Solr
  alternative *does* paginate — see step 5 — using `lookahead=0` and a
  larger page size, but it's still pulling pages, not bypassing the
  contract.)

The split keeps the migration scope honest: read-side ports paginate;
internal scans and database-resident assembly don't.

---

## 2. Proposed kernel contract

### `Page<T>` value object

```java
public record Page<T>(
        List<T> content,
        int pageNumber,            // 0-based
        int pageSize,
        int pagesAheadKnown,       // 0 when lookahead was 0, else 0..lookahead
        boolean moreBeyondLookahead // true if at least one row exists past the
                                    // lookahead window (or, when lookahead=0,
                                    // simply: at least one more row exists)
) implements ValueObject {
    public boolean hasNext() {
        return pagesAheadKnown > 0 || moreBeyondLookahead;
    }
    ...
}
```

Why offset/limit (`pageNumber`/`pageSize`) and not cursor-based:

- The two production storage backends we anticipate (RDBMS via MyBatis,
  in-memory `TestEntitySource`) both natively support offset/limit; cursor
  resumes would require ordering keys that not every entity has cleanly
  defined. MyBatis suits the design especially well — we hand-write the
  `LIMIT` clause and the optional probe SQL once per repository, no ORM
  paging abstraction to fight.
- Offset/limit pages are plumbing-cheap at the controller layer — the URL
  carries `?page=2`, the renderer maps it to `PageRequest.of(2, 25)`,
  done.
- We can switch a specific query to a cursor token later (per-port, not
  globally) when one of these bites — the value-object shape leaves room
  for a later `Page<T>` subclass or a separate `CursorPage<T>`.

**No total count.** A separate `COUNT(*)` is a tax we don't want to pay
— it doubles round trips and tempts adapters into clever caching to hide
the cost. Horizon information (the "page 3 of 8+" hint) comes from an
opt-in lookahead probe, never a count.

### Lookahead semantics — opt-in, never overfetched

A blanket overfetch of `pageSize * (1 + lookahead) + 1` rows on every
paged read would defeat the point of paging (we adopted paging *for*
resilience; making each request 6× heavier undoes that). Instead:

- `PageRequest.lookahead` is **0 by default**. Streaming consumers
  (catalog assembly, exports, internal scans) never pay for horizon.
  The page query reads `pageSize + 1` rows; the +1 sets
  `moreBeyondLookahead`. Single round trip, one discarded row.
- When the caller opts into `lookahead > 0` (console pages: typically
  5), the adapter executes a **second, narrow probe query** rather
  than fattening the page query:

  ```sql
  -- page query: actual rows, bounded at pageSize+1
  SELECT cols... FROM t ORDER BY name LIMIT :pageSize + 1 OFFSET :off

  -- probe query: index-only on Postgres, no heap reads
  SELECT 1 FROM t ORDER BY name
                   LIMIT :lookahead * :pageSize + 1
                   OFFSET :off + :pageSize
  ```

  The probe touches only the btree on the ordering column — wire
  payload is `lookahead * pageSize + 1` one-byte rows (~150 bytes for
  `lookahead=5, pageSize=25`). Cost is one extra round trip, no extra
  table I/O. The kernel divides the probe row count by `pageSize`, caps
  at `lookahead`, and sets `moreBeyondLookahead` from the +1 sentinel.
- The in-memory `AbstractTestEntityRepository` implements both paths
  trivially against its sorted name index.

Renderer logic:

| `pagesAheadKnown` | `moreBeyondLookahead` | UI shows                |
|-------------------|-----------------------|-------------------------|
| 0                 | false                 | last page, no Next link |
| 0                 | true                  | "Next →" (no horizon)   |
| 3                 | false                 | "page N of N+3"         |
| 5 (= lookahead)   | true                  | "page N of N+5+"        |

Console pages on plain browse use `lookahead=5` and render the "+"
case as a nudge toward filtering — the affordance is "more exists,
narrow your query," not "scroll forever."

### `PageRequest` value object

```java
public record PageRequest(int pageNumber, int pageSize, int lookahead)
        implements ValueObject {

    public static PageRequest of(int pageNumber, int pageSize) {
        return new PageRequest(pageNumber, pageSize, 0);
    }

    public static PageRequest of(int pageNumber, int pageSize, int lookahead) {
        return new PageRequest(pageNumber, pageSize, lookahead);
    }

    public static PageRequest first(int pageSize) {
        return of(0, pageSize, 0);
    }

    public static PageRequest console(int pageNumber) {
        return of(pageNumber, DEFAULT_CONSOLE_PAGE_SIZE, DEFAULT_LOOKAHEAD);
    }

    public int offset() { return pageNumber * pageSize; }
}
```

Constraints (kernel invariants, not configurable — configuration is how
caps quietly disappear in production):

- `pageNumber >= 0`
- `1 <= pageSize <= MAX_PAGE_SIZE` (proposed `MAX_PAGE_SIZE = 1000`)
- `0 <= lookahead <= MAX_LOOKAHEAD` (proposed `MAX_LOOKAHEAD = 10`)
- `DEFAULT_CONSOLE_PAGE_SIZE = 25`, `DEFAULT_LOOKAHEAD = 5`

### Kernel-level read port

`EntityQuery<NAME, E, EC>` gains:

```java
Page<E> findPage(PageRequest pageRequest);
```

`EntityRepository<NAME, E>` gains:

```java
Page<E> getPage(PageRequest pageRequest);

List<E> getByEntityNameSet(Set<NAME> nameSet);  // unchanged — input-bounded
```

Existing `getByName`, `findByNameSet` stay as-is (input-bounded).

### Per-domain filter ports

The 1b surfaces (`getByFunctionalGuild`, `forSpeciesName`, `forCompoundName`)
each take their filter argument plus a `PageRequest`:

```java
Page<InsectSpecies> getByFunctionalGuild(FunctionalGuild guild, PageRequest pageRequest);
```

The query interface mirrors that with `BehavioralCollection` replaced by
`Page<E>`.

### What disappears

`EntityNameSet<NAME> all<Subject>Names()` — gone from the public surface. The
catalog contribution is the only consumer today, and it should iterate via
`findPage(PageRequest)` with `hasNext = true` driving the loop. That migrates
the contribution from "load everything into memory" to "stream pages."

For the RDBMS future this is the load-bearing change: the catalog
contribution becomes a paged scan, not a single `SELECT *`.

---

## 3. Migration order

1. **Kernel `Page<T>` + `PageRequest`** in `kernels/framework/data/`.
   Standalone — value objects, invariants, unit tests covering the
   `pagesAheadKnown` / `moreBeyondLookahead` truth table. No callers yet.
2. **`EntityRepository::getPage`, `EntityQuery::findPage`** added as
   required (drop the unbounded methods in the same PR — small ripple,
   no shim).
3. **`AbstractTestEntityRepository`** gains the page implementation:
   sub-list of the sorted in-memory index, deterministic ascending
   `name()` order. When `lookahead > 0`, computes `pagesAheadKnown` and
   `moreBeyondLookahead` from the same sorted index — no separate
   probe needed in-memory, but the contract observed by the test
   matches what the rdbms adapter must produce. `AbstractEntityQuery`
   gains the corresponding query forwarder.
4. **`TestEntitySource::pageOf(PageRequest)`** — the storage-side primitive
   the test repository delegates to. Order is `name()` ascending,
   pre-sorted on insert (or sorted on read).
5. **Catalog contributions migrate first.** They're the surface most likely
   to bite production. Each `*CatalogContribution::searchableEntities()`
   becomes a paged stream using `PageRequest.first(pageSize)` (lookahead=0,
   no horizon overhead) — `Stream.iterate(firstPage, page -> page.hasNext()
   ? loadNext() : null).flatMap(p -> p.content().stream())`.
6. **Remaining read-side queries migrate.** Per domain, in dependency order
   (chemistry → insects → plants). Each domain's controller updates to
   accept `?page=N` query params and uses `PageRequest.console(page)`
   (pageSize=25, lookahead=5) so browse views render the horizon hint
   that nudges users toward filtering.
7. **`*Names()` methods deleted.** The contract is paged or input-bounded;
   nothing else.

Each step is a separate PR. Step 1 + 2 together is the kernel landing; step
3-7 is one PR per domain.

---

## 4. Resolved decisions and remaining open questions

### Resolved

- **Default page size.** 25 for console browse (`PageRequest.console`),
  1000 for bulk export (e.g. Solr push of catalog docs) using
  `PageRequest.of(n, 1000, 0)`. With Postgres tsvector + pg_trgm, search
  index assembly is a single `INSERT INTO ... SELECT ...` and the pager
  is irrelevant — so the "1000-per-page" default only applies if/when we
  pick Solr. No per-port default needed; callers pick the constructor.
- **`totalElements` cost.** Dropped. No COUNT, no `totalElements` field.
  Horizon information comes from the opt-in lookahead probe (see Section
  2). UX accepts "more exists, narrow your filter" over precise totals.
- **Repository contract test page size.** Contract test fixture takes
  `pageSize` as a parameter; default is 2 to exercise multi-page boundaries
  (last partial page, page past the end, pageSize=1 edge) against the
  small NamedTestEntitySource fixtures. Lookahead-saturation semantics
  are tested at the kernel level (`PageTest`, synthetic source) rather
  than per-domain — that keeps domain fixtures from having to grow to
  ≥12 entities just to saturate `lookahead=5` at `pageSize=2`.
- **Catalog assembly scan.** Pages serially with `lookahead=0` (no
  overfetch, no horizon cost). The streaming consumer is the canonical
  reason `lookahead` is opt-in.

### Still open

- **Stable ordering.** All paged ports require deterministic ordering for
  the page boundaries to make sense. Default: ascending `name()`. Filtered
  queries (`getByFunctionalGuild`) need an explicit secondary order. Worth
  encoding in `PageRequest` itself, or per-port?
- **Search-backend choice.** Solr vs. Postgres tsvector + pg_trgm. Doesn't
  block the kernel landing — the page contract is identical either way —
  but determines whether the bulk-export path (`pageSize=1000, lookahead=0`)
  is wired up at all.

---

## 5. What this PR series does NOT do

- Cursor-based pagination — deferred until a real port needs it.
- Search / sort surface — `PageRequest` carries no `Sort`. Add later if
  needed.
- Streaming readers (`Stream<E> stream()` returning a lazy stream backed by
  pages) — useful for bulk migration tools; build when first caller asks.
- RDBMS adapter wiring — that's M-future. The shape we land here is what
  the adapter implements against.

---

## 6. Resumption note

A fresh chat picking this up needs:

1. This document.
2. `docs/notes/fu-1-plan.md` to confirm FU-1 work is paused at PR-2f.
3. The `git log --oneline -10` snapshot — paged-queries lands on top of
   `9f6aed6` (PR-2e).
