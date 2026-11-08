package com.naturalist.garden;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.garden}. It declares no provisions: the graph builds
 * {@link PlantingQueryImpl}, {@link PlantedZoneFactory} and {@link PlantedZoneQueryImpl} (all three are
 * {@code @DomainService}, as production wires them) and the {@code PlantingEntityRepositoryMock} by
 * reflection.
 */
public class GardenTestContextInternal extends TestContext {
    public GardenTestContextInternal() {
    }
}
