package com.naturalist.chemistry.element;

import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ElementQueryImplTest {
    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    ElementRepository repository = new ElementEntityRepositoryMock(nte);
    ElementQuery elementQuery = new ElementQueryImpl(repository);

    @Test
    void findByNameSet_happyPath() {
        Set<ElementName> elementNames = Set.of(
                TestChemistryIdentifiers.Elements.C,
                TestChemistryIdentifiers.Elements.H
        );
        List<Element> expected = repository.getByEntityNameSet(elementNames);
        assertThat(expected)
                .hasSize(2)
                .extracting(Element::name)
                .containsAnyElementsOf(elementNames);

        ElementCollection ec = elementQuery.findByNameSet(elementNames);

        assertThat(ec).isNotNull();
        assertThat(ec.size()).isEqualTo(2);
        assertThat(ec.stream().toList())
                .containsExactlyElementsOf(expected);
    }
}
