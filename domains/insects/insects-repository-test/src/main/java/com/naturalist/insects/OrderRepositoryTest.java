package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link InsectRepository.OrderRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies InsectOrder-specific identity constants and entity construction.
 */
interface OrderRepositoryTest
        extends EntityRepositoryTest<InsectOrderName, InsectOrder> {

    @Override
    InsectRepository.OrderRepository repository();

    @Override
    default TestEntitySource<InsectOrderName, InsectOrder> source() {
        return db.getNamed(InsectOrderTestEntitySource.class);
    }

    @Override
    default InsectOrderName notFoundName() {
        return TestInsectsIdentifiers.InsectOrder.NotFound.name;
    }

    @Override
    default List<InsectOrderName> knownEntityNames() {
        return List.of(
                TestInsectsIdentifiers.InsectOrder.Diptera.name,
                TestInsectsIdentifiers.InsectOrder.Hymenoptera.name);
    }

    @Override
    default InsectOrder newEntity() {
        return new InsectOrder(
                InsectOrderName.of("test-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectOrder ghostEntity() {
        return new InsectOrder(
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectOrder modifiedEntity(InsectOrder original) {
        return new InsectOrder(
                original.name(),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(CommonName.of("alt-" + RandomValue.string())),
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
