package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicOrder;

import java.util.List;
import java.util.Set;

/**
 * Behavioral contract for {@link PlantRepository.OrderRepository}.
 * <p>
 * Inherits the {@link EntityRepositoryTest} cases (ADR-002).
 * Supplies PlantOrder-specific identity constants and entity construction.
 */
interface PlantOrderRepositoryTest
        extends EntityRepositoryTest<PlantOrderName, PlantOrder> {

    @Override
    PlantRepository.OrderRepository repository();

    @Override
    default TestEntitySource<PlantOrderName, PlantOrder> source() {
        return db.getNamed(PlantOrderTestEntitySource.class);
    }

    @Override
    default PlantOrderName notFoundName() {
        return TestPlantsIdentifiers.PlantOrders.NotFound.name;
    }

    @Override
    default List<PlantOrderName> knownEntityNames() {
        return List.of(
                TestPlantsIdentifiers.PlantOrders.Lamiales.name,
                TestPlantsIdentifiers.PlantOrders.Piperales.name);
    }

    @Override
    default PlantOrder newEntity() {
        return new PlantOrder(
                PlantOrderName.of("test-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default PlantOrder ghostEntity() {
        return new PlantOrder(
                PlantOrderName.of("test-ghost-order-xx"),
                TaxonomicOrder.of("Order" + RandomValue.string()),
                description(),
                Set.of(),
                null);
    }

    @Override
    default PlantOrder modifiedEntity(PlantOrder original) {
        return new PlantOrder(
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
