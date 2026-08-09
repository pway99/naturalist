# Glossary — a searchable home for naturalist vernacular

**Date:** 2026-07-16
**Status:** approved (v1 scope), implementation pending concurrent-agent coordination
**Scope:** library domain (api, core, repository-test, console) + identifiers

## Problem

The app is full of naturalist vernacular — *conspicuous, diagnostic, connexivum,
scutellum, pronotum, aposematic, voltinism* — surfaced in feature marks, ID
evidence, and even Javadoc. A learner (or developer) who hits one of these words
has nowhere to look up its meaning. This is a *learning* application; the
vocabulary is real and deserves a home, independent of any single page.

## v1 — a simple searchable glossary

Mirrors the existing `Concept` sub-context (a parallel, non-interacting
sub-context in the library alongside `Citation`), but at term granularity: a
concise one-line definition, not a page-sized four-level `Description`.

### Entity — `GlossaryTerm` (`library-api`)

```java
public record GlossaryTerm(
    GlossaryTermName name,     // slug: "connexivum", "conspicuous", "field-mark"
    String term,               // display form: "Connexivum", "Field mark"
    String definition,         // one concise sentence
    @Nullable String example   // optional usage sentence
) implements NamedEntity<GlossaryTermName> { … }
```

Invariants: `entityName(name)`, `notBlank(term)`, `notBlank(definition)`; example
optional.

### Placement + stack (mirror `Concept` exactly)

- `GlossaryTermName` → `domains/identifiers/.../com/naturalist/library/`
  (same module/package as `ConceptName`).
- `GlossaryTermQuery extends EntityQuery<GlossaryTermName, GlossaryTerm, GlossaryTermCollection>`,
  `GlossaryTermRepository`, `GlossaryTermCollection` in `library-api`
  (N=1 collapse, like Concept).
- `GlossaryTermQueryImpl` in `library-core`.
- `GlossaryTermRepositoryMock` + `GlossaryTermEntityRepositoryTest` (behavioral
  contract) in `library-repository-test`; wire into `LibraryTestContext` and
  `LibraryDataConfiguration`.
- Scaffolds do most of it: `/test-entity-source`, `/entity-repository`, `/entity-query`.

### Content — seed from the app's own vernacular

`GlossaryTermTestEntitySource` + `library/glossary/glossary-terms.json`, seeded
from words already appearing in feature marks and ID evidence: *conspicuous,
diagnostic, field mark, connexivum, scutellum, pronotum, dorsum, frons, venation,
aposematic, voltinism (univoltine / bivoltine / multivoltine), cephalothorax,
thomisid, elytra*, … (initial curated set; grows over time).

### Console (`library-console`) — mirror `/concepts`

- `GlossaryController`: `GET /glossary` (index) and `GET /glossary/{slug}` (detail,
  redirect to `/glossary` when absent) — same shape as `ConceptsController`.
- Templates `glossary/list.jte` + `glossary/detail.jte`.
- **Search: client-side filter box** over the fully-rendered A–Z list — no
  server-side search port for v1 (the vocabulary is small).
- Nav link to the glossary.

Listing uses the collection at console page size (`PageRequest.console`); the
glossary is small enough to render whole.

## Out of scope — phase 2

- **Domain tagging** — optional `domains` facet so the glossary can filter by
  domain and domain pages can surface their vocabulary. Deferred because it forces
  a catalog-serialization decision (`List<DomainId>` + a JSON shim like
  `CitationAssociationJson`, *or* a slug-valued `DomainTag` in `identifiers`). When
  built, lean toward the slug-valued tag for decoupling.
- **Inline auto-linking** — turning jargon inside feature marks / ID evidence /
  descriptions into links to `/glossary/{slug}`. The slug identity is the seam that
  makes this possible later.

## PR slicing (one concern per PR, ADR-019)

1. `GlossaryTerm` entity + `GlossaryTermName`.
2. `GlossaryTermTestEntitySource` + seed JSON.
3. Repository + mock + behavioral contract.
4. Query (`GlossaryTermQuery` + impl).
5. Console index + detail + nav link.
