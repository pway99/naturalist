package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.MockDomainService;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;

@MockDomainService
class PlantSeedLineageRepositoryMock
        extends AbstractTestEntityRepository<SeedLineageName, SeedLineage, PlantSeedLineageTestEntitySource>
        implements SeedLineageRepository {

    PlantSeedLineageRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<SeedLineage> getByCultivarName(CultivarName cultivarName) {
        MethodObserver getByCultivarName = observer().forMethod("getByCultivarName");
        getByCultivarName.arguments(i -> i.entityName(cultivarName, "cultivarName"))
                .throwWhenInvalid();
        var result = testEntitySource().entityStream()
                .filter(s -> cultivarName.equals(s.cultivarName()))
                .toList();
        getByCultivarName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }
}
