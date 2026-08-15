package com.naturalist.chemistry;

import com.naturalist.catalog.*;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.chemistry.TestChemistryIdentifiers.Compounds;
import com.naturalist.chemistry.TestChemistryIdentifiers.Elements;
import com.naturalist.chemistry.TestChemistryIdentifiers.Products;
import com.naturalist.chemistry.catalog.ChemistryCatalogContribution;
import com.naturalist.chemistry.compound.CompoundQuery;
import com.naturalist.chemistry.compound.CompoundQueryTestContextInternal;
import com.naturalist.chemistry.element.ElementQuery;
import com.naturalist.chemistry.element.ElementQueryTestContextInternal;
import com.naturalist.chemistry.product.ProductQuery;
import com.naturalist.chemistry.product.ProductQueryTestContextInternal;
import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.data.Pages;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.chemistry} (not {@code .catalog}) and uses the
 * package-friendly {@code *QueryTestContextInternal} classes in each sub-package so a
 * single test can wire both compounds and products without exposing either
 * sub-context's package-private mocks and adapters.
 */
class ChemistryCatalogContributionTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final CompoundQuery.CompoundEntityQuery compounds =
            CompoundQueryTestContextInternal.createEntityQuery(db);
    private final ProductQuery products = ProductQueryTestContextInternal.createQuery(db);
    private final ElementQuery elements = ElementQueryTestContextInternal.createQuery(db);
    private final ChemistryCatalogContribution contribution =
            new ChemistryCatalogContribution(compounds, products, elements);

    @Test
    void domainIsChemistry() {
        assertThat(contribution.domain()).isEqualTo(new ChemistryDomain());
    }

    @Test
    void constructorRejectsNullCompoundQuery() {
        assertThatThrownBy(() -> new ChemistryCatalogContribution(null, products, elements))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("compounds");
    }

    @Test
    void constructorRejectsNullProductQuery() {
        assertThatThrownBy(() -> new ChemistryCatalogContribution(compounds, null, elements))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("products");
    }

    @Test
    void constructorRejectsNullElementQuery() {
        assertThatThrownBy(() -> new ChemistryCatalogContribution(compounds, products, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("elements");
    }

    @Test
    void contributionEmitsOneSearchableEntityPerCompoundAndProductAndElement() {
        long compoundCount = Pages.stream(1000, compounds::findPage).count();
        long productCount = Pages.stream(1000, products::findPage).count();
        long elementCount = Pages.stream(1000, elements::findPage).count();
        assertThat(contribution.searchableEntities().count())
                .isEqualTo(compoundCount + productCount + elementCount);
    }

    @Test
    void everySearchableEntityIsAttributedToTheChemistryDomain() {
        contribution.searchableEntities().forEach(e ->
                assertThat(e.target().domain()).isEqualTo(new ChemistryDomain()));
    }

    @Test
    void compoundIsReachableThroughItsSlug() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Compounds.CalciumSulfateDihydrate.name);

        assertThat(catalog.search("calcium-sulfate-dihydrate").stream())
                .as("slug 'calcium-sulfate-dihydrate' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
    }

    @Test
    void compoundIsReachableThroughItsCommonName() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Compounds.FormicAcid.name);

        assertThat(targetsOf(catalog.search("Formic Acid")))
                .as("common name 'Formic Acid' should resolve to %s", expected)
                .contains(expected);
    }

    @Test
    void compoundIsReachableThroughItsFormula() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Compounds.FormicAcid.name);

        assertThat(targetsOf(catalog.search("HCOOH")))
                .as("formula 'HCOOH' should resolve to %s", expected)
                .contains(expected);
    }

    @Test
    void productIsReachableThroughItsSlug() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Products.Apiguard.name);

        assertThat(catalog.search("apiguard").stream())
                .as("slug 'apiguard' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
    }

    @Test
    void productIsReachableThroughItsDisplayName() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Products.Apiguard.name);

        assertThat(targetsOf(catalog.search("Apiguard")))
                .as("display name 'Apiguard' should resolve to %s", expected)
                .contains(expected);
    }

    @Test
    void elementIsReachableThroughItsSlug() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Elements.Ca);

        assertThat(catalog.search("calcium").stream())
                .as("slug 'calcium' should resolve to %s as EXACT_SLUG", expected)
                .anyMatch(h -> h.target().equals(expected) && h.kind() == MatchKind.EXACT_SLUG);
    }

    @Test
    void elementIsReachableThroughItsSymbol() {
        Catalog catalog = CatalogAssembly.from(contribution);
        EntityRef expected = new EntityRef(new ChemistryDomain(), Elements.Zn);

        assertThat(catalog.search("Zn").stream())
                .as("symbol 'Zn' should resolve to %s", expected)
                .anyMatch(h -> h.target().equals(expected));
    }

    @Test
    void findBySlugResolvesAnElementToTheChemistryDomain() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.findBySlug("boron"))
                .get()
                .isEqualTo(new EntityRef(new ChemistryDomain(), Elements.B));
    }

    @Test
    void unknownTokenReturnsEmptyResults() {
        Catalog catalog = CatalogAssembly.from(contribution);

        assertThat(catalog.search("qqqqxxxx").isEmpty()).isTrue();
        assertThat(catalog.search("zzzzzzz").isEmpty()).isTrue();
    }

    private static List<EntityRef> targetsOf(SearchResults results) {
        return results.stream().map(SearchHit::target).toList();
    }
}
