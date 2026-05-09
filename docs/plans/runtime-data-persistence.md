# Runtime Data Persistence — Sketch

A small write-back capability on `TestEntitySource` that persists `insert`
and `update` calls back to the canonical JSON files in the source tree, so
the management console becomes a usable JSON editor and console-driven data
entry survives restart without standing up an RDBMS adapter.

This is a **sketch**, not a binding plan. Promote to numbered milestones
when the design is firm and the first console write route is on deck.

---

## Why this exists

Today `TestEntitySource` is in-memory only. Reads come from canonical JSON
under `<domain>-repository-test/src/main/resources/<domain>/<subpackage>/`;
writes mutate a `HashMap` that resets on every JVM restart. The console app
is read-only as a consequence.

`plans/command-framework.md` added the public `EntityCommand` write port
(kernel + insects pilot, no controller wiring). It does **not** address
persistence — every consumer of the new write port loses data on restart.

The functional goal here is narrow: **let Pat curate canonical test data
through the management console instead of by hand-editing JSON.** Console
writes land in the same source-tree files he'd otherwise edit; he diffs
and commits like any other curation. This also unblocks the first concrete
console write route, which exercises the command UX end-to-end.

A real RDBMS adapter (MyBatis, M-future) is the long-term durability
answer, but it is multiple PRs of schema work and is overkill for a
single-user dev workflow whose entire point is making JSON curation
faster.

---

## What this is NOT

The previous sketch proposed a base+overlay model: canonical JSON stays
read-only, runtime writes go to a separate directory (`~/.naturalist/data`
or similar), reads merge with runtime overriding canonical on slug
collision. **This was wrong for the actual use case** and is rejected.

Reasons:

- The base+overlay shape is what RDBMS systems use when seed data is
  factory state and runtime data is independent user data — different
  lifecycles, different files. Pat's situation has no such split: every
  console write IS curation.
- The overlay creates a parallel JSON file that has to be reconciled
  back into canonical by hand. That's *more* work than hand-editing
  canonical directly.
- A `JsonRuntimeStore` port abstracts a behavior (`TestEntitySource`
  writing its own JSON) that has exactly one shape and one caller.
  Premature abstraction.

The shape below has **one layer**, **no separate runtime directory**,
**no new kernel interface**.

---

## Architectural decisions (declarative, do not re-derive)

**Console writes update canonical JSON in place.** No overlay, no
parallel runtime file, no merge-on-read. The file the source tree
loads from is the file the console writes back to.

**Origin tracking preserves the multi-file split.** A source like
`CompoundTestEntitySource` calls
`loadFiles("chemistry/compound/compounds-%s.json", "base", "aristolochic-acid")`
and merges both files into one map. Naively flushing to a single file
would lose the editorial split (and break the next reload). The source
remembers per-entity origin file at load time and writes each file with
its own subset on flush.

**New inserts go to a default file per source.** Configured per
subclass, defaulting to the first suffix in the `loadFiles(...)` call.
For `compounds`, that's `compounds-base.json`. If a new entry should
live in a topic file (`compounds-aristolochic-acid.json`), Pat moves it
manually before commit — that's a curatorial decision, not a kernel
concern.

**Persistence is a single global switch evaluated once at class-load.**
A `private static final boolean PERSISTENCE_ENABLED =
Boolean.getBoolean("naturalist.persistence.enabled")`. Tests don't set
the property → the field is `false` for the lifetime of the test JVM,
no `@BeforeEach` or runtime mutation can flip it. Production startup
(management-console's `main`) sets the property *before*
`SpringApplication.run`, so the property is already true when
`TestEntitySource` first loads. This is deliberately stronger than a
per-call read: **tests cannot accidentally enable persistence**, even
by setting the property at runtime, because the value is already
captured. The only ways persistence turns on are (a) production
startup, or (b) a deliberate Surefire `systemPropertyVariables`
config — both visible in code review, neither possible by accident.

**The writable path is the source tree, located by classpath
heuristic.** Not a parallel runtime directory. The source resolves the
target file per flush by inspecting
`getClass().getResource("/" + relativePath).toURI()` — in a Maven dev
layout this returns a `target/classes/...` URL, and string-replacing
`/target/classes/` with `/src/main/resources/` yields the source-tree
path. Dev-only by design: outside a Maven layout the heuristic finds
no `target/classes` segment and persistence silently disables, which
matches the project's current dev-tool framing. Zero per-source
configuration; nothing to maintain as new sources are added.

**Catalog index re-assembly is out of scope.** Newly-inserted entities
are visible to subsequent same-process reads (the in-memory map is
updated). Surfacing them in `Catalog.search` without restart is a
separate follow-up — likely a re-assembly trigger on the catalog
kernel.

---

## Sketch — minimal shape

### `TestEntitySource` changes (`kernels/framework-test`)

```java
private static final boolean PERSISTENCE_ENABLED =                // NEW
        Boolean.getBoolean("naturalist.persistence.enabled");

private final Map<NAME, ENTITY> entityMap = new HashMap<>();
private final Map<NAME, String> originFile = new HashMap<>();     // NEW
private @Nullable String defaultInsertFile;                        // NEW

// Constructor unchanged — no new arg.

public void loadFiles(String pathFormat, String... replacements) {
    if (replacements.length > 0 && defaultInsertFile == null) {
        defaultInsertFile = pathFormat.formatted(replacements[0]);
    }
    Stream.of(replacements)
            .map(pathFormat::formatted)
            .forEach(this::loadFile);
}

public void loadFile(String relativePath) {
    String json = TestDataHelper.readFileToString(relativePath);
    List<ENTITY> entities = TestDataHelper.readObjectsFromString(() -> json, entityClass());
    for (ENTITY entity : entities) {
        insert(entity);                                          // existing path
        originFile.put(entity.name(), relativePath);             // NEW
    }
}

public void insert(ENTITY entity) {
    // ... existing validation, FK / unique checks, map put
    if (!originFile.containsKey(entity.name())) {
        originFile.put(entity.name(), defaultInsertFile);        // route new inserts
    }
    flushIfWritable();
}

public void update(ENTITY entity) {
    // ... existing validation, FK / unique checks, map replace
    flushIfWritable();
}

private void flushIfWritable() {
    if (!PERSISTENCE_ENABLED) return;
    Map<String, List<ENTITY>> byFile = entityMap.values().stream()
            .collect(groupingBy(e -> originFile.get(e.name())));
    for (var entry : byFile.entrySet()) {
        Path target = resolveSourcePath(entry.getKey());
        if (target != null) writeJsonAtomic(target, entry.getValue());
    }
}

private @Nullable Path resolveSourcePath(String relativePath) {
    URL classpathUrl = getClass().getResource("/" + relativePath);
    if (classpathUrl == null) return null;
    String filePath = Paths.get(classpathUrl.toURI()).toString();
    if (!filePath.contains("/target/classes/")) return null;     // not a Maven dev layout
    return Paths.get(filePath.replace("/target/classes/", "/src/main/resources/"));
}
```

Key properties:

- `PERSISTENCE_ENABLED` is `static final`, evaluated once at class-load. Test JVMs see
  `false` for their entire lifetime — no `@BeforeEach` or runtime `System.setProperty`
  can flip it after the fact. `flushIfWritable` is a no-op in tests, structurally.
- The persistence path is **not** unit-tested. Origin tracking, default-file selection,
  and update-preserves-origin are pure in-memory state changes verifiable without
  flushing. Path resolution and atomic write are verified by manual smoke (run the
  console, edit a record, observe the source-tree file rewrite). No test ever writes
  to the source tree.
- `originFile` is populated during `loadFile` so updates round-trip to the file they came
  from. Aristolochic-acid edits land in `compounds-aristolochic-acid.json`, base edits in
  `compounds-base.json`, automatically.
- `defaultInsertFile` is the first file in the `loadFiles` call. New inserts go there.
  A subclass that wants a different default overrides one accessor.
- `resolveSourcePath` returns null outside a Maven layout — flush silently skips that
  file rather than throwing. The graceful-degrade matches the dev-tool framing.
- No new kernel interface, no constructor changes. The source writes its own JSON.

### Composition wiring

`apps/management-console` sets `naturalist.persistence.enabled=true` at startup (Spring
Boot `application.properties` `system-properties` mapping, or a one-line
`System.setProperty(...)` in the main class before `SpringApplication.run`). That's the
entire wiring change.

### Atomic write

`Files.write(tmp, ...)` then `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`.
Standard pattern; protects against torn writes if the JVM dies mid-flush.

---

## Pilot scope

One PR. Kernel change + console wiring + one source exercising it (compounds, since the
multi-file split is the interesting case).

| Module                                          | Files added / changed                                                     |
|-------------------------------------------------|---------------------------------------------------------------------------|
| `kernels/framework-test`                        | `TestEntitySource.java` — origin tracking, persistence flag, classpath-resolved atomic flush |
| `apps/management-console`                       | one-line startup hook setting `naturalist.persistence.enabled=true` |

No domain code changes. No constructor changes. Every existing `TestEntitySource`
subclass keeps working unchanged; the system property opts the whole composition root in.

---

## Verification

The kernel addition is small and `TestEntitySource` is itself test infrastructure;
adding a meta-layer of unit tests for it has diminishing returns. Verification is
**manual + by existing suites**:

1. **Existing contract suites pass.** Every `*RepositoryContractTest` and
   `*RepositoryMockTest` continues to run unchanged. The constructor signature
   doesn't change; persistence is off in tests by static-final; behavior in test
   mode is identical. User runs maven; type-check + contract suites green.
2. **Manual smoke.** Pat runs the management-console with persistence enabled,
   edits a `Compound` description through whatever UI surface exists at that time
   (or wires a one-off route ahead of the proper command-framework follow-up),
   exits, and observes that the source-tree `compounds-base.json` was rewritten
   with the new value. This is the *only* verification of the write path; it is
   deliberately out of CI.

No unit tests for origin tracking. The five lines of logic are easily inspected
by eye, and the only observable failure mode ("edits don't persist correctly")
surfaces immediately during console use.

---

## Out of scope

- The RDBMS adapter itself.
- Multi-process coordination (single-user, single-process).
- Console write routes — those land as a follow-up under `command-framework.md`. The
  first concrete route exercises this persistence shape end-to-end.
- Catalog re-assembly trigger — own follow-up under `plans/catalog-kernel.md`.
- Editorial discipline — runtime files capture whatever the console submits; if a
  description needs curatorial review before commit, that's a workflow concern (Pat's
  diff before commit), not a kernel concern.
- Routing new inserts to topic files (e.g. a new aristolochic-acid-related compound
  auto-landing in `compounds-aristolochic-acid.json` instead of `compounds-base.json`).
  Inserts go to the default file; manual move-before-commit for the rare case.

---

## How this slots into the work order

Per `docs/work-tracker.md` (Option B chosen):

1. ✅ **Command framework pilot** — kernel symmetry, no persistence change. Landed
   2026-05-09 (commit `482b48d`).
2. **Runtime data persistence** ← this plan, after the pilot.
3. **First console write route** (covered as a follow-up under
   `plans/command-framework.md`). Exercises the command port + this persistence shape
   end-to-end with one concrete `@PostMapping`.
4. **FU-1 PR-2f** — Species narrowing. Heaviest editorial slice; can interleave with
   #3 once both land.
