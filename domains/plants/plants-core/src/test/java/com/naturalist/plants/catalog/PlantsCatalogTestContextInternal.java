package com.naturalist.plants.catalog;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.plants.catalog}. It declares no provisions: the graph builds
 * {@link PlantsCatalogContribution} and {@link PlantsCompoundReferences} by reflection, over the queries the
 * plants and phytochemistry contexts supply.
 */
public class PlantsCatalogTestContextInternal extends TestContext {
    public PlantsCatalogTestContextInternal() {
    }
}
