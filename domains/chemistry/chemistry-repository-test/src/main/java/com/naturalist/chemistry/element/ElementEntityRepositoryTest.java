package com.naturalist.chemistry.element;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TestChemistryIdentifiers;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;

import java.util.List;


/**
 * Behavioral contract for {@link ElementRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies Element-specific identity constants and entity construction.
 */
interface ElementEntityRepositoryTest
        extends EntityRepositoryTest<ElementName, Element> {

    @Override
    ElementRepository repository();

    @Override
    default TestEntitySource<ElementName, Element> source() {
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
                TestChemistryIdentifiers.Elements.K,
                TestChemistryIdentifiers.Elements.Zn
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
