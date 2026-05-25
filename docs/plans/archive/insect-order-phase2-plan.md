# InsectOrder Phase 2 — Introduce Order-rank entity

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce `InsectOrder` as a first-class entity, refactor `InsectFamily` and `InsectGenus` to use typed `InsectOrderName` FKs, and add order-level console pages.

**Architecture:** Build the InsectOrder entity stack bottom-up (identifiers → entity → test data → repository → query), then refactor InsectFamily and InsectGenus to carry typed `InsectOrderName` references instead of `TaxonomicOrder` local copies. Console templates resolve parent order entities for display. Catalog contribution emits order-level search tokens.

**Tech Stack:** Java 21 records, Jackson 2.19, JTE templates, Spring MVC, AssertJ

**Design spec:** [`docs/plans/insect-order-design.md`](insect-order-design.md)

---

### Task 1: InsectOrderName + InsectRankName + LinnaeanOrder + TaxonomicSlugs

**Files:**
- Create: `domains/identifiers/src/main/java/com/naturalist/insects/InsectOrderName.java`
- Modify: `domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java`
- Create: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanOrder.java`
- Modify: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/TaxonomicSlugs.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAggregateFactory.java`

- [ ] **Step 1: Create `InsectOrderName`**

```java
package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@code InsectOrder} entities.
 * <p>
 * The slug is the lowercased Linnaean order epithet —
 * {@code "diptera"}, {@code "hymenoptera"}, {@code "lepidoptera"}.
 * Single word, no hyphens — order is the topmost rank within
 * Class Insecta.
 */
public final class InsectOrderName extends EntityName implements InsectRankName {

    private InsectOrderName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectOrderName of(String value) {
        return new InsectOrderName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }

    @Override
    public LinealRank rank() {
        return LinealRank.ORDER;
    }
}
```

- [ ] **Step 2: Add `InsectOrderName` to `InsectRankName` sealed permits**

In `InsectRankName.java`, update the sealed permits list and Javadoc:

Change:
```java
public sealed interface InsectRankName
        permits InsectFamilyName, InsectGenusName, InsectSpeciesName, InsectSubspeciesName {
```

To:
```java
public sealed interface InsectRankName
        permits InsectOrderName, InsectFamilyName, InsectGenusName, InsectSpeciesName, InsectSubspeciesName {
```

Update the Javadoc comment (line 6) to include `InsectOrderName` in the enumeration.

- [ ] **Step 3: Create `LinnaeanOrder` kernel interface**

```java
package com.naturalist.taxonomy;

/**
 * Contract for catalog entities that represent an order-rank Linnaean taxon.
 * <p>
 * The contract exposes the order epithet, which is non-null at the
 * implementing entity level. The order slug is derived mechanically
 * from the epithet.
 *
 * <p>Implemented by per-domain order entities (e.g.
 * {@code com.naturalist.insects.InsectOrder}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<ORDER_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 */
public interface LinnaeanOrder {

    TaxonomicOrder order();

    /**
     * The order slug derived from the order epithet — lowercase form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String orderSlug() {
        return TaxonomicSlugs.orderSlug(order());
    }
}
```

- [ ] **Step 4: Add `orderSlug` to `TaxonomicSlugs`**

In `TaxonomicSlugs.java`, add after the `genusSlug` method (after line 31):

```java
    static String orderSlug(TaxonomicOrder order) {
        Objects.requireNonNull(order, "order");
        return kebab(order.value());
    }
```

- [ ] **Step 5: Add `InsectOrderName` case to `InsectAggregateFactory` switch**

In `InsectAggregateFactory.java`, the switch expression (lines 57-68) must remain exhaustive after the new sealed permit. Add a case before the `InsectSubspeciesName` case:

```java
            case InsectOrderName orderName -> Optional.empty();
```

The full switch becomes:
```java
        return switch (name) {
            case InsectSpeciesName speciesName -> speciesQuery.getByName(speciesName)
                    .map(species -> observe(new InsectSpeciesAggregate(
                            species, imageQuery.forParentName(species.name()))));
            case InsectGenusName genusName -> genusQuery.getByName(genusName)
                    .map(genus -> observe(new InsectGenusAggregate(
                            genus, imageQuery.forParentName(genus.name()))));
            case InsectFamilyName familyName -> familyQuery.getByName(familyName)
                    .map(family -> observe(new InsectFamilyAggregate(
                            family, imageQuery.forParentName(family.name()))));
            case InsectOrderName orderName -> Optional.empty();
            case InsectSubspeciesName subspeciesName -> Optional.empty();
        };
```

- [ ] **Step 6: Stage changes**

```bash
git add domains/identifiers/src/main/java/com/naturalist/insects/InsectOrderName.java
git add domains/identifiers/src/main/java/com/naturalist/insects/InsectRankName.java
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanOrder.java
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/TaxonomicSlugs.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAggregateFactory.java
```

---

### Task 2: InsectOrder entity + InsectOrderTest

**Files:**
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrder.java`
- Create: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectOrderTest.java`

- [ ] **Step 1: Create `InsectOrder` entity record**

```java
package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.ddd.NamedEntity;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.lifestage.AdultStage;
import com.naturalist.insects.lifestage.EggStage;
import com.naturalist.insects.lifestage.LarvaStage;
import com.naturalist.insects.lifestage.PupaStage;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.LinnaeanOrder;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A catalogued insect order — the topmost rank within Class Insecta.
 * <p>
 * Order-rank records are first-class catalog citizens. A naturalist who
 * recognises a dipteran without resolving the family has a permanent home
 * for that observation here. As identification firms, an {@link InsectFamily}
 * record is added alongside this order record; the order record is never
 * replaced or migrated.
 * <p>
 * Order is the root of the hierarchy within the insects domain — it carries
 * no parent FK. {@link #order} is the proper-cased Linnaean epithet
 * (e.g., {@code "Diptera"}). The slug identity is derived mechanically
 * from the epithet via {@link LinnaeanOrder#orderSlug()}.
 */
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
) implements NamedEntity<InsectOrderName>, LinnaeanOrder {

    public InsectOrder withPlacedIn(@Nullable Clade value) {
        return new InsectOrder(name, order, description, commonNames,
                value, egg, larva, pupa, adult);
    }

    public InsectOrder withEgg(@Nullable EggStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, value, larva, pupa, adult);
    }

    public InsectOrder withLarva(@Nullable LarvaStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, value, pupa, adult);
    }

    public InsectOrder withPupa(@Nullable PupaStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, larva, value, adult);
    }

    public InsectOrder withAdult(@Nullable AdultStage value) {
        return new InsectOrder(name, order, description, commonNames,
                placedIn, egg, larva, pupa, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .namedValue(order, "order")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames")
                .namedEntityOrNull(egg, "egg")
                .namedEntityOrNull(larva, "larva")
                .namedEntityOrNull(pupa, "pupa")
                .namedEntityOrNull(adult, "adult");
    }
}
```

- [ ] **Step 2: Create `InsectOrderTest`**

```java
package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Holometabola;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectOrderTest {
    private static final Observer observer = Observer.forClass(InsectOrderTest.class);

    @Test
    void validOrderPassesAllInvariants() {
        var mo = observer.forMethod("validOrderPassesAllInvariants");
        InsectOrder order = new InsectOrder(
                InsectOrderName.of("diptera"),
                TaxonomicOrder.of("Diptera"),
                description(),
                Set.of(),
                null,
                null, null, null, null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedInvariantViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedInvariantViolations");
        InsectOrder order = new InsectOrder(
                null, null, null, null, null,
                null, null, null, null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".order.name",
                        ".order.order",
                        ".order.description",
                        ".order.commonNames");
    }

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectOrder order = orderWithPlacedIn(null);

        InsectOrder updated = order.withPlacedIn(new Holometabola());

        assertThat(updated.placedIn()).isEqualTo(new Holometabola());
        assertThat(order.placedIn()).isNull();
    }

    @Test
    void withEggPreservesPlacedIn() {
        InsectOrder order = orderWithPlacedIn(new Holometabola());

        InsectOrder updated = order.withEgg(null);

        assertThat(updated.placedIn()).isEqualTo(new Holometabola());
    }

    private static InsectOrder orderWithPlacedIn(Clade placedIn) {
        return new InsectOrder(
                InsectOrderName.of("diptera"),
                TaxonomicOrder.of("Diptera"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
```

- [ ] **Step 3: Stage changes**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrder.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectOrderTest.java
```

---

### Task 3: Test identifiers + InsectOrderTestEntitySource + insect-orders.json

**Files:**
- Modify: `domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectOrderTestEntitySource.java`
- Create: `domains/insects/insects-repository-test/src/main/resources/insects/insect-orders.json`

- [ ] **Step 1: Add `InsectOrder` scope to `TestInsectsIdentifiers`**

Add the following class BEFORE the existing `InsectFamily` class (around line 23), so rank order flows top-down:

```java
    public static class InsectOrder {

        private InsectOrder() {
        }

        /**
         * Fictitious identifier for the {@link com.naturalist.insects.InsectOrder}
         * scope — guaranteed absent from any catalog.
         */
        public static class NotFound {
            public static final InsectOrderName name = InsectOrderName.of("zygentoma");
        }

        public static class Diptera {
            public static final InsectOrderName name = InsectOrderName.of("diptera");
        }

        public static class Hymenoptera {
            public static final InsectOrderName name = InsectOrderName.of("hymenoptera");
        }
    }
```

- [ ] **Step 2: Create `InsectOrderTestEntitySource`**

```java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class InsectOrderTestEntitySource extends TestEntitySource<InsectOrderName, InsectOrder> {

    public InsectOrderTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-orders.json");
    }
}
```

No foreign key constraints — order is the root rank.

- [ ] **Step 3: Create `insect-orders.json`**

Create a JSON file with 7 order records. Each record has: `name` (slug), `order` (proper-cased epithet), `description` (four-level Durrell), `commonNames`, and optionally `placedIn`.

The 7 orders in the current catalog:

| Order | Slug | Families | placedIn |
|-------|------|----------|----------|
| Diptera | diptera | Tachinidae, Syrphidae, Tipulidae | holometabola |
| Hymenoptera | hymenoptera | Braconidae, Halictidae, Andrenidae, Apidae | holometabola |
| Coleoptera | coleoptera | Carabidae, Coccinellidae | holometabola |
| Lepidoptera | lepidoptera | Hesperiidae, Papilionidae, Nymphalidae, Pieridae | lepidoptera |
| Neuroptera | neuroptera | Chrysopidae | holometabola |
| Hemiptera | hemiptera | Cicadellidae | hemiptera |
| Blattodea | blattodea | Ectobiidae | (null) |

Write real, ecologically accurate four-level Durrell descriptions. Follow the existing style in `insect-families.json`:
- **preschool**: 1-2 sentences, simple language, relatable to a child's world
- **elementary**: 3-4 sentences, more detail, explains what the organisms do in the garden
- **secondary**: 5-7 sentences, uses scientific terminology, explains ecological roles
- **university**: 7-10 sentences, full scientific depth, cites specific behaviors and mechanisms

Example structure for one record:
```json
{
  "name": "diptera",
  "order": "Diptera",
  "description": {
    "preschool": "...",
    "elementary": "...",
    "secondary": "...",
    "university": "..."
  },
  "commonNames": [
    { "label": "true flies", "locale": "en" }
  ],
  "placedIn": "holometabola"
}
```

Common names per order:
- Diptera: `"true flies"`
- Hymenoptera: `"ants, bees, and wasps"`
- Coleoptera: `"beetles"`
- Lepidoptera: `"butterflies and moths"`
- Neuroptera: `"net-winged insects"` or `"lacewings"`
- Hemiptera: `"true bugs"`
- Blattodea: `"cockroaches"`

- [ ] **Step 4: Stage changes**

```bash
git add domains/identifiers-test/src/main/java/com/naturalist/insects/TestInsectsIdentifiers.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectOrderTestEntitySource.java
git add domains/insects/insects-repository-test/src/main/resources/insects/insect-orders.json
```

---

### Task 4: OrderRepository + OrderRepositoryMock + OrderRepositoryTest

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/OrderRepositoryMock.java`
- Create: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/OrderRepositoryTest.java`

- [ ] **Step 1: Add `OrderRepository` to `InsectRepository`**

Add the field, update the constructor, update `create`, add accessor, and add the interface.

Add field (after line 32):
```java
    final OrderRepository orderRepository;
```

Update private constructor signature to accept `OrderRepository`:
```java
    private InsectRepository(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository) {
```

Add `this.orderRepository = orderRepository;` to the constructor body.

Update `create` factory to accept and pass `OrderRepository`:
```java
    static InsectRepository create(
            SpeciesRepository speciesRepository,
            ImageRepository imageRepository,
            FamilyRepository familyRepository,
            GenusRepository genusRepository,
            FunctionalRoleRepository functionalRoleRepository,
            OrderRepository orderRepository) {
        return new InsectRepository(speciesRepository, imageRepository, familyRepository,
                genusRepository, functionalRoleRepository, orderRepository);
    }
```

Add accessor (after `functionalRoleRepository()` method):
```java
    OrderRepository orderRepository() {
        return orderRepository;
    }
```

Add interface (after `FunctionalRoleRepository` interface, before closing brace):
```java
    protected interface OrderRepository
            extends EntityRepository<InsectOrderName, InsectOrder> {
    }
```

- [ ] **Step 2: Create `OrderRepositoryMock`**

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

@DomainService
class OrderRepositoryMock
        extends AbstractTestEntityRepository<InsectOrderName, InsectOrder, InsectOrderTestEntitySource>
        implements InsectRepository.OrderRepository {

    OrderRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }
}
```

- [ ] **Step 3: Create `OrderRepositoryTest`**

```java
package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link InsectRepository.OrderRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectOrder-specific identity constants and entity construction.
 */
interface OrderRepositoryTest
        extends EntityRepositoryTest<InsectOrderName, InsectOrder> {

    @Override
    InsectRepository.OrderRepository repository();

    @Override
    default TestEntitySource<InsectOrderName, InsectOrder> source() {
        return db.getNamed(InsectOrderTestEntitySource.class);
    }

    @Override
    default InsectOrderName notFoundName() {
        return TestInsectsIdentifiers.InsectOrder.NotFound.name;
    }

    @Override
    default List<InsectOrderName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectOrder.Diptera.name,
                TestInsectsIdentifiers.InsectOrder.Hymenoptera.name);
    }

    @Override
    default InsectOrder newEntity() {
        return new InsectOrder(
                InsectOrderName.of("test-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }

    @Override
    default InsectOrder ghostEntity() {
        return new InsectOrder(
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }

    @Override
    default InsectOrder modifiedEntity(InsectOrder original) {
        return new InsectOrder(
                original.name(),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null, null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
```

- [ ] **Step 4: Update `InsectsTestContext` — pass `OrderRepositoryMock` to `InsectRepository.create`**

In `InsectsTestContext.java`, update the `InsectRepository.create(...)` call (lines 37-42) to include the order mock:

```java
        InsectRepository repository = InsectRepository.create(
                new SpeciesRepositoryMock(db),
                new InsectImageRepositoryMock(db),
                new FamilyRepositoryMock(db),
                new GenusRepositoryMock(db),
                new InsectFunctionalRoleRepositoryMock(db),
                new OrderRepositoryMock(db));
```

This is just the repository creation — query wiring comes in Task 5.

- [ ] **Step 5: Stage changes**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/OrderRepositoryMock.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/OrderRepositoryTest.java
git add domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
```

---

### Task 5: OrderCollection + OrderQuery + OrderQueryImpl + wiring

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/OrderQueryImpl.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java`
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`

- [ ] **Step 1: Add `OrderCollection` to `InsectEntityCollections`**

Add after `GenusCollection` (after line 85):

```java
    final class OrderCollection extends BehavioralCollection<InsectOrder> {

        OrderCollection(Collection<InsectOrder> orders) {
            super(orders);
        }

        public static OrderCollection of(Collection<InsectOrder> orders) {
            return new OrderCollection(orders);
        }

        public static OrderCollection empty() {
            return new OrderCollection(List.of());
        }
    }
```

Add the import `InsectEntityCollections.OrderCollection` reference in the Javadoc `<ul>` list.

- [ ] **Step 2: Add `OrderQuery` and `orders()` accessor to `InsectQuery`**

Add accessor method (after the `functionalRoles()` method, around line 57):
```java
    OrderQuery orders();
```

Add the `OrderQuery` interface (before the closing brace of `InsectQuery`):
```java
    interface OrderQuery extends EntityQuery<InsectOrderName, InsectOrder, InsectEntityCollections.OrderCollection> {
    }
```

Add the import for `InsectEntityCollections.OrderCollection` at the top of the file.

- [ ] **Step 3: Create `OrderQueryImpl`**

```java
package com.naturalist.insects;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.insects.InsectEntityCollections.OrderCollection;

import java.util.Set;

@DomainService
class OrderQueryImpl
        extends AbstractEntityQuery<
        InsectOrderName,
        InsectOrder,
        OrderCollection,
        InsectRepository.OrderRepository>
        implements InsectQuery.OrderQuery {

    OrderQueryImpl(InsectRepository.OrderRepository repository) {
        super(repository);
    }

    @Override
    public OrderCollection findByNameSet(Set<InsectOrderName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return OrderCollection.of(repository().getByEntityNameSet(names));
    }
}
```

- [ ] **Step 4: Update `InsectQueryImpl` — add `orderQuery` field and `orders()` accessor**

Add field (after `functionalRoleQuery`, around line 11):
```java
    private final OrderQuery orderQuery;
```

Update constructor signature to accept `OrderQuery`:
```java
    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery) {
```

Add to the Observer validation in the constructor:
```java
                        .notNull(orderQuery, "orderQuery"))
```

Add assignment:
```java
        this.orderQuery = orderQuery;
```

Add accessor method:
```java
    @Override
    public OrderQuery orders() {
        return orderQuery;
    }
```

**Note:** Do NOT update the `InsectAggregateFactory` constructor call yet — that happens in Task 8.

- [ ] **Step 5: Update `InsectsTestContext` — wire OrderQuery**

In `InsectsTestContext.java`, add the order query wiring after the existing query creations:

```java
        InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(repository.orderRepository);
```

Update the `InsectQueryImpl` constructor call to include `orderQuery`:
```java
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery);
```

- [ ] **Step 6: Stage changes**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectEntityCollections.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/OrderQueryImpl.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java
git add domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
```

---

### Task 6: InsectFamily refactor + LinnaeanFamily evolution + templates

This is the largest task. It drops `TaxonomicOrder order` from `InsectFamily`, adds `InsectOrderName orderName`, evolves the `LinnaeanFamily` kernel interface, and updates all 5 JTE templates that previously read `family.order().value()`.

**Files:**
- Modify: `kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanFamily.java`
- Modify: `kernels/taxonomy/src/test/java/com/naturalist/taxonomy/LinnaeanFamilyTest.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-families.json`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectFamilyTestEntitySource.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryMock.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/FamilyQueryImpl.java`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Modify: `domains/insects/insects-console/src/main/jte/insects/families.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/family.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/life-stages.jte`
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java`
- Modify: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java`

- [ ] **Step 1: Evolve `LinnaeanFamily` — add generic parameter and `orderName()` method**

Replace the entire `LinnaeanFamily.java`:

```java
package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;

/**
 * Contract for catalog entities that represent a family-rank Linnaean taxon.
 * <p>
 * The contract exposes the upward typed reference to the parent order
 * and the family epithet, which is non-null at the implementing entity
 * level. The family slug is derived mechanically from the epithet.
 *
 * <p>Implemented by per-domain family entities (e.g.
 * {@code com.naturalist.insects.InsectFamily}). The implementing entity
 * supplies its own typed name via its {@code NamedEntity<FAMILY_NAME>}
 * binding; this interface is concerned only with the rank-level contract.
 *
 * @param <ORDER_NAME> the parent order entity's typed name
 */
public interface LinnaeanFamily<ORDER_NAME extends EntityName> {

    ORDER_NAME orderName();

    TaxonomicFamily family();

    /**
     * The family slug derived from the family epithet — lowercase kebab form.
     * The slug is {@code identity}; common names are findable but not
     * authoritative.
     */
    default String familySlug() {
        return TaxonomicSlugs.familySlug(family());
    }
}
```

- [ ] **Step 2: Update `LinnaeanFamilyTest`**

Replace the entire `LinnaeanFamilyTest.java`:

```java
package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanFamilyTest {

    @Test
    void familySlugDerivesFromFamilyEpithet() {
        LinnaeanFamily<TestOrderName> tachinids = family("diptera", "Tachinidae");
        assertThat(tachinids.familySlug()).isEqualTo("tachinidae");
    }

    @Test
    void familySlugLowerCasesAndKebabsTheEpithet() {
        LinnaeanFamily<TestOrderName> syrphids = family("diptera", "Syrphidae");
        assertThat(syrphids.familySlug()).isEqualTo("syrphidae");
    }

    @Test
    void familySlugCollapsesInternalWhitespaceToHyphens() {
        LinnaeanFamily<TestOrderName> oddity = family("diptera", "Family name");
        assertThat(oddity.familySlug()).isEqualTo("family-name");
    }

    @Test
    void familySlugRejectsNullFamily() {
        LinnaeanFamily<TestOrderName> broken = new TestFamily(new TestOrderName("diptera"), null);
        assertThatThrownBy(broken::familySlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void orderNameIsCarriedAsTheUpwardTypedReference() {
        LinnaeanFamily<TestOrderName> tachinids = family("diptera", "Tachinidae");
        assertThat(tachinids.orderName()).isEqualTo(new TestOrderName("diptera"));
    }

    private static LinnaeanFamily<TestOrderName> family(String orderSlug, String familyEpithet) {
        return new TestFamily(
                new TestOrderName(orderSlug),
                familyEpithet == null ? null : new TaxonomicFamily(familyEpithet));
    }

    private record TestFamily(
            TestOrderName orderName,
            TaxonomicFamily family
    ) implements LinnaeanFamily<TestOrderName> {
    }

    private static final class TestOrderName extends EntityName {
        TestOrderName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }
}
```

- [ ] **Step 3: Refactor `InsectFamily` — drop `TaxonomicOrder order`, add `InsectOrderName orderName`**

Replace the record signature and update the `implements` clause:

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
) implements NamedEntity<InsectFamilyName>, LinnaeanFamily<InsectOrderName> {
```

Remove the `TaxonomicOrder` import, add `InsectOrderName` import (already in same package — no import needed).

Update invariants — replace `.namedValue(order, "order")` with `.entityName(orderName, "orderName")`:

```java
    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .entityName(orderName, "orderName")
                .namedValue(family, "family")
                .valueObject(description, "description")
                .notNull(commonNames, "commonNames")
                .namedEntityOrNull(egg, "egg")
                .namedEntityOrNull(larva, "larva")
                .namedEntityOrNull(pupa, "pupa")
                .namedEntityOrNull(adult, "adult");
    }
```

Update ALL five `with*` methods — replace `order` with `orderName` in constructor calls. Example:

```java
    public InsectFamily withPlacedIn(@Nullable Clade value) {
        return new InsectFamily(name, orderName, family, description, commonNames,
                value, egg, larva, pupa, adult);
    }
```

Apply the same pattern to `withEgg`, `withLarva`, `withPupa`, `withAdult`. The `withEgg` method has observer validation — preserve that, just update the constructor call.

- [ ] **Step 4: Update `InsectFamilyTest`**

Update every `new InsectFamily(...)` call — replace `TaxonomicOrder.of("...")` with `InsectOrderName.of("...")`.

In the valid test (line 44-54):
```java
        InsectFamily family = new InsectFamily(
                name,
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                adult);
```

In `withAdultReturnsNewInstanceCarryingTheStage` (lines 68-78):
```java
        InsectFamily family = new InsectFamily(
                name,
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null,
                null,
                null,
                null,
                null);
```

In `familyWithPlacedIn` helper (lines 115-127):
```java
    private static InsectFamily familyWithPlacedIn(Clade placedIn) {
        return new InsectFamily(
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                placedIn,
                null,
                null,
                null,
                null);
    }
```

Remove unused `import com.naturalist.taxonomy.TaxonomicOrder;`.

- [ ] **Step 5: Migrate `insect-families.json` — replace `"order"` with `"orderName"`**

In every record, replace the `"order": "Diptera"` line with `"orderName": "diptera"` (slug form matching `insect-orders.json` name values).

Mapping:
| Family | Old: `"order"` | New: `"orderName"` |
|--------|---------------|-------------------|
| tachinidae | `"Diptera"` | `"diptera"` |
| braconidae | `"Hymenoptera"` | `"hymenoptera"` |
| syrphidae | `"Diptera"` | `"diptera"` |
| carabidae | `"Coleoptera"` | `"coleoptera"` |
| tipulidae | `"Diptera"` | `"diptera"` |
| hesperiidae | `"Lepidoptera"` | `"lepidoptera"` |
| halictidae | `"Hymenoptera"` | `"hymenoptera"` |
| andrenidae | `"Hymenoptera"` | `"hymenoptera"` |
| chrysopidae | `"Neuroptera"` | `"neuroptera"` |
| cicadellidae | `"Hemiptera"` | `"hemiptera"` |
| papilionidae | `"Lepidoptera"` | `"lepidoptera"` |
| coccinellidae | `"Coleoptera"` | `"coleoptera"` |
| ectobiidae | `"Blattodea"` | `"blattodea"` |
| apidae | `"Hymenoptera"` | `"hymenoptera"` |
| nymphalidae | `"Lepidoptera"` | `"lepidoptera"` |
| pieridae | `"Lepidoptera"` | `"lepidoptera"` |

- [ ] **Step 6: Update `InsectFamilyTestEntitySource` — add FK constraint to order**

```java
package com.naturalist.insects;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class InsectFamilyTestEntitySource extends TestEntitySource<InsectFamilyName, InsectFamily> {

    public InsectFamilyTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-families.json");
    }

    @Override
    protected List<ForeignKeyConstraint<InsectFamily, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of(
                "orderName",
                InsectFamily::orderName,
                InsectOrderTestEntitySource.class));
    }
}
```

- [ ] **Step 7: Update `FamilyRepositoryTest` — update entity builders**

In `newEntity()`:
```java
    @Override
    default InsectFamily newEntity() {
        return new InsectFamily(
                InsectFamilyName.of("test-family-xx"),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }
```

In `ghostEntity()`:
```java
    @Override
    default InsectFamily ghostEntity() {
        return new InsectFamily(
                InsectFamilyName.of("test-ghost-xx"),
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }
```

In `modifiedEntity()`:
```java
    @Override
    default InsectFamily modifiedEntity(InsectFamily original) {
        return new InsectFamily(
                original.name(),
                InsectOrderName.of("hymenoptera"),
                TaxonomicFamily.of("Family" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null, null, null, null);
    }
```

Replace the `TaxonomicOrder` import with `InsectOrderName` import (same package — no import needed, but remove the `TaxonomicOrder` import).

- [ ] **Step 8: Add `getByOrderName` to `FamilyRepository` + `FamilyRepositoryMock` + `FamilyQuery` + `FamilyQueryImpl`**

In `InsectRepository.java`, add to the `FamilyRepository` interface:
```java
    protected interface FamilyRepository
            extends EntityRepository<InsectFamilyName, InsectFamily> {

        List<InsectFamily> getByOrderName(InsectOrderName orderName);
    }
```

In `FamilyRepositoryMock.java`:
```java
package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class FamilyRepositoryMock
        extends AbstractTestEntityRepository<InsectFamilyName, InsectFamily, InsectFamilyTestEntitySource>
        implements InsectRepository.FamilyRepository {

    FamilyRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectFamily> getByOrderName(InsectOrderName orderName) {
        observer().arguments("getByOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(f -> orderName.equals(f.orderName()))
                .toList();
    }
}
```

In `InsectQuery.java`, add to `FamilyQuery`:
```java
    interface FamilyQuery extends EntityQuery<InsectFamilyName, InsectFamily, FamilyCollection> {

        FamilyCollection forOrderName(InsectOrderName orderName);
    }
```

In `FamilyQueryImpl.java`, add the implementation:
```java
    @Override
    public FamilyCollection forOrderName(InsectOrderName orderName) {
        observer().arguments("forOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return FamilyCollection.of(repository().getByOrderName(orderName));
    }
```

Add contract tests to `FamilyRepositoryTest` (after `modifiedEntity`):

```java
    @Test
    default void getByOrderName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderName");
    }

    @Test
    default void getByOrderName_returnsFamiliesWithMatchingOrderName() {
        InsectOrderName diptera = InsectOrderName.of("diptera");

        var results = repository().getByOrderName(diptera);

        assertThat(results)
                .extracting(InsectFamily::name)
                .extracting(InsectFamilyName::value)
                .contains("tachinidae");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        InsectOrderName unknown = InsectOrderName.of("zygentoma");

        var results = repository().getByOrderName(unknown);

        assertThat(results).isEmpty();
    }
```

Add required imports to `FamilyRepositoryTest`: `InvariantViolationException`, `org.junit.jupiter.api.Test`, assertion statics. Also add `InsectOrderName` if needed.

- [ ] **Step 9: Update `InsectsController` — resolve parent order for templates**

Every controller method that passes an `InsectFamily` to a template that reads `.order()` must now resolve the parent `InsectOrder` entity and pass it as a model attribute.

**`familyDetail()` method** — resolve order:
```java
    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = InsectFamilyName.of(name);
        var family = insectQuery.families().getByName(familyName);
        if (family.isEmpty()) {
            return "redirect:/insects/families";
        }
        InsectOrder order = insectQuery.orders().getByName(family.get().orderName()).orElseThrow();
        var description = family.get().description();
        var genera = insectQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing(g -> g.name().value()))
                .toList();
        model.addAttribute("family", family.get());
        model.addAttribute("order", order);
        model.addAttribute("genera", genera);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/family";
    }
```

**`families()` method** — build orderByName map:
```java
    @GetMapping("/families")
    String families(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectFamily> familyPage = insectQuery.families()
                .findPage(PageRequest.console(Math.max(0, page)));
        Map<InsectOrderName, InsectOrder> orderByName = new LinkedHashMap<>();
        for (var family : familyPage.content()) {
            orderByName.computeIfAbsent(family.orderName(),
                    n -> insectQuery.orders().getByName(n).orElseThrow());
        }
        model.addAttribute("familyPage", familyPage);
        model.addAttribute("orderByName", orderByName);
        return "insects/families";
    }
```

**`genusDetail()` method** — resolve order through family:
```java
        InsectFamily family = insectQuery.families().getByName(genus.get().familyName()).orElseThrow();
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
```
Add `model.addAttribute("order", order);` after `model.addAttribute("family", family);`.

**`detail()` method** — resolve order through family (after the existing family resolution at line 171):
```java
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
```
Add `model.addAttribute("order", order);`.

**`lifeStages()` method** — resolve order through family:
```java
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
```
Add `model.addAttribute("order", order);`.

Add `InsectOrder` to the controller's imports.

- [ ] **Step 10: Update JTE templates — read order from resolved `InsectOrder` entity**

**`families.jte`:**
Add imports and param:
```jte
@import com.naturalist.insects.InsectOrder
@import com.naturalist.insects.InsectOrderName
@import java.util.Map

@param Map<InsectOrderName, InsectOrder> orderByName
```

Inside the `@for` loop, add lookup:
```jte
            !{var order = orderByName.get(f.orderName());}
```

Replace line 22:
```
${f.order().value()}
```
with:
```
${order.order().value()}
```

**`family.jte`:**
Add import and param:
```jte
@import com.naturalist.insects.InsectOrder

@param InsectOrder order
```

Replace line 15:
```
<p class="taxonomy">${family.order().value()}</p>
```
with:
```
<p class="taxonomy"><a href="/insects/orders/${order.name().value()}">${order.order().value()}</a></p>
```

**`genus.jte`:**
Add import and param:
```jte
@import com.naturalist.insects.InsectOrder

@param InsectOrder order
```

Replace line 18:
```
${family.order().value()} &middot;
```
with:
```
<a href="/insects/orders/${order.name().value()}">${order.order().value()}</a> &middot;
```

**`detail.jte`:**
Add import and param:
```jte
@import com.naturalist.insects.InsectOrder

@param InsectOrder order
```

Replace line 32:
```
${family.order().value()} &middot; ${family.family().value()}
```
with:
```
${order.order().value()} &middot; ${family.family().value()}
```

**`life-stages.jte`:**
Add import and param:
```jte
@import com.naturalist.insects.InsectOrder

@param InsectOrder order
```

Replace line 23:
```
${family.order().value()} &middot; ${family.family().value()}
```
with:
```
${order.order().value()} &middot; ${family.family().value()}
```

- [ ] **Step 11: Update template tests**

**`InsectsFamiliesTemplateTest`** — both test methods need order data.

`families_rendersWithoutError`:
```java
    @Test
    void families_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectFamily> familyPage = new InsectFamilyTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        InsectOrderTestEntitySource orderSource = new InsectOrderTestEntitySource(database);
        Map<InsectOrderName, InsectOrder> orderByName = new LinkedHashMap<>();
        for (var family : familyPage.content()) {
            orderByName.computeIfAbsent(family.orderName(),
                    n -> orderSource.getByName(n).orElseThrow());
        }
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/families.jte",
                Map.of("familyPage", familyPage,
                       "orderByName", orderByName),
                output);

        assertThat(output.toString()).isNotBlank();
    }
```

`family_rendersWithoutError`:
```java
    @Test
    void family_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = new InsectOrderTestEntitySource(database)
                .getByName(anyFamily.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
                        "order", order,
                        "genera", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
```

Add imports: `InsectOrder`, `InsectOrderName`, `InsectOrderTestEntitySource`, `LinkedHashMap`.

**`InsectsGeneraTemplateTest`** — `genus_rendersWithoutError` now needs `order` param:
```java
    @Test
    void genus_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectGenusTestEntitySource genusSource = new InsectGenusTestEntitySource(database);
        InsectGenus anyGenus = genusSource.entityStream().findFirst().orElseThrow();
        InsectFamily family = new InsectFamilyTestEntitySource(database)
                .getByName(anyGenus.familyName()).orElseThrow();
        InsectOrder order = new InsectOrderTestEntitySource(database)
                .getByName(family.orderName()).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/genus.jte",
                Map.of(
                        "genus", anyGenus,
                        "family", family,
                        "order", order,
                        "species", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
```

Add imports: `InsectOrder`, `InsectOrderTestEntitySource`.

- [ ] **Step 12: Stage changes**

```bash
git add kernels/taxonomy/src/main/java/com/naturalist/taxonomy/LinnaeanFamily.java
git add kernels/taxonomy/src/test/java/com/naturalist/taxonomy/LinnaeanFamilyTest.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamily.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectFamilyTest.java
git add domains/insects/insects-repository-test/src/main/resources/insects/insect-families.json
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectFamilyTestEntitySource.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryTest.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/FamilyRepositoryMock.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/FamilyQueryImpl.java
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/main/jte/insects/families.jte
git add domains/insects/insects-console/src/main/jte/insects/family.jte
git add domains/insects/insects-console/src/main/jte/insects/genus.jte
git add domains/insects/insects-console/src/main/jte/insects/detail.jte
git add domains/insects/insects-console/src/main/jte/insects/life-stages.jte
git add domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsFamiliesTemplateTest.java
git add domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsGeneraTemplateTest.java
```

---

### Task 7: InsectGenus update — add `InsectOrderName orderName`

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectGenusTestEntitySource.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java`
- Modify: `domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java`

- [ ] **Step 1: Add `InsectOrderName orderName` to `InsectGenus`**

Insert `InsectOrderName orderName` after `familyName` in the record signature:

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
) implements NamedEntity<InsectGenusName>, LinnaeanGenus<InsectFamilyName> {
```

Add `.entityName(orderName, "orderName")` to `invariants()` after `familyName`.

Update ALL five `with*` methods to include `orderName` in constructor calls:
```java
    public InsectGenus withPlacedIn(@Nullable Clade value) {
        return new InsectGenus(name, familyName, orderName, genus,
                description, commonNames, value, egg, larva, pupa, adult);
    }
```

Apply the same pattern to `withEgg`, `withLarva`, `withPupa`, `withAdult`.

- [ ] **Step 2: Update `InsectGenusTest`**

Update every `new InsectGenus(...)` call — add `InsectOrderName.of("...")` after `familyName`.

In `validGenusWithLarvaPassesAllInvariants` (the valid test):
```java
        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                InsectOrderName.of("neuroptera"),
                TaxonomicGenus.of("Chrysoperla"),
                ...
```

In `withLarvaReturnsNewInstanceCarryingTheStage`:
```java
        InsectGenus genus = new InsectGenus(
                name,
                InsectFamilyName.of("chrysopidae"),
                InsectOrderName.of("neuroptera"),
                TaxonomicGenus.of("Chrysoperla"),
                ...
```

In `genusWithPlacedIn` helper:
```java
    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }
```

- [ ] **Step 3: Update `InsectAggregateTest.validGenus()` and `InsectLifeStagesTest.genusWithPlacedIn()`**

In `InsectAggregateTest`, find the `validGenus()` helper and add `InsectOrderName.of("lepidoptera")` after `InsectFamilyName.of("papilionidae")`.

In `InsectLifeStagesTest`, find `genusWithPlacedIn()` and add `InsectOrderName.of("lepidoptera")` after `InsectFamilyName.of("papilionidae")`.

- [ ] **Step 4: Migrate `insect-genera.json` — add `"orderName"` field**

Add `"orderName"` field to every genus record, placed after `"familyName"`:

| Genus | familyName | orderName |
|-------|-----------|-----------|
| halictus | halictidae | hymenoptera |
| andrena | andrenidae | hymenoptera |
| chrysoperla | chrysopidae | neuroptera |
| empoasca | cicadellidae | hemiptera |
| hippodamia | coccinellidae | coleoptera |
| blattella | ectobiidae | blattodea |
| xylocopa | apidae | hymenoptera |
| vanessa | nymphalidae | lepidoptera |
| battus | papilionidae | lepidoptera |
| colias | pieridae | lepidoptera |

- [ ] **Step 5: Update `InsectGenusTestEntitySource` — add FK constraint to order**

Add the order FK constraint alongside the existing family constraint:

```java
    @Override
    protected List<ForeignKeyConstraint<InsectGenus, ?>> foreignKeyConstraints() {
        return List.of(
                ForeignKeyConstraint.of(
                        "familyName",
                        InsectGenus::familyName,
                        InsectFamilyTestEntitySource.class),
                ForeignKeyConstraint.of(
                        "orderName",
                        InsectGenus::orderName,
                        InsectOrderTestEntitySource.class));
    }
```

- [ ] **Step 6: Update `GenusRepositoryTest` entity builders**

In `newEntity()`:
```java
        return new InsectGenus(
                InsectGenusName.of("test-genus-xx"),
                InsectFamilyName.of("tachinidae"),
                InsectOrderName.of("diptera"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
```

In `ghostEntity()`:
```java
        return new InsectGenus(
                InsectGenusName.of("test-ghost-xx"),
                InsectFamilyName.of("test-ghost-family-xx"),
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(),
                null,
                null, null, null, null);
```

In `modifiedEntity()`:
```java
        return new InsectGenus(
                original.name(),
                InsectFamilyName.of("braconidae"),
                InsectOrderName.of("hymenoptera"),
                TaxonomicGenus.of("Genus" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null,
                null, null, null, null);
```

- [ ] **Step 7: Add `getByOrderName` to `GenusRepository` + mock + query + contract tests**

In `InsectRepository.java`, add to `GenusRepository`:
```java
        List<InsectGenus> getByOrderName(InsectOrderName orderName);
```

In `GenusRepositoryMock.java`, add:
```java
    @Override
    public List<InsectGenus> getByOrderName(InsectOrderName orderName) {
        observer().arguments("getByOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(g -> orderName.equals(g.orderName()))
                .toList();
    }
```

In `InsectQuery.java`, add to `GenusQuery`:
```java
        GenusCollection forOrderName(InsectOrderName orderName);
```

In `GenusQueryImpl.java`, add:
```java
    @Override
    public GenusCollection forOrderName(InsectOrderName orderName) {
        observer().arguments("forOrderName",
                        i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        return GenusCollection.of(repository().getByOrderName(orderName));
    }
```

Add contract tests to `GenusRepositoryTest`:
```java
    @Test
    default void getByOrderName_rejectsNull() {
        assertThatThrownBy(() -> repository().getByOrderName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("orderName");
    }

    @Test
    default void getByOrderName_returnsGeneraWithMatchingOrderName() {
        InsectOrderName hymenoptera = InsectOrderName.of("hymenoptera");

        var results = repository().getByOrderName(hymenoptera);

        assertThat(results)
                .extracting(InsectGenus::name)
                .extracting(InsectGenusName::value)
                .contains("halictus");
    }

    @Test
    default void getByOrderName_returnsEmptyForUnknownOrder() {
        InsectOrderName unknown = InsectOrderName.of("zygentoma");

        var results = repository().getByOrderName(unknown);

        assertThat(results).isEmpty();
    }
```

- [ ] **Step 8: Stage changes**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenus.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectGenusTest.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/lifestage/InsectLifeStagesTest.java
git add domains/insects/insects-repository-test/src/main/resources/insects/insect-genera.json
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/InsectGenusTestEntitySource.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryTest.java
git add domains/insects/insects-repository-test/src/main/java/com/naturalist/insects/GenusRepositoryMock.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectRepository.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/GenusQueryImpl.java
```

---

### Task 8: InsectOrderAggregate + factory + JsonSubTypes

**Files:**
- Create: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrderAggregate.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAggregateFactory.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFunctionalRole.java`

- [ ] **Step 1: Create `InsectOrderAggregate`**

```java
package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * Order-rank {@link InsectAggregate} — used when only order is resolved
 * (e.g. <i>Diptera</i> sp.). Composes the order record with the photographic
 * field record. Root identity is the order's {@link InsectOrderName}.
 */
public record InsectOrderAggregate(
        InsectOrder order,
        ImageCollection images
) implements InsectAggregate {

    public static InsectOrderAggregate of(InsectOrder order, ImageCollection images) {
        return new InsectOrderAggregate(order, images);
    }

    public static InsectOrderAggregate of(InsectOrder order) {
        return new InsectOrderAggregate(order, ImageCollection.empty());
    }

    @Override
    public InsectRankName name() {
        return order.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(order, "order")
                .observable(images, "images");
    }
}
```

- [ ] **Step 2: Update `InsectAggregate` sealed permits**

```java
public sealed interface InsectAggregate extends Aggregate
        permits InsectOrderAggregate, InsectFamilyAggregate, InsectGenusAggregate, InsectSpeciesAggregate {
```

Update the Javadoc to include order-rank in the list.

- [ ] **Step 3: Update `InsectAggregateFactory` — add `OrderQuery`, replace `Optional.empty()`**

Add field:
```java
    private final InsectQuery.OrderQuery orderQuery;
```

Update constructor:
```java
    InsectAggregateFactory(InsectQuery.SpeciesQuery speciesQuery,
                           InsectQuery.ImageQuery imageQuery,
                           InsectQuery.GenusQuery genusQuery,
                           InsectQuery.FamilyQuery familyQuery,
                           InsectQuery.OrderQuery orderQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
    }
```

Replace the `InsectOrderName` case:
```java
            case InsectOrderName on -> orderQuery.getByName(on)
                    .map(order -> observe(new InsectOrderAggregate(
                            order, imageQuery.forParentName(order.name()))));
```

- [ ] **Step 4: Update `InsectQueryImpl` — pass `orderQuery` to factory**

Update the factory creation (line 31-32):
```java
        InsectAggregateFactory factory =
                new InsectAggregateFactory(speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery);
```

- [ ] **Step 5: Add `InsectOrderName` to `@JsonSubTypes` on `InsectImage` and `InsectFunctionalRole`**

In `InsectImage.java`, add to the `@JsonSubTypes` annotation on the `parentName` field:
```java
    @JsonSubTypes({
        @Type(value = InsectOrderName.class, name = "ORDER"),
        @Type(value = InsectFamilyName.class, name = "FAMILY"),
        @Type(value = InsectGenusName.class, name = "GENUS"),
        @Type(value = InsectSpeciesName.class, name = "SPECIES"),
        @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
    })
```

Apply the same change to `InsectFunctionalRole.java`.

- [ ] **Step 6: Update `InsectAggregateTest`**

Add valid and invalid order aggregate test cases, following the existing family/genus patterns:

```java
    @Test
    void orderAggregateIsValid() {
        var mo = observer.forMethod("orderAggregateIsValid");
        InsectOrderAggregate agg = InsectOrderAggregate.of(validOrder());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void orderAggregateIsNotValid() {
        var mo = observer.forMethod("orderAggregateIsNotValid");
        InsectOrderAggregate agg = new InsectOrderAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.order", ".agg.images");
    }
```

Add `validOrder()` helper:
```java
    private static InsectOrder validOrder() {
        return new InsectOrder(
                InsectOrderName.of(RandomValue.string()),
                TaxonomicOrder.of("Diptera"),
                description(),
                Set.of(),
                null,
                null, null, null, null);
    }
```

Add imports: `InsectOrder`, `InsectOrderName`, `InsectOrderAggregate`, `TaxonomicOrder`.

- [ ] **Step 7: Stage changes**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrderAggregate.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectAggregate.java
git add domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectAggregateTest.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectAggregateFactory.java
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectImage.java
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFunctionalRole.java
```

---

### Task 9: Console order pages + nav

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/orders.jte`
- Create: `domains/insects/insects-console/src/main/jte/insects/order.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/nav.jte`
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`
- Create: `domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsOrdersTemplateTest.java`

- [ ] **Step 1: Create `orders.jte` — order listing page**

```jte
@import com.naturalist.insects.InsectOrder
@import com.naturalist.data.Page

@param Page<InsectOrder> orderPage

@template.layout.page(title = "Insect Orders", content = @`
    @template.insects.nav(active = "order")
    <h1>Insect Orders</h1>
    <p>Order-rank records in the Oak Vista catalog.</p>

    <div class="entity-grid">
        @for(var o : orderPage.content())
            !{var commonName = o.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(o.name().value());}
            <article>
                <header>
                    <a href="/insects/orders/${o.name().value()}">
                        <strong>${commonName}</strong>
                    </a>
                </header>
                <dl class="taxonomy">
                    <dt>order</dt>
                    <dd><em>${o.order().value()}</em></dd>
                </dl>
            </article>
        @endfor
    </div>

    @template.components.pager(page = orderPage, baseUrl = "/insects/orders")
`)
```

- [ ] **Step 2: Create `order.jte` — order detail page**

```jte
@import com.naturalist.insects.InsectFamily
@import com.naturalist.insects.InsectOrder
@import java.util.List

@param InsectOrder order
@param List<InsectFamily> families
@param String descriptionPreschool = ""
@param String descriptionElementary = ""
@param String descriptionSecondary = ""
@param String descriptionUniversity = ""

@template.layout.page(title = order.order().value(), content = @`
    @template.insects.nav(active = "order")
    <h1>${order.order().value()}</h1>

    @template.components.description(
        preschool = descriptionPreschool,
        elementary = descriptionElementary,
        secondary = descriptionSecondary,
        university = descriptionUniversity)

    <section>
        <h2>Families in this order</h2>
        @if(families.isEmpty())
            <p><em>No families catalogued under this order yet.</em></p>
        @else
            <div class="entity-grid">
                @for(var f : families)
                    !{var commonName = f.commonNames().stream().findFirst().map(cn -> cn.label()).orElse(f.name().value());}
                    <article>
                        <header>
                            <a href="/insects/families/${f.name().value()}">
                                <strong>${commonName}</strong>
                            </a>
                        </header>
                        <dl class="taxonomy">
                            <dt>family</dt>
                            <dd><em>${f.family().value()}</em></dd>
                        </dl>
                    </article>
                @endfor
            </div>
        @endif
    </section>
`)
```

- [ ] **Step 3: Update `nav.jte` — add Order link**

Add the Order nav link before the Family link:

```jte
<nav class="insects-subnav" aria-label="Insects catalog sub-navigation">
    <a href="/insects/orders" aria-current="${"order".equals(active) ? "page" : "false"}">Order</a>
    <a href="/insects/families" aria-current="${"family".equals(active) ? "page" : "false"}">Family</a>
    <a href="/insects/genera" aria-current="${"genus".equals(active) ? "page" : "false"}">Genus</a>
    <a href="/insects" aria-current="${"species".equals(active) ? "page" : "false"}">Species</a>
</nav>
```

- [ ] **Step 4: Add order endpoints to `InsectsController`**

Add after the `families` section (after the `familyDetail` method):

```java
    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectOrder> orderPage = insectQuery.orders()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("orderPage", orderPage);
        return "insects/orders";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = InsectOrderName.of(name);
        var order = insectQuery.orders().getByName(orderName);
        if (order.isEmpty()) {
            return "redirect:/insects/orders";
        }
        var description = order.get().description();
        var families = insectQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing(f -> f.name().value()))
                .toList();
        model.addAttribute("order", order.get());
        model.addAttribute("families", families);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/order";
    }
```

- [ ] **Step 5: Create `InsectsOrdersTemplateTest`**

```java
package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsOrdersTemplateTest {

    @Test
    void orders_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectOrder> orderPage = new InsectOrderTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/orders.jte",
                Map.of("orderPage", orderPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void order_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrder anyOrder = new InsectOrderTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/order.jte",
                Map.of(
                        "order", anyOrder,
                        "families", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
```

- [ ] **Step 6: Stage changes**

```bash
git add domains/insects/insects-console/src/main/jte/insects/orders.jte
git add domains/insects/insects-console/src/main/jte/insects/order.jte
git add domains/insects/insects-console/src/main/jte/insects/nav.jte
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git add domains/insects/insects-console/src/test/java/com/naturalist/insects/console/InsectsOrdersTemplateTest.java
```

---

### Task 10: InsectsCatalogContribution — order token emission

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/catalog/InsectsCatalogContribution.java`

- [ ] **Step 1: Add `OrderQuery` to the contribution**

Add field:
```java
    private final InsectQuery.OrderQuery orders;
```

Update constructor signature and validation:
```java
    public InsectsCatalogContribution(InsectQuery.SpeciesQuery species,
                                      InsectQuery.FamilyQuery families,
                                      InsectQuery.GenusQuery genera,
                                      InsectQuery.OrderQuery orders) {
        Observer.forClass(InsectsCatalogContribution.class)
                .arguments("constructor", i -> i
                        .notNull(species, "species")
                        .notNull(families, "families")
                        .notNull(genera, "genera")
                        .notNull(orders, "orders"))
                .throwWhenInvalid();
        this.species = species;
        this.families = families;
        this.genera = genera;
        this.orders = orders;
    }
```

- [ ] **Step 2: Add order entities to the searchable stream**

Update `searchableEntities()`:
```java
    @Override
    public Stream<SearchableEntity> searchableEntities() {
        Map<InsectGenusName, InsectGenus> genusByName = Pages.stream(ASSEMBLY_PAGE_SIZE, genera::findPage)
                .collect(Collectors.toMap(InsectGenus::name, Function.identity()));
        return Stream.of(
                speciesEntities(genusByName),
                familyEntities(),
                genusEntities(),
                orderEntities()
        ).flatMap(s -> s);
    }
```

Add the order entity stream:
```java
    private Stream<SearchableEntity> orderEntities() {
        return Pages.stream(ASSEMBLY_PAGE_SIZE, orders::findPage)
                .map(InsectsCatalogContribution::toSearchableOrder);
    }

    private static SearchableEntity toSearchableOrder(InsectOrder entity) {
        EntityRef target = new EntityRef(DOMAIN, entity.name());
        return new SearchableEntity(target, tokensFor(entity));
    }

    private static Stream<String> tokensFor(InsectOrder entity) {
        Stream.Builder<String> tokens = Stream.builder();
        tokens.add(entity.name().value());
        tokens.add(entity.order().value());
        entity.commonNames().forEach(commonName -> tokens.add(commonName.label()));
        return tokens.build();
    }
```

Add `InsectOrder` to imports.

- [ ] **Step 3: Update `InsectsCatalogContributionTest`**

Check the test file and update the constructor call to pass the `OrderQuery`. The test creates an `InsectsCatalogContribution` — it will need the `OrderQuery` from the test context.

- [ ] **Step 4: Stage changes**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/catalog/InsectsCatalogContribution.java
git add domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectsCatalogContributionTest.java
```

---

### Task 11: Verify the build

- [ ] **Step 1: Run the build**

The user runs `mvn verify` from the repository root. Key modules to watch:

- `taxonomy` — LinnaeanFamily, LinnaeanOrder, TaxonomicSlugs changes
- `identifiers` — InsectOrderName, InsectRankName changes
- `insects-api` — InsectOrder, InsectFamily, InsectGenus, aggregates, queries
- `insects-core` — query impls, factory, catalog contribution
- `insects-repository-test` — mocks, contract tests, JSON deserialization
- `insects-console` — JTE template compilation, controller, template tests

- [ ] **Step 2: Fix any remaining compilation or test failures**

If failures occur, check for:
1. Missed callers of `new InsectFamily(...)` or `new InsectGenus(...)` — search for these constructors and update
2. Templates referencing `family.order()` — search JTE files for this pattern
3. Missing imports for `InsectOrderName` or `InsectOrder`
4. `InsectsCatalogContributionTest` needing updated constructor args
