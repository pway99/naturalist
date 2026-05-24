package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.infrastructure.DomainService;

import java.util.List;

@DomainService
class SpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
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

    @Override
    public List<InsectSpecies> getByFamilyName(InsectFamilyName familyName) {
        observer().arguments("getByFamilyName",
                        i -> i.entityName(familyName, "familyName"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> familyName.equals(s.familyName()))
                .toList();
    }
}
