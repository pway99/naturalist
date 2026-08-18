package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class PlantImageRepositoryMock
        extends AbstractTestEntityRepository<PlantImageId, PlantImage, PlantImageTestEntitySource>
        implements PlantRepository.ImageRepository {

    PlantImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantImage> getByParentName(PlantRankName parentName) {
        observer().arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(image -> image.parentName().equals(parentName))
                .toList();
    }
}
