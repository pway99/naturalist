package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observation.OrganismImage;

import java.util.List;

@MockDomainService
class PlantImageRepositoryMock
        extends AbstractTestEntityRepository<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>, PlantImageTestEntitySource>
        implements PlantRepository.ImageRepository {

    PlantImageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> getByParentName(PlantRankName parentName) {
        MethodObserver getByParentName = observer().forMethod("getByParentName");
        getByParentName.arguments(i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(image -> image.parentName().equals(parentName))
                .toList();
        getByParentName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
