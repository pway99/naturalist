package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;

@MockDomainService
class PlantGenusRepositoryMock
        extends AbstractTestEntityRepository<PlantGenusName, PlantGenus, PlantGenusTestEntitySource>
        implements PlantRepository.GenusRepository {

    PlantGenusRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantGenus> getByFamilyName(PlantFamilyName familyName) {
        MethodObserver getByFamilyName = observer().forMethod("getByFamilyName");
        getByFamilyName.arguments(i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(g -> familyName.equals(g.familyName()))
                .toList();
        getByFamilyName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
