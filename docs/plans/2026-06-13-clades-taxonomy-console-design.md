# Clades & Taxonomy Console Teaching Surface — Design

**Date:** 2026-06-13
**Status:** Design (awaiting review) → implementation plan to follow
**Source content:** `docs/notes/clade-assignment-investigation/clades-taxonomy-durrell.md`
**Coordinates with (concurrent):** `docs/plans/2026-06-13-paraphyly-fixtures.md`

## Goal

Describe the concepts of **clades** and **taxonomy** to the naturalist inside the
management console, and make the evolutionary tree of life **browsable**, so a
reader on any insect page can climb from a specimen to "what is a clade" and
"how do clades relate to taxonomy".

Three user-visible capabilities:

1. **Concept pages** — render the two four-level Durrell concept descriptions
   (`clade`, `clade-taxonomy-relation`) authored in the source note.
2. **Clade browser** — a navigable tree of life; each clade shows its own
   four-level description (carried on the `Clade` class), its ancestry, and its
   direct children.
3. **Cross-linking** — clade names already shown in insect breadcrumbs link to
   the clade browser; clade pages link out to the concept pages; both reachable
   from the console's "More" navigation.

## Non-goals (deliberately deferred)

- **Clade → trait display** on the app-level clade pages (e.g. "confers
  holometabolous development"). That mapping (`InsectClades.traitsFor`) is
  insects-specific; surfacing it on a domain-agnostic clade page would couple the
  app to one domain. Better added later on insect species pages.
- **Concept search** via the catalog kernel (`domain-catalog` skill). Easy
  follow-up; not required to "describe the concepts".
- Any change to the per-clade prose, the clade permit set, or the insect catalog
  — all owned by the concurrent paraphyly-fixtures plan (see Coordination).

## Key facts that shaped this design

- The console is **JTE**-based and already has a reusable four-level Durrell
  renderer: `apps/management-console/src/main/jte/components/description.jte`
  plus `kernels/field-notes/.../render/DescriptionRenderer`.
- **Clades carry their own `Description`** on the class (e.g.
  `kernels/clades/.../Blattodea.java#description()`). The clade browser reads
  these directly from the kernel — the new domain entity holds only the two
  *meta*-concepts, never per-clade prose.
- Clades are surfaced today **only as breadcrumbs** (`InsectsController.cladePrefix`,
  `CLADE_URL` map) — no browsable clade page exists. There are **no** static /
  glossary / teaching pages in the console at all.
- The console gets its read surface from a `*TestContext.create(db)` factory per
  domain (chemistry/insects pattern); controllers talk only to the resulting
  **query**, never to JSON.
- The **`library`** domain already exists (api/core/repository-test) holding the
  sealed `Citation` NamedEntity. It has **no** `test-context` module, **no**
  console, is **not** wired into management-console, and its api does **not**
  depend on `field-notes`.
- `kernels/clades` exposes parent traversal only (`CladeTraversal.ancestry`,
  `findTrait`); there is **no** runtime enumeration of all clades and **no**
  children lookup.

## Architecture

### A. New sub-context `Concept` in the `library` domain

`Concept` is a teaching/reference entry — a parallel sub-context alongside
`Citation`, with no interaction with the sealed `Citation` hierarchy. Package
`com.naturalist.library`.

**`library-api`**
- `Concept` — `record Concept(ConceptName name, String title, Description description)
  implements NamedEntity<ConceptName>`. `Description` (four non-null Durrell
  levels) from `kernels/field-notes`. `title` is the human heading
  ("What is a clade?", "How do clades relate to taxonomy?").
- `ConceptRepository` — package-private `interface ConceptRepository extends
  EntityRepository<ConceptName, Concept>` (mirrors `CitationRepository`).
- `ConceptQuery` — `public interface ConceptQuery extends
  EntityQuery<ConceptName, Concept, ConceptCollection>`.
- `ConceptCollection` — `public final class ... extends BehavioralCollection<Concept>`.
- **pom:** add `field-notes` dependency to `library-api` (not present today).

**`library-core`**
- `ConceptQueryImpl` (`@DomainService`, package-private) + `ConceptQueryImplTest`.

**`library-repository-test`**
- `ConceptTestEntitySource extends TestEntitySource<ConceptName, Concept>`
  loading `library/concepts.json`.
- `ConceptRepositoryMock` (`@DomainService`) implementing `ConceptRepository`.
- `ConceptEntityRepositoryTest` contract interface + `ConceptRepositoryMockTest`
  + `ConceptTestEntitySourceTest`.
- **Resource** `library/concepts.json` — the two blocks from the source note,
  copied verbatim, each extended with a `title` field. This file is the
  single source of truth the console consumes (via the query).

**New module `library-test-context`** (mirrors `chemistry-test-context`)
- `LibraryTestContext.create(db)` exposing `conceptQuery()` (and room for
  `citationQuery()` later). Needed because the console wires its query through a
  test-context factory and library has none today.

**Identifiers**
- `domains/identifiers/.../com/naturalist/library/ConceptName.java` —
  `public final class ConceptName extends EntityName` (kebab slug; `@JsonCreator
  of(String)`).
- `domains/identifiers-test/.../com/naturalist/library/TestLibraryIdentifiers.java`
  — add a `Concepts` inner class (the two slugs + a `NotFound`).

Scaffolding uses the `test-entity-source`, `entity-repository`, and
`entity-query` skills.

### B. Kernel addition — clade enumeration (`kernels/clades`)

A browsable tree needs the full clade set and children; the kernel has neither.
Add **one new file** (no edits to existing kernel files — see Coordination):

- `CladeCatalog.java`
  - `static List<Clade> all()` — enumerates `Clade.class.getPermittedSubclasses()`
    and instantiates each (every permit is a no-arg record). Auto-tracks the
    permit list with zero dual-maintenance.
  - `static List<Clade> childrenOf(Clade parent)` — `all()` filtered to those
    whose `parent()` equals `parent`.
  - (`CladeTraversal.ancestry` already provides the upward chain.)

### C. App-level surfaces in `management-console`

Routing is domain-agnostic and app-level (clades/concepts are cross-cutting).
`management-console` already depends on everything it needs except library.

- **pom:** add `library-api`, `library-core`, `library-repository-test`,
  `library-test-context` dependencies.
- **`ConceptsController`**
  - `GET /concepts` — list the concepts (title + lead) from `ConceptQuery`.
  - `GET /concepts/{slug}` — render the four-level `Description` via
    `components/description.jte` + `DescriptionRenderer`.
  - Query obtained via `LibraryTestContext.create(NaturalistDatabase.create())`
    (same constructor pattern as `InsectsController`/`ChemistryController`).
- **`CladesController`**
  - `GET /clades` — the tree of life, rendered nested from `CladeCatalog.all()`
    + `childrenOf`.
  - `GET /clades/{slug}` — resolve via `Clade.of(slug)`; show `displayName()`,
    the four-level `Description` from `clade.description()`, the linked ancestry
    breadcrumb (`CladeTraversal.ancestry`), linked direct children
    (`CladeCatalog.childrenOf`), and links to the two concept pages.
  - Reads clades directly from the kernel (clades are code, not repository data —
    consistent with the existing `cladePrefix` breadcrumb code).
- **Templates** (new, under `apps/management-console/src/main/jte/`): a
  `concepts/` and `clades/` set reusing `layout/page.jte` and
  `components/description.jte`.

### D. Cross-linking & navigation

- Expand `InsectsController.CLADE_URL` so every clade in the breadcrumb /
  ancestor line links to `/clades/{slug}` (today only `Insecta` links anywhere).
- Clade detail pages link to `/concepts/clade` and
  `/concepts/clade-taxonomy-relation`.
- Populate the existing **"More" overflow** in `layout/page.jte` (it auto-reveals
  when non-empty, per the in-code comment) with **Tree of Life** (`/clades`) and
  **Concepts** (`/concepts`).

## Module / DAG impact

```
library-api            → framework, identifiers, field-notes   (field-notes is NEW)
library-core           → library-api
library-repository-test→ library-api, framework-test, identifiers-test
library-test-context   → library-api, library-core, library-repository-test   (NEW module)
management-console     → + library-api, library-core, library-repository-test, library-test-context
kernels/clades         → unchanged deps (adds CladeCatalog.java only)
```

No new cycles; mirrors the chemistry domain's shape.

## Coordination with the concurrent paraphyly-fixtures plan

The paraphyly plan (`docs/plans/2026-06-13-paraphyly-fixtures.md`) edits
`Clade.java` (permits + `of()` factory), `CladeTest.java`, the new permit files,
`Papilionidae.java`, `InsectClades.traitsFor`, the insect catalog JSON, and
`TestInsectsIdentifiers`.

This design **touches none of those**. The only shared area is `kernels/clades`,
where this work adds a **single new file** (`CladeCatalog.java`). Because
`CladeCatalog.all()` enumerates `getPermittedSubclasses()` at runtime, the clade
browser automatically reflects whatever permits exist whenever it runs — no
ordering dependency between the two efforts in either direction. The paraphyly
clades simply make the tree a richer worked example of the paraphyly the concept
text discusses.

## Content (single source of truth)

`library/concepts.json` carries the two entries, prose identical to the source
note, each with an added `title`:

```json
[
  {
    "name": "clade",
    "title": "What is a clade?",
    "description": { "preschool": "…", "elementary": "…", "secondary": "…", "university": "…" }
  },
  {
    "name": "clade-taxonomy-relation",
    "title": "How do clades relate to taxonomy?",
    "description": { "preschool": "…", "elementary": "…", "secondary": "…", "university": "…" }
  }
]
```

(Full text copied verbatim from
`docs/notes/clade-assignment-investigation/clades-taxonomy-durrell.md`; edits to
one must be mirrored to the other, per that note.)

## Testing

- `Concept` repository: behavioral contract test + mock test +
  test-entity-source test (per the standard NamedEntity stack); domain-specific
  repository methods (none beyond inherited) — none to add.
- `CladeCatalog`: unit test asserting `all()` covers every permit (size matches
  `getPermittedSubclasses().length`) and `childrenOf` returns the correct direct
  descendants for a known node.
- Controllers: existing console controllers have no slice tests; manual smoke via
  `mvn verify` + running the console (consistent with the repo's current console
  testing posture).

## Implementation order (for the plan to expand)

1. `library` `Concept` stack (api → core → repository-test → identifiers →
   test-context) + `concepts.json`.
2. `kernels/clades` `CladeCatalog`.
3. `management-console` wiring (poms) + `ConceptsController` + `CladesController`
   + templates.
4. Cross-linking (`CLADE_URL`, clade↔concept links) + "More" nav entries.
