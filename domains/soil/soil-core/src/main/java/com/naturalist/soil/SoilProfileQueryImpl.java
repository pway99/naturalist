package com.naturalist.soil;

import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Thin adapter for {@link SoilProfileQuery}: validates the argument, then delegates aggregate
 * assembly to {@link SoilProfileFactory} (ADR-010). Factory-backed query bean;
 * {@link SoilProfileFactory} is injected.
 */
@DomainService
class SoilProfileQueryImpl implements SoilProfileQuery {

    private final Observer observer = Observer.forClass(getClass());
    private final SoilProfileFactory factory;

    SoilProfileQueryImpl(SoilProfileFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<SoilProfile> getBySoilProfileName(SoilProfileName soilProfileName) {
        observer.arguments("getBySoilProfileName", i -> i.entityName(soilProfileName, "soilProfileName"))
                .throwWhenInvalid();
        return factory.buildByName(soilProfileName);
    }
}
