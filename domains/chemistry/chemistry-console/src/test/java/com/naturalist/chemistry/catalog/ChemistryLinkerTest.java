package com.naturalist.chemistry.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.chemistry.product.ProductName;
import com.naturalist.ddd.EntityName;
import com.naturalist.soil.observation.NutrientName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryLinkerTest {

    // The linker only switches on ref.name(); the domain is irrelevant here.
    private record TestDomain() implements DomainId {
        @Override
        public String value() {
            return "chemistry";
        }
    }

    private final ChemistryLinker linker = new ChemistryLinker();

    private String link(EntityName name) {
        return linker.linkFor(new EntityRef(new TestDomain(), name));
    }

    @Test
    void linksElementToElementDetail() {
        assertThat(link(ElementName.of("calcium"))).isEqualTo("/chemistry/elements/calcium");
    }

    @Test
    void linksCompoundToCompoundDetail() {
        assertThat(link(CompoundName.of("calcium-sulfate-dihydrate")))
                .isEqualTo("/chemistry/calcium-sulfate-dihydrate");
    }

    @Test
    void linksProductToProductDetail() {
        assertThat(link(ProductName.of("apiguard"))).isEqualTo("/chemistry/products/apiguard");
    }

    @Test
    void returnsNullForANameTypeItDoesNotOwn() {
        assertThat(link(NutrientName.of("calcium-soluble"))).isNull();
    }
}
