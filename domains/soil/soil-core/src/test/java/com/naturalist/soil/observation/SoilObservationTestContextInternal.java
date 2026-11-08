package com.naturalist.soil.observation;

import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.soil.observation}. It declares no provisions: the graph builds the
 * five observation {@code *QueryImpl} classes (lab analysis, nutrient reading, physical characteristics,
 * reported optimum, reported recommendation) and their {@code *EntityRepositoryMock}s by reflection.
 */
public class SoilObservationTestContextInternal extends TestContext {
    public SoilObservationTestContextInternal() {
    }
}
