package com.naturalist.garden;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Thin adapter for {@link GardenPlanQuery}: observe, dispatch, delegate to the aggregate factory
 * (ADR-010).
 */
@DomainService
class GardenPlanQueryImpl implements GardenPlanQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final GardenPlanFactory factory;

    GardenPlanQueryImpl(GardenPlanFactory factory) {
        observer.arguments("constructor", i -> i.notNull(factory, "factory")).throwWhenInvalid();
        this.factory = factory;
    }

    @Override
    public Optional<GardenPlan> getByCropTypeName(CropTypeName cropTypeName) {
        observer.arguments("getByCropTypeName", i -> i.entityName(cropTypeName, "cropTypeName"))
                .throwWhenInvalid();
        return factory.buildByName(cropTypeName);
    }
}
