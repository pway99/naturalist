package com.naturalist.garden;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;

import java.util.Set;

/**
 * Thin adapter for {@link CropTypeQuery}: observe, dispatch, delegate (ADR-010).
 */
@DomainService
class CropTypeQueryImpl
        extends AbstractEntityQuery<CropTypeName, CropType, CropTypeCollection, CropTypeRepository>
        implements CropTypeQuery {

    CropTypeQueryImpl(CropTypeRepository repository) {
        super(repository);
    }

    @Override
    public CropTypeCollection findByNameSet(Set<CropTypeName> names) {
        observer().arguments("findByNameSet", i -> i.entityNameCollection(names, "names"))
                .throwWhenInvalid();
        return CropTypeCollection.of(repository().getByEntityNameSet(names));
    }
}
