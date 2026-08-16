package com.naturalist.plants;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class SpeciesRepositoryMock
        extends AbstractTestEntityRepository<PlantSpeciesName, PlantSpecies, PlantSpeciesTestEntitySource>
        implements PlantRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<PlantSpecies> getByGenusName(PlantGenusName genusName) {
        observer().arguments("getByGenusName", i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusName.equals(s.genusName()))
                .toList();
    }
}
