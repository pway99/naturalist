package com.naturalist.soil;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

/**
 * Thin adapter for {@link SoilProfileInfoQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class SoilProfileInfoQueryImpl
        extends AbstractEntityQuery<
        SoilProfileName,
        SoilProfileInfo,
        SoilProfileInfoCollection,
        SoilProfileInfoRepository>
        implements SoilProfileInfoQuery {

    SoilProfileInfoQueryImpl(SoilProfileInfoRepository repository) {
        super(repository);
    }

    @Override
    public SoilProfileInfoCollection findByNameSet(Set<SoilProfileName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return SoilProfileInfoCollection.of(repository().getByEntityNameSet(names));
    }
}
