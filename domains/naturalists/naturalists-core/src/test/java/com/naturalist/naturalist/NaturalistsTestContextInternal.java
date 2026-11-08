package com.naturalist.naturalist;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.naturalist}. It declares no provisions: the graph builds
 * {@link NaturalistQueryImpl} and {@link NaturalistCommandImpl} (both {@code @DomainService}, as production
 * wires them) and the {@code NaturalistEntityRepositoryMock} by reflection.
 */
public class NaturalistsTestContextInternal extends TestContext {
    public NaturalistsTestContextInternal() {
    }
}
