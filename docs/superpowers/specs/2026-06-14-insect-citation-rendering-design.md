# Insect Citation Rendering via Insect Read Model

Present citation associations on insects detail pages by composing resolved
citations into the `Insect` read model, moving the console toward the composed
read model and away from ad-hoc entity assembly.

## Context

The infrastructure is already built:

- `CitationAssociationQuery.findBySubject(EntityRef)` returns associations
  linking a `CitationName` to any cataloged entity via `EntityRef`
- `InsectCitationQueryImpl` walks the taxonomy ancestry (species -> genus ->
  family -> order), builds `EntityRef` per rank, and collects
  `CitationAssociation` records
- `InsectCitationView` (ReadModel) with `RankedCitation` value object exists
- `InsectQuery.CitationQuery` interface is declared in the namespace

What's missing: the controller never calls the citation query, no template
renders citation data, and `RankedCitation` carries only `CitationName` slugs
(not resolved `Citation` objects with title/URL).

## Design

### 1. Resolve citations in `InsectCitationView`

`RankedCitation` gains the resolved `Citation` authority object instead of the
bare `CitationName` slug:

```java
// Before
public record RankedCitation(
    CitationName citationName,
    InsectRankName attachedAt,
    @Nullable String note
) implements ValueObject

// After
public record RankedCitation(
    Citation citation,
    InsectRankName attachedAt,
    @Nullable String note
) implements ValueObject
```

`InsectCitationQueryImpl` gains `CitationQuery` as a constructor dependency
(same `library-api` module it already imports). After collecting
`CitationAssociation` records per ancestor rank, it batch-resolves the
`CitationName` set via `CitationQuery.findByNameSet(names)` and constructs
`RankedCitation` with the resolved `Citation`.

Unresolvable `CitationName` values (association exists but no `Citation` entity)
are dropped from the result with an observer warning — a data-quality signal,
not a hard failure.

### 2. `Insect` read model gains citations

```java
public record Insect(
    ImageCollection observations,
    @Nullable InsectOrderView order,
    @Nullable InsectFamilyView family,
    @Nullable InsectGenusView genus,
    @Nullable InsectSpeciesView species,
    LifeStageCollection lifeStages,
    InsectCitationView citations       // new
) implements ReadModel
```

- `empty()` supplies `InsectCitationView` with an empty citations list
- New `withCitations(InsectCitationView)` mutator
- `invariants()` descends into citations via `readModel(citations, "citations")`
- Not nullable — always present (possibly with an empty citation list), like
  `observations` and `lifeStages`

Record arity change ripples to: `with*` methods, `empty()`, `InsectTest`.

### 3. `InsectFactory` and query path

New package-private `InsectFactory` in `insects-core` assembles the full
`Insect` read model. Same pattern as `InsectTaxonViewFactory` — concrete class,
no interface, no `Impl` suffix.

Dependencies:
- `InsectQuery.SpeciesQuery`, `GenusQuery`, `FamilyQuery`, `OrderQuery`
- `InsectQuery.ImageQuery`
- `InsectLifeStageQuery`
- `InsectQuery.CitationQuery`

`buildByName(InsectRankName)` assembly:
1. Resolve the named rank entity, walk up to fill ancestor slots
   (species -> genus -> family -> order)
2. Fetch images via `imageQuery.forParentName(name)`
3. Fetch life stages via `lifeStageQuery.lifeStages().forParentName(name)`
4. Fetch citations via `citationQuery.findByRankName(name)`
5. Compose into `Insect` with rank views built from the entities

`InsectQuery` gains a top-level method:

```java
public interface InsectQuery {
    Optional<Insect> getByName(InsectRankName name);  // new
    // ... existing nested queries unchanged
}
```

### 4. Controller and template integration

Each detail page refactored to use the read model:

```java
// Before: 6+ individual queries
InsectSpecies s = insectQuery.species().getByName(speciesName).get();
InsectGenus genus = insectQuery.genera().getByName(s.genusName()).orElseThrow();
// ... more queries ...

// After: one call
Insect insect = insectQuery.getByName(speciesName).orElse(null);
if (insect == null) return "redirect:/insects";
```

Controller still handles breadcrumb, ancestor intros, description rendering,
and functional role (queried separately). Rank entities, images, life stages,
and citations come from the read model.

Templates gain a citations section on all four detail pages (species, genus,
family, order). Pattern follows the library's `list.jte` — linked title with
author/year, `attachedAt` rank label, optional note:

```html
@if(!insect.citations().citations().isEmpty())
    <section class="citations">
        <h2>Citations</h2>
        <ul>
            @for(var rc : insect.citations().citations())
                <li>
                    <a href="${rc.citation().authorityReference().url()}"
                       target="naturalist-citation">${rc.citation().title()}</a>
                    <small>(${rc.attachedAt().value()})</small>
                    @if(rc.note() != null)
                        <span class="citation-note">— ${rc.note()}</span>
                    @endif
                </li>
            @endfor
        </ul>
    </section>
@endif
```

### What stays ad-hoc for now

- `InsectFunctionalRole` — not yet on the `Insect` read model; queried separately
- Description rendering — controller calls `descriptionRenderer.render()` per level
- Breadcrumbs and ancestor intros — controller builds from rank entities
  extracted from the read model

These are natural follow-up slices for deepening the read model migration.

## Files affected

| File | Change |
|------|--------|
| `insects-api/.../InsectCitationView.java` | `RankedCitation` gains `Citation` |
| `insects-api/.../Insect.java` | Add `InsectCitationView citations` component |
| `insects-api/.../InsectQuery.java` | Add `getByName(InsectRankName)` |
| `insects-api/test/.../InsectTest.java` | Update for new arity |
| `insects-core/.../InsectCitationQueryImpl.java` | Add `CitationQuery` dep, resolve citations |
| `insects-core/.../InsectFactory.java` | New — assembles `Insect` read model |
| `insects-core/.../InsectQueryImpl.java` | Implement `getByName`, wire factory |
| `insects-console/.../InsectsController.java` | Detail pages use `insectQuery.getByName()` |
| `insects-console/jte/insects/detail.jte` | Citation section, data from read model |
| `insects-console/jte/insects/order.jte` | Citation section, data from read model |
| `insects-console/jte/insects/family.jte` | Citation section, data from read model |
| `insects-console/jte/insects/genus.jte` | Citation section, data from read model |
| `insects-test-context/.../InsectsTestContext.java` | Wire `CitationQuery` into impl |
