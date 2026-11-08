package com.naturalist.plants;

import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import com.naturalist.test.context.Provisions;
import com.naturalist.test.context.TestContext;

/**
 * The test context of {@code com.naturalist.plants}. The graph builds the package's {@code @DomainService}
 * implementations and {@code @MockDomainService} repository mocks by reflection. This context provides only
 * {@link PlantFactory}, which is no {@code @DomainService} (production builds it inside {@link PlantQueryImpl}),
 * from the same queries the graph builds. The cultivar, management and phytochemistry queries come from their
 * own packages' contexts.
 */
public class PlantsTestContextInternal extends TestContext {
    public PlantsTestContextInternal() {
    }

    @Override
    protected void provisions(Provisions p) {
        p.provide(PlantFactory.class, g -> new PlantFactory(
                g.get(PlantQuery.SpeciesQuery.class), g.get(PlantQuery.GenusQuery.class),
                g.get(PlantQuery.FamilyQuery.class), g.get(PlantQuery.OrderQuery.class),
                g.get(PlantFeatureQueryImpl.class), g.get(PlantQuery.EcologicalRoleQuery.class),
                g.get(PlantQuery.ImageQuery.class), g.get(CultivarQuery.class),
                g.get(PlantProgramQuery.class), g.get(PhytochemicalConstituentQuery.class)));
    }
}
