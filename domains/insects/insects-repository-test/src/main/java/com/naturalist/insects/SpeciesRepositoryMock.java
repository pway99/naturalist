package com.naturalist.insects;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.NaturalistDatabase;

import java.util.List;

class SpeciesRepositoryMock
        extends AbstractTestEntityRepository<InsectSpeciesName, InsectSpecies, InsectSpeciesTestEntitySource>
        implements InsectRepository.SpeciesRepository {

    SpeciesRepositoryMock(NaturalistDatabase naturalistDatabase) {
        super(naturalistDatabase);
    }

    @Override
    public List<InsectSpeciesName> getAllSpeciesNames() {
        return testEntitySource().entityStream()
                .map(InsectSpecies::name)
                .toList();
    }

    @Override
    public List<InsectSpecies> getByFunctionalGuild(FunctionalGuild functionalGuild) {
        observer().arguments("getByFunctionalGuild", i -> i
                .notNull(functionalGuild, "functionalGuild"))
                .throwWhenInvalid();
        return testEntitySource().entityStream()
                .filter(s -> s.guilds().contains(functionalGuild))
                .toList();
    }
}
