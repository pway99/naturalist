package com.naturalist.soil;

import com.naturalist.observability.Observer;

import java.util.Optional;

/**
 * Thin adapter for {@link SoilProfileQuery}: validates the argument, then delegates aggregate
 * assembly to {@link SoilProfileFactory} (ADR-010). Mirrors {@code InsectTaxonViewQueryImpl} — no
 * {@code @DomainService}: the factory-backed aggregate query is wired manually in the context
 * (its {@code SoilProfileFactory} is not a Spring bean), never component-scanned.
 */
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
