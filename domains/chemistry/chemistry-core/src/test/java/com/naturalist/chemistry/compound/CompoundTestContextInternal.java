package com.naturalist.chemistry.compound;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.chemistry.compound}. It declares no provisions: the graph builds
 * {@code CompoundQueryImpl}, {@code CompoundEntityQueryImpl}, {@code DepictionQueryImpl} and the
 * {@code CompoundEntityRepositoryMock} / {@code DepictionEntityRepositoryMock} by reflection.
 */
public class CompoundTestContextInternal extends TestContext {
    public CompoundTestContextInternal() {
    }
}
