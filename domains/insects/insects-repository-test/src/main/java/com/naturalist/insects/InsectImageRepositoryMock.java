package com.naturalist.insects;

import com.naturalist.data.MockDomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
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
        MethodObserver getByParentName = observer().forMethod("getByParentName");
        getByParentName.arguments(i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(image -> image.parentName().equals(parentName))
                .toList();
        getByParentName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentNames(Set<InsectRankName> parentNames) {
        MethodObserver getByParentNames = observer().forMethod("getByParentNames");
        getByParentNames.arguments(i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(image -> parentNames.contains(image.parentName()))
                .toList();
        getByParentNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
