package com.naturalist.chemistry.element;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.EntityRepositoryContractTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;


/**
 * Behavioral contract for {@link ElementRepository.ElementEntityRepository}.
 * <p>
 * Inherits all 22 standard {@link EntityRepositoryContractTest} cases (ADR-002).
 * Supplies Element-specific identity constants and entity construction.
 */
interface ElementEntityRepositoryTest
        extends EntityRepositoryContractTest<ElementId, ElementName, Element> {

    @Override
    ElementRepository.ElementEntityRepository repository();

    @Override
    default TestEntitySource<ElementId, ElementName, Element> source() {
        return db.get(ElementTestEntitySource.class);
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
    default ElementId notFoundId() {
        return ElementId.of(Long.MAX_VALUE);
    }

    @Override
    default Element newEntity() {
        return new Element(
                null,
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
                ElementId.of(Long.MAX_VALUE),
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
                original.id(),
                original.name(),
                RandomValue.string(2),
                AtomicWeight.of(RandomValue.bigDecimal()),
                RandomValue.string(),
                RandomValue.integer()
        );
    }

}
