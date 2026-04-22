package com.naturalist.chemistry.element;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.NamedEntityRepositoryContractTest;
import com.naturalist.data.NamedTestEntitySource;

import java.util.List;


/**
 * Behavioral contract for {@link ElementRepository.ElementEntityRepository}.
 * <p>
 * Inherits the {@link NamedEntityRepositoryContractTest} cases (ADR-002).
 * Supplies Element-specific identity constants and entity construction.
 */
interface ElementEntityRepositoryTest
        extends NamedEntityRepositoryContractTest<ElementName, Element> {

    @Override
    ElementRepository.ElementEntityRepository repository();

    @Override
    default NamedTestEntitySource<ElementName, Element> source() {
        return db.getNamed(ElementTestEntitySource.class);
    }

    @Override
    default ElementName notFoundName() {
        return TestChemistryIdentifiers.Elements.NotFound.name;
    }

    @Override
    default List<ElementName> knownEntityNames() {
        return List.of(
                TestChemistryIdentifiers.Elements.Ca,
                TestChemistryIdentifiers.Elements.K
        );
    }

    @Override
    default Element newEntity() {
        return new Element(
                ElementName.of(RandomValue.string()),
                RandomValue.string(2),
                AtomicWeight.of(RandomValue.bigDecimal()),
                RandomValue.string(),
                RandomValue.integer()
        );
    }

    @Override
    default Element ghostEntity() {
        return new Element(
                ElementName.of(RandomValue.string()),
                RandomValue.string(2),
                AtomicWeight.of(RandomValue.bigDecimal()),
                RandomValue.string(),
                RandomValue.integer()
        );
    }

    @Override
    default Element modifiedEntity(Element original) {
        return new Element(
                original.name(),
                RandomValue.string(2),
                AtomicWeight.of(RandomValue.bigDecimal()),
                RandomValue.string(),
                RandomValue.integer()
        );
    }

}
