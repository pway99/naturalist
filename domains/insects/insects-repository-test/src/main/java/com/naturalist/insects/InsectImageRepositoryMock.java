package com.naturalist.insects;

import com.naturalist.data.MockDomainService;
import com.naturalist.observation.OrganismImage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;
import java.util.Set;

@MockDomainService
class InsectImageRepositoryMock
        extends AbstractTestEntityRepository<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>, InsectImageTestEntitySource>
        implements InsectRepository.ImageRepository {

    InsectImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(image -> image.parentName().equals(parentName))
                .toList();
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentNames(Set<InsectRankName> parentNames) {
        observer().arguments("getByParentNames", i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(image -> parentNames.contains(image.parentName()))
                .toList();
    }
}
