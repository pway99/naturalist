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
 * Supplies InsectOrderInfo-specific identity constants and entity construction.
 */
interface InsectOrderRepositoryTest
        extends EntityRepositoryTest<InsectOrderName, InsectOrderInfo> {

    @Override
    InsectRepository.OrderRepository repository();

    @Override
    default TestEntitySource<InsectOrderName, InsectOrderInfo> source() {
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
    default InsectOrderInfo newEntity() {
        return new InsectOrderInfo(
                InsectOrderName.of("test-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectOrderInfo ghostEntity() {
        return new InsectOrderInfo(
                InsectOrderName.of("test-ghost-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default InsectOrderInfo modifiedEntity(InsectOrderInfo original) {
        return new InsectOrderInfo(
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
