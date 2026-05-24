# InsectOrder: Order-rank entity and genus/family refactoring

Date: 2026-05-24

## Problem

The insects domain currently models three Linnaean ranks: Family, Genus, Species.
Family is the ceiling. But the 16 catalogued families span 7 distinct orders
(Diptera, Hymenoptera, Lepidoptera, Coleoptera, Neuroptera, Hemiptera, Blattodea),
and a naturalist navigating the catalog has no way to group or browse by order.
Order is the topmost rank within Class Insecta and needs to be a first-class entity.

A secondary problem: `InsectGenus` carries local copies of its ancestors' Linnaean
epithets (`TaxonomicOrder order`, `TaxonomicFamily family`) for display convenience.
`InsectFamily` carries `TaxonomicOrder order` the same way. This pattern diverges
from `InsectSpecies`, which carries only typed `EntityName` FKs to its ancestors
and its own `TaxonomicSpecies epithet`. The local-copy pattern lets catalog
infrastructure dictate the domain model. The domain should model the world; the
presentation layer should resolve what it needs.

## Design principles

- **Domain models the world.** A genus belongs to a family via a typed reference,
  not by carrying a copy of the family's Latin epithet. The catalog and console
  resolve parent entities when they need display epithets.
- **Typed identifiers.** Cross-entity references are always `EntityName` subclasses,
  never raw strings or `NamedValue` copies of ancestor epithets.
- **Consistent FK pattern.** Every rank carries typed FKs to its direct parent and
  grandparent (when they exist), plus only its own `Taxonomic*` epithet. This is
  the pattern `InsectSpecies` already follows.

## Phase 1: Refactor InsectGenus -- drop local epithet copies

### InsectGenus record changes

Current components:

```java
public record InsectGenus(
    InsectGenusName name,
    InsectFamilyName familyName,    // typed FK to parent -- KEEP
    TaxonomicOrder order,           // local copy -- REMOVE
    TaxonomicFamily family,         // local copy -- REMOVE
    TaxonomicGenus genus,           // own epithet -- KEEP
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn,
    @Nullable EggStage egg,
    @Nullable LarvaStage larva,
    @Nullable PupaStage pupa,
    @Nullable AdultStage adult
)
```

After refactoring:

```java
public record InsectGenus(
    InsectGenusName name,
    InsectFamilyName familyName,
    TaxonomicGenus genus,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn,
    @Nullable EggStage egg,
    @Nullable LarvaStage larva,
    @Nullable PupaStage pupa,
    @Nullable AdultStage adult
)
```

Remove `TaxonomicOrder order` and `TaxonomicFamily family`. The genus knows its
parent family by typed FK (`familyName`); when the family epithet or order is
needed for display, the consumer resolves the parent `InsectFamily` entity.

### LinnaeanGenus interface changes

Current contract:

```java
public interface LinnaeanGenus<FAMILY_NAME extends EntityName> {
    FAMILY_NAME familyName();
    TaxonomicFamily family();    // DROP
    TaxonomicGenus genus();
}
```

After refactoring:

```java
public interface LinnaeanGenus<FAMILY_NAME extends EntityName> {
    FAMILY_NAME familyName();
    TaxonomicGenus genus();
}
```

Drop `family()`. The parent-child consistency check (genus's family epithet must
match its resolved parent's family epithet) was the reason this method existed.
That check moves to catalog-assembly time where the parent entity is resolved
anyway. The domain model should not carry redundant data to serve a presentation
concern.

Note: if other organism domains (plants, arachnids) implement `LinnaeanGenus`,
they would need the same update. Currently only `InsectGenus` implements it;
the plants domain is deferred.

### InsectGenus invariants

Remove `.namedValue(order, "order")` and `.namedValue(family, "family")` from
`invariants()`. The remaining invariants are: `name`, `familyName`, `genus`,
`description`, `commonNames`, and the four nullable life stages.

### InsectGenus with* methods

Remove all references to `order` and `family` from the `with*` constructor calls.
Each method's parameter list shrinks by two components.

### JSON catalog: insect-genera.json

Remove `"order"` and `"family"` fields from every genus record. Before:

```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "order": "Hymenoptera",
  "family": "Halictidae",
  "genus": "Halictus",
  ...
}
```

After:

```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "genus": "Halictus",
  ...
}
```

### InsectsCatalogContribution

The catalog contribution currently builds a `Map<InsectGenusName, InsectGenus>` to
resolve genus epithets for species binomial tokens. The genus's `TaxonomicGenus
genus` epithet is still present after this refactor, so species token generation
is unaffected.

The genus-level `tokensFor(InsectGenus)` method uses `entity.genus().value()` for
the title-case epithet token -- unaffected.

No chain check currently uses `InsectGenus.family()` or `InsectGenus.order()` in
the catalog contribution, so no logic changes are needed.

### Console controller and templates

The genus detail page (`/insects/genera/{name}`) currently has access to the genus
entity. If any template displays the family epithet or order from the genus record,
it must resolve the parent family entity instead. The controller already resolves
parent entities for the species detail page (lines 170-171 of `InsectsController`);
the genus detail handler should follow the same pattern.

The species list page (`/insects`) already resolves family and genus entities into
maps (`familyByName`, `genusByName`). The genus entities in that map lose their
`order` and `family` fields, but the family entities still carry `TaxonomicFamily
family` and `TaxonomicOrder order` at this phase, so any template that needs order
or family epithets should read from the family map.

### Tests

- `InsectGenusTest`: Update valid/invalid invariant assertions. The invalid case
  drops `"order"` and `"family"` from the expected invariant name set.
- `InsectGenusTestEntitySource`: No structural change needed -- Jackson
  deserialization naturally ignores removed components when the JSON fields are
  removed.
- `TestInsectsIdentifiers`: No changes -- genus identifiers are `InsectGenusName`
  constants, not epithet strings.
- Repository contract tests for `GenusRepository`: Update `newEntity()`,
  `ghostEntity()`, and `modifiedEntity()` to drop the two removed components.

### TaxonomicSlugs

No changes. The `genusSlug()` method operates on `TaxonomicGenus`, which is
retained.

---

## Phase 2: Introduce InsectOrder

### InsectOrderName (domains/identifiers)

New `EntityName` subclass:

```java
public record InsectOrderName(String value) extends EntityName<String> {
    public static final int MAX_LENGTH = 64;

    @JsonCreator
    public static InsectOrderName of(String value) {
        return new InsectOrderName(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank() && value.length() <= MAX_LENGTH;
    }

    public LinealRank rank() {
        return LinealRank.ORDER;
    }
}
```

Slug convention: lowercased order epithet -- `"diptera"`, `"hymenoptera"`,
`"lepidoptera"`. Single word, no hyphens. Order is the topmost rank within
Class Insecta.

### InsectRankName sealed interface update

Add `InsectOrderName` as a fifth permit:

```java
public sealed interface InsectRankName
    permits InsectOrderName, InsectFamilyName, InsectGenusName,
            InsectSpeciesName, InsectSubspeciesName { ... }
```

### LinnaeanOrder interface (kernels/taxonomy)

New interface parallel to `LinnaeanFamily`:

```java
public interface LinnaeanOrder {
    TaxonomicOrder order();

    default String orderSlug() {
        return TaxonomicSlugs.orderSlug(order());
    }
}
```

### TaxonomicSlugs addition

Add `orderSlug(TaxonomicOrder)`:

```java
static String orderSlug(TaxonomicOrder order) {
    return kebab(order.value());
}
```

### LinnaeanFamily interface evolution

Add generic parameter for the parent order FK:

```java
public interface LinnaeanFamily<ORDER_NAME extends EntityName> {
    ORDER_NAME orderName();
    TaxonomicFamily family();

    default String familySlug() {
        return TaxonomicSlugs.familySlug(family());
    }
}
```

`InsectFamily` implements `LinnaeanFamily<InsectOrderName>`.

### InsectOrder entity (insects-api)

```java
public record InsectOrder(
    InsectOrderName name,
    TaxonomicOrder order,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn,
    @Nullable EggStage egg,
    @Nullable LarvaStage larva,
    @Nullable PupaStage pupa,
    @Nullable AdultStage adult
) implements NamedEntity<InsectOrderName>, LinnaeanOrder { ... }
```

Components follow the same shape as `InsectFamily`. Order is the root of the
hierarchy within the insects domain -- no parent FK. `TaxonomicOrder order` is
the proper-cased Linnaean epithet (e.g., `"Diptera"`). `with*` methods for
`placedIn` and all four life stages. Invariants validate `name`, `order`,
`description`, `commonNames`, and the four nullable stages.

### InsectFamily record changes

Drop `TaxonomicOrder order`, add `InsectOrderName orderName`:

```java
public record InsectFamily(
    InsectFamilyName name,
    InsectOrderName orderName,
    TaxonomicFamily family,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn,
    @Nullable EggStage egg,
    @Nullable LarvaStage larva,
    @Nullable PupaStage pupa,
    @Nullable AdultStage adult
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily<InsectOrderName> { ... }
```

Invariants: replace `.namedValue(order, "order")` with `.entityName(orderName,
"orderName")`. All `with*` methods and callers updated.

### InsectGenus record changes

Add `InsectOrderName orderName` as grandparent FK (same pattern as
`InsectSpecies` carrying `InsectFamilyName familyName` alongside its direct
parent `InsectGenusName genusName`):

```java
public record InsectGenus(
    InsectGenusName name,
    InsectFamilyName familyName,
    InsectOrderName orderName,
    TaxonomicGenus genus,
    Description description,
    Set<CommonName> commonNames,
    @Nullable Clade placedIn,
    @Nullable EggStage egg,
    @Nullable LarvaStage larva,
    @Nullable PupaStage pupa,
    @Nullable AdultStage adult
)
```

Invariants add `.entityName(orderName, "orderName")`. No `TaxonomicOrder` local
copy -- the order epithet is resolved through the parent chain when needed.

### InsectOrderAggregate

New permit on `InsectAggregate`:

```java
public record InsectOrderAggregate(
    InsectOrder order,
    ImageCollection images
) implements InsectAggregate {

    public static InsectOrderAggregate of(InsectOrder order, ImageCollection images) { ... }
    public static InsectOrderAggregate of(InsectOrder order) { ... }

    @Override
    public InsectRankName name() { return order.name(); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i.namedEntity(order, "order").observable(images, "images");
    }
}
```

### InsectAggregate sealed interface

Add fourth permit:

```java
public sealed interface InsectAggregate extends Aggregate
    permits InsectOrderAggregate, InsectFamilyAggregate,
            InsectGenusAggregate, InsectSpeciesAggregate { ... }
```

### InsectAggregateFactory

Add `InsectOrderName` dispatch case and inject `OrderQuery`:

```java
case InsectOrderName on -> {
    var order = orders.getByName(on);
    yield order.map(o -> InsectOrderAggregate.of(
        o, imageQuery.forParentName(o.name())));
}
```

### OrderCollection (InsectEntityCollections)

New `BehavioralCollection` subclass:

```java
public static final class OrderCollection extends BehavioralCollection<InsectOrder> {
    OrderCollection(List<InsectOrder> items) { super(items); }
    public static OrderCollection of(List<InsectOrder> items) { ... }
    public static OrderCollection empty() { ... }
}
```

### InsectQuery additions

New nested `OrderQuery`:

```java
interface OrderQuery extends EntityQuery<InsectOrderName, InsectOrder, OrderCollection> {
}
```

`FamilyQuery` gains:

```java
FamilyCollection forOrderName(InsectOrderName orderName);
```

`GenusQuery` gains:

```java
GenusCollection forOrderName(InsectOrderName orderName);
```

### InsectRepository additions

New nested `OrderRepository`:

```java
protected interface OrderRepository
    extends EntityRepository<InsectOrderName, InsectOrder> {
}
```

`FamilyRepository` gains:

```java
List<InsectFamily> getByOrderName(InsectOrderName orderName);
```

`GenusRepository` gains:

```java
List<InsectGenus> getByOrderName(InsectOrderName orderName);
```

### Query and repository implementations

- `OrderQueryImpl` in `insects-core` -- thin adapter, delegates to repository.
- `OrderRepositoryMock` in `insects-repository-test` -- in-memory mock.
- `FamilyQueryImpl` updated with `forOrderName()` delegation.
- `FamilyRepositoryMock` updated with `getByOrderName()` filtering.
- `GenusQueryImpl` updated with `forOrderName()` delegation.
- `GenusRepositoryMock` updated with `getByOrderName()` filtering.
- `InsectAggregateFactory` updated with `OrderQuery` injection and dispatch.
- `InsectQueryImpl` updated to expose `OrderQuery`.
- `InsectsTestContext` updated to wire `OrderRepositoryMock`, `OrderQueryImpl`.

### Repository behavioral contract

`InsectOrderRepositoryContractTest` in `insects-repository-test`:
- Extends `NamedEntityRepositoryContractTest<InsectOrderName, InsectOrder>`
- Hooks: `repository()`, `source()`, `notFoundName()`, `knownEntityNames()`,
  `newEntity()`, `ghostEntity()`, `modifiedEntity()`
- Standard three-case tests per select/write method

`InsectFamilyRepositoryContractTest` updated:
- `newEntity()` and `ghostEntity()` gain `InsectOrderName orderName` component
- `modifiedEntity()` drops `TaxonomicOrder order` mutation, adds `orderName` FK
  pointing to an order that exists in test data

`InsectGenusRepositoryContractTest` updated:
- `newEntity()` and `ghostEntity()` gain `InsectOrderName orderName` component
- `getByOrderName()` contract tests added

### Jackson: @JsonSubTypes on InsectImage and InsectFunctionalRole

Both records' `parentName` field gains `InsectOrderName` in `@JsonSubTypes`:

```java
@JsonSubTypes({
    @Type(value = InsectOrderName.class,      name = "ORDER"),
    @Type(value = InsectFamilyName.class,     name = "FAMILY"),
    @Type(value = InsectGenusName.class,      name = "GENUS"),
    @Type(value = InsectSpeciesName.class,    name = "SPECIES"),
    @Type(value = InsectSubspeciesName.class,  name = "SUBSPECIES")
})
InsectRankName parentName
```

### Console: order pages

Two new endpoints in `InsectsController`:

**`GET /insects/orders`** -- order listing page:
- Paginated `OrderQuery.findPage()`
- Template: `insects/orders.jte`

**`GET /insects/orders/{name}`** -- order detail page:
- Resolves `InsectOrder` by name
- Lists families under this order via `FamilyQuery.forOrderName()`
- Renders four-level Durrell description
- Template: `insects/order.jte`

**Family detail page update** (`GET /insects/families/{name}`):
- Resolve parent `InsectOrder` via `family.orderName()`
- Template gains upward navigation link to `/insects/orders/{orderName}`

**Genus detail page update** (`GET /insects/genera/{name}`):
- Resolve parent `InsectFamily` via `genus.familyName()` for display
- Family entity provides path to order for breadcrumb navigation

### InsectsCatalogContribution

Add order-level token emission:

```java
private Stream<SearchableEntity> orderEntities() {
    return Pages.stream(ASSEMBLY_PAGE_SIZE, orders::findPage)
            .map(InsectsCatalogContribution::toSearchableOrder);
}

private static Stream<String> tokensFor(InsectOrder entity) {
    Stream.Builder<String> tokens = Stream.builder();
    tokens.add(entity.name().value());          // "diptera"
    tokens.add(entity.order().value());         // "Diptera"
    entity.commonNames().forEach(cn -> tokens.add(cn.label()));
    return tokens.build();
}
```

Constructor gains `InsectQuery.OrderQuery orders` parameter.
`searchableEntities()` adds `orderEntities()` to the flat-mapped stream.

### JSON catalog: insect-orders.json

New file at `insects-repository-test/src/main/resources/insects/insect-orders.json`.
7 order records for the orders currently represented in the catalog:

| Order        | Families in catalog                                               |
|-------------|------------------------------------------------------------------|
| Diptera      | Tachinidae, Syrphidae, Tipulidae                                 |
| Hymenoptera  | Braconidae, Halictidae, Andrenidae, Apidae                       |
| Coleoptera   | Carabidae, Coccinellidae                                         |
| Lepidoptera  | Hesperiidae, Papilionidae, Nymphalidae, Pieridae                 |
| Neuroptera   | Chrysopidae                                                      |
| Hemiptera    | Cicadellidae                                                     |
| Blattodea    | Ectobiidae                                                       |

Each record: `name` (slug), `order` (epithet), `description` (four-level Durrell),
`commonNames`, `placedIn` (nullable Clade slug).

### JSON catalog: insect-families.json updates

Replace `"order": "Diptera"` with `"orderName": "diptera"` on every family record:

Before:
```json
{
  "name": "tachinidae",
  "order": "Diptera",
  "family": "Tachinidae",
  ...
}
```

After:
```json
{
  "name": "tachinidae",
  "orderName": "diptera",
  "family": "Tachinidae",
  ...
}
```

### JSON catalog: insect-genera.json updates

Add `"orderName"` field to every genus record (grandparent FK). The `"order"` and
`"family"` fields were already removed in Phase 1:

Before (after Phase 1):
```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "genus": "Halictus",
  ...
}
```

After:
```json
{
  "name": "halictus",
  "familyName": "halictidae",
  "orderName": "hymenoptera",
  "genus": "Halictus",
  ...
}
```

### TestEntitySource additions

`InsectOrderTestEntitySource` in `insects-repository-test`:
- Loads from `insects/insect-orders.json`
- No foreign key constraints (order is the root)

`InsectFamilyTestEntitySource` updated:
- Declares foreign key constraint to `InsectOrderTestEntitySource` via `orderName`

`InsectGenusTestEntitySource` updated:
- Declares foreign key constraint to `InsectOrderTestEntitySource` via `orderName`
  (in addition to existing `InsectFamilyTestEntitySource` constraint via `familyName`)

### TestInsectsIdentifiers additions

New `InsectOrder` scope with constants for at least two known orders:

```java
public static class Diptera {
    public static final InsectOrderName name = InsectOrderName.of("diptera");
}
public static class Hymenoptera {
    public static final InsectOrderName name = InsectOrderName.of("hymenoptera");
}
```

`NotFound` gains:
```java
public static final InsectOrderName orderName = InsectOrderName.of("zygentoma");
```

### InsectsTestContext

Updated to instantiate `OrderRepositoryMock` and wire `OrderQueryImpl`. The
`create()` factory gains the order source and repository in its assembly chain.
`NaturalistDatabase` instantiates `InsectOrderTestEntitySource` before
`InsectFamilyTestEntitySource` (respects FK ordering).

---

## Hierarchy summary after both phases

```
InsectOrder        name: InsectOrderName
                   order: TaxonomicOrder (own epithet)
    |
    v
InsectFamily       name: InsectFamilyName
                   orderName: InsectOrderName (parent FK)
                   family: TaxonomicFamily (own epithet)
    |
    v
InsectGenus        name: InsectGenusName
                   familyName: InsectFamilyName (parent FK)
                   orderName: InsectOrderName (grandparent FK)
                   genus: TaxonomicGenus (own epithet)
    |
    v
InsectSpecies      name: InsectSpeciesName
                   genusName: InsectGenusName (parent FK)
                   familyName: InsectFamilyName (grandparent FK)
                   epithet: TaxonomicSpecies (own epithet)
```

Each rank carries: own `EntityName` identity + typed FKs to parent/grandparent +
only its own `Taxonomic*` epithet. No local copies of ancestor epithets.

## Slice decomposition

Both phases should be broken into PRs per the project's size discipline
(target <=400 lines of meaningful diff). Suggested slicing:

**Phase 1:**
1. LinnaeanGenus interface change + InsectGenus record refactor + invariants +
   with* methods + JSON migration + tests

**Phase 2:**
1. InsectOrderName + InsectRankName permit update + LinnaeanOrder interface +
   TaxonomicSlugs.orderSlug
2. InsectOrder entity + InsectOrderAggregate + InsectAggregate permit update +
   InsectOrderTest
3. InsectFamily refactor (drop TaxonomicOrder, add InsectOrderName) +
   LinnaeanFamily evolution + InsectFamilyTest update
4. InsectGenus update (add InsectOrderName) + InsectGenusTest update
5. TestInsectsIdentifiers + InsectOrderTestEntitySource + insect-orders.json +
   insect-families.json migration + insect-genera.json migration
6. Repository mock + behavioral contract tests for InsectOrder
7. OrderQuery + OrderCollection + FamilyQuery.forOrderName +
   GenusQuery.forOrderName + query implementations
8. InsectAggregateFactory update + JsonSubTypes on InsectImage/InsectFunctionalRole
9. Console: order list/detail pages + family detail upward nav
10. InsectsCatalogContribution: order token emission
