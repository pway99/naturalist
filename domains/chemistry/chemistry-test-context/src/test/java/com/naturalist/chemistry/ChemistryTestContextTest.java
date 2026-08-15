package com.naturalist.chemistry;

import com.naturalist.chemistry.TestChemistryIdentifiers.Elements;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryTestContextTest {

    private final ChemistryTestContext context =
            ChemistryTestContext.create(NaturalistDatabase.create());

    @Test
    void elementQueryReadsTheElementCatalog() {
        assertThat(context.elementQuery().getByName(Elements.Ca))
                .get()
                .satisfies(element -> assertThat(element.symbol()).isEqualTo("Ca"));
    }

    @Test
    void elementQueryPagesTheWholeCatalog() {
        assertThat(context.elementQuery().findPage(PageRequest.console(0)).content())
                .hasSizeGreaterThanOrEqualTo(16);
    }

    @Test
    void elementQueryReturnsEmptyForAnUncataloguedName() {
        assertThat(context.elementQuery().getByName(Elements.NotFound.name)).isEmpty();
    }
}
