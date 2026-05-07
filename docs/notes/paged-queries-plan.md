# Paged Queries — Plan

**Status.** Draft, 2026-05-06. Pat flagged the unbounded-query hazard at the
end of PR-2e. Before PR-2f or PR-3 grows the read surface further, the kernel
needs a paged-result contract and the existing query/repository ports need
to migrate.

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
  loop, FK loop, `getByEntityNameSet`) — these run on insert/update, not on
  the read path. They stay as-is.
- Catalog assembly (`InMemoryCatalog` building the token index once) — the
  whole-catalog scan is the operation. Paging it doesn't help; only a
  different storage shape (e.g. a Lucene index) would.

The split keeps the migration scope honest: read-side ports paginate;
internal scans don't.

---

## 2. Proposed kernel contract

### `Page<T>` value object

```java
public record Page<T>(
        List<T> content,
        int pageNumber,        // 0-based
        int pageSize,
        long totalElements,    // -1 if not computed
        boolean hasNext
) implements ValueObject { ... }
```

Why offset/limit (`pageNumber`/`pageSize`) and not cursor-based:

- The two production storage backends we anticipate (RDBMS via JPA,
  in-memory `TestEntitySource`) both natively support offset/limit; cursor
  resumes would require ordering keys that not every entity has cleanly
  defined.
- Offset/limit pages are plumbing-cheap at the controller layer — the URL
  carries `?page=2`, the renderer maps it to `PageRequest.of(2, 25)`,
  done.
- We can switch a specific query to a cursor token later (per-port, not
  globally) when one of these bites — the value-object shape leaves room
  for a later `Page<T>` subclass or a separate `CursorPage<T>`.

`totalElements = -1` is the kernel's way of saying "I didn't compute this."
Most queries will compute it (it's cheap on the in-memory backend); some
RDBMS adapters may skip the count for hot paths. Renderers degrade
gracefully — "showing page 2 of N" becomes "showing page 2".

### `PageRequest` value object

```java
public record PageRequest(int pageNumber, int pageSize) implements ValueObject {
    public static PageRequest of(int pageNumber, int pageSize) { ...}

    public static PageRequest first(int pageSize) { ...}

    public int offset() {
        return pageNumber * pageSize;
    }
}
```

Constraints: `pageNumber >= 0`, `1 <= pageSize <= MAX_PAGE_SIZE` (proposed
`MAX_PAGE_SIZE = 1000`, hard-capped by an invariant — not configurable,
because configuration is how this kind of cap quietly disappears in
production).

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
   Standalone — value objects, invariants, unit tests. No callers yet.
2. **`EntityRepository::getPage`, `EntityQuery::findPage`** added as
   default-method-or-required, depending on whether we keep transitional
   sources. Recommend: add as required, drop the unbounded methods in the
   same PR — small ripple, no shim.
3. **`AbstractTestEntityRepository`** gains the page implementation
   (sub-list of the in-memory map, deterministic order by `name()`).
   `AbstractEntityQuery` gains the corresponding query forwarder.
4. **`TestEntitySource::pageOf(PageRequest)`** — the storage-side primitive
   the test repository delegates to. Order is `name()` ascending,
   pre-sorted on insert (or sorted on read). Either is fine in-memory; the
   ordering matters for catalog contributions and tests.
5. **Catalog contributions migrate first.** They're the surface most likely
   to bite production. Each `*CatalogContribution::searchableEntities()`
   becomes a paged stream — `Stream.iterate(firstPage, page -> page.hasNext()
   ? loadNext() : null).flatMap(p -> p.content().stream())`.
6. **Remaining read-side queries migrate.** Per domain, in dependency order
   (chemistry → insects → plants). Each domain's controller updates to
   accept `?page=N` query params.
7. **`*Names()` methods deleted.** The contract is paged or input-bounded;
   nothing else.

Each step is a separate PR. Step 1 + 2 together is the kernel landing; step
3-7 is one PR per domain.

---

## 4. Open questions

- **Default page size.** Proposed 25 for console pages, 200 for
  contribution-driven scans (catalog assembly). Single number? Or per-port
  default?
- **Stable ordering.** All paged ports require deterministic ordering for
  the page boundaries to make sense. Default: ascending `name()`. Filtered
  queries (`getByFunctionalGuild`) need an explicit secondary order. Worth
  encoding in `PageRequest` itself, or per-port?
- **`totalElements` cost.** Compute it everywhere or skip when expensive?
  The in-memory backend can always compute cheaply; the RDBMS adapter may
  want to opt out.
- **Catalog assembly scan.** Does the assembly loop over all pages
  serially, or do we accept a one-time bulk read at startup and only
  paginate the *runtime* surface? The cleaner story is "everything paginates";
  the simpler story is "assembly is one-shot."
- **Repository contract test.** `EntityRepositoryTest` needs page-edge
  cases — empty page, last partial page, page past the end, pageSize=1, etc.

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
