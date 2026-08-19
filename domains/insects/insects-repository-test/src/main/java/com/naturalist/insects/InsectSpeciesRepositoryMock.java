package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class InsectSpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    InsectSpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpecies> getByGenusName(InsectGenusName genusName) {
        observer().arguments("getByGenusName",
                        i -> i.entityName(genusName, "genusName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> genusName.equals(s.genusName()))
                .toList();
    }
}
