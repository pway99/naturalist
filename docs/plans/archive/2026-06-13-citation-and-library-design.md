# Citation + Library — Identification Phase 2, Slice 1 (Design)

Promotes the `kernels/bibliography` + `LiteratureReference` line item from
**Phase 2** of the identification roadmap
([`identification.md`](identification.md)) to its own design. Renames it
based on brainstorming: the citation concern (the shape of a cited
source) joins `kernels/authority`; the domain is `library` (the
naturalist's reference collection).

**Scope this slice:** `Citation` sealed NamedEntity + `OnlineSource`
permit in `kernels/authority`, the `domains/library` domain stack
(repository, mock, contract tests), and an EOL citation factory. No
console pages, no wiring `Set<CitationName>` onto existing entities —
those arrive with the identification workflow (Phase 2 proper).

---

## Decisions (settled in brainstorming — do not re-derive)

| Question | Decision |
|---|---|
| Kernel name | **No new kernel.** `Citation` lives in `kernels/authority` (`com.naturalist.authority`) alongside `AuthorityReference` and `AuthoritySource`. Every citation composes an `AuthorityReference`; the dependency is intrinsic, not incidental. |
| Domain name | **`domains/library`** — the naturalist's reference collection. Standard three-module split: `library-api`, `library-core`, `library-repository-test`. |
| Entity vs ValueObject | **NamedEntity.** A single citation may be referenced from multiple domains (e.g., a paper on pyrethrin biosynthesis backs claims in both plants and insects). Slug identity avoids cross-domain duplication. |
| Sealed interface | **Yes.** Different citation formats have different required fields. Permits are by citation *format* (online source, journal article, book), not by *provider* (EOL, GBIF). Providers are *producers*, not *type definers*. |
| First permit | **`OnlineSource`** — an HTTP-accessible resource in an external authority's catalog. Covers EOL pages, BugGuide nodes, iNat taxon pages. |
| Future permits | `JournalArticle`, `Book`, `ExtensionBulletin` — added when the library supports those formats. Additive; no existing code breaks. |
| Identifier location | **`CitationName` stays in `kernels/authority`** alongside `Citation`. Kernel-level EntityName subclasses stay alongside their entity; `domains/identifiers/` is for cross-domain cycle breaking between domain modules. |
| EOL factory | `Eol.citation(...)` factory method in `eol-api` — wires EOL-specific knowledge (source constant, deep link format) so callers don't assemble `AuthorityReference` by hand. |
| JAR naming | No collision — `Citation` joins the existing `authority` JAR; `domains/library` produces `library-api`, `library-core`, `library-repository-test` JARs. |

### Why Citation lives in kernels/authority, not a separate kernel

The identification roadmap originally named `kernels/bibliography` as a
new shared kernel. Brainstorming found that `Citation` composes
`AuthorityReference` as its mandatory locator — every citation is a
citation *of* an authority. Creating a separate kernel would add Maven
ceremony for a module whose sole dependency (beyond framework) is
authority itself. Any consumer of citations already needs authority
types.

The authority kernel is small (three classes pre-change). Adding
`Citation`, `OnlineSource`, and `CitationName` keeps it cohesive: the
kernel is about referencing external authorities (pointers), citing them
(metadata), and looking them up (the port).

### Why permits map to formats, not providers

EOL and GBIF both produce `OnlineSource` citations — they differ in
which `AuthoritySource` they carry, not in citation shape. A future
journal database and a DOI resolver both produce `JournalArticle`
citations. The `AuthorityReference` inside the citation already
identifies the provider. Provider-specific permits would require the
sealed interface to know about every provider, breaking the open
`AuthoritySource` design (ADR-023 precedent).

Java sealed types require all permits in the same package (no JPMS in
this project). Provider modules can't contribute permits to
`com.naturalist.authority`. This is a language constraint that aligns
with the design intent: the kernel owns the format taxonomy, providers
own the factories.

---

## Type details

### Citation (sealed NamedEntity)

```java
// in kernels/authority — com.naturalist.authority

public sealed interface Citation extends NamedEntity<CitationName> {

    AuthorityReference authorityReference();
    String title();
}
```

Required contract methods on the sealed interface: `authorityReference()`
and `title()`. Every citation, regardless of format, has a locator and a
title. `name()` is inherited from `NamedEntity`.

### Citation.OnlineSource (first permit)

```java
public record OnlineSource(
        CitationName name,
        AuthorityReference authorityReference,
        String title,
        @Nullable String author,
        @Nullable Integer year,
        @Nullable Instant lastModified
) implements Citation {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .valueObject(authorityReference, "authorityReference")
                .notBlank(title, "title");
    }
}
```

Fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | `CitationName` | yes | Slug identity, e.g. `"eol-battus-philenor-1188585"` |
| `authorityReference` | `AuthorityReference` | yes | Source + deep-link URI; descends into its own invariants |
| `title` | `String` | yes | Human-readable title of the resource |
| `author` | `String` | no | Author or editor, when available |
| `year` | `Integer` | no | Publication or copyright year |
| `lastModified` | `Instant` | no | When the online resource was last updated; enables staleness detection |

### CitationName (EntityName subclass)

```java
public final class CitationName extends EntityName {

    @JsonCreator
    public static CitationName of(@JsonProperty("value") String value) {
        return new CitationName(value);
    }

    private CitationName(String value) {
        super(value);
    }

    @Override
    protected int maxLength() {
        return 200;
    }
}
```

Slug format convention: `{source-id}-{subject-slug}-{provider-page-id}`,
e.g. `"eol-battus-philenor-1188585"`. Human-readable, unique across
sources.


---

## Domain: domains/library

### Module split

```
domains/library/
  library-api/               — repository interface, query surface
  library-core/              — application services (thin initially)
  library-repository-test/   — contract tests + in-memory mock
```

### Repository interface (package-private in library-api)

```java
interface CitationRepository {
    Optional<Citation> findByName(CitationName name);
    Set<Citation> findByAuthoritySource(AuthoritySource source);
    Set<Citation> findAll();
    void save(Citation citation);
}
```

### Query surface (public in library-api)

```java
public class LibraryQuery {
    public Set<Citation> forNames(Set<CitationName> names);
    public Set<Citation> forAuthoritySource(AuthoritySource source);
    public Set<Citation> all();
}
```

Consumer domains carry `Set<CitationName>` on their entities. The
console resolves full citation details at render time via
`LibraryQuery.forNames(...)`.

### In-memory mock (library-repository-test)

JSON-backed `CitationRepositoryMock`. Seeded with a small fixture file
containing at minimum the swallowtail EOL citation, so the
identification workflow has fixture data from day one.

### Dependencies

```
library-api               →  kernels/authority, kernels/framework
library-core              →  library-api
library-repository-test   →  library-api, kernels/framework-test
```

---

## EOL factory

In `external-authorities/eol/eol-api`, on the existing `Eol` utility
class:

```java
public static Citation.OnlineSource citation(
        CitationName name,
        EolPageId pageId,
        String title,
        @Nullable String author,
        @Nullable Integer year,
        @Nullable Instant lastModified) {
    return new Citation.OnlineSource(
        name,
        new AuthorityReference(SOURCE, deepLink(pageId)),
        title, author, year, lastModified);
}
```

Wires EOL-specific knowledge (source constant, deep link URI format) so
callers pass domain-meaningful arguments (`EolPageId`, title) rather
than assembling `AuthorityReference` by hand.

---

## DAG

```
domains/library-api            →  kernels/authority, kernels/framework
domains/library-core           →  domains/library-api
domains/library-repository-test →  domains/library-api, kernels/framework-test
external-authorities/eol-api   →  kernels/authority  (already; gains citation factory)
domains/<consumer>-api         →  kernels/authority  (carries Set<CitationName>)
```

No new kernel module. No new top-level grouping. EOL already depends on
authority. Consumer domains that already import authority for
`AuthorityReference` get `Citation` for free.

---

## What this slice does NOT include

- **Console pages.** No `/library` browsing page. Arrives when there is
  enough fixture data to justify it.
- **Consumer wiring.** No `Set<CitationName>` on `InsectSpecies` or any
  other existing entity. That happens when the identification workflow
  (Phase 2 proper) produces citations and needs somewhere to attach
  them.
- **Additional permits.** No `JournalArticle`, `Book`, or
  `ExtensionBulletin`. Added when the library supports those formats.
- **Real HTTP calls.** The EOL factory is a construction helper, not a
  fetch client. The real EOL REST adapter is Phase 4.
- **Citation population.** No automated walk of EOL data to populate
  citations. Phase 4 concern.

---

## Relationship to the identification roadmap

This slice delivers the **citation substrate** that Phase 2's
identification workflow will consume. The roadmap's Phase 2 description
("InsectIdentification workflow + kernels/bibliography") is a
compound phase; this slice unbundles the citation/library foundation as
an independent deliverable, per the work-tracker's "candidate next
slices" entry.

After this slice lands:
- The identification workflow (Phase 2 proper) can attach
  `Set<CitationName>` to curated statements, fulfilling ADR-009.
- The `domains/library` mock is seeded and ready for contract tests.
- The EOL factory produces citation instances the workflow can persist.

FU-2 (bibliography / provenance kernel) closes when this slice is
verified — the structural commitment is met.
