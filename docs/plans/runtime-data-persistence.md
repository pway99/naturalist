# Runtime Data Persistence — Sketch

A small write-back hook on `TestEntitySource` that persists `insert`
and `update` calls to JSON files outside the classpath, so
console-driven data entry survives restart without standing up an
RDBMS adapter.

This is a **sketch**, not a binding plan. Promote to numbered
milestones when the design is firm and the first console write route
is on deck.

---

## Why this exists

Today `TestEntitySource` is in-memory only. Reads come from canonical
JSON catalog files in `src/main/resources/<domain>/...`; writes mutate
a `HashMap` that resets on every JVM restart. The console app is
read-only as a consequence.

`plans/command-framework.md` adds the public `EntityCommand` write
port (kernel + insects pilot, no controller wiring). It does **not**
address persistence — every consumer of the new write port will still
lose data on restart.

A real RDBMS adapter (MyBatis, M-future) is the long-term durability
answer, but standing it up is multiple PRs of schema work,
session-management wiring, and migration tooling. Before that lands,
console-driven data entry needs a stop-gap:

1. Pat enters a new species sighting / image / phytochemical
   constituent through a future console route.
2. The route calls `insectCommand.species().insert(entity)`, which
   calls `repository.insert(entity)`, which calls
   `testEntitySource.insert(entity)`.
3. The entity persists to disk and is reloaded on next boot.

No new module, no MyBatis, no schema migration ceremony. JSON is
already the canonical data shape; we just need a writable copy.

---

## Architectural decisions (declarative, do not re-derive)

**Write-back is opt-in and configured at the composition root.**
`TestEntitySource` gains an optional `JsonRuntimeStore` collaborator.
When null (the unit-test default), behavior is unchanged — purely
in-memory, no disk I/O. When wired (the `apps/management-console`
composition root), `insert` and `update` also persist.

**Canonical JSON files are read-only at runtime.**
`src/main/resources/<domain>/...` is hand-curated and version-controlled
in the repo. Runtime writes go to a separate location outside the
classpath. The two layers merge on read with **runtime overriding
canonical on slug collision** (so a console edit of an existing record
shadows the seed without rewriting the canonical file).

**Runtime data location is configured, not hardcoded.** The composition
root binds `naturalist.data.dir` (default `~/.naturalist/data` or
similar) and wires it through to each entity source. The kernel knows
the abstraction; the value comes from configuration.

**One runtime file per source.** Append-only-feeling from the caller's
perspective; under the hood, each insert/update rewrites the source's
runtime file with the full current map. Simpler than journaling;
acceptable at fixture scale and well below RDBMS-justifying volume.

**Catalog index re-assembly is out of scope here.** Newly-inserted
entities are visible to subsequent same-process reads (the in-memory
map is updated). Surfacing them in `Catalog.search` without restart is
a separate follow-up — likely a re-assembly trigger on the catalog
kernel.

---

## Sketch — minimal shape

### Kernel addition

```java
// kernels/framework-test/.../data/JsonRuntimeStore.java
public interface JsonRuntimeStore<ENTITY> {
    List<ENTITY> read();          // existing runtime entries (empty list if none)
    void write(List<ENTITY> all); // full rewrite of the runtime file
}
```

### `TestEntitySource` change

```java
private final @Nullable JsonRuntimeStore<ENTITY> runtimeStore;

protected TestEntitySource(NaturalistDatabase database) {
    this(database, null);
}

protected TestEntitySource(NaturalistDatabase database,
                            @Nullable JsonRuntimeStore<ENTITY> runtimeStore) {
    this.database = database;
    this.runtimeStore = runtimeStore;
}

// loadFiles(...) loads canonical JSON first; subclasses call
// loadRuntimeOverlay() afterwards in their constructor (or the base
// class invokes it after the subclass has finished loading the
// canonical files — design choice in the implementing PR).
protected void loadRuntimeOverlay() {
    if (runtimeStore == null) return;
    for (ENTITY e : runtimeStore.read()) {
        entityMap.put(e.name(), e); // overrides canonical on slug collision
    }
}

public void insert(ENTITY entity) {
    // ... existing argument validation, FK / unique checks
    entityMap.put(name, entity);
    flushRuntime();
}

public void update(ENTITY entity) {
    // ... existing argument validation, FK / unique checks
    entityMap.replace(name, entity);
    flushRuntime();
}

private void flushRuntime() {
    if (runtimeStore != null) {
        runtimeStore.write(List.copyOf(entityMap.values()));
    }
}
```

### Concrete adapter

```java
// kernels/framework-test/.../data/FilesystemJsonRuntimeStore.java
public final class FilesystemJsonRuntimeStore<ENTITY> implements JsonRuntimeStore<ENTITY> {
    private final Path file;
    private final Class<ENTITY> entityClass;
    private final ObjectMapper mapper;

    public FilesystemJsonRuntimeStore(Path runtimeDir, String sourceName, Class<ENTITY> entityClass) {
        this.file = runtimeDir.resolve(sourceName + "-runtime.json");
        this.entityClass = entityClass;
        this.mapper = TestDataHelper.objectMapper();
    }

    @Override public List<ENTITY> read() { /* return [] if file absent; else readValue */ }
    @Override public void write(List<ENTITY> all) { /* atomic rename: write tmp, mv */ }
}
```

### Composition wiring

`apps/management-console` configuration class binds the runtime
directory and supplies it to each `TestEntitySource` subclass that
opts in. Tests get the no-arg constructor; production gets the
two-arg constructor with a real store.

---

## Open design questions

- **Subclass constructor API.** Two constructors per source (one for
  tests, one for production) vs. a setter call after construction.
  Lean two-constructor — keeps the wiring explicit at the composition
  root.
- **Runtime directory layout.** Flat (`<dir>/<source>-runtime.json`)
  vs. domain-grouped (`<dir>/<domain>/<source>-runtime.json`). Lean
  flat unless a domain emits enough sources to warrant grouping.
- **Atomic write.** `Files.move(..., ATOMIC_MOVE)` after writing a
  sibling tempfile keeps the runtime file from being torn during a
  crash mid-write. Standard pattern.
- **Concurrent process access.** Single-process at the moment. Flag
  if a second console ever runs against the same runtime directory;
  cheap fix is `FileChannel.tryLock` on write.
- **Search re-assembly.** Today's `InMemoryCatalog` builds its token
  index once at startup. After this lands, an inserted entity is in
  the source map but not in the catalog index until restart — fine
  for data entry, surprising for search. Follow-up effort: a
  re-assembly trigger on the catalog kernel (likely a sibling of
  `M9b`–`M12` in `plans/catalog-kernel.md`, or a new milestone there).
- **Migration to RDBMS.** When the MyBatis adapter lands, the
  runtime JSON files become the migration source for already-entered
  data. A small "import runtime JSON into Postgres" tool is M-future
  and lives with the RDBMS plan, not here.

---

## Out of scope

- The RDBMS adapter itself.
- Multi-process coordination beyond a single-process file lock.
- Console write routes — those land in `command-framework.md`
  follow-up #1; the first concrete route exercises this persistence
  shape end-to-end.
- Catalog re-assembly trigger — own follow-up under
  `plans/catalog-kernel.md`.
- Editorial discipline — runtime files capture whatever the console
  submits; if the description fields need curatorial review before
  promotion to canonical, that's a workflow concern (a future "review
  the runtime overlay" pass), not a kernel concern.

---

## How this slots into the work order

Per `docs/work-tracker.md` (Option B chosen):

1. **Command framework pilot** — kernel symmetry, no persistence change.
2. **Runtime data persistence** ← this plan, after the pilot lands.
3. **First console write route** (covered as a follow-up under
   `plans/command-framework.md`).
4. **FU-1 PR-2f** — Species narrowing (heavy editorial slice).

Steps 2 and 3 can interleave with FU-1 once they land — they don't
block PR-2f, but they do unblock console-side data entry, which
could shorten the editorial cycle for PR-2f's family/genus
descriptions if Pat prefers entering them through the console rather
than hand-editing JSON.
