# Insect Citation Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Present citation associations on insects detail pages by composing resolved citations into the `Insect` read model.

**Architecture:** `InsectCitationView.RankedCitation` gains the resolved `Citation` authority object (replacing the bare `CitationName` slug). The `Insect` read model gains an `@Nullable InsectCitationView` component. A new `InsectFactory` in `insects-core` assembles the full read model. `InsectQuery` gains a top-level `getByName(InsectRankName)` method. Controller detail pages switch from ad-hoc multi-query assembly to one `insectQuery.getByName()` call, and templates render citation links.

**Tech Stack:** Java records, JTE templates, Observer framework, existing `CitationQuery`/`CitationAssociationQuery` from library domain.

**Spec deviation:** The spec says `InsectCitationView citations` is "not nullable." However, `InsectCitationView` requires a non-null `subject` (`InsectRankName`), which `Insect.empty()` cannot supply (no rank identified). This plan uses `@Nullable InsectCitationView citations` on `Insect`, consistent with the nullable rank-view pattern already established on the record. The factory always populates it; only `Insect.empty()` passes `null`.

---

### Task 1: Update `RankedCitation` to carry resolved `Citation`

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCitationView.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectCitationViewTest.java`

- [ ] **Step 1: Update `RankedCitation` record to use `Citation` instead of `CitationName`**

```java
// In InsectCitationView.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.authority.Citation;
import com.naturalist.ddd.ReadModel;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public record InsectCitationView(
        InsectRankName subject,
        List<RankedCitation> citations
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .identifier(subject, "subject")
                .notNull(citations, "citations");
    }

    public record RankedCitation(
            Citation citation,
            InsectRankName attachedAt,
            @Nullable String note
    ) implements ValueObject {

        @Override
        public Consumer<? extends Constraints> invariants() {
            return i -> i
                    .namedEntity(citation, "citation")
                    .identifier(attachedAt, "attachedAt");
        }
    }
}
```

Key change: `CitationName citationName` → `Citation citation`. Invariant changes from `.entityName(citationName, ...)` to `.namedEntity(citation, ...)` because `Citation` is a `NamedEntity`, not an `EntityName`.

- [ ] **Step 2: Update `InsectCitationViewTest` to construct `RankedCitation` with a `Citation` object**

The test needs `Citation` instances. `Citation` is a sealed interface with one permit: `OnlineSource`. Construct using `OnlineSource`:

```java
// In InsectCitationViewTest.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCitationViewTest {

    private static final Observer observer = Observer.forClass(InsectCitationViewTest.class);

    @Test
    void validView_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validView_hasNoInvariantViolations");

        InsectCitationView view = new InsectCitationView(
                InsectSpeciesName.of("battus-philenor"),
                List.of(new InsectCitationView.RankedCitation(
                        citation("eol-battus-philenor-130502", "EOL: Battus philenor"),
                        InsectOrderName.of("lepidoptera"),
                        "EOL page")));

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportInvariantViolations");

        InsectCitationView view = new InsectCitationView(null, null);

        InvariantObservation result = mo.observable(view, "view");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".view.subject", ".view.citations");
    }

    @Test
    void validRankedCitation_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validRankedCitation_hasNoInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
                citation("eol-battus-philenor-130502", "EOL: Battus philenor"),
                InsectFamilyName.of("papilionidae"),
                null);

        InvariantObservation result = mo.observable(rc, "rankedCitation");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullRankedCitationComponents_reportInvariantViolations() {
        MethodObserver mo = observer.forMethod("nullRankedCitationComponents_reportInvariantViolations");

        InsectCitationView.RankedCitation rc = new InsectCitationView.RankedCitation(
                null, null, null);

        InvariantObservation result = mo.observable(rc, "rankedCitation");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".rankedCitation.citation", ".rankedCitation.attachedAt");
    }

    private static OnlineSource citation(String slug, String title) {
        return new OnlineSource(
                CitationName.of(slug),
                new AuthorityReference(
                        new AuthoritySource("eol", "Encyclopedia of Life"),
                        URI.create("https://eol.org/pages/130502")),
                title, null, null, null);
    }
}
```

Key changes: `CitationName.of(...)` in constructors → `citation(...)` helper that builds a full `OnlineSource`. Violation name changes from `.rankedCitation.citationName` to `.rankedCitation.citation`.

- [ ] **Step 3: Run tests to verify**

Run: `mvn test -pl domains/insects/insects-api -Dtest=InsectCitationViewTest`
Expected: all 4 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectCitationView.java \
       domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectCitationViewTest.java
git commit -m "refactor(insects): RankedCitation carries resolved Citation instead of CitationName"
```

---

### Task 2: Update `InsectCitationQueryImpl` to resolve `Citation` objects

**Files:**
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCitationQueryImpl.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCitationQueryImplTest.java`

- [ ] **Step 1: Update `InsectCitationQueryImplTest` to wire `CitationQuery` and assert on resolved `Citation`**

```java
// In InsectCitationQueryImplTest.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.library.LibraryTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectCitationQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);

    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);

    LibraryTestContext libraryContext = LibraryTestContext.create(db);
    CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
    CitationQuery citationQuery = libraryContext.citationQuery();

    InsectCitationQueryImpl citationQueryImpl = new InsectCitationQueryImpl(
            citationAssociationQuery, citationQuery, speciesQuery, genusQuery, familyQuery);

    @Test
    void findByRankName_rejectsNull() {
        assertThatThrownBy(() -> citationQueryImpl.findByRankName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("rankName");
    }

    @Test
    void findByRankName_orderWithCitation_returnsCitation() {
        InsectCitationView view = citationQueryImpl.findByRankName(
                InsectOrderName.of("lepidoptera"));

        assertThat(view.subject().value()).isEqualTo("lepidoptera");
        assertThat(view.citations()).hasSize(1);
        assertThat(view.citations().getFirst().citation().name().value())
                .isEqualTo("eol-battus-philenor-130502");
        assertThat(view.citations().getFirst().attachedAt().value())
                .isEqualTo("lepidoptera");
    }

    @Test
    void findByRankName_familyInheritsFromOrder() {
        InsectCitationView view = citationQueryImpl.findByRankName(
                InsectFamilyName.of("papilionidae"));

        assertThat(view.subject().value()).isEqualTo("papilionidae");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_speciesInheritsFullChain() {
        InsectCitationView view = citationQueryImpl.findByRankName(
                InsectSpeciesName.of("battus-philenor"));

        assertThat(view.subject().value()).isEqualTo("battus-philenor");
        assertThat(view.citations()).hasSize(2);
        assertThat(view.citations())
                .extracting(c -> c.attachedAt().value())
                .containsExactlyInAnyOrder("papilionidae", "lepidoptera");
    }

    @Test
    void findByRankName_orderWithNoCitations_returnsEmpty() {
        InsectCitationView view = citationQueryImpl.findByRankName(
                InsectOrderName.of("coleoptera"));

        assertThat(view.citations()).isEmpty();
    }
}
```

Key changes: `LibraryTestContext` provides both `citationAssociationQuery()` and `citationQuery()`. Constructor gains `citationQuery` param. Assertions use `.citation().name().value()` instead of `.citationName().value()`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectCitationQueryImplTest`
Expected: FAIL — `InsectCitationQueryImpl` constructor does not accept `CitationQuery` yet.

- [ ] **Step 3: Update `InsectCitationQueryImpl` to accept `CitationQuery` and resolve citations**

```java
// In InsectCitationQueryImpl.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.library.CitationAssociation;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class InsectCitationQueryImpl implements InsectQuery.CitationQuery {

    private static final InsectsDomain INSECTS = new InsectsDomain();
    private final Observer observer = Observer.forClass(getClass());
    private final CitationAssociationQuery citationAssociationQuery;
    private final CitationQuery citationQuery;
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;

    InsectCitationQueryImpl(CitationAssociationQuery citationAssociationQuery,
                            CitationQuery citationQuery,
                            InsectQuery.SpeciesQuery speciesQuery,
                            InsectQuery.GenusQuery genusQuery,
                            InsectQuery.FamilyQuery familyQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(citationQuery, "citationQuery")
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery"))
                .throwWhenInvalid();
        this.citationAssociationQuery = citationAssociationQuery;
        this.citationQuery = citationQuery;
        this.speciesQuery = speciesQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
    }

    @Override
    public InsectCitationView findByRankName(InsectRankName rankName) {
        observer.arguments("findByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();

        List<InsectRankName> ancestry = resolveAncestry(rankName);

        // Collect associations per rank, keyed by CitationName for batch resolution
        record PendingCitation(CitationName citationName, InsectRankName attachedAt, String note) {}
        List<PendingCitation> pending = new ArrayList<>();

        for (InsectRankName rank : ancestry) {
            EntityRef ref = new EntityRef(INSECTS, (EntityName) rank);
            for (CitationAssociation a : citationAssociationQuery.findBySubject(ref).stream().toList()) {
                pending.add(new PendingCitation(a.citationName(), rank, a.note()));
            }
        }

        if (pending.isEmpty()) {
            InsectCitationView view = new InsectCitationView(rankName, List.of());
            observer.observable(view, "citationView").observe(Level.WARN);
            return view;
        }

        // Batch-resolve all CitationName values to Citation entities
        Set<CitationName> names = pending.stream()
                .map(PendingCitation::citationName)
                .collect(Collectors.toSet());
        Map<CitationName, Citation> resolved = new LinkedHashMap<>();
        for (Citation c : citationQuery.findByNameSet(names).stream().toList()) {
            resolved.put(c.name(), c);
        }

        List<InsectCitationView.RankedCitation> citations = new ArrayList<>();
        for (PendingCitation p : pending) {
            Citation citation = resolved.get(p.citationName());
            if (citation != null) {
                citations.add(new InsectCitationView.RankedCitation(
                        citation, p.attachedAt(), p.note()));
            } else {
                observer.forMethod("findByRankName")
                        .value(p.citationName().value(), "unresolvedCitationName")
                        .observe(Level.WARN);
            }
        }

        InsectCitationView view = new InsectCitationView(rankName, List.copyOf(citations));
        observer.observable(view, "citationView").observe(Level.WARN);
        return view;
    }

    private List<InsectRankName> resolveAncestry(InsectRankName rankName) {
        List<InsectRankName> ancestry = new ArrayList<>();
        ancestry.add(rankName);

        return switch (rankName) {
            case InsectSpeciesName speciesName -> {
                speciesQuery.getByName(speciesName).ifPresent(species -> {
                    ancestry.add(species.genusName());
                    genusQuery.getByName(species.genusName()).ifPresent(genus -> {
                        ancestry.add(genus.familyName());
                        familyQuery.getByName(genus.familyName()).ifPresent(family ->
                                ancestry.add(family.orderName()));
                    });
                });
                yield ancestry;
            }
            case InsectGenusName genusName -> {
                genusQuery.getByName(genusName).ifPresent(genus -> {
                    ancestry.add(genus.familyName());
                    familyQuery.getByName(genus.familyName()).ifPresent(family ->
                            ancestry.add(family.orderName()));
                });
                yield ancestry;
            }
            case InsectFamilyName familyName -> {
                familyQuery.getByName(familyName).ifPresent(family ->
                        ancestry.add(family.orderName()));
                yield ancestry;
            }
            case InsectOrderName _ -> ancestry;
            case InsectSubspeciesName _ -> ancestry;
        };
    }
}
```

Key changes: new `citationQuery` field. After collecting `CitationAssociation` records, batch-resolves `CitationName` → `Citation` via `citationQuery.findByNameSet(names)`. Unresolvable names are logged and dropped. Uses a local `PendingCitation` record to stage pre-resolution state.

- [ ] **Step 4: Update `InsectQueryImpl` constructor to pass `CitationQuery` through**

In `InsectQueryImpl.java`, the constructor currently takes `CitationAssociationQuery` and builds `InsectCitationQueryImpl`. Add `CitationQuery` as a parameter and pass it through.

```java
// In InsectQueryImpl.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.observability.Observer;

class InsectQueryImpl implements InsectQuery {

    private final SpeciesQuery speciesQuery;
    private final ImageQuery imageQuery;
    private final FamilyQuery familyQuery;
    private final GenusQuery genusQuery;
    private final FunctionalRoleQuery functionalRoleQuery;
    private final OrderQuery orderQuery;
    private final TaxonViewQuery taxonViewQuery;
    private final CitationQuery citationQuery;

    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery,
                    CitationAssociationQuery citationAssociationQuery,
                    CitationQuery libraryCitationQuery) {
        Observer.forClass(InsectQueryImpl.class).arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(functionalRoleQuery, "functionalRoleQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(citationAssociationQuery, "citationAssociationQuery")
                        .notNull(libraryCitationQuery, "libraryCitationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.familyQuery = familyQuery;
        this.genusQuery = genusQuery;
        this.functionalRoleQuery = functionalRoleQuery;
        this.orderQuery = orderQuery;
        InsectTaxonViewFactory factory =
                new InsectTaxonViewFactory(speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery);
        this.taxonViewQuery = new TaxonViewQueryImpl(factory);
        this.citationQuery = new InsectCitationQueryImpl(
                citationAssociationQuery, libraryCitationQuery, speciesQuery, genusQuery, familyQuery);
    }

    @Override
    public TaxonViewQuery taxonView() {
        return taxonViewQuery;
    }

    @Override
    public SpeciesQuery species() {
        return speciesQuery;
    }

    @Override
    public ImageQuery images() {
        return imageQuery;
    }

    @Override
    public FamilyQuery families() {
        return familyQuery;
    }

    @Override
    public GenusQuery genera() {
        return genusQuery;
    }

    @Override
    public FunctionalRoleQuery functionalRoles() {
        return functionalRoleQuery;
    }

    @Override
    public OrderQuery orders() {
        return orderQuery;
    }

    @Override
    public CitationQuery citations() {
        return citationQuery;
    }
}
```

Key change: constructor gains `CitationQuery libraryCitationQuery` parameter (named `libraryCitationQuery` to avoid shadowing the field `citationQuery` which is the insects-domain `CitationQuery`). Passed through to `InsectCitationQueryImpl`.

- [ ] **Step 5: Update `InsectQueryImplTest` for new constructor arity**

In `InsectQueryImplTest.java`, add a stub `CitationQuery` alongside the existing stub `CitationAssociationQuery`, and update all constructor calls.

```java
// In InsectQueryImplTest.java, replace the full file:
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectQueryImplTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    InsectFunctionalRoleRepositoryMock functionalRoleRepository = new InsectFunctionalRoleRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);
    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.FunctionalRoleQuery functionalRoleQuery = new FunctionalRoleQueryImpl(functionalRoleRepository);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);
    com.naturalist.library.CitationAssociationQuery citationAssociationQuery =
            new com.naturalist.library.CitationAssociationQuery() {
                @Override
                public com.naturalist.library.CitationAssociationCollection findByCitationName(
                        com.naturalist.authority.CitationName citationName) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }

                @Override
                public com.naturalist.library.CitationAssociationCollection findBySubject(
                        com.naturalist.catalog.EntityRef subject) {
                    return com.naturalist.library.CitationAssociationCollection.empty();
                }
            };
    com.naturalist.library.CitationQuery libraryCitationQuery =
            new com.naturalist.library.CitationQuery() {
                @Override
                public java.util.Optional<com.naturalist.authority.Citation> getByName(
                        com.naturalist.authority.CitationName name) {
                    return java.util.Optional.empty();
                }

                @Override
                public com.naturalist.library.CitationCollection findByNameSet(
                        java.util.Set<com.naturalist.authority.CitationName> names) {
                    return com.naturalist.library.CitationCollection.empty();
                }

                @Override
                public com.naturalist.data.Page<com.naturalist.authority.Citation> findPage(
                        com.naturalist.data.PageRequest pageRequest) {
                    return com.naturalist.data.Page.empty();
                }
            };
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
            orderQuery, citationAssociationQuery, libraryCitationQuery);

    @Test
    void accessors_returnNonNullDelegates() {
        assertThat(insectQuery.species()).isSameAs(speciesQuery);
        assertThat(insectQuery.images()).isSameAs(imageQuery);
        assertThat(insectQuery.families()).isSameAs(familyQuery);
        assertThat(insectQuery.genera()).isSameAs(genusQuery);
        assertThat(insectQuery.functionalRoles()).isSameAs(functionalRoleQuery);
        assertThat(insectQuery.orders()).isSameAs(orderQuery);
        assertThat(insectQuery.taxonView()).isNotNull();
        assertThat(insectQuery.citations()).isNotNull();
    }

    @Test
    void accessors_idempotent() {
        assertThat(insectQuery.species()).isSameAs(insectQuery.species());
        assertThat(insectQuery.images()).isSameAs(insectQuery.images());
        assertThat(insectQuery.families()).isSameAs(insectQuery.families());
        assertThat(insectQuery.genera()).isSameAs(insectQuery.genera());
        assertThat(insectQuery.functionalRoles()).isSameAs(insectQuery.functionalRoles());
        assertThat(insectQuery.orders()).isSameAs(insectQuery.orders());
        assertThat(insectQuery.taxonView()).isSameAs(insectQuery.taxonView());
        assertThat(insectQuery.citations()).isSameAs(insectQuery.citations());
    }

    @Test
    void constructor_rejectsNullSpeciesQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, imageQuery, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery");
    }

    @Test
    void constructor_rejectsNullImageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, null, familyQuery, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("imageQuery");
    }

    @Test
    void constructor_rejectsNullFamilyQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, null, genusQuery, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("familyQuery");
    }

    @Test
    void constructor_rejectsNullGenusQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, null, functionalRoleQuery, orderQuery, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("genusQuery");
    }

    @Test
    void constructor_rejectsNullFunctionalRoleQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, null, orderQuery, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("functionalRoleQuery");
    }

    @Test
    void constructor_rejectsNullOrderQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery, null, citationAssociationQuery, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("orderQuery");
    }

    @Test
    void constructor_rejectsNullCitationAssociationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, null, libraryCitationQuery))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("citationAssociationQuery");
    }

    @Test
    void constructor_rejectsNullLibraryCitationQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("libraryCitationQuery");
    }

    @Test
    void constructor_collectsAllViolationsInSinglePass() {
        assertThatThrownBy(() -> new InsectQueryImpl(null, null, null, null, null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("speciesQuery", "imageQuery", "familyQuery",
                        "genusQuery", "functionalRoleQuery", "orderQuery",
                        "citationAssociationQuery", "libraryCitationQuery");
    }
}
```

Key changes: new `libraryCitationQuery` stub, all constructor calls gain the 8th parameter, new null-rejection test for `libraryCitationQuery`.

- [ ] **Step 6: Update `InsectsTestContext` to pass `CitationQuery` through**

In `InsectsTestContext.java`, extract `citationQuery` from `LibraryTestContext` alongside `citationAssociationQuery` and pass both to `InsectQueryImpl`.

```java
// In InsectsTestContext.java, change the InsectQueryImpl construction block.
// Replace lines 53-57 (the library context + InsectQueryImpl wiring):
        LibraryTestContext libraryContext = LibraryTestContext.create(db);
        CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
        CitationQuery libraryCitationQuery = libraryContext.citationQuery();
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationAssociationQuery, libraryCitationQuery);
```

Add the import:
```java
import com.naturalist.library.CitationQuery;
```

- [ ] **Step 7: Run tests to verify**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectCitationQueryImplTest,InsectQueryImplTest`
Expected: all tests PASS.

- [ ] **Step 8: Commit**

```bash
git add domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectCitationQueryImpl.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectCitationQueryImplTest.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java \
       domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
git commit -m "feat(insects): resolve Citation authority objects in InsectCitationQueryImpl"
```

---

### Task 3: Add `InsectCitationView` to the `Insect` record

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java`
- Modify: `domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java`

- [ ] **Step 1: Update `InsectTest` with tests for the new `citations` component**

Add a `withCitations` test and update existing test helpers. In `InsectTest.java`, make these changes:

First, add the import:
```java
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import java.net.URI;
```

Add the `withCitations` test after `withLifeStages` tests (after line 157):
```java
    @Test
    void withCitationsReplacesCitationsAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderView());
        InsectCitationView citations = new InsectCitationView(
                InsectOrderName.of("lepidoptera"),
                List.of(new InsectCitationView.RankedCitation(
                        new OnlineSource(
                                CitationName.of("eol-lepidoptera"),
                                new AuthorityReference(
                                        new AuthoritySource("eol", "Encyclopedia of Life"),
                                        URI.create("https://eol.org/pages/747")),
                                "EOL: Lepidoptera", null, null, null),
                        InsectOrderName.of("lepidoptera"),
                        null)));

        Insect updated = base.withCitations(citations);

        assertThat(updated.citations()).isSameAs(citations);
        assertThat(updated.order()).isSameAs(base.order());
        assertThat(updated.observations()).isSameAs(base.observations());
    }
```

Update `emptyAggregateHasNoObservationsNoRanksAndNoLifeStages` (line 27) — add assertion:
```java
        assertThat(insect.citations()).isNull();
```

Update all `new Insect(...)` calls in `InsectTest.java` — every direct constructor call needs the 7th `null` argument for `citations`. These are in the invariant violation test methods. Example for `nullObservationsReportsObservationsViolation`:
```java
        Insect insect = new Insect(
                null,
                null, null, null, null,
                LifeStageCollection.empty(),
                null);
```

Apply the same pattern to `nullLifeStagesReportsLifeStagesViolation`, `speciesPresentWithoutGenusReportsSpeciesGenusViolation`, `genusPresentWithoutFamilyReportsGenusFamilyViolation`, `familyPresentWithoutOrderReportsFamilyOrderViolation`, `withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations`, `familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation`, `genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation`, and `speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-api -Dtest=InsectTest`
Expected: FAIL — `Insect` record does not have 7 components yet.

- [ ] **Step 3: Update `Insect` record with `citations` component**

In `Insect.java`, make these changes:

Add the 7th component:
```java
public record Insect(
        ImageCollection observations,
        @Nullable InsectOrderView order,
        @Nullable InsectFamilyView family,
        @Nullable InsectGenusView genus,
        @Nullable InsectSpeciesView species,
        LifeStageCollection lifeStages,
        @Nullable InsectCitationView citations
) implements ReadModel {
```

Update `empty()`:
```java
    public static Insect empty() {
        return new Insect(
                ImageCollection.empty(),
                null,
                null,
                null,
                null,
                LifeStageCollection.empty(),
                null);
    }
```

Update every `with*` method to carry `citations` through:
```java
    public Insect withObservations(ImageCollection observations) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withOrder(@Nullable InsectOrderView order) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withFamily(@Nullable InsectFamilyView family) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withGenus(@Nullable InsectGenusView genus) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withSpecies(@Nullable InsectSpeciesView species) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withLifeStages(LifeStageCollection lifeStages) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }

    public Insect withCitations(@Nullable InsectCitationView citations) {
        return new Insect(observations, order, family, genus, species, lifeStages, citations);
    }
```

Add citations to `invariants()`, after the `.whenNotNull(species, ...)` block:
```java
            .whenNotNull(citations, c -> c
                .readModel(citations, "citations")
            )
```

- [ ] **Step 4: Run tests to verify**

Run: `mvn test -pl domains/insects/insects-api -Dtest=InsectTest`
Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java \
       domains/insects/insects-api/src/test/java/com/naturalist/insects/InsectTest.java
git commit -m "feat(insects): add InsectCitationView to Insect read model"
```

---

### Task 4: Create `InsectFactory` and wire `InsectQuery.getByName`

**Files:**
- Create: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java`
- Create: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java`
- Modify: `domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java`
- Modify: `domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java`
- Modify: `domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java`

- [ ] **Step 1: Add `getByName` to `InsectQuery` interface**

In `InsectQuery.java`, add above the existing `TaxonViewQuery taxonView()` method (around line 48):

```java
    /**
     * Assemble the full {@link Insect} read model for the given rank name.
     * Returns the rank chain, images, life stages, and citations composed
     * into a single view. Returns empty for unknown names or subspecies-rank
     * requests (no subspecies entity exists yet).
     */
    Optional<Insect> getByName(InsectRankName name);
```

Add the import at the top of the file:
```java
import java.util.Optional;
```

(`Optional` is already imported — verify before adding.)

- [ ] **Step 2: Write `InsectFactoryTest`**

```java
// Create: domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java
package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.InsectLifeStageTestContext;
import com.naturalist.library.CitationAssociationQuery;
import com.naturalist.library.CitationQuery;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectFactoryTest {

    static final Observer observer = Observer.forClass(InsectFactoryTest.class);

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SpeciesRepositoryMock speciesRepository = new SpeciesRepositoryMock(db);
    InsectImageRepositoryMock imageRepository = new InsectImageRepositoryMock(db);
    FamilyRepositoryMock familyRepository = new FamilyRepositoryMock(db);
    GenusRepositoryMock genusRepository = new GenusRepositoryMock(db);
    OrderRepositoryMock orderRepository = new OrderRepositoryMock(db);

    InsectQuery.ImageQuery imageQuery = new ImageQueryImpl(imageRepository);
    InsectQuery.FamilyQuery familyQuery = new FamilyQueryImpl(familyRepository);
    InsectQuery.GenusQuery genusQuery = new GenusQueryImpl(genusRepository, familyQuery);
    InsectQuery.SpeciesQuery speciesQuery = new SpeciesQueryImpl(speciesRepository, genusQuery);
    InsectQuery.OrderQuery orderQuery = new OrderQueryImpl(orderRepository);

    InsectLifeStageQuery lifeStageQuery = InsectLifeStageTestContext.createQuery(db);

    LibraryTestContext libraryContext = LibraryTestContext.create(db);
    CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
    CitationQuery libraryCitationQuery = libraryContext.citationQuery();

    InsectQuery.CitationQuery citationQuery = new InsectCitationQueryImpl(
            citationAssociationQuery, libraryCitationQuery, speciesQuery, genusQuery, familyQuery);

    InsectFactory factory = new InsectFactory(
            speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
            lifeStageQuery, citationQuery);

    @Test
    void buildByName_rejectsNull() {
        assertThatThrownBy(() -> factory.buildByName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("name");
    }

    @Test
    void buildByName_speciesName_returnsFullyPopulatedInsect() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.species()).isNotNull();
        assertThat(insect.species().species().name())
                .isEqualTo(TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);
        assertThat(insect.genus()).isNotNull();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.observations()).isNotNull();
        assertThat(insect.citations()).isNotNull();
        assertThat(observer.observable(insect, "insect").violations()).isEmpty();
    }

    @Test
    void buildByName_orderName_returnsOrderOnlyInsect() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectOrders.Lepidoptera.name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.order().order().name())
                .isEqualTo(TestInsectsIdentifiers.InsectOrders.Lepidoptera.name);
        assertThat(insect.family()).isNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.citations()).isNotNull();
    }

    @Test
    void buildByName_familyName_returnsFamilyAndOrderInsect() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectFamilies.Papilionidae.name);

        assertThat(result).isPresent();
        Insect insect = result.get();
        assertThat(insect.order()).isNotNull();
        assertThat(insect.family()).isNotNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
    }

    @Test
    void buildByName_unknownName_returnsEmpty() {
        Optional<Insect> result = factory.buildByName(
                InsectSpeciesName.of("unobtainium-beetle"));

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_subspeciesName_returnsEmpty() {
        Optional<Insect> result = factory.buildByName(
                InsectSubspeciesName.of("battus-philenor-hirsuta"));

        assertThat(result).isEmpty();
    }

    @Test
    void buildByName_speciesWithCitations_includesResolvedCitations() {
        Optional<Insect> result = factory.buildByName(
                TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name);

        assertThat(result).isPresent();
        assertThat(result.get().citations()).isNotNull();
        assertThat(result.get().citations().citations())
                .extracting(c -> c.citation().name().value())
                .contains("eol-battus-philenor-130502");
    }
}
```

Note: `TestInsectsIdentifiers` names used here must match what exists in the test data. Verify the exact constant paths (`InsectOrders.Lepidoptera.name`, `InsectFamilies.Papilionidae.name`, `InsectSpecies.BattusPhilenor.name`) against the actual `TestInsectsIdentifiers` class before running. Adjust if needed.

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectFactoryTest`
Expected: FAIL — `InsectFactory` class does not exist.

- [ ] **Step 4: Create `InsectFactory`**

```java
// Create: domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java
package com.naturalist.insects;

import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Name-keyed assembly of the full {@link Insect} read model. Resolves the rank
 * chain, photographs, life stages, and citations into a single composed view.
 *
 * <p>Same pattern as {@link InsectTaxonViewFactory} — concrete, package-private,
 * no interface. The factory validates arguments with {@code throwWhenInvalid()}
 * and observes the assembled read model with {@code observe()}.
 */
class InsectFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final InsectQuery.SpeciesQuery speciesQuery;
    private final InsectQuery.ImageQuery imageQuery;
    private final InsectQuery.GenusQuery genusQuery;
    private final InsectQuery.FamilyQuery familyQuery;
    private final InsectQuery.OrderQuery orderQuery;
    private final InsectLifeStageQuery lifeStageQuery;
    private final InsectQuery.CitationQuery citationQuery;

    InsectFactory(InsectQuery.SpeciesQuery speciesQuery,
                  InsectQuery.ImageQuery imageQuery,
                  InsectQuery.GenusQuery genusQuery,
                  InsectQuery.FamilyQuery familyQuery,
                  InsectQuery.OrderQuery orderQuery,
                  InsectLifeStageQuery lifeStageQuery,
                  InsectQuery.CitationQuery citationQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(speciesQuery, "speciesQuery")
                        .notNull(imageQuery, "imageQuery")
                        .notNull(genusQuery, "genusQuery")
                        .notNull(familyQuery, "familyQuery")
                        .notNull(orderQuery, "orderQuery")
                        .notNull(lifeStageQuery, "lifeStageQuery")
                        .notNull(citationQuery, "citationQuery"))
                .throwWhenInvalid();
        this.speciesQuery = speciesQuery;
        this.imageQuery = imageQuery;
        this.genusQuery = genusQuery;
        this.familyQuery = familyQuery;
        this.orderQuery = orderQuery;
        this.lifeStageQuery = lifeStageQuery;
        this.citationQuery = citationQuery;
    }

    Optional<Insect> buildByName(InsectRankName name) {
        observer.arguments("buildByName", i -> i.identifier(name, "name")).throwWhenInvalid();

        return switch (name) {
            case InsectSpeciesName speciesName ->
                    speciesQuery.getByName(speciesName).map(species -> {
                        Insect insect = Insect.empty()
                                .withObservations(imageQuery.forParentName(speciesName))
                                .withSpecies(InsectSpeciesView.of(species))
                                .withLifeStages(lifeStageQuery.lifeStages().forParentName(speciesName))
                                .withCitations(citationQuery.findByRankName(speciesName));
                        insect = resolveGenus(insect, species.genusName());
                        return observe(insect);
                    });
            case InsectGenusName genusName ->
                    genusQuery.getByName(genusName).map(genus -> {
                        Insect insect = Insect.empty()
                                .withObservations(imageQuery.forParentName(genusName))
                                .withGenus(InsectGenusView.of(genus))
                                .withLifeStages(lifeStageQuery.lifeStages().forParentName(genusName))
                                .withCitations(citationQuery.findByRankName(genusName));
                        insect = resolveFamily(insect, genus.familyName());
                        return observe(insect);
                    });
            case InsectFamilyName familyName ->
                    familyQuery.getByName(familyName).map(family -> {
                        Insect insect = Insect.empty()
                                .withObservations(imageQuery.forParentName(familyName))
                                .withFamily(InsectFamilyView.of(family))
                                .withLifeStages(lifeStageQuery.lifeStages().forParentName(familyName))
                                .withCitations(citationQuery.findByRankName(familyName));
                        insect = resolveOrder(insect, family.orderName());
                        return observe(insect);
                    });
            case InsectOrderName orderName ->
                    orderQuery.getByName(orderName).map(order -> observe(
                            Insect.empty()
                                    .withObservations(imageQuery.forParentName(orderName))
                                    .withOrder(InsectOrderView.of(order))
                                    .withLifeStages(lifeStageQuery.lifeStages().forParentName(orderName))
                                    .withCitations(citationQuery.findByRankName(orderName))));
            case InsectSubspeciesName _ -> Optional.empty();
        };
    }

    private Insect resolveGenus(Insect insect, InsectGenusName genusName) {
        return genusQuery.getByName(genusName)
                .map(genus -> resolveFamily(
                        insect.withGenus(InsectGenusView.of(genus)),
                        genus.familyName()))
                .orElse(insect);
    }

    private Insect resolveFamily(Insect insect, InsectFamilyName familyName) {
        return familyQuery.getByName(familyName)
                .map(family -> resolveOrder(
                        insect.withFamily(InsectFamilyView.of(family)),
                        family.orderName()))
                .orElse(insect);
    }

    private Insect resolveOrder(Insect insect, InsectOrderName orderName) {
        return orderQuery.getByName(orderName)
                .map(order -> insect.withOrder(InsectOrderView.of(order)))
                .orElse(insect);
    }

    private Insect observe(Insect insect) {
        observer.observable(insect, "insect").observe(Level.WARN);
        return insect;
    }
}
```

- [ ] **Step 5: Wire into `InsectQueryImpl`**

In `InsectQueryImpl.java`, add the factory field and implement `getByName`:

Add field (after `citationQuery`):
```java
    private final InsectFactory insectFactory;
```

In the constructor, after building `citationQuery`, add:
```java
        this.insectFactory = new InsectFactory(
                speciesQuery, imageQuery, genusQuery, familyQuery, orderQuery,
                insectLifeStageQuery, this.citationQuery);
```

The constructor needs `InsectLifeStageQuery insectLifeStageQuery` as a new parameter. Update the constructor signature:

```java
    InsectQueryImpl(SpeciesQuery speciesQuery,
                    ImageQuery imageQuery,
                    FamilyQuery familyQuery,
                    GenusQuery genusQuery,
                    FunctionalRoleQuery functionalRoleQuery,
                    OrderQuery orderQuery,
                    CitationAssociationQuery citationAssociationQuery,
                    CitationQuery libraryCitationQuery,
                    InsectLifeStageQuery insectLifeStageQuery) {
```

Add validation: `.notNull(insectLifeStageQuery, "insectLifeStageQuery")`.

Add the import:
```java
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import java.util.Optional;
```

Add the `getByName` implementation:
```java
    @Override
    public Optional<Insect> getByName(InsectRankName name) {
        return insectFactory.buildByName(name);
    }
```

- [ ] **Step 6: Update `InsectsTestContext` to pass `InsectLifeStageQuery`**

In `InsectsTestContext.java`, the `insectLifeStageQuery` is already created. Pass it as the 9th argument to `InsectQueryImpl`:

```java
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationAssociationQuery, libraryCitationQuery,
                this.insectLifeStageQuery);
```

Note: `this.insectLifeStageQuery` is assigned on the line after the current `InsectQueryImpl` construction. Reorder so it's assigned before `InsectQueryImpl`:

```java
        this.insectLifeStageQuery = InsectLifeStageTestContext.createQuery(db);
        LibraryTestContext libraryContext = LibraryTestContext.create(db);
        CitationAssociationQuery citationAssociationQuery = libraryContext.citationAssociationQuery();
        CitationQuery libraryCitationQuery = libraryContext.citationQuery();
        this.insectQuery = new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
                orderQuery, citationAssociationQuery, libraryCitationQuery,
                this.insectLifeStageQuery);
```

- [ ] **Step 7: Update `InsectQueryImplTest` for new constructor arity**

Add a stub `InsectLifeStageQuery` and pass as the 9th parameter. Add imports and a field:

```java
    com.naturalist.insects.lifestage.InsectLifeStageQuery insectLifeStageQuery =
            new com.naturalist.insects.lifestage.InsectLifeStageQuery() {
                @Override
                public LifeStageEntityQuery lifeStages() {
                    return new LifeStageEntityQuery() {
                        @Override
                        public com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection
                        forParentName(InsectRankName parentName) {
                            return com.naturalist.insects.lifestage.InsectLifeStageEntityCollections
                                    .LifeStageCollection.empty();
                        }

                        @Override
                        public java.util.Optional<com.naturalist.insects.lifestage.LifeStage>
                        getByName(com.naturalist.insects.LifeStageName name) {
                            return java.util.Optional.empty();
                        }

                        @Override
                        public com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection
                        findByNameSet(java.util.Set<com.naturalist.insects.LifeStageName> names) {
                            return com.naturalist.insects.lifestage.InsectLifeStageEntityCollections
                                    .LifeStageCollection.empty();
                        }

                        @Override
                        public com.naturalist.data.Page<com.naturalist.insects.lifestage.LifeStage>
                        findPage(com.naturalist.data.PageRequest pageRequest) {
                            return com.naturalist.data.Page.empty();
                        }
                    };
                }
            };
```

Update the `InsectQueryImpl` constructor call:
```java
    InsectQuery insectQuery = new InsectQueryImpl(
            speciesQuery, imageQuery, familyQuery, genusQuery, functionalRoleQuery,
            orderQuery, citationAssociationQuery, libraryCitationQuery,
            insectLifeStageQuery);
```

Update all `assertThatThrownBy` constructor calls to include the 9th argument. Update the `constructor_collectsAllViolationsInSinglePass` test to pass 9 nulls and expect `"insectLifeStageQuery"` in the message.

Add the new null-rejection test:
```java
    @Test
    void constructor_rejectsNullInsectLifeStageQuery() {
        assertThatThrownBy(() -> new InsectQueryImpl(
                speciesQuery, imageQuery, familyQuery, genusQuery,
                functionalRoleQuery, orderQuery, citationAssociationQuery,
                libraryCitationQuery, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("insectLifeStageQuery");
    }
```

Add a test for `getByName` accessor:
```java
    @Test
    void getByName_delegatesToFactory() {
        // Unknown name returns empty — verifies the factory is wired
        assertThat(insectQuery.getByName(InsectSpeciesName.of("unobtainium-beetle")))
                .isEmpty();
    }
```

- [ ] **Step 8: Run tests to verify**

Run: `mvn test -pl domains/insects/insects-core -Dtest=InsectFactoryTest,InsectQueryImplTest`
Expected: all tests PASS.

- [ ] **Step 9: Commit**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectQuery.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectFactory.java \
       domains/insects/insects-core/src/main/java/com/naturalist/insects/InsectQueryImpl.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectFactoryTest.java \
       domains/insects/insects-core/src/test/java/com/naturalist/insects/InsectQueryImplTest.java \
       domains/insects/insects-test-context/src/main/java/com/naturalist/insects/InsectsTestContext.java
git commit -m "feat(insects): InsectFactory assembles Insect read model, InsectQuery.getByName wired"
```

---

### Task 5: Refactor controller detail pages to use `insectQuery.getByName()`

**Files:**
- Modify: `domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java`

- [ ] **Step 1: Update the `InsectsController` constructor**

The controller currently creates `InsectsTestContext` and extracts `insectQuery` and `insectLifeStageQuery` separately. With `insectQuery.getByName()` now handling life stages internally, the controller no longer needs `insectLifeStageQuery` for detail pages (it still needs it for the standalone `/life-stages` page, since that page may survive past this refactor).

No constructor changes needed — the controller already has both `insectQuery` and `insectLifeStageQuery`.

- [ ] **Step 2: Refactor `detail` method (species detail page)**

Replace the species detail method body. The read model replaces the ad-hoc query assembly for rank entities, images, life stages, and citations. Functional role, description rendering, breadcrumb, and ancestor intros stay as controller concerns.

In `InsectsController.java`, replace the `detail` method (lines 406-441):

```java
    @GetMapping("/{name}")
    String detail(@PathVariable String name, HttpServletRequest request, Model model) {
        var speciesName = InsectSpeciesName.of(name);
        var insect = insectQuery.getByName(speciesName);
        if (insect.isEmpty()) {
            return "redirect:/insects";
        }
        Insect i = insect.get();
        InsectSpecies s = i.species().species();
        InsectGenus genus = i.genus().genus();
        InsectFamily family = i.family().family();
        InsectOrder order = i.order().order();
        var description = s.description();
        model.addAttribute("species", s);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", i.lifeStages().stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList());
        model.addAttribute("images", i.observations().stream().toList());
        model.addAttribute("citations", i.citations());
        model.addAttribute("role",
                insectQuery.functionalRoles().getByParentName(speciesName).orElse(null));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("ancestorIntros", introsForSpecies(order, family, genus));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        return "insects/detail";
    }
```

Add the import at the top:
```java
import com.naturalist.insects.Insect;
```

- [ ] **Step 3: Refactor `orderDetail` method**

Replace the order detail method body (lines 302-328):

```java
    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = InsectOrderName.of(name);
        var insect = insectQuery.getByName(orderName);
        if (insect.isEmpty()) {
            return "redirect:/insects/orders";
        }
        InsectOrder order = insect.get().order().order();
        var description = order.description();
        var families = insectQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing(f -> f.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesByFamily = new LinkedHashMap<>();
        for (var f : families) {
            imagesByFamily.put(f.name(), imagesForFamily(f.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
        model.addAttribute("order", order);
        model.addAttribute("families", families);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        return "insects/order";
    }
```

- [ ] **Step 4: Refactor `familyDetail` method**

Replace the family detail method body (lines 256-284):

```java
    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = InsectFamilyName.of(name);
        var insect = insectQuery.getByName(familyName);
        if (insect.isEmpty()) {
            return "redirect:/insects/families";
        }
        InsectFamily family = insect.get().family().family();
        InsectOrder order = insect.get().order().order();
        var description = family.description();
        var genera = insectQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing(g -> g.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesByGenus = new LinkedHashMap<>();
        for (var g : genera) {
            imagesByGenus.put(g.name(), imagesForGenus(g.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("genera", genera);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("ancestorIntros", introsForFamily(order));
        return "insects/family";
    }
```

- [ ] **Step 5: Refactor `genusDetail` method**

Replace the genus detail method body (lines 352-385):

```java
    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = InsectGenusName.of(name);
        var insect = insectQuery.getByName(genusName);
        if (insect.isEmpty()) {
            return "redirect:/insects/genera";
        }
        InsectGenus genus = insect.get().genus().genus();
        InsectFamily family = insect.get().family().family();
        InsectOrder order = insect.get().order().order();
        var description = genus.description();
        var members = insectQuery.species()
                .forGenusName(genusName)
                .stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesBySpecies = new LinkedHashMap<>();
        for (var s : members) {
            imagesBySpecies.put(s.name(),
                    insectQuery.images().forParentName(s.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesBySpecies);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("species", members);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
        model.addAttribute("ancestorIntros", introsForGenus(order, family));
        return "insects/genus";
    }
```

- [ ] **Step 6: Commit**

```bash
git add domains/insects/insects-console/src/main/java/com/naturalist/insects/console/InsectsController.java
git commit -m "refactor(insects): detail pages use Insect read model via insectQuery.getByName()"
```

---

### Task 6: Add citation rendering to templates

**Files:**
- Create: `domains/insects/insects-console/src/main/jte/insects/citations.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/detail.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/order.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/family.jte`
- Modify: `domains/insects/insects-console/src/main/jte/insects/genus.jte`

- [ ] **Step 1: Create reusable `citations.jte` template fragment**

```html
@import com.naturalist.insects.InsectCitationView
@import com.naturalist.authority.OnlineSource

@param InsectCitationView citations = null

@if(citations != null && !citations.citations().isEmpty())
    <section class="citations">
        <h2>Citations</h2>
        <ul class="citation-list">
            @for(var rc : citations.citations())
                <li>
                    <a href="${rc.citation().authorityReference().url().toString()}"
                       target="naturalist-citation"
                       rel="noopener">${rc.citation().title()}</a>
                    @if(rc.citation() instanceof OnlineSource os)
                        @if(os.author() != null || os.year() != null)
                            <span class="citation-meta">
                                — @if(os.author() != null)${os.author()}@endif@if(os.author() != null && os.year() != null), @endif@if(os.year() != null)${os.year()}@endif
                            </span>
                        @endif
                    @endif
                    <small class="citation-source">(${rc.attachedAt().value()})</small>
                    @if(rc.note() != null)
                        <span class="citation-note">— ${rc.note()}</span>
                    @endif
                </li>
            @endfor
        </ul>
    </section>
@endif
```

This follows the same pattern as `citations/list.jte` in the library console: linked title, author/year metadata for `OnlineSource`, plus the `attachedAt` rank label and optional note.

- [ ] **Step 2: Add citation param and rendering to `detail.jte`**

At the top of `detail.jte`, add the import and param after the existing params (after line 25):

```
@import com.naturalist.insects.InsectCitationView
```

Add the param (after line 25, with the other params):
```
@param InsectCitationView citations = null
```

Before the closing `\`)`  (before line 193), add the citation section:
```
    @template.insects.citations(citations = citations)
```

Place it after the ecological significance section and before the closing backtick — at the bottom of the page content, just before the photo-add form or at the very end of the content sections. The exact placement: insert after the ecological significance `@endif` block (after line 191) and before the closing `\`)`.

- [ ] **Step 3: Add citation param and rendering to `order.jte`**

At the top of `order.jte`, add the import:
```
@import com.naturalist.insects.InsectCitationView
```

Add the param (after line 16):
```
@param InsectCitationView citations = null
```

Add the citation section before the families list section (after the description block, before line 32):
```
    @template.insects.citations(citations = citations)
```

- [ ] **Step 4: Add citation param and rendering to `family.jte`**

At the top of `family.jte`, add the import:
```
@import com.naturalist.insects.InsectCitationView
```

Add the param (after line 18):
```
@param InsectCitationView citations = null
```

Add the citation section after the description block, before the genera list (before line 34):
```
    @template.insects.citations(citations = citations)
```

- [ ] **Step 5: Add citation param and rendering to `genus.jte`**

At the top of `genus.jte`, add the import:
```
@import com.naturalist.insects.InsectCitationView
```

Add the param (after line 20):
```
@param InsectCitationView citations = null
```

Add the citation section after the description block, before the species list (before line 36):
```
    @template.insects.citations(citations = citations)
```

- [ ] **Step 6: Start the management console and verify**

Run the management console and check:
1. Navigate to a species detail page (e.g. `/insects/battus-philenor`) — citations section should appear with linked citation titles
2. Navigate to an order detail page (e.g. `/insects/orders/lepidoptera`) — citations should appear
3. Navigate to a family detail page — inherited citations should appear
4. Navigate to a page with no citations — no citations section should render

- [ ] **Step 7: Commit**

```bash
git add domains/insects/insects-console/src/main/jte/insects/citations.jte \
       domains/insects/insects-console/src/main/jte/insects/detail.jte \
       domains/insects/insects-console/src/main/jte/insects/order.jte \
       domains/insects/insects-console/src/main/jte/insects/family.jte \
       domains/insects/insects-console/src/main/jte/insects/genus.jte
git commit -m "feat(insects): render citation associations on detail pages"
```
