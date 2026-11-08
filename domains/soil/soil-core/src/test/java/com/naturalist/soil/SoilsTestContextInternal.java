package com.naturalist.soil;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.soil}. It declares no provisions: the graph builds
 * {@link SoilProfileInfoQueryImpl}, {@link SoilProfileFactory} and {@link SoilProfileQueryImpl} (all three are
 * {@code @DomainService}, as production wires them) and the {@code SoilProfileInfoEntityRepositoryMock} by
 * reflection. The factory's five observation queries come from {@code SoilObservationTestContextInternal}.
 */
public class SoilsTestContextInternal extends TestContext {
    public SoilsTestContextInternal() {
    }
}
