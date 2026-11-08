package com.naturalist.chemistry.catalog;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.chemistry.catalog}. It declares no provisions: the graph builds
 * {@code ChemistryCatalogContribution} and {@code ChemistryCompoundReferences} by reflection, over the queries
 * the compound, product and element contexts build.
 */
public class ChemistryCatalogTestContextInternal extends TestContext {
    public ChemistryCatalogTestContextInternal() {
    }
}
