package com.naturalist.insects;

import com.naturalist.observation.OrganismImage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class InsectImageRepositoryMock
        extends AbstractTestEntityRepository<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, InsectImageTestEntitySource>
        implements InsectRepository.ImageRepository {

    InsectImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentName(InsectRankName parentName) {
        return testEntitySource().entityStream()
                .filter(image -> image.parentName().equals(parentName))
                .toList();
    }
}
