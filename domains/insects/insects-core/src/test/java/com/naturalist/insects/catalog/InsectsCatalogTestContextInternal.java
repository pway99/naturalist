package com.naturalist.insects.catalog;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.insects.catalog}. It declares no provisions: the graph builds
 * {@link InsectsCatalogContribution} by reflection over its one constructor, whose insect queries come from
 * {@link com.naturalist.insects.InsectsTestContextInternal}.
 */
public class InsectsCatalogTestContextInternal extends TestContext {
    public InsectsCatalogTestContextInternal() {
    }
}
