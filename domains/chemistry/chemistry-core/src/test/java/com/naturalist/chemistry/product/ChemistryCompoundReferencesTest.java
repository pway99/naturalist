package com.naturalist.chemistry.product;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.catalog.inmem.CatalogAssembly;
import com.naturalist.chemistry.ChemistryDomain;
import com.naturalist.chemistry.TestChemistryIdentifiers.Compounds;
import com.naturalist.chemistry.TestChemistryIdentifiers.Products;
import com.naturalist.chemistry.catalog.ChemistryCompoundReferences;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lives in {@code com.naturalist.chemistry.product} (not {@code .catalog}) so
 * the test can see the package-private {@link ProductEntityRepositoryMock} and
 * {@link ProductQueryImpl} directly.
 */
class ChemistryCompoundReferencesTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final ProductRepository repository = new ProductEntityRepositoryMock(nte);
    private final ProductQuery products = new ProductQueryImpl(repository);
    private final ChemistryCompoundReferences provider = new ChemistryCompoundReferences(products);

    @Test
    void domainIsChemistry() {
        assertThat(provider.domain()).isEqualTo(new ChemistryDomain());
    }

    @Test
    void referenceTypeIsCompoundName() {
        assertThat(provider.referenceType()).isEqualTo(CompoundName.class);
    }

    @Test
    void constructorRejectsNullProductQuery() {
        assertThatThrownBy(() -> new ChemistryCompoundReferences(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("products");
    }

    @Test
    void thymolResolvesToApiguard() {
        List<EntityRef> refs = provider.referencesTo(Compounds.Thymol.name).toList();

        EntityRef expected = new EntityRef(new ChemistryDomain(), Products.Apiguard.name);
        assertThat(refs).containsExactly(expected);
    }

    @Test
    void calciumChlorideResolvesIndependentlyOfThymol() {
        List<EntityRef> refs = provider.referencesTo(Compounds.CalciumChloride.name).toList();

        EntityRef expected = new EntityRef(new ChemistryDomain(), Products.BonideRotStopRtu.name);
        assertThat(refs).containsExactly(expected);
    }

    @Test
    void unknownCompoundReturnsEmpty() {
        assertThat(provider.referencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void nullTargetReturnsEmpty() {
        // Defensive — the kernel maps null to an empty result at the Catalog
        // surface, so a provider should not throw on null.
        assertThat(provider.referencesTo(null)).isEmpty();
    }

    @Test
    void catalogFindReferencesToGroupsResultsUnderChemistryDomain() {
        Catalog catalog = CatalogAssembly.from(List.of(),
                List.of(provider));

        Map<DomainId, List<EntityRef>> result = catalog.findReferencesTo(Compounds.Thymol.name);

        assertThat(result).containsOnlyKeys(new ChemistryDomain());
        assertThat(result.get(new ChemistryDomain()))
                .extracting(EntityRef::name)
                .containsExactly(Products.Apiguard.name);
    }

    @Test
    void catalogFindReferencesToReturnsEmptyForUnknownCompound() {
        Catalog catalog = CatalogAssembly.from(List.of(),
                List.of(provider));

        assertThat(catalog.findReferencesTo(Compounds.NotFound.name)).isEmpty();
    }

    @Test
    void catalogDomainsReferencingExposesChemistryForCompoundName() {
        Catalog catalog = CatalogAssembly.from(List.of(),
                List.of(provider));

        assertThat(catalog.domainsReferencing(CompoundName.class))
                .containsExactly(new ChemistryDomain());
    }
}
