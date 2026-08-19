package com.naturalist.plants;

import com.naturalist.data.AbstractEntityQuery;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observation.OrganismImage;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;

import java.util.Set;

@DomainService
class PlantImageQueryImpl
        extends AbstractEntityQuery<
        PlantImageId,
        OrganismImage<PlantImageId, PlantObservationId, PlantRankName>,
        ImageCollection,
        PlantRepository.ImageRepository>
        implements PlantQuery.ImageQuery {

    PlantImageQueryImpl(PlantRepository.ImageRepository repository) {
        super(repository);
    }

    @Override
    public ImageCollection findByNameSet(Set<PlantImageId> names) {
        observer().arguments("findByNameSet", i -> i.identifierSet(names, "names"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByEntityNameSet(names));
    }

    @Override
    public ImageCollection forParentName(PlantRankName parentName) {
        observer().arguments("forParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return ImageCollection.of(repository().getByParentName(parentName));
    }
}
