package com.naturalist.chemistry.element;

import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ElementQueryImplTest {
    @RegisterExtension
    NaturalistDatabase db = NaturalistDatabase.create();

    ElementRepository.ElementEntityRepository repository = new ElemenEntitytRepositoryMock(db);
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

    @Test
    void findByIdSet_happyPath() {
        Element carbon = repository.getByName(TestChemistryIdentifiers.Elements.C).orElseThrow();
        Element hydrogen = repository.getByName(TestChemistryIdentifiers.Elements.H).orElseThrow();
        Set<ElementId> elementIds = Set.of(carbon.id(), hydrogen.id());

        ElementCollection ec = elementQuery.findByIdSet(elementIds);

        assertThat(ec).isNotNull();
        assertThat(ec.size()).isEqualTo(2);
        assertThat(ec.stream().toList())
                .containsExactlyInAnyOrder(carbon, hydrogen);
    }
}
